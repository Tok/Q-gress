package util

import World
import agent.Faction
import agent.NonFaction
import config.*
import extension.Canvas
import extension.Ctx
import extension.clear
import org.w3c.dom.*
import system.display.Scene3D
import system.display.ui.ActionLimitsDisplay
import system.display.ui.table.TopAgentsDisplay
import util.data.Circle
import util.data.Line
import util.data.Pos
import util.ui.CycleChart
import util.ui.Inspector
import util.ui.StatsPanel
import kotlin.math.PI

object DrawUtil {
    const val CODA = "Coda"
    const val AMARILLO = "AmarilloUSAF"

    fun redraw() {
        // World entities now render in the 3D scene (Scene3D). The 2D world
        // canvas is kept clear so the 3D (on the map layer beneath) shows
        // through; the HUD is still drawn 2D in redrawUserInterface().
        clear()
        Scene3D.sync()
        Inspector.refresh()
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
        // MindUnits + entity counts + tick + Com + the Cycle MU graph (uPlot) are now DOM panels
        // (UI Stage 3, canvas→DOM). The TopAgents table is the last canvas widget pending migration.
        StatsPanel.update(firstMu, secondMu, factions)
        CycleChart.update()
        if (Styles.isDrawTopAgents) {
            TopAgentsDisplay.draw()
        }
        if (Config.isHighlighActionLimit) {
            ActionLimitsDisplay.draw()
        } else {
            ActionLimitsDisplay.drawTop()
        }
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
            ctx.fillRect(fromX, fromY, toY, -toX) // argument switch
        }
        ctx.fill()
        ctx.strokeStyle = stroke
        ctx.lineWidth = lineWidth
        ctx.beginPath()
        with(rect) {
            ctx.strokeRect(fromX, fromY, toY, -toX) // argument switch
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
        ctx: Ctx,
        pos: Pos,
        text: String,
        fill: String,
        fontSize: Int,
        fontName: String = CODA,
        lineWidth: Double = 0.0,
        stroke: String = Colors.black,
        textAlign: CanvasTextAlign = CanvasTextAlign.START,
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
        ctx: Ctx,
        circle: Circle,
        stroke: String,
        lineWidth: Double,
        fill: String? = null,
        alpha: Double = 1.0,
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
        ctx: Ctx,
        path: Path2D,
        stroke: String,
        lineWidth: Double,
        fill: String? = null,
        alpha: Double = 1.0,
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
