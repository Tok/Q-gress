package system

import agent.Faction

data class Checkpoint(val enlMu: Int, val resMu: Int, val isCycleEnd: Boolean) {
    fun total(): Int = enlMu + resMu
    fun mu(faction: Faction) = when (faction) {
        Faction.ENL -> enlMu
        Faction.RES -> resMu
        Faction.NONE -> 0
    }

    companion object {
        val durationH = 5
    }
}
