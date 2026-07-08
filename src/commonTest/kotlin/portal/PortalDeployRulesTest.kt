package portal

import Factory
import World
import agent.Agent
import agent.Faction
import agent.action.cond.Deployer
import config.Dim
import items.deployable.Resonator
import items.level.ResonatorLevel
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The authentic-Ingress resonator deploy/upgrade rules that make portals climb from L1 to L8:
 *
 *  - The per-level deploy caps ([ResonatorLevel.deployablePerPlayer]) are **per player**, not per portal.
 *  - A player can never upgrade their **own** resonator — only fill an empty slot or replace a *teammate's*
 *    lower one. Together with the caps, that means **one agent tops out at L5**, and a **level-8 portal needs
 *    8 different agents** (each contributing one L8).
 *  - Flipping a portal re-owns **every** resonator to the flipper.
 *
 * See [Portal.deploy] / [Portal.findAllowedResoLevels] and [Deployer]. Complements [PortalDeployTest] (the
 * distance clamp) and [agent.action.cond.DeployerTest] (the raw deploy gate).
 */
class PortalDeployRulesTest {

    private val loc = Pos(600, 600)

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.tick = 0
        World.isReady = true
        Rng.seed(707)
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.tick = 0
        World.isReady = false
    }

    private fun agentAt(faction: Faction, portal: Portal): Agent {
        val agent = if (faction == Faction.ENL) Agent.createFrog(Factory.grid(), loc) else Agent.createSmurf(Factory.grid(), loc)
        agent.addXm(agent.xmCapacity())
        agent.addAp(2_000_000) // a high level so any carried resonator is deployable
        agent.actionPortal = portal
        agent.inventory.items.clear()
        World.allAgents.add(agent)
        return agent
    }

    private fun capturedPortal(): Portal = Portal.create(loc).also { World.allPortals.add(it) }

    // 1×L8 + 1×L7 + 2×L6 + 2×L5 + 2×L4 = 45; 45/8 = 5. A lone agent can fill all 8 slots but, capped
    // per level and unable to upgrade its own rods, can push the portal no higher than level 5.
    @Test
    fun oneAgentAloneTopsOutAtLevelFive() {
        val portal = capturedPortal()
        val agent = agentAt(Faction.ENL, portal)
        // A generous stock of every level so greedy "highest first" always has a choice.
        ResonatorLevel.values().forEach { lvl -> repeat(8) { agent.inventory.items.add(Resonator.create(agent, lvl.level)) } }

        repeat(40) { if (Deployer.isActionPossible(agent)) Deployer.performAction(agent) }

        assertEquals(8, portal.filledSlots().count(), "the lone agent fills all 8 slots")
        assertEquals(5, portal.getLevel().value, "but a single player tops out at level 5")
        assertFalse(Deployer.isActionPossible(agent), "with no slot it may touch, it stops (no self-upgrade loop)")
    }

    // The headline rule: eight distinct agents, each dropping their one allowed L8, make a level-8 portal.
    @Test
    fun eightDifferentAgentsReachLevelEight() {
        val portal = capturedPortal()
        val agents = (1..8).map { agentAt(Faction.ENL, portal).also { it.inventory.items.add(Resonator.create(it, 8)) } }

        agents.forEach { Deployer.performAction(it) }

        assertEquals(8, portal.filledSlots().count { it.resonator?.level == ResonatorLevel.EIGHT }, "8 L8 resonators")
        assertEquals(8, portal.getLevel().value, "8×L8 → a level-8 portal")
        assertEquals(8, portal.filledSlots().mapNotNull { it.owner }.toSet().size, "each L8 is owned by a distinct agent")

        val ninth = agentAt(Faction.ENL, portal).also { it.inventory.items.add(Resonator.create(it, 8)) }
        assertFalse(Deployer.isActionPossible(ninth), "a 9th L8 has nowhere to go — the portal is maxed")
    }

    // Deploying a higher reso onto a slot you already own is a no-op (you must let a teammate upgrade it).
    @Test
    fun aPlayerCannotUpgradeTheirOwnResonator() {
        val portal = capturedPortal()
        val agent = agentAt(Faction.ENL, portal)
        portal.deploy(agent, mapOf(Octant.N to Resonator.create(agent, 1)), Dim.minDeploymentRange.toInt())

        portal.deploy(agent, mapOf(Octant.N to Resonator.create(agent, 8)), Dim.minDeploymentRange.toInt())

        assertEquals(ResonatorLevel.ONE, portal.slots.getValue(Octant.N).resonator?.level, "the agent's own reso is untouched")
        assertEquals(agent, portal.slots.getValue(Octant.N).owner, "and still owned by the same agent")
    }

    // Even when it owns every (low) slot and carries a high reso, an agent can't lift the portal alone.
    @Test
    fun anAgentOwningEverySlotCannotUpgradeItsOwnRods() {
        val portal = capturedPortal()
        val agent = agentAt(Faction.ENL, portal)
        Octant.values().forEach { portal.deploy(agent, mapOf(it to Resonator.create(agent, 1)), Dim.minDeploymentRange.toInt()) }
        assertEquals(8, portal.filledSlots().count { it.owner == agent }, "the agent owns all 8 L1 slots")

        agent.inventory.items.clear()
        agent.inventory.items.add(Resonator.create(agent, 8)) // an L8 it may only spend on OTHERS' rods

        assertFalse(Deployer.isActionPossible(agent), "it can't upgrade its own resonators → no deploy offered")
        assertEquals(1, portal.getLevel().value, "the portal is stuck at L1 until a teammate helps")
    }

    // The upgrade path: a teammate replaces a lower reso with a higher one and takes ownership of that slot.
    @Test
    fun aTeammateCanUpgradeALowerResonatorAndTakesTheSlot() {
        val portal = capturedPortal()
        val a = agentAt(Faction.ENL, portal)
        Octant.values().forEach { portal.deploy(a, mapOf(it to Resonator.create(a, 1)), Dim.minDeploymentRange.toInt()) }

        val b = agentAt(Faction.ENL, portal)
        b.inventory.items.add(Resonator.create(b, 8))
        assertTrue(Deployer.isActionPossible(b), "a teammate may upgrade a's lower resonators")

        Deployer.performAction(b)

        assertEquals(1, portal.filledSlots().count { it.resonator?.level == ResonatorLevel.EIGHT }, "one slot upgraded to L8")
        val upgraded = portal.slots.values.first { it.resonator?.level == ResonatorLevel.EIGHT }
        assertEquals(b, upgraded.owner, "the upgraded slot is re-owned by the upgrader")
    }

    // Flipping (virus) hands the whole portal — every resonator — to the flipper.
    @Test
    fun flippingReassignsEveryResonatorToTheFlipper() {
        val portal = capturedPortal()
        val a = agentAt(Faction.ENL, portal)
        val b = agentAt(Faction.ENL, portal)
        portal.deploy(a, mapOf(Octant.N to Resonator.create(a, 1)), Dim.minDeploymentRange.toInt())
        portal.deploy(b, mapOf(Octant.S to Resonator.create(b, 1)), Dim.minDeploymentRange.toInt())
        assertEquals(2, portal.filledSlots().mapNotNull { it.owner }.toSet().size, "two different owners before the flip")

        val flipper = agentAt(Faction.RES, portal)
        portal.refactor(flipper, Faction.RES)

        assertEquals(Faction.RES, portal.owner?.faction, "the portal flips to RES")
        portal.filledSlots().forEach { assertEquals(flipper, it.owner, "every resonator is re-owned by the flipper") }
    }
}
