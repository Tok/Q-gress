package items.deployable

import agent.Agent
import items.types.MultihackType
import portal.ModSlot
import portal.Portal

data class Multihack(val type: MultihackType, val slot: ModSlot?, val owner: Agent) : DeployableItem {
    fun isDeployed() = slot != null
    fun deploy(portal: Portal) {
        console.info("Deploying $this to portal $portal")
    }

    override fun toString() = type.abbr
    override fun getOwnerId(): String = owner.key()
    override fun getLevel(): Int = -1 //TODO

    companion object {
        fun calculateImprovedBurnout(allModsInPortal: List<DeployableItem>): Double {
            val multihacks = allModsInPortal.filter { it is Multihack }.map { it as Multihack }.sortedBy { it.type.order }
            val first = multihacks.first().type.additionalHacks
            val second = multihacks[1].type.additionalHacks * 0.5
            val third = multihacks[2].type.additionalHacks * 0.5
            val fourth = multihacks[3].type.additionalHacks * 0.5
            return first + second + third + fourth
        }
    }
}
