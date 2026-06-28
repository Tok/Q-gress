package system.ui.panel

import kotlinx.browser.document
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import system.audio.AmbientBed
import system.audio.AmbientPrefs
import system.audio.AudioFx
import system.audio.AudioPrefs
import system.audio.InstrumentPrefs
import system.audio.KickDrum
import system.audio.Mixer
import system.audio.MixerPrefs
import system.audio.Scale
import system.ui.AudioViz
import system.ui.Footer
import system.ui.TuningLab
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin

/**
 * The **AUDIO** footer tab: a live master-FX control surface (the sound *triggers* live in the `#audio` demo).
 * A read-only **key** display (major/minor follows whoever leads on MU), a **filter pad** (low-pass cutoff ×
 * resonance), big **knobs** for reverb / delay / compression, a master **ADSR** (display + knobs, wired into
 * the shared one-shot voice), and a live **scope + spectrum** off [AudioFx.analyser]. Every change persists
 * via [AudioPrefs]; the TUNING LAB resets + exports the values. Built once; [update] redraws the live bits.
 */
object AudioPanel {
    private const val CUTOFF_MIN = 200.0
    private val BIG_KNOBS = setOf("Reverb", "Distort") // the headline FX — φ-larger + ticked to draw the eye
    private val knobs = mutableListOf<Knob>()
    private var built = false
    private var lead: HTMLElement? = null
    private var padCanvas: HTMLCanvasElement? = null
    private var scope: HTMLCanvasElement? = null
    private var spectrum: HTMLCanvasElement? = null
    private var adsr: HTMLCanvasElement? = null
    private var glassEl: HTMLElement? = null
    private var fxPane: HTMLElement? = null
    private val panes = mutableMapOf<String, HTMLElement>()
    private val subButtons = mutableMapOf<String, HTMLElement>()

    fun update() {
        if (!ensure()) return
        if (glassEl?.offsetParent == null) return // tab hidden — skip the per-frame redraws
        if (fxPane?.offsetParent != null) { // only the Master FX sub-tab has the live viz/lead
            refreshLead()
            scope?.let { AudioViz.scope(it) }
            spectrum?.let { AudioViz.spectrum(it) }
        }
        TuningLab.refresh() // live-update the collapsed JSON export (no-op while collapsed/focused)
    }

    private fun ensure(): Boolean {
        if (built) return true
        if (document.body == null) return false
        built = true
        val glass = el("div", "footerGlass audioPanel")
        glassEl = glass
        glass.appendChild(subtabStrip())
        fxPane = el("div", "audioPane").also {
            it.appendChild(leadRow())
            it.appendChild(controlsRow())
            it.appendChild(vizRow())
        }
        panes["fx"] = fxPane as HTMLElement
        panes["mixer"] = mixerSubtab()
        panes["instruments"] = instrumentsSubtab()
        panes["ambient"] = ambientSubtab()
        panes["tts"] = TtsPanel.build()
        panes.values.forEach { glass.appendChild(it) }
        glass.appendChild(TuningLab.section()) // collapsed copy-paste JSON export (audio + gameplay) at the bottom
        Footer.tab("audio").appendChild(glass)
        switchSub("fx")
        return true
    }

    // --- Sub-tabs (Master FX | Mixer | Instruments) + the Reset button ----------------------------
    private fun subtabStrip(): HTMLElement {
        val strip = el("div", "audioSubtabs")
        strip.appendChild(subTabButton("Master FX", "fx"))
        strip.appendChild(subTabButton("Mixer", "mixer"))
        strip.appendChild(subTabButton("Instruments", "instruments"))
        strip.appendChild(subTabButton("Ambient", "ambient"))
        strip.appendChild(subTabButton("Voice", "tts"))
        val reset = document.createElement("button") as HTMLButtonElement
        reset.className = "audioReset" // far right: restore audio + gameplay defaults
        reset.textContent = "Reset to defaults"
        reset.onclick = {
            TuningLab.resetToDefaults()
            null
        }
        strip.appendChild(reset)
        return strip
    }

