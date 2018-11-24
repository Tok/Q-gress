package items.deployable

import agent.Agent
import items.level.ResonatorLevel
import portal.Octant
import portal.Portal
import util.data.Coords
import kotlin.math.max

data class Resonator(val owner: Agent, val level: ResonatorLevel, var energy: Int,
                     var portal: Portal? = null, var octant: Octant? = null, var coords: Coords? = null) : DeployableItem {
    //TODO move location and octant to ResonatorSlot
    fun calcHealthPercent() = energy * 100 / level.energy

    fun isAtCriticalLevel() = calcHealthPercent() < 20
    fun recharge(agent: Agent, xm: Int) {
        val capacity = level.energy - energy
        if (capacity >= xm) {
            this.energy += xm
            agent.removeXm(xm)
        } else {
            val diff = xm - capacity
            this.energy += diff
            agent.removeXm(diff)
        }
        agent.addAp(10)
    }

    private fun decayEnergy() = (level.energy * DECAY_RATIO).toInt()

    fun decay() {
        this.energy = max(0, energy - decayEnergy())
        if (energy <= 0) {
            portal?.removeReso(octant!!, null)
        }
    }

    private fun takeDamage(agent: Agent, damage: Int) {
        this.energy = energy - damage
        if (energy <= 0) {
            agent.ap = agent.ap + 75
            portal?.removeReso(octant!!, agent)
        }
    }

    fun deploy(portal: Portal, octant: Octant, coords: Coords) {
        this.portal = portal
        this.octant = octant
        this.coords = coords
    }

    override fun toString() = "R" + level.level
    override fun getOwnerId(): String = owner.key()
    override fun getLevel(): Int = level.level

    companion object {
        const val DECAY_RATIO = 0.15
        fun create(owner: Agent, level: ResonatorLevel) = Resonator(owner, level, level.energy)
        fun create(owner: Agent, level: Int) = create(owner, ResonatorLevel.valueOf(level))
    }
}
