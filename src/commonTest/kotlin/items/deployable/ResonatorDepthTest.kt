package items.deployable

import Factory
import World
import config.Dim
import portal.Octant
import portal.Portal
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The [Resonator] energy model: recharge (capped at capacity), scaled decay + the auto-remove when it hits
 * zero, combat damage (AP on kill), the critical-level read and the id/label accessors.
 */
class ResonatorDepthTest {

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        World.isReady = true
        Rng.seed(23)
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        World.isReady = false
    }

    private fun deployedReso(): Pair<Portal, Resonator> {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        val portal = Portal.create(Pos(500, 500))
        portal.deploy(owner, mapOf(Octant.N to Resonator.create(owner, 4)), Dim.minDeploymentRange.toInt())
        World.allPortals.add(portal)
        val reso = requireNotNull(portal.slots.getValue(Octant.N).resonator) { "the resonator is deployed" }
        return portal to reso
    }

    @Test
    fun rechargeFillsUpToCapacityAndSpendsAgentXm() {
        val (_, reso) = deployedReso()
        reso.energy = reso.totalCapacity() / 2
        val agent = Factory.frog()
        agent.addXm(agent.xmCapacity())
        val xmBefore = agent.xm
        reso.recharge(agent, 100_000) // more than the open capacity
        assertEquals(reso.totalCapacity(), reso.energy, "recharge never overfills")
        assertTrue(agent.xm < xmBefore, "recharging cost the agent XM")
        assertEquals(0, reso.openCapacity(), "a full resonator has no open capacity")
    }

    @Test
    fun decayScalesAndAutoRemovesWhenDrained() {
        val (portal, reso) = deployedReso()
        val energyBefore = reso.energy
        reso.decay(0.5) // half the normal decay
        assertTrue(reso.energy in 1 until energyBefore, "a scaled decay bleeds a fraction of energy")

        reso.energy = 1
        reso.decay() // full decay from near-zero → drains → auto-remove
        assertEquals(0, portal.numberOfResosLeft(), "a drained resonator removes itself from the portal")
    }

    @Test
    fun takeDamageDestroysAndAwardsApWhenDrained() {
        val (portal, reso) = deployedReso()
        val attacker = Factory.smurf()
        val apBefore = attacker.ap
        reso.takeDamage(attacker, reso.energy + 5) // over-kill → destroyed
        assertEquals(0, portal.numberOfResosLeft(), "a fully-drained resonator is destroyed")
        assertTrue(attacker.ap > apBefore, "the attacker earns AP for the kill")
    }

    @Test
    fun criticalLevelAndLabels() {
        val owner = Factory.frog()
        val reso = Resonator.create(owner, 4)
        assertTrue(reso.calcHealthPercent() > 20, "a fresh resonator is above critical")
        reso.energy = reso.totalCapacity() / 10
        assertTrue(reso.isAtCriticalLevel(), "a near-drained resonator reads critical")
        assertEquals("R4", reso.toString(), "the label is R + level")
        assertEquals(owner.key(), reso.getOwnerId(), "the owner id is the owner's key")
        assertEquals(4, reso.getLevel(), "the level accessor reads the level")
    }
}
