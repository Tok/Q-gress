package agent.action.cond

import Factory
import agent.Faction
import config.Dim
import items.level.ResonatorLevel
import portal.Octant
import kotlin.test.*

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
            assertEquals(8, portal.filledSlots().count())
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
            assertEquals(8, portal.filledSlots().count())
            assertEquals(7, portal.filledSlots().count { it.owner == agent })
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
            assertNotEquals(agent, portal.owner)
        }
    }

    @Test
    fun resoLevelDeployment() = with(Factory) {
        Faction.values().forEach { faction ->
            val agent = agent(faction)
            ResonatorLevel.values().forEach { level ->
                val portal = portal()
                Octant.values().forEach { octant ->
                    val resos = mapOf(octant to resonator(agent, level.level))
                    try {
                        portal.deploy(agent, resos, Dim.maxDeploymentRange.toInt())
                    } catch (e: IllegalStateException) {
                        //Portal can be fully deployed with level 1 resonators.
                        assertFalse { level.level == 1 }
                        //ISE should only be thrown when portal was fully
                        //deployed with resonators of the same level.
                        val filled = portal.filledSlots()
                        assertEquals(level.deployablePerPlayer, filled.count())
                    }
                    val filled = portal.filledSlots()
                    assertTrue(filled.count() <= level.deployablePerPlayer)
                }
            }
        }
    }
}
