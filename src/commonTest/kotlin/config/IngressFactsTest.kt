package config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pin tests for [IngressFacts] — the read-only record of how the *original* Ingress works (researched from the
 * public wikis). The file header says "DO NOT change these numbers to tune our sim"; these tests make that
 * contract enforceable, so an accidental edit to the reference tables fails the build. They also bring the pure
 * data into the Kover-measured shared core (it was previously 0% — never referenced from a JVM test).
 */
class IngressFactsTest {

    @Test
    fun xmpTableMatchesTheWiki() {
        // level → (damage, range m, cost, recycle). The eight burster levels in published order.
        assertEquals(8, IngressFacts.Xmp.entries.size)
        with(IngressFacts.Xmp.L1) {
            assertEquals(1, level)
            assertEquals(150, damageXm)
            assertEquals(42, rangeM)
            assertEquals(50, costXm)
            assertEquals(20, recycleXm)
        }
        with(IngressFacts.Xmp.L8) {
            assertEquals(8, level)
            assertEquals(2700, damageXm)
            assertEquals(168, rangeM)
            assertEquals(400, costXm)
            assertEquals(160, recycleXm)
        }
        // Damage, range and cost are monotonically non-decreasing up the levels.
        val byLevel = IngressFacts.Xmp.entries.sortedBy { it.level }
        assertTrue(byLevel.zipWithNext().all { (a, b) -> b.damageXm >= a.damageXm }, "damage climbs")
        assertTrue(byLevel.zipWithNext().all { (a, b) -> b.rangeM >= a.rangeM }, "range climbs")
        assertTrue(byLevel.zipWithNext().all { (a, b) -> b.costXm >= a.costXm }, "cost climbs")
    }

    @Test
    fun resoTableMatchesTheWiki() {
        assertEquals(8, IngressFacts.Reso.entries.size)
        with(IngressFacts.Reso.L1) {
            assertEquals(1000, energyXm)
            assertEquals(8, perPlayer)
        }
        with(IngressFacts.Reso.L8) {
            assertEquals(6000, energyXm)
            assertEquals(1, perPlayer)
        }
        // Energy climbs with level; the per-player deploy allowance only shrinks.
        val byLevel = IngressFacts.Reso.entries.sortedBy { it.level }
        assertTrue(byLevel.zipWithNext().all { (a, b) -> b.energyXm >= a.energyXm }, "max energy climbs")
        assertTrue(byLevel.zipWithNext().all { (a, b) -> b.perPlayer <= a.perPlayer }, "per-player allowance shrinks")
    }

    @Test
    fun shieldTierMitigationAndStickinessClimb() {
        assertEquals(4, IngressFacts.Shield.entries.size)
        assertEquals(30, IngressFacts.Shield.COMMON.mitigationPct)
        assertEquals(70, IngressFacts.Shield.AXA.mitigationPct)
        assertEquals("CS", IngressFacts.Shield.COMMON.abbr)
        val tiers = IngressFacts.Shield.entries.toList() // declared best-guess ordering, weakest → strongest
        assertTrue(tiers.zipWithNext().all { (a, b) -> b.mitigationPct >= a.mitigationPct }, "mitigation climbs")
        assertTrue(tiers.zipWithNext().all { (a, b) -> b.approxStickiness >= a.approxStickiness }, "stickiness climbs")
    }

    @Test
    fun combatAndDistanceConstantsArePinned() {
        assertEquals(intArrayOf(100, 50, 25, 13, 6).toList(), IngressFacts.RANGE_FALLOFF_PCT.toList())
        assertEquals(95, IngressFacts.MITIGATION_CAP_PCT)
        assertEquals(1, IngressFacts.MITIGATION_MIN_DAMAGE)
        assertEquals(2, IngressFacts.CRIT_MULTIPLIER)
        assertEquals(8, IngressFacts.RESO_SLOTS)
        assertEquals(4, IngressFacts.MOD_SLOTS)
        assertEquals(40, IngressFacts.DEPLOY_RANGE_M)
        assertEquals(300, IngressFacts.HACK_COOLDOWN_S)
        assertEquals(4, IngressFacts.HACKS_BEFORE_BURNOUT)
    }

    @Test
    fun apAwardsArePinned() {
        assertEquals(75, IngressFacts.AP_DESTROY_RESONATOR)
        assertEquals(75, IngressFacts.AP_DESTROY_MOD)
        assertEquals(125, IngressFacts.AP_DEPLOY_RESONATOR)
        assertEquals(500, IngressFacts.AP_CAPTURE_PORTAL)
        assertEquals(313, IngressFacts.AP_CREATE_LINK)
        assertEquals(1250, IngressFacts.AP_CREATE_FIELD)
    }
}
