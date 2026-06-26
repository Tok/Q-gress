package agent

import util.data.Pos
import kotlin.math.sqrt

/**
 * Pure NPC-movement math, extracted from [NonFaction] (which supplies the live field geometry + RNG) into the
 * shared functional core (`commonMain`). No `World` coupling — JVM-unit-tested + Kover-covered.
 */
object NonFactionMath {
    /**
     * From [all] off-map points, the half whose bearing from the centre ([cx], [cy]) most OPPOSES [from]'s
     * bearing (ascending dot product), so an NPC heads clear across the field instead of to the nearest edge.
     * Within [nearCentre] of the centre there's no meaningful opposite → return all.
     */
    fun opposingHalf(all: List<Pos>, from: Pos, cx: Double, cy: Double, nearCentre: Double): List<Pos> {
        val dx = from.x - cx
        val dy = from.y - cy
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < nearCentre) return all // anywhere is "across" from the middle → don't bias a direction
        val ux = dx / dist
        val uy = dy / dist
        return all.sortedBy { (it.x - cx) * ux + (it.y - cy) * uy }.take((all.size / 2).coerceAtLeast(1))
    }
}
