package agent.action.cond

import Factory
import World
import util.Rng
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * [Recharger] tops up a friendly portal's resonators from range using a held key. It only fires when the agent's
 * XM bar is full AND it holds a key to a below-full friendly portal. Covers the negative gate: a keyless agent
 * (however full its XM) has no chargeable portal, so recharging is never offered.
 */
class RechargerTest {

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        Rng.seed(17)
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
    }

    @Test
    fun aKeylessAgentHasNothingToRecharge() {
        val agent = Factory.frog()
        agent.inventory.items.clear() // no keys → no chargeable portal
        agent.addXm(agent.xmCapacity()) // XM bar filled — the only remaining gate is the (empty) key set
        assertFalse(Recharger.isActionPossible(agent), "full XM but no key → nothing to recharge")
    }
}
