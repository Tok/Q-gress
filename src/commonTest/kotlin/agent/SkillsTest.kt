package agent

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The per-agent recruiting aptitude → weight mapping ([Skills.recruitingFactor]): a 0..1 aptitude maps to a
 * 0.5×–1.5× multiplier averaging ~1.0, so the lever changes *who* recruits without shifting the overall pace.
 */
class SkillsTest {

    private fun withRecruiting(value: Double) = Skills(speed = 4.0, deployPrecision = 0.8, reliability = 0.7, recruiting = value)

    @Test
    fun recruitingFactorMapsAptitudeToAHalfToOnePointFiveMultiplier() {
        assertEquals(0.5, withRecruiting(0.0).recruitingFactor(), 1e-12, "min aptitude → 0.5×")
        assertEquals(1.0, withRecruiting(0.5).recruitingFactor(), 1e-12, "average aptitude → 1.0×")
        assertEquals(1.5, withRecruiting(1.0).recruitingFactor(), 1e-12, "max aptitude → 1.5×")
    }
}
