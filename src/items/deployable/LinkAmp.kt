package items.deployable

import agent.Agent
import items.types.LinkAmpType
import portal.ModSlot
import portal.Portal

data class LinkAmp(val type: LinkAmpType, val slot: ModSlot?, val owner: Agent) : DeployableItem {
    fun isDeployed() = slot != null
    fun deploy(portal: Portal) {
        console.info("Deploying $this to portal $portal")
    }

    override fun toString() = type.abbr
    override fun getOwnerId() = owner.key()
    override fun getLevel(): Int = -1 //TODO

    companion object {
        fun calculateImprovedRange(allModsInPortal: List<DeployableItem>, range: Double): Double {
            val linkamps = allModsInPortal.filter { it is LinkAmp }
            return when (linkamps.count()) {
                1 -> range * 2
                2 -> range * 2.5
                3 -> range * 2.75
                4 -> range * 5
                else -> range
            }
        }
    }
}
