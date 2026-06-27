package system.map

import World
import config.Sim
import config.Styles
import extension.*
import external.MapLibre
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.ImageData
import system.building.BuildingShake
import system.building.BuildingStream
import system.building.BuildingTiles
import system.display.OwnBuildings
import system.display.Scene3D
import system.ui.LoadingOverlay
import util.PortalNames
import util.Rng
import util.data.Pos
import kotlin.js.Json
import kotlin.math.log2
import kotlin.math.roundToInt

object MapController {
    private fun mapOptions(container: String, style: dynamic, preserveBuffer: Boolean): dynamic {
        val opts: dynamic = js("({})")
        opts.container = container
        opts.style = style
        opts.preserveDrawingBuffer = preserveBuffer
        // Collapse the required tile attribution to a small "ⓘ" that expands on click (the compliant
        // way to make it unobtrusive — OSM/Esri terms require attribution, so we don't suppress it).
        opts.attributionControl = js("({ compact: true })")
        return opts
    }

    private fun initInitialMapbox(): MapLibre.Map {
        val style = if (demoMode) MapStyles.DEMO_STYLE else MapStyles.SATELLITE_STYLE
        return MapLibre.Map(mapOptions(INITIAL_MAP, JSON.parse<Json>(style), false))
    }

    private fun initMapbox(): MapLibre.Map = MapLibre.Map(mapOptions(MAP, MapStyles.STREET_STYLE_URL, false))

    // preserveDrawingBuffer is required so the rendered street mask can be read
    // back with gl.readPixels (otherwise the buffer is cleared after compositing).
    private fun initShadowMap(): MapLibre.Map = MapLibre.Map(mapOptions(SHADOW_MAP, JSON.parse<Json>(MapStyles.SHADOW_STYLE), true))

    private const val INITIAL_MAP = "initialMap"
    private const val MAP = "map"
    private const val SHADOW_MAP = "shadowMap"
    private const val INVISIBLE = "invisible"

    // The grid/sim is built at ZOOM (the anchor). Min/max are a free range for the
    // camera; zooming just scales the canvas layer — the grid is never rebuilt.
    private const val ZOOM = 18
    private const val MIN_ZOOM = 3
    private const val MAX_ZOOM = 21

    // The grid is read from the shadow map at ZOOM over a Sim-sized canvas (SCALE× the screen).
    // The visible maps start zoomed out to frame that whole play area; the anchor stays at ZOOM.
    // Framed so the chosen play-area size fits; read at call time since the size is picked at onboarding.
    private fun displayZoomForSize() = (ZOOM - log2(Sim.scale)).roundToInt()
    private const val DEMO_ZOOM = 19 // demos frame one central object, so sit closer than the game
    private const val TITLE_ZOOM_BOOST = 1.1 // a touch closer
    private const val TITLE_PITCH = 35.0 // fairly top-down → the action sits just below screen centre (less sky/skyline)
    private var demoMode = false
    private fun displayZoom() = if (demoMode) DEMO_ZOOM else displayZoomForSize()
    private const val DEFAULT_PITCH = 50.0 // tilt the visible maps so the 3D scene reads as 3D
    private const val MAX_PITCH = 85.0 // MapLibre's ceiling; lets you tilt to near-horizon (and to 0 = top-down)

    private var map: MapLibre.Map? = null
    private var initMap: MapLibre.Map? = null
    private var shadowMap: MapLibre.Map? = null

    // Camera-follow anchor: the simulation lives in a fixed pixel space built at
    // this map view. We transform the canvas layer to follow the live camera.
    private var anchorCenter: dynamic = null
    private var anchorZoom = 0.0

    private fun referenceMap(): MapLibre.Map? = initMap ?: map

    /** Capture the view the grid/sim was built at (call once the grid is ready). */
    private fun captureAnchor() {
        val m = referenceMap() ?: return
        anchorCenter = m.getCenter()
        anchorZoom = ZOOM.toDouble() // grid is read at ZOOM regardless of the (framed) display zoom
    }

    fun rotateBy(degrees: Double) {
        cancelAutoCamFromUser()
        initMap?.let { it.setBearing(it.getBearing() + degrees) }
        map?.let { it.setBearing(it.getBearing() + degrees) }
    }

    fun pitchBy(degrees: Double) {
        cancelAutoCamFromUser()
        initMap?.let { it.setPitch((it.getPitch() + degrees).coerceIn(0.0, MAX_PITCH)) }
        map?.let { it.setPitch((it.getPitch() + degrees).coerceIn(0.0, MAX_PITCH)) }
    }

    fun panBy(dx: Double, dy: Double) {
        cancelAutoCamFromUser()
        val opts: dynamic = js("({animate: false})")
        val offset = arrayOf(dx, dy)
        initMap?.panBy(offset, opts)
        map?.panBy(offset, opts)
    }

