package items

import agent.Agent
import items.deployable.DeployableItem
import items.level.PowerCubeLevel

data class PowerCube(val owner: Agent, val level: PowerCubeLevel) : DeployableItem {
    override fun toString() = "PC" + level.level
    override fun getOwnerId(): String = owner.key()
    override fun getLevel(): Int = level.level

    companion object {
        fun create(owner: Agent, level: PowerCubeLevel) = PowerCube(owner, level)
        fun create(owner: Agent, level: Int) = create(owner, PowerCubeLevel.valueOf(level))
    }
}
