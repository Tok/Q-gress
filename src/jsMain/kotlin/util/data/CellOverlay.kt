package util.data

import system.grid.Pathfinding

/**
 * The 3D passability-overlay colour for a [Cell] (the [Pathfinding] heat ramp) — kept in the JS shell because
 * it reads the `system.grid.Pathfinding` heat range. Split out of `CellExt` so the pure neighbour check
 * [isPassableInAllDirections] can ride the commonMain lift. Used only by `system.display.PassabilityOverlay`.
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
