package portal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [Octant] — the 8 compass directions used for resonator slots. Screen convention: +x is East,
 * +y is South (downwards), so e.g. S = (0, +r) and N = (0, -r). Covers the angle→offset trig and the signs.
 */
class OctantTest {

    private val r = 100

    @Test
    fun cardinalOffsetsPointTheRightWay() {
        // E → +x, 0 y. W → -x, 0 y. (sin(0)=0, sin(180°)≈0.)
        assertEquals(r, Octant.E.calcXOffset(r))
        assertEquals(0, Octant.E.calcYOffset(r))
        assertEquals(-r, Octant.W.calcXOffset(r))
        // S → +y (down), ~0 x. N → -y (up), ~0 x.
        assertEquals(r, Octant.S.calcYOffset(r))
        assertEquals(-r, Octant.N.calcYOffset(r))
    }

    @Test
    fun diagonalsHaveTheExpectedSigns() {
        // SE is down-right: +x, +y. NW is up-left: -x, -y.
        assertTrue(Octant.SE.calcXOffset(r) > 0 && Octant.SE.calcYOffset(r) > 0)
        assertTrue(Octant.NW.calcXOffset(r) < 0 && Octant.NW.calcYOffset(r) < 0)
        // NE is up-right: +x, -y. SW is down-left: -x, +y.
        assertTrue(Octant.NE.calcXOffset(r) > 0 && Octant.NE.calcYOffset(r) < 0)
        assertTrue(Octant.SW.calcXOffset(r) < 0 && Octant.SW.calcYOffset(r) > 0)
    }

    @Test
    fun offsetsLieOnTheCircleOfTheGivenRadius() {
        // Every direction's offset vector should have magnitude ≈ r (within rounding to Int).
        Octant.values().forEach { o ->
            val x = o.calcXOffset(r)
            val y = o.calcYOffset(r)
            val mag = kotlin.math.sqrt((x * x + y * y).toDouble())
            assertTrue(kotlin.math.abs(mag - r) <= 2.0, "$o offset ($x,$y) magnitude $mag should be ≈ $r")
        }
    }

    @Test
    fun arrowGlyphRendersAsToString() {
        assertEquals("→", Octant.E.toString())
        assertEquals("↑", Octant.N.toString())
    }
}
