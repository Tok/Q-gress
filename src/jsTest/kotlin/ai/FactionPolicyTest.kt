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
        assertEquals(DomSliderPolicy.DEFAULT_WEIGHT, policy.weight(QActions.HACK))
    }

    @Test
    fun registryDefaultsToDomPolicyPerFaction() {
        assertEquals(DomSliderPolicy.DEFAULT_WEIGHT, FactionPolicies.of(Faction.RES).weight(QActions.ATTACK))
    }

    @Test
    fun setInstallsAVectorPolicyForOneFactionOnly() {
        val vector = SliderVector.uniform(0.1).with(QActions.ATTACK, 0.75)
        FactionPolicies.set(Faction.ENL, SliderVectorPolicy(vector))

        assertEquals(0.75, FactionPolicies.of(Faction.ENL).weight(QActions.ATTACK), "ENL uses the vector")
        assertEquals(
            DomSliderPolicy.DEFAULT_WEIGHT,
            FactionPolicies.of(Faction.RES).weight(QActions.ATTACK),
            "RES still on its default DOM policy",
        )
    }
}
