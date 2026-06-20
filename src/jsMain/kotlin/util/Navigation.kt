package util

import World
import kotlinx.browser.window
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent

/**
 * Free 3D map navigation driven from the (top) UI canvas:
 * - wheel = zoom, right-button drag = pan, WASD = pan,
 * - Q/E = rotate, R/F = pitch.
 *
 * The world is rendered in [system.display.Scene3D] (a MapLibre custom layer),
 * so moving the map camera moves the scene natively — no canvas transform. The
 * 2D canvases hold only the (screen-fixed) HUD. Left-click stays portal placement.
 */
object Navigation {
    const val CANVAS_LAYER_ID = "canvasLayer"

    private const val ZOOM_FACTOR = 0.0015
    private const val PAN_STEP = 60.0
    private const val ROTATE_STEP = 15.0
    private const val PITCH_STEP = 10.0

    private var isPanning = false
    private var lastPanX = 0.0
    private var lastPanY = 0.0

    fun setup() {
        val canvas = World.uiCan
        canvas.addEventListener("wheel", { event ->
            event.preventDefault()
            MapUtil.zoomBy(-(event.asDynamic().deltaY as Double) * ZOOM_FACTOR)
        }, js("({passive: false})"))
        canvas.addEventListener("contextmenu", { event -> event.preventDefault() }, false)
        canvas.addEventListener("mousedown", { event ->
            val mouse = event as MouseEvent
            if (mouse.button.toInt() == 2) {
                isPanning = true
                lastPanX = mouse.clientX.toDouble()
                lastPanY = mouse.clientY.toDouble()
            }
        }, false)
        canvas.addEventListener("mousemove", { event ->
            if (isPanning) {
                val mouse = event as MouseEvent
                val dx = mouse.clientX.toDouble() - lastPanX
                val dy = mouse.clientY.toDouble() - lastPanY
                lastPanX = mouse.clientX.toDouble()
                lastPanY = mouse.clientY.toDouble()
                MapUtil.panBy(-dx, -dy) // grab: content follows the cursor
            }
        }, false)
        val endPan = { _: Event -> isPanning = false }
        canvas.addEventListener("mouseup", endPan, false)
        canvas.addEventListener("mouseleave", endPan, false)
        window.addEventListener("keydown", { event ->
            when ((event as KeyboardEvent).key.lowercase()) {
                "w" -> MapUtil.panBy(0.0, -PAN_STEP)
                "s" -> MapUtil.panBy(0.0, PAN_STEP)
                "a" -> MapUtil.panBy(-PAN_STEP, 0.0)
                "d" -> MapUtil.panBy(PAN_STEP, 0.0)
                "q" -> MapUtil.rotateBy(-ROTATE_STEP)
                "e" -> MapUtil.rotateBy(ROTATE_STEP)
                "r" -> MapUtil.pitchBy(PITCH_STEP)
                "f" -> MapUtil.pitchBy(-PITCH_STEP)
            }
        }, false)
    }
}
