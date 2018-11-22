package util

import Canvas
import Ctx
import World
import agent.NonFaction
import config.*
import items.level.PortalLevel
import org.w3c.dom.*
import portal.Portal
import portal.XmMap
import system.Com
import system.display.*
import system.display.ui.ActionLimitsDisplay
import system.display.ui.CycleDisplay
import system.display.ui.MindUnits
import system.display.ui.StatsDisplay
import system.display.ui.table.TopAgentsDisplay
import util.data.Circle
import util.data.Coords
import util.data.Line
import kotlin.browser.document
import kotlin.dom.clear
import kotlin.math.PI

object DrawUtil {
    const val CODA = "Coda"
    const val AMARILLO = "AmarilloUSAF"

    fun redraw() {
        clear()
        with(World) {
            XmMap.draw()
            allAgents.forEach { it.drawRadius(ctx()) }
            allPortals.forEach { it.drawResonators(ctx()) }
            allNonFaction.forEach { it.draw(ctx()) }
            allFields().forEach { it.draw(ctx()) }
            allLinks().forEach { it.draw(ctx()) }
            allAgents.forEach { it.draw(ctx()) }
            allPortals.forEach { it.drawCenter(ctx()) }
            Attacks.draw()
            if (Styles.isDrawPortalNames) {
                allPortals.forEach { it.drawName(ctx()) }
            }
        }
    }

    fun clear() = redraw(World.can, World.ctx())
    fun clearBackground() {
        val maybeImage: ImageData? = if (Styles.isDrawNoiseMap) World.noiseImage else null
        redraw(World.bgCan, World.bgCtx(), maybeImage)
    }

    private fun clearUserInterface() = redraw(World.uiCan, World.uiCtx())

    private fun redraw(canvas: Canvas, ctx: Ctx, image: ImageData? = null) {
        canvas.width = Dim.width
        canvas.height = Dim.height
        if (image != null) {
            ctx.putImageData(image, 0.0, 0.0)
        } else {
            ctx.clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
        }
    }

    fun drawNonFaction(nonFaction: NonFaction) = nonFaction.draw(World.ctx())
    fun drawAllNonFaction(ctx: Ctx) = World.allNonFaction.forEach { it.draw(ctx) }
    fun drawAllPortals(ctx: Ctx) = World.allPortals.forEach { it.drawCenter(ctx) }

    fun redrawUserInterface(enlMu: Int, resMu: Int) {
        clearUserInterface()
        MindUnits.draw(enlMu, resMu)
        CycleDisplay.draw()
        TickDisplay.draw()
        StatsDisplay.draw()
        if (Styles.isDrawCom) {
            Com.draw(World.uiCtx())
        }
        if (Styles.isDrawTopAgents) {
            TopAgentsDisplay.draw()
        }
        if (World.mousePos != null) {
            highlightMouse(World.mousePos!!)
        }
        if (Config.isHighlighActionLimit) {
            ActionLimitsDisplay.draw()
        }
    }

    private fun highlightMouse(pos: Coords) {
        when {
            World.shadowStreetMap == null -> return
            ActionLimitsDisplay.isBlocked(pos) -> return
        }
        val ctx = World.uiCtx()
        val r = Dim.maxDeploymentRange * Constants.phi
        val circle = Circle(pos, r)

        val tempCan = document.createElement("canvas") as Canvas
        val tempCtx = tempCan.getContext("2d") as Ctx
        tempCan.width = 2 * circle.radius.toInt()
        tempCan.height = 2 * circle.radius.toInt()
        val xOffset = -(circle.center.xx() - r)
        val yOffset = -(circle.center.yy() - r)
        tempCtx.putImageData(World.shadowStreetMap!!, xOffset, yOffset)

        ctx.beginPath()
        ctx.arc(circle.center.xx(), circle.center.yy(), circle.radius, 0.0, 2.0 * PI)
        ctx.clip()

        ctx.beginPath()
        ctx.drawImage(tempCan, pos.xx() - r, pos.yy() - r, 2 * r, 2 * r)

        ctx.globalAlpha = 0.5

        val color = when {
            pos.hasClosePortalForClick() -> Colors.orange
            pos.isBuildable() -> Colors.white
            else -> Colors.red
        }
        val image = Portal.renderPortalCenter(color, PortalLevel.ZERO)
        ctx.drawImage(image, pos.xx() - (image.width / 2), pos.yy() - (image.height / 2))
        ctx.globalAlpha = 1.0
    }

    fun renderBarImage(color: String, health: Int, h: Int, w: Int, lineWidth: Int): Canvas {
        val pWidth = health * w / 100
        return HtmlUtil.preRender(w, h, fun(ctx: Ctx) {
            if (color != Colors.white) {
                val path = Path2D()
                path.moveTo(0.0, 0.0)
                path.lineTo(w.toDouble(), 0.0)
                path.lineTo(w.toDouble(), h.toDouble())
                path.lineTo(0.0, h.toDouble())
                path.lineTo(0.0, 0.0)
                path.closePath()
                DrawUtil.drawPath(ctx, path, Colors.black, lineWidth.toDouble())
                val fillPath = Path2D()
                fillPath.moveTo(0.0, 0.0)
                fillPath.lineTo(pWidth.toDouble(), 0.0)
                fillPath.lineTo(pWidth.toDouble(), h.toDouble())
                fillPath.lineTo(0.0, h.toDouble())
                fillPath.lineTo(0.0, 0.0)
                fillPath.closePath()
                DrawUtil.drawPath(ctx, fillPath, Colors.black, lineWidth.toDouble(), color)
            }
        })
    }

