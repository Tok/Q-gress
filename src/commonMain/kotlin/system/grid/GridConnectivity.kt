package system.grid

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

    /** 4-connected components of the passable cells matching [include], each as its list of positions. */
    private fun componentsWhere(grid: Grid, include: (Pos) -> Boolean): List<List<Pos>> {
        val seen = HashSet<Pos>()
        val comps = mutableListOf<List<Pos>>()
        grid.forEach { (start, cell) ->
            if (!cell.isPassable || !include(start) || start in seen) return@forEach
            val comp = mutableListOf<Pos>()
            val queue = ArrayDeque<Pos>()
            queue.add(start)
            seen.add(start)
            while (queue.isNotEmpty()) {
                val cur = queue.removeFirst()
                comp.add(cur)
                neighbours(cur).forEach { n ->
                    if (n !in seen && include(n) && grid[n]?.isPassable == true) {
                        seen.add(n)
                        queue.add(n)
                    }
                }
            }
            comps.add(comp)
        }
        return comps
    }

    /** All 4-connected components of passable cells, each as the list of its cell positions. */
    fun components(grid: Grid): List<List<Pos>> = componentsWhere(grid) { true }

    private fun isOnScreen(p: Pos, w: Int, h: Int) = p.x >= 0 && p.y >= 0 && p.x < w && p.y < h

    /**
     * Components of passable cells **restricted to the on-screen area** [0,w)×[0,h) (the off-screen
     * border ring is excluded). More than one means some playable regions are reachable from each
     * other only by detouring through the off-screen ring — agents/NPCs then path the long way
     * around the map edge, which reads as aimless wandering even though the grid is "connected".
     */
    fun onScreenComponents(grid: Grid, w: Int, h: Int): List<List<Pos>> = componentsWhere(grid) { isOnScreen(it, w, h) }

    /**
     * Connectivity health of a (carved) grid. [islands] should be 1 after [connectIslands];
     * [onScreenIslands] > 1 flags the off-screen-detour hazard (see [onScreenComponents]).
     */
    data class ConnectivityReport(val islands: Int, val onScreenIslands: Int, val walkability: Double) {
        val isHealthy get() = islands <= 1 && onScreenIslands <= 1
    }

    /** Summarize a grid's connectivity. Run on the post-[connectIslands] grid for the live diagnostic. */
    fun report(grid: Grid, w: Int, h: Int) = ConnectivityReport(
        islands = components(grid).size,
        onScreenIslands = onScreenComponents(grid, w, h).size,
        walkability = walkability(grid, w, h),
    )

    const val MIN_WALKABILITY = 0.12 // below this the area is mostly water/blocked → unplayable

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
     * ([CORRIDOR_PENALTY]). A no-op when the grid is already fully connected. (Whole-grid only — for
     * the gameplay grid use the [w]/[h] overload, which also joins on-screen regions.)
     */
    fun connectIslands(grid: Grid): Grid = connectComponents(grid, components(grid)) { true }

    /**
     * Full gameplay connectivity: first seal every enclosed pocket to the outside (whole-grid), then
     * join the **on-screen play** regions to each other directly. The first pass alone leaves regions that
     * both touch the off-screen ring reachable only by detouring around the map edge ([onScreenComponents]
     * > 1) — agents path the long way and look stuck. The second pass carves on-screen corridors between
     * them, so afterwards every playable cell reaches every other without leaving the screen.
     *
     * [inCircle] marks the play area for a round field (default `{ true }` = the whole on-screen rectangle):
     * neither pass may flood/carve a corridor through an on-screen cell OUTSIDE it, because the caller masks
     * those impassable afterwards — a corridor riding one would be re-severed, re-fragmenting the grid (the
     * `UNHEALTHY` connectivity warning). Off-screen ring cells stay traversable (they're never masked), so
     * pass 1 still reaches in-circle pockets via the mid-edges where the circle meets the ring.
     */
    fun connectIslands(grid: Grid, w: Int, h: Int, inCircle: (Pos) -> Boolean = { true }): Grid {
        val sealed = connectComponents(grid, components(grid)) { !(isOnScreen(it, w, h) && !inCircle(it)) }
        val inPlay = { p: Pos -> isOnScreen(p, w, h) && inCircle(p) }
        return connectComponents(sealed, componentsWhere(sealed, inPlay), inPlay)
    }

    /**
     * Carve shortest corridors joining each component in [comps] to the largest, flooding only through
     * cells where [traversable] holds (so on-screen joins stay on-screen). Impassable cells on the
     * chosen paths become passable ([CORRIDOR_PENALTY]). No-op for ≤1 component.
     */
    private fun connectComponents(grid: Grid, comps: List<List<Pos>>, traversable: (Pos) -> Boolean): Grid {
        if (comps.size <= 1) return grid
        val main = comps.maxByOrNull { it.size } ?: return grid
        val mainSet = HashSet(main)
        val islandOf = HashMap<Pos, Int>()
        comps.forEachIndexed { i, c -> if (c !== main) c.forEach { islandOf[it] = i } }
        val remaining = comps.indices.filterTo(HashSet()) { comps[it] !== main }

        val result = grid.toMutableMap()
        val parent = HashMap<Pos, Pos>()
        val visited = HashSet(mainSet)
        val queue = ArrayDeque(main) // flood outward from the whole main region
        while (queue.isNotEmpty() && remaining.isNotEmpty()) {
            val cur = queue.removeFirst()
            val island = islandOf[cur]
            if (island != null && remaining.remove(island)) carvePath(result, parent, mainSet, cur)
            neighbours(cur).forEach { n ->
                if (n in result && n !in visited && traversable(n)) {
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
