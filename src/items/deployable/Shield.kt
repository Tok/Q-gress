package items.deployable

import agent.Agent
import items.types.ShieldType

data class Shield(val type: ShieldType, val owner: Agent) : DeployableItem {
    override fun toString() = type.abbr
    override fun getOwnerId(): String = owner.key()
}
