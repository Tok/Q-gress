package portal

import agent.Agent
import config.Probabilities
import items.deployable.DeployableItem
import util.Util


class PortalKey(val portal: Portal, val owner: Agent) : DeployableItem {
    override fun toString() = "Key-$portal"
    override fun getOwnerId(): String = owner.key()
    override fun getLevel(): Int = throw NotImplementedError("Portal Key has no level.")

    companion object {
        //TODO Probabilities
        fun tryHack(portal: Portal, agent: Agent): PortalKey? = if (Util.random() < Probabilities.keyChance) {
            PortalKey(portal, agent)
        } else {
            null
        }
    }
}