    private fun subTabButton(label: String, name: String): HTMLElement {
        val b = document.createElement("button") as HTMLButtonElement
        b.className = "audioSubtab"
        b.textContent = label
        b.onclick = {
            switchSub(name)
            null
        }
        subButtons[name] = b
        return b
    }

    private fun switchSub(name: String) {
        panes.forEach { (n, p) -> p.style.display = if (n == name) "block" else "none" }
        subButtons.forEach { (n, b) -> if (n == name) b.classList.add("active") else b.classList.remove("active") }
    }

    // --- Key (major/minor) read-only display: follows the live MU lead (Scale), not player-set ---------
    private fun leadRow(): HTMLElement {
        val row = el("div", "audioLeadRow")
        row.appendChild(el("div", "audioHead").also { it.textContent = "Key" })
        lead = el("div", "audioLead").also { it.textContent = "—" }
        row.appendChild(lead as HTMLElement)
        return row
    }

    // --- Mixer sub-tab: a per-role channel (volume + mute) ----------------------------------------
    private fun mixerSubtab(): HTMLElement {
        val pane = el("div", "audioPane")
        pane.appendChild(el("div", "audioHead").also { it.textContent = "Mixer · per-role levels" })
        Mixer.Group.values().forEach { pane.appendChild(channelStrip(it)) }
        return pane
    }

    private fun channelStrip(g: Mixer.Group): HTMLElement {
        val row = el("div", "audioChannel")
        row.appendChild(el("div", "audioChannelLabel").also { it.textContent = g.label })
        val slider = document.createElement("input") as HTMLInputElement
        slider.type = "range"
        slider.className = "slider"
        slider.min = "0"
        slider.max = "1"
        slider.step = "0.01"
        slider.value = Mixer.volume(g).toString()
        slider.oninput = {
            Mixer.setVolume(g, slider.valueAsNumber)
            MixerPrefs.save()
            null
        }
        row.appendChild(slider)
        val mute = document.createElement("button") as HTMLButtonElement
        mute.className = "audioMute"
        fun paint() {
            mute.textContent = if (Mixer.isMuted(g)) "muted" else "mute"
            mute.classList.toggle("on", Mixer.isMuted(g))
        }
        paint()
        mute.onclick = {
            Mixer.setMuted(g, !Mixer.isMuted(g))
            paint()
            MixerPrefs.save()
            null
        }
        row.appendChild(mute)
        return row
    }

    // --- Instruments sub-tab: per-instrument synth tuning (the explosion basskick) ----------------
    private fun instrumentsSubtab(): HTMLElement {
        val pane = el("div", "audioPane")
        val box = el("div", "audioSection")
        val head = el("div", "audioLeadRow")
        head.appendChild(el("div", "audioHead").also { it.textContent = "Explosion kick (XMP / Ultra-Strike)" })
        val test = document.createElement("button") as HTMLButtonElement
        test.className = "audioReset"
        test.textContent = "▶ Test"
        test.onclick = {
            KickDrum.test()
            null
        }
        head.appendChild(test)
        box.appendChild(head)
        val grid = el("div", "audioKnobs")
        grid.appendChild(knob("Pitch", 0.4..2.5, 1.0, { KickDrum.pitchMult }, { KickDrum.setPitchMult(it) }, ::mult))
        grid.appendChild(knob("Decay", 0.3..3.0, 1.0, { KickDrum.decayMult }, { KickDrum.setDecayMult(it) }, ::mult))
        grid.appendChild(knob("Click", 0.0..3.0, 1.0, { KickDrum.clickMult }, { KickDrum.setClickMult(it) }, ::mult))
        grid.appendChild(knob("Drive", 0.0..1.0, 0.0, { KickDrum.drive }, { KickDrum.setDrive(it) }, ::pct))
        box.appendChild(grid)
        pane.appendChild(box)
        return pane
    }

    // --- Ambient sub-tab: the generative atmospheric bed (on/off + level + cutoff) -----------------
    private var ambientRepaint: (() -> Unit)? = null

