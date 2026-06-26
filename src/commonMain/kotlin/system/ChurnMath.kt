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
}
