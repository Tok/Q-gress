package portal

import Canvas
import Ctx
import World
import config.Colors
import util.DrawUtil
import util.HtmlUtil
import util.Util
import util.data.Circle
import util.data.Coords

data class XmHeap(private val cores: Triple<Int, Int, Int>, private var isCollected: Boolean = false) {
    val xm = cores.first + cores.second + cores.third
    fun isCollected() = isCollected
    fun collect() {
        isCollected = true
    }

    private val IMAGE = drawHeapTemplate()
    fun draw(position: Coords) {
        World.ctx().drawImage(IMAGE, position.xx(), position.yy())
    }

    companion object {
        private const val coreCount = 3
        const val strayXmMinDistance = 8

        private val CORE_IMAGE = drawCoreTemplate()
        private fun drawHeapTemplate(): Canvas {
            val scatter = 5
            val w = 15
            val h = 15
            val r = 5
            return HtmlUtil.preRender(w, h, fun(ctx: Ctx) {
                (0..coreCount).forEach {
                    val pos = Coords(r, r).randomNearPoint(scatter)
                    ctx.drawImage(CORE_IMAGE, pos.xx(), pos.yy())
                }
            })
        }

        private fun drawCoreTemplate(): Canvas {
            val r = 2
            val w = 4
            val h = 4
            val stroke = Colors.white + "33"
            val lineWidth = 1.0
            val fill = Colors.white
            val alpha = 0.4
            return HtmlUtil.preRender(w, h, fun(ctx: Ctx) {
                val circle = Circle(Coords(r, r), r.toDouble())
                DrawUtil.drawCircle(ctx, circle, stroke, lineWidth, fill, alpha)
            })
        }

        private const val minCapacity = 35
        private const val maxCapacity = 100
        const val capacity = maxCapacity - minCapacity
        private fun createCore(): Int = Util.randomInt(minCapacity, maxCapacity)
        private fun createCores() = Triple(createCore(), createCore(), createCore())
        fun create() = XmHeap(createCores())
    }
}
