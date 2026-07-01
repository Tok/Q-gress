package items.deployable

import Factory
import World
import portal.Octant
import portal.Portal
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [Resonator] lifecycle: [deploy] (position/octant/portal wired up), [recharge] (energy back up, XM spent, AP
 * gained, capped at capacity), and [takeDamage] (energy drops; a full drain destroys it, awards AP, clears its slot).
 */
class ResonatorDeployTest {

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        Rng.seed(606)
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
    }

    @Test
    fun deployWiresUpPortalOctantAndPosition() = with(Factory) {
        val agent = frog()
        val reso = Resonator.create(agent, 1)
        val portal = Portal.create(Pos(100, 100))
        reso.deploy(portal, Octant.N, Pos(100, 66))
        assertEquals(portal, reso.portal, "deploy records the portal")
        assertEquals(Octant.N, reso.octant, "deploy records the octant")
        assertEquals(Pos(100, 66), reso.position, "deploy records the world position")
    }

    @Test
    fun rechargeRefillsEnergySpendsXmAndAwardsAp() = with(Factory) {
        val agent = frog()
        agent.addXm(agent.xmCapacity())
        val reso = Resonator.create(agent, 2)
        reso.energy = reso.totalCapacity() - 400 // 400 room to top up
        val xmBefore = agent.xm
        val apBefore = agent.ap

        reso.recharge(agent, 1000) // more than the open capacity → clamps to the 400 gap

        assertEquals(reso.totalCapacity(), reso.energy, "recharge fills up to (but not past) capacity")
        assertEquals(xmBefore - 400, agent.xm, "only the energy actually added costs XM")
        assertTrue(agent.ap > apBefore, "recharging awards AP")
    }

    @Test
    fun takeDamageChipsEnergyWithoutDestroyingWhenSurviving() = with(Factory) {
        val agent = frog()
        val reso = Resonator.create(agent, 3)
        val before = reso.energy
        reso.takeDamage(agent, 100)
        assertEquals(before - 100, reso.energy, "a non-lethal hit just lowers energy")
    }

    @Test
    fun takeDamageDestroysAndClearsTheSlotOnFullDrain() = with(Factory) {
        val owner = smurf()
        val portal = Portal.create(Pos(200, 200))
        portal.deploy(owner, mapOf(Octant.N to Resonator.create(owner, 1)), 13)
        val reso = requireNotNull(portal.slots.getValue(Octant.N).resonator)
        val attacker = frog()
        val apBefore = attacker.ap

        reso.takeDamage(attacker, reso.energy + 50) // overkill → destroyed

        assertEquals(0, reso.energy, "the drained resonator has no energy left")
        assertNull(portal.slots.getValue(Octant.N).resonator, "the destroyed resonator is cleared from its slot")
        assertTrue(attacker.ap > apBefore, "destroying a resonator awards the attacker AP")
    }
}
