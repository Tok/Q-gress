package items.deployable

import agent.Agent
import config.IngressFacts
import items.level.ResonatorLevel
import portal.Octant
import portal.Portal
import util.data.Pos
import kotlin.math.max
import kotlin.math.min

data class Resonator(
    val owner: Agent,
    val level: ResonatorLevel,
    var energy: Int,
    var portal: Portal? = null,
    var octant: Octant? = null,
    var position: Pos? = null,
) : DeployableItem {
    // TODO move location and octant to ResonatorSlot
    fun calcHealthPercent() = energy * 100 / level.energy

    fun isAtCriticalLevel() = calcHealthPercent() < CRITICAL_HEALTH_PCT
    fun totalCapacity() = level.energy
    fun openCapacity() = totalCapacity() - energy
    fun recharge(agent: Agent, xm: Int) {
        val value = min(xm, openCapacity())
        this.energy = min(energy + value, totalCapacity())
        agent.removeXm(value)
        agent.addAp(RECHARGE_AP)
    }

    private fun decayEnergy() = (level.energy * DECAY_RATIO).toInt()

    /** Lose [scale]× the base decay (1.0 = the normal cycle-end decay; a fraction = dominance erosion). */
    fun decay(scale: Double = 1.0) {
        val newEnergy = max(energy - (decayEnergy() * scale).toInt(), 0)
        this.energy = newEnergy
        if (newEnergy <= 0) {
            octant?.let { portal?.removeReso(it, null) }
        }
    }

    fun takeDamage(agent: Agent, damage: Int) {
        val newEnergy = max(energy - damage, 0)
        this.energy = newEnergy
        if (newEnergy <= 0) { // only destroyed when fully drained (was `<= newEnergy` — a bug: every hit killed it)
            agent.addAp(IngressFacts.AP_DESTROY_RESONATOR)
            octant?.let { portal?.removeReso(it, agent) }
        }
    }

    fun deploy(portal: Portal, octant: Octant, pos: Pos) {
        this.portal = portal
        this.octant = octant
        this.position = pos
    }

    override fun toString() = "R" + level.level
    override fun getOwnerId(): String = owner.key()
    override fun getLevel(): Int = level.level

    companion object {
        const val DECAY_RATIO = 0.15
        private const val CRITICAL_HEALTH_PCT = 20 // a resonator below this health% reads as "critical"
        private const val RECHARGE_AP = 10 // AP for recharging a resonator
        fun create(owner: Agent, level: ResonatorLevel) = Resonator(owner, level, level.energy)
        fun create(owner: Agent, level: Int) = create(owner, ResonatorLevel.valueOf(level))
    }
}
