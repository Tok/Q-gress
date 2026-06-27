package util.ui

import config.DropRates
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import system.audio.AmbientBed
import system.audio.AmbientPrefs
import system.audio.AudioFx
import system.audio.AudioPrefs
import system.audio.InstrumentPrefs
import system.audio.KickDrum
import system.audio.MixerPrefs
import util.GameplayPrefs

/**
 * The **tuning export** — a small, slim collapsible section at the bottom of the AUDIO tab ([AudioPanel]):
 * every live-tunable setting (audio FX + gameplay) as one **copy-paste JSON** block, so values dialled in the
 * AUDIO tab / Menu can be pasted back for baking new defaults. **Click the JSON to copy it.** ([resetToDefaults]
 * lives at the top of the AUDIO tab.) The text refreshes live via [refresh] (except while focused, so a
 * selection survives), but only while the section is expanded.
 */
object TuningLab {
    private var text: HTMLTextAreaElement? = null
    private var status: HTMLElement? = null

    /** Refresh the JSON in place — called each frame by [AudioPanel.update]; no-op while collapsed/hidden/focused. */
    fun refresh() {
        val box = text ?: return
        if (box.offsetParent == null) return // section collapsed or tab hidden — skip
        if (document.activeElement != box) box.value = snapshot() // don't stomp an active selection
    }

    /** A slim `<details>` (collapsed by default) holding the copy-paste JSON — embedded at the AUDIO tab bottom. */
    fun section(): HTMLElement {
        val details = document.createElement("details") as HTMLElement
        details.className = "audioTuning"
        val summary = document.createElement("summary") as HTMLElement
        summary.textContent = "Tuning · copy-paste JSON (click to copy)"
        details.appendChild(summary)
        val area = document.createElement("textarea") as HTMLTextAreaElement
        area.className = "tuningLabText"
        area.readOnly = true
        area.value = snapshot()
        area.asDynamic().onclick = {
            area.select() // click anywhere on the JSON → select all + copy
            copy()
            null
        }
        text = area
        details.appendChild(area)
        status = el("div", "tuningLabStatus")
        details.appendChild(status as HTMLElement)
        return details
    }

    /** Restore audio + gameplay settings to their shipped defaults, re-syncing the AUDIO knobs + Menu sliders. */
    fun resetToDefaults() {
        AudioFx.resetToDefaults()
        KickDrum.resetTuning()
        AmbientBed.resetTuning()
        AudioPrefs.save()
        InstrumentPrefs.save()
        AmbientPrefs.save()
        AudioPanel.syncFromState() // refresh the AUDIO tab's knobs/pad/adsr + ambient toggle
        GameplayPrefs.resetToDefaults()
        syncMenuSlider("combatDynSlider", config.Config.combatDynamism)
        syncMenuSlider("progressSlider", config.Config.progressSpeed)
        text?.value = snapshot()
    }

    /** The full settings snapshot as pretty JSON: { audio, gameplay, dropRates }. */
    private fun snapshot(): String {
        val o: dynamic = js("({})")
        o.audio = AudioPrefs.json()
        o.mixer = MixerPrefs.json()
        o.instruments = InstrumentPrefs.json()
        o.ambient = AmbientPrefs.json()
        o.gameplay = GameplayPrefs.json()
        o.dropRates = dropRatesJson()
        return JSON.stringify(o, null, 2)
    }

    private fun dropRatesJson(): dynamic {
        val o: dynamic = js("({})")
        o.keyChance = DropRates.keyChance
        o.xmpDropMultiplier = DropRates.xmpDropMultiplier
        o.usDropChance = DropRates.usDropChance
        o.shields = mapJson(DropRates.shieldChance)
        o.heatSinks = mapJson(DropRates.heatSinkChance)
        o.multihacks = mapJson(DropRates.multihackChance)
        o.viruses = mapJson(DropRates.virusChance)
        return o
    }

    private fun <K> mapJson(m: Map<K, Double>): dynamic {
        val o: dynamic = js("({})")
        m.forEach { (k, v) -> o[k.toString()] = v }
        return o
    }

    private fun copy() {
        val box = text ?: return
        window.navigator.asDynamic().clipboard?.writeText(box.value)
        status?.textContent = "Copied ✓"
    }

    private fun syncMenuSlider(id: String, value: Double) {
        (document.getElementById(id) as? HTMLInputElement)?.value = value.toString()
    }
}
