package agent.action.cond

import Factory
import agent.Faction
import config.Dim
import portal.Octant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeployerTest {

    @Test
    fun deployEmpty() = with(Factory) {
        Faction.values().forEach { faction ->
            val agent = agent(faction)
            val portal = portal()
            Octant.values().forEach {
                val resos = mapOf(it to resonator(agent))
                portal.deploy(agent, resos, Dim.maxDeploymentRange.toInt())
            }
            assertEquals(agent, portal.owner)
            assertEquals(8, portal.slots.filterNot { it.value.isEmpty() }.count())
        }
    }

    @Test
    fun deployFriendly() = with(Factory) {
        Faction.values().forEach { faction ->
            val agent = agent(faction)
            val portal = portal(agent.faction)
            val predeployed = Octant.N
            Octant.values().filterNot { it == predeployed }.forEach {
                val resos = mapOf(it to resonator(agent))
                portal.deploy(agent, resos, Dim.maxDeploymentRange.toInt())
            }
            assertEquals(8, portal.slots.filterNot { it.value.isEmpty() }.count())
        }
    }

    @Test
    fun noEnemyPortalDeployment() = with(Factory) {
        Faction.values().forEach { faction ->
            val agent = agent(faction)
            val portal = portal(agent.faction.enemy())
            val predeployed = Octant.N
            Octant.values().filterNot { it == predeployed }.forEach {
                val resos = mapOf(it to resonator(agent))
                assertFailsWith(IllegalStateException::class) {
                    portal.deploy(agent, resos, Dim.maxDeploymentRange.toInt())
                }
            }
        }
    }
}
