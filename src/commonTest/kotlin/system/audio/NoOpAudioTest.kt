package system.audio

import Factory
import World
import agent.AgentSize
import agent.Faction
import agent.NonFaction
import extension.VectorField
import portal.Field
import portal.Link
import portal.Portal
import system.Checkpoint
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The headless [NoOpAudio] sink: every interface method is a no-op, but they must all be callable (the game
 * logic fires audio freely outside the browser). Drives every method once to lock the seam in.
 */
class NoOpAudioTest {

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
    }

    @Test
    fun everyNoOpMethodIsCallable() {
        val audio: Audio = NoOpAudio
        val pos = Pos(100, 100)
        val agent = Factory.frog()
        val a = Portal.create(Pos(0, 0))
        val b = Portal.create(Pos(300, 0))
        val c = Portal.create(Pos(0, 400))
        val link = requireNotNull(Link.create(a, b, agent)) { "link forms" }
        val field = requireNotNull(Field.create(a, b, c, agent)) { "field forms" }
        val npc = NonFaction(pos, 5.0, AgentSize(0), pos, VectorField.EMPTY, -1)
        val checkpoint = Checkpoint(enlMu = 100, resMu = 50, isCycleEnd = false)

        audio.playLevelUp(pos)
        audio.playPortalCreationSound(pos)
        audio.playLinkingSound(link)
        audio.playFieldingSound(field)
        audio.playShieldRemoveSound(pos, 1)
        audio.playResoDeploySound(pos, 1)
        audio.playModDeploySound(pos, 1)
        audio.playShieldDeploySound(pos, 1)
        audio.playVirusSound(pos, Faction.ENL)
        audio.playRecruitSuccess(pos)
        audio.playOffScreenLocationCreationSound()
        audio.playNpcCreationSound(npc)
        audio.playKnockOutSound(pos)
        audio.playThunderSound(0.5, 0.8)
        audio.playGlassShatterSound(pos, 0.5, 0.8)
        audio.playCheckpointSound(checkpoint)
        audio.playCycleSound()
        audio.hack("id", pos, 1.0, Faction.RES, intArrayOf(0, 1))
        audio.glyph("id", pos, 1, 1.0, Faction.ENL, intArrayOf(0, 1))
        audio.announceHugeField(Faction.ENL, 1000)
        audio.announcePortalDiscovery("Portal")
        audio.announceRecruitment(Faction.RES)
        audio.announceGlyphHack(Faction.ENL)
        audio.announceCheckpointLead(Faction.RES, 500)

        assertTrue(true, "every NoOpAudio method returned without touching Web Audio")
    }
}