    private fun ambientSubtab(): HTMLElement {
        val pane = el("div", "audioPane")
        val box = el("div", "audioSection")
        val head = el("div", "audioLeadRow")
        head.appendChild(el("div", "audioHead").also { it.textContent = "Field hum (volume ∝ field coverage)" })
        val toggle = document.createElement("button") as HTMLButtonElement
        toggle.className = "audioMute"
        fun paint() {
            toggle.textContent = if (AmbientBed.enabled) "on" else "off"
            toggle.classList.toggle("on", AmbientBed.enabled)
        }
        ambientRepaint = ::paint
        paint()
        toggle.onclick = {
            AmbientBed.setEnabled(!AmbientBed.enabled)
            paint()
            AmbientPrefs.save()
            null
        }
        head.appendChild(toggle)
        box.appendChild(head)
        val grid = el("div", "audioKnobs")
        grid.appendChild(knob("Level", 0.0..1.0, 0.6, { AmbientBed.level }, { AmbientBed.setLevel(it) }, ::pct))
        grid.appendChild(knob("Cutoff", 80.0..2000.0, 320.0, { AmbientBed.cutoffHz }, { AmbientBed.setCutoff(it) }, ::khz))
        grid.appendChild(knob("Distance", 0.0..1.0, 0.4, { AmbientBed.distance }, { AmbientBed.setDistance(it) }, ::pct))
        box.appendChild(grid)
        pane.appendChild(box)
        return pane
    }

    private fun refreshLead() {
        val major = Scale.isLeading()
        lead?.textContent = if (major) "major — your faction leads" else "minor — your faction trails"
        lead?.classList?.toggle("major", major)
        lead?.classList?.toggle("minor", !major)
    }

    // --- The control surface: filter pad + FX knobs + ADSR -------------------------------------------
    private fun controlsRow(): HTMLElement {
        val row = el("div", "audioControls")
        row.appendChild(filterSection())
        row.appendChild(fxKnobs())
        row.appendChild(lfoSection())
        row.appendChild(adsrSection())
        return row
    }

    private fun lfoSection(): HTMLElement {
        val box = el("div", "audioSection")
        box.appendChild(el("div", "audioHead").also { it.textContent = "LFO → cutoff" })
        val grid = el("div", "audioKnobs")
        grid.appendChild(knob("Rate", AudioFx.LFO_MIN_HZ..AudioFx.LFO_MAX_HZ, 1.0, { AudioFx.lfoRateHz }, { AudioFx.setLfoRate(it) }, ::hz))
        grid.appendChild(knob("Depth", 0.0..1.0, 0.0, { AudioFx.lfoDepth }, { AudioFx.setLfoDepth(it) }, ::pct))
        box.appendChild(grid)
        return box
    }

    private fun filterSection(): HTMLElement {
        val box = el("div", "audioSection")
        box.appendChild(el("div", "audioHead").also { it.textContent = "Filter · cutoff × resonance" })
        val canvas = makeCanvas("audioPad", PAD_W, PAD_H)
        padCanvas = canvas
        val onMove = { e: MouseEvent ->
            val r = canvas.getBoundingClientRect()
            val nx = ((e.clientX - r.left) / r.width).coerceIn(0.0, 1.0)
            val ny = (1.0 - (e.clientY - r.top) / r.height).coerceIn(0.0, 1.0)
            AudioFx.setLowpass(cutoffHz(nx))
            AudioFx.setLowpassQ(AudioFx.MIN_Q + ny * (AudioFx.MAX_Q - AudioFx.MIN_Q))
            persist()
            syncFromState() // drag the pad → the Cutoff/Reso knobs (and the rest) follow
        }
        drag(canvas, onMove)
        box.appendChild(canvas)
        // Cutoff + resonance knobs under the pad, two-way synced with it (moving one drags the other).
        val grid = el("div", "audioKnobs")
        grid.appendChild(
            knob("Cutoff", 0.0..1.0, 1.0, { cutoffNorm() }, {
                AudioFx.setLowpass(cutoffHz(it))
                drawPad()
            }, { khz(cutoffHz(it)) }),
        )
        grid.appendChild(
            knob("Reso", AudioFx.MIN_Q..AudioFx.MAX_Q, AudioFx.MIN_Q, { AudioFx.lowpassQ }, {
                AudioFx.setLowpassQ(it)
                drawPad()
            }, ::qfmt),
        )
        box.appendChild(grid)
        drawPad()
        return box
    }

