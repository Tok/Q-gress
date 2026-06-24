package ai

import agent.qvalue.QActions
import agent.qvalue.QDestinations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The adaptive driver's pure observation→sliders mapping ([HeuristicPolicy.tune]) — proves it swings the
 * right way (attack when behind, consolidate when ahead) and never leaves the 0..1 slider range.
 */
class HeuristicPolicyTest {

    // An observation with a given MU dominance (slot 1); everything else neutral at 0.5.
    private fun obs(muShare: Double) = DoubleArray(Observation.SIZE) { if (it == 1) muShare else 0.5 }

    @Test
    fun behindPressesTheAttackAheadConsolidates() {
        val behind = HeuristicPolicy.tune(obs(0.1))
        val ahead = HeuristicPolicy.tune(obs(0.9))

        assertTrue(behind[QActions.ATTACK] > ahead[QActions.ATTACK], "a trailing faction attacks harder")
        assertTrue(behind[QDestinations.MOVE_TO_WEAK_ENEMY] > ahead[QDestinations.MOVE_TO_WEAK_ENEMY], "it hunts enemy portals")
        assertTrue(ahead[QActions.LINK] >= behind[QActions.LINK], "a leading faction leans into fields")
        assertTrue(ahead[QDestinations.MOVE_TO_UNCAPTURED] > behind[QDestinations.MOVE_TO_UNCAPTURED], "and claims open ground")
    }

    @Test
    fun everySlotStaysInRange() {
        listOf(0.0, 0.3, 0.5, 0.8, 1.0).forEach { share ->
            val v = HeuristicPolicy.tune(obs(share))
            SliderVector.ORDER.forEach { assertTrue(v[it] in 0.0..1.0, "${it.id}=${v[it]} out of range at share=$share") }
        }
    }

    @Test
    fun sliderVectorPolicyExposesItsVectorForDisplay() {
        val vector = SliderVector.uniform(0.42)
        assertEquals(0.42, SliderVectorPolicy(vector).currentVector()[QActions.LINK], "currentVector mirrors the driven vector")
        assertEquals(null, DomSliderPolicy(agent.Faction.ENL).currentVector(), "the manual policy is NOT an AI in control")
    }
}