    /**
     * "Home": fly the camera back over the play area, framed and **top-down** (pitch 0, bearing 0) —
     * so a player who has panned/rotated away can instantly find the action again. Centers on the
     * grid anchor at the framed display zoom.
     */
    fun goHome(durationMs: Int = 900) {
        val center = anchorCenter ?: return
        val opts: dynamic = js("({ pitch: 0.0, bearing: 0.0 })")
        opts.duration = durationMs
        opts.center = center
        opts.zoom = displayZoom()
        opts.padding = js("({ top: 0.0, bottom: 0.0, left: 0.0, right: 0.0 })") // clear the build-time centre lift
        initMap?.asDynamic()?.flyTo(opts)
        map?.asDynamic()?.flyTo(opts)
    }

    /** Zoom the live map by [delta] levels with a short ease (keyboard zoom: PageUp/PageDown). */
    fun zoomBy(delta: Double) {
        val m = map ?: return
        val opts: dynamic = js("({ duration: 220 })")
        m.asDynamic().zoomTo(m.getZoom() + delta, opts)
    }

    private const val BUILD_SPIN_DEG = 0.12 // gentle bearing orbit during world build (~7°/s)
    private const val BUILD_CENTRE_LIFT_FRAC = 0.4 // raise the tilted play-area centre toward screen centre during build
    private var cinematicActive = false

    /** A slow orbit around the play area while the world builds (close — portal placements stay visible). */
    fun startBuildCinematic() {
        if (cinematicActive) return
        cinematicActive = true
        liftViewToCentre() // face the play-area centre from the start (the 3D tilt otherwise sinks it to the bottom)
        window.requestAnimationFrame { spinBuild() }
        applyBuildInflate(0.0) // start flat; the city rises in step with world-gen progress (setBuildProgress)
    }

    // The build camera keeps DEFAULT_PITCH for the 3D look, which pushes the play-area centre low on
    // screen. A bottom padding (viewport-relative, so it's stable under the bearing spin) lifts the
    // centre back up to the middle so the first flow-field vectors / portals read centre-frame.
    private fun liftViewToCentre() {
        if (demoMode) return
        val m = initMap ?: return
        val pad: dynamic = js("({ top: 0.0, left: 0.0, right: 0.0 })")
        pad.bottom = window.innerHeight * BUILD_CENTRE_LIFT_FRAC
        m.asDynamic().setPadding(pad)
    }

    private const val BUILD_INFLATE_MS = 2800.0 // TITLE-only fixed-duration rise (~3 s; title has no world-gen progress)
    private var inflateStart = 0.0

    /** TITLE: animate the 3D buildings rising over a fixed duration (no loading overlay there). */
    private fun startBuildInflate() {
        if (demoMode || !Styles.use3DBuildings) return
        inflateStart = js("performance.now()") as Double
        window.requestAnimationFrame { stepInflate() }
    }

    private fun stepInflate() {
        val t = (((js("performance.now()") as Double) - inflateStart) / BUILD_INFLATE_MS).coerceIn(0.0, 1.0)
        applyBuildInflate(easeOutCubic(t))
        if (t < 1.0) window.requestAnimationFrame { stepInflate() }
    }

    // --- IN-GAME: buildings grow IN STEP with world-gen progress, only reaching full height once gen is
    // finished (portals + agents + NPCs all created) — so the city keeps rising while people drop in.
    private const val BUILD_EASE = 0.12 // how fast the shown height eases toward the live gen-progress target
    private const val BUILD_DELAY_FRAC = 0.15 // keep the ground empty for the first part of gen, THEN raise the city
    private const val BUILD_RISE_SPAN = 0.45 // …and finish the rise within this span of gen (snappier than the full build)
    private var buildTarget = 0.0
    private var buildShown = 0.0
    private var buildLoopRunning = false

    /** Drive the building grow-in by world-gen progress (0..1, from LoadingOverlay). Eases up toward
     *  [fraction] (after a short delay so the bare ground reads first); only snaps to full when gen
     *  reports done — so growth spans the whole build. */
    fun setBuildProgress(fraction: Double) {
        if (demoMode || !Styles.use3DBuildings) return
        // Delay the rise: stay flat until gen passes BUILD_DELAY_FRAC, then grow (quickly) over BUILD_RISE_SPAN.
        buildTarget = ((fraction.coerceIn(0.0, 1.0) - BUILD_DELAY_FRAC) / BUILD_RISE_SPAN).coerceIn(0.0, 1.0)
        if (!buildLoopRunning && buildTarget > 0.0) {
            buildLoopRunning = true
            window.requestAnimationFrame { stepBuildGrow() }
        }
    }

