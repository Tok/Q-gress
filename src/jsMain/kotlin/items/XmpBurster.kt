package items

import World
import agent.Agent
import items.deployable.DeployableItem
import items.level.XmpLevel
import portal.ModSlot
import portal.Portal
import system.effect.Fx
import util.Util
import util.data.Pos

data class XmpBurster(val owner: Agent, val level: XmpLevel) : DeployableItem {

    /** Apply this burst's damage to every resonator in range (quintile falloff + mitigation; see [Combat]).
     *  Returns the total XM dealt (so the caller can pop one aggregate 3D damage number). */
    fun dealDamage(agent: Agent): Int {
        var total = 0
        agent.findResosInAttackRange(level).forEach { reso ->
            val position = requireNotNull(reso.position) { "resonator in attack range without a position" }
            val distFrac = Combat.distanceFraction(position.distanceTo(agent.pos), level.rangeM, ultra = false)
            val crit = distFrac < 0.2 && Util.random() < Combat.CRIT_RATE
            val mitigation = reso.portal?.totalMitigation() ?: 0 // shields + links reduce incoming damage
            val dmg = Combat.resoDamage(level.damage, distFrac, mitigation, ultra = false, crit = crit)
            reso.takeDamage(agent, dmg)
            total += dmg
        }
        return total
    }

    override fun toString() = "XMP" + level.level
    override fun getOwnerId(): String = owner.key()
    override fun getLevel(): Int = level.level

    companion object {
        /**
         * One attack's **mod knock-out** roll against [portal] from a blast at [blastPos]: each slotted
         * mod is independently knocked out with [Combat.knockChance] (proximity + stickiness + weapon).
         * Ultra-Strikes ([ultra]) are far better at it than Bursters. [rng] is injectable for tests.
         * Returns how many mods were knocked off.
         */
        fun knockMods(
            portal: Portal,
            blastPos: Pos,
            level: XmpLevel,
            ultra: Boolean,
            agent: Agent?,
            rng: () -> Double = Util::random,
        ): Int {
            val distFrac = Combat.distanceFraction(portal.location.distanceTo(blastPos), level.rangeM, ultra)
            if (distFrac >= 1.0) return 0
            var knocked = 0
            ModSlot.values().forEach { slot ->
                val mod = portal.mods[slot] ?: return@forEach
                if (rng() < Combat.knockChance(mod.stickiness, distFrac, ultra) && portal.stripMod(slot, agent) != null) {
                    knocked++
                }
            }
            return knocked
        }

        /**
         * A faction-agnostic blast at scene-point [pos] (the title mini-game): damages resonators in
         * range AND rolls mod knock-out on every portal in range — so an XMP shatters portals and an
         * [ultra]-strike strips their shields/mods, just like in-game. [attacker] credits the AP.
         */
        fun blastAt(pos: Pos, level: XmpLevel, attacker: Agent, ultra: Boolean = false) {
            World.allPortals.forEach { portal ->
                var portalDamage = 0
                portal.slots.values.forEach { slot ->
                    val reso = slot.resonator ?: return@forEach
                    val rp = reso.position ?: return@forEach
                    val distFrac = Combat.distanceFraction(rp.distanceTo(pos), level.rangeM, ultra)
                    if (distFrac >= 1.0) return@forEach
                    val crit = distFrac < 0.2 && Util.random() < Combat.CRIT_RATE
                    val dmg = Combat.resoDamage(level.damage, distFrac, portal.totalMitigation(), ultra, crit)
                    reso.takeDamage(attacker, dmg)
                    portalDamage += dmg
                }
                if (portalDamage > 0) Fx.sink.showDamageNumber(portal, portalDamage)
                knockMods(portal, pos, level, ultra, attacker)
            }
        }

        fun create(owner: Agent, level: XmpLevel) = XmpBurster(owner, level)
        fun create(owner: Agent, level: Int) = create(owner, XmpLevel.valueOf(level))
    }
}
