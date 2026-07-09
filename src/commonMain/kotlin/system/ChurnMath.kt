package system

/**
 * Pure density-churn math, extracted from [Cycle.managePortalDensity] into the shared functional core
 * (`commonMain`). The board's portal count converges toward a target: discovery dominates when sparse, evens
 * to ~1:1 at the target, and removal dominates above it. No `World` coupling — JVM-unit-tested + Kover-covered.
 */
object ChurnMath {
    /** The create/remove probabilities for one churn tick — see [churnChances]. */
    data class ChurnChances(val create: Double, val remove: Double)

    /**
     * At portal [count] against [target] (with churn [rate]), discovery fades and removal grows as the board
     * fills (they cross at the target). When there's no [hasSpace] to place a non-clipping portal, the would-be
     * discovery budget rolls into removal so a packed board thins out instead of stalling. Caller fires
     * `create` only when `hasSpace` (it's the removal top-up otherwise).
     */
    fun churnChances(count: Int, target: Int, rate: Double, hasSpace: Boolean): ChurnChances {
        val d = count / target.toDouble()
        val create = rate * (1.0 - d / 2.0).coerceIn(0.0, 1.0)
        val remove = rate * (d / 2.0).coerceIn(0.0, 1.0) + if (hasSpace) 0.0 else create
        return ChurnChances(create, remove)
    }

    /**
     * Rescale [chances] so churn is a rate **per unit of sim time**, not per roll: one full [periodTicks] of
     * elapsed world time is worth one whole [churnChances] roll.
     *
     * The caller samples this at irregular moments (an agent finishing a discovery stroll), so weighting each
     * roll by the time it represents makes the expected churn over any span depend only on that span — the
     * elapsed gaps sum to it, whatever the number of samples. Without this, churn tracked how *often* agents
     * happened to arrive: fixing wedged wanderers (which used to hold the discoverer slots forever) silently
     * doubled the board's churn. Sparse sampling clamps at one whole roll, so a long gap can't spike.
     */
    fun perElapsed(chances: ChurnChances, elapsedTicks: Int, periodTicks: Int): ChurnChances {
        val scale = (elapsedTicks.toDouble() / periodTicks).coerceIn(0.0, 1.0)
        return ChurnChances(chances.create * scale, chances.remove * scale)
    }
}
