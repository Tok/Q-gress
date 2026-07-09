package agent
import Factory
import World
import config.Sim
import system.grid.GridFixture
import util.data.*
import kotlin.math.sqrt
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [Movement] (PLAN non-functional track — the last phase-A gap). [headingTo] is the pure
 * unit-vector heading; [Movement.wander] must only ever pick a destination that is passable AND inside the
 * play area, so a wandering agent can never stray off-map / off-field the way loose NPCs can.
 */
class MovementTest {

    // --- headingTo (pure) ----------------------------------------------------

    @Test
    fun headingToPointsAtTheTargetWithUnitMagnitude() {
        val east = Movement.headingTo(Pos(0, 0), Pos(10, 0))
        assertEquals(1.0, sqrt(east.re * east.re + east.im * east.im), 1e-9, "always a unit vector")
        assertTrue(east.re > 0.99 && kotlin.math.abs(east.im) < 1e-9, "due east")
    }

    @Test
    fun headingToIsZeroWhenAlreadyThere() {
        val still = Movement.headingTo(Pos(5, 5), Pos(5, 5))
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
        val dest = Movement.wander(agent).destination
        assertTrue(dest.isPassable(), "wander destinations are always on passable ground")
        assertTrue(Sim.isInPlayArea(dest.x, dest.y), "wander destinations are always inside the play area")
    }

    @Test
    fun wanderNeverLeavesTheMapEvenFromTheEdge() {
        // Near the corner, many ring samples fall off-map; the isWanderable gate must reject them all.
        val agent = Factory.agent().copy(pos = Pos(20, 20))
        val dest = Movement.wander(agent).destination
        assertTrue(dest.isPassable() && Sim.isInPlayArea(dest.x, dest.y), "an edge agent still stays on-map")
    }

    // --- clampToPlayable (sub-stepping wall slide / creep) --------------------
    // A 5×5-cell passable quadrant (shadow res 10 → sim 0..49 passable); everything at cell x≥5 OR y≥5 is a
    // wall. Lets us exercise the diagonal-blocked slide + the doubled-speed overshoot creep + the dead corner.

    private fun setQuadrantGrid() {
        World.grid = GridFixture(
            "CLAMP",
            10,
            10,
            2,
            GridFixture.rleEncode(List(10 * 10) { idx -> (idx % 10) < 5 && (idx / 10) < 5 }),
        ).toGrid()
    }

    @Test
    fun clampReturnsAPassableTargetUnchanged() {
        setQuadrantGrid()
        assertEquals(Pos(44, 44), Movement.clampToPlayable(Pos(42, 42), Pos(44, 44)), "open ground → step taken as-is")
    }

    @Test
    fun clampCreepsTowardAWallInsteadOfFreezing() {
        // Full step lands in the wall (cell x=5), but a shorter step stays in the passable cell — the agent
        // creeps up to the wall rather than holding (the doubled-speed thin-passage regression).
        setQuadrantGrid()
        val next = Movement.clampToPlayable(Pos(42, 42), Pos(52, 42))
        assertTrue(next.isPassable(), "creep result is on passable ground")
        assertTrue(next.x.toInt() in 43..49 && next.y.toInt() == 42, "made forward progress toward the wall (was 42 → $next)")
    }

    @Test
    fun clampConvergesToAHoldInaTrueDeadCorner() {
        // Wedged in the far corner of the passable quadrant. Positions are continuous, so the agent may first
        // creep the last sub-pixel up to the wall (49.0 → 49.99 is still inside its own passable cell); from
        // there every probe (both slides + the shortened diagonal) crosses into the wall → a fixed point.
        // StuckTracker / the per-action bail takes over from the hold.
        setQuadrantGrid()
        var pos = Pos(49, 49)
        repeat(5) { pos = Movement.clampToPlayable(pos, Pos(pos.x + 3, pos.y + 3)) }
        assertTrue(pos.isPassable(), "the clamp never steps into the wall")
        assertEquals(pos, Movement.clampToPlayable(pos, Pos(pos.x + 3, pos.y + 3)), "boxed-in corner → hold")
    }
}
