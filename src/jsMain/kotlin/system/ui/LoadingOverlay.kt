package system.ui

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import system.map.MapController

/**
 * Full-screen DOM loading overlay shown from the very first frame, so the long world-build (map
 * tiles → shadow-map render → grid readback → flow fields → spawn) never looks frozen.
 *
 * Two bars, driven by one rAF loop so progress moves **steadily** rather than sliding-then-pausing:
 * - **overall** (large) — the global 0→100%; eased toward its target so it's always gliding.
 * - **sub-process** (small) — the CURRENT stage's own fill, 0→100% per stage type (map, street, shadow,
 *   grid, portals, people). For the spawn stages it's the real done/total; for the single-async-wait stages
 *   (no granular signal) it **creeps** toward ~90% and the next stage's start snaps the remainder.
 *
 * Tinted by the chosen faction ([setAccent]). At the world-build stage the backdrop goes translucent so the
 * spawning world (portals + their colour-coded flow vectors) shows through. The 3D buildings grow in step
 * with the overall bar ([driveBuildings]) so the city reaches full height exactly as the game starts.
 */
object LoadingOverlay {
    // Stage fill targets (%), in load order — kept here so Bootstrap + MapController share one scale.
    const val PCT_MAP = 12
    const val PCT_STREET = 22
    const val PCT_SHADOW = 38
    const val PCT_GRID = 60
    const val PCT_WORLD = 78 // portals begin
    const val PCT_PEOPLE = 92 // agents/NPCs begin

    // The ordered stage boundaries — used to find a stage's band end (the next boundary) so the overall bar
    // can creep across the band while a single-async-wait stage runs.
    private val STAGE_PCTS = intArrayOf(PCT_MAP, PCT_STREET, PCT_SHADOW, PCT_GRID, PCT_WORLD, PCT_PEOPLE, 100)

    private const val OVERLAY_ID = "loadingOverlay"
    private var statusEl: HTMLElement? = null
    private var detailEl: HTMLElement? = null
    private var fillEl: HTMLElement? = null // overall progress (large bar)
    private var subFillEl: HTMLElement? = null // current sub-process (small bar)

    // --- progress state (driven by the rAF loop) ---------------------------------------------------------
    private var bandStart = 0.0 // the current stage's overall-% band…
    private var bandEnd = PCT_MAP.toDouble() // …its end (next boundary)
    private var stageFrac = 0.0 // the current stage's internal progress, 0..1 (real for spawns, creeping for waits)
    private var indeterminate = true // creep stageFrac (a single async wait) vs. track a real done/total
    private var mainShown = START_PCT // the displayed overall % (eased toward its target each frame)
    private var subShown = 0.0
    private var animating = false

    private const val START_PCT = 4.0
    private const val CREEP_CEIL = 0.9 // a waiting stage creeps its fraction toward this (never quite reaching)
    private const val CREEP_EASE = 0.02 // …slowly, so a long wait keeps inching rather than stalling
    private const val MAIN_EASE = 0.14 // how fast the shown overall bar chases its target (snappy = real progress shows)
    private const val SUB_EASE = 0.22
    private const val FINISH_HOLD_MS = 380 // let the bar ease to 100 before the fade
    private const val FADE_MS = 600

    /** Build + show the overlay (call once, as early as possible). No-op if already shown. */
    fun show() {
        if (document.getElementById(OVERLAY_ID) != null) return
        val body = document.body ?: return
        val overlay = document.createElement("div") as HTMLElement
        overlay.id = OVERLAY_ID
        overlay.className = "loadingOverlay"
        // No Q-GRESS wordmark here — the player just came from the title; the build pane stays compact.
        val status = el("loadingStatus").also { it.textContent = "Starting…" }
        val track = el("loadingTrack")
        val fill = el("loadingFill").also { it.style.width = "$START_PCT%" }
        track.appendChild(fill)
        val subTrack = document.createElement("div") as HTMLElement
        subTrack.className = "loadingTrack loadingSubTrack"
        val subFill = el("loadingFill").also { it.style.width = "0%" }
        subTrack.appendChild(subFill)
        val detail = el("loadingDetail")
        // Glass pane behind the text + bars so they stay readable once the backdrop goes translucent.
        val panel = el("loadingPanel")
        listOf(status, track, subTrack, detail).forEach { panel.appendChild(it) }
        overlay.appendChild(panel)
        body.appendChild(overlay)
        statusEl = status
        detailEl = detail
        fillEl = fill
        subFillEl = subFill
        // Reset the animation state (a rebuild reuses the singleton) and start the steady-progress loop.
        bandStart = 0.0
        bandEnd = PCT_MAP.toDouble()
        stageFrac = 0.0
        indeterminate = true
        mainShown = START_PCT
        subShown = 0.0
        morphFromOnboarding(panel) // continue the onboarding glass morph across the reload (down to the footer)
        startLoop()
    }

    private fun el(cls: String): HTMLElement = (document.createElement("div") as HTMLElement).also { it.className = cls }

    /** Sub-status line for what's being created right now, e.g. "Creating portal X  (3/21)". */
    fun detail(text: String) {
        detailEl?.textContent = text
    }

