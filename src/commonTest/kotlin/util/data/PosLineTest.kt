package util.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Shared-core tests for the pure geometry now in commonMain (runs on jsNodeTest + jvmTest). World/grid/geo
 *  coupled members live in the JS shell (PosExt) and are exercised there. */
class PosLineTest {
    @Test
    fun distanceAndDiffs() {
        assertEquals(5.0, Pos(0, 0).distanceTo(Pos(3, 4)), 1e-9, "3-4-5 triangle")
        assertEquals(0.0, Pos(2, 7).distanceTo(Pos(2, 7)), "same point")
        assertEquals(-3.0, Pos(1, 1).xDiff(Pos(4, 1)))
        assertEquals(2.0, Pos(1, 5).yDiff(Pos(1, 3)))
    }

    @Test
    fun surroundingClampsToBounds() {
        assertEquals(8, Pos(1, 1).surrounding(3, 3).size, "interior cell → 8 neighbours")
        assertEquals(3, Pos(0, 0).surrounding(3, 3).size, "corner → 3")
        assertEquals(5, Pos(1, 0).surrounding(3, 3).size, "edge → 5")
    }

    @Test
    fun equalityAndHashByValue() {
        assertEquals(Pos(2, 3), Pos(2.0, 3.0))
        assertEquals(Pos(2, 3).hashCode(), Pos(2, 3).hashCode())
        assertFalse(Pos(2, 3) == Pos(3, 2))
    }

    @Test
    fun lineLengthCenterTaxi() {
        val line = Line(0, 0, 3, 4)
        assertEquals(5.0, line.length(), 1e-9)
        assertEquals(Pos(1.5, 2.0), line.center())
        assertEquals(7, line.calcTaxiLength(), "|3| + |4|")
    }

    @Test
    fun lineIntersection() {
        val a = Line(0, 0, 4, 4)
        val crossing = Line(0, 4, 4, 0)
        val parallel = Line(0, 1, 4, 5)
        assertTrue(a.doesIntersect(crossing), "an X crosses")
        assertFalse(a.doesIntersect(parallel), "parallel never crosses")
    }

    @Test
    fun closestPointClampsToSegmentEnds() {
        val seg = Line(0, 0, 10, 0)
        assertEquals(Pos(5, 0), seg.findClosestPointTo(Pos(5, 5)), "drops perpendicular onto the segment")
        assertEquals(Pos(0.0, 0.0), seg.findClosestPointTo(Pos(-3, 2)), "before the start clamps to 'from'")
        assertEquals(Pos(10.0, 0.0), seg.findClosestPointTo(Pos(99, 2)), "past the end clamps to 'to'")
    }

    @Test
    fun pointInAreaRespectsValidRect() {
        val rect = Line(0, 0, 10, 10) // top-left → bottom-right
        assertTrue(rect.isPointInArea(Pos(5, 5)))
        assertFalse(rect.isPointInArea(Pos(11, 5)), "outside")
        assertFalse(Line(10, 10, 0, 0).isPointInArea(Pos(5, 5)), "invalid (not top-left→bottom-right)")
    }

    @Test
    fun doubleCtorCreateAndToString() {
        assertEquals(5.0, Line(0.0, 0.0, 3.0, 4.0).length(), 1e-9, "Double secondary constructor builds the same line")
        assertEquals(Line(0, 0, 3, 4), Line.create(0, 0, 3, 4), "create() companion mirrors the Int constructor")
        val line = Line(0, 0, 3, 4) // structural check (Double rendering differs JS vs JVM, so don't pin the literal)
        assertEquals(line.from.toString() + "-" + line.to.toString(), line.toString(), "from-to rendering")
    }

    @Test
    fun closestPointOnVerticalSegment() {
        // Vertical segment exercises the xDiff==0 side of findClosestPointTo's non-degeneracy guard.
        val seg = Line(0, 0, 0, 10)
        assertEquals(Pos(0, 5), seg.findClosestPointTo(Pos(4, 5)), "drops perpendicular onto a vertical segment")
    }

    @Test
    fun closestPointRejectsDegenerateLine() {
        // from == to → zero direction vector → the guard fires (covers its failing branch).
        assertFailsWith<IllegalStateException> { Line(3, 3, 3, 3).findClosestPointTo(Pos(0, 0)) }
    }

    @Test
    fun intersectionRespectsSegmentBounds() {
        // The horizontal segment lies on y=0 for x in [0,4]; the others would cross its infinite line.
        val seg = Line(0, 0, 4, 0)
        assertTrue(seg.doesIntersect(Line(2, -2, 2, 2)), "a vertical that straddles the segment crosses it")
        assertFalse(seg.doesIntersect(Line(2, -5, 2, -1)), "crossing point is beyond the OTHER segment → no hit")
        assertFalse(seg.doesIntersect(Line(8, -2, 8, 2)), "crossing point is past THIS segment's far end → no hit")
        assertFalse(seg.doesIntersect(Line(-2, -2, -2, 2)), "crossing point is before THIS segment's start → no hit")
        assertFalse(seg.doesIntersect(Line(2, 2, 2, 5)), "the other segment sits entirely off the crossing → no hit")
    }

    @Test
    fun validAreaNeedsTopLeftToBottomRight() {
        assertTrue(Line(0, 0, 10, 10).isValidArea())
        assertFalse(Line(10, 0, 0, 10).isValidArea(), "from.x > to.x")
        assertFalse(Line(0, 10, 10, 0).isValidArea(), "from.y > to.y")
    }

    @Test
    fun pointInAreaChecksEachBound() {
        val rect = Line(0, 0, 10, 10)
        assertFalse(rect.isPointInArea(Pos(-1, 5)), "left of from.x")
        assertFalse(rect.isPointInArea(Pos(5, -1)), "above from.y")
        assertFalse(rect.isPointInArea(Pos(11, 5)), "right of to.x")
        assertFalse(rect.isPointInArea(Pos(5, 11)), "below to.y")
    }

    @Test
    fun surroundingClampsUpperBoundsToo() {
        assertEquals(3, Pos(2, 2).surrounding(3, 3).size, "far corner → 3 (upper x/y bounds clamp)")
        assertEquals(5, Pos(2, 1).surrounding(3, 3).size, "right edge → 5")
    }

    @Test
    fun equalsRejectsNonPosAndEitherAxisDiff() {
        assertFalse(Pos(2, 3).equals("not a pos"), "a non-Pos is never equal")
        assertFalse(Pos(2, 3) == Pos(2, 4), "differs in y")
        assertFalse(Pos(2, 3) == Pos(9, 3), "differs in x")
    }
}
