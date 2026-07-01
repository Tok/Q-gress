package items.deployable

import agent.Agent
import items.types.MultihackType
import items.types.Rarity

/** A multi-hack portal mod: raises the hacks-before-burnout limit (see [additionalHacks] / Portal.maxHacks). */
data class Multihack(val type: MultihackType, val owner: Agent) : Mod {
    override val rarity: Rarity get() = type.rarity
    override val abbr: String get() = type.abbr
    override fun modType() = ModType.MULTIHACK
    override fun toString() = type.abbr
    override fun getOwnerId(): String = owner.key()
    override fun getLevel(): Int = type.level

    companion object {
        /** Extra hacks-before-burnout from every multi-hack deployed on a portal: the rarest counts at full
         *  effect, each additional one at half (authentic Ingress). 0 when none are deployed. */
        fun additionalHacks(mods: Collection<Mod>): Int {
            val multihacks = mods.filterIsInstance<Multihack>().sortedByDescending { it.type.additionalHacks }
            if (multihacks.isEmpty()) return 0
            val full = multihacks.first().type.additionalHacks
            val halved = multihacks.drop(1).sumOf { it.type.additionalHacks } / 2
            return full + halved
        }
    }
}
