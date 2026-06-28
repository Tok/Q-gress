package util

/**
 * Persists the rendering-quality toggles surfaced in the menu's **Graphics** group — visual-only knobs (no
 * gameplay effect) that let low-end machines dial detail down and high-end crank it. Mirrors the other
 * `*Prefs` stores: [load] runs at startup **before** the scene + menu build so both seed themselves from the
 * saved values, and each setter persists immediately.
 *
 * - [highShadows] — the larger shadow map (crisper edges) vs the cheaper half-resolution one (live).
 */
object GraphicsPrefs {
    private const val KEY = "qgress.graphics"

    var highShadows = true
        private set

    fun load() {
        val o = Prefs.read(KEY) ?: return
        (o.highShadows as? Boolean)?.let { highShadows = it }
    }

    fun setHighShadows(on: Boolean) {
        highShadows = on
        save()
    }

    private fun save() = Prefs.save(KEY) {
        val o: dynamic = js("({})")
        o.highShadows = highShadows
        o
    }
}
