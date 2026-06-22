package util.ui

import kotlinx.browser.document
import kotlinx.dom.addClass
import org.w3c.dom.HTMLElement

/** A small keyboard-shortcuts reference popup (Menu → Shortcuts). Closes on any click. */
object ShortcutsHelp {
    private const val ID = "shortcutsHelp"
    private val ROWS = listOf(
        "Space" to "Pause / resume",
        "Home" to "Recenter on the play area",
        "PageUp / PageDown" to "Zoom in / out",
        "Arrow keys / WASD" to "Pan the map",
        "− / +" to "Slower / faster simulation",
        ", / ." to "Building transparency",
        "Tab" to "Switch footer tab",
        "M" to "Mute / unmute",
        "Esc" to "Close panels",
    )

    fun show() {
        close()
        val backdrop = div("shortcutsBackdrop")
        backdrop.id = ID
        val panel = div("shortcutsPanel")
        panel.innerHTML = html()
        backdrop.appendChild(panel)
        backdrop.onclick = {
            close()
            null
        } // close on any click (anywhere, incl. the panel)
        document.body?.appendChild(backdrop)
    }

    fun close() {
        document.getElementById(ID)?.remove()
    }

    private fun html(): String {
        val sb = StringBuilder("<div class=\"shortcutsTitle\">Keyboard shortcuts</div>")
        ROWS.forEach { (key, desc) ->
            sb.append("<div class=\"shortcutsRow\"><span class=\"shortcutsKey\">$key</span><span>$desc</span></div>")
        }
        sb.append("<div class=\"shortcutsNote\">Click anywhere to close.</div>")
        return sb.toString()
    }

    private fun div(cls: String): HTMLElement {
        val e = document.createElement("div") as HTMLElement
        e.addClass(cls)
        return e
    }
}
