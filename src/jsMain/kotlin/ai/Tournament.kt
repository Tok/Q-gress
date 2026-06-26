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
        val session = Session(grid, drivers, seeds, ticks, setup)
        while (!session.done) session.step()
        return session.standings()
    }

    /**
     * A resumable round-robin: one [step] = one match, so the in-game leaderboard can drive it from a UI loop
     * (a `setTimeout` per match) and show progress between matches. [roundRobin] is just a `while` over this.
     * Every unordered pair plays both faction assignments per seed; deterministic given (grid, drivers, seeds).
     */
    class Session(
        private val grid: Grid,
        drivers: List<Driver>,
        seeds: List<Int>,
        private val ticks: Int = Config.ticksPerCycle,
        private val setup: MatchSetup = MatchSetup(),
    ) {
        // The full match list up front: each pair, both colour assignments, per seed (fair + order-independent).
        private val schedule: List<Match> = buildList {
            for (i in drivers.indices) {
                for (j in i + 1 until drivers.size) {
                    seeds.forEach { seed ->
                        add(Match(drivers[i], drivers[j], seed))
                        add(Match(drivers[j], drivers[i], seed))
                    }
                }
            }
        }
        private val standings = drivers.associate { it.name to Standing(it.name) }

        var played = 0
            private set
        val total get() = schedule.size
        val done get() = played >= schedule.size

        /** Play the next scheduled match (recording both drivers' results); a no-op once [done]. */
        fun step() {
            if (done) return
            val m = schedule[played]
            val result = SimRunner.runMatch(grid, m.seed, ticks, setup, m.enl.policy(Faction.ENL), m.res.policy(Faction.RES))
            val enlMu = result.checkpointMuSum(Faction.ENL)
            val resMu = result.checkpointMuSum(Faction.RES)
            standings.getValue(m.enl.name).record(enlMu, resMu)
            standings.getValue(m.res.name).record(resMu, enlMu)
            played++
        }

        /** The current standings, ranked by average MU margin (ties broken by wins). */
        fun standings(): List<Standing> =
            standings.values.sortedWith(compareByDescending<Standing> { it.avgMargin() }.thenByDescending { it.wins })

        private class Match(val enl: Driver, val res: Driver, val seed: Int)
    }
}
