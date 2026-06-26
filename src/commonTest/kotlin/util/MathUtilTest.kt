package util

import kotlin.test.Test
import kotlin.test.assertEquals

/** Shared-core test: runs on BOTH js (jsNodeTest) and jvm (jvmTest, Kover-instrumented). */
class MathUtilTest {
    @Test
    fun clipBoundsIntoRange() {
        assertEquals(5, MathUtil.clip(5, 0, 10))
        assertEquals(0, MathUtil.clip(-3, 0, 10))
        assertEquals(10, MathUtil.clip(42, 0, 10))
    }

    @Test
    fun clipDoubleBoundsIntoRange() {
        assertEquals(2.5, MathUtil.clipDouble(2.5, 0.0, 5.0))
        assertEquals(0.0, MathUtil.clipDouble(-1.0, 0.0, 5.0))
        assertEquals(5.0, MathUtil.clipDouble(9.0, 0.0, 5.0))
    }

    @Test
    fun degRadRoundTrip() {
        assertEquals(180.0, MathUtil.radToDeg(MathUtil.degToRad(180.0)), 1e-9)
        assertEquals(0.0, MathUtil.degToRad(0.0))
    }

    @Test
    fun formatSecondsZeroPadsHmsFields() {
        assertEquals("00:00:00", MathUtil.formatSeconds(0))
        assertEquals("00:00:09", MathUtil.formatSeconds(9))
        assertEquals("00:01:05", MathUtil.formatSeconds(65))
        assertEquals("01:00:00", MathUtil.formatSeconds(3600))
        assertEquals("01:01:01", MathUtil.formatSeconds(3661))
    }
}
