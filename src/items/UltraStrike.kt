package items

import agent.Agent
import items.deployable.DeployableItem
import items.level.UltraStrikeLevel

data class UltraStrike(val level: UltraStrikeLevel, val owner: Agent) : DeployableItem {
    override fun toString() = "US" + level.level
    override fun getOwnerId(): String = owner.key()
}
