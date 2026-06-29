package system.map

import World
import kotlinx.browser.window
import system.display.Scene3D
import util.Rng

/**
 * Camera + cinematics for the map views, split out of [MapController] (the map hub). Owns every camera
 * motion — manual nav (rotate/pitch/pan/home/zoom), the world-build orbit, the title fly-in + drift, and
 * the in-game auto-cam — driving both visible maps in lockstep. Reads the map handles + framing helpers from
 * [MapController] (`internal`); calls back into it for terrain/colour/building-inflate side effects.
 */
object MapCamera {
    fun rotateBy(degrees: Double) {
        cancelAutoCamFromUser()
        MapController.initMap?.let { it.setBearing(it.getBearing() + degrees) }
        MapController.map?.let { it.setBearing(it.getBearing() + degrees) }
    }

    fun pitchBy(degrees: Double) {
        cancelAutoCamFromUser()
        MapController.initMap?.let { it.setPitch((it.getPitch() + degrees).coerceIn(0.0, MapController.MAX_PITCH)) }
        MapController.map?.let { it.setPitch((it.getPitch() + degrees).coerceIn(0.0, MapController.MAX_PITCH)) }
    }

    fun panBy(dx: Double, dy: Double) {
        cancelAutoCamFromUser()
        val opts: dynamic = js("({animate: false})")
        val offset = arrayOf(dx, dy)
        MapController.initMap?.panBy(offset, opts)
        MapController.map?.panBy(offset, opts)
    }

    /**
     * "Home": fly the camera back over the play area, framed and **top-down** (pitch 0, bearing 0) —
     * so a player who has panned/rotated away can instantly find the action again. Centers on the
     * grid anchor at the framed display zoom.
     */
    fun goHome(durationMs: Int = 900) {
        focusKey = null // Home drops any agent/portal focus → back to the play-area centre of attention
        followActive = false
        val center = MapController.anchorCenter ?: return
        val opts: dynamic = js("({ pitch: 0.0, bearing: 0.0 })")
        opts.duration = durationMs
        opts.center = center
        opts.zoom = MapController.displayZoom()
        opts.padding = js("({ top: 0.0, bottom: 0.0, left: 0.0, right: 0.0 })") // clear the build-time centre lift
        MapController.initMap?.asDynamic()?.flyTo(opts)
        MapController.map?.asDynamic()?.flyTo(opts)
    }

    /** Zoom the live map by [delta] levels with a short ease (keyboard zoom: PageUp/PageDown). */
    fun zoomBy(delta: Double) {
        val m = MapController.map ?: return
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
        // No inflate reset: the city has already begun rising at the reveal (PCT_GRID, before this orbit at
        // PCT_WORLD), so it keeps growing through the orbit to full at game start — the build's entertainment.
    }

    // The build camera keeps DEFAULT_PITCH for the 3D look, which pushes the play-area centre low on
    // screen. A bottom padding (viewport-relative, so it's stable under the bearing spin) lifts the
    // centre back up to the middle so the first flow-field vectors / portals read centre-frame.
    private fun liftViewToCentre() {
        if (MapController.demoMode) return
        val m = MapController.initMap ?: return
        val pad: dynamic = js("({ top: 0.0, left: 0.0, right: 0.0 })")
        pad.bottom = window.innerHeight * BUILD_CENTRE_LIFT_FRAC
        m.asDynamic().setPadding(pad)
    }

