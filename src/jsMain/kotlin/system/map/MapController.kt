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
import util.Profiler
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

    private fun initInitialMap(): MapLibre.Map {
        val style = if (demoMode) MapStyles.DEMO_STYLE else MapStyles.SATELLITE_STYLE
        return MapLibre.Map(mapOptions(INITIAL_MAP, JSON.parse<Json>(style), false))
    }

    private fun initMainMap(): MapLibre.Map = MapLibre.Map(mapOptions(MAP, MapStyles.STREET_STYLE_URL, false))

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
    internal fun displayZoomForSize() = (ZOOM - log2(Sim.scale)).roundToInt()
    private const val DEMO_ZOOM = 19 // demos frame one central object, so sit closer than the game
    internal var demoMode = false
    internal fun displayZoom() = if (demoMode) DEMO_ZOOM else displayZoomForSize()
    private const val DEFAULT_PITCH = 50.0 // tilt the visible maps so the 3D scene reads as 3D
    internal const val MAX_PITCH = 85.0 // MapLibre's ceiling; lets you tilt to near-horizon (and to 0 = top-down)

    // The map's INITIAL zoom/pitch: for the title, the fly-in's zoomed-out start + top-down pitch, so the close
    // framing never renders first (no zoom-jump flash); otherwise the framed display zoom + the 3D-reading tilt.
    private fun initialZoom(): Double = if (titleIntroLoad) MapCamera.titleStartZoom() else displayZoom().toDouble()
    private fun initialPitch(): Double = if (titleIntroLoad) 0.0 else DEFAULT_PITCH

    internal var map: MapLibre.Map? = null
    internal var initMap: MapLibre.Map? = null
    private var shadowMap: MapLibre.Map? = null

    // Camera-follow anchor: the simulation lives in a fixed pixel space built at
    // this map view. We transform the canvas layer to follow the live camera. ([MapCamera] reads these.)
    internal var anchorCenter: dynamic = null
    internal var anchorZoom = 0.0

    internal fun referenceMap(): MapLibre.Map? = initMap ?: map

    /** Capture the view the grid/sim was built at (call once the grid is ready). */
    private fun captureAnchor() {
        val m = referenceMap() ?: return
        anchorCenter = m.getCenter()
        anchorZoom = ZOOM.toDouble() // grid is read at ZOOM regardless of the (framed) display zoom
    }

    private const val BUILD_INFLATE_MS = 2800.0 // TITLE-only fixed-duration rise (~3 s; title has no world-gen progress)
    private var inflateStart = 0.0

    /** TITLE: animate the 3D buildings rising over a fixed duration (no loading overlay there). [MapCamera]
     *  kicks this off as the title swoops in. */
    internal fun startBuildInflate() {
        if (demoMode || !Styles.use3DBuildings) return
        inflateStart = js("performance.now()") as Double
        window.requestAnimationFrame { stepInflate() }
    }

    private fun stepInflate() {
        val t = (((js("performance.now()") as Double) - inflateStart) / BUILD_INFLATE_MS).coerceIn(0.0, 1.0)
        applyBuildInflate(easeOutCubic(t))
        if (t < 1.0) window.requestAnimationFrame { stepInflate() }
    }

    // --- IN-GAME: buildings grow IN STEP with the overall load (LoadingOverlay drives the 0..1 fraction,
    // delayed start → full at 100%), only reaching full height as the game starts — so the city keeps rising
    // across the whole build instead of finishing early.
    // How fast the shown height eases toward the gen-progress target. Low on purpose: it's a duration FLOOR —
    // even if the spawn bar jumps to 100 (few portals/NPCs), the city still rises gradually (~2 s) rather than
    // snapping up, so the growth reads as a slow build, not a pop.
    private const val BUILD_EASE = 0.04
    private var buildTarget = 0.0
    private var buildShown = 0.0
    private var buildLoopRunning = false

    /** Drive the building grow-in by overall load progress (0..1, from LoadingOverlay, which already shapes the
     *  delayed-start/full-at-100% curve). Eases the shown height toward [fraction]. */
    fun setBuildProgress(fraction: Double) {
        if (demoMode || !Styles.use3DBuildings) return
        buildTarget = fraction.coerceIn(0.0, 1.0)
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
        // Per-building replacement: each time the view settles, hide the MapLibre footprints we've meshed (new
        // tiles bring fresh generateId ids, so it must re-run on every idle — it's idempotent + gated internally).
        initMap?.let { m ->
            m.asDynamic().on("idle", fun() {
                hideMeshedMapLibreBuildings()
            })
        }
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
        val cancelOnUser = { event: dynamic -> if (event.originalEvent != null) MapCamera.cancelAutoCamFromUser() }
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

    // The title initialises its map already zoomed-out (the fly-in start) so the close framing never renders
    // first → no zoom-jump flash before the swoop. Set by [loadMaps], read by [loadInitialMap].
    private var titleIntroLoad = false

    fun loadMaps(center: Json, demo: Boolean = false, titleIntro: Boolean = false, callback: (Grid) -> Unit) {
        demoMode = demo
        titleIntroLoad = titleIntro
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
            val newMap = initInitialMap()
            initMap = newMap
            newMap.on("load") {
                addLayers(newMap)
                callback(newMap)
            }
            newMap.setMinZoom(MIN_ZOOM)
            newMap.setMaxZoom(MAX_ZOOM)
            newMap.asDynamic().setZoom(initialZoom()) // fractional title start zoom (the typed binding wants Int)
            newMap.setCenter(center)
            newMap.setPitch(initialPitch())
        } else {
            existing.on("moveend") {
                addLayers(existing)
                callback(existing)
            }
            val options: Json = JSON.parse("""{"center": [$center], "zoom": ${initialZoom()}, "pitch": ${initialPitch()}}""")
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
            val newMap = initMainMap()
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
        val grid = Profiler.time("grid-build") { ShadowGridBuilder.build(imageData, width, height) }
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
                "fill-extrusion-color": ["case", ["boolean", ["feature-state", "hidden"], false], "rgba(0,0,0,0)", "#333333"],
                "fill-extrusion-height": ["+", ["coalesce", ["get", "render_height"], 8], ${BuildingShake.SHAKE_TERM}],
                "fill-extrusion-base": ["+", ["coalesce", ["get", "render_min_height"], 0], ${BuildingShake.SHAKE_TERM}],
                "fill-extrusion-opacity": 0.85
            }
        }""",
    )

    // Per-building replacement: hide ONLY the MapLibre footprints we've meshed (centroid match), so our mesh is
    // the sole visual there while MapLibre fills the gaps. Re-run on every idle so newly-streamed tiles (whose
    // generateId feature ids are tile-local) get hidden too. Idempotent; a no-op unless PARALLEL + PER_BUILDING.
    fun hideMeshedMapLibreBuildings() {
        if (!OwnBuildings.PARALLEL_MODE || !OwnBuildings.PER_BUILDING_REPLACE) return
        val map = initMap?.asDynamic() ?: return
        if (map.getLayer("3d-buildings") == null) return
        val params: dynamic = js("({})")
        params.sourceLayer = "building"
        val feats = map.querySourceFeatures("openmaptiles", params)
        val n = (feats.length as? Int) ?: return
        var i = 0
        while (i < n) {
            val f = feats[i]
            i++
            // Need a (generateId) feature id to target with setFeatureState, and a centroid match to one of ours.
            if (f.id != null && OwnBuildings.coversGeometry(f.geometry)) {
                val ref: dynamic = js("({})")
                ref.source = "openmaptiles"
                ref.sourceLayer = "building"
                ref.id = f.id
                val state: dynamic = js("({})")
                state.hidden = true
                map.setFeatureState(ref, state)
            }
        }
    }

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

    internal fun applyTerrain(map: MapLibre.Map) {
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
        // fill-extrusion-opacity is layer-wide (no data expressions allowed); the per-building HIDE is done via a
        // feature-state-driven transparent fill-extrusion-COLOR instead, so the slider here stays a plain value.
        initMap?.setPaintProperty("3d-buildings", "fill-extrusion-opacity", buildingOpacity)
    }

    private var buildingOpacity = 0.85
    fun currentBuildingOpacity() = buildingOpacity

    /** Nudge building transparency by [delta] (keyboard -/+); clamped to 0..1. */
    fun nudgeBuildingOpacity(delta: Double) = setBuildingOpacity(buildingOpacity + delta)
}
