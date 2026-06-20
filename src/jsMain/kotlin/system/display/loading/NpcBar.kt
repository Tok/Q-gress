package system.display.loading

import World
import config.Dim
import util.DrawUtil
import util.data.Line

object NpcBar : Loading() {
    fun draw(x: Int, y: Int, h: Int, value: Int, of: Int) {
        fun doDraw(w: Int, fill: String) =
            DrawUtil.drawRect(World.uiCtx(), Line(x, y, h, w), fill, stroke, lineWidth)

        fun drawBack() {
            val w = Dim.loadingBarLength.toInt()
            doDraw(w, fillOff)
        }

        fun drawValue() {
            val w = (Dim.loadingBarLength * value / of).toInt()
            doDraw(w, fillOn)
        }
        drawBack()
        drawValue()
    }
}
