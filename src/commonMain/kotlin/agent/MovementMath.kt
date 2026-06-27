package agent

import util.data.Complex
import util.data.Pos

/**
 * Pure agent-steering vector math, extracted from [Movement] into the shared functional core (`commonMain`).
 * [Movement] keeps the World-coupled target selection; these are the side-effect-free heading/velocity
 * computations it delegates to, so they're JVM-unit-tested + Kover-covered.
 */
object MovementMath {
    /**
     * Unit-magnitude heading from [from] to [to], or [Complex.ZERO] when they coincide. Zero (rather than a
     * NaN-normalised vector) so a not-yet-computed flow field leaves the agent momentarily still instead of
     * broken — it keeps moving once the field lands, and never stalls on a degenerate vector.
     */
    fun headingTo(from: Pos, to: Pos): Complex {
        val raw = Complex(to.x - from.x, to.y - from.y)
        return if (raw == Complex.ZERO) Complex.ZERO else raw.copyWithNewMagnitude(1.0)
    }

    /**
     * Integrate [force] into [velocity], capped at speed [limit]. A zero force gets a random nudge (so a
     * stalled agent keeps drifting and re-samples the field) — the only non-deterministic part (seeded
     * [util.Rng]).
     */
    fun move(velocity: Complex, force: Complex, limit: Double): Complex {
        val actualForce = if (force != Complex.ZERO) force else Complex.random()
        val newVelo = velocity + actualForce
        return if (newVelo.mag <= limit) newVelo else newVelo.copyWithNewMagnitude(limit)
    }
}
