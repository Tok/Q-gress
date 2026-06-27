package system.ui

import agent.Faction
import config.Sim
import system.Checkpoint
import system.audio.HackSound
import system.audio.Sound
import system.audio.SteamSound
import system.display.fx.HackFx
import util.data.Pos

/**
 * The shared SFX palette — every standalone game sound as a (label → trigger) pair — auditioned both by the
 * `#audio` demo ([AudioDemo]) and the in-game **AUDIO** footer tab ([AudioPanel]). Level-keyed sounds read the
 * caller's currently-selected L1–L8 via [level]. (Link/field/NPC sounds need live world objects, so they're
 * excluded here.)
 */
object AudioSounds {
    private val center get() = Pos(Sim.width / 2, Sim.height / 2)

    fun list(level: () -> Int): List<Pair<String, () -> Unit>> = listOf(
        "Portal create" to { Sound.playPortalCreationSound(center) },
        "Portal remove" to { Sound.playPortalRemovalSound(center) },
        "Glass shatter" to { Sound.playGlassShatterSound(center) },
        "Neutralize" to { Sound.playNeutralizeSound(center) },
        "XMP" to { Sound.playXmpSound(center, level()) },
        "Ultra-strike" to { Sound.playUltraStrike(center) },
        "Burnout (steam)" to { SteamSound.play(center) },
        "Hack" to { HackSound.hack("demo", center, HackFx.HACK_S, Faction.ENL, IntArray(8) { level() }) },
        "Glyph" to { HackSound.glyph("demo", center, level(), HackFx.glyphDuration(level()), Faction.RES, IntArray(8) { level() }) },
        "Reso deploy" to { Sound.playResoDeploySound(center, level()) },
        "Mod deploy" to { Sound.playModDeploySound(center, level()) },
        "Shield deploy" to { Sound.playShieldDeploySound(center, level()) },
        "Shield remove" to { Sound.playShieldRemoveSound(center, level()) },
        "Knock-out (plop)" to { Sound.playKnockOutSound(center) },
        "Virus ADA (ENL)" to { Sound.playVirusSound(center, Faction.ENL) },
        "Virus JARVIS (RES)" to { Sound.playVirusSound(center, Faction.RES) },
        "Upgrade" to { Sound.playUpgradeSound(center, level()) },
        "Downgrade" to { Sound.playDowngradeSound(center, level()) },
        "Agent level-up" to { Sound.playLevelUp(center) },
        "Deploy (legacy)" to { Sound.playDeploySound(center, 50) },
        "Field down" to { Sound.playFieldDownSound() },
        "Cycle" to { Sound.playCycleSound() },
        "Fail" to { Sound.playFailSound() },
        "Checkpoint" to { Sound.playCheckpointSound(js("({})").unsafeCast<Checkpoint>()) },
        "Noise gen" to { Sound.playNoiseGenSound() },
        "Offscreen portal" to { Sound.playOffScreenLocationCreationSound() },
        "Thunder" to { Sound.playThunderSound(0.0) },
    )
}
