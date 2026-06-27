package util.ui

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

        val bar = el("div", "panelBar panelBar-$side")
        val collapse = el("div", "panelCollapse displayFont")
        collapse.textContent = "–"
        collapse.title = "Collapse / expand"
        collapse.onclick = {
            val collapsed = container.classList.toggle("collapsed")
            collapse.textContent = if (collapsed) "+" else "–"
            null
        }
        bar.appendChild(collapse)
        container.insertBefore(bar, container.firstChild)
    }
}
