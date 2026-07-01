package agent.action.cond

import Factory
import World
import agent.AgentSize
import agent.NonFaction
import agent.action.ActionItem
import config.Config
import extension.VectorField
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [Recruiter] — the idle fallback that converts NPCs. Covers [canRecruit] (roster room + concurrent-recruiter
 * cap), [performAction] heading to the nearest NPC, and [resolve] actually converting an NPC on a successful roll.
 */
class RecruiterRecruitTest {

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.pendingAgents.clear()
        World.tick = 0
        NonFaction.reset()
        Rng.seed(404)
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.pendingAgents.clear()
        World.tick = 0
        NonFaction.reset()
    }

    private fun npc(pos: Pos) =
        NonFaction(pos, speed = 5.0, size = AgentSize(0), destination = pos, vectors = VectorField.EMPTY, busyUntil = -1)

    @Test
    fun canRecruitOnlyWhileUnderTheConcurrentCap() = with(Factory) {
        World.allNonFaction.add(npc(Pos(50, 50)))
        val agent = frog()
        assertTrue(Recruiter.canRecruit(agent), "roster room + no other recruiters → an idle agent may recruit")

        // Saturate the faction's recruiter slots with agents already on RECRUIT.
        repeat(Config.maxConcurrentRecruiters) {
            val busy = frog()
            busy.action.start(ActionItem.RECRUIT)
            World.allAgents.add(busy)
        }
        assertFalse(Recruiter.canRecruit(agent), "at the concurrent-recruiter cap, no further idle agent recruits")
    }

    @Test
    fun performActionTargetsAndWalksToTheNearestNpc() = with(Factory) {
        val near = npc(Pos(20, 20))
        val far = npc(Pos(4000, 4000))
        World.allNonFaction.add(near)
        World.allNonFaction.add(far)
        val agent = frog()

        Recruiter.performAction(agent)

        assertEquals(near.id, agent.recruitTargetId, "the recruiter targets the NEAREST NPC")
        assertEquals(near.pos, agent.destination, "and heads for that NPC's position")
        assertEquals(ActionItem.RECRUIT, agent.action.item, "the RECRUIT action is now underway")
    }

    @Test
    fun resolveConvertsAnNpcOnSuccess() = with(Factory) {
        // Many attempts, each on a fresh NPC: with a positive per-meeting chance at least one converts (removed
        // from the crowd, a pending agent queued). Deterministic under the fixed seed.
        Config.startStage = config.StartStage.MID
        World.grid = emptyMap() // a successful recruit mints a teammate via Agent.createFrog(World.grid, …)
        // Keep the crowd above MIN_NONFACTION so a successful recruit never triggers a World.grid-backed refill.
        repeat(Config.MIN_NONFACTION + 10) { i -> World.allNonFaction.add(npc(Pos(1000 + i, 1000 + i))) }
        val agent = frog()
        var converted = 0
        repeat(200) {
            val target = npc(Pos(10, 10))
            World.allNonFaction.add(target)
            val crowdBefore = World.countNonFaction()
            Recruiter.resolve(agent, target)
            if (World.countNonFaction() < crowdBefore || !World.allNonFaction.contains(target)) converted++
        }
        assertTrue(converted > 0, "over many meetings at least one NPC is successfully recruited")
        assertTrue(World.pendingAgents.isNotEmpty(), "each successful recruit queues a pending teammate")
        assertEquals(ActionItem.WAIT, agent.action.item, "resolve ends the action so the agent re-selects")
        assertEquals(null, agent.recruitTargetId, "the recruit target is cleared after resolving")
    }
}
