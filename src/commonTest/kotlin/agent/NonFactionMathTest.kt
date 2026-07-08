package agent

import util.data.Pos
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [NonFactionMath.opposingHalf] — the pure directional pick extracted from
 * `opposingOffscreenDestination` (PLAN non-functional track, phase B). It selects the off-map points whose
 * bearing from the centre most OPPOSES the NPC's, so NPCs walk clear across the field (the fix for the
 * round-field "everyone piles up north" bug) rather than to the nearest edge.
 */
class NonFactionMathTest {

    // Four off-map points around the origin: East, West, North, South.
    private val east = Pos(100, 0)
    private val west = Pos(-100, 0)
    private val north = Pos(0, 100)
    private val south = Pos(0, -100)
    private val all = listOf(east, west, north, south)

    @Test
    fun picksTheHalfOpposingTheNpcsBearing() {
        // Standing in the East → the opposing hemisphere is West only; the perpendicular North/South points
        // (dot 0) are dropped SYMMETRICALLY, so there's no tie-break skew toward one pole.
        val half = NonFactionMath.opposingHalf(all, from = east, cx = 0.0, cy = 0.0, nearCentre = 10.0)
        assertTrue(half.contains(west), "West most opposes an Eastern bearing")
        assertTrue(!half.contains(east), "never send the NPC back the way it came")
        assertTrue(!half.contains(north) && !half.contains(south), "perpendicular points aren't 'across' → excluded (no N/S skew)")
    }

    @Test
    fun nearTheCentreEveryDirectionIsFairGame() {
        // Within nearCentre of the middle there's no meaningful 'opposite' → return all candidates.
        val half = NonFactionMath.opposingHalf(all, from = Pos(1, 0), cx = 0.0, cy = 0.0, nearCentre = 10.0)
        assertEquals(all.toSet(), half.toSet(), "no directional bias near the centre")
    }

    @Test
    fun opposingHalfTargetsAreAlwaysAtLeastAQuarterCircleAway() {
        // A 12-point ring around the origin: from ANY point, every opposing-hemisphere target must be more than
        // 90° (a quarter circle) around the ring — so an NPC at an off-map target never picks a near-by one.
        val n = 12
        val ring = (0 until n).map { Pos((100 * cos(2 * PI * it / n)).toInt(), (100 * sin(2 * PI * it / n)).toInt()) }
        ring.forEach { from ->
            NonFactionMath.opposingHalf(ring, from, cx = 0.0, cy = 0.0, nearCentre = 1.0).forEach { to ->
                // centre is (0,0): the dot of the two bearings ≤ 0 ⇔ they're AT LEAST 90° (a quarter circle) apart
                val dot = from.x.toDouble() * to.x + from.y.toDouble() * to.y
                assertTrue(dot <= 0.0, "opposing target is ≥90° from the source (dot=$dot)")
            }
        }
    }

    // ---- ringDestinations: street-aware off-map ring placement ----

    // Index i of the returned fractional slots maps to angle 2π·i/N; the arc it falls in is floor-ish.
    private fun allTrue(n: Int) = BooleanArray(n) { true }

    @Test
    fun anAllWalkableRingSpreadsTheBudgetEvenly() {
        val slots = NonFactionMath.ringDestinations(allTrue(100), targetCount = 10, minArcSamples = 2)
        assertEquals(10, slots.size, "an unobstructed ring uses the full budget")
        assertTrue(slots.all { it in 0.0..100.0 }, "slots are fractional sample indices in [0, N)")
        val sorted = slots.sorted()
        val gaps = sorted.indices.map { (sorted[(it + 1) % sorted.size] - sorted[it] + 100.0) % 100.0 }
        assertTrue(gaps.all { it in 8.0..12.0 }, "10 points around 100 samples ⇒ ~even 10-sample spacing (was $gaps)")
    }

    @Test
    fun destinationsOnlyLandOnWalkableArcs() {
        // Left half walkable, right half a wall.
        val placeable = BooleanArray(100) { it < 50 }
        val slots = NonFactionMath.ringDestinations(placeable, targetCount = 10, minArcSamples = 2)
        assertEquals(10, slots.size, "the whole budget still fits on the one walkable arc")
        assertTrue(slots.all { it in 0.0..50.0 }, "no destination lands on the blocked half (slots=$slots)")
    }

