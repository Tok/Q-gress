package system

import World
import agent.Agent
import agent.Faction
import agent.action.ActionItem
import config.Config
import config.Dim
import portal.Portal
import portal.XmHeap
import portal.XmMap
import util.SoundUtil
import util.Util
import util.data.Pos

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
                    .forEach { it.recoverIfStuck() }
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
                spawnXm() // every checkpoint (not just cycle end) so agent XM is replenished mid-cycle
                World.allPortals.toList().forEach { it.erodeByDominance() } // the leader's empire erodes → board reopens
                managePortalDensity() // neutral portal discovery + removal, density-driven toward the target
                if (cp.isCycleEnd) {
                    removeFrogs()
                    removeSmurfs()
                    factionChange()
                    SoundUtil.playCycleSound() // SoundUtil self-guards headless (isMuted())
                    World.allPortals.forEach { it.decay() }
                } else {
                    SoundUtil.playCheckpointSound(cp)
                }
            }
        }

        // Neutral, density-driven portal churn (run every checkpoint). The board's portal count converges to
        // [Config.targetPortals]: discovery dominates when sparse (~4:1 near empty), evens to ~1:1 at target,
        // and removal dominates above it. d = count / target (1.0 at target); create fades as d rises, remove
        // grows — they cross at the target. Helps no faction directly; it just keeps the map alive + bounded.
        private fun managePortalDensity() {
            val count = World.countPortals()
            val d = count / Config.targetPortals().toDouble()
            val createChance = Config.portalChurnRate * (1.0 - d / 2.0).coerceIn(0.0, 1.0)
            // No room to place a non-clipping portal → don't even try (no wasted attempt); roll the would-be
            // discovery budget into REMOVAL instead, so a packed board thins out rather than stalling.
            val hasSpace = count < Config.maxPortals && Pos.hasPortalSpace()
            val removeChance = Config.portalChurnRate * (d / 2.0).coerceIn(0.0, 1.0) + if (hasSpace) 0.0 else createChance
            if (hasSpace && Util.random() < createChance) {
                val discovered = Portal.createRandom()
                World.allPortals.add(discovered)
                Com.addMessage("A new portal $discovered was discovered.")
            }
            if (count > Config.minPortals && Util.random() < removeChance) {
                val gone = World.randomPortal()
                gone.remove()
                Com.addMessage("Portal $gone no longer exists.")
            }
        }

        private fun removeAgents(agents: Set<Agent>, minCount: Int, maxCount: Int, fc: Boolean = false) {
            val count = agents.count()
            if (count < minCount) {
                val ratio = count / maxCount
                if (Util.random() <= ratio) {
                    val selection = agents.sortedBy { it.getLevel() }.takeLast(count - maxCount)
                    val removed = Util.shuffle(selection).first() // seeded — NOT stdlib shuffled() (unseeded → nondeterministic)
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
            val mult = Config.strayXmMultiplier
            World.allPortals.map { it.leakXm() }
                .flatMap { (pos, xm) ->
                    val heapCount = (xm / XmHeap.capacity * mult).toInt()
                    (0..heapCount).map {
                        pos.randomNearPoint(Dim.portalXmSpawnRadius)
                    }
                }.forEach { XmMap.createStrayXm(it, true) }

            Util.shuffle(World.allNonFaction.filterNot { it.pos.isOffScreen() }) // seeded; stdlib shuffled() is unseeded
                .take((World.allNonFaction.size * Config.npcXmSpawnRatio * mult).toInt())
                .map { XmMap.createStrayXm(it.pos.randomNearPoint(Dim.npcXmSpawnRadius), false) }
        }
    }
}
