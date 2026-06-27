package system.map

import kotlinx.browser.window
import org.w3c.dom.events.KeyboardEvent

/**
 * Pan/rotate/tilt/zoom are handled by MapLibre's own (standard) interaction
 * handlers — see [MapController.setupNavigation]: left-drag pans, right-drag rotates
 * AND tilts, the wheel zooms, all unrestricted, plus the on-screen control block.
 *
 * This object only layers optional WASD / Q-E / R-F keyboard shortcuts on top,
 * for players who prefer keys. The world is a MapLibre custom layer
 * ([system.display.Scene3D]), so moving the map camera moves the scene for free.
 */
object Navigation {
    private const val PAN_STEP = 60.0
    private const val ROTATE_STEP = 15.0
    private const val PITCH_STEP = 10.0

    fun setup() {
        window.addEventListener("keydown", { event ->
            when ((event as KeyboardEvent).key.lowercase()) {
                "w" -> MapController.panBy(0.0, -PAN_STEP)
                "s" -> MapController.panBy(0.0, PAN_STEP)
                "a" -> MapController.panBy(-PAN_STEP, 0.0)
                "d" -> MapController.panBy(PAN_STEP, 0.0)
                "q" -> MapController.rotateBy(-ROTATE_STEP)
                "e" -> MapController.rotateBy(ROTATE_STEP)
                "r" -> MapController.pitchBy(PITCH_STEP)
                "f" -> MapController.pitchBy(-PITCH_STEP)
            }
        }, false)
    }
}
