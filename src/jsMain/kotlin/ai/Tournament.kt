package ai

import agent.Faction
import config.Config
import extension.Grid

/** A named driver in a [Tournament]: [policy] builds a fresh [FactionPolicy] for the given faction (null = the
 *  default uniform-slider baseline). The factory is called per match so each gets its own policy instance. */
class Driver(val name: String, val policy: (Faction) -> FactionPolicy?)

/** One driver's standing after a [Tournament]: its record + summed Mind-Unit margin across its matches. */
class Standing(val name: String) {
    var matches = 0
        private set
    var wins = 0
        private set
    var losses = 0
        private set
    var ties = 0
        private set
    var muFor = 0L
        private set
    var muAgainst = 0L
        private set

    /** Average per-match MU margin (for − against) — the ranking key (ties broken by wins). */
    fun avgMargin(): Double = if (matches == 0) 0.0 else (muFor - muAgainst).toDouble() / matches

    internal fun record(forMu: Int, againstMu: Int) {
        matches++
        muFor += forMu
        muAgainst += againstMu
        when {
            forMu > againstMu -> wins++
            forMu < againstMu -> losses++
            else -> ties++
        }
    }
}

/**
 * Headless **driver tournament** (PLAN Phase 6.4): pits AI drivers (Manual baseline / Heuristic / a trained
 * Net / …) against each other over seeded [SimRunner] matches and ranks them by average per-checkpoint MU
 * margin — the same fitness objective the trainer optimizes. Every unordered pair plays **both** faction
 * assignments per seed (so colour/turn-order can't bias the result), and the run is fully deterministic given
 * (grid, drivers, seeds). Pure over the headless harness — the live in-game benchmark wraps this in a
 * [system.WorldSnapshot] capture/restore so it doesn't disturb the player's game.
 */
object Tournament {

    fun roundRobin(
        grid: Grid,
        drivers: List<Driver>,
        seeds: List<Int>,
        ticks: Int = Config.ticksPerCycle,
        setup: MatchSetup = MatchSetup(),
    ): List<Standing> {
        val round = Round(grid, ticks, setup, drivers.associate { it.name to Standing(it.name) })
        for (i in drivers.indices) {
            for (j in i + 1 until drivers.size) {
                val a = drivers[i]
                val b = drivers[j]
                seeds.forEach { seed ->
                    play(round, a, b, seed) // a = ENL, b = RES
                    play(round, b, a, seed) // swapped, same seed → fair
                }
            }
        }
        return round.standings.values.sortedWith(compareByDescending<Standing> { it.avgMargin() }.thenByDescending { it.wins })
    }

    // The invariants shared by every match of one round-robin (so [play] stays a small signature).
    private class Round(val grid: Grid, val ticks: Int, val setup: MatchSetup, val standings: Map<String, Standing>)

    private fun play(round: Round, enl: Driver, res: Driver, seed: Int) {
        val result = SimRunner.runMatch(round.grid, seed, round.ticks, round.setup, enl.policy(Faction.ENL), res.policy(Faction.RES))
        val enlMu = result.checkpointMuSum(Faction.ENL)
        val resMu = result.checkpointMuSum(Faction.RES)
        round.standings.getValue(enl.name).record(enlMu, resMu)
        round.standings.getValue(res.name).record(resMu, enlMu)
    }
}
