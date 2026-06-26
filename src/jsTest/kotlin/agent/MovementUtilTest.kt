package agent
import Factory
import World
import config.Sim
import util.GridFixture
import util.data.*
import kotlin.math.sqrt
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [MovementUtil] (PLAN non-functional track — the last phase-A gap). [headingTo] is the pure
 * unit-vector heading; [MovementUtil.wander] must only ever pick a destination that is passable AND inside the
 * play area, so a wandering agent can never stray off-map / off-field the way loose NPCs can.
 */
class MovementUtilTest {

    // --- headingTo (pure) ----------------------------------------------------

    @Test
    fun headingToPointsAtTheTargetWithUnitMagnitude() {
        val east = MovementUtil.headingTo(Pos(0, 0), Pos(10, 0))
        assertEquals(1.0, sqrt(east.re * east.re + east.im * east.im), 1e-9, "always a unit vector")
        assertTrue(east.re > 0.99 && kotlin.math.abs(east.im) < 1e-9, "due east")
    }

    @Test
    fun headingToIsZeroWhenAlreadyThere() {
        val still = MovementUtil.headingTo(Pos(5, 5), Pos(5, 5))
        assertEquals(0.0, still.re, 1e-12)
        assertEquals(0.0, still.im, 1e-12)
    }

    // --- wander stays on-map -------------------------------------------------

    private var savedRound = true

    @BeforeTest
    fun setUpField() {
        savedRound = Sim.roundField
        Sim.roundField = false // rectangle: isInPlayArea is just the on-screen bounds (no circle to reason about)
        // A fully-passable grid spanning the default 1800×1200 play area (shadow res 10 → 180×120 cells).
        World.grid = GridFixture("WANDER", 180, 120, 2, GridFixture.rleEncode(List(180 * 120) { true })).toGrid()
    }

    @AfterTest
    fun tearDownField() {
        Sim.roundField = savedRound
        World.grid = emptyMap()
    }

    @Test
    fun wanderPicksAPassableInPlayAreaDestination() {
        val agent = Factory.agent().copy(pos = Pos(600, 400))
        val dest = MovementUtil.wander(agent).destination
        assertTrue(dest.isPassable(), "wander destinations are always on passable ground")
        assertTrue(Sim.isInPlayArea(dest.x, dest.y), "wander destinations are always inside the play area")
    }

    @Test
    fun wanderNeverLeavesTheMapEvenFromTheEdge() {
        // Near the corner, many ring samples fall off-map; the isWanderable gate must reject them all.
        val agent = Factory.agent().copy(pos = Pos(20, 20))
        val dest = MovementUtil.wander(agent).destination
        assertTrue(dest.isPassable() && Sim.isInPlayArea(dest.x, dest.y), "an edge agent still stays on-map")
    }
}
