package extension

import util.data.Complex
import util.data.Pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The flat [VectorField] flow-field store: the O(1) packed-index [VectorField.get] (in- and out-of-bounds),
 * the empty/non-empty checks, the [VectorField.all] predicate, the [VectorField.forEach] visit (recovers each
 * cell's position + vector), value equality / hashCode, and the shared [VectorField.EMPTY] sentinel.
 */
class VectorFieldCoverageTest {

    // A 2×2 field anchored at (10, 20): re/im laid out row-major (y-major, stride w).
    private fun sample() = VectorField(
        minX = 10,
        minY = 20,
        w = 2,
        h = 2,
        re = doubleArrayOf(1.0, 2.0, 3.0, 4.0),
        im = doubleArrayOf(5.0, 6.0, 7.0, 8.0),
    )

    @Test
    fun getReadsThePackedCellAndReturnsNullOutside() {
        val field = sample()
        assertEquals(Complex(1.0, 5.0), field[Pos(10, 20)], "origin cell (index 0)")
        assertEquals(Complex(2.0, 6.0), field[Pos(11, 20)], "next column (index 1)")
        assertEquals(Complex(3.0, 7.0), field[Pos(10, 21)], "next row (index w)")
        assertEquals(Complex(4.0, 8.0), field[Pos(11, 21)], "last cell (index 3)")
        assertNull(field[Pos(12, 20)], "x past the right edge is out")
        assertNull(field[Pos(9, 20)], "x before the left edge is out")
        assertNull(field[Pos(10, 22)], "y past the bottom edge is out")
        assertNull(field[Pos(10, 19)], "y before the top edge is out")
    }

    @Test
    fun emptyAndNonEmpty() {
        assertTrue(sample().isNotEmpty(), "a populated field is non-empty")
        assertFalse(sample().isEmpty(), "a populated field is not empty")
        assertTrue(VectorField.EMPTY.isEmpty(), "the sentinel is empty")
        assertFalse(VectorField.EMPTY.isNotEmpty(), "the sentinel is not non-empty")
        assertNull(VectorField.EMPTY[Pos(0, 0)], "EMPTY.get is null for any cell")
    }

    @Test
    fun allTestsEveryCell() {
        val field = sample() // every re is >= 1.0
        assertTrue(field.all { it.re >= 1.0 }, "all cells satisfy re >= 1")
        assertFalse(field.all { it.re >= 3.0 }, "not every cell has re >= 3")
    }

    @Test
    fun forEachVisitsEveryCellWithItsPosition() {
        val field = sample()
        val visited = mutableMapOf<Pos, Complex>()
        field.forEach { pos, vec -> visited[pos] = vec }
        assertEquals(4, visited.size, "all four cells are visited")
        assertEquals(Complex(1.0, 5.0), visited[Pos(10.0, 20.0)])
        assertEquals(Complex(4.0, 8.0), visited[Pos(11.0, 21.0)])
    }

    @Test
    fun equalsAndHashCodeAreValueBased() {
        assertEquals(sample(), sample(), "two fields with identical contents are equal")
        assertEquals(sample().hashCode(), sample().hashCode(), "equal fields share a hashCode")
        val shifted = VectorField(11, 20, 2, 2, doubleArrayOf(1.0, 2.0, 3.0, 4.0), doubleArrayOf(5.0, 6.0, 7.0, 8.0))
        assertNotEquals(sample(), shifted, "a different origin makes them unequal")
        assertNotEquals<Any?>(sample(), "not a field", "a non-VectorField is never equal")
    }
}
