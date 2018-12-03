package system.display.loading

import World
import config.Dim
import util.DrawUtil
import util.data.Line

object VectorBar : Loading() {
    fun draw(x: Int, y: Int, h: Int, value: Int, of: Int) {
        val w = Dim.loadingBarLength / of
        (0 until of).forEach {
            val xx = x + (it * w)
            val fill = if (it <= value) fillOn else fillOff
            val rect = Line(xx.toInt(), y, h, w.toInt())
            DrawUtil.drawRect(World.uiCtx(), rect, fill, stroke, lineWidth)
        }
    }
}
