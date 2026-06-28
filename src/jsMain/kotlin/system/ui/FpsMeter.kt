package system.ui

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.addClass
import org.w3c.dom.HTMLElement
import util.Debug
import util.Prefs

/**
 * FPS / frame-time readout (PLAN phase D — establish a baseline). Runs its own `requestAnimationFrame` loop,
 * computing a rolling FPS once per second (independent of the sim tick, so it measures the real display/render
 * rate, not the sim speed). Two independent consumers:
 *  - **On-screen display** — a small Coda readout in the top-right (under the volume widget), toggled at runtime
 *    from the menu ([setDisplay]); on by default and persisted across sessions ([PREF_KEY]).
 *  - **Debug capture** — the `[perf] fps=… frame=…ms` console logs the headless CDP profiler scrapes, gated by
 *    `?debug` (unchanged). The menu toggle controls ONLY the display, never this capture.
 * The rAF loop runs only while at least one consumer is active, and re-baselines when it (re)starts.
 */
object FpsMeter {
    private const val WINDOW_MS = 1000.0 // recompute the average once per second
    private const val PREF_KEY = "fpsDisplay" // localStorage key for the show/hide state
    private var last = 0.0
    private var frames = 0
    private var accumMs = 0.0
    private var readout: HTMLElement? = null
    private var looping = false

    /** Whether the on-screen readout is currently shown (menu checkbox state). On by default — the FPS readout
     *  is useful at a glance and the menu checkbox can hide it — and persisted across sessions ([PREF_KEY]). */
    var displayEnabled = Prefs.read(PREF_KEY)?.unsafeCast<Boolean>() ?: true
        private set

    /** Create the readout and start measuring if it's shown by default or `?debug` capture is on. Call once
     *  the HUD is ready. */
    fun start() {
        ensureReadout()
        if (Debug.enabled || displayEnabled) ensureLoop()
    }

    /** Menu toggle: show/hide the on-screen FPS readout at runtime (persisted) — independent of the `?debug`
     *  console capture. */
    fun setDisplay(on: Boolean) {
        displayEnabled = on
        Prefs.save(PREF_KEY) { on }
        ensureReadout()
        readout?.style?.display = if (on) "block" else "none"
        if (on) ensureLoop()
    }

    private fun ensureReadout() {
        if (readout != null || document.body == null) return
        readout = (document.createElement("div") as HTMLElement).also {
            it.id = "fpsReadout"
            it.addClass("fpsReadout") // Coda, top-right under the volume widget, no glass pane (see CSS)
            it.style.display = if (displayEnabled) "block" else "none"
            document.body?.appendChild(it)
        }
    }

    private fun ensureLoop() {
        if (looping) return
        looping = true
        last = 0.0 // re-baseline so the first interval after a (re)start isn't a huge gap
        frames = 0
        accumMs = 0.0
        loop()
    }

    private fun loop() {
        window.requestAnimationFrame { t ->
            if (last > 0.0) {
                accumMs += t - last
                frames++
                if (accumMs >= WINDOW_MS) {
                    val fps = frames * 1000.0 / accumMs
                    val frameMs = accumMs / frames
                    if (displayEnabled) readout?.textContent = "${fps.toInt()} FPS"
                    if (Debug.enabled) console.log("[perf] fps ${fps.toInt()} · frame ${frameMs.asTenths()}ms")
                    frames = 0
                    accumMs = 0.0
                }
            }
            last = t
            // Keep ticking only while something needs us; otherwise idle (re-armed by start()/setDisplay()).
            if (displayEnabled || Debug.enabled) loop() else looping = false
        }
    }

    private fun Double.asTenths(): String {
        val tenths = (this * 10).toInt()
        return "${tenths / 10}.${tenths % 10}"
    }
}
