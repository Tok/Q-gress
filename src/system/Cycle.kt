package system

import Canvas
import Ctx
import World
import agent.Faction
import config.Colors
import config.Config
import config.Dim
import config.Time
import org.w3c.dom.CanvasRenderingContext2D
import portal.XmHeap
import portal.XmMap
import util.DrawUtil
import util.HtmlUtil
import util.SoundUtil
import util.data.Coords
import util.data.Line

enum class Cycle(val checkpoints: MutableMap<Int, Checkpoint>, var image: Canvas?) {
    INSTANCE(mutableMapOf(), null);

    companion object {
        private const val numberOfCheckpoints = 35
        private fun isNewCheckpoint(tick: Int) = tick % Config.ticksPerCheckpoint == 0
        private fun isNewCycle(tick: Int) = tick % Config.ticksPerCycle == 0
        fun updateCheckpoints(tick: Int, enlMu: Int, resMu: Int) {
            if (isNewCheckpoint(tick)) {
                val cp = Checkpoint(enlMu, resMu, isNewCycle(tick))
                val limit = numberOfCheckpoints - 1
                val old = INSTANCE.checkpoints.toList().sortedBy { tick }.takeLast(limit)
                INSTANCE.checkpoints.clear()
                INSTANCE.checkpoints.putAll(old)
                INSTANCE.checkpoints[tick] = cp
                INSTANCE.image = createImage()
                if (cp.isCycleEnd) {
                    spawnXm()
                    SoundUtil.playCycleSound()
                    World.allPortals.forEach { it.decay() }
                } else {
                    SoundUtil.playCheckpointSound(cp)
                }
            }
        }

        private const val npcXmSpawnRatio = 0.05
        private fun spawnXm() {
            World.allPortals.map { it.leakXm() }
                    .flatMap { (pos, xm) ->
                        val heapCount = xm / XmHeap.capacity
                        (0..heapCount).map {
                            pos.randomNearPoint(Dim.portalXmSpawnRadius)
                        }
                    }.forEach { XmMap.createStrayXm(it) }

            World.allNonFaction.filterNot { it.pos.isOffScreen() }
                    .shuffled()
                    .take((World.allNonFaction.size * npcXmSpawnRatio).toInt())
                    .map { XmMap.createStrayXm(it.pos.randomNearPoint(Dim.npcXmSpawnRadius)) }
        }

        val ww = 8
        private fun createImage(): Canvas {
            val off = 4
            val h = Dim.cycleH + (2 * off)
            val w = (ww * Cycle.numberOfCheckpoints - 1) + (2 * off)
            val lineAlpha = 0.5
            val dotAlpha = 0.5
            val lineWidth = 1.0
            val r = 2.0

            fun drawCheckpointDot(ctx: Ctx, pos: Coords, style: String, isCycleEnded: Boolean) {
                val radius = if (isCycleEnded) r + 1 else r
                val circle = util.data.Circle(pos, radius)
                util.DrawUtil.drawCircle(ctx, circle, config.Colors.black, lineWidth, style, dotAlpha)
            }

            fun drawCheckpoint(ctx: CanvasRenderingContext2D, index: Int, withNext: Pair<Checkpoint, Checkpoint>, maxTotal: Int) {
                fun calcY(mu: Int, maxTotal: Int) = Dim.cycleH - (mu * Dim.cycleH / maxTotal)
                val x = (index * ww)
                Faction.all().forEach { faction ->
                    val y = calcY(withNext.first.mu(faction), maxTotal)
                    val current = Coords(x, y + r.toInt() + 2)
                    val nextY = calcY(withNext.second.mu(faction), maxTotal)
                    val next = Coords(x + ww, nextY + r.toInt() + 2)
                    val top = Coords(x + ww, 0)
                    val bot = Coords(x + ww, h - 3)
                    val lw = if (withNext.second.isCycleEnd) 2.0 else 0.3
                    DrawUtil.drawLine(ctx, Line(top, bot), Colors.white, lw, 0.3)
                    if (index > 0) {
                        DrawUtil.drawLine(ctx, Line(current, next), faction.color, lineWidth, lineAlpha)
                    }
                    drawCheckpointDot(ctx, next, faction.color, withNext.second.isCycleEnd)
                }
            }

            fun drawBackground(ctx: Ctx) {
                    DrawUtil.drawRect(ctx, Coords(0, 0), -h.toDouble(), w.toDouble() - 8,
                            "#00000077", "#00000077", 0.0)
            }

            fun drawBaseLine(ctx: Ctx) {
                val y = h - off
                val from = Coords(off, y)
                val to = Coords(w - off - 8, y)
                DrawUtil.drawLine(ctx, Line(from, to), Colors.white, 2.0, 0.3)
            }

            return HtmlUtil.preRender(w, h, fun(ctx: Ctx) {
                val checkpoints = Cycle.INSTANCE.checkpoints
                val maxTotal = checkpoints.values.maxBy { it.total() }?.total() ?: 0
                drawBackground(ctx)
                drawBaseLine(ctx)
                checkpoints.values.zipWithNext().mapIndexed { i, pair ->
                    drawCheckpoint(ctx, i, pair, maxTotal)
                }
            })
        }
    }
}
