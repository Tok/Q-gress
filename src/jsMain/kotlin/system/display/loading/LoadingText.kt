package system.display.loading

import World
import config.Colors
import config.Dim
import util.DrawUtil
import util.data.Pos

object LoadingText : Loading() {
    fun draw(text: String) {
        val y = (Dim.height / 2) - 3
        val x = ((Dim.width / 2.0) - (Dim.loadingBarLength / 2.0)).toInt() + 13
        val lineWidth = 3.0
        val h = Dim.loadingFontSize
        val hh = h / 2
        clearUiLine(y - hh - 1, h + 2)
        DrawUtil.strokeText(
            World.uiCtx(),
            Pos(x, y),
            text,
            Colors.white,
            h,
            DrawUtil.AMARILLO,
            lineWidth,
            Colors.black
        )
    }
}
