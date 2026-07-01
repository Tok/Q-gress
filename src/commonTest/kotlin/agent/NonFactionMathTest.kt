package agent

import util.data.Pos
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
    fun laneOffsetsFanNpcsOutSymmetricallyAndStayBounded() {
        val lanes = (0 until 7).map { NonFactionMath.laneOffset(it) }
        assertTrue(lanes.all { it in -0.45..0.45 }, "every lane stays within the ± bound")
        assertTrue(lanes.toSet().size >= 5, "different NPC ids land in different lanes (not single-file)")
        assertTrue(lanes.any { it < -0.1 } && lanes.any { it > 0.1 }, "lanes fan out to BOTH sides of the heading")
        assertEquals(NonFactionMath.laneOffset(3), NonFactionMath.laneOffset(3 + 7), "the lane is stable per id (mod buckets)")
    }
}
