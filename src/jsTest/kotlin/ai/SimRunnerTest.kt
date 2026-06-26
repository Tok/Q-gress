package ai

import agent.Faction
import util.GridFixture
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The headless match harness (PLAN Phase 6.1) — proves a full match runs in Node (no browser) without
 * crashing, captures per-checkpoint MU (the fitness signal), and is deterministic for a given seed. The
 * payoff of the functional-core split (effect sink + sync pathfinding + the shared [system.Simulation]).
 */
class SimRunnerTest {

    // A small all-passable arena: flow-field computes stay cheap (O(cells)) so the match runs well
    // under the test timeout. Full-resolution-grid throughput is the deferred "pathfinding scalability"
    // work (PLAN) — this test proves the harness runs deterministically, not match realism.
    private fun openGrid() = GridFixture("TEST", 40, 30, 2, GridFixture.rleEncode(List(40 * 30) { true })).toGrid()

    @AfterTest
    fun tearDown() = SimRunner.reset()

    @Test
    fun runsHeadlessAndCapturesCheckpoints() {
        val result = SimRunner.runMatch(openGrid(), seed = 42, maxTicks = 301, setup = MatchSetup(npcs = 10))

        assertEquals(301, result.ticks)
        // Checkpoints land every Config.ticksPerCheckpoint (300) ticks → at tick 0 and tick 300.
        assertTrue(result.checkpoints.size >= 2, "captured >= 2 checkpoints, got ${result.checkpoints.size}")
        assertTrue(
            result.checkpointMuSum(Faction.ENL) >= 0 && result.checkpointMuSum(Faction.RES) >= 0,
            "per-checkpoint MU sums are well-defined",
        )
    }

    @Test
    fun sameSeedIsDeterministic() {
        val a = SimRunner.runMatch(openGrid(), seed = 7, maxTicks = 150, setup = MatchSetup(npcs = 8))
        val b = SimRunner.runMatch(openGrid(), seed = 7, maxTicks = 150, setup = MatchSetup(npcs = 8))

        assertEquals(a.checkpoints, b.checkpoints, "same seed + grid → identical checkpoint history")
        assertEquals(a.finalEnlMu, b.finalEnlMu)
        assertEquals(a.finalResMu, b.finalResMu)
    }

    // An arena + seed that actually produce FIELDS (MU>0), so the determinism check below isn't vacuous —
    // the leak it guards (stdlib shuffled() is unseeded) only manifests once gameplay reaches fields/XM.
    private fun muGrid() = GridFixture("MU", 60, 40, 2, GridFixture.rleEncode(List(60 * 40) { true })).toGrid()

    @Test
    fun reproducibleAfterAnotherMatch() {
        val grid = muGrid()
        val a = SimRunner.runMatch(grid, seed = 1, maxTicks = 1800, setup = MatchSetup(npcs = 12))
        SimRunner.runMatch(grid, seed = 1001, maxTicks = 600) // a DIFFERENT match in between
        val b = SimRunner.runMatch(grid, seed = 1, maxTicks = 1800, setup = MatchSetup(npcs = 12))

        assertTrue(
            a.checkpointMuSum(Faction.ENL) + a.checkpointMuSum(Faction.RES) > 0,
            "non-vacuous: this match must form fields (MU>0) at some checkpoint",
        )
        assertEquals(a.checkpoints, b.checkpoints, "a match is reproducible even after another match runs between")
    }
}
