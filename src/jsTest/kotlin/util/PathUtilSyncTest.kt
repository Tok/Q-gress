package util

import World
import config.Config
import portal.Portal
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The synchronous flow-field path (PLAN Phase 6.1 / Stage 2 of the functional-core split): [PathUtil.
 * computeFieldSync] computes a flow field inline (no coroutine), so a headless match has deterministic
 * pathfinding without the `MainScope` event loop. Also covers the [Config.headlessFieldCompute] opt-in
 * that routes `Portal.create` through the sync path headless.
 */
class PathUtilSyncTest {

    // A small all-passable arena (12×12 on-screen + a 2-cell ring), keyed by shadow-space Pos.
    private val goal = Pos(60, 60) // sim coords → shadow (6, 6), interior of the arena

    @BeforeTest
    fun setUp() {
        val onScreen = List(12 * 12) { true }
        World.grid = GridFixture("TEST", 12, 12, 2, GridFixture.rleEncode(onScreen)).toGrid()
    }

    @AfterTest
    fun tearDown() {
        Config.headlessFieldCompute = false
    }

    @Test
    fun computeFieldSyncProducesANonEmptyField() {
        assertTrue(PathUtil.computeFieldSync(goal).isNotEmpty(), "a flow field is produced over the grid")
    }

    @Test
    fun computeFieldSyncIsDeterministic() {
        // Same grid + goal → byte-for-byte the same field (no coroutine scheduling, no RNG).
        assertEquals(PathUtil.computeFieldSync(goal), PathUtil.computeFieldSync(goal))
    }

    @Test
    fun fieldVectorsHaveSaneBoundedMagnitudes() {
        val field = PathUtil.computeFieldSync(goal)
        assertTrue(
            field.values.all { it.re.isFinite() && it.im.isFinite() && it.magnitude in 0.0..1.0001 },
            "every flow vector is finite with a speed-scaled magnitude in [0, 1]",
        )
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
