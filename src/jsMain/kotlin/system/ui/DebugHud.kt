package system.ui

import agent.StuckTracker
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import util.Debug

/** ?debug HUD: a tiny fixed badge with live diagnostic counters (stuck agents/NPCs). */
object DebugHud {
    private val badge: HTMLElement? by lazy {
        val el = document.createElement("div") as HTMLElement
        el.id = "debugHud"
        el.setAttribute(
            "style",
            "position:fixed;top:8px;right:8px;z-index:9999;font:12px 'Chakra Petch',monospace;" +
                "background:rgba(0,0,0,.6);color:#ff2d2d;padding:4px 8px;border-radius:4px;pointer-events:none;",
        )
        document.body?.appendChild(el)
        el
    }

    fun update() {
        if (!Debug.enabled) return
        (badge ?: return).textContent = "⛏ debug · stuck: ${StuckTracker.count()}"
    }
}
