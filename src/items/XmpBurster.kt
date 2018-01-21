package items

import agent.Agent
import config.Constants
import config.Dimensions
import items.deployable.DeployableItem
import items.deployable.Resonator
import items.level.XmpLevel
import util.Util
import util.data.Damage
import kotlin.math.max
import kotlin.math.min

data class XmpBurster(val level: XmpLevel, val owner: Agent) : DeployableItem {
    private fun calcBaseDamage(isCritical: Boolean) = if (isCritical) level.damage * CRIT_DAMAGE_MULTIPLIER else level.damage
    fun dealDamage(agent: Agent): List<Damage> {
        val resosInRange: List<Resonator> = agent.findResosInAttackRange(level)
        return resosInRange.map { reso ->
            val distanceToAgent: Double = reso.coords?.distanceTo(agent.pos)!!
            val fixedDist = distanceToAgent * Dimensions.pixelToMFactor
            val distanceRatio = max(0.0, min(1.0, 1.0 - (fixedDist / level.rangeM)))
            val isCloseEnough = distanceRatio < (Constants.phi - 1)
            val isCritical = isCloseEnough && Util.random() <= CRIT_RATE
            val damageValue: Int = (calcBaseDamage(isCritical) * distanceRatio * GLOBAL_DAMAGE_MULTIPLIER).toInt()
            reso.takeDamage(agent, damageValue)
            Damage(damageValue, reso.coords!!, isCritical)
        }
    }

    override fun toString() = "XMP" + level.level
    override fun getOwnerId(): String = owner.key()

    companion object {
        val GLOBAL_DAMAGE_MULTIPLIER = 0.20 //FIXME
        val CRIT_DAMAGE_MULTIPLIER = 3
        val CRIT_RATE = 0.2
        fun create(level: XmpLevel, agent: Agent) = XmpBurster(level, agent)
        fun create(level: Int, agent: Agent) = create(XmpLevel.valueOf(level), agent)
    }
}