    // The pad/Cutoff-knob share a log cutoff scale (200 Hz → open); norm is the 0..1 position on it.
    private fun cutoffNorm(): Double = (ln(AudioFx.lowpassHz / CUTOFF_MIN) / ln(AudioFx.LOWPASS_OPEN_HZ / CUTOFF_MIN)).coerceIn(0.0, 1.0)

    private fun cutoffHz(norm: Double): Double = CUTOFF_MIN * (AudioFx.LOWPASS_OPEN_HZ / CUTOFF_MIN).pow(norm.coerceIn(0.0, 1.0))

    private fun khz(hz: Double): String = if (hz >= 1000.0) "${(hz / 100).toInt() / 10.0}k" else "${hz.toInt()}"
    private fun qfmt(q: Double): String = "Q${(q * 10).toInt() / 10.0}"

    private fun fxKnobs(): HTMLElement {
        val box = el("div", "audioSection")
        box.appendChild(el("div", "audioHead").also { it.textContent = "FX" })
        val grid = el("div", "audioKnobs")
        grid.appendChild(knob("Reverb", 0.0..1.0, 0.0, { AudioFx.reverbMix }, { AudioFx.setReverbMix(it) }, ::pct))
        grid.appendChild(knob("Echo", 0.0..1.0, 0.0, { AudioFx.delayMix }, { AudioFx.setDelayMix(it) }, ::pct))
        grid.appendChild(knob("Echo time", 0.0..AudioFx.MAX_DELAY_S, 0.25, { AudioFx.delayTimeS }, { AudioFx.setDelayTime(it) }, ::ms))
        grid.appendChild(knob("Feedback", 0.0..0.95, 0.3, { AudioFx.delayFeedback01 }, { AudioFx.setDelayFeedback(it) }, ::pct))
        grid.appendChild(knob("Distort", 0.0..1.0, 0.0, { AudioFx.distortionAmount }, { AudioFx.setDistortion(it) }, ::pct))
        grid.appendChild(knob("Compress", 0.0..1.0, 0.0, { AudioFx.compressAmount }, { AudioFx.setCompress(it) }, ::pct))
        box.appendChild(grid)
        return box
    }

    private fun adsrSection(): HTMLElement {
        val box = el("div", "audioSection")
        box.appendChild(el("div", "audioHead").also { it.textContent = "Envelope (ADSR)" })
        val canvas = makeCanvas("audioAdsr", ADSR_W, ADSR_H)
        adsr = canvas
        dragAdsr(canvas) // drag the curve's handles directly; the 4 knobs below stay in sync
        box.appendChild(canvas)
        val grid = el("div", "audioKnobs")
        grid.appendChild(envKnob("Attack", 0.0..0.5, 0.0, { AudioFx.envAttackS }, { AudioFx.setEnvAttack(it) }, ::ms))
        grid.appendChild(envKnob("Decay", 0.0..0.5, 0.0, { AudioFx.envDecayS }, { AudioFx.setEnvDecay(it) }, ::ms))
        grid.appendChild(envKnob("Sustain", 0.0..1.0, 1.0, { AudioFx.envSustain }, { AudioFx.setEnvSustain(it) }, ::pct))
        grid.appendChild(envKnob("Release", 0.2..3.0, 1.0, { AudioFx.envReleaseMult }, { AudioFx.setEnvRelease(it) }, ::mult))
        box.appendChild(grid)
        drawAdsr()
        return box
    }

    // An ADSR knob — like [knob] but its writes also redraw the envelope curve.
    private fun envKnob(
        label: String,
        range: ClosedFloatingPointRange<Double>,
        default: Double,
        read: () -> Double,
        write: (Double) -> Unit,
        fmt: (Double) -> String,
    ): HTMLElement = knob(label, range, default, read, {
        write(it)
        drawAdsr()
    }, fmt)

