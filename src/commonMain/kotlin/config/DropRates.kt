package config

import items.types.HeatSinkType
import items.types.MultihackType
import items.types.ShieldType
import items.types.VirusType

/**
 * The single, tunable source of item drop chances per hack roll. Mutable so a future per-game setup
 * can override it; surfaced in-app via the Menu "Drop rates" readout and documented in
 * docs/MECHANICS.md (with ~2018 Ingress references).
 *
 * Resonator / XMP / Power-Cube *tiers* still roll via [portal.Quality]; those chances live there.
 * Link amps are inactive and never drop.
 */
object DropRates {
    /** Portal-key drop chance per hack. */
    var keyChance: Double = Probabilities.keyChance

    /** Weapon-draw multiplier per hack (sim-tuning, not authentic): sets how many weapon draws a hack rolls,
     *  driving BOTH the XMP yield and the Ultra-Strike yield ([portal.HackLoot.weaponDraws]). Was 2× (a boost
     *  so agents could sustain assaults on defended portals); halved to 1 to trim the XMP/US glut. Scaled live
     *  on top of this by the menu "Weapon drops" slider ([config.Config.weaponDropMultiplier]). */
    var xmpDropMultiplier: Int = 1

    /** Ultra-Strike drop chance per weapon draw (rarer than XMP, one Bernoulli vs XMP's quality cascade —
     *  as in Ingress). Also scaled live by the "Weapon drops" slider ([config.Config.weaponDropMultiplier]). */
    var usDropChance: Double = 0.25

    /** Per-shield-type drop chance (rarer = lower); defaults from [ShieldType.chance]. */
    val shieldChance: MutableMap<ShieldType, Double> =
        ShieldType.values().associateWith { it.chance }.toMutableMap()

    /** Per-heat-sink-type drop chance. */
    val heatSinkChance: MutableMap<HeatSinkType, Double> = mutableMapOf(
        HeatSinkType.COMMON to 10.0 / 100,
        HeatSinkType.RARE to 10.0 / 800,
        HeatSinkType.VERY_RARE to 10.0 / 2000,
    )

    /** Per-multi-hack-type drop chance (rarer = lower), mirroring the heat-sink scale. */
    val multihackChance: MutableMap<MultihackType, Double> = mutableMapOf(
        MultihackType.COMMON to 10.0 / 100,
        MultihackType.RARE to 10.0 / 800,
        MultihackType.VERY_RARE to 10.0 / 2000,
    )

    /** Per-virus drop chance per hack (ADA / JARVIS); defaults from `1 / VirusType.roll`. */
    val virusChance: MutableMap<VirusType, Double> =
        VirusType.values().associateWith { 1.0 / it.roll }.toMutableMap()
}
