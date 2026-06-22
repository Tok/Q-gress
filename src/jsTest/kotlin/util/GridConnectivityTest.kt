package util

import extension.Grid
import util.data.Cell
import util.data.Pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GridConnectivityTest {

    // Build a grid from ASCII rows: '.' = passable, '#' = impassable. Row index = y, char index = x.
    private fun grid(vararg rows: String): Grid = rows.flatMapIndexed { y, row ->
        row.mapIndexed { x, ch ->
            val pos = Pos(x.toDouble(), y.toDouble())
            pos to Cell(pos, ch == '.', 50)
        }
    }.toMap()

    @Test
    fun componentsCountsSeparatePassableBlobs() {
        val g = grid(
            ".#.",
            ".#.",
            ".#.",
        )
        assertEquals(2, GridConnectivity.components(g).size, "two columns split by a wall = 2 components")
    }

    @Test
    fun componentsMergesDiagonallyTouchingViaEdge() {
        // 4-connectivity only: a single L-shape is one component.
        val g = grid(
            "..#",
            "#.#",
            "#..",
        )
        assertEquals(1, GridConnectivity.components(g).size)
    }

    @Test
    fun walkabilityIsPassableFraction() {
        val g = grid(
            "..",
            "#.",
        )
        assertEquals(0.75, GridConnectivity.walkability(g, 2, 2), 1e-9)
        assertEquals(0.0, GridConnectivity.walkability(grid("##", "##"), 2, 2), 1e-9)
        assertEquals(1.0, GridConnectivity.walkability(grid("..", ".."), 2, 2), 1e-9)
    }

    @Test
    fun connectIslandsJoinsAnEnclosedPocket() {
        // Outer frame is the main component; the centre cell is walled off.
        val g = grid(
            ".....",
            ".###.",
            ".#.#.",
            ".###.",
            ".....",
        )
        assertEquals(2, GridConnectivity.components(g).size, "centre is isolated before")
        val connected = GridConnectivity.connectIslands(g)
        assertEquals(1, GridConnectivity.components(connected).size, "one corridor carved → fully connected")
        // The centre stays passable and at least one wall cell was opened to a corridor.
        assertTrue(connected.getValue(Pos(2.0, 2.0)).isPassable)
        val carved = connected.values.count { it.isPassable && it.movementPenalty == GridConnectivity.CORRIDOR_PENALTY }
        assertTrue(carved >= 1, "at least one wall cell carved into a corridor")
    }

    @Test
    fun connectIslandsIsNoOpWhenAlreadyConnected() {
        val g = grid("..", "..")
        assertEquals(g, GridConnectivity.connectIslands(g))
    }

    @Test
    fun onScreenComponentsSplitsRegionsJoinedOnlyViaTheRing() {
        // On-screen = [0,3)×[0,2) (rows 0–1). A wall column at x=1 splits the on-screen area into a
        // left and a right region; they connect ONLY through the off-screen rows (y≥2, all passable).
        val g = grid(
            ".#.",
            ".#.",
            "...", // off-screen (y=2)
            "...", // off-screen (y=3)
        )
        // Whole-grid: one component (left and right joined via the off-screen rows).
        assertEquals(1, GridConnectivity.components(g).size)
        // On-screen only: two regions (the off-screen detour is invisible to gameplay routing).
        assertEquals(2, GridConnectivity.onScreenComponents(g, 3, 2).size)
    }

    @Test
    fun reportFlagsOffScreenDetourAndHealthy() {
        val healthy = grid("...", "...", "...")
        val r = GridConnectivity.report(healthy, 3, 3)
        assertEquals(1, r.islands)
        assertEquals(1, r.onScreenIslands)
        assertTrue(r.isHealthy)

        // Two on-screen blobs split by an interior wall column → on-screen islands = 2, not healthy.
        val split = grid(".#.", ".#.", ".#.")
        val rs = GridConnectivity.report(split, 3, 3)
        assertEquals(2, rs.onScreenIslands)
        assertTrue(!rs.isHealthy)
    }

    @Test
    fun connectIslandsJoinsMultiplePockets() {
        // Two separate enclosed single-cell pockets, both must end connected.
        val g = grid(
            ".......",
            ".#####.",
            ".#.#.#.",
            ".#####.",
            ".......",
        )
        assertEquals(3, GridConnectivity.components(g).size)
        assertEquals(1, GridConnectivity.components(GridConnectivity.connectIslands(g)).size)
    }
}
