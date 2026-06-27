package system.effect

import agent.Faction
import items.RewardMote
import items.deployable.Mod
import portal.Octant
import portal.Portal
import util.data.Pos

/**
 * The imperative-shell boundary for the crash-prone **visual** side effects that game logic fires inline
 * (XMP bursts, hack/deploy animations, reward motes, retaliation bolts, portal shatter, …). Logic calls
 * `Fx.sink.<op>(...)`; [BrowserEffects] forwards 1:1 to the three.js renderer, [NoOpEffects] does nothing
 * (headless: Node tests / the future `SimRunner`). No three.js / DOM types cross this seam, so the whole
 * tick loop runs headless without touching the renderer.
 *
 * Audio is **not** here — [util.Sound] already self-guards headless; the message log [system.Com] is
 * pure. This seam covers only the renderer (`system/display/`) calls. Mirrors [ai.FactionPolicies].
 */
interface Effects {
    /** XMP detonation burst at [location] (records the blast origin so destroyed parts fly away from it). */
    fun playXmpBurst(location: Pos, level: Int, sound: Boolean = true)

    /** Floating damage number over [portal]. */
    fun showDamageNumber(portal: Portal, amount: Int)

    /** The portal collar hack/glyph spin animation (ENL cw / RES ccw; [glyph] = the stronger glyph spin). */
    fun recordHack(id: String, faction: Faction, glyph: Boolean, durationS: Double)

    /** Reward drops flying from a hacked portal to the agent — one mote per [motes] entry (cube, or sphere for viruses). */
    fun rewardFx(portalLocation: Pos, level: Int, to: Pos, motes: List<RewardMote>)

    /** A one-shot white-steam puff vented from a portal's flask top when an agent burns it out (over-hacks it). */
    fun steamPuff(portalLocation: Pos, level: Int)

    /** A deployed resonator rod flying into [octant] from the agent at [from]. */
    fun recordDeploy(id: String, octant: Octant, from: Pos)

    /** A collected XM mote flying from [from] to the agent at [to]. */
    fun collectXmFx(from: Pos, to: Pos)

    /** A portal-defense retaliation bolt from the portal to the agent at [to]. */
    fun fireBolt(from: Pos, fromLevel: Int, to: Pos, color: String)

    /** Mods tumbling out as a portal goes down. */
    fun dropMods(location: Pos, level: Int, mods: List<Mod>)

    /** Glass shards + resonators falling when a portal is destroyed. */
    fun shatterPortal(location: Pos, color: String, level: Int, resos: Map<Octant, Int>)

    /** A single destroyed resonator rod falling out of its octant. */
    fun dropResonator(location: Pos, level: Int, octantIndex: Int, resoLevel: Int)

    /** The one-shot flow-field flash for a portal once its vector field finishes computing. */
    fun flashVectorField(portalId: String)

    /**
     * A virus flip (ADA / JARVIS): the orb re-skins to the new faction colour WITHOUT the capture
     * shatter — the portal changes hands, it isn't destroyed and rebuilt. Suppresses the next colour-
     * change shatter for [portalId] (see [system.display.CaptureFx]).
     */
    fun refactorPortal(portalId: String)
}
