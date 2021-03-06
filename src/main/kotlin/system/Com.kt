package system

import config.Colors
import config.Config
import config.Dim
import extension.Ctx
import util.DrawUtil
import util.data.Pos

object Com {
    private val messages: MutableList<String> = mutableListOf()
    fun messageCount() = messages.count()

    fun clear() = messages.clear()

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
            val pos = Pos(xPos, yFixOffset - (yOffset * index))
            DrawUtil.strokeText(ctx, pos, text, Colors.white, Dim.comFontSize, DrawUtil.CODA, 1.5)
        }
    }
}
