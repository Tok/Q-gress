package util.data

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

class Vec3Test {
    private fun assertVec(expected: DoubleArray, actual: DoubleArray, eps: Double = 1e-9) {
        assertEquals(3, actual.size)
        for (i in 0..2) assertEquals(expected[i], actual[i], eps, "component $i")
    }

    @Test
    fun addSubScale() {
        assertVec(doubleArrayOf(5.0, 7.0, 9.0), add(doubleArrayOf(1.0, 2.0, 3.0), doubleArrayOf(4.0, 5.0, 6.0)))
        assertVec(doubleArrayOf(-3.0, -3.0, -3.0), sub(doubleArrayOf(1.0, 2.0, 3.0), doubleArrayOf(4.0, 5.0, 6.0)))
        assertVec(doubleArrayOf(2.0, 4.0, 6.0), scale(doubleArrayOf(1.0, 2.0, 3.0), 2.0))
    }

    @Test
    fun dotLenSqCross() {
        assertEquals(32.0, dot(doubleArrayOf(1.0, 2.0, 3.0), doubleArrayOf(4.0, 5.0, 6.0)), 1e-9) // 4+10+18
        assertEquals(14.0, lenSq(doubleArrayOf(1.0, 2.0, 3.0)), 1e-9) // 1+4+9
        assertVec(doubleArrayOf(0.0, 0.0, 1.0), cross(doubleArrayOf(1.0, 0.0, 0.0), doubleArrayOf(0.0, 1.0, 0.0))) // x×y=z
    }

    @Test
    fun normUnitAndZeroFallback() {
        assertVec(doubleArrayOf(1.0, 0.0, 0.0), norm(doubleArrayOf(5.0, 0.0, 0.0))) // unit along x
        assertVec(doubleArrayOf(0.0, 1.0, 0.0), norm(doubleArrayOf(0.0, 0.0, 0.0))) // zero → default fallback
        assertVec(doubleArrayOf(9.0, 9.0, 9.0), norm(doubleArrayOf(0.0, 0.0, 0.0), doubleArrayOf(9.0, 9.0, 9.0)))
    }

    @Test
    fun lerpAndDist() {
        assertVec(doubleArrayOf(0.5, 1.0, 1.5), lerp(doubleArrayOf(0.0, 0.0, 0.0), doubleArrayOf(1.0, 2.0, 3.0), 0.5))
        assertEquals(2.5, lerp1(0.0, 5.0, 0.5), 1e-9)
        assertEquals(5.0, dist(doubleArrayOf(0.0, 0.0, 0.0), doubleArrayOf(3.0, 4.0, 0.0)), 1e-9) // 3-4-5
    }

    @Test
    fun rotateQuarterTurnAboutZ() {
        // x-axis rotated 90° about z → y-axis (Rodrigues' rotation).
        assertVec(doubleArrayOf(0.0, 1.0, 0.0), rotate(doubleArrayOf(1.0, 0.0, 0.0), doubleArrayOf(0.0, 0.0, 1.0), PI / 2))
    }
}
