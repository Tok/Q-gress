package agent.action.cond

import Factory
import World
import agent.Agent
import agent.Faction
import items.deployable.HeatSink
import items.deployable.Multihack
import items.deployable.Resonator
import items.deployable.Shield
import items.types.HeatSinkType
import items.types.MultihackType
import items.types.ShieldType
import portal.Portal
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [Deployer.performAction] end to end: dropping an actual resonator into a slot (slot filled, reso consumed) and,
 * once resonators are exhausted, slotting a mod (shield / heat sink / multi-hack) into a free mod slot. Complements
 * [DeployerTest], which covers only the gate + the raw [Portal.deploy] rules.
 */
class DeployerDeployTest {

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.tick = 0
        Rng.seed(505)
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.tick = 0
    }

    private val loc = Pos(600, 600)

    private fun agentAtPortal(faction: Faction, portal: Portal): Agent {
        val agent = if (faction == Faction.ENL) Agent.createFrog(Factory.grid(), loc) else Agent.createSmurf(Factory.grid(), loc)
        agent.addXm(agent.xmCapacity())
        agent.addAp(2_000_000) // a high level so any carried resonator is deployable
        agent.actionPortal = portal
        agent.inventory.items.clear()
        return agent
    }

    @Test
    fun performActionDeploysAResonatorAndConsumesIt() = with(Factory) {
        val portal = Portal.create(loc) // neutral → the agent captures it by deploying
        World.allPortals.add(portal)
        val agent = agentAtPortal(Faction.ENL, portal)
        agent.inventory.items.add(Resonator.create(agent, 1))
        val filledBefore = portal.filledSlots().count()

        assertTrue(Deployer.isActionPossible(agent), "a captured/neutral portal + a carried reso → deploy is possible")
        Deployer.performAction(agent)

        assertEquals(filledBefore + 1, portal.filledSlots().count(), "a resonator lands in an empty slot")
        assertTrue(agent.inventory.findResonators().isEmpty(), "the deployed resonator is consumed from inventory")
        assertEquals(agent, portal.owner, "deploying onto a neutral portal captures it")
    }

    @Test
    fun performActionSlotsAModWhenNoResoFits() = with(Factory) {
        val portal = portal(Faction.ENL) // already ours, carrying one resonator
        portal.owner = null // detach the random starter owner so we can re-own it as our test agent
        World.allPortals.add(portal)
        val agent = agentAtPortal(Faction.ENL, portal)
        portal.owner = agent
        portal.slots.values.forEach { it.owner = agent }
        // The agent carries NO resonators (only a shield), so the reso branch can't fire → the mod branch does.
        agent.inventory.items.add(Shield(ShieldType.COMMON, agent))
        val modsBefore = portal.modCount()

        Deployer.performAction(agent)

        assertEquals(modsBefore + 1, portal.modCount(), "with no reso to place, a mod is slotted instead")
        assertTrue(agent.inventory.findShields().isEmpty(), "the deployed shield is consumed")
    }

    @Test
    fun performActionSlotsAHeatSinkWhenNoResoFits() = with(Factory) {
        val portal = portal(Faction.ENL)
        portal.owner = null // detach the random starter owner so we can re-own it below
        World.allPortals.add(portal)
        val agent = agentAtPortal(Faction.ENL, portal)
        portal.owner = agent
        portal.slots.values.forEach { it.owner = agent } // full slots → only the mod branch can act
        agent.inventory.items.add(HeatSink(HeatSinkType.COMMON, agent))
        agent.inventory.items.add(Multihack(MultihackType.COMMON, agent))
        val modsBefore = portal.modCount()

        Deployer.performAction(agent)

        assertEquals(modsBefore + 1, portal.modCount(), "a heat sink is slotted when no resonator fits")
        assertTrue(agent.inventory.findHeatSinks().isEmpty(), "the deployed heat sink is consumed (shields-first ordering)")
    }
}
