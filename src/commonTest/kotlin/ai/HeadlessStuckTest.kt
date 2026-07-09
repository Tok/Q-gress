package ai

import World
import agent.StuckTracker
import extension.Grid
import system.grid.GridFixture
import util.Rng
import util.data.Cell
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A headless match must never leave an agent wedged — a frozen agent silently poisons the training/eval signal
 * ([MatchResult.checkpointFitness]) with a faction that is a member short.
 *
 * Matches run with `flowFields = false` by default ("cheap abstract movement"): with no flow field,
 * `Portal.vectors` is empty and every travelling agent falls back to a **bare heading**, which is the steering
 * that wedges on geometry (see `agent.StuckNavigationTest`). Two things keep headless safe, and this pins both:
 *
 *  1. every headless grid is a fully-passable rectangle — [ai.SimRunner] installs the fixture as-is and never
 *     applies the round-arena mask (that lives in the jsMain map pipeline), so `roundField` only bounds
 *     `Sim.isInPlayArea`, not passability. There is no wall to wedge on;
 *  2. and even when there *is* one (a captured real-map fixture would have buildings), [agent.StuckTracker] now
 *     watches every travelling action and [agent.Agent.recoverIfStuck] re-targets, so no agent stays frozen.
 */
class HeadlessStuckTest {

    private val w = 60
    private val h = 40
    private val ticks = 1800

    @AfterTest
    fun tidy() = SimRunner.reset()

    /** The grid every headless caller actually uses (Tournament, ChampionTrainNN, ChampionBake, Evolution…). */
    private fun openGrid() = GridFixture("MU", w, h, 2, GridFixture.rleEncode(List(w * h) { true })).toGrid()

    /** The same, with buildings punched in — what a captured real-map fixture would look like. */
    private fun buildingGrid(seed: Int): Grid {
        Rng.seed(seed)
        val blocks = (0 until 20).map {
            intArrayOf(Rng.randomInt(2, w - 9), Rng.randomInt(2, h - 7), Rng.randomInt(3, 9), Rng.randomInt(3, 7))
        }
        fun blocked(x: Int, y: Int) = blocks.any { x >= it[0] && x < it[0] + it[2] && y >= it[1] && y < it[1] + it[3] }
        return openGrid().mapValues { (p, c) ->
            val onScreen = p.x >= 0 && p.y >= 0 && p.x < w && p.y < h
            if (onScreen && blocked(p.x.toInt(), p.y.toInt())) Cell(p, false, c.movementPenalty) else c
        }
    }

    private class Travel(val ticks: MutableMap<String, Int> = HashMap(), val px: MutableMap<String, Double> = HashMap()) {
        var flaggedTicks = 0

        /** Agents that spent real time travelling yet barely covered ground — i.e. wedged, not merely slowed. */
        fun wedged() = ticks.filter { (key, t) -> t > 200 && (px[key] ?: 0.0) < t * WEDGED_PX_PER_TICK }.keys
    }

    private fun runAndTrack(grid: Grid, flowFields: Boolean): Travel {
        val travel = Travel()
        SimRunner.runMatch(
            grid,
            seed = 3,
            maxTicks = ticks,
            setup = MatchSetup(npcs = 0, flowFields = flowFields),
            onTick = {
                if (StuckTracker.count() > 0) travel.flaggedTicks++
                World.allAgents.forEach { agent ->
                    if (agent.isTravelling()) {
                        travel.ticks[agent.key()] = (travel.ticks[agent.key()] ?: 0) + 1
                        travel.px[agent.key()] = (travel.px[agent.key()] ?: 0.0) + agent.stepPx
                    }
                }
            },
        )
        return travel
    }

    /** The real invariant the AI track depends on: no agent is ever frozen in a headless match. */
    @Test
    fun theHeadlessGridHasNoWallsSoNoAgentEverWedges() {
        val travel = runAndTrack(openGrid(), flowFields = false)
        assertTrue(travel.ticks.isNotEmpty(), "sanity: agents did travel")
        assertEquals(emptySet(), travel.wedged(), "a wall-free headless match wedges nobody")
        assertTrue(travel.flaggedTicks < ticks / 4, "and nobody loiters as stuck for long; got ${travel.flaggedTicks}/$ticks")
    }

    /** Defence in depth: give the fixture buildings (bare-heading steering's worst case) and the recovery still
     *  keeps every agent moving. They are *slowed* — a heading grinds on walls where a flow field rounds them —
     *  which is why a realistic captured fixture should be run with `flowFields = true`. */
    @Test
    fun evenOnAWalledFixtureRecoveryKeepsEveryAgentMoving() {
        val bare = runAndTrack(buildingGrid(11), flowFields = false)
        assertEquals(emptySet(), bare.wedged(), "stuck-recovery frees every agent even with no flow field to route it")

        val fields = runAndTrack(buildingGrid(11), flowFields = true)
        assertEquals(emptySet(), fields.wedged(), "and flow fields never wedge anyone either")
        assertTrue(
            fields.flaggedTicks < bare.flaggedTicks,
            "flow fields round walls, so far less grinding: ${fields.flaggedTicks} vs ${bare.flaggedTicks} flagged ticks",
        )
    }

    companion object {
        // A travelling agent walks at ~3 px/tick (Skills.speed 3.0-4.9). Below this it is not walking, it is stuck.
        private const val WEDGED_PX_PER_TICK = 0.5
    }
}
