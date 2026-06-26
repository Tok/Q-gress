package portal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CooldownTest {
    @Test
    fun noCooldownIsHackable() {
        assertTrue(Cooldown.NONE.isHackable())
    }

    @Test
    fun noTimeHasNoCooldown() {
        assertEquals(Cooldown.NONE, Cooldown.valueOf(0))
    }

    @Test
    fun onlyNoCooldownIsHackable() {
        Cooldown.values().filterNot { it == Cooldown.NONE }.forEach {
            assertTrue("$it must not be hackable.") { !it.isHackable() }
        }
    }

    // --- PortalMath.cooldownAfter (pure hack-cooldown math, phase B) --------------

    @Test
    fun aFreshHackIsOnFullCooldown() {
        assertEquals(Cooldown.FIVE, PortalMath.cooldownAfter(ticksSinceLastHack = 0, baseCooldownS = 300), "just hacked")
        assertTrue(!PortalMath.cooldownAfter(0, 300).isHackable())
    }

    @Test
    fun cooldownClearsOnceTheWindowElapses() {
        assertEquals(Cooldown.NONE, PortalMath.cooldownAfter(ticksSinceLastHack = 300, baseCooldownS = 300), "window done")
        assertEquals(Cooldown.NONE, PortalMath.cooldownAfter(ticksSinceLastHack = 9999, baseCooldownS = 300), "long past")
        assertTrue(PortalMath.cooldownAfter(300, 300).isHackable())
    }

    @Test
    fun cooldownDecaysThroughTheBucketsAsTimePasses() {
        // Halfway through a 300s window → bucketed to the next-lower cooldown tier (still not hackable).
        val mid = PortalMath.cooldownAfter(ticksSinceLastHack = 150, baseCooldownS = 300)
        assertTrue(!mid.isHackable(), "still cooling at the halfway point")
        assertTrue(mid.seconds < Cooldown.FIVE.seconds, "but less than a fresh full cooldown")
    }

    // --- PortalMath.isBurnedOut (pure burnout check) ------------------------------

    @Test
    fun burnoutTriggersWhenEveryRecentHackIsInsideTheWindow() {
        val tick = 20000
        assertTrue(PortalMath.isBurnedOut(listOf(tick - 1, tick - 50), tick), "all hacks recent → burnt out")
    }

    @Test
    fun noBurnoutWhileAnOldHackHasAgedOut() {
        val tick = 20000 // BURNOUT window is 14400 ticks; a hack at tick 100 has aged out
        assertTrue(!PortalMath.isBurnedOut(listOf(100, tick - 1), tick), "an aged-out hack means not burnt out")
    }
}
