package system

import agent.Faction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CheckpointStatsTest {
    private fun cp(enl: Int, res: Int, cycleEnd: Boolean = false) = Checkpoint(enl, res, cycleEnd)

    @Test
    fun winnerIsTheMuLeaderOrNullOnTie() {
        assertEquals(Faction.ENL, CheckpointStats.winner(cp(10, 5)))
        assertEquals(Faction.RES, CheckpointStats.winner(cp(5, 10)))
        assertNull(CheckpointStats.winner(cp(7, 7)))
        assertNull(CheckpointStats.winner(cp(0, 0)))
    }

    @Test
    fun tallyCountsLeadsIgnoringTies() {
        val cps = listOf(cp(10, 5), cp(3, 9), cp(4, 4), cp(8, 1))
        val t = CheckpointStats.tally(cps)
        assertEquals(2, t.getValue(Faction.ENL))
        assertEquals(1, t.getValue(Faction.RES))
    }

    @Test
    fun cycleWinnerIsWhoLedMostCheckpointsInTheCycle() {
        val cps = listOf(
            cp(9, 1),
            cp(9, 1),
            cp(1, 9, cycleEnd = true), // cycle 1: ENL 2, RES 1 → ENL
            cp(1, 9),
            cp(1, 9),
            cp(9, 1, cycleEnd = true), // cycle 2: RES 2, ENL 1 → RES
        )
        assertEquals(Faction.ENL, CheckpointStats.cycleWinner(cps, 2))
        assertEquals(Faction.RES, CheckpointStats.cycleWinner(cps, 5))
        assertNull(CheckpointStats.cycleWinner(cps, 1), "not a cycle end → no winner")
    }

    @Test
    fun cycleWinnerIsNullOnATiedCycle() {
        val cps = listOf(cp(9, 1), cp(1, 9, cycleEnd = true)) // ENL 1, RES 1 → tie
        assertNull(CheckpointStats.cycleWinner(cps, 1))
    }

    @Test
    fun cycleTallyCountsWonCycles() {
        val cps = listOf(
            cp(9, 1), cp(9, 1), cp(1, 9, cycleEnd = true), // → ENL
            cp(1, 9), cp(1, 9), cp(9, 1, cycleEnd = true), // → RES
            cp(9, 1), cp(9, 1), cp(9, 1, cycleEnd = true), // → ENL
        )
        val t = CheckpointStats.cycleTally(cps)
        assertEquals(2, t.getValue(Faction.ENL))
        assertEquals(1, t.getValue(Faction.RES))
    }
}