    private fun stepBuildGrow() {
        if (initMap == null) {
            buildLoopRunning = false
            return
        }
        buildShown += (buildTarget - buildShown) * BUILD_EASE
        applyBuildInflate(easeOutCubic(buildShown))
        if (buildShown < 0.999 || buildTarget < 1.0) {
            window.requestAnimationFrame { stepBuildGrow() }
        } else {
            applyBuildInflate(1.0)
            buildLoopRunning = false
        }
    }

    private fun easeOutCubic(t: Double) = 1.0 - (1.0 - t) * (1.0 - t) * (1.0 - t)

    /** Set the 3D buildings to [factor]×full height (0 = flat, 1 = full), incl. the blast-bob term. No-op
     *  without the building layer (demo style / not yet added). */
    fun applyBuildInflate(factor: Double) {
        val m = initMap ?: return
        if (demoMode || !Styles.use3DBuildings) return
        if (OwnBuildings.REPLACE_BUILDINGS) OwnBuildings.setInflate(factor) // grow our own meshes (the visible ones)
        if (m.asDynamic().getLayer("3d-buildings") == null) return
        m.setPaintProperty("3d-buildings", "fill-extrusion-height", inflateExpr(factor, "render_height", 8))
        m.setPaintProperty("3d-buildings", "fill-extrusion-base", inflateExpr(factor, "render_min_height", 0))
    }

    // Inflate factor × the real height, PLUS the per-building blast-bob (feature-state, see BuildingShake).
    private fun inflateExpr(factor: Double, prop: String, fallback: Int): Json =
        JSON.parse("""["+", ["*", $factor, ["coalesce", ["get", "$prop"], $fallback]], ${BuildingShake.SHAKE_TERM}]""")

    private fun spinBuild() {
        if (!cinematicActive) return
        initMap?.let { it.setBearing(it.getBearing() + BUILD_SPIN_DEG) }
        window.requestAnimationFrame { spinBuild() }
    }

    /** Stop the build orbit and settle to the Home view (top-down over the play area). */
    fun stopBuildCinematicAndHome() {
        cinematicActive = false
        goHome(INITIAL_HOME_MS) // a gentle, unhurried first settle (the Home button stays snappy at the default)
        // Auto-cam is on by default: start the drift once the (slower) initial flight has settled.
        if (autoCamActive) window.setTimeout({ autoCamLeg(autoCamGen) }, INITIAL_HOME_MS + 300)
    }

    private const val INITIAL_HOME_MS = 2600 // the first fly-to-Home when a game starts (slow; was an abrupt 900)

    private const val TITLE_FLYIN_MS = 5200.0 // dramatic swoop-in to the title location (slow)
    private const val TITLE_FLYIN_ZOOM_OUT = 4 // start this many zoom levels above the framing zoom
    private const val TITLE_COLOR_FADE_MS = 20000.0 // grayscale → colour over ~20s on the title (vs 30s in-game)
    private const val TITLE_LEG_MS = 10400.0 // duration of each randomized camera leg (slow, ~half speed)
    private var titleOrbitActive = false
    private var titleOrbitGen = 0

    /** Title scene: 3D terrain, a dramatic fly-in to the location, fast colour fade, then a flowing
     *  randomized camera (chained eased legs through random bearing/pitch/zoom — a spline-ish drift). */
    fun startTitleCinematic() {
        val m = initMap ?: return
        // Block map input during the swoop-in: an early click/drag cancels the flyTo and leaves the map
        // stuck zoomed out. Re-enabled when the fly-in lands (startTitleLeg) — also gates early blasts.
        m.asDynamic().getCanvasContainer().style.pointerEvents = "none"
        applyTerrain(m) // DEM relief (the demo style now carries the terrain source)
        Scene3D.onTerrainChanged() // sample heights so the portals sit on the terrain
        m.asDynamic().setZoom(titleZoom() - TITLE_FLYIN_ZOOM_OUT) // start high + top-down … (fractional zoom)
        m.setPitch(0.0)
        val fly: dynamic = js("({})")
        fly.zoom = titleZoom()
        fly.pitch = TITLE_PITCH
        fly.duration = TITLE_FLYIN_MS
        m.asDynamic().flyTo(fly) // … swoop down into the location
        fadeInColor(TITLE_COLOR_FADE_MS)
        startBuildInflate() // the city rises while we fly in
        titleOrbitActive = true
        window.setTimeout({ startTitleLeg() }, TITLE_FLYIN_MS.toInt()) // drift once the swoop settles
        // A user zoom cancels the running easeTo — restart the drift the moment they finish, so the
        // title auto-cam never stalls (originalEvent ⇒ user move; the orbit's own easeTo is ignored).
        m.onEvent("moveend") { e -> if (titleOrbitActive && e.originalEvent != null) startTitleLeg() }
    }

