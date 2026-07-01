package ai

import World
import agent.Agent
import agent.Faction
import config.Config

/**
 * A fixed, normalized feature vector describing the match from one faction's point of view — the NN/LLM
 * INPUT (PLAN Phase 6.0). Read-only + deterministic given world state (it reads the [World] singleton but
 * mutates nothing), so the same state yields the same vector. Every feature is already in 0..1 (shares,
 * fractions), so no external scaling is needed, and [SIZE] is fixed so a trained net's input layer stays
 * valid as long as the layout below is unchanged.
 */
object Observation {
    /** Length of [observe]'s vector — keep in sync with the `doubleArrayOf` below. */
    const val SIZE = 13

    // Named slot indices into [observe]'s vector, for consumers that read individual features by hand (e.g.
    // [HeuristicTune]) — keep in sync with the layout comments in [observe].
    const val SLOT_MU_SHARE = 1 // Mind-Unit dominance (the headline score)
    const val SLOT_AVG_XM = 11 // our average agent XM (fraction of capacity)

    private const val MAX_LEVEL = 8.0

    /** The 0..1 feature vector for [faction] vs its enemy, from the current [World] state. */
    fun observe(faction: Faction): DoubleArray {
        val enemy = faction.enemy()
        val totalPortals = World.countPortals().coerceAtLeast(1).toDouble()
        return doubleArrayOf(
            // 0: where we are in the scoring cycle (0 → start, 1 → end)
            (World.tick % Config.ticksPerCycle).toDouble() / Config.ticksPerCycle,
            // 1: Mind-Unit dominance (the headline score)
            share(World.calcTotalMu(faction).toDouble(), World.calcTotalMu(enemy).toDouble()),
            // 2-4: portal control — ours / enemy's / still neutral
            World.countPortals(faction) / totalPortals,
            World.countPortals(enemy) / totalPortals,
            World.unclaimedPortals().count() / totalPortals,
            // 5-6: link + field dominance
            share(World.countLinks(faction).toDouble(), World.countLinks(enemy).toDouble()),
            share(World.countFields(faction).toDouble(), World.countFields(enemy).toDouble()),
            // 7-8: roster fill (agents vs the per-faction cap)
            rosterFill(faction),
            rosterFill(enemy),
            // 9-10: average agent level (vs L8)
            avgLevel(faction),
            avgLevel(enemy),
            // 11-12: average agent XM (fraction of capacity)
            avgXm(faction),
            avgXm(enemy),
        )
    }

    /** [mine] / ([mine] + [theirs]); a neutral 0.5 when neither side has any. */
    private fun share(mine: Double, theirs: Double): Double {
        val total = mine + theirs
        return if (total <= 0.0) 0.5 else mine / total
    }

    private fun agentsOf(faction: Faction): List<Agent> = World.allAgents.filter { it.faction == faction }

    private fun rosterFill(faction: Faction): Double = agentsOf(faction).size / Config.maxFor(faction).coerceAtLeast(1).toDouble()

    private fun avgLevel(faction: Faction): Double {
        val agents = agentsOf(faction)
        return if (agents.isEmpty()) 0.0 else agents.map { it.getLevel() }.average() / MAX_LEVEL
    }

    private fun avgXm(faction: Faction): Double {
        val agents = agentsOf(faction)
        return if (agents.isEmpty()) {
            0.0
        } else {
            agents.map { it.xm.toDouble() / it.xmCapacity().coerceAtLeast(1) }.average()
        }
    }
}
