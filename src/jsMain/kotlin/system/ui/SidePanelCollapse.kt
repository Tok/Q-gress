package system.ui

import kotlinx.browser.document
import org.w3c.dom.HTMLElement

/**
 * Collapse toggles for the two side HUD columns: the left **tuning sliders** (`#hudLeft`) and the right
 * **stats** column (`#hudRight`). Each column gets a small "–" button (left-aligned on the left column,
 * right-aligned on the right); clicking it collapses the column to just that button (which flips to "+"),
 * clicking again restores it. No separate restore bubble — the one button is the whole control.
 *
 * Idempotent: [ensure] is called each frame and attaches a side's control the first time that column exists
 * (the panels create `#hudLeft` / `#hudRight` lazily), so it survives the panels building in any order.
 */
object SidePanelCollapse {
    private val attached = mutableSetOf<String>()

    fun ensure() {
        attach("hudLeft", "left")
        attach("hudRight", "right")
    }

    private fun attach(containerId: String, side: String) {
        if (side in attached) return
        val container = document.getElementById(containerId) as? HTMLElement ?: return
        attached += side

        // The toggle is a direct flex child of the column (no wrapper) so the column's align-items + the
        // button's own align-self pin it to the column's outer edge — identical in the expanded and collapsed
        // states, so it never drifts toward the centre.
        val collapse = el("div", "panelCollapse panelCollapse-$side displayFont")
        collapse.textContent = "–"
        collapse.title = "Collapse / expand"
        collapse.onclick = {
            val collapsed = container.classList.toggle("collapsed")
            collapse.textContent = if (collapsed) "+" else "–"
            null
        }
        container.insertBefore(collapse, container.firstChild)
    }
}