    private fun startTitleLeg() {
        // Fly-in has landed → hand input back (free-look + the blast mini-game; idempotent on re-entry).
        initMap?.let { it.asDynamic().getCanvasContainer().style.pointerEvents = "" }
        titleOrbitGen++ // invalidate any in-flight chain so we don't end up with two overlapping orbits
        titleOrbitLeg(titleOrbitGen)
    }

    private fun titleZoom() = displayZoomForSize() + TITLE_ZOOM_BOOST

    // One randomized camera leg: keep the centre on the action (so portals stay in view) and ease to a
    // new yaw/pitch/zoom, then chain another → a flowing orbit around the arena. (MapLibre has no camera
    // roll; yaw = bearing, pitch = tilt. To also fly the camera *position* while facing centre we'd need
    // FreeCamera — a follow-up.)
    private fun titleOrbitLeg(gen: Int) {
        if (!titleOrbitActive || gen != titleOrbitGen) return
        val m = initMap ?: return
        val turn = (50.0 + Rng.random() * 130.0) * (if (Rng.randomBool()) 1.0 else -1.0)
        val opts: dynamic = js("({})")
        opts.center = anchorCenter // hold the centre on the action area → portals stay framed
        opts.bearing = m.getBearing() + turn
        opts.pitch = TITLE_PITCH - 8.0 + Rng.random() * 16.0 // gentle tilt variation around TITLE_PITCH
        // Drift gently around the CURRENT zoom (clamped) so a player can scroll out without the orbit
        // snapping back — i.e. the auto-cam keeps running through a manual zoom.
        opts.zoom = (m.getZoom() + (Rng.random() * 0.5 - 0.25)).coerceIn(titleZoom() - 4.0, titleZoom() + 1.5)
        opts.duration = TITLE_LEG_MS
        m.asDynamic().easeTo(opts)
        window.setTimeout({ titleOrbitLeg(gen) }, TITLE_LEG_MS.toInt())
    }

    fun stopTitleOrbit() {
        titleOrbitActive = false
    }

    private const val AUTOCAM_LEG_MS = 27000.0 // in-game auto-cam leg — ~2× slower than the title, then ~30% slower again
    private const val AUTOCAM_PITCH = 42.0 // a bit more top-down than DEFAULT_PITCH so the action stays framed
    private const val AUTOCAM_ZOOM_LO = 0.4 // can pull a touch wider than the framed zoom…
    private const val AUTOCAM_ZOOM_HI = 1.2 // …or push a little closer in (still keeping the action in view)
    private var autoCamActive = false
    private var autoCamGen = 0 // bumped on every on/off so a stale chained leg can't keep running

    /** Notified whenever the auto-cam turns on/off — incl. when a manual move cancels it (UI syncs the toggle). */
    var onAutoCamChanged: ((Boolean) -> Unit)? = null

    fun isAutoCamOn() = autoCamActive

    /** Toggle the in-game auto-cam. On → start the slow cinematic drift; off → it settles where it is. */
    fun setAutoCam(on: Boolean) {
        if (on == autoCamActive) return
        autoCamActive = on
        autoCamGen++
        if (on) {
            cinematicActive = false // don't fight the build spin (if somehow still running)
            autoCamLeg(autoCamGen)
        }
        onAutoCamChanged?.invoke(on)
    }

    // User grabbed the camera (pan/rotate/tilt) → drop the drift + snap the toggle out (zoom is exempt).
    private fun cancelAutoCamFromUser() {
        if (autoCamActive) setAutoCam(false)
    }

    // One in-game auto-cam leg: like the title drift but slower/wider — hold the play-area centre framed,
    // ease to a new yaw/pitch/zoom, chain another. Wall-clock (setTimeout/easeTo) → sim-speed-independent;
    // drives both maps like goHome; [gen] guards a stale chain (toggled off→on) outliving its turn.
    private fun autoCamLeg(gen: Int) {
        if (!autoCamActive || gen != autoCamGen) return
        val center = anchorCenter ?: return
        val ref = referenceMap() ?: return
        val turn = (50.0 + Rng.random() * 130.0) * (if (Rng.randomBool()) 1.0 else -1.0)
        val opts: dynamic = js("({})")
        opts.center = center // keep the action framed (no fly-in to detail like the title does)
        opts.bearing = ref.getBearing() + turn
        opts.pitch = AUTOCAM_PITCH - 6.0 + Rng.random() * 12.0 // gentle tilt variation
        // a bit wider … to a little closer than the framed zoom
        opts.zoom = displayZoom() - AUTOCAM_ZOOM_LO + Rng.random() * (AUTOCAM_ZOOM_LO + AUTOCAM_ZOOM_HI)
        opts.duration = AUTOCAM_LEG_MS
        initMap?.asDynamic()?.easeTo(opts)
        map?.asDynamic()?.easeTo(opts)
        window.setTimeout({ autoCamLeg(gen) }, AUTOCAM_LEG_MS.toInt())
    }

