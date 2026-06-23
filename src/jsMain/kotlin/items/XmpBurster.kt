package items

import World
import agent.Agent
import config.Constants
import config.Dim
import items.deployable.DeployableItem
import items.level.XmpLevel
import util.Util
import util.data.Pos
import kotlin.math.max
import kotlin.math.min

data class XmpBurster(val owner: Agent, val level: XmpLevel) : DeployableItem {
    private fun calcBaseDamage(isCritical: Boolean) = if (isCritical) level.damage * CRIT_DAMAGE_MULTIPLIER else level.damage

    /** Apply this burst's damage to every resonator in range (distance- and crit-scaled). */
    fun dealDamage(agent: Agent) {
        agent.findResosInAttackRange(level).forEach { reso ->
            val position = requireNotNull(reso.position) { "resonator in attack range without a position" }
            val fixedDist = position.distanceTo(agent.pos) * Dim.pixelToMFactor
            val distanceRatio = max(0.0, min(1.0, 1.0 - (fixedDist / level.rangeM)))
            val isCloseEnough = distanceRatio < (Constants.phi - 1)
            val isCritical = isCloseEnough && Util.random() <= CRIT_RATE
            val raw: Int = (calcBaseDamage(isCritical) * distanceRatio * GLOBAL_DAMAGE_MULTIPLIER).toInt()
            val mitigation = reso.portal?.totalMitigation() ?: 0 // shields + links reduce incoming damage
            reso.takeDamage(agent, raw * (100 - mitigation) / 100)
        }
    }

    override fun toString() = "XMP" + level.level
    override fun getOwnerId(): String = owner.key()
    override fun getLevel(): Int = level.level

    companion object {
        /**
         * A faction-agnostic blast at scene-point [pos] (the title mini-game): damages every resonator
         * in range — destroying them, so portals shatter and drop their mods/shields just like an
         * agent's XMP. [attacker] credits the damage (any agent; only matters for AP). [level] sizes it.
         */
        fun blastAt(pos: Pos, level: XmpLevel, attacker: Agent) {
            val range = level.rangeM * 0.5
            World.allPortals.flatMap { it.slots.values }.forEach { slot ->
                val reso = slot.resonator ?: return@forEach
                val rp = reso.position ?: return@forEach
                if (rp.distanceTo(pos) > range) return@forEach
                val ratio = max(0.0, min(1.0, 1.0 - rp.distanceTo(pos) * Dim.pixelToMFactor / level.rangeM))
                val raw = (level.damage * ratio * GLOBAL_DAMAGE_MULTIPLIER).toInt()
                val mitigation = reso.portal?.totalMitigation() ?: 0
                reso.takeDamage(attacker, raw * (100 - mitigation) / 100)
            }
        }

        const val GLOBAL_DAMAGE_MULTIPLIER = 0.20 // FIXME
        const val CRIT_DAMAGE_MULTIPLIER = 3
        const val CRIT_RATE = 0.2
        fun create(owner: Agent, level: XmpLevel) = XmpBurster(owner, level)
        fun create(owner: Agent, level: Int) = create(owner, XmpLevel.valueOf(level))
    }
}
