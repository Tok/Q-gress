package system.loading

import World
import config.Colors
import config.Dim
import util.DrawUtil
import util.data.Coords

object LoadingText : Loading() {
    fun draw(text: String) {
        val y = Dim.height / 2
        val x = ((Dim.width / 2.0) - (Dim.loadingBarLength / 2.0)).toInt()
        val lineWidth = 3.0
        val strokeStyle: String = Colors.black
        val h = Dim.loadingFontSize
        val hh = h / 2
        clearUiLine(y.toDouble() - hh - 1, h.toDouble() + 2)
        DrawUtil.strokeText(World.uiCtx(), Coords(x, y), text, Colors.white, h, DrawUtil.AMARILLO, lineWidth, strokeStyle)
    }
}