    private var ownBuildingsHooked = false
    private const val OWN_BUILD_RETRY_MS = 500.0 // poll terrain readiness this often before loading
    private const val OWN_BUILD_MAX_RETRY = 16 // …up to ~8 s (DEM tiles can be slow)

    /**
     * Load a COMPLETE set of play-area building footprints by fetching + decoding the OpenFreeMap `.pbf`
     * vector tiles ourselves ([BuildingTiles]) — MapLibre's query APIs only ever returned a fraction.
     * Then seed debris colliders from them, and — when [OwnBuildings.REPLACE_BUILDINGS] is on — mesh our
     * OWN buildings and hide MapLibre's fill-extrusion layer (opacity 0). Deterministic: we know exactly
     * which tiles cover the area, so there's no wait-and-retry on tile streaming (only on terrain, which
     * we need for correct building base z).
     */
    fun buildBuildingColliders() {
        if (demoMode || !Styles.use3DBuildings || ownBuildingsHooked) return
        ownBuildingsHooked = true
        loadOwnBuildings(0)
        // Stream buildings for wherever the camera goes (auto-cam drift, title orbit, manual pan).
        initMap?.let { BuildingStream.attach(it) }
    }

    private fun loadOwnBuildings(attempt: Int) {
        if (demoMode || !Styles.use3DBuildings) return
        val center = anchorCenter ?: return
        if (!Scene3D.terrainReady()) { // need terrain for correct building base z; poll briefly
            if (attempt < OWN_BUILD_MAX_RETRY) {
                window.setTimeout({ loadOwnBuildings(attempt + 1) }, OWN_BUILD_RETRY_MS.toInt())
            }
            return
        }
        val halfW = Sim.width / 2.0 * Scene3D.metersPerPixel
        val halfH = Sim.height / 2.0 * Scene3D.metersPerPixel
        BuildingTiles.load(center.lng as Double, center.lat as Double, halfW, halfH) { feats ->
            val n = (feats.length as? Int) ?: 0
            Scene3D.buildBuildingColliders(feats) // debris colliders from the full set
            // Only take over the look if OSM actually returned buildings (Overpass can rate-limit / fail);
            // otherwise leave MapLibre's own fill-extrusion visible as the fallback.
            if (OwnBuildings.REPLACE_BUILDINGS && n > 0) {
                OwnBuildings.addFeatures(feats) // new meshes pop in at the current grow-in level (applyBuildInflate)
                // Full-replacement only: hide MapLibre's extrusion so OUR meshes take over the look. In
                // PARALLEL_MODE we keep MapLibre visible everywhere (no gaps) and our meshes are shadow-only.
                if (!OwnBuildings.PARALLEL_MODE) {
                    val md = initMap?.asDynamic()
                    if (md != null && md.getLayer("3d-buildings") != null) {
                        md.setPaintProperty("3d-buildings", "fill-extrusion-opacity", 0)
                    }
                }
            }
        }
    }

    /** Register the 3D scene (three.js custom layer) on the base map, anchored at the grid view. */
    fun enable3D() {
        val targetMap = initMap ?: return
        val center = anchorCenter ?: return
        Scene3D.register(targetMap, center.lng as Double, center.lat as Double, anchorZoom)
        BuildingShake.attach(targetMap.asDynamic()) // buildings bob when XMPs detonate nearby
        setupNavigation(targetMap)
        MapControls.lift(INITIAL_MAP) // float the zoom/compass + ⓘ above the footer (escape #initialMap's stacking context)
    }

    /**
     * Hand navigation to MapLibre's own (standard) handlers: left-drag pans, right-drag rotates
     * AND tilts, wheel zooms, all unrestricted — plus a NavigationControl block (zoom + compass +
     * pitch visualiser). The 3D scene is a custom layer glued to this map, so it follows for free.
     */
    private fun setupNavigation(m: MapLibre.Map) {
        m.setMaxPitch(MAX_PITCH)
        val opts: dynamic = js("({ visualizePitch: true, showCompass: true, showZoom: true })")
        // Bottom-right keeps it clear of the top control bar.
        m.addControl(MapLibre.NavigationControl(opts), "bottom-right")
    }

    /** Wire map-native click/hover (only after [enable3D]); MapLibre tells click from drag for us. */
    fun bindInteractions(onClick: (dynamic) -> Unit, onMove: (dynamic) -> Unit) {
        val m = initMap ?: return
        m.onEvent("click", onClick)
        m.onEvent("mousemove", onMove)
        // originalEvent is present only for user moves → the cam's own easeTo won't self-cancel (zoom unbound).
        val cancelOnUser = { event: dynamic -> if (event.originalEvent != null) cancelAutoCamFromUser() }
        listOf("dragstart", "rotatestart", "pitchstart").forEach { m.onEvent(it, cancelOnUser) }
    }

