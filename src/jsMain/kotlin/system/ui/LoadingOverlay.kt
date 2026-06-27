package system.ui

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import system.map.MapUtil

/**
 * Full-screen DOM loading overlay shown from the very first frame, so the long world-build (map
 * tiles → shadow-map render → grid readback → flow fields → spawn) never looks frozen. Previously
 * the only progress was the canvas VectorBar, which appears ~halfway through (once portals spawn).
 *
 * Tinted by the chosen faction ([setAccent]) — the only colour, per the "faction colour for faction
 * things" rule. Staged: each [stage] sets a labelled step + a target fill %, with a shimmer so even
 * a long single stage reads as alive. At the world-build stage the backdrop goes translucent so the
 * spawning world (portals + their colour-coded flow vectors) shows through behind the progress.
 */
object LoadingOverlay {
    // Stage fill targets (%), in load order — kept here so HtmlUtil + MapUtil share one scale.
    const val PCT_MAP = 12
    const val PCT_STREET = 22
    const val PCT_SHADOW = 38
    const val PCT_GRID = 60
    const val PCT_WORLD = 78 // portals begin
    const val PCT_PEOPLE = 92 // agents/NPCs begin

    private const val OVERLAY_ID = "loadingOverlay"
    private var statusEl: HTMLElement? = null
    private var detailEl: HTMLElement? = null
    private var fillEl: HTMLElement? = null // overall progress (large bar)
    private var subFillEl: HTMLElement? = null // current sub-process (small bar)

    /** Build + show the overlay (call once, as early as possible). No-op if already shown. */
    fun show() {
        if (document.getElementById(OVERLAY_ID) != null) return
        val body = document.body ?: return
        val overlay = document.createElement("div") as HTMLElement
        overlay.id = OVERLAY_ID
        overlay.className = "loadingOverlay"
        // No Q-GRESS wordmark here — the player just came from the title; the build pane stays compact.
        val status = document.createElement("div") as HTMLElement
        status.className = "loadingStatus"
        status.textContent = "Starting…"
        val track = document.createElement("div") as HTMLElement
        track.className = "loadingTrack"
        val fill = document.createElement("div") as HTMLElement
        fill.className = "loadingFill"
        fill.style.width = "4%"
        track.appendChild(fill)
        val subTrack = document.createElement("div") as HTMLElement
        subTrack.className = "loadingTrack loadingSubTrack"
        val subFill = document.createElement("div") as HTMLElement
        subFill.className = "loadingFill"
        subFill.style.width = "0%"
        subTrack.appendChild(subFill)
        val detail = document.createElement("div") as HTMLElement
        detail.className = "loadingDetail"
        // Glass pane behind the text + bars so they stay readable once the backdrop goes translucent.
        val panel = document.createElement("div") as HTMLElement
        panel.className = "loadingPanel"
        panel.appendChild(status)
        panel.appendChild(track)
        panel.appendChild(subTrack)
        panel.appendChild(detail)
        overlay.appendChild(panel)
        body.appendChild(overlay)
        statusEl = status
        detailEl = detail
        fillEl = fill
        subFillEl = subFill
    }

    /** Sub-status line for what's being created right now, e.g. "Creating portal X  (3/21)". */
    fun detail(text: String) {
        detailEl?.textContent = text
    }

    /**
     * Build-phase progress: the **overall** bar advances across a [from]→[to] band while the **sub**
     * bar tracks the current sub-process ([done]/[total]), with a [label] detail line — replaces the
     * old canvas VectorBar/NpcBar + LoadingText.
     */
    fun building(from: Int, to: Int, done: Int, total: Int, label: String) {
        val frac = if (total <= 0) 1.0 else (done.toDouble() / total).coerceIn(0.0, 1.0)
        statusEl?.textContent = "Building world…"
        val percent = from + (to - from) * frac
        fillEl?.style?.width = "${percent.toInt()}%"
        subFillEl?.style?.width = "${(frac * 100).toInt()}%"
        detail("$label  ($done/$total)")
        driveBuildings(percent)
        reveal()
    }

    private const val BUILD_START_PCT = PCT_WORLD // buildings start rising once portals begin (delayed), full at 100%

    // Grow the 3D buildings in step with world-gen progress — they only reach full height when gen is done.
    private fun driveBuildings(percent: Double) {
        val frac = ((percent - BUILD_START_PCT) / (100.0 - BUILD_START_PCT)).coerceIn(0.0, 1.0)
        MapUtil.setBuildProgress(frac)
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

    /** Advance the overall bar to a labelled [text] step at [percent] (0..100); resets the sub bar. */
    fun stage(percent: Int, text: String) {
        statusEl?.textContent = text
        fillEl?.style?.width = "${percent.coerceIn(0, 100)}%"
        subFillEl?.style?.width = "0%"
        driveBuildings(percent.toDouble())
        // At the world-build stage, reveal the scene behind so the spawning portals + flow vectors show.
        if (percent >= PCT_WORLD) reveal()
    }

    /** Fill to 100% and fade the overlay out, then remove it. */
    fun done() {
        fillEl?.style?.width = "100%"
        driveBuildings(100.0) // city reaches full height exactly as world-gen completes
        val overlay = document.getElementById(OVERLAY_ID) as? HTMLElement ?: return
        overlay.className = "loadingOverlay loadingOverlayDone"
        statusEl = null
        detailEl = null
        fillEl = null
        subFillEl = null
        // Remove after the CSS fade so it stops intercepting nothing (it's already pointer-events:none).
        window.setTimeout({ overlay.remove() }, FADE_MS)
    }

    private const val FADE_MS = 600
    private val window get() = kotlinx.browser.window
}
