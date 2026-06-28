package system.ui.panel

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import system.ui.el
import util.Prefs

/**
 * Named tuning presets for the player's faction — save the current TUNE-tab slider set under a name and
 * recall it later. Renders a small bar above the slider list (a `<select>` of saved names + Save + Delete).
 *
 * The library persists in `localStorage`; each entry is the per-faction CSV [TuningPanel.captureUser]
 * produces (Actions then Destinations). Recalling applies it via [TuningPanel.applyUser] — directly under
 * manual control; while an AI drives, it seeds the sliders the player can then lock. Only shown in the
 * interactive (player-tunable) mode.
 */
internal object TuningPresets {
    private const val KEY = "qgress.tuning.presets"

    private val presets = linkedMapOf<String, String>()
    private var select: HTMLSelectElement? = null

    /** Build the preset bar (saved-name select + Save + Delete) for the TUNE tab. */
    fun bar(): HTMLElement {
        load()
        val bar = el("div", "tunePresetBar")
        val sel = (document.createElement("select") as HTMLSelectElement).also { it.className = "select tunePresetSelect" }
        sel.title = "Recall a saved slider preset"
        select = sel
        refreshOptions()
        sel.onchange = {
            presets[sel.value]?.let { TuningPanel.applyUser(it) } // empty placeholder → no match → no-op
            null
        }
        bar.appendChild(sel)
        bar.appendChild(button("Save", "Save the current sliders as a named preset") { saveCurrent() })
        bar.appendChild(button("Delete", "Delete the selected preset") { deleteSelected() })
        return bar
    }

    private fun button(text: String, tip: String, onClick: () -> Unit): HTMLButtonElement {
        val b = document.createElement("button") as HTMLButtonElement
        b.className = "tunePresetBtn"
        b.textContent = text
        b.title = tip
        b.onclick = {
            onClick()
            null
        }
        return b
    }

    private fun saveCurrent() {
        val name = window.prompt("Save the current sliders as a preset named:")?.trim() ?: return
        if (name.isEmpty()) return
        presets[name] = TuningPanel.captureUser()
        persist()
        refreshOptions()
        select?.value = name
    }

    private fun deleteSelected() {
        val name = select?.value ?: return
        if (name.isEmpty() || presets.remove(name) == null) return
        persist()
        refreshOptions()
    }

    // Rebuild the <select>: a leading placeholder (so re-picking the same preset still fires onchange), then
    // one option per saved name.
    private fun refreshOptions() {
        val sel = select ?: return
        sel.innerHTML = ""
        sel.appendChild(option("", if (presets.isEmpty()) "No presets saved" else "Recall preset…"))
        presets.keys.forEach { sel.appendChild(option(it, it)) }
        sel.value = ""
    }

    private fun option(value: String, label: String): HTMLOptionElement = (document.createElement("option") as HTMLOptionElement).also {
        it.value = value
        it.textContent = label
    }

    private fun load() {
        val o = Prefs.read(KEY) ?: return
        val keys = js("Object.keys(o)").unsafeCast<Array<String>>()
        keys.forEach { k -> (o[k] as? String)?.let { presets[k] = it } }
    }

    private fun persist() = Prefs.save(KEY) {
        val o: dynamic = js("({})")
        presets.forEach { (k, v) -> o[k] = v }
        o
    }
}
