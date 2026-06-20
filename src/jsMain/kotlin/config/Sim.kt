package config

/**
 * Simulation/grid extent — independent of the screen ([Dim], which drives the HUD). The world
 * covers [SCALE]× the screen linearly (SCALE² area) so the playable area spans the pitched
 * view, not just the top-down footprint. The Pos→metre bridge stays anchored at zoom 18.
 *
 * Larger scales should wait for the pathfinding rework: each portal builds a full-map flow
 * field, so cost grows with the area (see PLAN.md, 3D-rework Stage 3).
 */
object Sim {
    const val SCALE = 2.0
    val width = (Dim.width * SCALE).toInt()
    val height = (Dim.height * SCALE).toInt()

    // Spawn margins where no portals are placed (absolute, same as Dim's).
    val leftOffset = Dim.leftOffset
    val rightOffset = Dim.rightOffset
    val topOffset = Dim.topOffset
    val botOffset = Dim.botOffset
}
