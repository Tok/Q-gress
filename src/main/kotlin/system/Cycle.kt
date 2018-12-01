package system

import Canvas
import Ctx
import World
import agent.Agent
import agent.Faction
import agent.action.ActionItem
import config.Colors
import config.Config
import config.Dim
import org.w3c.dom.CanvasRenderingContext2D
import portal.XmHeap
import portal.XmMap
import util.DrawUtil
import util.HtmlUtil
import util.SoundUtil
import util.Util
import util.data.Coords
import util.data.Line

enum class Cycle(val checkpoints: MutableMap<Int, Checkpoint>, var image: Canvas?) {
    INSTANCE(mutableMapOf(), null);

    companion object {
        private const val numberOfCheckpoints = 35
        private fun isUpdateStuck(tick: Int) = tick % 60 == 0
        private fun isNewCheckpoint(tick: Int) = tick % Config.ticksPerCheckpoint == 0
        private fun isNewCycle(tick: Int) = tick % Config.ticksPerCycle == 0
        fun updateCheckpoints(tick: Int, enlMu: Int, resMu: Int) {
            if (isUpdateStuck(tick)) {
                World.allAgents.filter { it.action.item == ActionItem.MOVE }
                        .forEach { it.updateLastPos() }
            }
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
                    removePortals()
                    removeFrogs()
                    removeSmurfs()
                    factionChange()
                    SoundUtil.playCycleSound()
                    World.allPortals.forEach { it.decay() }
                } else {
                    SoundUtil.playCheckpointSound(cp)
                }
            }
        }

        private fun removePortals() {
            if (Util.random() <= Config.portalRemovalRate) {
                val ratio = World.countPortals() / Config.maxPortals
                if (Util.random() <= ratio) {
                    val deprecated = World.randomPortal()
                    deprecated.remove()
                    Com.addMessage("Portal $deprecated no longer exists.")
                }
            }
        }

        private fun removeAgents(agents: Set<Agent>, minCount: Int, maxCount: Int, fc: Boolean = false) {
            val count = agents.count()
            if (count < minCount) {
                val ratio = count / maxCount
                if (Util.random() <= ratio) {
                    val selection = agents.sortedBy { it.getLevel() }.takeLast(count - maxCount)
                    val removed = selection.shuffled().first()
                    if (fc) {
                        Com.addMessage("Portal $removed quit the game.")
                    } else {
                        Com.addMessage("Portal $removed has left ${removed.faction.abbr}.")
                    }
                }
            }
        }

        private fun removeFrogs() {
            if (Util.random() <= Config.frogQuitRate) {
                removeAgents(World.frogs, Config.minFrogs, Config.maxFrogs)
            }
        }

        private fun removeSmurfs() {
            if (Util.random() <= Config.smurfQuitRate) {
                removeAgents(World.smurfs, Config.minFrogs, Config.maxFrogs)
            }
        }

        private fun factionChange() {
            if (Util.random() <= Config.factionChangeRate) {
                val xfAgent = if (Util.randomBool()) {
                    removeAgents(World.frogs, Config.minFrogs, Config.maxFrogs, true)
                    Agent.createSmurf(World.grid)
                } else {
                    removeAgents(World.smurfs, Config.minFrogs, Config.maxFrogs, true)
                    Agent.createFrog(World.grid)
                }
                Com.addMessage("${xfAgent.name} restarted as ${xfAgent.faction.abbr}.")
                World.allAgents.add(xfAgent)
            }
        }

        private fun spawnXm() {
            World.allPortals.map { it.leakXm() }
                    .flatMap { (pos, xm) ->
                        val heapCount = xm / XmHeap.capacity
                        (0..heapCount).map {
                            pos.randomNearPoint(Dim.portalXmSpawnRadius)
                        }
                    }.forEach { XmMap.createStrayXm(it, true) }

            World.allNonFaction.filterNot { it.pos.isOffScreen() }
                    .shuffled()
                    .take((World.allNonFaction.size * Config.npcXmSpawnRatio).toInt())
                    .map { XmMap.createStrayXm(it.pos.randomNearPoint(Dim.npcXmSpawnRadius), false) }
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
