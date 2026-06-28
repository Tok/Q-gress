package glyph

import util.Rng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GlyphTest {

    @Test
    fun everyGlyphHasASpokenName() {
        Glyph.values().forEach { assertTrue(it.spokenName.isNotBlank(), "$it needs a spoken name") }
        assertTrue(Glyph.values().size >= 130, "the active Ingress glyph set is ~135")
    }

    @Test
    fun spokenNamesAreUnique() {
        val names = Glyph.values().map { it.spokenName }
        assertEquals(names.size, names.toSet().size, "no two glyphs share a spoken name")
    }

    @Test
    fun randomSequenceIsDistinctAndSized() {
        Rng.seed(42)
        val seq = Glyph.randomSequence(Rng, 3)
        assertEquals(3, seq.size)
        assertEquals(3, seq.toSet().size, "a glyph sequence has no repeats")
    }

    @Test
    fun randomSequenceIsDeterministicUnderSeed() {
        Rng.seed(7)
        val a = Glyph.randomSequence(Rng, 3)
        Rng.seed(7)
        val b = Glyph.randomSequence(Rng, 3)
        assertEquals(a, b, "same seed → same sequence")
    }

    @Test
    fun sequenceClampsToThePool() {
        Rng.seed(1)
        assertEquals(Glyph.values().size, Glyph.randomSequence(Rng, 9999).size, "count is clamped to the set size")
    }
}
