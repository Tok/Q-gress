package util

import kotlinx.browser.window
import org.w3c.dom.events.KeyboardEvent

/**
 * Pan/rotate/tilt/zoom are handled by MapLibre's own (standard) interaction
 * handlers — see [MapUtil.setupNavigation]: left-drag pans, right-drag rotates
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