    @Test
    fun oneDestinationInAnArcSitsAtItsCentre() {
        // Two thin, equal streets far apart; a budget of 2 ⇒ exactly one centred point in each.
        val placeable = BooleanArray(100) { it in 10..14 || it in 60..64 }
        val slots = NonFactionMath.ringDestinations(placeable, targetCount = 2, minArcSamples = 2).sorted()
        assertEquals(2, slots.size, "one per street")
        assertEquals(12.5, slots[0], 0.001, "centred in the [10,14] street (start 10 + half of width 5)")
        assertEquals(62.5, slots[1], 0.001, "centred in the [60,64] street")
    }

    @Test
    fun twoDestinationsInAnArcAreCentredAndSpread() {
        val placeable = BooleanArray(100) { it < 20 } // one 20-wide arc
        val slots = NonFactionMath.ringDestinations(placeable, targetCount = 2, minArcSamples = 2).sorted()
        assertEquals(2, slots.size)
        assertEquals(5.0, slots[0], 0.001, "first at 1/4 into the arc")
        assertEquals(15.0, slots[1], 0.001, "second at 3/4 into the arc (both centred, not on the edges)")
    }

    @Test
    fun aWiderStreetHostsProportionallyMoreDestinations() {
        // A 30-wide arc and a 10-wide arc, budget 8 ⇒ 6 and 2 by width.
        val placeable = BooleanArray(100) { it < 30 || it in 50..59 }
        val slots = NonFactionMath.ringDestinations(placeable, targetCount = 8, minArcSamples = 2)
        val wide = slots.count { it < 30 }
        val thin = slots.count { it in 50.0..60.0 }
        assertEquals(8, slots.size, "the full budget is placed")
        assertEquals(6, wide, "the 30-wide street gets ~3× the thin one")
        assertEquals(2, thin, "the 10-wide street gets the remaining share")
    }

    @Test
    fun sliverArcsBelowTheMinWidthAreIgnored() {
        // A 1-sample sliver (diagonal artefact) plus a real 20-wide street; the sliver is dropped.
        val placeable = BooleanArray(100) { it == 40 || it in 70..89 }
        val slots = NonFactionMath.ringDestinations(placeable, targetCount = 5, minArcSamples = 2)
        assertEquals(5, slots.size)
        assertTrue(slots.all { it in 70.0..90.0 }, "everything lands on the real street, none on the sliver at 40")
    }

    @Test
    fun aFullyBlockedRingYieldsNothing() {
        assertTrue(
            NonFactionMath.ringDestinations(
                BooleanArray(100) {
                    false
                },
                10,
                2,
            ).isEmpty(),
            "no walkable ground ⇒ empty (caller falls back)",
        )
    }

    @Test
    fun aWalkableArcAcrossTheSeamIsOneStreet() {
        // Walkable wraps the 0/N seam: [95,99] ∪ [0,4]. It must be treated as ONE 10-wide arc, centred at the
        // seam (≈ index 0), not two half-streets.
        val placeable = BooleanArray(100) { it >= 95 || it < 5 }
        val slots = NonFactionMath.ringDestinations(placeable, targetCount = 1, minArcSamples = 2)
        assertEquals(1, slots.size, "one street across the seam ⇒ one destination")
        val idx = slots.first()
        val nearSeam = idx >= 99.0 || idx <= 1.0 // centre of [95..104 mod 100] is index 100 ≡ 0
        assertTrue(nearSeam, "the single destination is centred on the seam (was $idx)")
    }

    @Test
    fun laneOffsetsFanNpcsOutSymmetricallyAndStayBounded() {
        val lanes = (0 until 7).map { NonFactionMath.laneOffset(it) }
        assertTrue(lanes.all { it in -0.0301..0.0301 }, "every lane stays within the tiny ± bound (gentle, penalty-obeying)")
        assertTrue(lanes.toSet().size >= 5, "different NPC ids land in different lanes (not single-file)")
        assertTrue(lanes.min() < 0.0 && lanes.max() > 0.0, "lanes fan out to BOTH sides of the heading")
        assertEquals(NonFactionMath.laneOffset(3), NonFactionMath.laneOffset(3 + 7), "the lane is stable per id (mod buckets)")
    }
}
