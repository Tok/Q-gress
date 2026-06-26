package system.effect

import agent.Faction
import items.deployable.Mod
import portal.Octant
import portal.Portal
import system.display.DeployFx
import system.display.HackFx
import system.display.Scene3D
import system.display.VectorFieldOverlay
import util.data.Pos

/**
 * The browser [Effects] sink: forwards each op 1:1 to the three.js renderer (`system/display/`), so live
 * rendering is byte-for-byte identical to the pre-seam inline calls. The default in the browser.
 */
object BrowserEffects : Effects {
    override fun playXmpBurst(location: Pos, level: Int, sound: Boolean) = Scene3D.playXmpBurst(location, level, sound)
    override fun showDamageNumber(portal: Portal, amount: Int) = Scene3D.showDamageNumber(portal, amount)
    override fun recordHack(id: String, faction: Faction, glyph: Boolean, durationS: Double) = HackFx.record(id, faction, glyph, durationS)
    override fun rewardFx(portalLocation: Pos, level: Int, to: Pos, colors: List<String>) =
        Scene3D.rewardFx(portalLocation, level, to, colors)
    override fun recordDeploy(id: String, octant: Octant, from: Pos) = DeployFx.record(id, octant, from)
    override fun collectXmFx(from: Pos, to: Pos) = Scene3D.collectXmFx(from, to)
    override fun fireBolt(from: Pos, fromLevel: Int, to: Pos, color: String) = Scene3D.fireBolt(from, fromLevel, to, color)
    override fun dropMods(location: Pos, level: Int, mods: List<Mod>) = Scene3D.dropMods(location, level, mods)
    override fun shatterPortal(location: Pos, color: String, level: Int, resos: Map<Octant, Int>) =
        Scene3D.shatterPortal(location, color, level, resos)

    override fun dropResonator(location: Pos, level: Int, octantIndex: Int, resoLevel: Int) =
        Scene3D.dropResonator(location, level, octantIndex, resoLevel)

    override fun flashVectorField(portalId: String) = VectorFieldOverlay.flash(portalId)
}
