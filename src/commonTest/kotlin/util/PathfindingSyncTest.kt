package util

import World
import config.Config
import extension.VectorField
import portal.Portal
import system.grid.GridFixture
import system.grid.Nav
import system.grid.Pathfinding
import system.grid.SyncFieldFlow
import util.data.Pos
import util.data.toShadow
import kotlin.math.sqrt
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The synchronous flow-field path (PLAN Phase 6.1 / Stage 2 of the functional-core split): [Pathfinding.
 * computeFieldSync] computes a flow field inline (no coroutine), so a headless match has deterministic
 * pathfinding without the `MainScope` event loop. Also covers the [Config.headlessFieldCompute] opt-in
 * that routes `Portal.create` through the sync path headless.
 */
class PathfindingSyncTest {

    // A small all-passable arena (12×12 on-screen + a 2-cell ring), keyed by shadow-space Pos.
    private val goal = Pos(60, 60) // sim coords → shadow (6, 6), interior of the arena

    @BeforeTest
    fun setUp() {
        val onScreen = List(12 * 12) { true }
        World.grid = GridFixture("TEST", 12, 12, 2, GridFixture.rleEncode(onScreen)).toGrid()
        Nav.install(SyncFieldFlow) // route Portal.create's field request through the sync compute
        World.allPortals.clear()
    }

    @AfterTest
    fun tearDown() {
        Config.headlessFieldCompute = false
        Nav.reset()
        World.allPortals.clear()
    }

    @Test
    fun computeFieldSyncProducesANonEmptyField() {
        assertTrue(Pathfinding.computeFieldSync(goal).isNotEmpty(), "a flow field is produced over the grid")
    }

    @Test
    fun computeFieldSyncIsDeterministic() {
        // Same grid + goal → byte-for-byte the same field (no coroutine scheduling, no RNG).
        assertEquals(Pathfinding.computeFieldSync(goal), Pathfinding.computeFieldSync(goal))
    }

    @Test
    fun fieldVectorsHaveSaneBoundedMagnitudes() {
        val field = Pathfinding.computeFieldSync(goal)
        assertTrue(
            field.all { it.re.isFinite() && it.im.isFinite() && it.magnitude in 0.0..1.0001 },
            "every flow vector is finite with a speed-scaled magnitude in [0, 1]",
        )
    }

    @Test
    fun fieldGuidesAroundAnObstacleWithoutTrappingWhirls() {
        // A solid block sits between the goal (left) and a start cell (right), so the field must route around
        // it — the obstacle "shadow" is exactly where box-blur smoothing can spin up a whirl. The de-whirl pass
        // guarantees a downhill component everywhere, so FOLLOWING the field from behind the block still reaches
        // the goal instead of orbiting a trap.
        World.grid = GridFixture(
            "OBSTACLE",
            24,
            24,
            2,
            GridFixture.rleEncode(List(24 * 24) { idx -> !((idx % 24) in 10..13 && (idx / 24) in 10..13) }),
        ).toGrid()
        val goalSim = Pos(40, 120) // shadow (4, 12) — left of the block
        val field = Pathfinding.computeFieldSync(goalSim)
        assertTrue(followReachesGoal(field, Pos(200, 120), goalSim), "field threads around the block to the goal")
    }

    // Walk the continuous field from [startSim], stepping ≤ one cell per tick (magnitude ≤ 1 × Pos.res); reaches
    // the goal within ~1.5 cells, or returns false if it stalls / orbits past the step cap.
    private fun followReachesGoal(field: VectorField, startSim: Pos, goalSim: Pos, maxSteps: Int = 600): Boolean {
        var p = startSim
        repeat(maxSteps) {
            if (p.distanceTo(goalSim) <= Pos.res * 1.5) return true
            val v = field[p.toShadow()] ?: return false
            if (sqrt(v.re * v.re + v.im * v.im) < 1e-6) return false
            p = Pos(p.x + v.re * Pos.res, p.y + v.im * Pos.res)
        }
        return false
    }

    @Test
    fun unreachedPocketCellsSteerAwayFromWallsNotIntoThem() {
        // A fully-sealed passable pocket (a 2-cell-thick wall box) with the goal OUTSIDE it, to the LEFT. The heat
        // flood never reaches the pocket, so its cells are UNREACHED. The bug: such a cell aimed `destination -
        // cell` — straight at the goal THROUGH the wall — pinning the agent against it (the red stuck marker). The
        // fix repels it from adjacent walls, so a pocket cell against the box's left (goal-side) wall must steer
        // right (re > 0), never left into the wall.
        World.grid = GridFixture(
            "POCKET",
            24,
            24,
            2,
            GridFixture.rleEncode(
                List(24 * 24) { idx ->
                    val x = idx % 24
                    val y = idx / 24
                    val inBox = x in 7..15 && y in 7..15
                    val isWall = inBox && (x <= 8 || x >= 14 || y <= 8 || y >= 14) // 2-thick perimeter seals the interior
                    !isWall
                },
            ),
        ).toGrid()
        val goalSim = Pos(20, 120) // shadow (2, 12) — outside the box, to the LEFT
        val field = Pathfinding.computeFieldSync(goalSim)
        val v = field[Pos(90, 120).toShadow()] // shadow (9, 12): pocket cell against the box's left (goal-side) wall
        assertNotNull(v, "the sealed pocket cell still has a flow vector")
        assertTrue(v.re > 0.0, "an unreached pocket cell steers away from the wall (right), not toward the goal through it")
    }

    @Test
    fun portalCreateSkipsFieldsHeadlessByDefault() {
        Config.headlessFieldCompute = false
        assertTrue(Portal.create(goal).vectors.isEmpty(), "default headless: no field (agents bee-line)")
    }

    @Test
    fun portalCreatePopulatesFieldsHeadlessWhenOptedIn() {
        Config.headlessFieldCompute = true
        assertTrue(Portal.create(goal).vectors.isNotEmpty(), "opt-in headless: portal gets a synchronous field")
    }
}
