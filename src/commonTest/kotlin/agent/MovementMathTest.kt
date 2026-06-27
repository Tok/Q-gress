package agent

import util.Rng
import util.data.Complex
import util.data.Pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [MovementMath] — the pure heading/velocity math extracted from [Movement] (PLAN non-functional
 * track, phase B). Covers the unit-magnitude heading (incl. the coincident-point zero case) and the
 * force-integration cap, plus the random nudge a zero force gets.
 */
class MovementMathTest {

    @Test
    fun headingToCoincidentPointIsZero() {
        // Equal points → ZERO (not a NaN-normalised vector), so the agent stays put rather than breaking.
        assertEquals(Complex.ZERO, MovementMath.headingTo(Pos(5, 5), Pos(5, 5)))
    }

    @Test
    fun headingIsUnitMagnitudeAlongTheDirection() {
        val h = MovementMath.headingTo(Pos(0, 0), Pos(3, 4)) // 3-4-5 triangle
        assertEquals(1.0, h.mag, 1e-9, "headings are normalised to unit length")
        assertEquals(0.6, h.re, 1e-9)
        assertEquals(0.8, h.im, 1e-9)
    }

    @Test
    fun moveAddsForceWhenUnderTheSpeedLimit() {
        val result = MovementMath.move(velocity = Complex(1.0, 0.0), force = Complex(2.0, 0.0), limit = 10.0)
        assertEquals(3.0, result.re, 1e-9)
        assertEquals(0.0, result.im, 1e-9)
    }

    @Test
    fun moveClampsToTheSpeedLimitWhenExceeded() {
        // velocity+force = (12,0), magnitude 12 > limit 5 → clamped to magnitude 5 along the same direction.
        val result = MovementMath.move(velocity = Complex(6.0, 0.0), force = Complex(6.0, 0.0), limit = 5.0)
        assertEquals(5.0, result.mag, 1e-9)
        assertEquals(5.0, result.re, 1e-9, "direction preserved (was purely real)")
    }

    @Test
    fun zeroForceGetsARandomNudgeSoTheAgentNeverStalls() {
        Rng.seed(1)
        val result = MovementMath.move(velocity = Complex.ZERO, force = Complex.ZERO, limit = 10.0)
        assertTrue(result != Complex.ZERO, "a zero force is replaced by a random drift, not left dead-still")
        assertTrue(result.mag <= 10.0, "the nudge still respects the speed limit")
    }
}
