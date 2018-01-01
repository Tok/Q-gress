package system

import Ctx
import World
import config.Colors
import config.Config
import config.Dimensions
import util.DrawUtil
import util.data.Coords

object Com {
    val messages: MutableList<String> = mutableListOf()

    fun addMessage(message: String) {
        messages.add(message)
        if (messages.size > Config.comMessageLimit) {
            messages.removeAt(0)
        }
    }

    fun draw(ctx: Ctx) {
        val messages = messages.toList()
        val xPos = World.can.width - Dimensions.comRightOffset
        val yFixOffset = World.can.height - Dimensions.comBottomOffset
        val yOffset = (Dimensions.comFontSize * 3 / 2)
        val reversed = messages.reversed()
        reversed.forEachIndexed { index, text ->
            DrawUtil.drawText(ctx, Coords(xPos, yFixOffset - (yOffset * index)), text, Colors.white, Dimensions.comFontSize, DrawUtil.CODA)
        }
    }
}
