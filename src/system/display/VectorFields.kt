package system.display

import Canvas
import Ctx
import World
import config.Colors
import config.Constants
import config.Dim
import config.Styles
import org.w3c.dom.ImageData
import org.w3c.dom.Path2D
import portal.Portal
import util.ColorUtil
import util.DrawUtil
import util.HtmlUtil
import util.PathUtil
import util.data.Complex
import util.data.Coords
import util.data.Line

object VectorFields {
    fun draw(portal: Portal) {
        draw(portal.vectorField)
        portal.drawCenter(World.bgCtx(), false)
    }

    fun draw(vectorField: Map<Coords, Complex>) {
        if (World.isReady) return
        World.bgCtx().clearRect(0.0, 0.0, Dim.width.toDouble(), Dim.height.toDouble())
        val w = PathUtil.res - 1
        val h = PathUtil.res - 1
        vectorField.forEach {
            fun isWalkable() = World.grid[it.key]?.isPassable ?: false
            if (Styles.isDrawObstructedVectors || isWalkable()) {
                val vectorImageData = getOrCreateVectorImageData(w, h, it.value)
                val pos = PathUtil.shadowPosToPos(it.key)
                val isBlocked = HtmlUtil.isBlockedForVector(pos)
                if (!isBlocked) {
                    World.bgCtx().putImageData(vectorImageData, pos.xx(), pos.yy())
                }
            }
        }
    }

    private val VECTORS = mutableMapOf<Line, ImageData>()

    private fun createLine(center: Int, scaled: Complex): Line {
        val re = scaled.re.toInt()
        val im = scaled.im.toInt()
        val negRe = (re / Constants.phi).toInt()
        val negIm = (im / Constants.phi).toInt()
        val from = Coords(center - negRe, center - negIm)
        val to = Coords(center + re, center + im)
        return Line(from, to)
    }

    private fun getOrCreateVectorImageData(w: Int, h: Int, complex: Complex): ImageData {
        val center = PathUtil.res / 2
        val scaled = Complex.fromMagnitudeAndPhase(complex.magnitude * center, complex.phase)
        val line = createLine(center, scaled)
        val maybeImage = VECTORS[line]
        return if (maybeImage != null) {
            maybeImage
        } else {
            val newImageCan = createVectorImage(w, h, complex, line)
            val newImageCtx = newImageCan.getContext("2d") as Ctx
            val imageData = newImageCtx.getImageData(0.toDouble(), 0.toDouble(), w.toDouble(), h.toDouble())
            VECTORS[line] = imageData
            imageData
        }
    }

    private fun drawCircle(ctx: Ctx, r: Double) {
        val path = Path2D()
        path.moveTo(r, r)
        path.arc(r, r, r, 0.0, 2.0 * kotlin.math.PI)
        ctx.fill(path)
    }

    private fun drawSquare(ctx: Ctx, w: Int, h: Int) {
        ctx.fillRect(1.0, 1.0, w.toDouble(), h.toDouble())
        ctx.fill()
    }

    private fun stroke(complex: Complex) =
            if (Styles.useColorVectors) ColorUtil.getColor(complex) + "AA" else Colors.black + "AA"

    private fun createVectorImage(w: Int, h: Int, complex: Complex, line: Line): Canvas {
        return HtmlUtil.preRender(w, h, fun(ctx: Ctx) {
            ctx.fillStyle = "#ffffff44"
            when (Styles.vectorStyle) {
                Styles.VectorStyle.CIRCLE -> drawCircle(ctx, w / 2.0)
                Styles.VectorStyle.SQUARE -> drawSquare(ctx, w, h)
            }
            val lineWidth = 2.0
            DrawUtil.drawLine(ctx, line, stroke(complex), lineWidth)
        })
    }
}
