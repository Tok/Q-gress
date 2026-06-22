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
 * Space=pause · Home=recenter · PageUp/PageDown=zoom · WASD=pan (arrows pan natively) ·
 * -/+ = sim speed · ,/. = building transparency · Tab=footer tab · M=mute · Esc=close panels.
 */
object Shortcuts {
    enum class Command { PAUSE, HOME, CYCLE_TAB, MUTE, CLOSE }

    class Handlers(
        val command: (Command) -> Unit,
        val zoom: (Double) -> Unit,
        val pan: (Double, Double) -> Unit,
        val buildingOpacity: (Double) -> Unit,
        val speedDelta: (Double) -> Unit,
    )

    private const val ZOOM = 0.6 // zoom levels per PageUp/PageDown
    private const val PAN = 90.0 // pixels per pan key
    private const val SPEED = 0.25 // sim-speed step per ,/.
    private const val OPACITY = 0.1 // building-transparency step per -/+
    private var bound = false

    fun bind(handlers: Handlers) {
        if (bound) return
        bound = true
        // Capture phase + stopPropagation: intercept our keys (e.g. -/+) BEFORE MapLibre's own keyboard
        // handler so it doesn't also zoom. Unhandled keys (arrows) fall through to MapLibre's native pan.
        document.addEventListener(
            "keydown",
            { e ->
                val ev = e as KeyboardEvent
                if (!isTyping(ev.target) && dispatch(ev, handlers)) {
                    ev.preventDefault()
                    ev.stopPropagation()
                }
            },
            true,
        )
    }

    private fun isTyping(t: dynamic) = t is HTMLInputElement || t is HTMLTextAreaElement || t is HTMLSelectElement

    private fun dispatch(ev: KeyboardEvent, h: Handlers) = handlePan(ev, h) || handleView(ev, h) || handleSim(ev, h)

    /** WASD pan (arrow keys are handled natively by MapLibre). Returns true if [ev] was handled. */
    private fun handlePan(ev: KeyboardEvent, h: Handlers): Boolean {
        when (ev.code) {
            "KeyW" -> h.pan(0.0, -PAN)
            "KeyS" -> h.pan(0.0, PAN)
            "KeyA" -> h.pan(-PAN, 0.0)
            "KeyD" -> h.pan(PAN, 0.0)
            else -> return false
        }
        return true
    }

    /** Camera/view keys: pause, recenter, zoom (PageUp/PageDown). */
    private fun handleView(ev: KeyboardEvent, h: Handlers): Boolean {
        when (ev.code) {
            "Space" -> if (!ev.repeat) h.command(Command.PAUSE)
            "Home" -> if (!ev.repeat) h.command(Command.HOME)
            "PageUp" -> h.zoom(ZOOM)
            "PageDown" -> h.zoom(-ZOOM)
            else -> return false
        }
        return true
    }

    /** Sim/audio/panel keys: -/+ sim speed, ,/. building transparency, Tab/M/Esc. */
    private fun handleSim(ev: KeyboardEvent, h: Handlers): Boolean {
        when (ev.code) {
            "Minus", "NumpadSubtract" -> h.speedDelta(-SPEED)
            "Equal", "NumpadAdd" -> h.speedDelta(SPEED)
            "Comma" -> h.buildingOpacity(-OPACITY)
            "Period" -> h.buildingOpacity(OPACITY)
            "Tab" -> if (!ev.repeat) h.command(Command.CYCLE_TAB)
            "KeyM" -> if (!ev.repeat) h.command(Command.MUTE)
            "Escape" -> h.command(Command.CLOSE)
            else -> return false
        }
        return true
    }
}
