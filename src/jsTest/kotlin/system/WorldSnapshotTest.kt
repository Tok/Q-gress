package system

import World
import agent.Agent
import agent.Faction
import ai.FactionPolicies
import ai.HeuristicPolicy
import ai.SimRunner
import portal.Portal
import util.GridFixture
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** A captured live world must survive a headless eval that clobbers the shared [World] singletons. */
class WorldSnapshotTest {

    private fun grid() = GridFixture("SNAP", 40, 30, 2, GridFixture.rleEncode(List(40 * 30) { true })).toGrid()

    @AfterTest
    fun tearDown() = SimRunner.reset()

    @Test
    fun captureRestoreSurvivesAnInterveningMatch() {
        SimRunner.reset()
        val liveGrid = grid()
        World.grid = liveGrid
        World.userFaction = Faction.RES
        World.tick = 1234
        World.isReady = true
        val portal = Portal.create(Pos(50.0, 50.0)).also { World.allPortals.add(it) }
        val agent = Agent.createFrog(liveGrid).also { World.allAgents.add(it) }
        FactionPolicies.set(Faction.ENL, HeuristicPolicy(Faction.ENL))

        val snapshot = WorldSnapshot.capture()
        SimRunner.runMatch(grid(), seed = 9, maxTicks = 40) // an eval trashes the shared singletons
        assertFalse(World.allPortals.contains(portal), "the eval did replace the live world")

        WorldSnapshot.restore(snapshot)

        assertEquals(listOf(portal), World.allPortals, "the live portals are back")
        assertEquals(setOf(agent), World.allAgents, "the live agents are back")
        assertEquals(Faction.RES, World.userFaction)
        assertEquals(1234, World.tick)
        assertTrue(World.isReady)
        assertSame(liveGrid, World.grid, "the live grid is restored")
        assertTrue(FactionPolicies.of(Faction.ENL) is HeuristicPolicy, "the installed AI driver survives")
    }
}
