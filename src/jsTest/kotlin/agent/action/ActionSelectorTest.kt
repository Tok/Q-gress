package agent.action

import agent.Faction
import agent.qvalue.QActions
import ai.FactionPolicies
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Characterization tests (PLAN non-functional track, phase A) for [ActionSelector.q], the pure scoring used to
 * weight every AI action. `q` is the faction policy's per-slider weight × the QValue's own intrinsic weight;
 * with a uniform headless policy weight, the QValue weights set the priority ordering that makes agents
 * consolidate ground (LINK > DEPLOY > HACK > MOVE) rather than hack-and-capture forever.
 */
class ActionSelectorTest {

    @Test
    fun qIsThePolicyWeightTimesTheValueWeight() {
        val faction = Faction.ENL
        val policy = FactionPolicies.of(faction)
        listOf(QActions.MOVE_ELSEWHERE, QActions.HACK, QActions.DEPLOY, QActions.LINK, QActions.ATTACK).forEach { v ->
            assertEquals(policy.weight(v) * v.weight, ActionSelector.q(faction, v), 1e-12, "q == policyWeight × ${v.id}")
        }
    }

    @Test
    fun fieldMakingActionsOutrankBusywork() {
        Faction.values().forEach { faction ->
            val link = ActionSelector.q(faction, QActions.LINK)
            val deploy = ActionSelector.q(faction, QActions.DEPLOY)
            val hack = ActionSelector.q(faction, QActions.HACK)
            val move = ActionSelector.q(faction, QActions.MOVE_ELSEWHERE)
            assertTrue(link > deploy, "link (the field-maker) is the top priority for $faction")
            assertTrue(deploy > hack, "deploying consolidates ground above plain hacking for $faction")
            assertTrue(hack > move, "any portal work beats wandering for $faction")
        }
    }
}
