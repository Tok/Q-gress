package util

import agent.Faction
import config.*
import extension.Ctx
import org.w3c.dom.*
import system.display.Scene3D
import util.data.Circle
import util.data.Line
import util.data.Pos
import util.ui.AiPanel
import util.ui.AudioPanel
import util.ui.BrainsPanel
import util.ui.DebugHud
import util.ui.HistoryPanel
import util.ui.Inspector
import util.ui.LeaderboardPanel
import util.ui.LlmReasoningPanel
import util.ui.PortalsPanel
import util.ui.SliderHistoryPanel
import util.ui.StatsPanel
import util.ui.TopAgentsPanel
import util.ui.TrainerPanel
import util.ui.TuningPanel
import kotlin.math.PI

object DrawUtil {
    const val CODA = "Coda"

    fun redraw() {
        // The world renders in the three.js custom layer (Scene3D); the HUD is DOM. No 2D canvas
        // to paint or clear anymore — just drive the 3D sync and the inspector.
        Scene3D.sync()
        Inspector.refresh()
    }

    fun redrawUserInterface(firstMu: Int, secondMu: Int, factions: Pair<Faction, Faction>) {
        // The whole HUD is DOM: MindUnits + tick + Com log (StatsPanel), the per-metric history
        // dashboard (HistoryPanel/uPlot — MU + Portals/Links/Fields/Agents over time, with live
        // values), and the top-agents table (TopAgentsPanel).
        StatsPanel.update(firstMu, secondMu, factions)
        HistoryPanel.update()
        TuningPanel.refresh() // mirror an AI driver's vector onto the sliders (no-op under manual control)
        AiPanel.update() // AI footer tab: per-faction driver + live observation readout
        BrainsPanel.update() // BRAINS footer tab: per-faction driver summary + live NN activation / LLM reasoning
        TrainerPanel.update() // TRAIN footer tab: lazy-build the in-browser neuro-evolution trainer
        LeaderboardPanel.update() // TRAIN footer tab: the driver leaderboard (round-robin ranking)
        SliderHistoryPanel.update() // AI tab: each slider's value over the checkpoint window
        LlmReasoningPanel.update() // AI tab: the LLM driver's prompt/reply/parsed reasoning
        AudioPanel.update() // AUDIO footer tab: master-FX control surface, live scope/spectrum + tuning export
        if (Styles.isDrawTopAgents) {
            TopAgentsPanel.update()
            PortalsPanel.update() // PORTALS footer tab: every portal as a sortable table (like AGENTS)
        }
        DebugHud.update() // no-op unless ?debug
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

    fun drawCircle(ctx: Ctx, circle: Circle, stroke: String, lineWidth: Double, fill: String? = null, alpha: Double = 1.0) {
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
