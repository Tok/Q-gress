package agent

import util.data.Pos
import kotlin.math.sqrt

/**
 * Pure NPC-movement math, extracted from [NonFaction] (which supplies the live field geometry + RNG) into the
 * shared functional core (`commonMain`). No `World` coupling — JVM-unit-tested + Kover-covered.
 */
object NonFactionMath {
    /**
     * From [all] off-map points, those whose bearing from the centre ([cx], [cy]) points AWAY from [from]
     * (negative dot product) — the strictly-opposing hemisphere — so an NPC heads clear across the field
     * instead of to the nearest edge. Within [nearCentre] of the centre there's no meaningful opposite → all.
     *
     * Uses a **threshold** (dot < 0), NOT a sort-then-take-half: the two points perpendicular to [from]'s
     * bearing sit exactly on dot 0 and are dropped symmetrically. The old `sortedBy{}.take(half)` instead
     * broke that perpendicular TIE by the (stable) input order of the ring, which is generated E→SE→S→…, so
     * the lower-indexed pole always won the tie and every pick skewed toward it — NPCs piling up N/S. Falls
     * back to [all] only in the degenerate case of no opposing point (never happens for a real ≥8-point ring).
     */
    fun opposingHalf(all: List<Pos>, from: Pos, cx: Double, cy: Double, nearCentre: Double): List<Pos> {
        val dx = from.x - cx
        val dy = from.y - cy
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < nearCentre) return all // anywhere is "across" from the middle → don't bias a direction
        val ux = dx / dist
        val uy = dy / dist
        return all.filter { (it.x - cx) * ux + (it.y - cy) * uy < 0.0 }.ifEmpty { all }
    }
}
