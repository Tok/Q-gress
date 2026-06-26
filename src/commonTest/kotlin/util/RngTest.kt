package util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Characterization tests (PLAN non-functional track, phase A) for [Rng]'s foundational, pure primitives: the
 * seedable mulberry32 PRNG (the game's ONLY randomness source) and [Rng.select], the weighted-random picker
 * that drives every AI choice. These pin reproducibility + the selection contract before the refactor.
 */
class RngTest {

    // --- the seeded PRNG -----------------------------------------------------

    @Test
    fun sameSeedReproducesTheSameSequence() {
        Rng.seed(42)
        val first = (1..10).map { Rng.random() }
        Rng.seed(42)
        val second = (1..10).map { Rng.random() }
        assertEquals(first, second, "a re-seed replays the identical stream — the basis of shareable worlds")
    }

    @Test
    fun differentSeedsDiverge() {
        Rng.seed(1)
        val a = (1..10).map { Rng.random() }
        Rng.seed(2)
        val b = (1..10).map { Rng.random() }
        assertTrue(a != b, "distinct seeds produce distinct streams")
    }

    @Test
    fun randomStaysInTheUnitInterval() {
        Rng.seed(123)
        assertTrue((1..1000).all { Rng.random() in 0.0..1.0 }, "random() ∈ [0,1)")
    }

    @Test
    fun currentSeedReportsTheActiveSeed() {
        Rng.seed(7777)
        assertEquals(7777, Rng.currentSeed())
    }

    @Test
    fun randomIntStaysWithinTheInclusiveRange() {
        Rng.seed(55)
        assertTrue((1..1000).all { Rng.randomInt(3, 9) in 3..9 }, "randomInt(min,max) is inclusive on both ends")
    }

    @Test
    fun shuffleIsAPermutationOfTheInput() {
        Rng.seed(9)
        val input = (1..50).toList()
        val shuffled = Rng.shuffle(input)
        assertEquals(input.toSet(), shuffled.toSet(), "shuffle preserves the exact element set")
        assertEquals(input.size, shuffled.size, "shuffle preserves the count (no drops/dupes)")
    }

    // --- the weighted picker -------------------------------------------------

    @Test
    fun selectReturnsTheDefaultOnAnEmptyList() {
        Rng.seed(1)
        assertEquals("fallback", Rng.select(emptyList(), "fallback"))
    }

    @Test
    fun selectReturnsTheDefaultWhenAllWeightsAreNonPositive() {
        Rng.seed(1)
        val options = listOf(0.0 to "a", -1.0 to "b", -0.5 to "c")
        assertEquals("fallback", Rng.select(options, "fallback"), "ineligible (≤0) options never win — fall back")
    }

    @Test
    fun selectAlwaysPicksTheOnlyPositiveOption() {
        // Zero/negative siblings are filtered out, so the single positive entry is the certain winner.
        repeat(20) { i ->
            Rng.seed(i)
            val options = listOf(0.0 to "zero", -3.0 to "neg", 2.5 to "winner")
            assertEquals("winner", Rng.select(options, "default"))
        }
    }

    @Test
    fun selectRoughlyHonoursTheWeightProportions() {
        // A heavily-weighted option should dominate the draws over a large seeded sample.
        Rng.seed(2024)
        val counts = mutableMapOf("rare" to 0, "common" to 0)
        repeat(4000) {
            val pick = Rng.select(listOf(1.0 to "rare", 9.0 to "common"), "default")
            counts[pick] = (counts.getValue(pick)) + 1
        }
        assertTrue(counts.getValue("common") > counts.getValue("rare") * 3, "9:1 weight clearly favours 'common'")
        assertTrue(counts.getValue("rare") > 0, "the rare option still wins occasionally")
    }
}
