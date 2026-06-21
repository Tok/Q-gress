package util.ui

import kotlinx.browser.document
import org.w3c.dom.HTMLElement

/**
 * Groups the HUD into a few fixed containers instead of panels scattered to each corner:
 * - [left] — the "scoreboard" column (MU bars + Time/Tick/Stray XM),
 * - [right] — the "intel" column (history dashboard above, action LOG below),
 * - [bottom] — a centered strip (the agent leaderboard).
 *
 * Panels append themselves into these (lazily, on first build). Ordering inside a column is set in
 * CSS (`order`) so it doesn't depend on which panel builds first.
 */
object Hud {
    fun left(): HTMLElement = container("hudLeft")
    fun right(): HTMLElement = container("hudRight")
    fun bottom(): HTMLElement = container("hudBottom")

    private fun container(id: String): HTMLElement {
        (document.getElementById(id) as? HTMLElement)?.let { return it }
        val div = document.createElement("div") as HTMLElement
        div.id = id
        document.body?.appendChild(div)
        return div
    }
}
