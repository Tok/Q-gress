package items

import agent.Agent
import items.deployable.DeployableItem
import items.level.PowerCubeLevel

data class PowerCube(val level: PowerCubeLevel, val owner: Agent) : DeployableItem {
    override fun toString() = "PC" + level.level
    override fun getOwnerId(): String = owner.key()
    companion object {
        fun create(level: PowerCubeLevel, agent: Agent) = PowerCube(level, agent)
        fun create(level: Int, agent: Agent) = create(PowerCubeLevel.valueOf(level), agent)
    }
}