    private fun vizRow(): HTMLElement {
        val row = el("div", "audioViz")
        scope = makeCanvas("audioScope", VIZ_W, VIZ_H).also { row.appendChild(it) }
        spectrum = makeCanvas("audioSpectrum", VIZ_W, VIZ_H).also { row.appendChild(it) }
        return row
    }

    // --- A draggable knob (vertical drag), drawn on a canvas, with a tiny reset-to-default dot -------
    private class Knob(
        val canvas: HTMLCanvasElement,
        val valueLabel: HTMLElement,
        val range: ClosedFloatingPointRange<Double>,
        val default: Double,
        val read: () -> Double,
        val write: (Double) -> Unit,
        val fmt: (Double) -> String,
    ) {
        var reset: HTMLElement? = null // the per-knob reset nub (highlights when off-default)
    }

    // Light the reset nub when the knob differs from its default, so changed settings are obvious at a glance.
    private fun markChanged(k: Knob) {
        k.reset?.classList?.toggle("changed", abs(k.read() - k.default) > 1e-6)
    }

    private fun knob(
        label: String,
        range: ClosedFloatingPointRange<Double>,
        default: Double,
        read: () -> Double,
        write: (Double) -> Unit,
        fmt: (Double) -> String,
    ): HTMLElement {
        val big = label in BIG_KNOBS // a couple of headline knobs are φ-larger (with ticks) to draw the eye
        val cell = el("div", if (big) "audioKnob big" else "audioKnob")
        val px = if (big) KNOB_BIG_PX else KNOB_PX
        val canvas = makeCanvas("audioKnobDial", px, px)
        val value = el("div", "audioKnobVal").also { it.textContent = fmt(read()) }
        val k = Knob(canvas, value, range, default, read, write, fmt)
        knobs.add(k)
        val lo = range.start
        val hi = range.endInclusive
        fun commit(v: Double) {
            write(v)
            value.textContent = fmt(read())
            drawKnob(k)
            markChanged(k)
            persist()
        }
        drag(canvas) { e ->
            val r = canvas.getBoundingClientRect()
            val cur = ((read() - lo) / (hi - lo)).coerceIn(0.0, 1.0)
            val ny = (1.0 - (e.clientY - r.top) / r.height).coerceIn(0.0, 1.0)
            // Blend toward the pointer's vertical position so a click-drag anywhere on the dial tracks smoothly.
            val next = (cur * 0.5 + ny * 0.5)
            commit(lo + next.coerceIn(0.0, 1.0) * (hi - lo))
        }
        cell.appendChild(canvas)
        cell.appendChild(el("div", "audioKnobLabel").also { it.textContent = label })
        cell.appendChild(value)
        val reset = document.createElement("button") as HTMLButtonElement
        reset.className = "audioKnobReset" // tiny rubbery nub: snap the knob back to its default
        reset.title = "Reset to default"
        reset.onclick = {
            commit(default)
            null
        }
        cell.appendChild(reset)
        k.reset = reset
        drawKnob(k)
        markChanged(k)
        return cell
    }

    /** Refresh every knob dial + value + the pad/adsr from the current [AudioFx] state (after a TUNING LAB reset). */
    fun syncFromState() {
        if (!built) return
        knobs.forEach {
            it.valueLabel.textContent = it.fmt(it.read())
            drawKnob(it)
            markChanged(it)
        }
        drawPad()
        drawAdsr()
        ambientRepaint?.invoke() // the ambient on/off toggle isn't a knob — repaint it after a global reset
    }

