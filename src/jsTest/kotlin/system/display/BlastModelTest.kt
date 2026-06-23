package system.display

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlastModelTest {

    private fun len(v: DoubleArray) = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])

    @Test
    fun levelGainSpansFloorToOne() {
        assertEquals(1.0, BlastModel.levelGain(8, 0.4), 1e-9, "L8 = full energy")
        assertEquals(0.4 + 0.6 * (1.0 / 8.0), BlastModel.levelGain(1, 0.4), 1e-9, "L1 = floor + one step")
    }

    @Test
    fun levelGainClampsAndIsMonotonic() {
        assertEquals(BlastModel.levelGain(1, 0.3), BlastModel.levelGain(0, 0.3), 1e-9, "below L1 clamps to L1")
        assertEquals(BlastModel.levelGain(8, 0.3), BlastModel.levelGain(99, 0.3), 1e-9, "above L8 clamps to L8")
        assertTrue(BlastModel.levelGain(8, 0.3) > BlastModel.levelGain(4, 0.3), "higher level → more energy")
    }

    @Test
    fun cloudHeightRisesWithLevel() {
        assertTrue(BlastModel.cloudHeight(8) > BlastModel.cloudHeight(1), "the cloud centre rises with level")
    }

    @Test
    fun impulsePointsRadiallyOutFromOrigin() {
        val origin = doubleArrayOf(0.0, 0.0, 0.0)
        val piece = doubleArrayOf(3.0, 0.0, 4.0) // dist 5 along +x/+z
        val imp = BlastModel.blastImpulse(origin, piece, 8, 10.0, 100.0, 0.4)
        // direction is the unit (piece − origin): (0.6, 0, 0.8)
        val m = len(imp)
        assertEquals(0.6, imp[0] / m, 1e-9)
        assertEquals(0.0, imp[1] / m, 1e-9)
        assertEquals(0.8, imp[2] / m, 1e-9)
    }

    @Test
    fun impulseMagnitudeFollowsTheFalloffLaw() {
        val origin = doubleArrayOf(0.0, 0.0, 0.0)
        val piece = doubleArrayOf(0.0, 0.0, 5.0)
        val baseSpeed = 10.0
        val ref = 100.0
        val imp = BlastModel.blastImpulse(origin, piece, 8, baseSpeed, ref, 0.4)
        val expected = baseSpeed * 1.0 * ref / (ref + 5.0) // L8 gain = 1.0, dist = 5
        assertEquals(expected, len(imp), 1e-9)
    }

    @Test
    fun fartherPiecesGetLessPush() {
        val origin = doubleArrayOf(0.0, 0.0, 0.0)
        val near = BlastModel.blastImpulse(origin, doubleArrayOf(0.0, 0.0, 10.0), 8, 10.0, 100.0, 0.4)
        val far = BlastModel.blastImpulse(origin, doubleArrayOf(0.0, 0.0, 200.0), 8, 10.0, 100.0, 0.4)
        assertTrue(len(near) > len(far), "distance falloff: nearer pieces are pushed harder")
    }

    @Test
    fun degeneratePieceAtCentreGoesStraightUp() {
        val origin = doubleArrayOf(7.0, 7.0, 7.0)
        val imp = BlastModel.blastImpulse(origin, doubleArrayOf(7.0, 7.0, 7.0), 8, 10.0, 100.0, 0.4)
        assertEquals(0.0, imp[0], 1e-12)
        assertEquals(0.0, imp[1], 1e-12)
        assertTrue(imp[2] > 0.0, "a piece exactly at the centre is launched straight up")
    }
}
