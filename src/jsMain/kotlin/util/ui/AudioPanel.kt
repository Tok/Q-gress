package util.ui

import kotlinx.browser.document
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import util.AudioFx
import util.AudioPrefs
import util.Scale
import kotlin.math.PI
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
    private val knobs = mutableListOf<Knob>()
    private var built = false
    private var lead: HTMLElement? = null
    private var padCanvas: HTMLCanvasElement? = null
    private var scope: HTMLCanvasElement? = null
    private var spectrum: HTMLCanvasElement? = null
    private var adsr: HTMLCanvasElement? = null
    private var timeData: Uint8Array? = null
    private var freqData: Uint8Array? = null

    fun update() {
        if (!ensure()) return
        val glass = lead?.parentElement?.parentElement as? HTMLElement
        if (glass?.offsetParent == null) return // tab hidden — skip the per-frame redraws
        refreshLead()
        drawScope()
        drawSpectrum()
    }

    private fun ensure(): Boolean {
        if (built) return true
        if (document.body == null) return false
        built = true
        val glass = el("div", "footerGlass audioPanel")
        glass.appendChild(leadRow())
        glass.appendChild(controlsRow())
        glass.appendChild(vizRow())
        Footer.tab("audio").appendChild(glass)
        return true
    }

    // --- Key (major/minor) read-only display: follows the live MU lead (Scale), not player-set ---------
    private fun leadRow(): HTMLElement {
        val row = el("div", "audioLeadRow")
        row.appendChild(el("div", "audioHead").also { it.textContent = "Key" })
        lead = el("div", "audioLead").also { it.textContent = "—" }
        row.appendChild(lead as HTMLElement)
        return row
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
        row.appendChild(adsrSection())
        return row
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
            AudioFx.setLowpass(CUTOFF_MIN * (AudioFx.LOWPASS_OPEN_HZ / CUTOFF_MIN).pow(nx))
            AudioFx.setLowpassQ(AudioFx.MIN_Q + ny * (AudioFx.MAX_Q - AudioFx.MIN_Q))
            drawPad()
            AudioPrefs.save()
        }
        drag(canvas, onMove)
        box.appendChild(canvas)
        drawPad()
        return box
    }

    private fun fxKnobs(): HTMLElement {
        val box = el("div", "audioSection")
        box.appendChild(el("div", "audioHead").also { it.textContent = "FX" })
        val grid = el("div", "audioKnobs")
        grid.appendChild(knob("Reverb", 0.0, 1.0, { AudioFx.reverbMix }, { AudioFx.setReverbMix(it) }, ::pct))
        grid.appendChild(knob("Echo", 0.0, 1.0, { AudioFx.delayMix }, { AudioFx.setDelayMix(it) }, ::pct))
        grid.appendChild(knob("Echo time", 0.0, AudioFx.MAX_DELAY_S, { AudioFx.delayTimeS }, { AudioFx.setDelayTime(it) }, ::ms))
        grid.appendChild(knob("Feedback", 0.0, 0.95, { AudioFx.delayFeedback01 }, { AudioFx.setDelayFeedback(it) }, ::pct))
        grid.appendChild(knob("Compress", 0.0, 1.0, { AudioFx.compressAmount }, { AudioFx.setCompress(it) }, ::pct))
        box.appendChild(grid)
        return box
    }

    private fun adsrSection(): HTMLElement {
        val box = el("div", "audioSection")
        box.appendChild(el("div", "audioHead").also { it.textContent = "Envelope (ADSR)" })
        val canvas = makeCanvas("audioAdsr", ADSR_W, ADSR_H)
        adsr = canvas
        box.appendChild(canvas)
        val grid = el("div", "audioKnobs")
        grid.appendChild(
            knob("Attack", 0.0, 0.5, { AudioFx.envAttackS }, {
                AudioFx.setEnvAttack(it)
                drawAdsr()
            }, ::ms),
        )
        grid.appendChild(
            knob("Decay", 0.0, 0.5, { AudioFx.envDecayS }, {
                AudioFx.setEnvDecay(it)
                drawAdsr()
            }, ::ms),
        )
        grid.appendChild(
            knob("Sustain", 0.0, 1.0, { AudioFx.envSustain }, {
                AudioFx.setEnvSustain(it)
                drawAdsr()
            }, ::pct),
        )
        grid.appendChild(
            knob("Release", 0.2, 3.0, { AudioFx.envReleaseMult }, {
                AudioFx.setEnvRelease(it)
                drawAdsr()
            }, ::mult),
        )
        box.appendChild(grid)
        drawAdsr()
        return box
    }

    private fun vizRow(): HTMLElement {
        val row = el("div", "audioViz")
        scope = makeCanvas("audioScope", VIZ_W, VIZ_H).also { row.appendChild(it) }
        spectrum = makeCanvas("audioSpectrum", VIZ_W, VIZ_H).also { row.appendChild(it) }
        return row
    }

    // --- A draggable knob (vertical drag), drawn on a canvas -----------------------------------------
    private class Knob(
        val canvas: HTMLCanvasElement,
        val valueLabel: HTMLElement,
        val min: Double,
        val max: Double,
        val read: () -> Double,
        val write: (Double) -> Unit,
        val fmt: (Double) -> String,
    )

    private fun knob(
        label: String,
        min: Double,
        max: Double,
        read: () -> Double,
        write: (Double) -> Unit,
        fmt: (Double) -> String,
    ): HTMLElement {
        val cell = el("div", "audioKnob")
        val canvas = makeCanvas("audioKnobDial", KNOB_PX, KNOB_PX)
        val value = el("div", "audioKnobVal").also { it.textContent = fmt(read()) }
        val k = Knob(canvas, value, min, max, read, write, fmt)
        knobs.add(k)
        drag(canvas) { e ->
            val r = canvas.getBoundingClientRect()
            val cur = ((read() - min) / (max - min)).coerceIn(0.0, 1.0)
            val ny = (1.0 - (e.clientY - r.top) / r.height).coerceIn(0.0, 1.0)
            // Blend toward the pointer's vertical position so a click-drag anywhere on the dial tracks smoothly.
            val next = (cur * 0.5 + ny * 0.5)
            write(min + next.coerceIn(0.0, 1.0) * (max - min))
            value.textContent = fmt(read())
            drawKnob(k)
            AudioPrefs.save()
        }
        cell.appendChild(canvas)
        cell.appendChild(el("div", "audioKnobLabel").also { it.textContent = label })
        cell.appendChild(value)
        drawKnob(k)
        return cell
    }

    /** Refresh every knob dial + value + the pad/adsr from the current [AudioFx] state (after a TUNING LAB reset). */
    fun syncFromState() {
        if (!built) return
        knobs.forEach {
            it.valueLabel.textContent = it.fmt(it.read())
            drawKnob(it)
        }
        drawPad()
        drawAdsr()
    }

    // --- Canvas drawing ------------------------------------------------------------------------------
    private fun drawKnob(k: Knob) {
        val ctx = ctx(k.canvas) ?: return
        val w = k.canvas.width.toDouble()
        val cx = w / 2
        val r = w / 2 - 5.0
        val v = ((k.read() - k.min) / (k.max - k.min)).coerceIn(0.0, 1.0)
        val a0 = PI * 0.75
        val a1 = PI * 2.25
        val a = a0 + (a1 - a0) * v
        ctx.clearRect(0.0, 0.0, w, w)
        ctx.lineWidth = 4.0
        ctx.strokeStyle = "rgba(255,255,255,0.16)"
        strokeArc(ctx, cx, r, a0, a1)
        ctx.strokeStyle = "#cfd6e6"
        strokeArc(ctx, cx, r, a0, a)
        ctx.beginPath()
        ctx.lineWidth = 2.5
        ctx.strokeStyle = "#ffffff"
        ctx.moveTo(cx, cx)
        ctx.lineTo(cx + cos(a) * r, cx + sin(a) * r)
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

    private fun drawAdsr() {
        val canvas = adsr ?: return
        val ctx = ctx(canvas) ?: return
        val w = canvas.width.toDouble()
        val h = canvas.height.toDouble()
        val pad = 4.0
        ctx.clearRect(0.0, 0.0, w, h)
        ctx.fillStyle = "rgba(0,0,0,0.35)"
        ctx.fillRect(0.0, 0.0, w, h)
        // A=attack, D=decay-to-sustain, S=sustain hold, R=release tail. Scaled to fill the box for legibility.
        val a = AudioFx.envAttackS
        val d = AudioFx.envDecayS
        val rel = AudioFx.envReleaseMult * 0.4
        val span = (a + d + 0.3 + rel).coerceAtLeast(0.001)
        val sx = (w - 2 * pad) / span
        val top = pad
        val bot = h - pad
        val sustainY = bot - (bot - top) * AudioFx.envSustain
        ctx.beginPath()
        ctx.lineWidth = 2.0
        ctx.strokeStyle = "#cfd6e6"
        ctx.moveTo(pad, bot)
        var x = pad
        x += a * sx
        ctx.lineTo(x, top) // attack → peak
        x += d * sx
        ctx.lineTo(x, sustainY) // decay → sustain
        x += 0.3 * sx
        ctx.lineTo(x, sustainY) // sustain hold
        x += rel * sx
        ctx.lineTo(x, bot) // release → silence
        ctx.stroke()
    }

    private fun drawScope() {
        val canvas = scope ?: return
        val ctx = ctx(canvas) ?: return
        val an = AudioFx.analyser()
        val w = canvas.width.toDouble()
        val h = canvas.height.toDouble()
        ctx.clearRect(0.0, 0.0, w, h)
        ctx.fillStyle = "rgba(0,0,0,0.35)"
        ctx.fillRect(0.0, 0.0, w, h)
        if (an == null) return
        val n = an.frequencyBinCount as Int
        val data = timeData ?: Uint8Array(n).also { timeData = it }
        an.getByteTimeDomainData(data)
        ctx.beginPath()
        ctx.lineWidth = 1.5
        ctx.strokeStyle = "#6cf0c2"
        for (i in 0 until n) {
            val x = i.toDouble() / n * w
            val y = (data[i].toInt() and 0xff) / 255.0 * h
            if (i == 0) ctx.moveTo(x, y) else ctx.lineTo(x, y)
        }
        ctx.stroke()
    }

    private fun drawSpectrum() {
        val canvas = spectrum ?: return
        val ctx = ctx(canvas) ?: return
        val an = AudioFx.analyser()
        val w = canvas.width.toDouble()
        val h = canvas.height.toDouble()
        ctx.clearRect(0.0, 0.0, w, h)
        ctx.fillStyle = "rgba(0,0,0,0.35)"
        ctx.fillRect(0.0, 0.0, w, h)
        if (an == null) return
        val n = an.frequencyBinCount as Int
        val data = freqData ?: Uint8Array(n).also { freqData = it }
        an.getByteFrequencyData(data)
        val bars = 48
        val step = n / bars
        ctx.fillStyle = "#6c9cf0"
        for (b in 0 until bars) {
            val v = (data[b * step].toInt() and 0xff) / 255.0
            val bw = w / bars
            ctx.fillRect(b * bw, h - v * h, bw - 1.0, v * h)
        }
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

    private fun pct(v: Double): String = "${(v * 100).toInt()}%"
    private fun ms(v: Double): String = "${(v * 1000).toInt()} ms"
    private fun mult(v: Double): String = "${(v * 100).toInt() / 100.0}×"

    private fun el(tag: String, cls: String): HTMLDivElement {
        val e = document.createElement(tag) as HTMLDivElement
        if (cls.isNotEmpty()) e.className = cls
        return e
    }

    private const val PAD_W = 150
    private const val PAD_H = 110
    private const val ADSR_W = 150
    private const val ADSR_H = 70
    private const val VIZ_W = 280
    private const val VIZ_H = 70
    private const val KNOB_PX = 54
}
