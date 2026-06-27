package util.ui

import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import util.AudioFx
import util.Scale

/**
 * The **AUDIO** footer tab: a live sound toy. The same SFX palette + master-FX controls as the `#audio` demo
 * ([AudioSounds] / [AudioFx] / [Scale]), but docked in the footer so the player can reshape the audio **while
 * the sim runs** — Q-Gress is as much a toy as a game. Built once (lazy), then static: the controls drive the
 * shared audio chain directly, so there's nothing to refresh per frame.
 */
object AudioPanel {
    private var built = false
    private var level = 8
    private var leadMajor = false
    private val levelButtons = mutableListOf<HTMLButtonElement>()

    /** Lazy-build the tab on first sight (matches the other footer panels' update/ensure pattern). */
    fun update() {
        ensure()
    }

    private fun ensure() {
        if (built) return
        if (document.body == null) return
        built = true
        val glass = el("div", "footerGlass audioPanel")
        glass.appendChild(head("Master FX"))
        glass.appendChild(fxRow())
        glass.appendChild(head("Trigger"))
        glass.appendChild(levelRow())
        glass.appendChild(soundGrid())
        Footer.tab("audio").appendChild(glass)
        selectLevel(level)
    }

    // Master-FX tuning (live on the running sim) + a major/minor key toggle for the musical SFX.
    private fun fxRow(): HTMLElement {
        val row = el("div", "audioFx")
        val keyBtn = button("Key: minor", "audioBtn") {}
        keyBtn.onclick = {
            leadMajor = !leadMajor
            Scale.setLeading(leadMajor)
            keyBtn.textContent = if (leadMajor) "Key: major" else "Key: minor"
            null
        }
        row.appendChild(keyBtn)
        row.appendChild(slider("Low-pass", 200.0, AudioFx.LOWPASS_OPEN_HZ, AudioFx.LOWPASS_OPEN_HZ) { AudioFx.setLowpass(it) })
        row.appendChild(slider("High-pass", AudioFx.HIGHPASS_OPEN_HZ, 2000.0, AudioFx.HIGHPASS_OPEN_HZ) { AudioFx.setHighpass(it) })
        row.appendChild(slider("Reverb", 0.0, 1.0, 0.0) { AudioFx.setReverbMix(it) })
        return row
    }

    private fun slider(label: String, min: Double, max: Double, value: Double, onInput: (Double) -> Unit): HTMLElement {
        val field = el("div", "audioField")
        field.appendChild(el("div", "audioFieldLabel").also { it.textContent = label })
        val s = document.createElement("input") as HTMLInputElement
        s.type = "range"
        s.className = "slider"
        s.min = min.toString()
        s.max = max.toString()
        s.step = if (max <= 1.0) "0.01" else "1"
        s.value = value.toString()
        s.oninput = {
            onInput(s.valueAsNumber)
            null
        }
        field.appendChild(s)
        return field
    }

    // L1–L8: which level the level-keyed triggers (XMP, hack, deploy, …) fire at.
    private fun levelRow(): HTMLElement {
        val row = el("div", "audioLevels")
        levelButtons.clear()
        for (lvl in 1..8) {
            val b = button("L$lvl", "audioBtn audioMini") { selectLevel(lvl) }
            levelButtons.add(b)
            row.appendChild(b)
        }
        return row
    }

    private fun selectLevel(lvl: Int) {
        level = lvl
        levelButtons.forEachIndexed { i, b -> if (i + 1 == lvl) b.classList.add("sel") else b.classList.remove("sel") }
    }

    private fun soundGrid(): HTMLElement {
        val grid = el("div", "audioGrid")
        AudioSounds.list { level }.forEach { (label, action) -> grid.appendChild(button(label, "audioBtn", action)) }
        return grid
    }

    private fun head(text: String): HTMLElement = el("div", "audioHead").also { it.textContent = text }

    private fun button(label: String, cls: String, onClick: () -> Unit): HTMLButtonElement {
        val b = document.createElement("button") as HTMLButtonElement
        b.className = cls
        b.textContent = label
        b.onclick = {
            onClick()
            null
        }
        return b
    }

    private fun el(tag: String, cls: String): HTMLDivElement {
        val e = document.createElement(tag) as HTMLDivElement
        if (cls.isNotEmpty()) e.className = cls
        return e
    }
}
