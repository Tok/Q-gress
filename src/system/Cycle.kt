package system

import Canvas
import Ctx
import agent.Faction
import config.Dim
import config.Time
import org.w3c.dom.CanvasRenderingContext2D
import util.DrawUtil
import util.HtmlUtil
import util.SoundUtil
import util.data.Coords
import util.data.Line

enum class Cycle(val checkpoints: MutableMap<Int, Checkpoint>, var image: Canvas?) {
    INSTANCE(mutableMapOf(), null);

    companion object {
        private const val xmPerCycle = 100 //FIXME tune
        private const val durationH = 175
        private const val numberOfCheckpoints = 35
        private val ticksPerCheckpoint = Time.secondsToTicks(300) //TODO tune
        private fun isNew(tick: Int) = tick % ticksPerCheckpoint == 0
        fun updateCheckpoints(tick: Int, enlMu: Int, resMu: Int) {
            if (isNew(tick)) {
                val cp = Checkpoint(enlMu, resMu)
                val limit = numberOfCheckpoints - 1
                val old = INSTANCE.checkpoints.toList().sortedBy { tick }.takeLast(limit)
                INSTANCE.checkpoints.clear()
                INSTANCE.checkpoints.putAll(old)
                INSTANCE.checkpoints[tick] = cp
                SoundUtil.playCheckpointSound(cp)
                INSTANCE.image = createImage()

                val atPortal = World.allAgents.filter { it.isAtActionPortal() }
                atPortal.forEach { it.addXm(it.actionPortal.leakXm()) }

                val notAtPortal = World.allAgents.filterNot { it.isAtActionPortal() }
                notAtPortal.forEach { agent ->
                    val closeNpcs = World.allNonFaction.filter { npc -> npc.pos.distanceTo(agent.pos) < Dim.agentRadius }
                    val xm = closeNpcs.count() * 50
                    agent.addXm(xm)
                }
            }
        }

        private fun createImage(): Canvas {
            val h = Dim.cycleH + 8
            val w = 7 * Cycle.numberOfCheckpoints + 8
            val lineAlpha = 0.5
            val dotAlpha = 0.5
            val lineWidth = 1.0
            val r = 2.0

            fun drawCheckpointDot(ctx: Ctx, pos: Coords, style: String) {
                val circle = util.data.Circle(pos, r)
                util.DrawUtil.drawCircle(ctx, circle, config.Colors.black, lineWidth, style, dotAlpha)
            }

            fun drawCheckpoint(ctx: CanvasRenderingContext2D, index: Int, withNext: Pair<Checkpoint, Checkpoint>, maxTotal: Int) {
                fun calcY(mu: Int, maxTotal: Int) = Dim.cycleH - (mu * Dim.cycleH / maxTotal)
                val w = 7
                val x = (index * w)
                Faction.all().forEach { faction ->
                    val y = calcY(withNext.first.mu(faction), maxTotal)
                    val current = Coords(x, y + r.toInt())
                    val nextY = calcY(withNext.second.mu(faction), maxTotal)
                    val next = Coords(x + w, nextY + r.toInt())
                    if (index > 0) {
                        DrawUtil.drawLine(ctx, Line(current, next), faction.color, lineWidth, lineAlpha)
                    }
                    drawCheckpointDot(ctx, next, faction.color)
                }
            }

            return HtmlUtil.preRender(w, h, fun(ctx: Ctx) {
                val checkpoints = Cycle.INSTANCE.checkpoints
                val maxTotal = checkpoints.values.maxBy { it.total() }?.total() ?: 0
                checkpoints.values.zipWithNext().mapIndexed { i, pair ->
                    drawCheckpoint(ctx, i, pair, maxTotal)
                }
            })
        }
    }
}
