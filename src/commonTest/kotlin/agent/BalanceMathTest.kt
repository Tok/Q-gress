package agent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Shared-core tests for the pure anti-snowball formulas (runs on jsNodeTest + jvmTest). The World-coupled
 *  wrappers are characterized separately in jsTest ([BalanceTest], RecruiterTest). */
class BalanceMathTest {
    @Test
    fun shareDeficitIsZeroWhenEvenOrAheadAndOneWhenShutOut() {
        assertEquals(0.0, BalanceMath.shareDeficit(5, 5), "even")
        assertEquals(0.0, BalanceMath.shareDeficit(0, 0), "nothing held → 0, not NaN")
        assertEquals(0.0, BalanceMath.shareDeficit(8, 2), "ahead clamps to 0")
        assertEquals(1.0, BalanceMath.shareDeficit(0, 9), "fully shut out")
        assertEquals(0.8, BalanceMath.shareDeficit(1, 9), 1e-9, "(9−1)/10")
    }

    @Test
    fun recruitFactorFavoursTheSmallerRosterAndClamps() {
        assertEquals(1.0, BalanceMath.recruitFactor(3, 3, 0.3, 3.0), 1e-12, "even → base")
        assertTrue(BalanceMath.recruitFactor(5, 1, 0.3, 3.0) < 1.0, "larger roster recruits below base")
        assertTrue(BalanceMath.recruitFactor(1, 5, 0.3, 3.0) > 1.0, "smaller roster recruits above base")
        assertEquals(0.3, BalanceMath.recruitFactor(20, 0, 0.3, 3.0), "leader clamped to floor")
        assertEquals(3.0, BalanceMath.recruitFactor(0, 20, 0.3, 3.0), "underdog clamped to ceiling")
    }

    @Test
    fun attackBoostGrowsWithTheSquareOfTheDeficit() {
        assertEquals(1.0, BalanceMath.attackBoost(0.0, 2.0, 0.5), "even → no boost")
        assertEquals(2.0, BalanceMath.attackBoost(1.0, 2.0, 0.5), 1e-9, "1 + 2 × 0.5 × 1²")
        assertEquals(1.64, BalanceMath.attackBoost(0.8, 2.0, 0.5), 1e-9, "1 + 1 × 0.8² = 1.64")
    }

    @Test
    fun recruitSuccessDiminishesAsTheRosterFills() {
        assertEquals(0.05, BalanceMath.recruitSuccessProbability(0.0, 0.05), 1e-12, "empty → full base")
        assertEquals(0.0, BalanceMath.recruitSuccessProbability(1.0, 0.05), 1e-12, "full → ~0")
        assertEquals(0.025, BalanceMath.recruitSuccessProbability(0.5, 0.05), 1e-12, "half → half")
    }
}
