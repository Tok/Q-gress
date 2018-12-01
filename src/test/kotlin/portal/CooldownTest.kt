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
}
