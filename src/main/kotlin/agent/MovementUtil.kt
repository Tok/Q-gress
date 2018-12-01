package agent

import World
import agent.action.ActionItem
import config.Dim
import items.level.PortalLevel
import portal.Portal
import util.Util
import util.data.Complex

object MovementUtil {
    fun findUncapturedPortals() = World.allPortals.filter { it.isUncaptured() }
    fun hasUncapturedPortals() = findUncapturedPortals().isNotEmpty()
    fun findEnemyPortals(agent: Agent): List<Portal> = World.allPortals.filter { it.isEnemyOf(agent) }
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
    fun attackClosePortal(a: Agent) = goAttack(a, findEnemyPortals(a).sortedBy { a.distanceToPortal(it) }.firstOrNull())

    fun attackMostLinkedPortal(a: Agent) = goAttack(a, findEnemyPortals(a).sortedBy { it.links.size }.firstOrNull())
    fun attackMostVulnerablePortal(a: Agent) = goAttack(a, findEnemyPortals(a).sortedBy { -it.calcHealth() }.firstOrNull())
    private fun goAttack(agent: Agent, target: Portal?): Agent {
        return if (target != null) {
            goToDestinationPortal(agent, target)
        } else {
            agent.action.end()
            agent
        }
    }
    /* End Enemy Portals */

    /* All Portals */
    fun moveToNearestPortal(agent: Agent): Agent {
        val target = World.allPortals.sortedBy { agent.distanceToPortal(it) }.first()
        return goToDestinationPortal(agent, target)
    }

    fun moveToRandomPortal(agent: Agent): Agent {
        return goToDestinationPortal(agent, World.randomPortal())
    }
    /* End All Portals */

    private fun goToDestinationPortal(agent: Agent, destination: Portal): Agent {
        val distance = agent.skills.deployPrecision * Dim.maxDeploymentRange
        val nextDest = destination.findRandomPointNearPortal(distance.toInt())
        agent.action.start(ActionItem.MOVE)
        return agent.copy(actionPortal = destination, destination = nextDest)
    }

    fun move(velocity: Complex, force: Complex, limit: Double): Complex {
        //if (isDrunk && Util.randomBool()) { return Complex.random() } //TODO
        val actualForce = if (force != Complex.ZERO) force else Complex.random()
        val newVelo = velocity + actualForce
        return if (newVelo.mag <= limit) {
            newVelo
        } else {
            newVelo.copyWithNewMagnitude(limit)
        }
    }
}