    // --- Canvas drawing ------------------------------------------------------------------------------
    private fun drawKnob(k: Knob) {
        val ctx = ctx(k.canvas) ?: return
        val w = k.canvas.width.toDouble()
        val cx = w / 2
        val r = w / 2 - 5.0
        val cap = r - 4.0 // the chrome cap sits inside the value ring
        val v = ((k.read() - k.range.start) / (k.range.endInclusive - k.range.start)).coerceIn(0.0, 1.0)
        val a0 = PI * 0.75
        val a1 = PI * 2.25
        val a = a0 + (a1 - a0) * v
        ctx.clearRect(0.0, 0.0, w, w)
        // Scale ticks (headline knobs only) — small marks around the 270° sweep.
        if (k.canvas.width > KNOB_PX) {
            ctx.lineWidth = 1.0
            ctx.strokeStyle = "rgba(255,255,255,0.3)"
            for (t in 0..10) {
                val ta = a0 + (a1 - a0) * (t / 10.0)
                ctx.beginPath()
                ctx.moveTo(cx + cos(ta) * (r + 1.0), cx + sin(ta) * (r + 1.0))
                ctx.lineTo(cx + cos(ta) * (r + 4.0), cx + sin(ta) * (r + 4.0))
                ctx.stroke()
            }
        }
        // Value ring on the rim.
        ctx.lineWidth = 4.0
        ctx.strokeStyle = "rgba(255,255,255,0.16)"
        strokeArc(ctx, cx, r, a0, a1)
        ctx.strokeStyle = "#cfd6e6"
        strokeArc(ctx, cx, r, a0, a)
        // Chrome cap: a vertical neutral-gray gradient (light top, dark bottom, bright reflection band) + rim.
        val grad = ctx.createLinearGradient(0.0, cx - cap, 0.0, cx + cap)
        grad.addColorStop(0.0, "#e6e6e6")
        grad.addColorStop(0.42, "#8c8c8c")
        grad.addColorStop(0.5, "#d2d2d2")
        grad.addColorStop(0.58, "#6e6e6e")
        grad.addColorStop(1.0, "#343434")
        ctx.beginPath()
        ctx.arc(cx, cx, cap, 0.0, PI * 2)
        ctx.fillStyle = grad
        ctx.fill()
        ctx.lineWidth = 1.0
        ctx.strokeStyle = "rgba(255,255,255,0.28)" // bright rim highlight
        strokeArc(ctx, cx, cap, 0.0, PI * 2)
        // Pointer notch — dark so it reads against the chrome.
        ctx.beginPath()
        ctx.lineWidth = 2.5
        ctx.strokeStyle = "#222222"
        ctx.moveTo(cx + cos(a) * cap * 0.35, cx + sin(a) * cap * 0.35)
        ctx.lineTo(cx + cos(a) * cap * 0.92, cx + sin(a) * cap * 0.92)
        ctx.stroke()
    }

    private fun drawPad() {
        val canvas = padCanvas ?: return
        val ctx = ctx(canvas) ?: return
        val w = canvas.width.toDouble()
        val h = canvas.height.toDouble()
        ctx.clearRect(0.0, 0.0, w, h)
        ctx.fillStyle = "rgba(0,0,0,0.35)"
        ctx.fillRect(0.0, 0.0, w, h)
        grid(ctx, w, h)
        val nx = ln(AudioFx.lowpassHz / CUTOFF_MIN) / ln(AudioFx.LOWPASS_OPEN_HZ / CUTOFF_MIN)
        val ny = (AudioFx.lowpassQ - AudioFx.MIN_Q) / (AudioFx.MAX_Q - AudioFx.MIN_Q)
        val x = (nx.coerceIn(0.0, 1.0)) * w
        val y = (1.0 - ny.coerceIn(0.0, 1.0)) * h
        ctx.beginPath()
        ctx.fillStyle = "#cfe0ff"
        ctx.arc(x, y, 6.0, 0.0, PI * 2)
        ctx.fill()
    }

    // Fixed envelope layout so the draggable handles map predictably (X = time on a fixed scale).
    private class AdsrLayout(val pad: Double, val top: Double, val bot: Double, val sx: Double) {
        val peakX get() = pad + AudioFx.envAttackS * sx
        val kneeX get() = peakX + AudioFx.envDecayS * sx
        val kneeY get() = bot - (bot - top) * AudioFx.envSustain
        val relStartX get() = kneeX + ADSR_HOLD * sx
        val relEndX get() = relStartX + AudioFx.envReleaseMult * ADSR_REL_VIS * sx
    }

