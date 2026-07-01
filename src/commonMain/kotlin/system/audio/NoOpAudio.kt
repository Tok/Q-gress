package system.audio

import agent.Faction
import agent.NonFaction
import portal.Field
import portal.Link
import system.Checkpoint
import util.data.Pos

/**
 * The headless [Audio] sink: every op is a no-op. The default outside the browser (Node tests / the
 * `SimRunner`), so game logic can fire audio freely without touching Web Audio. Imports nothing from the
 * Web-Audio engine (`external.sound` / `dynamic` / `kotlinx.browser`).
 */
object NoOpAudio : Audio {
    override fun playLevelUp(pos: Pos) = Unit
    override fun playPortalCreationSound(pos: Pos) = Unit
    override fun playLinkingSound(link: Link) = Unit
    override fun playFieldingSound(field: Field) = Unit
    override fun playShieldRemoveSound(pos: Pos, level: Int) = Unit
    override fun playResoDeploySound(pos: Pos, level: Int) = Unit
    override fun playModDeploySound(pos: Pos, level: Int) = Unit
    override fun playShieldDeploySound(pos: Pos, level: Int) = Unit
    override fun playVirusSound(pos: Pos, faction: Faction) = Unit
    override fun playRecruitSuccess(pos: Pos) = Unit
    override fun playOffScreenLocationCreationSound() = Unit
    override fun playNpcCreationSound(npc: NonFaction) = Unit
    override fun playKnockOutSound(pos: Pos) = Unit
    override fun playThunderSound(pan: Double, decayMult: Double) = Unit
    override fun playGlassShatterSound(pos: Pos, heaviness: Double, amplitude: Double) = Unit
    override fun playCheckpointSound(checkpoint: Checkpoint) = Unit
    override fun playCycleSound() = Unit
    override fun hack(id: String, pos: Pos, dur: Double, faction: Faction, slots: IntArray) = Unit
    override fun glyph(id: String, pos: Pos, level: Int, dur: Double, faction: Faction, slots: IntArray) = Unit
    override fun announceHugeField(owner: Faction, mu: Int) = Unit
    override fun announcePortalDiscovery(name: String) = Unit
    override fun announceRecruitment(faction: Faction) = Unit
    override fun announceGlyphHack(faction: Faction) = Unit
    override fun announceCheckpointLead(leader: Faction, leadMu: Int) = Unit
}
