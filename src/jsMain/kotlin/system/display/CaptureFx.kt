package system.display

/**
 * Capture animation state: detects when a portal's faction colour changes (a capture or a
 * neutralization) by diffing against the previous [Scene3D] sync, and drives the "old orb shatters,
 * new orb grows in" effect. Split out of [Scene3D] (size limit). Scene3D does the actual shatter
 * (via the [check] callback) and multiplies the orb scale by [reformFactor] so it pops back in.
 *
 * The portal stays standing — only the glass orb re-skins — so a newly-spawned neutral (white) portal
 * that's captured right away just shatters white and re-grows in the faction colour.
 */
object CaptureFx {
    private const val REFORM_S = 0.45 // seconds for the new orb to pop in

    private val prevColor = mutableMapOf<String, String>()
    private val reformStart = mutableMapOf<String, Double>() // id → ms when the new orb started growing

    /**
     * On a colour change for [id], invoke [onShatter] with the OLD colour (Scene3D shatters that orb)
     * and start the re-grow. First sight (no previous colour) does nothing but record it.
     */
    fun check(id: String, color: String, onShatter: (oldColor: String) -> Unit) {
        val prev = prevColor[id]
        if (prev != null && prev != color) {
            onShatter(prev)
            reformStart[id] = now()
        }
        prevColor[id] = color
    }

    /** Orb-scale multiplier for [id] while it re-grows after a capture (1.0 = not capturing). */
    fun reformFactor(id: String): Double {
        val t = reformStart[id] ?: return 1.0
        val e = (now() - t) / 1000.0
        if (e >= REFORM_S) {
            reformStart.remove(id)
            return 1.0
        }
        val p = (e / REFORM_S).coerceIn(0.0, 1.0)
        val c1 = 1.70158
        val x = p - 1.0
        return (1.0 + (c1 + 1.0) * x * x * x + c1 * x * x).coerceAtLeast(0.0) // easeOutBack pop
    }

    /** Forget a removed portal's state (called from the sync teardown). */
    fun forget(id: String) {
        prevColor.remove(id)
        reformStart.remove(id)
    }

    private fun now() = Scene3D.animMs() // sim-scaled clock so FX track sim speed
}
