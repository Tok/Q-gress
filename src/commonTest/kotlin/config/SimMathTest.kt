package config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [SimMath] — the pure play-field geometry extracted from [Sim] (PLAN non-functional track, phase B).
 * Covers the inscribed-circle radius, rectangle-vs-circle membership, and the displayable on-screen clamp.
 */
class SimMathTest {

    @Test
    fun fieldRadiusIsTheSmallerHalfExtent() {
        assertEquals(50.0, SimMath.fieldRadius(width = 200, height = 100), 1e-12, "radius = min(w,h)/2")
        assertEquals(50.0, SimMath.fieldRadius(width = 100, height = 200), 1e-12, "orientation-independent")
        assertEquals(60.0, SimMath.fieldRadius(width = 120, height = 120), 1e-12, "square → half the side")
    }

    @Test
    fun rectangleFieldAcceptsEveryPoint() {
        // A non-round field is the bare rectangle: the circle test is skipped, so even far corners pass.
        assertTrue(SimMath.isInsideField(roundField = false, width = 100, height = 100, x = 0.0, y = 0.0))
        assertTrue(SimMath.isInsideField(roundField = false, width = 100, height = 100, x = 99.0, y = 99.0))
    }

    @Test
    fun roundFieldKeepsTheCentreAndRejectsTheCorners() {
        val w = 100
        val h = 100
        assertTrue(SimMath.isInsideField(roundField = true, width = w, height = h, x = 50.0, y = 50.0), "centre is in")
        // Corner (0,0) is at distance ~70.7 from the centre (50,50) — well outside r=50.
        assertFalse(SimMath.isInsideField(roundField = true, width = w, height = h, x = 0.0, y = 0.0), "corner is out")
    }

    @Test
    fun roundFieldBoundaryIsInclusive() {
        // The point on the circle straight left of centre: (centre - r) = (0, 50), exactly on the rim.
        assertTrue(
            SimMath.isInsideField(roundField = true, width = 100, height = 100, x = 0.0, y = 50.0),
            "on-rim points count as inside (<=)",
        )
        // Just past the rim is out.
        assertFalse(SimMath.isInsideField(roundField = true, width = 100, height = 100, x = -0.5, y = 50.0))
    }

    @Test
    fun playAreaClampsToOnScreenBounds() {
        val w = 100
        val h = 100
        // In-bounds and inside the circle.
        assertTrue(SimMath.isInPlayArea(roundField = true, width = w, height = h, x = 50.0, y = 50.0))
        // Negative / out-of-range coords fail the on-screen clamp even though a rectangle field would pass.
        assertFalse(SimMath.isInPlayArea(roundField = false, width = w, height = h, x = -1.0, y = 50.0), "x<0")
        assertFalse(SimMath.isInPlayArea(roundField = false, width = w, height = h, x = 50.0, y = -1.0), "y<0")
        assertFalse(SimMath.isInPlayArea(roundField = false, width = w, height = h, x = 100.0, y = 50.0), "x==width is out")
        assertFalse(SimMath.isInPlayArea(roundField = false, width = w, height = h, x = 50.0, y = 100.0), "y==height is out")
    }

    @Test
    fun playAreaStillExcludesRoundFieldCorners() {
        // Inside the rectangle bounds but outside the inscribed circle → not in the play area.
        assertFalse(SimMath.isInPlayArea(roundField = true, width = 100, height = 100, x = 1.0, y = 1.0))
    }
}
