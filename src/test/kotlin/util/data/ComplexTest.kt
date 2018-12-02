package util.data

import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertEquals("i", I.toString())
        assertEquals("0", ZERO.toString())
        assertEquals("1", ONE.toString())
        assertEquals("0.3", Complex(0.3, 0.0).toString())
        assertEquals("0.5*i", Complex(0.0, 0.5).toString())
        assertEquals("0.3+0.5*i", Complex(0.3, 0.5).toString())
    }
}
