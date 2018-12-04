package system.display.loading

import World
import agent.NonFaction
import config.Colors
import config.Config
import config.Dim
import extension.clearRect
import util.data.Line

abstract class Loading {
    val lineWidth = 1.0
    val stroke = Colors.black
    val fillOn = Colors.white
    val fillOff = Colors.white + "44"

    companion object {
        fun clearUiLine(y: Int, h: Int) {
            val rect = Line(0, y, World.uiCan.width, h)
            World.uiCtx().clearRect(rect)
        }

        fun draw() {
            val vecCount = World.countPortals() + NonFaction.offscreenCount()
            val vecY = (2.0 + 34.0 + (Dim.height / 2.0)).toInt()
            val vecX = ((Dim.width / 2.0) - (Dim.loadingBarLength / 2.0)).toInt()
            val vecTot = Config.startPortals + NonFaction.offscreenTotal()
            val vecH = 21
            val npcY = vecY + vecH - 13
            val npcH = 8
            clearUiLine(vecY - vecH - 1, vecH + npcH + 2)
            VectorBar.draw(vecX, vecY, vecH, vecCount, vecTot)
            NpcBar.draw(vecX, npcY, npcH, World.countNonFaction(), Config.maxFor())
        }
    }
}
