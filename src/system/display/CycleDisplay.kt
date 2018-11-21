package system.display

import World
import config.Dim
import system.Cycle

object CycleDisplay {
    fun draw() {
        if (Cycle.INSTANCE.image != null) {
            val xPos = Dim.width - Dim.cycleRightOffset
            val yPos = Dim.cycleTopOffset
            World.uiCtx().drawImage(Cycle.INSTANCE.image, xPos.toDouble(), yPos.toDouble())
        }
    }
}
