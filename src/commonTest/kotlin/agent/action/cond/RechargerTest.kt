package agent.action.cond

import Factory
import World
import util.Rng
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * [Recharger] refills a friendly portal's resonators from the agent's XM — a portal in interaction range
 * (no key needed) or a keyed remote one. Covers the negative gate: with no portal in range and no key,
 * there is no chargeable target, so recharging is never offered (however full the agent's XM).
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
    fun aKeylessAgentWithNoPortalInRangeHasNothingToRecharge() {
        val agent = Factory.frog()
        agent.inventory.items.clear() // no keys — and the board holds no portals to be in range of
        agent.addXm(agent.xmCapacity()) // XM bar filled — the only remaining gate is the missing target
        assertFalse(Recharger.isActionPossible(agent), "full XM but no key and nothing in range → nothing to recharge")
    }
}
