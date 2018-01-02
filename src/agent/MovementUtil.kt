package agent

import World
import agent.action.Action
import agent.action.ActionItem
import config.Dimensions
import items.level.PortalLevel
import portal.Portal
import util.Util

object MovementUtil {
    fun findUncapturedPortals() = World.allPortals.filter { it.isUncaptured() }
    fun hasUncapturedPortals() = findUncapturedPortals().isNotEmpty()
    fun findEnemyPortals(agent: Agent) = World.allPortals.filter { it.isEnemyOf(agent) }
    fun hasEnemyPortals(agent: Agent) = findEnemyPortals(agent).isNotEmpty()
    fun findFriendlyPortals(agent: Agent) = World.allPortals.filter { it.isFriendlyTo(agent) }
    fun hasFriendlyPortals(agent: Agent) = findFriendlyPortals(agent).isNotEmpty()

    /* Uncaptured Portals */
    fun moveToUncapturedPortal(agent: Agent): Agent {
        check(hasUncapturedPortals())
        val uncaptured = World.allPortals.filter { it.isUncaptured() }.sortedBy { agent.distanceToPortal(it) }
        uncaptured.forEach { portal ->
            if (Util.random() < agent.skills.reliability) {
                return goToDestinationPortal(agent, portal)
            }
        }
        return agent
    }
    /* End Uncaptured Portals */

    /* Friendly Portals */
    fun moveToFriendlyHighLevelPortal(agent: Agent): Agent {
        check(hasFriendlyPortals(agent))
        val friendlyPortals = World.allPortals.filter { it.isFriendlyTo(agent) }
        val maxLevel = friendlyPortals.maxBy { it.getLevel() }?.getLevel() ?: PortalLevel.ZERO
        val selection = friendlyPortals.filter { it.getLevel() == maxLevel }
        val target = selection[(Util.random() * (selection.size - 1)).toInt()]
        return goToDestinationPortal(agent, target)
    }
    /* End Friendly Portals */

    /* Enemy Portals */
    fun moveToCloseEnemyPortal(agent: Agent): Agent {
        check(hasEnemyPortals(agent))
        val target = findEnemyPortals(agent).sortedBy { agent.distanceToPortal(it) }.first()
        return goToDestinationPortal(agent, target)
    }

    fun moveToMostLinkedEnemyPortal(agent: Agent): Agent {
        check(hasEnemyPortals(agent))
        val target = findEnemyPortals(agent).sortedBy { it.links.size }.first()
        return goToDestinationPortal(agent, target)
    }

    fun moveToMostVulnerableEnemyPortal(agent: Agent): Agent {
        check(hasEnemyPortals(agent))
        val target = findEnemyPortals(agent).sortedBy { -it.calcHealth() }.first()
        return goToDestinationPortal(agent, target)
    }
    /* End Enemy Portals */

    /* All Portals */
    fun moveToNearestPortal(agent: Agent): Agent {
        val target = World.allPortals.sortedBy { agent.distanceToPortal(it) }.first()
        return goToDestinationPortal(agent, target)
    }

    fun moveToRandomPortal(agent: Agent): Agent {
        val random: Portal = World.allPortals[(Util.random() * (World.allPortals.size - 1)).toInt()]
        return goToDestinationPortal(agent, random)
    }
    /* End All Portals */

    private fun goToDestinationPortal(agent: Agent, destination: Portal): Agent {
        val distance = agent.skills.deployPrecision * Dimensions.maxDeploymentRange
        val nextDest = destination.findRandomPointNearPortal(distance.toInt())
        return agent.copy(action = Action.start(ActionItem.MOVE, World.tick), actionPortal = destination, destination = nextDest)
    }
}
