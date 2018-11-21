package system.display.ui

import World
import config.Dim
import system.Cycle
import system.display.Display

object CycleDisplay : Display {
    override fun draw() {
        if (Cycle.INSTANCE.image != null) {
            val xPos = Dim.width - Dim.cycleRightOffset
            val yPos = Dim.cycleTopOffset
            World.uiCtx().drawImage(Cycle.INSTANCE.image, xPos.toDouble(), yPos.toDouble())
        }
    }
}
