package system.audio

import agent.Faction
import agent.NonFaction
import portal.Field
import portal.Link
import system.Checkpoint
import util.data.Pos

/**
 * The imperative-shell boundary for the **audio** side effects that game logic fires inline (level-ups,
 * link/field/deploy SFX, detonation blasts, recruit/discovery TTS, …). Logic calls `Snd.sink.<op>(...)`;
 * [BrowserAudio] forwards 1:1 to the Web-Audio engine ([Sound] / [BlastSound] / [HackSound] / [Tts]),
 * [NoOpAudio] does nothing (headless: Node tests / the `SimRunner`). No `external.sound` / `dynamic` /
 * `kotlinx.browser` types cross this seam, so the whole tick loop runs headless without touching Web Audio.
 *
 * Mirrors the visual [system.effect.Effects] seam; the PLAN Phase-B prerequisite for lifting
 * `Portal`/`Agent`/`NonFaction` into `commonMain` (they no longer name the Web-Audio-bound `Sound` object
 * directly). The few entity-typed params ([Link]/[Field]/[NonFaction]) ride the same migration batch as
 * those types, exactly as [system.effect.Effects] references [portal.Portal].
 */
interface Audio {
    /** A player agent levelled up (a rising ding). */
    fun playLevelUp(pos: Pos)

    /** A portal was created/discovered. */
    fun playPortalCreationSound(pos: Pos)

    /** A new link snaps into place (pitch ramp set by the link's length + endpoint levels). */
    fun playLinkingSound(link: Link)

    /** A control field powers up (a swelling triad whose shape/register come from the field's geometry). */
    fun playFieldingSound(field: Field)

    /** A shield mod was stripped off a portal. */
    fun playShieldRemoveSound(pos: Pos, level: Int)

    /** A resonator rod deployed into a portal. */
    fun playResoDeploySound(pos: Pos, level: Int)

    /** A (non-shield) mod deployed onto a portal. */
    fun playModDeploySound(pos: Pos, level: Int)

    /** A shield mod deployed onto a portal. */
    fun playShieldDeploySound(pos: Pos, level: Int)

    /** A virus (ADA/JARVIS) flips a portal — a glitch sweep pitched to the new colour. */
    fun playVirusSound(pos: Pos, faction: Faction)

    /** An NPC was recruited (success chime). */
    fun playRecruitSuccess(pos: Pos)

    /** An off-screen location was created (faint, distant). */
    fun playOffScreenLocationCreationSound()

    /** An NPC spawned (pitch set by the NPC's size). */
    fun playNpcCreationSound(npc: NonFaction)

    /** A portal knocked out (resonator destroyed). */
    fun playKnockOutSound(pos: Pos)

    /** Portal-defense retaliation thunder, panned by the target's screen position. */
    fun playThunderSound(pan: Double, decayMult: Double = 1.0)

    /** A portal shatters (glass break) when fully destroyed. */
    fun playGlassShatterSound(pos: Pos, heaviness: Double, amplitude: Double)

    /** A checkpoint tick. */
    fun playCheckpointSound(checkpoint: Checkpoint)

    /** A full cycle completed. */
    fun playCycleSound()

    /** The portal collar hack spin (records the per-resonator slot levels for the visual collar's audio). */
    fun hack(id: String, pos: Pos, dur: Double, faction: Faction, slots: IntArray)

    /** The stronger glyph-hack spin. */
    fun glyph(id: String, pos: Pos, level: Int, dur: Double, faction: Faction, slots: IntArray)

    /** TTS: a huge field was created. */
    fun announceHugeField(owner: Faction, mu: Int)

    /** TTS: a portal was discovered. */
    fun announcePortalDiscovery(name: String)

    /** TTS: an NPC was recruited. */
    fun announceRecruitment(faction: Faction)

    /** TTS: a glyph hack (reads the glyph sequence back). */
    fun announceGlyphHack(faction: Faction)

    /** TTS: the checkpoint lead. */
    fun announceCheckpointLead(leader: Faction, leadMu: Int)
}
