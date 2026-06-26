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
        private fun isUpdateStuck(tick: Int) = tick % 20 == 0 // react quickly once flagged (not once a minute)
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
                logCheckpoint(enlMu, resMu)
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

        // The headline scoreboard line: both factions' MU at this checkpoint, each in its own colour (MAJOR
        // so it survives the "only key events" filter — the sustained-MU race is the whole game).
        private fun logCheckpoint(enlMu: Int, resMu: Int) {
            Com.addMessage(
                Com.Entry(
                    Com.Importance.MAJOR,
                    listOf(
                        Com.Segment("◆ Checkpoint — "),
                        Com.Segment("ENL $enlMu", Faction.ENL.color),
                        Com.Segment("  ·  "),
                        Com.Segment("RES $resMu", Faction.RES.color),
                        Com.Segment(" MU"),
                    ),
                ),
            )
        }

        // Neutral, density-driven portal churn (run every checkpoint). The board's portal count converges to
        // [Config.targetPortals]: discovery dominates when sparse (~4:1 near empty), evens to ~1:1 at target,
        // and removal dominates above it. d = count / target (1.0 at target); create fades as d rises, remove
        // grows — they cross at the target. Helps no faction directly; it just keeps the map alive + bounded.
        // The pure math lives in [ChurnMath]; this just supplies live World/Config state and acts on the result.

        private fun managePortalDensity() {
            val count = World.countPortals()
            // No room to place a non-clipping portal → don't even try (no wasted attempt); roll the would-be
            // discovery budget into REMOVAL instead, so a packed board thins out rather than stalling.
            val hasSpace = count < Config.maxPortals && Pos.hasPortalSpace()
            val (createChance, removeChance) =
                ChurnMath.churnChances(count, Config.targetPortals(), Config.portalChurnRate, hasSpace)
            if (hasSpace && Util.random() < createChance) {
                val discovered = Portal.createRandom()
                World.allPortals.add(discovered)
                Com.addMessage("A new portal $discovered was discovered.", Com.Importance.MAJOR, Com.NEUTRAL)
            }
            if (count > Config.minPortals && Util.random() < removeChance) {
                val gone = World.randomPortal()
                gone.remove()
                Com.addMessage("Portal $gone no longer exists.", Com.Importance.MAJOR, Com.NEUTRAL)
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
                        Com.addMessage("Portal $removed quit the game.", Com.Importance.MINOR, Com.NEUTRAL)
                    } else {
                        Com.addMessage("Portal $removed has left ${removed.faction.abbr}.", Com.Importance.MINOR, removed.faction.color)
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
                Com.addMessage("${xfAgent.name} restarted as ${xfAgent.faction.abbr}.", Com.Importance.MINOR, xfAgent.faction.color)
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