    fun drawRect(ctx: Ctx, pos: Coords, h: Double, w: Double,
                 fillStyle: String, strokeStyle: String, lineWidth: Double) {
        drawExactRect(ctx, pos.xx(), pos.yy(), h, w, fillStyle, strokeStyle, lineWidth)
    }

    fun drawExactRect(ctx: Ctx, x: Double, y: Double, h: Double, w: Double,
                      fillStyle: String, strokeStyle: String, lineWidth: Double) {
        ctx.fillStyle = fillStyle
        ctx.fillRect(x, y, w, -h)
        ctx.fill()
        ctx.strokeStyle = strokeStyle
        ctx.lineWidth = lineWidth
        ctx.beginPath()
        ctx.strokeRect(x, y, w, -h)
        ctx.closePath()
        ctx.stroke()
    }

    fun drawGrid() {
        with(World) {
            if (isReady) {
                grid.forEach {
                    val pos = PathUtil.shadowPosToPos(it.key)
                    val cell = it.value
                    bgCtx().fillStyle = cell.getColor()
                    val w = PathUtil.res.toDouble() - 1
                    val h = w
                    bgCtx().fillRect(pos.xx() + 1, pos.yy() + 1, w, h)
                    bgCtx().fill()
                }
            }
        }
    }

    fun drawText(ctx: Ctx, coords: Coords, text: String, fillStyle: String, fontSize: Int, fontName: String) {
        ctx.textAlign = CanvasTextAlign.START
        ctx.font = fontSize.toString() + "px '$fontName'"
        ctx.fillStyle = fillStyle
        val xOff = (fontSize / 2) - 2
        val yOff = fontSize / 3
        ctx.fillText(text, coords.x.toDouble() - xOff, coords.y.toDouble() + yOff)
    }

    fun strokeText(ctx: Ctx, pos: Coords, text: String, fillStyle: String, fontSize: Int, fontName: String = CODA,
                   lineWidth: Double = 0.0, strokeStyle: String = Colors.black,
                   textAlign: CanvasTextAlign = CanvasTextAlign.START) {
        val xOff: Double = (fontSize / 2.0) - 2
        val yOff: Double = fontSize / 3.0
        ctx.beginPath()
        ctx.font = fontSize.toString() + "px '$fontName'"
        ctx.fillStyle = fillStyle
        ctx.lineCap = CanvasLineCap.ROUND
        ctx.lineJoin = CanvasLineJoin.ROUND
        ctx.textAlign = textAlign
        if (lineWidth > 0.0) {
            ctx.lineWidth = lineWidth
            ctx.strokeStyle = strokeStyle
            ctx.strokeText(text, pos.x - xOff, pos.y + yOff)
        }
        ctx.fillText(text, pos.x - xOff, pos.y + yOff)
        ctx.closePath()
        if (lineWidth > 0.0) {
            ctx.stroke()
        }
    }

    fun drawCircle(ctx: Ctx, circle: Circle, strokeStyle: String, lineWidth: Double, fillStyle: String? = null, alpha: Double = 1.0) {
        ctx.globalAlpha = alpha
        ctx.strokeStyle = strokeStyle
        ctx.lineWidth = lineWidth
        ctx.beginPath()
        ctx.arc(circle.center.xx(), circle.center.yy(), circle.radius, 0.0, 2.0 * PI)
        ctx.closePath()
        ctx.stroke()
        if (fillStyle != null) {
            ctx.fillStyle = fillStyle
            ctx.fill()
        }
        ctx.globalAlpha = 1.0
    }

    private fun drawPath(ctx: Ctx, path: Path2D, strokeStyle: String, lineWidth: Double, fillStyle: String? = null, alpha: Double = 1.0) {
        ctx.globalAlpha = alpha
        if (fillStyle != null) {
            ctx.fillStyle = fillStyle
            ctx.fill(path)
        }
        ctx.strokeStyle = strokeStyle
        ctx.lineWidth = lineWidth
        ctx.beginPath()
        ctx.stroke(path)
        ctx.closePath()
        ctx.stroke()
        ctx.globalAlpha = 1.0
    }

    fun drawLine(ctx: Ctx, line: Line, strokeStyle: String, lineWidth: Double, alpha: Double = 1.0) {
        ctx.globalAlpha = alpha
        ctx.strokeStyle = strokeStyle
        ctx.lineWidth = lineWidth
        ctx.beginPath()
        ctx.moveTo(line.from.xx(), line.from.yy())
        ctx.lineTo(line.to.xx(), line.to.yy())
        ctx.closePath()
        ctx.stroke()
        ctx.globalAlpha = 1.0
    }
}
