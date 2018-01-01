package items.deployable

import agent.Agent
import items.types.VirusType

data class Virus(val type: VirusType, val owner: Agent) : DeployableItem {
    override fun toString() = type.abbr
    override fun getOwnerId(): String = owner.key()
}
