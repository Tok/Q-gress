package util.ui

import config.DropRates
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import util.AudioFx
import util.AudioPrefs
import util.GameplayPrefs

/**
 * The **TUNING LAB** footer tab: every live-tunable setting (audio FX + gameplay) as one **copy-paste JSON**
 * block, so values dialled in the AUDIO tab / Menu can be pasted back for baking new defaults. A **Copy**
 * button and a **Reset to defaults** button (restores audio + gameplay, re-syncs the AUDIO knobs + Menu
 * sliders). The text refreshes live (except while it's focused, so a selection survives).
 */
object TuningLab {
    private var built = false
    private var text: HTMLTextAreaElement? = null
    private var status: HTMLElement? = null

    fun update() {
        if (!ensure()) return
        val box = text ?: return
        if (box.offsetParent == null) return // tab hidden — skip
        if (document.activeElement != box) box.value = snapshot() // don't stomp an active selection
    }

    private fun ensure(): Boolean {
        if (built) return true
        if (document.body == null) return false
        built = true
        val glass = el("div", "footerGlass tuningLab")
        glass.appendChild(el("div", "audioHead").also { it.textContent = "Tunable settings (copy-paste JSON)" })
        val area = document.createElement("textarea") as HTMLTextAreaElement
        area.className = "tuningLabText"
        area.readOnly = true
        area.value = snapshot()
        text = area
        glass.appendChild(area)
        val btns = el("div", "tuningLabBtns")
        btns.appendChild(button("Copy") { copy() })
        btns.appendChild(button("Reset to defaults") { reset() })
        status = el("span", "tuningLabStatus")
        btns.appendChild(status as HTMLElement)
        glass.appendChild(btns)
        Footer.tab("tuning").appendChild(glass)
        return true
    }

    /** The full settings snapshot as pretty JSON: { audio, gameplay, dropRates }. */
    private fun snapshot(): String {
        val o: dynamic = js("({})")
        o.audio = AudioPrefs.json()
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
        flash("Copied")
    }

    private fun reset() {
        AudioFx.resetToDefaults()
        AudioPrefs.save()
        AudioPanel.syncFromState() // refresh the AUDIO tab's knobs/pad/adsr
        GameplayPrefs.resetToDefaults()
        syncMenuSlider("combatDynSlider", config.Config.combatDynamism)
        syncMenuSlider("progressSlider", config.Config.progressSpeed)
        text?.value = snapshot()
        flash("Reset to defaults")
    }

    private fun syncMenuSlider(id: String, value: Double) {
        (document.getElementById(id) as? HTMLInputElement)?.value = value.toString()
    }

    private fun flash(msg: String) {
        status?.textContent = msg
    }

    private fun button(label: String, onClick: () -> Unit): HTMLElement {
        val b = document.createElement("button") as HTMLElement
        b.className = "trainAction" // reuse the TRAIN tab's button look
        b.textContent = label
        b.asDynamic().onclick = {
            onClick()
            null
        }
        return b
    }

    private fun el(tag: String, cls: String): HTMLElement {
        val e = document.createElement(tag) as HTMLElement
        if (cls.isNotEmpty()) e.className = cls
        return e
    }
}
