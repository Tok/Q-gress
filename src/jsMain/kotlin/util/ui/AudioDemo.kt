package util.ui

import kotlinx.browser.document
import kotlinx.dom.addClass
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import util.AudioFx
import util.Scale
import util.SoundUtil

/**
 * Audio demo (`#audio`): a panel of buttons that trigger every (standalone) game sound, so the SFX
 * palette can be auditioned without playing. Level-keyed sounds use the selected L1–L8. (Link/field/
 * NPC sounds need live world objects, so they're not included here.) The same palette + master-FX
 * controls also live in-game on the **AUDIO** footer tab ([AudioPanel]); both share [AudioSounds].
 */
object AudioDemo {
    private var level = 8

    fun show() {
        SoundUtil.enableAudio() // first gesture (the page load into the demo) resumes the audio context
        val panel = document.createElement("div") as HTMLDivElement
        panel.id = "demoPanel"
        panel.addClass("demoPanel", "coda", "audioDemo") // audioDemo → wide layout + button grid (see CSS)
        panel.append(heading("AUDIO DEMO"))
        panel.append(link("#demo", "← Demos", "demoBack"))
        panel.append(levelRow())
        panel.append(fxRow())
        val grid = document.createElement("div") as HTMLDivElement
        grid.addClass("demoGrid") // the sound buttons fill the screen in a responsive grid
        AudioSounds.list { level }.forEach { (label, action) -> grid.append(button(label, "demoButton", action)) }
        panel.append(grid)
        document.body?.append(panel)
    }

    // Live master-FX tuning (AudioFx) + a major/minor key toggle — so the SFX can be dialled in here.
    private var leadMajor = false
    private fun fxRow(): HTMLDivElement {
        val row = document.createElement("div") as HTMLDivElement
        row.addClass("demoRow", "demoFx")
        val keyBtn = document.createElement("button") as HTMLButtonElement
        keyBtn.className = "demoButton demoMini"
        keyBtn.innerHTML = "Key: minor"
        keyBtn.onclick = {
            leadMajor = !leadMajor
            Scale.setLeading(leadMajor)
            keyBtn.innerHTML = if (leadMajor) "Key: major" else "Key: minor"
            null
        }
        row.append(keyBtn)
        row.append(fxSlider("Low-pass", 200.0, AudioFx.LOWPASS_OPEN_HZ, AudioFx.LOWPASS_OPEN_HZ) { AudioFx.setLowpass(it) })
        row.append(fxSlider("High-pass", AudioFx.HIGHPASS_OPEN_HZ, 2000.0, AudioFx.HIGHPASS_OPEN_HZ) { AudioFx.setHighpass(it) })
        row.append(fxSlider("Reverb", 0.0, 1.0, 0.0) { AudioFx.setReverbMix(it) })
        return row
    }

    private fun fxSlider(label: String, min: Double, max: Double, value: Double, onInput: (Double) -> Unit): HTMLDivElement {
        val wrap = document.createElement("div") as HTMLDivElement
        wrap.addClass("demoFxItem")
        val lbl = document.createElement("div") as HTMLDivElement
        lbl.addClass("demoFxLabel")
        lbl.innerHTML = label
        val s = document.createElement("input") as HTMLInputElement
        s.type = "range"
        s.min = min.toString()
        s.max = max.toString()
        s.step = if (max <= 1.0) "0.01" else "1"
        s.value = value.toString()
        s.addClass("slider")
        s.oninput = {
            onInput(s.valueAsNumber)
            null
        }
        wrap.append(lbl)
        wrap.append(s)
        return wrap
    }

    private fun levelRow(): HTMLDivElement {
        val row = document.createElement("div") as HTMLDivElement
        row.addClass("demoRow")
        for (lvl in 1..8) row.append(button("L$lvl", "demoButton demoMini") { level = lvl })
        return row
    }

    private fun heading(text: String): HTMLDivElement {
        val d = document.createElement("div") as HTMLDivElement
        d.addClass("demoHeading")
        d.innerHTML = text
        return d
    }

    private fun button(label: String, cssClass: String, onClick: () -> Unit): HTMLButtonElement {
        val b = document.createElement("button") as HTMLButtonElement
        b.className = cssClass
        b.innerHTML = label
        b.onclick = {
            onClick()
            null
        }
        return b
    }

    private fun link(href: String, label: String, cssClass: String): HTMLAnchorElement {
        val a = document.createElement("a") as HTMLAnchorElement
        a.href = href
        a.className = cssClass
        a.innerHTML = label
        return a
    }
}
