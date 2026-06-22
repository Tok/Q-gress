package util.ui

import kotlinx.browser.document
import org.w3c.dom.HTMLElement

/**
 * Fixed, click-through HUD containers, positioned by CSS:
 * - [left] — the player-controls column (the tuning sliders),
 * - [right] — the "intel" column (history dashboard + the contextual inspector),
 * - [top] — a slim centered scoreboard strip under the toolbar.
 *
 * Panels append themselves here (lazily). The containers never intercept map gestures; only their
 * interactive children (sliders, the inspector) opt back into pointer events.
 */
object Hud {
    fun left(): HTMLElement = container("hudLeft")
    fun right(): HTMLElement = container("hudRight")
    fun top(): HTMLElement = container("hudTop")

    private fun container(id: String): HTMLElement {
        (document.getElementById(id) as? HTMLElement)?.let { return it }
        val div = document.createElement("div") as HTMLElement
        div.id = id
        document.body?.appendChild(div)
        return div
    }
}
