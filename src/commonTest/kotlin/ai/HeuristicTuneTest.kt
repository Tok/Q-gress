package ai

import agent.qvalue.QActions
import agent.qvalue.QDestinations
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The adaptive driver's pure observation→sliders mapping ([HeuristicTune.tune]) — proves it swings the right
 * way (attack when behind, consolidate when ahead) and never leaves the 0..1 slider range.
 */
class HeuristicTuneTest {

    // A minimal observation: slot 1 = MU dominance (what tune swings on), the rest neutral at 0.5. 13 slots
    // mirrors the live Observation width; tune only reads slots 1 (dominance) and 11 (avg XM).
    private fun obs(muShare: Double) = DoubleArray(13) { if (it == 1) muShare else 0.5 }

    @Test
    fun behindPressesTheAttackAheadConsolidates() {
        val behind = HeuristicTune.tune(obs(0.1))
        val ahead = HeuristicTune.tune(obs(0.9))

        assertTrue(behind[QActions.ATTACK] > ahead[QActions.ATTACK], "a trailing faction attacks harder")
        assertTrue(behind[QDestinations.MOVE_TO_WEAK_ENEMY] > ahead[QDestinations.MOVE_TO_WEAK_ENEMY], "it hunts enemy portals")
        assertTrue(ahead[QActions.LINK] >= behind[QActions.LINK], "a leading faction leans into fields")
        assertTrue(ahead[QDestinations.MOVE_TO_UNCAPTURED] > behind[QDestinations.MOVE_TO_UNCAPTURED], "and claims open ground")
    }

    @Test
    fun everySlotStaysInRange() {
        listOf(0.0, 0.3, 0.5, 0.8, 1.0).forEach { share ->
            val v = HeuristicTune.tune(obs(share))
            SliderVector.ORDER.forEach { assertTrue(v[it] in 0.0..1.0, "${it.id}=${v[it]} out of range at share=$share") }
        }
    }
}
