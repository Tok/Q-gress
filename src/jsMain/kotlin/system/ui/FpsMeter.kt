package system.ui

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import util.Debug

/**
 * Per-frame FPS / frame-time readout (PLAN phase D — establish a baseline). Runs its own
 * `requestAnimationFrame` loop, computing a rolling FPS + average frame-time over a short window, shown in a
 * fixed corner overlay and logged to the console as `[perf] fps=… frame=…ms` (so the headless CDP profiler
 * captures it too). Gated by `?debug` — off and zero-cost in normal play. Independent of the sim tick, so it
 * measures the real display/render rate (3D scene + HUD), not the sim speed.
 */
object FpsMeter {
    private const val WINDOW_MS = 1000.0 // recompute the average once per second
    private var last = 0.0
    private var frames = 0
    private var accumMs = 0.0
    private var hud: HTMLElement? = null
    private var running = false

    /** Start the readout if `?debug` is on (idempotent). Call once the world is ready. */
    fun start() {
        if (running || !Debug.enabled) return
        running = true
        hud = (document.createElement("div") as HTMLElement).also {
            it.id = "fpsMeter"
            it.setAttribute(
                "style",
                "position:fixed;top:6px;left:50%;transform:translateX(-50%);z-index:99999;" +
                    "font:11px monospace;color:#a0a0a0;background:rgba(0,0,0,.55);padding:2px 8px;border-radius:6px;pointer-events:none",
            )
            document.body?.appendChild(it)
        }
        loop()
    }

    private fun loop() {
        window.requestAnimationFrame { t ->
            if (last > 0.0) {
                accumMs += t - last
                frames++
                if (accumMs >= WINDOW_MS) {
                    val fps = (frames * 1000.0 / accumMs)
                    val frameMs = accumMs / frames
                    val text = "fps ${fps.toInt()} · frame ${frameMs.asTenths()}ms"
                    hud?.textContent = text
                    console.log("[perf] $text")
                    frames = 0
                    accumMs = 0.0
                }
            }
            last = t
            loop()
        }
    }

    private fun Double.asTenths(): String {
        val tenths = (this * 10).toInt()
        return "${tenths / 10}.${tenths % 10}"
    }
}
