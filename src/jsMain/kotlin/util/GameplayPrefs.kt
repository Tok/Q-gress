package util

import config.Config

/**
 * Persists the gameplay-affecting tunables ([Config.combatDynamism], [Config.progressSpeed],
 * [Config.portalChurnRate]) across reloads, mirroring [VolumePrefs] / [AudioPrefs]. These change how a run
 * plays out, so the TUNING LAB also surfaces them (copy-paste JSON) and can [resetToDefaults]. [load] runs at
 * startup **before** the menu sliders build; [save] is called whenever a slider moves.
 */
object GameplayPrefs {
    private const val KEY = "qgress.gameplay"

    // The canonical defaults live in commonMain [Config] (single source); kept aliased here for the menu/slider
    // call sites that read them.
    const val DEFAULT_COMBAT = Config.DEFAULT_COMBAT_DYNAMISM
    const val DEFAULT_PROGRESS = Config.DEFAULT_PROGRESS_SPEED
    const val DEFAULT_CHURN = Config.DEFAULT_PORTAL_CHURN

    fun load() {
        val o = Prefs.read(KEY) ?: return
        Prefs.apply(o.combatDynamism) { Config.combatDynamism = it }
        Prefs.apply(o.progressSpeed) { Config.progressSpeed = it }
        Prefs.apply(o.portalChurnRate) { Config.portalChurnRate = it }
    }

    fun save() = Prefs.save(KEY, ::json)

    /** The current gameplay tunables as a plain object — persisted by [save] and shown by the TUNING LAB. */
    fun json(): dynamic {
        val o: dynamic = js("({})")
        o.combatDynamism = Config.combatDynamism
        o.progressSpeed = Config.progressSpeed
        o.portalChurnRate = Config.portalChurnRate
        return o
    }

    /** Restore the gameplay knobs to their shipped defaults (the TUNING LAB reset), then persist. */
    fun resetToDefaults() {
        Config.combatDynamism = DEFAULT_COMBAT
        Config.progressSpeed = DEFAULT_PROGRESS
        Config.portalChurnRate = DEFAULT_CHURN
        save()
    }
}
