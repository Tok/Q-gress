package agent

import util.data.Pos
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
     * Choose off-map ring destinations that land on WALKABLE arcs only (streets, not buildings). [placeable]
     * is the ring sampled at N even angles (index i ⇔ angle 2π·i/N; `true` = walkable ground). Contiguous
     * walkable samples form an **arc** (a street crossing the ring, or an off-screen gap in the round-arena
     * mask).
     *
     * Points are placed at a constant angular **density** — [targetCount] over the WHOLE circle — so each arc
     * gets its proportional share (min 1, so any real street still gets one centred target) and the blocked
     * spans simply stay empty. We deliberately do NOT re-pack the whole budget into the few open arcs: that
     * concentrated e.g. 20/4 points into each narrow poke of the round mask → four tight clusters (a "cross").
     * Constant density instead reproduces the old even spread on an open ring while still centring on streets
     * in a city. Within an arc the k points are centred (1 ⇒ midpoint, k ⇒ k evenly spaced). Arcs narrower
     * than [minArcSamples] are ignored as noise (single-cell diagonal artefacts) unless that drops them all.
     * Total is capped at [targetCount] (widest arcs kept). Returns fractional sample indices in `[0, N)` (the
     * caller maps each to an angle); empty when nothing is walkable — the caller then falls back to a raw even
     * ring. No World/RNG coupling → JVM-unit-tested.
     */
    fun ringDestinations(placeable: BooleanArray, targetCount: Int, minArcSamples: Int): List<Double> {
        val n = placeable.size
        if (n == 0 || targetCount <= 0) return emptyList()
        val allArcs = walkableArcs(placeable, n)
        if (allArcs.isEmpty()) return emptyList()
        val arcs = allArcs.filter { it.second >= minArcSamples }.ifEmpty { allArcs }
        val density = targetCount.toDouble() / n // points per sample over the FULL ring (even angular density)
        val wanted = arcs.map { maxOf(1, (it.second * density).roundToInt()) }
        val counts = capToBudget(arcs, wanted, targetCount)
        val result = mutableListOf<Double>()
        arcs.forEachIndexed { i, (start, len) ->
            val k = counts[i]
            for (j in 0 until k) {
                result.add((start + (j + 0.5) / k * len) % n) // centred: 1 ⇒ midpoint, k ⇒ evenly spaced
            }
        }
        return result
    }

    /**
     * Contiguous runs of walkable samples around the circular ring, each as `(startIndex, lengthInSamples)`.
     * Scanning starts at a blocked sample so no run straddles the array seam; an all-walkable ring is one
     * full arc `(0, n)`; a fully blocked ring is no arcs.
     */
    private fun walkableArcs(placeable: BooleanArray, n: Int): List<Pair<Int, Int>> {
        if (placeable.none { it }) return emptyList()
        val firstBlocked = (0 until n).firstOrNull { !placeable[it] } ?: return listOf(0 to n) // all walkable
        val arcs = mutableListOf<Pair<Int, Int>>()
        var runStart = -1
        var runLen = 0
        for (i in 0 until n) {
            val idx = (firstBlocked + i) % n
            if (placeable[idx]) {
                if (runLen == 0) runStart = idx
                runLen++
            } else if (runLen > 0) {
                arcs.add(runStart to runLen)
                runLen = 0
            }
        }
        if (runLen > 0) arcs.add(runStart to runLen)
        return arcs
    }

    /**
     * Bound the total destinations to [budget] (flow-field cost) when the min-1-per-arc floor overshoots it
     * (a very fragmented ring with many arcs). Keeps the WIDEST arcs' shares and drops the thinnest — a no-op
     * when the density counts already fit.
     */
    private fun capToBudget(arcs: List<Pair<Int, Int>>, wanted: List<Int>, budget: Int): IntArray {
        if (wanted.sum() <= budget) return wanted.toIntArray()
        val counts = IntArray(arcs.size)
        var left = budget
        for (idx in arcs.indices.sortedByDescending { arcs[it].second }) {
            if (left <= 0) break
            counts[idx] = minOf(wanted[idx], left)
            left -= counts[idx]
        }
        return counts
    }
}
