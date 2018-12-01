package system.display.loading

import World
import config.Dim
import util.DrawUtil

object NpcBar : Loading() {
    fun draw(x: Double, y: Double, h: Double, value: Int, of: Int) {
        fun drawBack() {
            val w = Dim.loadingBarLength
            DrawUtil.drawExactRect(World.uiCtx(), x, y, h, w, fillOff, stroke, lineWidth)
        }

        fun drawValue() {
            val w = Dim.loadingBarLength * value / of
            DrawUtil.drawExactRect(World.uiCtx(), x, y, h, w, fillOn, stroke, lineWidth)
        }
        drawBack()
        drawValue()
    }
}
