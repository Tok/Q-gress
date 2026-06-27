package ai

import config.Config
import system.grid.GridFixture
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Headless tuning sweep (run on demand — it's slow, so it lives apart from the gate). Plays BALANCED matches
 * (both factions on the identical default policy + fair turn order) across a grid of combat-tuning values and
 * scores each for the dynamic-but-fair ideal: the MU lead should ALTERNATE at a healthy rate (not static, not
 * chaotic), the two factions should SHARE the lead ~50/50, and real MU should form (fields, not 0/0). Prints a
 * ranked table; pick the top combo as the Config defaults. Re-run after mechanic changes.
 */
class BalanceSweep {

    private fun grid() = GridFixture("SWEEP", 90, 60, 2, GridFixture.rleEncode(List(90 * 60) { true })).toGrid()

    private data class Score(val composite: Double, val leadChanges: Double, val balance: Double, val live: Double, val mu: Int)

    // Dynamism score for one match: liveness × balance × rate-sweetness (peaks at TARGET_RATE lead changes
    // per live checkpoint; 0 when static or chaotic). 0..1, higher = closer to the dynamic-but-fair ideal.
    private fun scoreMatch(r: MatchResult): Score {
        val cps = r.checkpoints
        val leaders = cps.map {
            if (it.enlMu == it.resMu) {
                0
            } else if (it.enlMu > it.resMu) {
                1
            } else {
                -1
            }
        }
        val liveCps = cps.count { it.enlMu + it.resMu > 0 }
        if (liveCps == 0) return Score(0.0, 0.0, 0.0, 0.0, 0)
        val nonTie = leaders.filter { it != 0 }
        val leadChanges = nonTie.zipWithNext().count { (a, b) -> a != b }.toDouble()
        val enlLead = leaders.count { it == 1 }
        val resLead = leaders.count { it == -1 }
        val balance = if (enlLead + resLead > 0) 1.0 - abs(enlLead - resLead).toDouble() / (enlLead + resLead) else 0.0
        val liveness = liveCps.toDouble() / cps.size
        val changeRate = leadChanges / liveCps
        val sweetness = (1.0 - abs(changeRate - TARGET_RATE) / TARGET_RATE).coerceIn(0.0, 1.0)
        val mu = cps.sumOf { it.enlMu + it.resMu } / cps.size
        return Score(liveness * balance * sweetness, leadChanges, balance, liveness, mu)
    }

    @Test
    fun sweep() {
        if (!RUN) return // flip RUN=true (or run this test explicitly) to execute the sweep
        val grid = grid()
        val seeds = 1..4
        val results = mutableListOf<Pair<String, Score>>()
        // Hold the tuned optimum; sweep the leader tempo handicap (0 = off = the current default).
        Config.combatDynamism = 0.6
        Config.comebackMax = 3.0
        Config.dominanceDecay = 3.0
        for (ld in listOf(0.0, 0.25, 0.5, 0.75, 1.0)) {
            Config.leaderDistraction = ld
            val scores = seeds.map { seed ->
                val r = SimRunner.runMatch(grid, seed = seed, maxTicks = 6000, setup = MatchSetup(frogs = 12, smurfs = 12, npcs = 20))
                SimRunner.reset()
                scoreMatch(r)
            }
            val avg = Score(
                scores.map { it.composite }.average(),
                scores.map { it.leadChanges }.average(),
                scores.map { it.balance }.average(),
                scores.map { it.live }.average(),
                scores.map { it.mu }.average().toInt(),
            )
            results.add("leaderDistraction=$ld" to avg)
        }
        results.sortedByDescending { it.second.composite }.forEach { (k, s) ->
            println(
                "SWEEP $k -> score=${fmt(
                    s.composite,
                )} leadChanges=${fmt(s.leadChanges)} balance=${fmt(s.balance)} live=${fmt(s.live)} avgMu=${s.mu}",
            )
        }
        assertTrue(true)
    }

    private fun fmt(d: Double) = ((d * 100).toInt() / 100.0).toString()

    companion object {
        private const val RUN = false // set true (or run this test explicitly) to execute the slow sweep
        private const val TARGET_RATE = 0.3 // ideal MU-lead changes per live checkpoint (a change every ~3)
    }
}
