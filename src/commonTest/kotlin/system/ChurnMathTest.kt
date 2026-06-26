package system

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [ChurnMath.churnChances] — the pure density-churn math extracted from Cycle.managePortalDensity (PLAN
 * non-functional track, phase B). Discovery dominates a sparse board, evens out at the target, and removal
 * takes over above it; with no room to place a portal the discovery budget rolls into removal.
 */
class ChurnMathTest {

    private val rate = 0.5
    private val target = 10

    @Test
    fun discoveryDominatesOnAnEmptyBoard() {
        val c = ChurnMath.churnChances(count = 0, target = target, rate = rate, hasSpace = true)
        assertEquals(rate, c.create, 1e-12, "at d=0 the full create budget is available")
        assertEquals(0.0, c.remove, 1e-12, "nothing to thin out yet")
        assertTrue(c.create > c.remove)
    }

    @Test
    fun creationAndRemovalCrossAtTheTarget() {
        val c = ChurnMath.churnChances(count = target, target = target, rate = rate, hasSpace = true)
        assertEquals(rate * 0.5, c.create, 1e-12)
        assertEquals(rate * 0.5, c.remove, 1e-12)
        assertEquals(c.create, c.remove, 1e-12, "they balance ~1:1 at the target")
    }

    @Test
    fun removalDominatesAboveTheTarget() {
        val c = ChurnMath.churnChances(count = 2 * target, target = target, rate = rate, hasSpace = true)
        assertEquals(0.0, c.create, 1e-12, "create fades to 0 at d=2 (clamped)")
        assertEquals(rate, c.remove, 1e-12, "removal saturates at the full rate")
        assertTrue(c.remove > c.create)
    }

    @Test
    fun bothChancesAreClampedFarAboveTheTarget() {
        val c = ChurnMath.churnChances(count = 100, target = target, rate = rate, hasSpace = true)
        assertEquals(0.0, c.create, 1e-12, "create floored at 0, never negative")
        assertEquals(rate, c.remove, 1e-12, "remove capped at the rate, never above")
    }

    @Test
    fun withNoSpaceTheDiscoveryBudgetRollsIntoRemoval() {
        // Sparse board (would normally create heavily) but nowhere to place → that budget becomes removal.
        val open = ChurnMath.churnChances(count = 0, target = target, rate = rate, hasSpace = true)
        val packed = ChurnMath.churnChances(count = 0, target = target, rate = rate, hasSpace = false)
        assertEquals(open.create, packed.create, 1e-12, "the create value itself is unchanged")
        assertEquals(open.create, packed.remove, 1e-12, "but with no room it is added to removal so the board thins")
    }
}
