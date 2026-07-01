package agent.action.cond

import Factory
import World
import agent.action.ActionItem
import config.Dim
import config.Sim
import items.deployable.Resonator
import portal.Octant
import portal.Portal
import portal.PortalKey
import system.grid.GridFixture
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The positive [Recharger] path: an agent with a full XM bar holding a key to a damaged friendly portal
 * recharges its resonators from range — filling their energy and spending the agent's XM.
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

    @Test
    fun rechargingFillsResonatorsAndSpendsXm() {
        val owner = Factory.frog()
        World.allAgents.add(owner)
        val portal = Portal.create(Pos(500, 500))
        portal.deploy(owner, Octant.values().associateWith { Resonator.create(owner, 4) }, Dim.minDeploymentRange.toInt())
        World.allPortals.add(portal)
        portal.filledSlots().forEach { it.resonator?.energy = 10 } // damaged → below full, health ≤ 90
        owner.inventory.items.add(PortalKey(portal, owner)) // a key to the friendly portal
        owner.addXm(owner.xmCapacity())

        assertTrue(Recharger.isActionPossible(owner), "full XM + a key to a damaged friendly portal → recharge is possible")
        val energyBefore = portal.filledSlots().sumOf { it.resonator?.energy ?: 0 }
        val xmBefore = owner.xm

        Recharger.performAction(owner)

        assertTrue(portal.filledSlots().sumOf { it.resonator?.energy ?: 0 } > energyBefore, "recharging refills resonator energy")
        assertTrue(owner.xm < xmBefore, "recharging spends the agent's XM")
        assertTrue(owner.action.item == ActionItem.RECHARGE, "performAction commits to RECHARGE")
    }
}
