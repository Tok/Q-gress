package util.data

import system.grid.Pathfinding

/**
 * Presentation / neighbour helpers for [Cell] kept out of the pure commonMain data class: the 3D
 * passability-overlay colour (the [Pathfinding] heat ramp) and the neighbour-aware all-directions check
 * (reads `World.grid`). Extension functions, so call sites are unchanged. (The old `Cell.getColor()` was
 * dead — its only references were the unrelated `getColor()` on the item-level types — so it was dropped.)
 */

/**
 * Colour for the 3D passability overlay: blocked cells read as dark blocks, walkable cells as a
 * white(fast road)→grey(high-penalty ground) ramp. Semi-transparent so the map shows through.
 */
fun Cell.overlayColor(): String {
    if (!isPassable) return "rgba(0, 0, 0, 0.75)"
    val span = (Pathfinding.MAX_HEAT - Pathfinding.MIN_HEAT).toDouble()
    val t = ((movementPenalty - Pathfinding.MIN_HEAT) / span).coerceIn(0.0, 1.0)
    val v = (255 - t * 175).toInt() // 255 = road, ~80 = high-penalty ground
    return "rgba($v, $v, $v, 0.45)"
}

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
