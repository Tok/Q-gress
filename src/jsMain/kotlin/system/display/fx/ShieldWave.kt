package system.display.fx

/**
 * Per-portal "energy absorbed" ripple state for shield bubbles. When an XMP/ultra-strike detonates
 * near a shielded portal that survives, [hit] starts a short ripple; [amplitudeFor] returns the
 * decaying envelope (1→0) the [ShieldShader] reads each frame so the shell waves like liquid, then
 * settles. Keyed by portal id; entries self-expire. (If the shield is destroyed the bubble simply
 * vanishes on the next sync, so a stale entry is harmless.)
 */
object ShieldWave {
    private const val DURATION = 1.7 // seconds a ripple lasts
    private val active = mutableMapOf<String, Double>() // portal id → end time (sim-clock seconds)

    fun hit(portalId: String, now: Double) {
        active[portalId] = now + DURATION
    }

    /** Envelope (1→0, ease-out) for [portalId] at [now]; 0 (and forgets it) once elapsed. */
    fun amplitudeFor(portalId: String, now: Double): Double {
        val end = active[portalId] ?: return 0.0
        if (now >= end) {
            active.remove(portalId)
            return 0.0
        }
        val t = (end - now) / DURATION // 1 → 0
        return t * t // ease-out
    }
}
