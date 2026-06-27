package util.data

/**
 * A single passability-grid cell: its [position], whether it [isPassable], and the [movementPenalty]
 * (pathfinding heat) for crossing it. Pure data in the shared functional core (`commonMain`) so the grid
 * algorithms over it ([system.grid.GridConnectivity], [system.grid.GridFixture]) are JVM-unit-testable.
 * The 3D-overlay colouring and the neighbour-aware "passable in all directions" check are presentation /
 * World-coupled and live in jsMain (`CellExt`).
 */
data class Cell(val position: Pos, val isPassable: Boolean, val movementPenalty: Int) {
    override fun toString() = position.x.toString() + ":" + position.y.toString()
}
