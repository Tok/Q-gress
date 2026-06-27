package portal

import util.data.Pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Shared-core tests for the pure field geometry (runs on jsNodeTest + jvmTest). The World/Portal-coupled
 *  [Field] wrapper is characterized in jsTest (FieldTest). */
class FieldMathTest {
    @Test
    fun heronAreaScaledToMu() {
        // 300-400-500 right triangle → area 60000 px² → /100 = 600 MU.
        val mu = FieldMath.triangleAreaMu(Pos(0, 0), Pos(300, 0), Pos(0, 400))
        assertEquals(600, mu, "right triangle 300×400 → 60000/100")
    }

    @Test
    fun degenerateTriangleFloorsToOneMu() {
        assertEquals(1, FieldMath.triangleAreaMu(Pos(0, 0), Pos(10, 0), Pos(20, 0)), "collinear → floored at 1")
    }

    @Test
    fun pointInsideTriangleIsCovered() {
        val a = Pos(0, 0)
        val b = Pos(100, 0)
        val c = Pos(0, 100)
        assertTrue(FieldMath.isInsideTriangle(Pos(20, 20), a, b, c), "well inside")
        assertFalse(FieldMath.isInsideTriangle(Pos(90, 90), a, b, c), "outside the hypotenuse")
        assertFalse(FieldMath.isInsideTriangle(Pos(-5, 20), a, b, c), "outside an edge")
    }

    @Test
    fun windingOrderDoesNotMatter() {
        val inside = Pos(20, 20)
        val cw = FieldMath.isInsideTriangle(inside, Pos(0, 0), Pos(100, 0), Pos(0, 100))
        val ccw = FieldMath.isInsideTriangle(inside, Pos(0, 0), Pos(0, 100), Pos(100, 0))
        assertEquals(cw, ccw, "the sign test handles either vertex winding")
        assertTrue(cw)
    }

    @Test
    fun ccwWindingInsideAndOutsideEachEdge() {
        // Reversed vertices drive the d<0 arm of the sign test; outside points fail each sub-condition.
        val a = Pos(0, 0)
        val b = Pos(0, 100)
        val c = Pos(100, 0)
        assertTrue(FieldMath.isInsideTriangle(Pos(20, 20), a, b, c), "well inside (ccw)")
        assertFalse(FieldMath.isInsideTriangle(Pos(90, 90), a, b, c), "past the hypotenuse (ccw)")
        assertFalse(FieldMath.isInsideTriangle(Pos(-5, 20), a, b, c), "left of an edge (ccw)")
        assertFalse(FieldMath.isInsideTriangle(Pos(20, -5), a, b, c), "below an edge (ccw)")
    }
}