    private fun spinBuild() {
        if (!cinematicActive) return
        MapController.initMap?.let { it.setBearing(it.getBearing() + BUILD_SPIN_DEG) }
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

    private const val TITLE_ZOOM_BOOST = 0.4 // was 1.1 — pulled back (the title felt too close; the bigger Tiny arena lifts it further)
    private const val TITLE_PITCH = 35.0 // fairly top-down → the action sits just below screen centre (less sky/skyline)
    private const val TITLE_FLYIN_MS = 5200.0 // dramatic swoop-in to the title location (slow)
    private const val TITLE_FLYIN_ZOOM_OUT = 4 // start this many zoom levels above the framing zoom
    private const val TITLE_COLOR_FADE_MS = 20000.0 // grayscale → colour over ~20s on the title (vs 30s in-game)
    private const val TITLE_LEG_MS = 10400.0 // duration of each randomized camera leg (slow, ~half speed)
    private var titleOrbitActive = false
    private var titleOrbitGen = 0

    /** Title scene: 3D terrain, a dramatic fly-in to the location, fast colour fade, then a flowing
     *  randomized camera (chained eased legs through random bearing/pitch/zoom — a spline-ish drift). */
    fun startTitleCinematic() {
        val m = MapController.initMap ?: return
        // Block map input during the swoop-in: an early click/drag cancels the flyTo and leaves the map
        // stuck zoomed out. Re-enabled when the fly-in lands (startTitleLeg) — also gates early blasts.
        m.asDynamic().getCanvasContainer().style.pointerEvents = "none"
        MapController.applyTerrain(m) // DEM relief (the demo style now carries the terrain source)
        Scene3D.onTerrainChanged() // sample heights so the portals sit on the terrain
        m.asDynamic().setZoom(titleZoom() - TITLE_FLYIN_ZOOM_OUT) // start high + top-down … (fractional zoom)
        m.setPitch(0.0)
        val fly: dynamic = js("({})")
        fly.zoom = titleZoom()
        fly.pitch = TITLE_PITCH
        fly.duration = TITLE_FLYIN_MS
        m.asDynamic().flyTo(fly) // … swoop down into the location
        MapController.fadeInColor(TITLE_COLOR_FADE_MS)
        MapController.startBuildInflate() // the city rises while we fly in
        titleOrbitActive = true
        window.setTimeout({ startTitleLeg() }, TITLE_FLYIN_MS.toInt()) // drift once the swoop settles
        // A user zoom cancels the running easeTo — restart the drift the moment they finish, so the
        // title auto-cam never stalls (originalEvent ⇒ user move; the orbit's own easeTo is ignored).
        m.onEvent("moveend") { e -> if (titleOrbitActive && e.originalEvent != null) startTitleLeg() }
    }

    private fun startTitleLeg() {
        // Fly-in has landed → hand input back (free-look + the blast mini-game; idempotent on re-entry).
        MapController.initMap?.let { it.asDynamic().getCanvasContainer().style.pointerEvents = "" }
        titleOrbitGen++ // invalidate any in-flight chain so we don't end up with two overlapping orbits
        titleOrbitLeg(titleOrbitGen)
    }

    private fun titleZoom() = MapController.displayZoomForSize() + TITLE_ZOOM_BOOST

    /** The fly-in's STARTING zoom (well above the framing zoom). The title map is initialised here
     *  ([MapController.loadMaps] titleIntro) so it never renders the close framing first → no zoom-jump flash. */
    fun titleStartZoom(): Double = titleZoom() - TITLE_FLYIN_ZOOM_OUT

    // One randomized camera leg: keep the centre on the action (so portals stay in view) and ease to a
    // new yaw/pitch/zoom, then chain another → a flowing orbit around the arena. (MapLibre has no camera
    // roll; yaw = bearing, pitch = tilt. To also fly the camera *position* while facing centre we'd need
    // FreeCamera — a follow-up.)
    private fun titleOrbitLeg(gen: Int) {
        if (!titleOrbitActive || gen != titleOrbitGen) return
        val m = MapController.initMap ?: return
        val turn = (50.0 + Rng.random() * 130.0) * (if (Rng.randomBool()) 1.0 else -1.0)
        val opts: dynamic = js("({})")
        opts.center = MapController.anchorCenter // hold the centre on the action area → portals stay framed
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

    /**
     * Toggle the in-game auto-cam. On → start the slow cinematic drift around the current [focusCenter] (the
     * selected agent/portal, or the play-area centre when nothing is selected); off → it settles where it is, and
     * a focused entity is then tracked statically by [updateFollow]. The focus is KEPT across the toggle.
     */
    fun setAutoCam(on: Boolean) {
        if (on == autoCamActive) return
        autoCamActive = on
        autoCamGen++
        if (on) {
            cinematicActive = false // don't fight the build spin (if somehow still running)
            autoCamLeg(autoCamGen)
        } else if (focusKey != null) {
            followActive = true // drift off but still focused → the per-frame follow takes over the centring
        }
        onAutoCamChanged?.invoke(on)
    }

    // User grabbed the camera (pan/rotate/tilt) → drop the drift AND the focus (back to a free, play-area view).
    internal fun cancelAutoCamFromUser() {
        focusKey = null
        followActive = false
        if (autoCamActive) setAutoCam(false)
    }

    // --- Focus: the camera's "centre of attention". A selected agent/portal becomes the point the auto-cam ORBITS
    // (exactly as it orbits the play-area centre when nothing is selected); with the auto-cam OFF the camera simply
    // TRACKS it. Set by clicking a name in the AGENTS/PORTALS tables. ---
    private const val FOCUS_ZOOM_BOOST = 1.6 // cam IN closer than the framed overview when focusing a target
    private const val FOCUS_MS = 900 // the cam-in flight
    private var focusKey: String? = null // "agent:<name>" / "portal:<id>", or null = the play-area centre
    private var followActive = false // true once a cam-in lands → per-frame tracking may run (auto-cam OFF only)

    /** The current centre of attention as a map centre: the live position of the focused agent/portal, or the
     *  play-area anchor when nothing is focused (or the entity is gone — which also clears the stale focus). */
    private fun focusCenter(): dynamic {
        val key = focusKey ?: return MapController.anchorCenter
        val pos = when {
            key.startsWith("agent:") -> World.allAgents.firstOrNull { "agent:" + it.name == key }?.pos
            key.startsWith("portal:") -> World.allPortals.firstOrNull { "portal:" + it.id == key }?.location
            else -> null
        }
        if (pos == null) {
            focusKey = null // the entity vanished → fall back to the play-area centre
            return MapController.anchorCenter
        }
        val ll = Scene3D.simPosToLngLat(pos)
        return arrayOf(ll[0], ll[1])
    }

    /**
     * Make [id] ("agent:<name>" / "portal:<id>", or null to clear) the camera's centre of attention: cam in on it,
     * then EITHER keep the auto-cam orbiting it (if on) OR track it per-frame ([updateFollow], if off). A user
     * pan/rotate/tilt drops the focus ([cancelAutoCamFromUser]). Clicking a name in the HUD tables calls this.
     */
    fun focusOn(id: String?) {
        focusKey = id
        followActive = false
        stopCamera() // cancel any in-flight drift/fly before the cam-in
        val opts: dynamic = js("({})")
        opts.center = focusCenter()
        opts.zoom = MapController.displayZoom() + FOCUS_ZOOM_BOOST
        opts.duration = FOCUS_MS
        MapController.initMap?.asDynamic()?.flyTo(opts)
        MapController.map?.asDynamic()?.flyTo(opts)
        // After the cam-in lands: arm per-frame tracking, and (auto-cam on) resume the orbit around the new focus.
        window.setTimeout({ followActive = true }, FOCUS_MS)
        if (autoCamActive) {
            autoCamGen++ // invalidate the old chained leg (was orbiting the previous centre)
            val gen = autoCamGen
            window.setTimeout({ autoCamLeg(gen) }, FOCUS_MS)
        }
    }

    /**
     * Per-frame (from [system.ui.HudRenderer.redraw]): with the auto-cam OFF, keep both maps centred on the
     * focused agent/portal as it moves. No-op while the auto-cam is on (its legs do the centring) or nothing is
     * focused. A user gesture clears the focus first, so this stops the instant the player grabs the map.
     */
    fun updateFollow() {
        if (autoCamActive || !followActive || focusKey == null) return
        val center = focusCenter() // resolves the live position (clears + returns the anchor if the entity is gone)
        if (focusKey == null) return // vanished this very frame → don't yank a now-free view to the anchor
        MapController.initMap?.asDynamic()?.setCenter(center)
        MapController.map?.asDynamic()?.setCenter(center)
    }

    /**
     * Halt any in-flight camera animation (an auto-cam ease, a goHome fly, …) immediately. Turning the
     * auto-cam off only stops the *next* leg from chaining; the current [AUTOCAM_LEG_MS] (27 s) ease keeps
     * gliding. Pause calls this so the view freezes at once instead of drifting on for up to ~27 s.
     */
    fun stopCamera() {
        MapController.initMap?.asDynamic()?.stop()
        MapController.map?.asDynamic()?.stop()
    }

    // One in-game auto-cam leg: like the title drift but slower/wider — hold the play-area centre framed,
    // ease to a new yaw/pitch/zoom, chain another. Wall-clock (setTimeout/easeTo) → sim-speed-independent;
    // drives both maps like goHome; [gen] guards a stale chain (toggled off→on) outliving its turn.
    private fun autoCamLeg(gen: Int) {
        if (!autoCamActive || gen != autoCamGen) return
        val center = focusCenter() ?: return // orbit the selected agent/portal, or the play-area centre if none
        val ref = MapController.referenceMap() ?: return
        val turn = (50.0 + Rng.random() * 130.0) * (if (Rng.randomBool()) 1.0 else -1.0)
        val opts: dynamic = js("({})")
        opts.center = center // keep the action framed (no fly-in to detail like the title does)
        opts.bearing = ref.getBearing() + turn
        opts.pitch = AUTOCAM_PITCH - 6.0 + Rng.random() * 12.0 // gentle tilt variation
        // a bit wider … to a little closer than the framed zoom
        opts.zoom = MapController.displayZoom() - AUTOCAM_ZOOM_LO + Rng.random() * (AUTOCAM_ZOOM_LO + AUTOCAM_ZOOM_HI)
        opts.duration = AUTOCAM_LEG_MS
        MapController.initMap?.asDynamic()?.easeTo(opts)
        MapController.map?.asDynamic()?.easeTo(opts)
        window.setTimeout({ autoCamLeg(gen) }, AUTOCAM_LEG_MS.toInt())
    }
}
