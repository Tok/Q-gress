package system.map

import config.Sim
import util.data.Cell
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the pure round-arena mask of the shadow→grid pipeline ([ShadowGridBuilder.maskToCircle]), extracted
 * from MapController (PLAN phase B SoC split). On-screen cells outside the inscribed circle are forced
 * impassable when the field is round; a rectangle field is left untouched.
 */
class ShadowGridBuilderTest {
    private val w = 10
    private val h = 10

    private fun fullPassableGrid(): Map<Pos, Cell> =
        (0 until w).flatMap { x -> (0 until h).map { y -> Pos(x, y) } }.associateWith { Cell(it, true, 50) }

    @AfterTest
    fun restore() {
        Sim.roundField = true // the default; tests below flip it
    }

    @Test
    fun roundFieldMasksCornersButKeepsTheCentre() {
        Sim.roundField = true
        val masked = ShadowGridBuilder.maskToCircle(fullPassableGrid(), w, h)
        // Inscribed circle: centre (5,5), r=5. The centre is well inside; the corners are at distance ~7.07.
        assertTrue(masked.getValue(Pos(5, 5)).isPassable, "the centre stays inside the circle")
        assertFalse(masked.getValue(Pos(0, 0)).isPassable, "a corner is outside the inscribed circle → masked")
        assertFalse(masked.getValue(Pos(9, 9)).isPassable, "the opposite corner is masked too")
    }

    @Test
    fun rectangleFieldLeavesEveryCellPassable() {
        Sim.roundField = false
        val masked = ShadowGridBuilder.maskToCircle(fullPassableGrid(), w, h)
        assertTrue(masked.values.all { it.isPassable }, "no masking when the field is a rectangle")
    }
}
