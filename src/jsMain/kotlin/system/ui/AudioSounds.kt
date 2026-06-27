package system.ui

import agent.Faction
import config.Sim
import system.Checkpoint
import system.audio.HackSound
import system.audio.SoundUtil
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
        "Portal create" to { SoundUtil.playPortalCreationSound(center) },
        "Portal remove" to { SoundUtil.playPortalRemovalSound(center) },
        "Glass shatter" to { SoundUtil.playGlassShatterSound(center) },
        "Neutralize" to { SoundUtil.playNeutralizeSound(center) },
        "XMP" to { SoundUtil.playXmpSound(center, level()) },
        "Ultra-strike" to { SoundUtil.playUltraStrike(center) },
        "Burnout (steam)" to { SteamSound.play(center) },
        "Hack" to { HackSound.hack("demo", center, HackFx.HACK_S, Faction.ENL, IntArray(8) { level() }) },
        "Glyph" to { HackSound.glyph("demo", center, level(), HackFx.glyphDuration(level()), Faction.RES, IntArray(8) { level() }) },
        "Reso deploy" to { SoundUtil.playResoDeploySound(center, level()) },
        "Mod deploy" to { SoundUtil.playModDeploySound(center, level()) },
        "Shield deploy" to { SoundUtil.playShieldDeploySound(center, level()) },
        "Shield remove" to { SoundUtil.playShieldRemoveSound(center, level()) },
        "Knock-out (plop)" to { SoundUtil.playKnockOutSound(center) },
        "Virus ADA (ENL)" to { SoundUtil.playVirusSound(center, Faction.ENL) },
        "Virus JARVIS (RES)" to { SoundUtil.playVirusSound(center, Faction.RES) },
        "Upgrade" to { SoundUtil.playUpgradeSound(center, level()) },
        "Downgrade" to { SoundUtil.playDowngradeSound(center, level()) },
        "Agent level-up" to { SoundUtil.playLevelUp(center) },
        "Deploy (legacy)" to { SoundUtil.playDeploySound(center, 50) },
        "Field down" to { SoundUtil.playFieldDownSound() },
        "Cycle" to { SoundUtil.playCycleSound() },
        "Fail" to { SoundUtil.playFailSound() },
        "Checkpoint" to { SoundUtil.playCheckpointSound(js("({})").unsafeCast<Checkpoint>()) },
        "Noise gen" to { SoundUtil.playNoiseGenSound() },
        "Offscreen portal" to { SoundUtil.playOffScreenLocationCreationSound() },
        "Thunder" to { SoundUtil.playThunderSound(0.0) },
    )
}
