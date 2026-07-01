package agent.action

import Factory
import World
import agent.Faction
import config.Dim
import config.Sim
import items.deployable.Resonator
import portal.Octant
import portal.Portal
import system.grid.GridFixture
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * [ActionSelector.doSomethingElse] — the action roulette an agent runs when it has no committed action. It
 * branches on where the agent stands (at a friendly / enemy / neutral portal, or out in the open) and spins the
 * Q-weighted wheel of possible actions. These characterization tests drive each branch to a real, non-throwing
 * decision; the pure `q` scoring itself is covered by [ActionSelectorTest].
 */
class ActionSelectorRouletteTest {

    @BeforeTest
    fun reset() {
        Sim.roundField = false
        World.grid = GridFixture("SEL", 180, 120, 2, GridFixture.rleEncode(List(180 * 120) { true })).toGrid()
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        Rng.seed(5)
    }

    @AfterTest
    fun tidy() {
        Sim.roundField = true
        World.grid = emptyMap()
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
    }

    private fun ownedPortalAt(pos: Pos, faction: Faction): Portal {
        val p = Portal.create(pos)
        val a = Factory.agent(faction)
        p.owner = a
        p.slots.getValue(Octant.N).deployReso(a, Resonator.create(a, 1), Dim.maxDeploymentRange.toInt())
        return p
    }

    @Test
    fun awayFromAnyPortalTheAgentPicksAnAnywhereAction() {
        World.allPortals.add(Portal.create(Pos(1600, 400)))
        val agent = Factory.frog().copy(pos = Pos(50, 50))
        agent.actionPortal = World.allPortals.first() // far away → not at the portal
        assertNotNull(ActionSelector.doSomethingElse(agent), "an agent between portals still lands on a decision")
    }

    @Test
    fun atANeutralPortalTheAgentDecides() {
        val neutral = Portal.create(Pos(600, 400))
        World.allPortals.add(neutral)
        val agent = Factory.frog().copy(pos = neutral.location) // standing on it
        agent.actionPortal = neutral
        assertNotNull(ActionSelector.doSomethingElse(agent), "at a neutral portal the roulette resolves")
    }

    @Test
    fun atAFriendlyPortalTheAgentDecides() {
        val friendly = ownedPortalAt(Pos(600, 400), Faction.ENL)
        World.allPortals.add(friendly)
        val agent = Factory.frog().copy(pos = friendly.location)
        agent.actionPortal = friendly
        World.allAgents.add(agent)
        assertNotNull(ActionSelector.doSomethingElse(agent), "at a friendly portal the roulette resolves")
    }

    @Test
    fun atAnEnemyPortalTheAgentDecides() {
        val enemy = ownedPortalAt(Pos(600, 400), Faction.RES)
        World.allPortals.add(enemy)
        val agent = Factory.frog().copy(pos = enemy.location)
        agent.actionPortal = enemy
        assertNotNull(ActionSelector.doSomethingElse(agent), "at an enemy portal the roulette resolves")
    }
}
