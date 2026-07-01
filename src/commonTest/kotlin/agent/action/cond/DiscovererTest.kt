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
 * Discovery is the other idle fallback (the sibling of [Recruiter]): an idle agent strolls to open ground and, on
 * arrival, rolls a density-driven portal create/remove. These cover [Discoverer.canDiscover], the gate that keeps
 * only [Config.maxConcurrentDiscoverers] per faction exploring at once (the rest go seek work). The density-churn
 * math itself is unit-tested in [system.ChurnMathTest].
 */
class DiscovererTest {

    @BeforeTest
    @AfterTest
    fun clean() {
        World.allAgents.clear()
    }

    @Test
    fun anIdleAgentDiscoversWhenAFreeSlotIsOpen() {
        val agent = Factory.frog()
        World.allAgents.add(agent)
        assertTrue(Discoverer.canDiscover(agent), "nobody discovering → this idle agent can discover")
    }

    @Test
    fun theConcurrentCapStopsEveryIdleAgentDiscovering() {
        repeat(Config.maxConcurrentDiscoverers) {
            World.allAgents.add(Factory.frog().also { it.action.start(ActionItem.EXPLORE) })
        }
        val idle = Factory.frog().also { World.allAgents.add(it) }
        assertFalse(Discoverer.canDiscover(idle), "at the per-faction concurrent cap → the next idle agent seeks work instead")
    }

    @Test
    fun theCapIsPerFactionNotGlobal() {
        repeat(Config.maxConcurrentDiscoverers) {
            World.allAgents.add(Factory.smurf().also { it.action.start(ActionItem.EXPLORE) })
        }
        val frog = Factory.frog().also { World.allAgents.add(it) }
        assertTrue(Discoverer.canDiscover(frog), "the cap counts only the agent's own faction → a frog still discovers")
    }

    @Test
    fun anAgentAlreadyDiscoveringDoesNotPickItAgain() {
        val agent = Factory.frog().also { it.action.start(ActionItem.EXPLORE) }
        World.allAgents.add(agent)
        assertFalse(Discoverer.canDiscover(agent))
    }
}
