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

    // --- perElapsed: churn as a rate per unit TIME, not per roll ---------------

    private val chances = ChurnMath.ChurnChances(create = 0.4, remove = 0.2)

    @Test
    fun onePeriodOfElapsedTimeIsOneWholeRoll() {
        val c = ChurnMath.perElapsed(chances, elapsedTicks = 100, periodTicks = 100)
        assertEquals(chances.create, c.create, 1e-12)
        assertEquals(chances.remove, c.remove, 1e-12)
    }

    @Test
    fun halfAPeriodIsHalfARoll() {
        val c = ChurnMath.perElapsed(chances, elapsedTicks = 50, periodTicks = 100)
        assertEquals(0.2, c.create, 1e-12)
        assertEquals(0.1, c.remove, 1e-12)
    }

    @Test
    fun aLongGapClampsToOneWholeRollSoSparseSamplingCannotSpike() {
        val c = ChurnMath.perElapsed(chances, elapsedTicks = 10_000, periodTicks = 100)
        assertEquals(chances.create, c.create, 1e-12, "never more than one roll's worth, however long the gap")
        assertEquals(chances.remove, c.remove, 1e-12)
    }

    /**
     * The whole point: expected churn over a span depends only on the SPAN, not on how many times it was
     * sampled. Two strollers arriving 10 ticks apart must contribute exactly what one arriving after 20 does —
     * otherwise un-wedging wanderers (they used to hold the discoverer slots forever) inflates the board's churn.
     */
    @Test
    fun expectedChurnOverASpanIsIndependentOfHowOftenItIsSampled() {
        val period = 100
        val span = 60
        val once = ChurnMath.perElapsed(chances, span, period).create
        val split = listOf(10, 20, 30).sumOf { ChurnMath.perElapsed(chances, it, period).create }
        val many = List(span) { ChurnMath.perElapsed(chances, 1, period).create }.sum()
        assertEquals(once, split, 1e-12, "three samples across the span sum to the single-sample value")
        assertEquals(once, many, 1e-12, "and so do sixty")
    }

    @Test
    fun aZeroLengthGapContributesNoChurn() {
        val c = ChurnMath.perElapsed(chances, elapsedTicks = 0, periodTicks = 100)
        assertTrue(c.create == 0.0 && c.remove == 0.0, "a second stroller homing on the same tick adds nothing")
    }
}
