package util.data

import World

/**
 * Neighbour helper for [Cell] kept out of the pure commonMain data class because it reads `World.grid`. An
 * extension function, so call sites are unchanged. (The 3D passability-overlay colour lives in the JS-only
 * [overlayColor] — see `CellOverlay`.)
 */

/** True when this cell and all 8 of its neighbours are passable (a cell well clear of any wall/water). */
fun Cell.isPassableInAllDirections(): Boolean {
    fun passable(dx: Int, dy: Int) = World.grid[Pos(position.x + dx, position.y + dy)]?.isPassable ?: false
    return isPassable &&
        passable(-1, 0) &&
        passable(1, 0) &&
        passable(0, -1) &&
        passable(0, 1) &&
        passable(-1, -1) &&
        passable(1, -1) &&
        passable(-1, 1) &&
        passable(1, 1)
}
