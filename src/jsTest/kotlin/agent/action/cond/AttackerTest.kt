package agent.action.cond

import Factory
import agent.Faction
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression for the "stuck attacking nothing" bug: a well-stocked agent (XMPs ≥ threshold) that commits to
 * ATTACK keeps re-entering [agent.Agent.attackPortal] every tick. The only thing that breaks the loop when the
 * target dies *without* the agent spending its XMPs (neutralised by someone else, flipped friendly, or its own
 * takedown finishing) is [Attacker.isTargetValid] going false. If it stayed true the agent would re-fire 0-volley
 * assaults on a dead portal forever (the symptom seen in the title animation). Mirrors DeployerTest's
 * globalSameLevelCapOffersNoDeploy guard for the deploy-loop.
 */
class AttackerTest {

    @Test
    fun enemyPortalWithResosIsAValidTarget() = with(Factory) {
        Faction.values().forEach { faction ->
            val agent = agent(faction)
            agent.actionPortal = portal(faction.enemy()) // enemy-owned, carries a resonator
            assertTrue(Attacker.isTargetValid(agent), "an enemy portal with resos left is worth attacking")
        }
    }

    @Test
    fun friendlyPortalIsNotAValidTarget() = with(Factory) {
        Faction.values().forEach { faction ->
            val agent = agent(faction)
            agent.actionPortal = portal(faction) // flipped to our own faction mid-assault
            assertFalse(Attacker.isTargetValid(agent), "never keep attacking a portal that's now ours")
        }
    }

    @Test
    fun neutralisedPortalIsNotAValidTarget() = with(Factory) {
        Faction.values().forEach { faction ->
            val agent = agent(faction)
            agent.actionPortal = portal() // fresh/neutral: no owner, no resonators → nothing to destroy
            assertFalse(
                Attacker.isTargetValid(agent),
                "a portal with no resos left is dead — abandon it so the agent re-selects (was an infinite loop)",
            )
        }
    }
}