    /** Wire just a map click (demo scenes); MapLibre distinguishes a click from a nav drag. */
    fun bindClick(onClick: (dynamic) -> Unit) {
        initMap?.onEvent("click", onClick)
    }

    /** Title mini-game: LMB blast + RMB ultra-strike on the title map (RMB suppresses the context menu). */
    fun bindTitleBlasts(onLmb: (dynamic) -> Unit, onRmb: (dynamic) -> Unit) {
        val m = initMap ?: return
        m.onEvent("click", onLmb)
        m.onEvent("contextmenu") { event ->
            event.preventDefault()
            onRmb(event)
        }
    }

    /** Portal demo: LMB places, RMB removes (suppressing the browser context menu). */
    fun bindPortalDemo(onPlace: (dynamic) -> Unit, onRemove: (dynamic) -> Unit, onMove: (dynamic) -> Unit) {
        val m = initMap ?: return
        m.onEvent("click", onPlace)
        m.onEvent("mousemove", onMove)
        m.onEvent("contextmenu") { event ->
            event.preventDefault()
            onRemove(event)
        }
    }

    /** Ground sim position under a MapLibre mouse event (uses its lng/lat — pitch/zoom safe). */
    fun eventToSimPos(event: dynamic): Pos? {
        val lngLat = event.lngLat ?: return null
        return Scene3D.lngLatToSimPos(lngLat.lng as Double, lngLat.lat as Double)
    }

    fun loadMaps(center: Json, demo: Boolean = false, callback: (Grid) -> Unit) {
        demoMode = demo
        if (!demo) LoadingOverlay.stage(LoadingOverlay.PCT_MAP, "Loading map…")
        document.getElementById(MAP)?.addClass(INVISIBLE)
        document.getElementById(SHADOW_MAP)?.addClass(INVISIBLE)
        loadInitialMap(center, fun(initMap: MapLibre.Map) {
            loadMap(initMap, callback)
        })
    }

    private var demoBuildingsAdded = false

    /**
     * Demo scenes only: toggle the (hidden-by-default) satellite raster over the gray backdrop.
     * With satellite on we also bring up the 3D buildings (beneath the custom 3D layer) for depth.
     */
    fun setDemoSatellite(on: Boolean) {
        val m = initMap ?: return
        m.setLayoutProperty("satellite", "visibility", if (on) "visible" else "none")
        if (on && !demoBuildingsAdded) {
            m.addLayer(buildingLayerConfig(), Scene3D.CUSTOM_LAYER_ID) // under the portals/agents
            demoBuildingsAdded = true
        } else if (demoBuildingsAdded) {
            m.setLayoutProperty("3d-buildings", "visibility", if (on) "visible" else "none")
        }
    }

    private fun loadInitialMap(center: Json, callback: (MapLibre.Map) -> Unit) {
        document.getElementById(INITIAL_MAP)?.removeClass(INVISIBLE)
        fun addLayers(targetMap: MapLibre.Map) {
            if (!demoMode && Styles.use3DBuildings) { // demos use the bare gray style, no buildings
                targetMap.addLayer(buildingLayerConfig())
            }
            if (!demoMode) targetMap.setPaintProperty("satellite", "raster-saturation", -1.0) // grayscale during world-gen
            if (!demoMode) applyTerrain(targetMap) // 3D relief from the DEM source
            if (!demoMode) applySky(targetMap) // atmospheric skybox above the horizon when pitched
        }
        val existing = initMap
        if (existing == null) {
            val newMap = initInitialMapbox()
            initMap = newMap
            newMap.on("load") {
                addLayers(newMap)
                callback(newMap)
            }
            newMap.setMinZoom(MIN_ZOOM)
            newMap.setMaxZoom(MAX_ZOOM)
            newMap.setZoom(displayZoom())
            newMap.setCenter(center)
            newMap.setPitch(DEFAULT_PITCH)
        } else {
            existing.on("moveend") {
                addLayers(existing)
                callback(existing)
            }
            val options: Json = JSON.parse("""{"center": [$center], "zoom": ${displayZoom()}}""")
            existing.jumpTo(options)
        }
    }

