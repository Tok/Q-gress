package agent.action.cond

import agent.Agent
import agent.Inventory
import agent.action.ActionItem
import config.Dim
import items.deployable.Resonator
import items.level.ResonatorLevel
import portal.Octant
import portal.Portal
import portal.ResonatorSlot
import util.SoundUtil
import util.Util
import kotlin.math.max
import kotlin.math.min

object Deployer : ConditionalAction {
    override fun isActionPossible(agent: Agent): Boolean {
        if (!isActionPortalFriendly(agent) || !areMoreResosAllowed(agent)) {
            return false
        }
        val inventoryResos = inventoryResos(agent.inventory)
        if (inventoryResos.isEmpty()) {
            return false
        }
        val ownedInPortal = ownedInPortal(agent)
        val results = inventoryResos.toSet().map { reso ->
            maybeDeployReso(inventoryResos, ownedInPortal, reso, true, agent)
        }
        return results.any { it }
    }

    override fun performAction(agent: Agent): Agent {
        val inventoryResos = inventoryResos(agent.inventory)
        val ownedInPortal = ownedInPortal(agent)
        val result = inventoryResos.toSet().map { reso ->
            maybeDeployReso(inventoryResos, ownedInPortal, reso, false, agent)
        }
        if (result.none { it }) {
            console.warn("Deployment failed..")
        }
        agent.action.end()
        return agent
    }

    private fun isActionPortalFriendly(agent: Agent) = !agent.actionPortal.isEnemyOf(agent)
    private fun areMoreResosAllowed(agent: Agent) = allowedResoLevels(agent).map { it.value }.sum() > 0

    private fun allowedResoLevels(agent: Agent): Map<ResonatorLevel, Int> = agent.actionPortal.findAllowedResoLevels(agent)

    private fun ownedInPortal(agent: Agent) = agent.actionPortal.resoSlots.filter { it.value.isOwnedBy(agent) }.toList()
    private fun inventoryResos(inv: Inventory) = inv.items.filter { it is Resonator }.map { it as Resonator }.sortedBy { it.level }
    private fun maxDeployable(ownedInPortal: List<Pair<Octant, ResonatorSlot>>, reso: Resonator): Int {
        val owned = ownedInPortal.filter { slot -> slot.second.resonator?.level?.level ?: 0 >= reso.level.level }.count()
        return max(reso.level.deployablePerPlayer - owned, 0)
    }

    private fun deployable(portal: Portal, reso: Resonator): List<Pair<Octant, ResonatorSlot>> = portal.resoSlots.filter { it.value.resonator?.level?.level ?: 0 < reso.level.level }.toList()
    private fun levelResos(inventoryResos: List<Resonator>, reso: Resonator, agent: Agent) = inventoryResos.filter { it.level.level == reso.level.level && it.level.level <= agent.getLevel() }
    private fun deployResos(levelResos: List<Resonator>, maxDeployable: Int) = levelResos.take(min(maxDeployable, levelResos.size - 1))

    private fun actuallyDeploy(agent: Agent, deployable: List<Pair<Octant, ResonatorSlot>>, resos: List<Resonator>) {
        val portal = agent.actionPortal
        val deployMap = Util.shuffle(deployable).zip(resos).map { it.first.first to it.second }.toMap()
        val distance = max(agent.distanceToPortal(portal), Dim.minDeploymentRange)
        portal.deploy(agent, deployMap, distance.toInt())
        SoundUtil.playDeploySound(portal.location, distance.toInt())
        agent.action.start(ActionItem.DEPLOY)
    }

    private fun maybeDeployReso(inventoryResos: List<Resonator>, ownedInPortal: List<Pair<Octant, ResonatorSlot>>,
                                reso: Resonator, isTryOnly: Boolean, agent: Agent): Boolean {
        val maxDeployable = maxDeployable(ownedInPortal, reso)
        val levelResos = levelResos(inventoryResos, reso, agent)
        if (levelResos.isNullOrEmpty()) {
            return false
        }
        val resos = deployResos(levelResos, maxDeployable)
        if (resos.isNullOrEmpty()) {
            return false
        }
        val deployable = deployable(agent.actionPortal, reso)
        if (deployable.isNullOrEmpty()) {
            return false
        }
        if (!isTryOnly) {
            actuallyDeploy(agent, deployable, resos)
        }
        return true
    }
}
