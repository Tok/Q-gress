package util.ui

import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import system.audio.AudioFx

/** The AUDIO-tab live visualisers — a waveform **scope** and a **spectrum**, drawn each frame off the
 *  [AudioFx.analyser] tap. Split out of [AudioPanel] (size). */
object AudioViz {
    private var timeData: Uint8Array? = null
    private var freqData: Uint8Array? = null

    fun scope(canvas: HTMLCanvasElement) {
        val ctx = bg(canvas) ?: return
        val an = AudioFx.analyser() ?: return
        val w = canvas.width.toDouble()
        val h = canvas.height.toDouble()
        val n = an.frequencyBinCount as Int
        val data = timeData ?: Uint8Array(n).also { timeData = it }
        an.getByteTimeDomainData(data)
        ctx.beginPath()
        ctx.lineWidth = 1.5
        ctx.strokeStyle = "#d2d2d2" // neutral light gray (faction-agnostic viz)
        for (i in 0 until n) {
            val x = i.toDouble() / n * w
            val y = (data[i].toInt() and 0xff) / 255.0 * h
            if (i == 0) ctx.moveTo(x, y) else ctx.lineTo(x, y)
        }
        ctx.stroke()
    }

    fun spectrum(canvas: HTMLCanvasElement) {
        val ctx = bg(canvas) ?: return
        val an = AudioFx.analyser() ?: return
        val w = canvas.width.toDouble()
        val h = canvas.height.toDouble()
        val n = an.frequencyBinCount as Int
        val data = freqData ?: Uint8Array(n).also { freqData = it }
        an.getByteFrequencyData(data)
        val bars = 48
        val step = n / bars
        ctx.fillStyle = "#d2d2d2" // neutral light gray (faction-agnostic viz)
        for (b in 0 until bars) {
            val v = (data[b * step].toInt() and 0xff) / 255.0
            val bw = w / bars
            ctx.fillRect(b * bw, h - v * h, bw - 1.0, v * h)
        }
    }

    // Clear + paint the dark backdrop; returns the 2D context (or null if unavailable).
    private fun bg(canvas: HTMLCanvasElement): CanvasRenderingContext2D? {
        val ctx = canvas.getContext("2d") as? CanvasRenderingContext2D ?: return null
        val w = canvas.width.toDouble()
        val h = canvas.height.toDouble()
        ctx.clearRect(0.0, 0.0, w, h)
        ctx.fillStyle = "rgba(0,0,0,0.35)"
        ctx.fillRect(0.0, 0.0, w, h)
        return ctx
    }
}
