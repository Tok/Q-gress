package util.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ComplexTest {

    @Test
    fun constants() = with(Complex) {
        assertEquals(ZERO, Complex(0, 0))
        assertEquals(ONE, Complex(1, 0))
        assertEquals(I, Complex(0, 1))
    }

    @Test
    fun randomMagnitudeAndPhase() {
        (0..1000).map { Complex.random() }.forEach {
            assertTrue(it.magnitude >= 0.0)
            assertTrue(it.magnitude <= 1.0)
            assertTrue(it.phase >= -kotlin.math.PI)
            assertTrue(it.phase <= kotlin.math.PI)
        }
    }

    @Test
    fun stringRepresentation() = with(Complex) {
        // Decimal (non-integer) values + the special-cased `i` format identically on JS and JVM;
        // whole-number Double→String differs by platform ("1" vs "1.0"), so those aren't pinned here.
        assertEquals("i", I.toString())
        assertEquals("0.3", Complex(0.3, 0.0).toString())
        assertEquals("0.5*i", Complex(0.0, 0.5).toString())
        assertEquals("0.3+0.5*i", Complex(0.3, 0.5).toString())
        assertEquals("0.3-0.5*i", Complex(0.3, -0.5).toString()) // negative imaginary → '-' join
    }

    @Test
    fun arithmeticOperators() = with(Complex) {
        assertEquals(Complex(-2.0, -3.0), Complex(2.0, 3.0).negate())
        assertEquals(Complex(-2.0, -3.0), !Complex(2.0, 3.0)) // `not` == negate
        assertEquals(Complex(2.0, -3.0), Complex(2.0, 3.0).conjugate())
        assertEquals(Complex(-2.0, 3.0), Complex(2.0, 3.0).reverse())
        assertEquals(Complex(3.0, 5.0), Complex(1.0, 2.0) + Complex(2.0, 3.0))
        assertEquals(Complex(3.0, 2.0), Complex(1.0, 2.0) + 2.0)
        assertEquals(Complex(-1.0, -1.0), Complex(1.0, 2.0) - Complex(2.0, 3.0))
        assertEquals(Complex(-1.0, 2.0), Complex(1.0, 2.0) - 2.0)
        assertEquals(Complex(-4.0, 7.0), Complex(1.0, 2.0) * Complex(2.0, 3.0)) // (1+2i)(2+3i) = -4+7i
    }

    @Test
    fun divisionAndItsGuards() {
        // Block body (not expression body): ending in assertFailsWith would return Throwable, and
        // JUnit5 silently skips @Test methods that don't return Unit/void → div would go uncovered.
        with(Complex) {
            assertEquals(Complex(1.5, 0.5), Complex(1.0, 2.0) / Complex(1.0, 1.0)) // d=2 → (3/2, 1/2)
            assertFailsWith<IllegalArgumentException> { Complex(1.0, 2.0) / Complex(0.0, 1.0) } // re == 0
            assertFailsWith<IllegalArgumentException> { Complex(1.0, 2.0) / Complex(1.0, 0.0) } // im == 0
        }
    }

    @Test
    fun factoriesAndMagnitude() = with(Complex) {
        assertEquals(Complex(0.0, 5.0), fromImaginary(5.0))
        assertEquals(Complex(0.0, 3.0), fromImaginaryInt(3))
        assertEquals(Complex(2.0, 0.0), valueOf(2.0, 0.0)) // mag 2, phase 0 → (2, 0)
        assertEquals(10.0, Complex(3.0, 4.0).copyWithNewMagnitude(10.0).magnitude, 1e-9) // keeps phase, sets mag
    }

    @Test
    fun selectStrongerPicksLowerMagnitude() {
        assertEquals(Complex(1.0, 0.0), Complex.selectStronger(Complex(1.0, 0.0), Complex(3.0, 0.0)))
        assertEquals(Complex(1.0, 0.0), Complex.selectStronger(Complex(3.0, 0.0), Complex(1.0, 0.0))) // else arm
    }
}
