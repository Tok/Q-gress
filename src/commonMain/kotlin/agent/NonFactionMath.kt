package agent

import util.data.Pos
import kotlin.math.sqrt

/**
 * Pure NPC-movement math, extracted from [NonFaction] (which supplies the live field geometry + RNG) into the
 * shared functional core (`commonMain`). No `World` coupling — JVM-unit-tested + Kover-covered.
 */
object NonFactionMath {
    private const val LANE_BUCKETS = 7
    private const val MAX_LANE_OFFSET = 0.45 // ± this rotates each NPC's heading (~24°) into its own lane

    /**
     * A stable per-NPC lateral "lane" in [-[MAX_LANE_OFFSET], +[MAX_LANE_OFFSET]], spread across [LANE_BUCKETS]
     * buckets by [id]. Rotating an NPC's travel heading by this fans a SHARED flow-field stream (many NPCs
     * routing to the same few off-map destinations follow the identical field) into a ribbon, so they spread
     * out instead of walking single-file. Deterministic (no RNG) so it's stable across ticks for a given NPC.
     */
    fun laneOffset(id: Int): Double {
        val bucket = ((id % LANE_BUCKETS) + LANE_BUCKETS) % LANE_BUCKETS // non-negative even if id is odd/negative
        return (bucket / (LANE_BUCKETS - 1.0) - 0.5) * 2.0 * MAX_LANE_OFFSET
    }

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
