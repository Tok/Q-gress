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
import kotlin.math.sqrt
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The [Movement] helpers that read the live board ([World.allPortals]) to classify targets and pick a
 * destination portal — plus the pure heading/clamp primitives. Distinct class name from the jsMain-only
 * `agent.MovementTest` (both compile into the JS test target); these run on the JVM too (Kover coverage).
 */
class MovementCoreTest {

    @BeforeTest
    fun reset() {
        Sim.roundField = false
        World.grid = GridFixture("MOVE", 180, 120, 2, GridFixture.rleEncode(List(180 * 120) { true })).toGrid()
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        Rng.seed(3)
    }

    @AfterTest
    fun tidy() {
        Sim.roundField = true
        World.grid = emptyMap()
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
    }

    private fun ownedPortalAt(pos: Pos, faction: Faction): Portal {
        val p = Portal.create(pos)
        val a = Factory.agent(faction)
        p.owner = a
        p.slots.getValue(Octant.N).deployReso(a, Resonator.create(a, 1), Dim.maxDeploymentRange.toInt())
        return p
    }

    // --- pure primitives (0% on the JVM before this: the jsMain MovementTest doesn't feed Kover) ---

    @Test
    fun headingToIsAUnitVectorTowardTheTarget() {
        val east = Movement.headingTo(Pos(0, 0), Pos(10, 0))
        assertEquals(1.0, sqrt(east.re * east.re + east.im * east.im), 1e-9, "unit magnitude")
        assertTrue(east.re > 0.99, "points east")
    }

    @Test
    fun clampReturnsAPassableTargetAsIs() {
        val to = Pos(600, 400)
        assertEquals(to, Movement.clampToPlayable(Pos(590, 400), to), "open ground → step taken unchanged")
    }

    // --- board classification -------------------------------------------------

    @Test
    fun classifiesPortalsByOwnership() {
        val frog = Factory.frog()
        World.allPortals.add(Portal.create(Pos(400, 400))) // neutral
        World.allPortals.add(ownedPortalAt(Pos(500, 400), Faction.RES)) // enemy of a frog
        World.allPortals.add(ownedPortalAt(Pos(600, 400), Faction.ENL)) // friendly to a frog
        assertTrue(Movement.hasUncapturedPortals(), "the neutral portal is uncaptured")
        assertTrue(Movement.hasEnemyPortals(frog), "the RES portal is an enemy of the frog")
        assertTrue(Movement.hasFriendlyPortals(frog), "the ENL portal is friendly to the frog")
        assertEquals(1, Movement.findUncapturedPortals().size)
        assertEquals(1, Movement.findEnemyPortals(frog).size)
        assertEquals(1, Movement.findFriendlyPortals(frog).size)
    }

    @Test
    fun moveToNearestPortalCommitsToMoveTowardAnAwayPortal() {
        val near = Portal.create(Pos(400, 400))
        val far = Portal.create(Pos(1600, 400))
        World.allPortals.add(near)
        World.allPortals.add(far)
        val agent = Factory.frog().copy(pos = Pos(100, 400))
        val next = Movement.moveToNearestPortal(agent)
        assertEquals(ActionItem.MOVE, agent.action.item, "moving to a portal starts the MOVE action")
        assertEquals(near, next.actionPortal, "the nearest away-portal is chosen")
    }

    @Test
    fun moveToRandomPortalCommitsToMove() {
        World.allPortals.add(Portal.create(Pos(400, 400)))
        World.allPortals.add(Portal.create(Pos(1600, 400)))
        val agent = Factory.frog().copy(pos = Pos(100, 400))
        Movement.moveToRandomPortal(agent)
        assertEquals(ActionItem.MOVE, agent.action.item, "moveToRandomPortal starts the MOVE action")
    }

    @Test
    fun moveToUncapturedPortalHeadsForANeutralPortal() {
        repeat(6) { World.allPortals.add(Portal.create(Pos(400 + it * 120, 400))) } // several neutral, far from the agent
        val agent = Factory.frog().copy(pos = Pos(100, 100))
        val next = Movement.moveToUncapturedPortal(agent)
        assertEquals(ActionItem.MOVE, agent.action.item, "some reliable roll picks a neutral portal to capture (MOVE)")
        assertTrue(next.actionPortal.isUncaptured(), "the chosen target is an uncaptured portal")
    }

    @Test
    fun attackClosePortalGoesForTheNearestEnemy() {
        World.allPortals.add(ownedPortalAt(Pos(300, 400), Faction.RES))
        World.allPortals.add(ownedPortalAt(Pos(1600, 400), Faction.RES))
        val agent = Factory.frog().copy(pos = Pos(100, 400))
        val next = Movement.attackClosePortal(agent)
        assertEquals(ActionItem.MOVE, agent.action.item, "attackClosePortal starts the approach MOVE")
        assertEquals(Pos(300, 400), next.actionPortal.location, "the nearer enemy portal is targeted")
    }

    @Test
    fun attackWithNoEnemyEndsTheAction() {
        // No enemy portals on the board → goAttack(null) ends the action rather than moving.
        val agent = Factory.frog().also { it.action.start(ActionItem.MOVE) }
        Movement.attackClosePortal(agent)
        assertEquals(ActionItem.WAIT, agent.action.item, "no enemy to attack → the action ends (→ WAIT)")
    }

    @Test
    fun wanderStrollsToOpenGroundInThePlayArea() {
        val agent = Factory.frog().copy(pos = Pos(600, 400))
        val next = Movement.wander(agent)
        assertEquals(ActionItem.EXPLORE, agent.action.item, "wander starts the EXPLORE action")
        assertTrue(next.destination.isPassable() && Sim.isInPlayArea(next.destination.x, next.destination.y), "wanders on-map")
    }
}
