package util

import config.Config
import kotlinx.browser.localStorage

/**
 * Persists the gameplay-affecting tunables ([Config.combatDynamism], [Config.progressSpeed],
 * [Config.portalChurnRate]) across reloads, mirroring [VolumePrefs] / [AudioPrefs]. These change how a run
 * plays out, so the TUNING LAB also surfaces them (copy-paste JSON) and can [resetToDefaults]. [load] runs at
 * startup **before** the menu sliders build; [save] is called whenever a slider moves.
 */
object GameplayPrefs {
    private const val KEY = "qgress.gameplay"

    const val DEFAULT_COMBAT = 0.6
    const val DEFAULT_PROGRESS = 1.0
    const val DEFAULT_CHURN = 0.17

    fun load() {
        if (HtmlUtil.isNotRunningInBrowser()) return
        runCatching {
            val raw = localStorage.getItem(KEY) ?: return
            val o = JSON.parse<dynamic>(raw)
            apply(o.combatDynamism) { Config.combatDynamism = it }
            apply(o.progressSpeed) { Config.progressSpeed = it }
            apply(o.portalChurnRate) { Config.portalChurnRate = it }
        }
    }

    fun save() {
        if (HtmlUtil.isNotRunningInBrowser()) return
        runCatching { localStorage.setItem(KEY, JSON.stringify(json())) }
    }

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

    private fun apply(v: dynamic, set: (Double) -> Unit) {
        if (v != null) set(v.unsafeCast<Double>())
    }
}
