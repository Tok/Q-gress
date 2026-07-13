package agent.action.cond

import Factory
import World
import agent.Agent
import agent.action.ActionItem
import config.Dim
import config.Sim
import items.PowerCube
import items.deployable.Resonator
import items.level.PowerCubeLevel
import portal.Octant
import portal.Portal
import portal.PortalKey
import system.grid.GridFixture
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The positive [Recharger] paths: an agent recharges a damaged friendly portal's resonators — remotely via a
 * held key or standing in range without one — spending its own XM, split evenly with a 1000-XM-per-resonator
 * cap (the authentic Ingress "recharge all"). Also covers the recharge↔recycle loop: a drained agent can't
 * recharge, taps a power cube ([Recycler]), and recharges again.
 */
class RechargerRechargeTest {

    @BeforeTest
    fun reset() {
        Sim.roundField = false
        World.grid = GridFixture("RECHARGE", 180, 120, 2, GridFixture.rleEncode(List(180 * 120) { true })).toGrid()
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        World.isReady = true
        Rng.seed(17)
    }

    @AfterTest
    fun tidy() {
        Sim.roundField = true
        World.grid = emptyMap()
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        World.isReady = false
    }

    // A damaged 8-reso portal owned by [owner] at (500, 500), registered in the world.
    private fun damagedPortal(owner: Agent): Portal {
        val portal = Portal.create(Pos(500, 500))
        portal.deploy(owner, Octant.values().associateWith { Resonator.create(owner, 4) }, Dim.minDeploymentRange.toInt())
        World.allPortals.add(portal)
        portal.filledSlots().forEach { it.resonator?.energy = 10 } // damaged → below full, health ≤ 90
        return portal
    }

    @Test
    fun rechargingFillsResonatorsAndSpendsXm() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        val portal = damagedPortal(owner)
        owner.inventory.items.add(PortalKey(portal, owner)) // a key to the friendly portal
        owner.addXm(owner.xmCapacity())

        assertTrue(Recharger.isActionPossible(owner), "XM + a key to a damaged friendly portal → recharge is possible")
        val energyBefore = portal.filledSlots().sumOf { it.resonator?.energy ?: 0 }
        val xmBefore = owner.xm

        Recharger.performAction(owner)

        assertTrue(portal.filledSlots().sumOf { it.resonator?.energy ?: 0 } > energyBefore, "recharging refills resonator energy")
        assertTrue(owner.xm < xmBefore, "recharging spends the agent's XM")
        assertTrue(owner.action.item == ActionItem.RECHARGE, "performAction commits to RECHARGE")
    }

    @Test
    fun aPortalTheAgentStandsAtNeedsNoKey() {
        val away = Factory.frog()
        World.allAgents.add(away)
        val portal = damagedPortal(away)
        val owner = away.copy(pos = portal.location) // standing AT the portal it works…
        owner.actionPortal = portal // …as its action portal
        owner.inventory.items.clear() // no keys at all
        owner.removeXm(owner.xm)
        owner.addXm(owner.xmCapacity())

        assertTrue(Recharger.isActionPossible(owner), "the damaged friendly portal the agent stands at is chargeable without a key")
        val energyBefore = portal.filledSlots().sumOf { it.resonator?.energy ?: 0 }
        Recharger.performAction(owner)
        assertTrue(portal.filledSlots().sumOf { it.resonator?.energy ?: 0 } > energyBefore, "at-portal recharge refills the resonators")
    }

    @Test
    fun rechargeSplitsXmEvenlyAcrossResonators() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        val portal = damagedPortal(owner)
        owner.inventory.items.clear()
        owner.inventory.items.add(PortalKey(portal, owner))
        owner.ap = 0 // level 1 → 3000 XM capacity, so an even split is 3000/4 = 750 per resonator (under the cap)
        owner.removeXm(owner.xm)
        owner.addXm(owner.xmCapacity())

        assertEquals(4, portal.filledSlots().count(), "one agent deploys at most 4 L4 resonators (authentic limit)")
        Recharger.performAction(owner)

        portal.filledSlots().forEach {
            assertEquals(10 + 750, it.resonator?.energy, "each resonator gets the same even share of the agent's XM")
        }
        assertEquals(0, owner.xm, "the whole bar went into the portal (4 × 750 = 3000)")
    }

    @Test
    fun rechargeCapsAtAuthentic1000XmPerResonator() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        val portal = damagedPortal(owner)
        owner.inventory.items.clear()
        owner.inventory.items.add(PortalKey(portal, owner))
        owner.ap = 400_000 // level 6 → 8000 XM capacity: an even split would be 1000/reso — exactly the cap
        owner.removeXm(owner.xm)
        owner.addXm(owner.xmCapacity())

        Recharger.performAction(owner)

        portal.filledSlots().forEach {
            assertEquals(10 + 1000, it.resonator?.energy, "one recharge feeds a resonator at most 1000 XM (as in Ingress)")
        }
    }

    @Test
    fun aDrainedAgentRecyclesACubeThenRechargesAgain() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        val portal = damagedPortal(owner)
        owner.inventory.items.clear()
        owner.inventory.items.add(PortalKey(portal, owner))
        owner.inventory.items.add(PowerCube(owner, PowerCubeLevel.ONE))
        owner.ap = 0
        owner.removeXm(owner.xm) // drained: below the low-XM mark

        assertFalse(Recharger.isActionPossible(owner), "a drained agent can't recharge")
        assertTrue(Recycler.isActionPossible(owner), "…but it can recycle a power cube for XM")
        Recycler.performAction(owner)
        assertTrue(owner.xm > 0, "the cube refilled the bar")
        assertTrue(Recharger.isActionPossible(owner), "…and recharging is back on — the recharge↔recycle loop")
    }
}
