package util.ui

import kotlinx.browser.document
import org.w3c.dom.HTMLElement

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
    const val PCT_WORLD = 78

    private const val OVERLAY_ID = "loadingOverlay"
    private const val DEFAULT_ACCENT = "#ffffff"
    private var statusEl: HTMLElement? = null
    private var fillEl: HTMLElement? = null
    private var titleEl: HTMLElement? = null
    private var accent = DEFAULT_ACCENT

    /** Build + show the overlay (call once, as early as possible). No-op if already shown. */
    fun show() {
        if (document.getElementById(OVERLAY_ID) != null) return
        val body = document.body ?: return
        val overlay = document.createElement("div") as HTMLElement
        overlay.id = OVERLAY_ID
        overlay.className = "loadingOverlay"
        val title = document.createElement("div") as HTMLElement
        title.className = "loadingTitle"
        title.textContent = "Q-GRESS"
        val status = document.createElement("div") as HTMLElement
        status.className = "loadingStatus"
        status.textContent = "Starting…"
        val track = document.createElement("div") as HTMLElement
        track.className = "loadingTrack"
        val fill = document.createElement("div") as HTMLElement
        fill.className = "loadingFill"
        fill.style.width = "4%"
        track.appendChild(fill)
        overlay.appendChild(title)
        overlay.appendChild(status)
        overlay.appendChild(track)
        body.appendChild(overlay)
        statusEl = status
        fillEl = fill
        titleEl = title
        applyAccent()
    }

    /** Tint the overlay with the chosen faction colour (call once a faction is picked). */
    fun setAccent(color: String) {
        accent = color
        applyAccent()
    }

    private fun applyAccent() {
        titleEl?.style?.textShadow = "0 0 18px $accent"
        fillEl?.style?.background = "linear-gradient(90deg, ${accent}66, $accent)"
    }

    /** Advance to a labelled [text] step at [percent] (0..100) fill. */
    fun stage(percent: Int, text: String) {
        statusEl?.textContent = text
        fillEl?.style?.width = "${percent.coerceIn(0, 100)}%"
        // At the world-build stage, reveal the scene behind so the spawning portals + flow vectors show.
        if (percent >= PCT_WORLD) {
            (document.getElementById(OVERLAY_ID) as? HTMLElement)?.className = "loadingOverlay loadingOverlayReveal"
        }
    }

    /** Fill to 100% and fade the overlay out, then remove it. */
    fun done() {
        fillEl?.style?.width = "100%"
        val overlay = document.getElementById(OVERLAY_ID) as? HTMLElement ?: return
        overlay.className = "loadingOverlay loadingOverlayDone"
        statusEl = null
        fillEl = null
        titleEl = null
        // Remove after the CSS fade so it stops intercepting nothing (it's already pointer-events:none).
        window.setTimeout({ overlay.remove() }, FADE_MS)
    }

    private const val FADE_MS = 600
    private val window get() = kotlinx.browser.window
}
