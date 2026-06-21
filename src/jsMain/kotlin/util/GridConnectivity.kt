package util

import extension.Grid
import util.data.Cell
import util.data.Pos

/**
 * Pure grid-connectivity helpers (functional core — no DOM/WebGL, unit-tested in Node).
 *
 * The passability [Grid] can contain **closed-off pockets**: passable cells fully walled in by
 * impassable ones (buildings/water), unreachable from the rest of the map. Since the off-screen
 * border ring is always passable and forms the single largest component (the "outside"),
 * [connectIslands] carves the shortest corridor from every other passable component to it, so no
 * area is ever sealed off. [walkability] gates fully-water / unplayable locations.
 */
object GridConnectivity {
    const val CORRIDOR_PENALTY = 80 // a carved connector: passable but rough (≈ off-screen ground)

    private fun neighbours(p: Pos) = listOf(
        Pos(p.x + 1.0, p.y),
        Pos(p.x - 1.0, p.y),
        Pos(p.x, p.y + 1.0),
        Pos(p.x, p.y - 1.0),
    )

    /** All 4-connected components of passable cells, each as the list of its cell positions. */
    fun components(grid: Grid): List<List<Pos>> {
        val seen = HashSet<Pos>()
        val comps = mutableListOf<List<Pos>>()
        grid.forEach { (start, cell) ->
            if (!cell.isPassable || start in seen) return@forEach
            val comp = mutableListOf<Pos>()
            val queue = ArrayDeque<Pos>()
            queue.add(start)
            seen.add(start)
            while (queue.isNotEmpty()) {
                val cur = queue.removeFirst()
                comp.add(cur)
                neighbours(cur).forEach { n ->
                    if (n !in seen && grid[n]?.isPassable == true) {
                        seen.add(n)
                        queue.add(n)
                    }
                }
            }
            comps.add(comp)
        }
        return comps
    }

    /** Fraction (0..1) of the on-screen cells in [0,w)×[0,h) that are passable (excludes the ring). */
    fun walkability(grid: Grid, w: Int, h: Int): Double {
        var passable = 0
        var total = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val cell = grid[Pos(x.toDouble(), y.toDouble())] ?: continue
                total++
                if (cell.isPassable) passable++
            }
        }
        return if (total == 0) 0.0 else passable.toDouble() / total
    }

    /**
     * Carve corridors so every passable island joins the largest component (the off-screen outside).
     * Returns a new grid; impassable cells along each shortest main→island path become passable
     * ([CORRIDOR_PENALTY]). A no-op when the grid is already fully connected.
     */
    fun connectIslands(grid: Grid): Grid {
        val comps = components(grid)
        if (comps.size <= 1) return grid
        val main = comps.maxByOrNull { it.size } ?: return grid
        val mainSet = HashSet(main)
        val islandOf = HashMap<Pos, Int>()
        comps.forEachIndexed { i, c -> if (c !== main) c.forEach { islandOf[it] = i } }
        val remaining = comps.indices.filterTo(HashSet()) { comps[it] !== main }

        val result = grid.toMutableMap()
        val parent = HashMap<Pos, Pos>()
        val visited = HashSet(mainSet)
        val queue = ArrayDeque(main) // flood outward from the whole outside region
        while (queue.isNotEmpty() && remaining.isNotEmpty()) {
            val cur = queue.removeFirst()
            val island = islandOf[cur]
            if (island != null && remaining.remove(island)) carvePath(result, parent, mainSet, cur)
            neighbours(cur).forEach { n ->
                if (n in result && n !in visited) {
                    visited.add(n)
                    parent[n] = cur
                    queue.add(n)
                }
            }
        }
        return result
    }

    /** Walk the BFS parent chain from [from] back to the main region, making impassable cells passable. */
    private fun carvePath(result: MutableMap<Pos, Cell>, parent: Map<Pos, Pos>, mainSet: Set<Pos>, from: Pos) {
        var p: Pos? = from
        while (p != null && p !in mainSet) {
            val cell = result[p]
            if (cell != null && !cell.isPassable) result[p] = Cell(p, true, CORRIDOR_PENALTY)
            p = parent[p]
        }
    }
}
