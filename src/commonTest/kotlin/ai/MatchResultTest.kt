package ai

import agent.Faction
import system.Checkpoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pure fitness/scoring maths over a synthetic checkpoint history — the objective the champion bake and
 * [Tournament] optimize. No World/DOM coupling, so it's exercised directly with hand-built [Checkpoint]s.
 */
class MatchResultTest {

    // ENL leads cp1 + cp2, RES leads cp3 → ENL nets +1 checkpoint. MU sums: ENL 190, RES 130.
    private fun sampleResult() = MatchResult(
        seed = 1,
        ticks = 300,
        checkpoints = listOf(
            Checkpoint(enlMu = 100, resMu = 50, isCycleEnd = false),
            Checkpoint(enlMu = 80, resMu = 20, isCycleEnd = false),
            Checkpoint(enlMu = 10, resMu = 60, isCycleEnd = true),
        ),
        finalEnlMu = 10,
        finalResMu = 60,
    )

    @Test
    fun checkpointMuSumAddsEachFactionsMu() {
        val r = sampleResult()
        assertEquals(190, r.checkpointMuSum(Faction.ENL))
        assertEquals(130, r.checkpointMuSum(Faction.RES))
    }

    @Test
    fun checkpointMuAvgDividesByCount() {
        val r = sampleResult()
        assertEquals(190.0 / 3, r.checkpointMuAvg(Faction.ENL))
        assertEquals(130.0 / 3, r.checkpointMuAvg(Faction.RES))
    }

    @Test
    fun checkpointMuAvgIsZeroWithNoCheckpoints() {
        val empty = MatchResult(seed = 0, ticks = 0, checkpoints = emptyList(), finalEnlMu = 0, finalResMu = 0)
        assertEquals(0.0, empty.checkpointMuAvg(Faction.ENL))
        assertEquals(0.0, empty.checkpointMuAvg(Faction.RES))
    }

    @Test
    fun checkpointWinMarginIsNetCheckpointsLed() {
        val r = sampleResult()
        assertEquals(1, r.checkpointWinMargin(Faction.ENL)) // led 2, lost 1
        assertEquals(-1, r.checkpointWinMargin(Faction.RES))
    }

    @Test
    fun checkpointFitnessAddsFractionalMuTiebreak() {
        val r = sampleResult()
        // margin +1, plus (190-130)/MU_TIEBREAK_SCALE — a fraction ≪ 1, so it never overturns a win diff.
        assertEquals(1 + 60 / MatchResult.MU_TIEBREAK_SCALE, r.checkpointFitness(Faction.ENL))
        assertEquals(-1 - 60 / MatchResult.MU_TIEBREAK_SCALE, r.checkpointFitness(Faction.RES))
    }

    @Test
    fun winnerIsWhoeverLedTheMostCheckpoints() {
        assertEquals(Faction.ENL, sampleResult().winner())
    }

    @Test
    fun winnerIsResWhenSmurfsLeadMore() {
        val r = MatchResult(
            seed = 2,
            ticks = 200,
            checkpoints = listOf(
                Checkpoint(enlMu = 10, resMu = 90, isCycleEnd = false),
                Checkpoint(enlMu = 5, resMu = 40, isCycleEnd = false),
            ),
            finalEnlMu = 5,
            finalResMu = 40,
        )
        assertEquals(Faction.RES, r.winner())
    }

    @Test
    fun winnerIsNullOnACheckpointTie() {
        val r = MatchResult(
            seed = 3,
            ticks = 100,
            checkpoints = listOf(
                Checkpoint(enlMu = 100, resMu = 50, isCycleEnd = false), // ENL
                Checkpoint(enlMu = 20, resMu = 70, isCycleEnd = false), // RES
                Checkpoint(enlMu = 40, resMu = 40, isCycleEnd = false), // tie — counts for neither
            ),
            finalEnlMu = 40,
            finalResMu = 40,
        )
        assertNull(r.winner())
    }
}
