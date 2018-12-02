package system.display

import Canvas
import Ctx
import World
import config.*
import org.w3c.dom.ImageData
import org.w3c.dom.Path2D
import portal.Portal
import util.ColorUtil
import util.DrawUtil
import util.HtmlUtil
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
        val w = Coords.res - 1
        val h = Coords.res - 1
        vectorField.forEach {
            fun isWalkable() = World.grid[it.key]?.isPassable ?: false
            if (Styles.isDrawObstructedVectors || isWalkable()) {
                val vectorImageData = getOrCreateVectorImageData(w, h, it.value)
                val pos = it.key.fromShadow()
                if (!HtmlUtil.isBlockedByMapbox(pos)) {
                    World.bgCtx().putImageData(vectorImageData, pos.x, pos.y)
                }
            }
        }
    }

    data class VecKey(val line: Line, val style: VectorStyle, val isColor: Boolean)

    private val VECTORS = mutableMapOf<VecKey, ImageData>()
    private fun findVec(line: Line, style: VectorStyle, isColor: Boolean): ImageData? {
        val key = VecKey(line, style, isColor)
        return VECTORS[key]
    }

    private fun putVec(line: Line, style: VectorStyle, isColor: Boolean, image: ImageData) {
        val key = VecKey(line, style, isColor)
        VECTORS[key] = image
    }

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
        val style = Styles.vectorStyle()
        val isColor = Styles.isColorVectors()
        val center = Coords.res / 2
        val vecMag = center.toDouble() //* complex.magnitude
        val scaled = Complex.fromMagnitudeAndPhase(vecMag, complex.phase)
        val line = createLine(center, scaled)
        val maybeImage = findVec(line, style, isColor)
        return if (maybeImage != null) {
            maybeImage
        } else {
            val newImageCan = createVectorImage(w, h, complex, line, style, isColor)
            val newImageCtx = newImageCan.getContext("2d") as Ctx
            val imageData = newImageCtx.getImageData(0.toDouble(), 0.toDouble(), w.toDouble(), h.toDouble())
            putVec(line, style, isColor, imageData)
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

    private fun createVectorImage(
        w: Int, h: Int, complex: Complex, line: Line,
        style: VectorStyle, isColor: Boolean
    ): Canvas {
        val brightness = 1.0 - Constants.phi
        val stroke =
            if (isColor) {
                ColorUtil.getColor(complex.copyWithNewMagnitude(brightness))
            } else {
                Colors.black
            } + "AA"
        return HtmlUtil.preRender(w, h, fun(ctx: Ctx) {
            ctx.fillStyle = "#ffffff44"
            when (style) {
                VectorStyle.CIRCLE -> drawCircle(ctx, w / 2.0)
                VectorStyle.SQUARE -> drawSquare(ctx, w, h)
            }
            val lineWidth = 1.5
            DrawUtil.drawLine(ctx, line, stroke, lineWidth)
        })
    }
}