    // https://maplibre.org/maplibre-gl-js/docs/API/
    private fun loadMap(initMap: MapLibre.Map, callback: (Grid) -> Unit) {
        if (!demoMode) LoadingOverlay.stage(LoadingOverlay.PCT_STREET, "Loading street tiles…")
        val center = initMap.getCenter()
        document.getElementById(MAP)?.removeClass(INVISIBLE)
        val existing = map
        if (existing == null) {
            val newMap = initMapbox()
            map = newMap
            newMap.on("load", fun() {
                loadShadowMap(center, callback)
            })
            val geoCtrl: dynamic = js("({})")
            geoCtrl.positionOptions = js("({enableHighAccuracy: true})")
            geoCtrl.trackUserLocation = false
            newMap.addControl(MapLibre.GeolocateControl(geoCtrl))
            newMap.setMinZoom(MIN_ZOOM)
            newMap.setMaxZoom(MAX_ZOOM)
            newMap.setZoom(displayZoom())
            newMap.setCenter(center)
            newMap.setPitch(DEFAULT_PITCH)
        } else {
            existing.on("moveend", fun() {
                loadShadowMap(center, callback)
            })
            val lng = center["lng"]
            val lat = center["lat"]
            val options: Json = JSON.parse("""{"center": [$lng,$lat],"zoom": ${displayZoom()}}""")
            existing.jumpTo(options)
        }
    }

    private fun loadShadowMap(center: Json, callback: (Grid) -> Unit) {
        if (!demoMode) LoadingOverlay.stage(LoadingOverlay.PCT_SHADOW, "Rendering passability map…")
        document.getElementById(SHADOW_MAP)?.remove()
        val div = document.createElement("div") as HTMLDivElement
        div.id = SHADOW_MAP
        div.addClass(SHADOW_MAP, "top")
        // Render the shadow map over the whole Sim area (SCALE× the screen) at full ZOOM detail,
        // so the readback grid covers the larger play area.
        div.style.width = "${Sim.width}px"
        div.style.height = "${Sim.height}px"
        document.body?.append(div)
        val newMap = initShadowMap()
        shadowMap = newMap
        // Wait for 'idle' (all tiles loaded AND fully rendered), not 'load',
        // before reading pixels — otherwise the street mask can be incomplete.
        newMap.once("idle", fun() {
            addGrid(callback)
        })
        newMap.setMinZoom(MIN_ZOOM)
        newMap.setMaxZoom(MAX_ZOOM)
        newMap.setZoom(ZOOM)
        newMap.setCenter(center)
    }

    private fun addGrid(callback: (Grid) -> Unit) {
        if (!demoMode) LoadingOverlay.stage(LoadingOverlay.PCT_GRID, "Reading street grid…")
        // Select the shadow map's own canvas robustly (query within its
        // container) instead of relying on a fragile global canvas index.
        val container = document.getElementById(SHADOW_MAP)
        val shadowMapCan: dynamic = container?.asDynamic()?.querySelector("canvas.maplibregl-canvas")
        // MapLibre renders with WebGL2; fall back to WebGL1 just in case.
        val gl: dynamic = shadowMapCan.getContext("webgl2") ?: shadowMapCan.getContext("webgl")
        val width = gl.canvas.width as Int
        val height = gl.canvas.height as Int
        val rawBuf = Uint8Array((width * height * 4))
        gl.readPixels(0, 0, width, height, gl.RGBA, gl.UNSIGNED_BYTE, rawBuf)
        val imageData: ImageData = World.createStreetImage(rawBuf, width, height)
        if (!demoMode) LoadingOverlay.detail("Tracing roads, water & terrain…")
        val grid = ShadowGridBuilder.build(imageData, width, height)
        if (!demoMode) LoadingOverlay.detail("Walkable ground: ${(World.walkability * 100).toInt()}% · reading place names…")
        shadowMap?.let { PortalNames.build(it) } // query POI/street names while the tiles are loaded
        teardownShadowMap() // grid + names are read — destroy the shadow map to free its WebGL context
        captureAnchor()
        callback(grid)
    }

    /**
     * The shadow map is only needed at build time (passability readback + POI/street names). Destroy
     * the MapLibre instance (frees its WebGL context — otherwise it leaked, since rebuilds only ever
     * dropped its DOM container) and remove the container. A later rebuild re-creates it fresh.
     */
    private fun teardownShadowMap() {
        shadowMap?.asDynamic()?.remove()
        shadowMap = null
        document.getElementById(SHADOW_MAP)?.remove()
    }

    // 3D building extrusions from the OpenFreeMap (openmaptiles) vector tiles.
    // openmaptiles exposes render_height / render_min_height on the building layer.
    private fun buildingLayerConfig(): Json = JSON.parse(
        """{
            "id": "3d-buildings",
            "source": "openmaptiles",
            "source-layer": "building",
            "type": "fill-extrusion",
            "minzoom": 14,
            "paint": {
                "fill-extrusion-color": "#333333",
                "fill-extrusion-height": ["+", ["coalesce", ["get", "render_height"], 8], ${BuildingShake.SHAKE_TERM}],
                "fill-extrusion-base": ["+", ["coalesce", ["get", "render_min_height"], 0], ${BuildingShake.SHAKE_TERM}],
                "fill-extrusion-opacity": 0.85
            }
        }""",
    )

