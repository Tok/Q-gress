package util

import World
import agent.Faction
import config.*
import extension.Canvas
import extension.Ctx
import extension.clear
import org.w3c.dom.*
import system.display.Scene3D
import util.data.Circle
import util.data.Line
import util.data.Pos
import util.ui.HistoryPanel
import util.ui.Inspector
import util.ui.StatsPanel
import util.ui.TopAgentsPanel
import kotlin.math.PI

object DrawUtil {
    const val CODA = "Coda"

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

    fun redrawUserInterface(firstMu: Int, secondMu: Int, factions: Pair<Faction, Faction>) {
        clearUserInterface()
        // The whole HUD is DOM: MindUnits + tick + Com log (StatsPanel), the per-metric history
        // dashboard (HistoryPanel/uPlot — MU + Portals/Links/Fields/Agents over time, with live
        // values), and the top-agents table (TopAgentsPanel).
        StatsPanel.update(firstMu, secondMu, factions)
        HistoryPanel.update()
        if (Styles.isDrawTopAgents) {
            TopAgentsPanel.update()
        }
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
