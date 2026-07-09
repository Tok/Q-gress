package system
import World
import agent.Agent
import agent.Faction
import config.Config
import config.Dim
import portal.XmHeap
import portal.XmMap
import system.audio.Snd
import util.Rng
import util.data.*

enum class Cycle(val checkpoints: MutableMap<Int, Checkpoint>) {
    INSTANCE(mutableMapOf()),
    ;

    companion object {
        // The rolling history window = one full SCORING cycle, so the graph fills across a cycle then scrolls.
        private val numberOfCheckpoints = Config.checkpointsPerCycle
        private fun isUpdateStuck(tick: Int) = tick % 20 == 0 // react quickly once flagged (not once a minute)
        private fun isNewCheckpoint(tick: Int) = tick % Config.ticksPerCheckpoint == 0

        // GAME-EVENT cadence (portal decay + roster churn) — the short ~6-checkpoint beat, UNCHANGED.
        private fun isGameEventCycle(tick: Int) = tick % Config.ticksPerCycle == 0

        // SCORING/display cycle end: Ingress's 35 checkpoints. Drives Checkpoint.isCycleEnd (winner dots/lines)
        // + the deeper cycle sound. `tick > 0` so the very first checkpoint isn't a spurious cycle end.
        private fun isScoringCycleEnd(tick: Int) = tick > 0 && tick % (Config.checkpointsPerCycle * Config.ticksPerCheckpoint) == 0

        fun updateCheckpoints(tick: Int, enlMu: Int, resMu: Int) {
            if (isUpdateStuck(tick)) {
                World.allAgents.filter { it.isTravelling() }.forEach { it.recoverIfStuck() }
            }
            if (isNewCheckpoint(tick)) {
                val cp = Checkpoint(
                    enlMu, resMu, isScoringCycleEnd(tick),
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
                val lead = enlMu - resMu // VERBOSE TTS: who's ahead this checkpoint, and by how much
                if (lead != 0) Snd.sink.announceCheckpointLead(if (lead > 0) Faction.ENL else Faction.RES, if (lead > 0) lead else -lead)
                logCheckpoint(enlMu, resMu)
                spawnXm() // every checkpoint (not just cycle end) so agent XM is replenished mid-cycle
                World.allPortals.toList().forEach { it.erodeByDominance() } // the leader's empire erodes → board reopens
                // Portal discovery/removal is no longer a checkpoint tick — it's the agent DISCOVERY idle action now
                // (agent/action/cond/Discoverer, on a wander's arrival); density-driven toward Config.targetPortals.
                // Game events (portal decay + roster churn) stay on the short game-event cadence — NOT the
                // scoring cycle — so lengthening the scoring cycle doesn't make the board tankier.
                if (isGameEventCycle(tick)) {
                    removeFrogs()
                    removeSmurfs()
                    factionChange()
                    World.allPortals.forEach { it.decay() }
                }
                // The deeper cycle blip marks a SCORING cycle end (the big milestone); else the checkpoint blip.
                if (cp.isCycleEnd) Snd.sink.playCycleSound() else Snd.sink.playCheckpointSound(cp) // headless → NoOpAudio
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

        private fun removeAgents(agents: Set<Agent>, minCount: Int, maxCount: Int, fc: Boolean = false) {
            val count = agents.count()
            if (count < minCount) {
                val ratio = count / maxCount
                if (Rng.random() <= ratio) {
                    val selection = agents.sortedBy { it.getLevel() }.takeLast(count - maxCount)
                    val removed = Rng.shuffle(selection).first() // seeded — NOT stdlib shuffled() (unseeded → nondeterministic)
                    if (fc) {
                        Com.addMessage("Portal $removed quit the game.", Com.Importance.MINOR, Com.NEUTRAL)
                    } else {
                        Com.addMessage("Portal $removed has left ${removed.faction.abbr}.", Com.Importance.MINOR, removed.faction.color)
                    }
                }
            }
        }

        private fun removeFrogs() {
            if (Rng.random() <= Config.frogQuitRate) {
                removeAgents(World.frogs, Config.minFrogs, Config.maxFrogs)
            }
        }

        private fun removeSmurfs() {
            if (Rng.random() <= Config.smurfQuitRate) {
                removeAgents(World.smurfs, Config.minFrogs, Config.maxFrogs)
            }
        }

        private fun factionChange() {
            if (Rng.random() <= Config.factionChangeRate) {
                val xfAgent = if (Rng.randomBool()) {
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

            Rng.shuffle(World.allNonFaction.filterNot { it.pos.isOffScreen() }) // seeded; stdlib shuffled() is unseeded
                .take((World.allNonFaction.size * Config.npcXmSpawnRatio * mult).toInt())
                .map { XmMap.createStrayXm(it.pos.randomNearPoint(Dim.npcXmSpawnRadius), false) }
        }
    }
}
