package system

import Ctx
import config.Colors
import config.Config
import config.Dim
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
        val xPos = Dim.width - Dim.comRightOffset
        val yFixOffset = Dim.height - Dim.comBottomOffset
        val yOffset = (Dim.comFontSize * 3 / 2)
        val reversed = messages.reversed()
        reversed.forEachIndexed { index, text ->
            val pos = Coords(xPos, yFixOffset - (yOffset * index))
            DrawUtil.strokeText(ctx, pos, text, Colors.white, Dim.comFontSize, DrawUtil.CODA, 1.5)
        }
    }
}
