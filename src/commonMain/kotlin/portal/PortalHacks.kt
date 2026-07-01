package portal

import World
import agent.Agent
import items.QgressItem
import items.deployable.Multihack
import system.audio.Snd
import system.effect.Fx
import util.Rng
import kotlin.math.min

/**
 * The hack / glyph-hack subsystem of [Portal], split out of the data class: drop rolls, the XM/AP cost, the
 * per-agent hack history and the burnout + time cooldown. [Portal] keeps thin `canHack`/`tryHack`/`tryGlyph`
 * delegates (so callers are unchanged) and owns the per-portal `lastHacks` state this reads + mutates.
 */
internal object PortalHacks {
    fun canHack(portal: Portal, hacker: Agent): Boolean = handleCooldown(portal, hacker, true) == Cooldown.NONE

    fun tryHack(portal: Portal, hacker: Agent): HackResult {
        val cooldown = handleCooldown(portal, hacker, false)
        if (cooldown == Cooldown.NONE) {
            val stuff = hack(portal, hacker)
            return HackResult(stuff, null)
        }
        return HackResult(null, cooldown)
    }

    fun tryGlyph(portal: Portal, glypher: Agent): HackResult {
        val normal = tryHack(portal, glypher)
        if (normal.cooldown == null) {
            val glyphItems = mutableListOf<QgressItem>()
            glyphItems.addAll(normal.items ?: emptyList())
            glyphItems.addAll(hack(portal, glypher))
            if (Rng.random() < glypher.skills.glyphSkill) {
                glyphItems.addAll(hack(portal, glypher))
            }
            Snd.sink.announceGlyphHack(glypher.faction) // reads 1–3 glyphs (GLYPH verbosity only; no-op headless)
            return HackResult(glyphItems.toList(), null)
        }
        return HackResult(null, normal.cooldown)
    }

    private fun hack(portal: Portal, hacker: Agent): MutableList<QgressItem> {
        val level = min(portal.calculateLevel(), hacker.getLevel())
        val newStuff = HackLoot.rollDrops(hacker, level).toMutableList()
        PortalKey.tryHack(portal, hacker)?.let { newStuff.add(it) }
        chargeHackCost(portal, hacker)
        return newStuff
    }

    // The XM cost of a hack (and the AP for hacking an enemy's), scaled by portal level — the portal-specific
    // half of a hack the drop table ([HackLoot]) can't own.
    private fun chargeHackCost(portal: Portal, hacker: Agent) {
        val isEnemyPortal = portal.owner != null && hacker.faction != portal.owner?.faction
        if (isEnemyPortal) {
            hacker.addAp(100)
            hacker.removeXm(300 * portal.calculateLevel())
        } else {
            hacker.removeXm(50 * portal.calculateLevel())
        }
    }

    /** Hacks allowed before burnout: the base [Portal.MAX_HACKS] plus any deployed multi-hacks' bonus. */
    private fun maxHacks(portal: Portal): Int = Portal.MAX_HACKS + Multihack.additionalHacks(portal.mods.values)

    private fun handleCooldown(portal: Portal, hacker: Agent, readOnly: Boolean): Cooldown {
        // Per-agent hack history (tick numbers). Burnout = maxHacks() hacks all still within the burnout window
        // (PortalMath.isBurnedOut); the time-cooldown between hacks is keyed off the most recent. The list is
        // kept to the last maxHacks() entries so it's bounded and burnout can recur once old hacks age out.
        val hacks = portal.lastHacks.getOrPut(hacker.key()) { mutableListOf() }
        if (hacks.size >= maxHacks(portal) && PortalMath.isBurnedOut(hacks, World.tick)) return Cooldown.BURNOUT
        val baseCooldownS = (Cooldown.FIVE.seconds * portal.cooldownFactor()).toInt() // heat sinks shorten it
        val cooldown = if (hacks.isEmpty()) Cooldown.NONE else PortalMath.cooldownAfter(World.tick - hacks.max(), baseCooldownS)
        if (cooldown == Cooldown.NONE && !readOnly) {
            hacks.add(World.tick)
            while (hacks.size > maxHacks(portal)) hacks.removeAt(0)
            // This hack just tipped the portal into burnout for this agent → vent a one-shot steam puff.
            if (hacks.size >= maxHacks(portal) && PortalMath.isBurnedOut(hacks, World.tick)) {
                Fx.sink.steamPuff(portal.location, portal.getLevel().toInt())
            }
        }
        return cooldown
    }
}
