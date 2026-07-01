package agent

import Factory
import World
import agent.action.ActionItem
import config.Sim
import portal.Portal
import system.grid.GridFixture
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The [Agent] core the AI drives: AP → level and the XM bar clamp, the [Agent.act] dispatch into the committed
 * ATTACK/DEPLOY handlers, the idle [Agent.moveElsewhere] relocation, and identity ([Agent.key]/equals). The pure
 * attack-targeting filter is covered by [AgentTargetingTest]; here we exercise the stateful instance methods.
 */
class AgentCoreTest {

    @BeforeTest
    fun reset() {
        Sim.roundField = false
        World.grid = GridFixture("AGENT", 180, 120, 2, GridFixture.rleEncode(List(180 * 120) { true })).toGrid()
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
        Rng.seed(11)
    }

    @AfterTest
    fun tidy() {
        Sim.roundField = true
        World.grid = emptyMap()
        World.allPortals.clear()
        World.allAgents.clear()
        World.tick = 0
    }

    // A RES-owned portal (resonator deployed) at a fixed spot, a valid enemy target for a frog.
    private fun enemyPortalAt(pos: Pos): Portal {
        val p = Portal.create(pos)
        val foe = Factory.smurf()
        p.owner = foe
        p.slots.getValue(portal.Octant.N).deployReso(foe, Factory.resonator(foe, 1), config.Dim.maxDeploymentRange.toInt())
        return p
    }

    @Test
    fun levelRisesWithActionPoints() {
        val agent = Factory.frog()
        agent.ap = 5_000
        assertEquals(1, agent.getLevel(), "≤10k AP is level 1")
        agent.ap = 20_000
        assertEquals(2, agent.getLevel(), "20k AP is level 2")
        agent.ap = 50_000
        assertEquals(3, agent.getLevel(), "50k AP is level 3")
    }

    @Test
    fun addApScalesByTheApMultiplier() {
        val agent = Factory.frog()
        agent.ap = 0
        agent.addAp(100) // × apMultiplier (10) × progressSpeed (1.0)
        assertEquals(1_000, agent.ap, "addAp applies the AP multiplier")
    }

    @Test
    fun xmClampsToCapacity() {
        val agent = Factory.frog()
        agent.ap = 5_000 // level 1 → capacity 3000
        assertEquals(3_000, agent.xmCapacity(), "level-1 XM capacity")
        agent.addXm(99_999)
        assertEquals(agent.xmCapacity(), agent.xm, "addXm never exceeds capacity")
        agent.removeXm(99_999)
        assertEquals(0, agent.xm, "removeXm never drops below zero")
    }

    @Test
    fun capacityGrowsWithLevel() {
        val agent = Factory.frog()
        agent.ap = 20_000 // level 2
        assertEquals(4_000, agent.xmCapacity(), "level-2 capacity")
        agent.ap = 200_000 // level 5
        assertEquals(7_000, agent.xmCapacity(), "level-5 capacity")
    }

    @Test
    fun aCopyKeepsIdentity() {
        val agent = Factory.frog()
        val moved = agent.copy(pos = Pos(123, 456))
        assertEquals(agent, moved, "a movement copy() is the same agent (faction + name key)")
        assertEquals(agent.hashCode(), moved.hashCode(), "identity hash is stable across a move")
        assertEquals(agent.key(), moved.key(), "the stable key survives the copy")
        assertTrue(agent.toString().contains(agent.faction.abbr), "toString carries the faction")
    }

    @Test
    fun attackFirstCommitsToTheAttackAction() {
        val agent = Factory.frog()
        agent.actionPortal = enemyPortalAt(Pos(600, 400))
        agent.attackPortal(true)
        assertEquals(ActionItem.ATTACK, agent.action.item, "attackPortal(true) starts the ATTACK action")
    }

    @Test
    fun actRoutesAnAttackerTowardItsTarget() {
        val enemy = enemyPortalAt(Pos(1500, 400))
        val agent = Factory.frog().copy(pos = Pos(50, 400))
        agent.actionPortal = enemy
        agent.destination = enemy.location
        agent.action.start(ActionItem.ATTACK) // committed + busy → act() drives the move-into-range
        val next = agent.act()
        assertEquals(ActionItem.ATTACK, next.action.item, "act() keeps driving a committed ATTACK")
        assertTrue(next.pos.distanceTo(enemy.location) < agent.pos.distanceTo(enemy.location), "it closed on the target")
    }

    @Test
    fun deployFirstCommitsToTheDeployAction() {
        val agent = Factory.frog()
        agent.actionPortal = Factory.portal() // neutral
        agent.deployPortal(true)
        assertEquals(ActionItem.DEPLOY, agent.action.item, "deployPortal(true) starts the DEPLOY action")
    }

    @Test
    fun moveElsewhereHeadsForAPortal() {
        World.allPortals.add(enemyPortalAt(Pos(800, 200)))
        World.allPortals.add(Factory.portal())
        val next = Factory.frog().copy(pos = Pos(50, 50)).moveElsewhere()
        assertTrue(
            next.action.item == ActionItem.MOVE || next.action.item == ActionItem.EXPLORE,
            "an idle agent relocates (MOVE toward a portal, or EXPLORE when none are away) — never idles",
        )
    }

    @Test
    fun recoverIfStuckIsANoOpWhenNotStuck() {
        val agent = Factory.frog()
        StuckTracker.reset() // nothing sampled → not stuck
        val portalBefore = agent.actionPortal
        agent.recoverIfStuck()
        assertEquals(portalBefore, agent.actionPortal, "a free agent isn't re-targeted")
    }

    @Test
    fun recoverIfStuckBeelinesAStuckAgentBeforeReTargeting() {
        val agent = Factory.frog().copy(pos = Pos(500, 500))
        World.allAgents.add(agent)
        // Feed StuckTracker enough same-spot samples to flag this agent (net displacement under a deploy range).
        repeat(40) { StuckTracker.sample(listOf(agent.key() to agent.pos)) }
        assertTrue(StuckTracker.isStuck(agent.key()), "a pinned agent registers as stuck")
        val portalBefore = agent.actionPortal
        agent.recoverIfStuck() // first escalation: spend a bee-line, don't re-target yet
        assertEquals(portalBefore, agent.actionPortal, "the first recovery step is a bee-line, not a re-target")
        StuckTracker.reset()
    }
}
