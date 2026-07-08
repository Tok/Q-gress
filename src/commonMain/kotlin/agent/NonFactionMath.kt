package agent

import util.data.Pos
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Pure NPC-movement math, extracted from [NonFaction] (which supplies the live field geometry + RNG) into the
 * shared functional core (`commonMain`). No `World` coupling — JVM-unit-tested + Kover-covered.
 */
object NonFactionMath {
    private const val LANE_BUCKETS = 7
    private const val MAX_LANE_OFFSET = 0.03 // ± this rotates each NPC's heading only ~1.7° into its own lane

    /**
     * A stable per-NPC lateral "lane" in [-[MAX_LANE_OFFSET], +[MAX_LANE_OFFSET]], spread across [LANE_BUCKETS]
     * buckets by [id]. Rotating an NPC's travel heading by this GENTLY fans a shared flow-field stream into a
     * ribbon so they don't walk single-file — kept tiny (~1.7° max) so NPCs still almost entirely follow the
     * flow field and its movement penalties (walking roads, avoiding water/buildings), which is the point of
     * having them. Deterministic (no RNG) so it's stable across ticks for a given NPC.
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

    /**
     * Place ~[targetCount] off-map destinations EVENLY spaced around the ring, but only where there's walkable
     * space. [placeable] is the ring sampled at N even angles (index i ⇔ angle 2π·i/N; `true` = walkable
     * ground). For each of [targetCount] evenly-spaced candidate angles we snap to the nearest walkable sample
     * within [maxNudgeSamples] — so a candidate landing on a building shifts onto the adjacent street — and
     * DROP it when there's no walkable ground within reach (a big blocked span, e.g. the round-arena moat
     * between the off-screen pokes; NPCs can't head there anyway). A [minGapSamples] spacing floor stops two
     * candidates snapping onto the same spot.
     *
     * This keeps the even spread of the old ring while avoiding houses — instead of segmenting into arcs and
     * re-packing the budget into the few open ones, which clustered them into a "cross". Returns fractional
     * sample indices in `[0, N)` (the caller maps each to an angle); empty when nothing is walkable — the
     * caller then falls back to a raw even ring. No World/RNG coupling → JVM-unit-tested.
     */
    fun ringDestinations(placeable: BooleanArray, targetCount: Int, maxNudgeSamples: Int, minGapSamples: Int): List<Double> {
        val n = placeable.size
        if (n == 0 || targetCount <= 0 || placeable.none { it }) return emptyList()
        val placed = mutableListOf<Double>()
        for (i in 0 until targetCount) {
            val ideal = i.toDouble() * n / targetCount // the evenly-spaced candidate
            val snapped = nearestWalkable(placeable, ideal, maxNudgeSamples) ?: continue // no street nearby → skip
            if (placed.none { circularGapSamples(it, snapped, n) < minGapSamples }) placed.add(snapped)
        }
        return placed
    }

    /** The walkable sample nearest [ideal] (searching out symmetrically up to [maxNudge] samples), or null if
     *  every sample in that window is blocked. Returned as a sample index (Double) the caller turns into an angle. */
    private fun nearestWalkable(placeable: BooleanArray, ideal: Double, maxNudge: Int): Double? {
        val n = placeable.size
        val base = ((ideal.roundToInt() % n) + n) % n
        for (d in 0..maxNudge) {
            val up = (base + d) % n
            if (placeable[up]) return up.toDouble()
            val down = ((base - d) % n + n) % n
            if (placeable[down]) return down.toDouble()
        }
        return null
    }

    /** Shortest gap between two sample indices around the circular ring of [n] samples. */
    private fun circularGapSamples(a: Double, b: Double, n: Int): Double {
        val diff = abs(a - b) % n
        return minOf(diff, n - diff)
    }
}
