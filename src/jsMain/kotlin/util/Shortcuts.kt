package util

import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.KeyboardEvent

/**
 * Global keyboard shortcuts for the running game. The actual effects live in [HtmlUtil] (map, sim,
 * audio); this just maps keys → [Handlers] callbacks. Keys are ignored while typing in a form field,
 * and a handled key calls `preventDefault` (so Space/arrows don't scroll, +/- don't browser-zoom).
 *
 * Space=pause · Home=recenter · PageUp/+ & PageDown/- = zoom · arrows = pan · ,/. = sim speed ·
 * M=mute · Esc=close panels.
 */
object Shortcuts {
    class Handlers(
        val pause: () -> Unit,
        val home: () -> Unit,
        val zoom: (Double) -> Unit,
        val pan: (Double, Double) -> Unit,
        val speedDelta: (Double) -> Unit,
        val mute: () -> Unit,
        val close: () -> Unit,
    )

    private const val ZOOM = 0.6 // zoom levels per key press
    private const val PAN = 90.0 // pixels per arrow-key pan
    private const val SPEED = 0.25 // sim-speed step per ,/. press
    private var bound = false

    fun bind(handlers: Handlers) {
        if (bound) return
        bound = true
        document.addEventListener("keydown", { e ->
            val ev = e as KeyboardEvent
            if (!isTyping(ev.target) && (handleView(ev, handlers) || handleSim(ev, handlers))) {
                ev.preventDefault()
            }
        })
    }

    private fun isTyping(t: dynamic) = t is HTMLInputElement || t is HTMLTextAreaElement || t is HTMLSelectElement

    /** Map/camera keys. Returns true if [ev] was handled. */
    private fun handleView(ev: KeyboardEvent, h: Handlers): Boolean {
        when (ev.code) {
            "Space" -> if (!ev.repeat) h.pause()
            "Home" -> if (!ev.repeat) h.home()
            "PageUp", "Equal", "NumpadAdd" -> h.zoom(ZOOM)
            "PageDown", "Minus", "NumpadSubtract" -> h.zoom(-ZOOM)
            "ArrowUp" -> h.pan(0.0, -PAN)
            "ArrowDown" -> h.pan(0.0, PAN)
            "ArrowLeft" -> h.pan(-PAN, 0.0)
            "ArrowRight" -> h.pan(PAN, 0.0)
            else -> return false
        }
        return true
    }

    /** Sim/audio/panel keys. Returns true if [ev] was handled. */
    private fun handleSim(ev: KeyboardEvent, h: Handlers): Boolean {
        when (ev.code) {
            "Comma" -> if (!ev.repeat) h.speedDelta(-SPEED)
            "Period" -> if (!ev.repeat) h.speedDelta(SPEED)
            "KeyM" -> if (!ev.repeat) h.mute()
            "Escape" -> h.close()
            else -> return false
        }
        return true
    }
}
