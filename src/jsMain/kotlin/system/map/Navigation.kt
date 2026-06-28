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
                "w" -> MapCamera.panBy(0.0, -PAN_STEP)
                "s" -> MapCamera.panBy(0.0, PAN_STEP)
                "a" -> MapCamera.panBy(-PAN_STEP, 0.0)
                "d" -> MapCamera.panBy(PAN_STEP, 0.0)
                "q" -> MapCamera.rotateBy(-ROTATE_STEP)
                "e" -> MapCamera.rotateBy(ROTATE_STEP)
                "r" -> MapCamera.pitchBy(PITCH_STEP)
                "f" -> MapCamera.pitchBy(-PITCH_STEP)
            }
        }, false)
    }
}
