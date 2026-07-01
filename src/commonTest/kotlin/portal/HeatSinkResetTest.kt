package portal

import Factory
import items.deployable.HeatSink
import items.deployable.Shield
import items.types.HeatSinkType
import items.types.ShieldType
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Heat-sink "instant cooldown reset on attach" (PLAN portal-mod follow-up): deploying a heat sink wipes the
 * portal-wide hack history ([Portal.lastHacks]) so every agent's cooldown + any burnout clears immediately.
 * Other mods (shields) must leave the cooldown untouched.
 */
class HeatSinkResetTest {

    @Test
    fun deployingAHeatSinkClearsTheHackCooldown() = with(Factory) {
        val portal = portal()
        val agent = frog()
        portal.lastHacks[agent.key()] = mutableListOf(1, 2, 3) // recent hacks → on cooldown
        portal.deployMod(agent, HeatSink(HeatSinkType.COMMON, agent))
        assertTrue(portal.lastHacks.isEmpty(), "a heat sink resets the portal-wide hack history")
    }

    @Test
    fun deployingAShieldLeavesTheHackCooldownIntact() = with(Factory) {
        val portal = portal()
        val agent = frog()
        portal.lastHacks[agent.key()] = mutableListOf(1, 2, 3)
        portal.deployMod(agent, Shield(ShieldType.COMMON, agent))
        assertTrue(portal.lastHacks.isNotEmpty(), "a shield doesn't touch the hack cooldown")
    }
}
