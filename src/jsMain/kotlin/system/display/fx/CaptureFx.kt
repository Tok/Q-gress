package system.display.fx
import system.display.Scene3D

/**
 * Capture animation state: detects when a portal's faction colour changes (a capture or a
 * neutralization) by diffing against the previous [Scene3D] sync, and drives the "old orb shatters,
 * new orb grows in" effect. Split out of [Scene3D] (size limit). Scene3D does the actual shatter
 * (via the [check] callback) and multiplies the orb scale by [reformFactor] so it pops back in.
 *
 * The portal stays standing — only the glass orb re-skins — so a newly-spawned neutral (white) portal
 * that's captured right away just shatters white and re-grows in the faction colour.
 *
 * A **virus flip** ([recolorWithoutShatter]) is the gentle case: no shatter, no pop — the orb just
 * morphs from the old faction colour to the new one over [RECOLOR_S] (Ingress flips instantly; we
 * stretch the visual so it reads as a deliberate re-skin). Scene3D lerps the orb colour using
 * [recolorFrom] + [recolorT].
 */
object CaptureFx {
    private const val REFORM_S = 0.45 // seconds for the new orb to pop in
    private const val RECOLOR_S = 1.0 // seconds for a virus-flip orb to morph to the new faction colour

    private val prevColor = mutableMapOf<String, String>()
    private val reformStart = mutableMapOf<String, Double>() // id → ms when the new orb started growing
    private val silentRecolor = mutableSetOf<String>() // ids whose NEXT colour change morphs instead of shattering
    private val recolorFrom = mutableMapOf<String, String>() // id → old colour while a virus-flip morph runs
    private val recolorStart = mutableMapOf<String, Double>() // id → ms when the morph began

    /**
     * On a colour change for [id], invoke [onShatter] with the OLD colour (Scene3D shatters that orb)
     * and start the re-grow. First sight (no previous colour) does nothing but record it. A virus flip
     * marked via [recolorWithoutShatter] instead starts a colour morph (no shatter, no pop).
     */
    fun check(id: String, color: String, onShatter: (oldColor: String) -> Unit) {
        val prev = prevColor[id]
        if (prev != null && prev != color) {
            if (silentRecolor.remove(id)) { // virus flip: morph old→new colour, glass stays intact
                recolorFrom[id] = prev
                recolorStart[id] = now()
            } else {
                onShatter(prev)
                reformStart[id] = now()
            }
        }
        prevColor[id] = color
    }

    /** Mark [id] so its next colour change (a virus flip) morphs the orb colour instead of shattering it. */
    fun recolorWithoutShatter(id: String) {
        silentRecolor.add(id)
    }

    /** The colour the orb is morphing FROM during a virus flip, or null if it isn't flipping. */
    fun recolorFrom(id: String): String? = recolorFrom[id]

    /** Eased 0..1 progress of the virus-flip colour morph (1.0 = done / not flipping; self-clears when done). */
    fun recolorT(id: String): Double {
        val t = recolorStart[id] ?: return 1.0
        val e = (now() - t) / 1000.0
        if (e >= RECOLOR_S) {
            recolorStart.remove(id)
            recolorFrom.remove(id)
            return 1.0
        }
        val p = (e / RECOLOR_S).coerceIn(0.0, 1.0)
        return p * p * (3.0 - 2.0 * p) // smoothstep ease
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
        silentRecolor.remove(id)
        recolorFrom.remove(id)
        recolorStart.remove(id)
    }

    private fun now() = Scene3D.animMs() // sim-scaled clock so FX track sim speed
}