    private fun adsrLayout(canvas: HTMLCanvasElement): AdsrLayout {
        val pad = 4.0
        val span = ADSR_A_MAX + ADSR_D_MAX + ADSR_HOLD + 3.0 * ADSR_REL_VIS // fixed → stable handle positions
        return AdsrLayout(pad, pad, canvas.height - pad, (canvas.width - 2 * pad) / span)
    }

    private fun drawAdsr() {
        val canvas = adsr ?: return
        val ctx = ctx(canvas) ?: return
        val w = canvas.width.toDouble()
        val h = canvas.height.toDouble()
        val l = adsrLayout(canvas)
        ctx.clearRect(0.0, 0.0, w, h)
        ctx.fillStyle = "rgba(0,0,0,0.35)"
        ctx.fillRect(0.0, 0.0, w, h)
        ctx.beginPath()
        ctx.lineWidth = 2.0
        ctx.strokeStyle = "#cfd6e6"
        ctx.moveTo(l.pad, l.bot)
        ctx.lineTo(l.peakX, l.top) // attack → peak
        ctx.lineTo(l.kneeX, l.kneeY) // decay → sustain
        ctx.lineTo(l.relStartX, l.kneeY) // sustain hold
        ctx.lineTo(l.relEndX, l.bot) // release → silence
        ctx.stroke()
        // Draggable handles: peak (attack), knee (decay + sustain), release-end.
        dot(ctx, l.peakX, l.top)
        dot(ctx, l.kneeX, l.kneeY)
        dot(ctx, l.relEndX, l.bot)
    }

    private fun dot(ctx: CanvasRenderingContext2D, x: Double, y: Double) {
        ctx.beginPath()
        ctx.fillStyle = "#cfe0ff"
        ctx.arc(x, y, 3.5, 0.0, PI * 2)
        ctx.fill()
    }

    // Drag the envelope's handles directly: peak=attack, knee=decay+sustain, end=release. Locks the nearest
    // handle at mousedown for the whole gesture and keeps the 4 ADSR knobs in sync (via [syncFromState]).
    private fun dragAdsr(canvas: HTMLCanvasElement) {
        canvas.onmousedown = { down ->
            val handle = pickAdsrHandle(canvas, canvasX(canvas, down))
            val apply = { e: MouseEvent -> applyAdsrHandle(handle, canvas, canvasX(canvas, e), canvasY(canvas, e)) }
            apply(down)
            val move: (Event) -> Unit = { e -> apply(e.unsafeCast<MouseEvent>()) }
            var up: ((Event) -> Unit)? = null
            up = {
                document.removeEventListener("mousemove", move)
                up?.let { document.removeEventListener("mouseup", it) }
            }
            document.addEventListener("mousemove", move)
            document.addEventListener("mouseup", up)
            down.preventDefault()
            null
        }
    }

    private fun canvasX(canvas: HTMLCanvasElement, e: MouseEvent): Double {
        val r = canvas.getBoundingClientRect()
        return (e.clientX - r.left) / r.width * canvas.width
    }

    private fun canvasY(canvas: HTMLCanvasElement, e: MouseEvent): Double {
        val r = canvas.getBoundingClientRect()
        return (e.clientY - r.top) / r.height * canvas.height
    }

    // Which handle is nearest the pointer X: 0=peak (attack), 1=knee (decay/sustain), 2=release end.
    private fun pickAdsrHandle(canvas: HTMLCanvasElement, px: Double): Int {
        val l = adsrLayout(canvas)
        val d0 = abs(px - l.peakX)
        val d1 = abs(px - l.kneeX)
        val d2 = abs(px - l.relEndX)
        return if (d0 <= d1 && d0 <= d2) {
            0
        } else if (d1 <= d2) {
            1
        } else {
            2
        }
    }

