package system.grid

import util.data.Pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GridFixtureTest {

    @Test
    fun rleRoundTrips() {
        val cases = listOf(
            listOf(false, false, true, true, true, false),
            listOf(true), // starts passable → leading 0 run
            listOf(false),
            List(50) { it % 3 == 0 },
            emptyList(),
        )
        cases.forEach { bits ->
            val rle = GridFixture.rleEncode(bits)
            assertEquals(bits, GridFixture.rleDecode(rle, bits.size), "round-trip for $rle")
        }
    }

    @Test
    fun rleStartingPassableEmitsLeadingZero() {
        assertEquals("0,3", GridFixture.rleEncode(listOf(true, true, true)))
    }

    @Test
    fun toGridReconstructsOnScreenAndPassableRing() {
        // 3×2 on-screen: row0 = [passable, impassable, passable], row1 = all passable.
        val onScreen = listOf(true, false, true, true, true, true)
        val fx = GridFixture("TEST", 3, 2, 1, GridFixture.rleEncode(onScreen))
        val grid = fx.toGrid()

        // on-screen cells match
        assertEquals(true, grid.getValue(Pos(0.0, 0.0)).isPassable)
        assertEquals(false, grid.getValue(Pos(1.0, 0.0)).isPassable)
        assertEquals(true, grid.getValue(Pos(2.0, 0.0)).isPassable)
        // off-screen ring is passable
        assertTrue(grid.getValue(Pos(-1.0, -1.0)).isPassable)
        assertTrue(grid.getValue(Pos(3.0, 2.0)).isPassable)
        // full grid spans [-off, w+off) × [-off, h+off)
        assertEquals((3 + 2) * (2 + 2), grid.size)
    }

    @Test
    fun fromGridThenToGridPreservesOnScreenPassability() {
        // Build a fixture from a grid, reconstruct, and confirm on-screen passability survives.
        val w = 4
        val h = 3
        val src = (0 until h).flatMap { y ->
            (0 until w).map { x ->
                val pos = Pos(x.toDouble(), y.toDouble())
                pos to util.data.Cell(pos, (x + y) % 2 == 0, 50)
            }
        }.toMap()
        val fx = GridFixture.fromGrid("TEST", src, w, h, 2)
        val grid = fx.toGrid()
        for (y in 0 until h) {
            for (x in 0 until w) {
                assertEquals(
                    (x + y) % 2 == 0,
                    grid.getValue(Pos(x.toDouble(), y.toDouble())).isPassable,
                    "cell ($x,$y)",
                )
            }
        }
    }
}
