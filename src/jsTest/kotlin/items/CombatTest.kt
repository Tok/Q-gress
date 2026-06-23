package items

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CombatTest {

    @Test
    fun steppedQuintileFalloff() {
        assertEquals(1.0, Combat.rangeFalloff(0.0))
        assertEquals(1.0, Combat.rangeFalloff(0.19))
        assertEquals(0.5, Combat.rangeFalloff(0.3))
        assertEquals(0.25, Combat.rangeFalloff(0.5))
        assertEquals(0.125, Combat.rangeFalloff(0.7))
        assertEquals(0.0625, Combat.rangeFalloff(0.9))
        assertEquals(0.0, Combat.rangeFalloff(1.0)) // edge + beyond = out of range
        assertEquals(0.0, Combat.rangeFalloff(1.5))
    }

    @Test
    fun outOfRangeDealsNoDamage() {
        assertEquals(0, Combat.resoDamage(2700, distFrac = 1.2, mitigation = 0, ultra = false, crit = false))
    }

    @Test
    fun inRangeAlwaysDealsAtLeastOne() {
        // Tiny ultra damage in the last quintile still floors at 1 XM (Ingress rule).
        assertEquals(1, Combat.resoDamage(150, distFrac = 0.9, mitigation = 0, ultra = true, crit = false))
    }

    @Test
    fun mitigationHalvesDamageAtFifty() {
        val full = Combat.resoDamage(2700, distFrac = 0.0, mitigation = 0, ultra = false, crit = false)
        val shielded = Combat.resoDamage(2700, distFrac = 0.0, mitigation = 50, ultra = false, crit = false)
        assertEquals(full / 2, shielded)
    }

    @Test
    fun mitigationIsCappedAtNinetyFive() {
        val capped = Combat.resoDamage(2700, 0.0, mitigation = 95, ultra = false, crit = false)
        val overCap = Combat.resoDamage(2700, 0.0, mitigation = 100_000, ultra = false, crit = false)
        assertEquals(capped, overCap, "mitigation beyond 95% must not reduce damage further")
    }

    @Test
    fun ultraStrikeDealsFarLessResoDamageThanBurster() {
        val xmp = Combat.resoDamage(2700, 0.0, 0, ultra = false, crit = false)
        val us = Combat.resoDamage(2700, 0.0, 0, ultra = true, crit = false)
        assertTrue(us < xmp / 5, "an Ultra-Strike should barely scratch resonators ($us vs $xmp)")
    }

    @Test
    fun critRaisesDamage() {
        val normal = Combat.resoDamage(2700, 0.0, 0, ultra = false, crit = false)
        val crit = Combat.resoDamage(2700, 0.0, 0, ultra = false, crit = true)
        assertTrue(crit > normal)
    }

    @Test
    fun ultraStrikeKnocksModsBetterThanBurster() {
        assertTrue(Combat.knockChance(0, 0.0, ultra = true) > Combat.knockChance(0, 0.0, ultra = false))
    }

    @Test
    fun knockChanceFallsWithDistance() {
        assertTrue(Combat.knockChance(0, 0.1, ultra = true) > Combat.knockChance(0, 0.8, ultra = true))
    }

    @Test
    fun stickierModsResistKnockOut() {
        val easy = Combat.knockChance(stickiness = 0, distFrac = 0.0, ultra = true) // common shield / link amp
        val sticky = Combat.knockChance(stickiness = 80, distFrac = 0.0, ultra = true) // AEGIS
        assertTrue(sticky < easy, "a high-stickiness shield must be harder to knock out ($sticky vs $easy)")
    }

    @Test
    fun outOfRangeNeverKnocks() {
        assertEquals(0.0, Combat.knockChance(0, distFrac = 1.0, ultra = true))
    }
}
