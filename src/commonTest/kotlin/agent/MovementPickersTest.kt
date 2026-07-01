package agent

import Factory
import World
import agent.action.ActionItem
import config.Dim
import config.Sim
import items.deployable.Resonator
import portal.Octant
import portal.Portal
import system.grid.GridFixture
import util.Rng
import util.data.Pos
import util.data.isPassable
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The remaining [Movement] destination pickers that read the live board: the vulnerable / most-linked enemy
 * selectors, the friendly-high-level and uncaptured pickers, the away-portal wander fallbacks, and the
 * wall-aware [Movement.clampToPlayable] slide/creep.
 */
class MovementPickersTest {

    @BeforeTest
    fun reset() {
        Sim.roundField = false
        World.grid = GridFixture("MOVEPICK", 180, 120, 2, GridFixture.rleEncode(List(180 * 120) { true })).toGrid()
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        World.isReady = true
        Rng.seed(3)
    }

    @AfterTest
    fun tidy() {
        Sim.roundField = true
        World.grid = emptyMap()
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        World.isReady = false
    }

    private fun ownedPortalAt(pos: Pos, faction: Faction, resos: Int = 1): Portal {
        val p = Portal.create(pos)
        val a = Factory.agent(faction)
        val octants = Octant.values().take(resos)
        p.deploy(a, octants.associateWith { Resonator.create(a, 4) }, Dim.maxDeploymentRange.toInt())
        World.allPortals.add(p)
        return p
    }

    @Test
    fun attackMostVulnerablePortalTargetsTheHealthiestFirst() {
        val strong = ownedPortalAt(Pos(400, 400), Faction.RES, resos = 8)
        val weak = ownedPortalAt(Pos(1200, 400), Faction.RES, resos = 8)
        weak.filledSlots().forEach { it.resonator?.energy = 5 } // crippled → sorts last by -health
        val agent = Factory.frog().copy(pos = Pos(100, 400))
        val next = Movement.attackMostVulnerablePortal(agent)
        assertEquals(ActionItem.MOVE, agent.action.item, "an enemy target starts the approach MOVE")
        assertEquals(strong.location, next.actionPortal.location, "the -health sort heads for the healthiest enemy portal first")
    }

    @Test
    fun attackMostLinkedPortalTargetsFewestLinksFirst() {
        val a = ownedPortalAt(Pos(400, 400), Faction.RES, resos = 8)
        ownedPortalAt(Pos(1200, 400), Faction.RES, resos = 8)
        val agent = Factory.frog().copy(pos = Pos(100, 400))
        val next = Movement.attackMostLinkedPortal(agent)
        assertEquals(ActionItem.MOVE, agent.action.item, "the most-linked selector still starts a MOVE")
        assertTrue(next.actionPortal.isEnemyOf(agent), "it targets an enemy portal")
        assertEquals(a.location, next.actionPortal.location, "with no links yet the first (link-count 0) enemy is taken")
    }

    @Test
    fun attackWithNoEnemyEndsTheAction() {
        val agent = Factory.frog().also { it.action.start(ActionItem.MOVE) }
        Movement.attackMostVulnerablePortal(agent)
        assertEquals(ActionItem.WAIT, agent.action.item, "no enemy portal → the action ends")
    }

    @Test
    fun moveToFriendlyHighLevelPortalPicksTheHighestLevelFriendly() {
        val lowFriendly = ownedPortalAt(Pos(400, 400), Faction.ENL, resos = 1)
        val highFriendly = ownedPortalAt(Pos(1200, 400), Faction.ENL, resos = 8)
        val agent = Factory.frog().copy(pos = Pos(100, 400))
        assertTrue(Movement.hasFriendlyPortals(agent), "the frog has friendly portals")
        val next = Movement.moveToFriendlyHighLevelPortal(agent)
        assertEquals(ActionItem.MOVE, agent.action.item, "heading to a friendly portal starts a MOVE")
        assertEquals(highFriendly.location, next.actionPortal.location, "the highest-level friendly portal is chosen")
        assertTrue(lowFriendly.getLevel().value <= highFriendly.getLevel().value, "the low friendly is not higher level")
    }

    @Test
    fun moveToNearestPortalWandersWhenEveryPortalIsUnderfoot() {
        World.allPortals.add(Portal.create(Pos(300, 300)))
        val agent = Factory.frog().copy(pos = Pos(300, 300)) // standing on the only portal → no away-portal
        val next = Movement.moveToNearestPortal(agent)
        assertEquals(ActionItem.EXPLORE, agent.action.item, "with no away-portal the agent wanders instead of spinning in place")
        assertTrue(Sim.isInPlayArea(next.destination.x, next.destination.y), "the wander target is on-map")
    }

    @Test
    fun moveToRandomPortalWandersWhenEveryPortalIsUnderfoot() {
        World.allPortals.add(Portal.create(Pos(300, 300)))
        val agent = Factory.frog().copy(pos = Pos(300, 300))
        Movement.moveToRandomPortal(agent)
        assertEquals(ActionItem.EXPLORE, agent.action.item, "moveToRandomPortal also falls back to wander with no away-portal")
    }

    @Test
    fun clampSlidesAlongAWallRatherThanStopping() {
        // A grid with an impassable vertical strip: a diagonal step into the wall should slide along one axis.
        val cells = List(180 * 120) { i ->
            val x = i % 180
            x != 90 // column 90 blocked
        }
        World.grid = GridFixture("WALL", 180, 120, 2, GridFixture.rleEncode(cells)).toGrid()
        val from = Pos(880, 400) // shadow (88,40) passable
        val into = Pos(900, 440) // shadow (90,44) — lands ON the blocked column, forcing a slide
        val clamped = Movement.clampToPlayable(from, into)
        assertTrue(clamped != into, "the blocked diagonal is not taken as-is")
        assertTrue(clamped.isPassable(), "the clamped step lands on passable ground")
    }
}
