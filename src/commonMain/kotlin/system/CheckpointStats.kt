package system

import agent.Faction

/**
 * Pure winner logic over the checkpoint history (the sustained-MU race). A faction "won" a checkpoint by
 * holding more MU through it; a CYCLE is won by whoever led the most checkpoints within it (our sim's take on
 * Ingress, where a cycle averages 35 checkpoints and the higher average wins). No World/DOM coupling → the
 * HUD ([system.ui.panel.HistoryPanel]) and any headless report share one tested implementation.
 */
object CheckpointStats {
    /** The faction leading on MU at [cp], or null on a tie (incl. 0–0 before any fields form). */
    fun winner(cp: Checkpoint): Faction? = when {
        cp.enlMu > cp.resMu -> Faction.ENL
        cp.resMu > cp.enlMu -> Faction.RES
        else -> null
    }

    /** Per-faction count of checkpoints led over [cps] (ties count for neither). */
    fun tally(cps: List<Checkpoint>): Map<Faction, Int> = mapOf(
        Faction.ENL to cps.count { winner(it) == Faction.ENL },
        Faction.RES to cps.count { winner(it) == Faction.RES },
    )

    /**
     * The winner of the cycle that ENDS at [endIndex] in [cps]: the faction that led the most checkpoints from
     * just after the previous cycle end up to and including [endIndex]. Null if [endIndex] is out of range,
     * isn't a cycle end, or the cycle is tied on checkpoint wins.
     */
    fun cycleWinner(cps: List<Checkpoint>, endIndex: Int): Faction? {
        if (endIndex !in cps.indices || !cps[endIndex].isCycleEnd) return null
        val prevEnd = (endIndex - 1 downTo 0).firstOrNull { cps[it].isCycleEnd } ?: -1
        val t = tally(cps.subList(prevEnd + 1, endIndex + 1))
        val enl = t.getValue(Faction.ENL)
        val res = t.getValue(Faction.RES)
        return when {
            enl > res -> Faction.ENL
            res > enl -> Faction.RES
            else -> null
        }
    }

    /** Per-faction count of CYCLES won over [cps] (each cycle-end index resolved via [cycleWinner]). */
    fun cycleTally(cps: List<Checkpoint>): Map<Faction, Int> {
        val winners = cps.indices.filter { cps[it].isCycleEnd }.mapNotNull { cycleWinner(cps, it) }
        return mapOf(
            Faction.ENL to winners.count { it == Faction.ENL },
            Faction.RES to winners.count { it == Faction.RES },
        )
    }
}
