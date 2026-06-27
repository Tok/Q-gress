package ai

import agent.Faction
import agent.qvalue.QActions
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FactionPolicyTest {

    @AfterTest
    fun restoreDefaults() = FactionPolicies.reset()

    @Test
    fun domPolicyFallsBackToDefaultWeightHeadless() {
        // No tuning UI in the Node test → getElementById is null → the default weighting.
        val policy = DomSliderPolicy(Faction.ENL)
        assertEquals(SliderVector.DEFAULT_WEIGHT, policy.weight(QActions.HACK))
    }

    @Test
    fun registryDefaultsToDomPolicyPerFaction() {
        assertEquals(SliderVector.DEFAULT_WEIGHT, FactionPolicies.of(Faction.RES).weight(QActions.ATTACK))
    }

    @Test
    fun setInstallsAVectorPolicyForOneFactionOnly() {
        val vector = SliderVector.uniform(0.1).with(QActions.ATTACK, 0.75)
        FactionPolicies.set(Faction.ENL, SliderVectorPolicy(vector))

        assertEquals(0.75, FactionPolicies.of(Faction.ENL).weight(QActions.ATTACK), "ENL uses the vector")
        assertEquals(
            SliderVector.DEFAULT_WEIGHT,
            FactionPolicies.of(Faction.RES).weight(QActions.ATTACK),
            "RES still on its default DOM policy",
        )
    }

    @Test
    fun currentVectorMarksWhoIsInControl() {
        // A SliderVectorPolicy exposes its driven vector (an AI is in control → the tuning UI mirrors it);
        // a DomSliderPolicy returns null (the manual sliders ARE the source, so the UI stays interactive).
        val vector = SliderVector.uniform(0.42)
        assertEquals(0.42, SliderVectorPolicy(vector).currentVector()[QActions.LINK], "currentVector mirrors the driven vector")
        assertEquals(null, DomSliderPolicy(Faction.ENL).currentVector(), "the manual policy is NOT an AI in control")
    }
}
