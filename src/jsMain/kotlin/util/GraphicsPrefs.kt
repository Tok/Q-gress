package util

/**
 * Persists the rendering-quality toggles surfaced in the menu's **Graphics** group — visual-only knobs (no
 * gameplay effect) that let low-end machines dial detail down and high-end crank it. Mirrors the other
 * `*Prefs` stores: [load] runs at startup **before** the scene + menu build so both seed themselves from the
 * saved values, and each setter persists immediately.
 *
 * - [antialias] — MSAA on the MapLibre WebGL context (the three.js custom layer inherits it → smooth edges on
 *   links/poles/buildings). It's a context-creation option, so a change only takes effect on the next reload.
 * - [highShadows] — the larger shadow map (crisper edges) vs the cheaper half-resolution one (live).
 */
object GraphicsPrefs {
    private const val KEY = "qgress.graphics"

    var antialias = true
        private set
    var highShadows = true
        private set

    fun load() {
        val o = Prefs.read(KEY) ?: return
        (o.antialias as? Boolean)?.let { antialias = it }
        (o.highShadows as? Boolean)?.let { highShadows = it }
    }

    fun setAntialias(on: Boolean) {
        antialias = on
        save()
    }

    fun setHighShadows(on: Boolean) {
        highShadows = on
        save()
    }

    private fun save() = Prefs.save(KEY) {
        val o: dynamic = js("({})")
        o.antialias = antialias
        o.highShadows = highShadows
        o
    }
}
