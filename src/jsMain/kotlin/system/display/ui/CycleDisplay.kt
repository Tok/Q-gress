package system.display.ui

import World
import config.Dim
import extension.Canvas
import extension.drawImage
import system.Cycle
import system.display.Display

object CycleDisplay : Display {
    override fun draw() {
        if (Cycle.INSTANCE.image != null) {
            val xPos = Dim.width - Dim.cycleRightOffset
            val yPos = Dim.cycleTopOffset
            World.uiCtx().drawImage(Cycle.INSTANCE.image as Canvas, xPos, yPos)
        }
    }
}
