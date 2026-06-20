package util.data

import config.Constants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoordsTest {

    @Test
    fun distance() {
        val from = Pos(0, 0)
        val to = Pos(1, 1)
        assertEquals(Constants.sqrt2, from.distanceTo(to))
    }

    @Test
    fun countSurroundingAtCenter() =
        assertEquals(8, Pos(1, 1).surrounding(3, 3).size)

    @Test
    fun countSurroundingAtBorder() {
        assertEquals(5, Pos(1, 0).surrounding(3, 3).size)
        assertEquals(5, Pos(0, 1).surrounding(3, 3).size)
        assertEquals(5, Pos(1, 2).surrounding(3, 3).size)
        assertEquals(5, Pos(2, 1).surrounding(3, 3).size)
    }

    @Test
    fun countSurroundingAtEdges() {
        assertEquals(3, Pos(0, 0).surrounding(3, 3).size)
        assertEquals(3, Pos(0, 2).surrounding(3, 3).size)
        assertEquals(3, Pos(2, 0).surrounding(3, 3).size)
        assertEquals(3, Pos(2, 2).surrounding(3, 3).size)
    }

    @Test
    fun randomNearPointsInRadius() {
        val pos = Pos(50, 50)
        val r = 30
        (0..1000).map { pos.randomNearPoint(r) }.forEach {
            assertTrue(it.distanceTo(pos) <= r)
        }
    }
}
