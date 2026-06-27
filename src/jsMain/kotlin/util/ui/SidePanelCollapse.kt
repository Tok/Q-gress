package util.ui

import kotlinx.browser.document
import org.w3c.dom.HTMLElement

/**
 * Collapse toggles for the two side HUD columns: the left **tuning sliders** (`#hudLeft`) and the right
 * **stats** column (`#hudRight`). Each column gets a small "–" button (top-right of the column); clicking it
 * hides the whole column and reveals a round restore bubble pinned where the column sat — the same
 * collapse-into-a-bubble idiom as the "?" controls legend. Clicking the bubble brings the column back.
 *
 * Idempotent: [ensure] is called each frame and attaches a side's controls the first time that column exists
 * (the panels create `#hudLeft` / `#hudRight` lazily), so it survives the panels building in any order.
 */
object SidePanelCollapse {
    private val attached = mutableSetOf<String>()

    fun ensure() {
        attach("hudLeft", "left", "≡", "Show tuning sliders")
        attach("hudRight", "right", "▦", "Show stats")
    }

    private fun attach(containerId: String, side: String, glyph: String, restoreTitle: String) {
        if (side in attached) return
        val container = document.getElementById(containerId) as? HTMLElement ?: return
        attached += side

        val bubble = el("div", "panelBubble panelBubble-$side invisible")
        bubble.textContent = glyph
        bubble.title = restoreTitle

        val bar = el("div", "panelBar")
        val collapse = el("div", "panelCollapse displayFont")
        collapse.textContent = "–"
        collapse.title = "Collapse"
        collapse.onclick = {
            container.classList.add("invisible")
            bubble.classList.remove("invisible")
            null
        }
        bar.appendChild(collapse)
        container.insertBefore(bar, container.firstChild)

        bubble.onclick = {
            container.classList.remove("invisible")
            bubble.classList.add("invisible")
            null
        }
        document.body?.appendChild(bubble)
    }
}