    private fun applyAdsrHandle(handle: Int, canvas: HTMLCanvasElement, px: Double, py: Double) {
        val l = adsrLayout(canvas)
        when (handle) {
            0 -> AudioFx.setEnvAttack(((px - l.pad) / l.sx).coerceIn(0.0, ADSR_A_MAX))
            1 -> {
                AudioFx.setEnvDecay(((px - l.peakX) / l.sx).coerceIn(0.0, ADSR_D_MAX))
                AudioFx.setEnvSustain(((l.bot - py) / (l.bot - l.top)).coerceIn(0.0, 1.0))
            }
            else -> AudioFx.setEnvRelease((((px - l.relStartX) / l.sx) / ADSR_REL_VIS).coerceIn(0.2, 3.0))
        }
        drawAdsr()
        syncFromState() // refresh the 4 ADSR knobs to match the dragged shape
        persist()
    }

    // Stroke a centred arc (knob dial); colour + lineWidth are set by the caller. cx == cy (square canvas).
    private fun strokeArc(ctx: CanvasRenderingContext2D, c: Double, r: Double, a0: Double, a1: Double) {
        ctx.beginPath()
        ctx.arc(c, c, r, a0, a1, false)
        ctx.stroke()
    }

    private fun grid(ctx: CanvasRenderingContext2D, w: Double, h: Double) {
        ctx.strokeStyle = "rgba(255,255,255,0.08)"
        ctx.lineWidth = 1.0
        var i = 1
        while (i < 4) {
            ctx.beginPath()
            ctx.moveTo(w * i / 4, 0.0)
            ctx.lineTo(w * i / 4, h)
            ctx.stroke()
            ctx.beginPath()
            ctx.moveTo(0.0, h * i / 4)
            ctx.lineTo(w, h * i / 4)
            ctx.stroke()
            i++
        }
    }

    // --- helpers -------------------------------------------------------------------------------------
    private fun drag(canvas: HTMLCanvasElement, onMove: (MouseEvent) -> Unit) {
        canvas.onmousedown = { down ->
            onMove(down)
            val move: (Event) -> Unit = { e -> onMove(e.unsafeCast<MouseEvent>()) }
            var up: ((Event) -> Unit)? = null
            up = {
                document.removeEventListener("mousemove", move)
                up?.let { document.removeEventListener("mouseup", it) }
            }
            document.addEventListener("mousemove", move)
            document.addEventListener("mouseup", up)
            down.preventDefault()
            null
        }
    }

    private fun ctx(canvas: HTMLCanvasElement): CanvasRenderingContext2D? = canvas.getContext("2d") as? CanvasRenderingContext2D

    private fun makeCanvas(cls: String, w: Int, h: Int): HTMLCanvasElement {
        val c = document.createElement("canvas") as HTMLCanvasElement
        c.className = cls
        c.width = w
        c.height = h
        return c
    }

    // Persist whatever a knob/pad/ADSR change touched — master FX + instrument + ambient tuning (cheap writes).
    private fun persist() {
        AudioPrefs.save()
        InstrumentPrefs.save()
        AmbientPrefs.save()
    }

    private fun pct(v: Double): String = "${(v * 100).toInt()}%"
    private fun ms(v: Double): String = "${(v * 1000).toInt()} ms"
    private fun hz(v: Double): String = if (v < 1.0) "${(v * 100).toInt() / 100.0} Hz" else "${(v * 10).toInt() / 10.0} Hz"
    private fun mult(v: Double): String = "${(v * 100).toInt() / 100.0}×"

    private fun el(tag: String, cls: String): HTMLDivElement {
        val e = document.createElement(tag) as HTMLDivElement
        if (cls.isNotEmpty()) e.className = cls
        return e
    }

    private const val PAD_W = 150
    private const val PAD_H = 150 // square cutoff × resonance pad
    private const val ADSR_W = 150
    private const val ADSR_H = 70
    private const val ADSR_A_MAX = 0.5 // attack knob max (s) — also the envelope display's attack span
    private const val ADSR_D_MAX = 0.5 // decay knob max (s)
    private const val ADSR_HOLD = 0.3 // fixed sustain-hold width on the display (s)
    private const val ADSR_REL_VIS = 0.4 // display seconds per release-multiplier unit
    private const val VIZ_W = 280
    private const val VIZ_H = 70
    private const val KNOB_PX = 54
    private const val KNOB_BIG_PX = 87 // 54 × φ — the headline knobs
}
