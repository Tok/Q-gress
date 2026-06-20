package util

import World
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent

/**
 * Free map navigation (camera-follow): wheel = zoom, right-button drag = pan,
 * WASD = pan. The simulation stays in its fixed anchored pixel space; the canvas
 * layer is CSS-transformed to track the MapLibre camera so everything stays glued
 * to the world (see MapUtil.cameraTransform). Left-click stays portal placement.
 */
object Navigation {
    const val CANVAS_LAYER_ID = "canvasLayer"

    private const val ZOOM_FACTOR = 0.0015
    private const val PAN_STEP = 60.0

    private var isPanning = false
    private var lastPanX = 0.0
    private var lastPanY = 0.0

    private fun syncCanvasToCamera() {
        val layer = document.getElementById(CANVAS_LAYER_ID) as? HTMLElement ?: return
        layer.style.setProperty("transform", MapUtil.cameraTransform())
    }

    fun setup() {
        MapUtil.onCameraMove { syncCanvasToCamera() }
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
            }
        }, false)
    }
}
