package system.effect

import agent.Faction
import items.deployable.Mod
import portal.Octant
import portal.Portal
import util.data.Pos

/**
 * The headless [Effects] sink: every op is a no-op. The default outside the browser (Node tests / the
 * future `SimRunner`), so game logic can fire visual effects freely without touching the renderer.
 * Imports nothing from `system/display/`.
 */
object NoOpEffects : Effects {
    override fun playXmpBurst(location: Pos, level: Int, sound: Boolean) = Unit
    override fun showDamageNumber(portal: Portal, amount: Int) = Unit
    override fun recordHack(id: String, faction: Faction, glyph: Boolean, durationS: Double) = Unit
    override fun rewardFx(portalLocation: Pos, level: Int, to: Pos, count: Int) = Unit
    override fun recordDeploy(id: String, octant: Octant, from: Pos) = Unit
    override fun collectXmFx(from: Pos, to: Pos) = Unit
    override fun fireBolt(from: Pos, fromLevel: Int, to: Pos, color: String) = Unit
    override fun dropMods(location: Pos, level: Int, mods: List<Mod>) = Unit
    override fun shatterPortal(location: Pos, color: String, level: Int, resos: Map<Octant, Int>) = Unit
    override fun dropResonator(location: Pos, level: Int, octantIndex: Int, resoLevel: Int) = Unit
    override fun flashVectorField(portalId: String) = Unit
}
