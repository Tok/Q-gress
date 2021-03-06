package util

import World
import agent.Faction
import agent.NonFaction
import config.*
import extension.Canvas
import extension.Ctx
import extension.clear
import items.level.PortalLevel
import org.w3c.dom.*
import portal.Portal
import portal.XmMap
import system.Com
import system.display.Attacks
import system.display.TickDisplay
import system.display.ui.ActionLimitsDisplay
import system.display.ui.CycleDisplay
import system.display.ui.MindUnits
import system.display.ui.StatsDisplay
import system.display.ui.table.TopAgentsDisplay
import util.data.Circle
import util.data.Line
import util.data.Pos
import kotlin.browser.document
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
            if (Styles.isDrawPortalNames) {
                allPortals.forEach { it.drawName(ctx()) }
            }
            allNonFaction.forEach { it.draw(ctx()) }
            allAgents.forEach { it.draw(ctx()) }
            allFields().forEach { it.draw(ctx()) }
            allLinks().forEach { it.draw(ctx()) }
            allPortals.forEach { it.drawCenter(ctx()) }
            Attacks.draw()
        }
    }

    fun clear() = redraw(World.can, World.ctx())
    fun clearBackground() {
        val maybeImage: ImageData? = if (Styles.isDrawNoiseMap) World.noiseImage else null
        redraw(World.bgCan, World.bgCtx(), maybeImage)
    }

    fun clearUserInterface() = redraw(World.uiCan, World.uiCtx())

    private fun redraw(canvas: Canvas, ctx: Ctx, image: ImageData? = null) {
        canvas.width = Dim.width
        canvas.height = Dim.height
        if (image != null) {
            ctx.putImageData(image, 0.0, 0.0)
        } else {
            ctx.clear(canvas)
        }
    }

    fun drawNonFaction(nonFaction: NonFaction) = nonFaction.draw(World.ctx())
    fun drawAllNonFaction(ctx: Ctx) = World.allNonFaction.forEach { it.draw(ctx) }
    fun drawAllPortals(ctx: Ctx) = World.allPortals.forEach { it.drawCenter(ctx) }

    fun redrawUserInterface(firstMu: Int, secondMu: Int, factions: Pair<Faction, Faction>) {
        clearUserInterface()
        MindUnits.draw(firstMu, secondMu, factions)
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
        } else {
            ActionLimitsDisplay.drawTop()
        }
    }

    private fun highlightMouse(pos: Pos) {
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
        val xOffset = -(circle.center.x - r)
        val yOffset = -(circle.center.y - r)
        tempCtx.putImageData(World.shadowStreetMap!!, xOffset, yOffset)

        ctx.beginPath()
        ctx.arc(circle.center.x, circle.center.y, circle.radius, 0.0, 2.0 * PI)
        ctx.clip()

        ctx.beginPath()
        ctx.drawImage(tempCan, pos.x - r, pos.y - r, 2 * r, 2 * r)

        ctx.globalAlpha = 0.5

        val color = when {
            pos.hasClosePortalForClick() -> Colors.orange
            pos.isBuildable() -> Colors.white
            else -> Colors.red
        }
        val image = Portal.renderPortalCenter(color, PortalLevel.ZERO)
        ctx.drawImage(image, pos.x - (image.width / 2), pos.y - (image.height / 2))
        ctx.globalAlpha = 1.0
    }

    fun renderBarImage(color: String, health: Int, h: Double, w: Double, lineWidth: Int): Canvas {
        val pWidth = health * w / 100
        return HtmlUtil.preRender(w.toInt(), h.toInt(), fun(ctx: Ctx) {
            if (color != Colors.white) {
                val path = Path2D()
                path.moveTo(0.0, 0.0)
                path.lineTo(w, 0.0)
                path.lineTo(w, h)
                path.lineTo(0.0, h)
                path.lineTo(0.0, 0.0)
                path.closePath()
                DrawUtil.drawPath(ctx, path, Colors.black, lineWidth.toDouble())
                val fillPath = Path2D()
                fillPath.moveTo(0.0, 0.0)
                fillPath.lineTo(pWidth, 0.0)
                fillPath.lineTo(pWidth, h)
                fillPath.lineTo(0.0, h)
                fillPath.lineTo(0.0, 0.0)
                fillPath.closePath()
                DrawUtil.drawPath(ctx, fillPath, Colors.black, lineWidth.toDouble(), color)
            }
        })
    }

    fun drawRect(ctx: Ctx, rect: Line, fill: String, stroke: String, lineWidth: Double) {
        ctx.fillStyle = fill
        with(rect) {
            ctx.fillRect(fromX, fromY, toY, -toX) //argument switch
        }
        ctx.fill()
        ctx.strokeStyle = stroke
        ctx.lineWidth = lineWidth
        ctx.beginPath()
        with(rect) {
            ctx.strokeRect(fromX, fromY, toY, -toX) //argument switch
        }
        ctx.closePath()
        ctx.stroke()
    }

    fun drawGrid() {
        with(World) {
            if (isReady) {
                grid.forEach {
                    val pos = it.key.fromShadow()
                    val cell = it.value
                    bgCtx().fillStyle = cell.getColor()
                    val w = Pos.res - 1.0
                    val h = w
                    bgCtx().fillRect(pos.x + 1, pos.y + 1, w, h)
                    bgCtx().fill()
                }
            }
        }
    }

    fun drawText(ctx: Ctx, pos: Pos, text: String, fill: String, fontSize: Int, fontName: String) {
        ctx.textAlign = CanvasTextAlign.START
        ctx.font = fontSize.toString() + "px '$fontName'"
        ctx.fillStyle = fill
        val xOff = (fontSize / 2) - 2
        val yOff = fontSize / 3
        ctx.fillText(text, pos.x - xOff, pos.y + yOff)
    }

    fun strokeText(
        ctx: Ctx, pos: Pos, text: String, fill: String, fontSize: Int,
        fontName: String = CODA, lineWidth: Double = 0.0,
        stroke: String = Colors.black, textAlign: CanvasTextAlign = CanvasTextAlign.START
    ) {
        val xOff: Double = (fontSize / 2.0) - 2
        val yOff: Double = fontSize / 3.0
        ctx.beginPath()
        ctx.font = fontSize.toString() + "px '$fontName'"
        ctx.fillStyle = fill
        ctx.lineCap = CanvasLineCap.ROUND
        ctx.lineJoin = CanvasLineJoin.ROUND
        ctx.textAlign = textAlign
        if (lineWidth > 0.0) {
            ctx.lineWidth = lineWidth
            ctx.strokeStyle = stroke
            ctx.strokeText(text, pos.x - xOff, pos.y + yOff)
        }
        ctx.fillText(text, pos.x - xOff, pos.y + yOff)
        ctx.closePath()
        if (lineWidth > 0.0) {
            ctx.stroke()
        }
    }

    fun drawCircle(
        ctx: Ctx, circle: Circle, stroke: String, lineWidth: Double,
        fill: String? = null, alpha: Double = 1.0
    ) {
        ctx.globalAlpha = alpha
        ctx.strokeStyle = stroke
        ctx.lineWidth = lineWidth
        ctx.beginPath()
        ctx.arc(circle.center.x, circle.center.y, circle.radius, 0.0, 2.0 * PI)
        ctx.closePath()
        ctx.stroke()
        if (fill != null) {
            ctx.fillStyle = fill
            ctx.fill()
        }
        ctx.globalAlpha = 1.0
    }

    private fun drawPath(
        ctx: Ctx, path: Path2D, stroke: String, lineWidth: Double,
        fill: String? = null, alpha: Double = 1.0
    ) {
        ctx.globalAlpha = alpha
        if (fill != null) {
            ctx.fillStyle = fill
            ctx.fill(path)
        }
        ctx.strokeStyle = stroke
        ctx.lineWidth = lineWidth
        ctx.beginPath()
        ctx.stroke(path)
        ctx.closePath()
        ctx.stroke()
        ctx.globalAlpha = 1.0
    }

    fun drawLine(ctx: Ctx, line: Line, stroke: String, lineWidth: Double, alpha: Double = 1.0) {
        ctx.globalAlpha = alpha
        ctx.strokeStyle = stroke
        ctx.lineWidth = lineWidth
        ctx.beginPath()
        ctx.moveTo(line.from.x, line.from.y)
        ctx.lineTo(line.to.x, line.to.y)
        ctx.closePath()
        ctx.stroke()
        ctx.globalAlpha = 1.0
    }
}