    /**
     * Build-phase progress: the **overall** bar advances across a [from]→[to] band while the **sub** bar
     * tracks the real sub-process ([done]/[total]) — the spawn stages (portals, people) have granular signal,
     * so this is determinate (no creep).
     */
    fun building(from: Int, to: Int, done: Int, total: Int, label: String) {
        statusEl?.textContent = "Building world…"
        bandStart = from.toDouble()
        bandEnd = to.toDouble()
        stageFrac = if (total <= 0) 1.0 else (done.toDouble() / total).coerceIn(0.0, 1.0)
        indeterminate = false
        // Pad the count to the total's width with a digit-wide figure space (+ tabular figures in CSS) so the
        // centred line doesn't shimmy left/right a few px as the numbers change ("3/21" → "12/21").
        val padded = done.toString().padStart(total.toString().length, ' ') // U+2007 figure space (digit-wide)
        detail("$label  ($padded/$total)")
        reveal()
    }

    /** Advance to a labelled [text] stage starting at [percent]; its sub-bar creeps across the band to the
     *  next boundary while the (single) async step runs. */
    fun stage(percent: Int, text: String) {
        statusEl?.textContent = text
        bandStart = percent.toDouble()
        bandEnd = (STAGE_PCTS.firstOrNull { it > percent } ?: 100).toDouble()
        stageFrac = 0.0
        indeterminate = true
        subShown = 0.0
        // At the world-build stage, reveal the scene behind so the spawning portals + flow vectors show.
        if (percent >= PCT_WORLD) reveal()
    }

    private fun startLoop() {
        if (animating) return
        animating = true
        window.requestAnimationFrame { step() }
    }

    private fun step() {
        if (!animating) return
        if (indeterminate) stageFrac += (CREEP_CEIL - stageFrac) * CREEP_EASE // a waiting stage keeps inching forward
        val mainTarget = bandStart + (bandEnd - bandStart) * stageFrac
        mainShown += (mainTarget - mainShown) * MAIN_EASE
        subShown += (stageFrac * 100.0 - subShown) * SUB_EASE
        fillEl?.style?.width = pct(mainShown)
        subFillEl?.style?.width = pct(subShown)
        driveBuildings(mainShown)
        window.requestAnimationFrame { step() }
    }

    private fun pct(v: Double): String = "${(v * 10).toInt() / 10.0}%"

    // Grow the 3D buildings only across the VISIBLE world-build (after the reveal at PCT_WORLD → full at 100%),
    // so the rise is actually seen + spans the (slow, setTimeout-paced) spawn phase, and the heavy map/shadow
    // load earlier isn't slowed by per-frame MapLibre paint updates competing with the shadow-map readback.
    private fun driveBuildings(overallPercent: Double) {
        val frac = ((overallPercent - PCT_WORLD) / (100.0 - PCT_WORLD)).coerceIn(0.0, 1.0)
        MapController.setBuildProgress(frac)
    }

    private fun reveal() {
        (document.getElementById(OVERLAY_ID) as? HTMLElement)?.className = "loadingOverlay loadingOverlayReveal"
    }

    /** Tint the whole UI with the chosen faction colour (call once a faction is picked). Sets the
     *  global `--faction` CSS variable, so the loading bars/title + all faction-branded controls
     *  (sliders, checkboxes, button glows) update at once — and a player never sees the other faction's
     *  colour in the chrome. */
    fun setAccent(color: String) {
        (document.documentElement as? HTMLElement)?.style?.setProperty("--faction", color)
    }

    /** Fill to 100% and fade the overlay out, then remove it. */
    fun done() {
        bandStart = 100.0
        bandEnd = 100.0
        stageFrac = 1.0
        indeterminate = false
        driveBuildings(100.0) // city reaches full height exactly as world-gen completes
        // Let the loop ease the bar to 100 first, then stop it, snap, fade + remove.
        window.setTimeout({
            animating = false
            fillEl?.style?.width = "100%"
            subFillEl?.style?.width = "100%"
            val overlay = document.getElementById(OVERLAY_ID) as? HTMLElement ?: return@setTimeout
            overlay.className = "loadingOverlay loadingOverlayDone"
            statusEl = null
            detailEl = null
            fillEl = null
            subFillEl = null
            window.setTimeout({ overlay.remove() }, FADE_MS)
        }, FINISH_HOLD_MS)
    }

    // If the last onboarding step saved a footprint before its reload ([MorphPane.persistForReload]), fly THIS
    // panel from there down to its footer position — so the morph continues across the page reload. No-op for
    // an in-game Reset (no saved footprint), where the panel just sits at the footer with its normal glass.
    private fun morphFromOnboarding(panel: HTMLElement) {
        val from = MorphPane.consumeReloadFrom() ?: return
        val natural = panel.asDynamic().getBoundingClientRect() // its flex (footer) rect, measured before we pin it
        panel.classList.add("loadingPanelMorph")
        setRect(panel, from) // start on the onboarding panel's centre footprint…
        panel.asDynamic().offsetWidth // …commit that before the transition…
        window.requestAnimationFrame { setRect(panel, natural) } // …then glide down to the footer
    }

    private fun setRect(el: HTMLElement, r: dynamic) {
        el.style.left = "${r.left as Double}px"
        el.style.top = "${r.top as Double}px"
        el.style.width = "${r.width as Double}px"
        el.style.height = "${r.height as Double}px"
    }

    private val window get() = kotlinx.browser.window
}
