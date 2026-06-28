package system.ui.panel

import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import system.audio.Tts
import system.ui.el

/**
 * The **Voice** sub-tab of the AUDIO footer tab — controls for the [Tts] announcer: enable, verbosity, voice
 * selection, and volume/rate/pitch tuning, plus a Test button (which reads a random glyph sequence). Split out
 * of [AudioPanel] to keep it under the size limit.
 */
object TtsPanel {
    fun build(): HTMLElement {
        val pane = el("div", "audioPane")
        pane.appendChild(el("div", "audioHead").also { it.textContent = "Voice · spoken announcer (text-to-speech)" })
        pane.appendChild(enableRow())
        pane.appendChild(
            selectRow("Verbosity", Tts.Verbosity.values().map { it.name to it.label }, Tts.verbosity.name) { v ->
                Tts.Verbosity.values().firstOrNull { it.name == v }?.let { Tts.setVerbosity(it) }
            },
        )
        val voiceOpts = listOf("" to "Default (auto)") + Tts.voices().map { it.first to "${it.first} · ${it.second}" }
        pane.appendChild(selectRow("Voice", voiceOpts, Tts.voiceName ?: "") { name -> Tts.setVoice(name.ifBlank { null }) })
        pane.appendChild(sliderRow("Volume", Tts.volume, 0.0, 1.0) { Tts.setVolume(it) })
        pane.appendChild(sliderRow("Rate", Tts.rate, 0.5, 2.0) { Tts.setRate(it) })
        pane.appendChild(sliderRow("Pitch", Tts.pitch, 0.0, 2.0) { Tts.setPitch(it) })
        pane.appendChild(testButton())
        return pane
    }

    private fun enableRow(): HTMLElement {
        val row = el("div", "audioChannel")
        row.appendChild(el("div", "audioChannelLabel").also { it.textContent = "Enable" })
        val cb = document.createElement("input") as HTMLInputElement
        cb.type = "checkbox"
        cb.className = "checkbox"
        cb.checked = Tts.enabled
        cb.onchange = {
            Tts.setEnabled(cb.checked)
            null
        }
        row.appendChild(cb)
        return row
    }

    private fun selectRow(label: String, options: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit): HTMLElement {
        val row = el("div", "audioChannel")
        row.appendChild(el("div", "audioChannelLabel").also { it.textContent = label })
        val sel = document.createElement("select") as HTMLSelectElement
        sel.className = "topDrop"
        options.forEach { (value, text) ->
            val opt = document.createElement("option") as HTMLOptionElement
            opt.value = value
            opt.text = text
            if (value == selected) opt.selected = true
            sel.appendChild(opt)
        }
        sel.onchange = {
            onPick(sel.value)
            null
        }
        row.appendChild(sel)
        return row
    }

    private fun sliderRow(label: String, initial: Double, min: Double, max: Double, onInput: (Double) -> Unit): HTMLElement {
        val row = el("div", "audioChannel")
        row.appendChild(el("div", "audioChannelLabel").also { it.textContent = label })
        val s = document.createElement("input") as HTMLInputElement
        s.type = "range"
        s.className = "slider"
        s.min = min.toString()
        s.max = max.toString()
        s.step = "0.05"
        s.value = initial.toString()
        s.oninput = {
            onInput(s.valueAsNumber)
            null
        }
        row.appendChild(s)
        return row
    }

    private fun testButton(): HTMLElement {
        val row = el("div", "audioChannel")
        val b = document.createElement("button") as HTMLButtonElement
        b.className = "audioMute"
        b.textContent = "Test voice"
        b.onclick = {
            Tts.test()
            null
        }
        row.appendChild(b)
        return row
    }
}
