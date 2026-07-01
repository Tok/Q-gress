package ai

import agent.Faction
import agent.qvalue.QActions
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The player-override layer (PLAN Phase 6.4): a locked slot returns the player's value; the rest pass to the AI. */
class OverridePolicyTest {

    @AfterTest
    fun tearDown() = FactionPolicies.reset()

    @Test
    fun lockedSlotOverridesTheAiBothInWeightAndVector() {
        val ai = SliderVectorPolicy(SliderVector.uniform(0.2))
        val override = OverridePolicy(ai)
        override.lock(QActions.ATTACK, 0.9)

        assertEquals(0.9, override.weight(QActions.ATTACK), "behaviour reads the locked value")
        assertEquals(0.2, override.weight(QActions.LINK), "an unlocked slot still reads the AI")
        assertEquals(0.9, override.currentVector()?.get(QActions.ATTACK), "the display vector shows the lock")
        assertEquals(0.2, override.currentVector()?.get(QActions.LINK))

        override.unlock(QActions.ATTACK)
        assertEquals(0.2, override.weight(QActions.ATTACK), "unlocking hands it back to the AI")
    }

    @Test
    fun registryLockWrapsTheInstalledAiPolicy() {
        FactionPolicies.set(Faction.ENL, SliderVectorPolicy(SliderVector.uniform(0.3)))
        FactionPolicies.lock(Faction.ENL, QActions.DEPLOY, 0.75)

        assertEquals(0.75, FactionPolicies.lockedValue(Faction.ENL, QActions.DEPLOY))
        assertEquals(0.75, FactionPolicies.of(Faction.ENL).weight(QActions.DEPLOY))
        assertNull(FactionPolicies.lockedValue(Faction.ENL, QActions.LINK), "untouched slots aren't locked")
    }
}
