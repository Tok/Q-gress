package system

import agent.Faction

/**
 * A point-in-time snapshot of the match, taken every `Config.ticksPerCheckpoint` ticks and kept in a
 * rolling window ([Cycle.checkpoints]). Beyond MU (the headline "covered area" score) it now also
 * records per-faction entity counts so the history dashboard can graph every metric over time, not
 * just MU. New count fields default to 0 so older two-arg construction (e.g. tests) still compiles.
 */
data class Checkpoint(
    val enlMu: Int,
    val resMu: Int,
    val isCycleEnd: Boolean,
    val enlPortals: Int = 0,
    val resPortals: Int = 0,
    val enlLinks: Int = 0,
    val resLinks: Int = 0,
    val enlFields: Int = 0,
    val resFields: Int = 0,
    val enlAgents: Int = 0,
    val resAgents: Int = 0,
) {
    fun total(): Int = enlMu + resMu
    fun mu(faction: Faction) = when (faction) {
        Faction.ENL -> enlMu
        Faction.RES -> resMu
    }

    companion object {
        val durationH = 5
    }
}
