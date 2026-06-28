package agent.action.cond

import Factory
import World
import agent.action.ActionItem
import config.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Recruiting is the idle fallback now (an agent recruits when it has no gameplay action, like EXPLORE) — these
 * cover [Recruiter.canRecruit], the gate that keeps only [Config.maxConcurrentRecruiters] per faction recruiting
 * at once (the rest explore). The per-meeting success math is unit-tested in [agent.BalanceMathTest].
 */
class RecruiterTest {

    @BeforeTest
    @AfterTest
    fun clean() {
        World.allAgents.clear()
    }

    @Test
    fun anIdleAgentRecruitsWhenThereIsRoomAndAFreeSlot() {
        val agent = Factory.frog()
        World.allAgents.add(agent)
        assertTrue(Recruiter.canRecruit(agent), "roster has room + nobody recruiting → this idle agent recruits")
    }

    @Test
    fun theConcurrentCapStopsEveryIdleAgentRecruiting() {
        repeat(Config.maxConcurrentRecruiters) {
            World.allAgents.add(Factory.frog().also { it.action.start(ActionItem.RECRUIT) })
        }
        val idle = Factory.frog().also { World.allAgents.add(it) }
        assertFalse(Recruiter.canRecruit(idle), "at the per-faction concurrent cap → the next idle agent explores instead")
    }

    @Test
    fun anAgentAlreadyRecruitingDoesNotPickItAgain() {
        val agent = Factory.frog().also { it.action.start(ActionItem.RECRUIT) }
        World.allAgents.add(agent)
        assertFalse(Recruiter.canRecruit(agent))
    }
}
