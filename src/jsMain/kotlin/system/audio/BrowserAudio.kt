package system.audio

import agent.Faction
import agent.NonFaction
import portal.Field
import portal.Link
import system.Checkpoint
import util.data.Pos

/**
 * The live, in-browser [Audio] sink: forwards 1:1 to the Web-Audio engine ([Sound] / [BlastSound] /
 * [HackSound] / [Tts]). The engine objects already self-guard headless via `Sound.isMuted()`, but this
 * seam keeps the entity logic free of any `external.sound` / `dynamic` reference. Mirrors
 * [system.effect.BrowserEffects].
 */
object BrowserAudio : Audio {
    override fun playLevelUp(pos: Pos) = Sound.playLevelUp(pos)
    override fun playPortalCreationSound(pos: Pos) = Sound.playPortalCreationSound(pos)
    override fun playLinkingSound(link: Link) = Sound.playLinkingSound(link)
    override fun playFieldingSound(field: Field) = Sound.playFieldingSound(field)
    override fun playShieldRemoveSound(pos: Pos, level: Int) = Sound.playShieldRemoveSound(pos, level)
    override fun playResoDeploySound(pos: Pos, level: Int) = Sound.playResoDeploySound(pos, level)
    override fun playModDeploySound(pos: Pos, level: Int) = Sound.playModDeploySound(pos, level)
    override fun playShieldDeploySound(pos: Pos, level: Int) = Sound.playShieldDeploySound(pos, level)
    override fun playVirusSound(pos: Pos, faction: Faction) = Sound.playVirusSound(pos, faction)
    override fun playRecruitSuccess(pos: Pos) = Sound.playRecruitSuccess(pos)
    override fun playOffScreenLocationCreationSound() = Sound.playOffScreenLocationCreationSound()
    override fun playNpcCreationSound(npc: NonFaction) = Sound.playNpcCreationSound(npc)
    override fun playKnockOutSound(pos: Pos) = BlastSound.playKnockOutSound(pos)
    override fun playThunderSound(pan: Double, decayMult: Double) = BlastSound.playThunderSound(pan, decayMult)
    override fun playGlassShatterSound(pos: Pos, heaviness: Double, amplitude: Double) =
        BlastSound.playGlassShatterSound(pos, heaviness, amplitude)

    override fun playCheckpointSound(checkpoint: Checkpoint) = Sound.playCheckpointSound(checkpoint)
    override fun playCycleSound() = Sound.playCycleSound()
    override fun hack(id: String, pos: Pos, dur: Double, faction: Faction, slots: IntArray) = HackSound.hack(id, pos, dur, faction, slots)

    override fun glyph(id: String, pos: Pos, level: Int, dur: Double, faction: Faction, slots: IntArray) =
        HackSound.glyph(id, pos, level, dur, faction, slots)

    override fun announceHugeField(owner: Faction, mu: Int) = Tts.announceHugeField(owner, mu)
    override fun announcePortalDiscovery(name: String) = Tts.announcePortalDiscovery(name)
    override fun announceRecruitment(faction: Faction) = Tts.announceRecruitment(faction)
    override fun announceGlyphHack(faction: Faction) = Tts.announceGlyphHack(faction)
    override fun announceCheckpointLead(leader: Faction, leadMu: Int) = Tts.announceCheckpointLead(leader, leadMu)
}
