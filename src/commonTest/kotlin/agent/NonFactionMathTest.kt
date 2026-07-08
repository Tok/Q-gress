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
                val dot = from.x * to.x + from.y * to.y
                assertTrue(dot <= 0.0, "opposing target is ≥90° from the source (dot=$dot)")
            }
        }
    }

    // ---- ringDestinations: even, street-aware off-map ring placement ----
    // Signature: ringDestinations(placeable, targetCount, maxNudgeSamples, minGapSamples) → fractional sample
    // indices in [0, N). Candidates are the targetCount evenly-spaced angles; each snaps to nearby walkable
    // ground, else KEEPS its even position; placed points stay ≥ minGap apart.

    private fun allTrue(n: Int) = BooleanArray(n) { true }

    private fun gaps(slots: List<Double>, n: Int): List<Double> {
        val s = slots.sorted()
        return s.indices.map { (s[(it + 1) % s.size] - s[it] + n) % n }
    }

    @Test
    fun anOpenRingIsEvenlySpaced() {
        val slots = NonFactionMath.ringDestinations(allTrue(100), targetCount = 10, maxNudgeSamples = 4, minGapSamples = 5)
        assertEquals(10, slots.size, "an unobstructed ring places every candidate")
        assertTrue(slots.all { it in 0.0..100.0 }, "slots are sample indices in [0, N)")
        assertTrue(gaps(slots, 100).all { it in 9.0..11.0 }, "candidates land at even ~10-sample spacing (was ${gaps(slots, 100)})")
    }

    @Test
    fun aCandidateOnABuildingSnapsOntoTheAdjacentStreet() {
        // Candidate #3 lands at sample 30; a small building blocks 28..32, so it shifts onto the nearest street.
        val placeable = BooleanArray(100) { it !in 28..32 }
        val slots = NonFactionMath.ringDestinations(placeable, targetCount = 10, maxNudgeSamples = 4, minGapSamples = 5)
        assertTrue(slots.all { placeable[it.toInt() % 100] }, "no destination sits on a building")
        val near30 = slots.filter { it in 25.0..35.0 }
        assertEquals(listOf(33.0), near30, "the blocked candidate snaps to the first walkable sample (33), not dropped")
    }

    @Test
    fun aCandidateWithNoNearbyStreetKeepsItsEvenPosition() {
        // A wide moat [40,60] swallows candidate #5 (sample 50); nothing walkable within the nudge window, so it
        // KEEPS its even angle rather than being dropped (dropping it would collapse the ring into a "cross").
        val placeable = BooleanArray(100) { it !in 40..60 }
        val slots = NonFactionMath.ringDestinations(placeable, targetCount = 10, maxNudgeSamples = 4, minGapSamples = 5)
        assertEquals(10, slots.size, "the even ring is kept whole — no candidate is dropped")
        assertTrue(50.0 in slots, "the moat candidate stays at its even position (50), not dropped")
        assertTrue(50.0 !in slots.filter { placeable[it.toInt() % 100] }, "…and it's the one point not on walkable ground")
    }

    @Test
    fun placedTargetsNeverBunchCloserThanTheMinGap() {
        // An open ring with a min-gap wider than the candidate spacing → alternating candidates are dropped so
        // nothing bunches. 20 candidates (spacing 5) with a min gap of 6 ⇒ every other one survives (gaps ≥ 6).
        val slots = NonFactionMath.ringDestinations(allTrue(100), targetCount = 20, maxNudgeSamples = 4, minGapSamples = 6)
        assertTrue(gaps(slots, 100).all { it >= 6.0 }, "no two placed targets are closer than the min gap (${gaps(slots, 100)})")
    }

    @Test
    fun aFullyBlockedRingKeepsAnEvenRing() {
        // Nothing walkable (e.g. bare unit grid) → every candidate keeps its even angle → a plain even ring.
        val slots = NonFactionMath.ringDestinations(BooleanArray(100) { false }, targetCount = 10, maxNudgeSamples = 4, minGapSamples = 5)
        assertEquals(10, slots.size, "an even ring, not empty")
        assertTrue(gaps(slots, 100).all { it in 9.0..11.0 }, "evenly spaced (${gaps(slots, 100)})")
    }

    @Test
    fun aCandidateSnapsAcrossTheSeam() {
        // Candidate #0 sits at sample 0 on a building [98..2]; the nearest street is at 3, across the 0/N seam.
        val placeable = BooleanArray(100) { it !in 0..2 && it !in 98..99 }
        val slots = NonFactionMath.ringDestinations(placeable, targetCount = 10, maxNudgeSamples = 5, minGapSamples = 5)
        assertTrue(slots.all { placeable[it.toInt() % 100] }, "the seam candidate snapped onto walkable ground, not a wall")
        assertTrue(slots.any { it >= 3.0 && it <= 5.0 } || slots.any { it >= 95.0 }, "it found the street just past the seam")
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
