package system

import World
import agent.Agent
import agent.Faction
import agent.action.ActionItem
import config.Config
import config.Dim
import portal.XmHeap
import portal.XmMap
import util.SoundUtil
import util.Util

enum class Cycle(val checkpoints: MutableMap<Int, Checkpoint>) {
    INSTANCE(mutableMapOf()),
    ;

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
                val cp = Checkpoint(
                    enlMu, resMu, isNewCycle(tick),
                    World.countPortals(Faction.ENL), World.countPortals(Faction.RES),
                    World.countLinks(Faction.ENL), World.countLinks(Faction.RES),
                    World.countFields(Faction.ENL), World.countFields(Faction.RES),
                    World.countAgents(Faction.ENL), World.countAgents(Faction.RES),
                )
                val limit = numberOfCheckpoints - 1
                val old = INSTANCE.checkpoints.toList().sortedBy { tick }.takeLast(limit)
                INSTANCE.checkpoints.clear()
                INSTANCE.checkpoints.putAll(old)
                INSTANCE.checkpoints[tick] = cp
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
    }
}
