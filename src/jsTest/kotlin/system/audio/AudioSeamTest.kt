package system.audio

import Factory
import World
import agent.Faction
import agent.NonFaction
import portal.Field
import portal.Link
import system.Checkpoint
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * The audio-sink seam (PLAN Phase-B groundwork): game logic fires SFX/TTS through [Snd.sink], so it runs
 * headless without touching Web Audio. Proves (a) the default headless sink ([NoOpAudio]) lets the
 * tick-path logic run without crashing, and (b) logic actually reaches the installed sink. Mirrors
 * [system.effect.EffectsSeamTest].
 */
class AudioSeamTest {

    @BeforeTest
    fun clean() {
        World.allPortals.clear()
        World.allAgents.clear()
    }

    @AfterTest
    fun restore() {
        World.allPortals.clear()
        World.allAgents.clear()
        Snd.reset()
    }

    @Test
    fun portalRemoveRunsHeadlessWithDefaultSink() {
        // Default headless sink is NoOpAudio — remove() fires audio through the sink without touching Web Audio.
        val portal = Factory.portal(Faction.ENL)
        World.allPortals.add(portal)

        portal.remove()

        assertFalse(portal in World.allPortals, "removed portal is gone from the world")
    }

    @Test
    fun removeRoutesShatterSoundToTheInstalledSink() {
        val fake = CountingAudio()
        Snd.install(fake)
        val portal = Factory.portal(Faction.ENL)
        World.allPortals.add(portal)

        portal.remove()

        assertEquals(1, fake.playGlassShatterSound, "remove() fires exactly one glass-shatter sound through the sink")
    }

    @Test
    fun retaliateRoutesThunderToTheInstalledSink() {
        val fake = CountingAudio()
        Snd.install(fake)
        val enemyPortal = Factory.portal(Faction.ENL) // owned by ENL

        enemyPortal.retaliate(Factory.smurf()) // a RES agent intrudes → the portal zaps back

        assertEquals(1, fake.playThunderSound, "an enemy portal fires one retaliation thunder through the sink")
    }
}

/** A test [Audio] sink that records how many times each op was called. */
private class CountingAudio : Audio {
    var playLevelUp = 0
    var playPortalCreationSound = 0
    var playLinkingSound = 0
    var playFieldingSound = 0
    var playShieldRemoveSound = 0
    var playResoDeploySound = 0
    var playModDeploySound = 0
    var playShieldDeploySound = 0
    var playVirusSound = 0
    var playRecruitSuccess = 0
    var playOffScreenLocationCreationSound = 0
    var playNpcCreationSound = 0
    var playKnockOutSound = 0
    var playThunderSound = 0
    var playGlassShatterSound = 0
    var playCheckpointSound = 0
    var playCycleSound = 0
    var hack = 0
    var glyph = 0
    var announceHugeField = 0
    var announcePortalDiscovery = 0
    var announceRecruitment = 0
    var announceGlyphHack = 0
    var announceCheckpointLead = 0

    override fun playLevelUp(pos: Pos) {
        playLevelUp++
    }

    override fun playPortalCreationSound(pos: Pos) {
        playPortalCreationSound++
    }

    override fun playLinkingSound(link: Link) {
        playLinkingSound++
    }

    override fun playFieldingSound(field: Field) {
        playFieldingSound++
    }

    override fun playShieldRemoveSound(pos: Pos, level: Int) {
        playShieldRemoveSound++
    }

    override fun playResoDeploySound(pos: Pos, level: Int) {
        playResoDeploySound++
    }

    override fun playModDeploySound(pos: Pos, level: Int) {
        playModDeploySound++
    }

    override fun playShieldDeploySound(pos: Pos, level: Int) {
        playShieldDeploySound++
    }

    override fun playVirusSound(pos: Pos, faction: Faction) {
        playVirusSound++
    }

    override fun playRecruitSuccess(pos: Pos) {
        playRecruitSuccess++
    }

    override fun playOffScreenLocationCreationSound() {
        playOffScreenLocationCreationSound++
    }

    override fun playNpcCreationSound(npc: NonFaction) {
        playNpcCreationSound++
    }

    override fun playKnockOutSound(pos: Pos) {
        playKnockOutSound++
    }

    override fun playThunderSound(pan: Double, decayMult: Double) {
        playThunderSound++
    }

    override fun playGlassShatterSound(pos: Pos, heaviness: Double, amplitude: Double) {
        playGlassShatterSound++
    }

    override fun playCheckpointSound(checkpoint: Checkpoint) {
        playCheckpointSound++
    }

    override fun playCycleSound() {
        playCycleSound++
    }

    override fun hack(id: String, pos: Pos, dur: Double, faction: Faction, slots: IntArray) {
        hack++
    }

    override fun glyph(id: String, pos: Pos, level: Int, dur: Double, faction: Faction, slots: IntArray) {
        glyph++
    }

    override fun announceHugeField(owner: Faction, mu: Int) {
        announceHugeField++
    }

    override fun announcePortalDiscovery(name: String) {
        announcePortalDiscovery++
    }

    override fun announceRecruitment(faction: Faction) {
        announceRecruitment++
    }

    override fun announceGlyphHack(faction: Faction) {
        announceGlyphHack++
    }

    override fun announceCheckpointLead(leader: Faction, leadMu: Int) {
        announceCheckpointLead++
    }
}