    // Layer switching: #initialMap holds the satellite style, #map the street style.
    fun showSatellite() {
        document.getElementById(INITIAL_MAP)?.removeClass(INVISIBLE)
        document.getElementById(MAP)?.addClass(INVISIBLE)
        MapControls.setVisible(true) // the lifted controls drive #initialMap — show them with it
    }

    fun showStreet() {
        document.getElementById(INITIAL_MAP)?.addClass(INVISIBLE)
        document.getElementById(MAP)?.removeClass(INVISIBLE)
        MapControls.setVisible(false)
    }

    /**
     * Desaturate the satellite raster (terrain only — the 3D portals/agents render in a separate
     * custom layer and stay coloured). Grayscale is the build-time default; colour fades in once the
     * world is built ([fadeInColor]). The Menu "Colored map" toggle flips it instantly ([setColored]).
     */
    fun setGrayscale(on: Boolean) = setRasterSaturation(if (on) -1.0 else 0.0)

    private fun setRasterSaturation(sat: Double) {
        initMap?.setPaintProperty("satellite", "raster-saturation", sat)
    }

    private var terrainEnabled = true

    /** Drape the satellite over the DEM (3D relief). [on] false flattens back to today's map. */
    // A clean daytime sky with a cool sci-fi tint: deep blue overhead, pale cyan horizon, soft fog at
    // ground level so the terrain melts into the haze rather than ending on a hard line.
    private fun applySky(map: MapLibre.Map) {
        val sky: dynamic = js("({})")
        sky["sky-color"] = MapStyles.SKY_COLOR
        sky["sky-horizon-blend"] = 0.6
        sky["horizon-color"] = MapStyles.SKY_HORIZON_COLOR
        sky["horizon-fog-blend"] = 0.5
        sky["fog-color"] = MapStyles.SKY_FOG_COLOR
        sky["fog-ground-blend"] = 0.4
        map.setSky(sky)
    }

    private fun applyTerrain(map: MapLibre.Map) {
        val opts: dynamic = if (terrainEnabled) js("({})") else null
        if (opts != null) {
            opts.source = MapStyles.TERRAIN_SOURCE
            opts.exaggeration = MapStyles.TERRAIN_EXAGGERATION
        }
        map.asDynamic().setTerrain(opts)
    }

    fun isTerrainEnabled() = terrainEnabled

    /** Menu toggle: turn the 3D relief on/off (and re-sample the height grid that objects sit on). */
    fun setTerrainEnabled(on: Boolean) {
        terrainEnabled = on
        initMap?.let { applyTerrain(it) }
        Scene3D.onTerrainChanged()
    }

    private const val COLOR_FADE_MS = 30000.0 // colour eases in over 30 s after world-gen completes
    private var fadeActive = false
    private var fadeStart = 0.0
    private var fadeDurationMs = COLOR_FADE_MS

    /** Instantly set colour vs grayscale (the Menu toggle); cancels any in-progress fade. */
    fun setColored(on: Boolean) {
        fadeActive = false
        setRasterSaturation(if (on) 0.0 else -1.0)
    }

    /** Ease the terrain from grayscale → full colour over [durationMs] (call once world-gen is done). */
    fun fadeInColor(durationMs: Double = COLOR_FADE_MS) {
        fadeDurationMs = durationMs
        fadeStart = window.asDynamic().performance.now() as Double
        fadeActive = true
        window.requestAnimationFrame { stepFade(it) }
    }

    private fun stepFade(@Suppress("UNUSED_PARAMETER") t: Double) {
        if (!fadeActive) return
        val now = window.asDynamic().performance.now() as Double
        val p = ((now - fadeStart) / fadeDurationMs).coerceIn(0.0, 1.0)
        setRasterSaturation(-1.0 + p) // -1 (gray) → 0 (full colour)
        if (p < 1.0) window.requestAnimationFrame { stepFade(it) } else fadeActive = false
    }

    /** Fade the 3D buildings (0 = invisible … 1 = solid) so crowded areas don't hide the action. */
    fun setBuildingOpacity(opacity: Double) {
        buildingOpacity = opacity.coerceIn(0.0, 1.0)
        initMap?.setPaintProperty("3d-buildings", "fill-extrusion-opacity", buildingOpacity)
    }

    private var buildingOpacity = 0.85
    fun currentBuildingOpacity() = buildingOpacity

    /** Nudge building transparency by [delta] (keyboard -/+); clamped to 0..1. */
    fun nudgeBuildingOpacity(delta: Double) = setBuildingOpacity(buildingOpacity + delta)
}
