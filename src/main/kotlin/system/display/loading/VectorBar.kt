package system.display.loading

import World
import config.Dim
import util.DrawUtil

object VectorBar : Loading() {
    fun draw(x: Double, y: Double, h: Double, value: Int, of: Int) {
        val w = Dim.loadingBarLength / of
        (0 until of).forEach {
            val xx = x + (it * w)
            val fill = if (it <= value) fillOn else fillOff
            DrawUtil.drawExactRect(World.uiCtx(), xx, y, h, w, fill, stroke, lineWidth)
        }
    }
}
