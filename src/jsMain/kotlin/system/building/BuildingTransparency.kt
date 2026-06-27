package system.building

import system.display.OwnBuildings
import util.MapUtil

/**
 * The "Buildings transparency" control (menu slider + keyboard nudge): one knob over BOTH building sets, since
 * they render together — MapLibre's gap-fillers underneath and our own meshes on top. 0 = solid, 1 = fully
 * see-through; our own meshes are kept a touch more transparent ([OWN_EXTRA]) so the action behind them reads.
 */
object BuildingTransparency {
    private const val DEFAULT_TRANSPARENCY = 0.15
    private const val OWN_EXTRA = 0.2 // our meshes a touch more see-through than MapLibre's

    /** The slider's initial transparency. */
    fun default() = DEFAULT_TRANSPARENCY

    /** Set both building sets from a transparency value (0 = solid … 1 = see-through). */
    fun set(transparency: Double) = applyOpacity((1.0 - transparency).coerceIn(0.0, 1.0))

    /** Keyboard nudge: shift MapLibre's opacity by [deltaOpacity], then mirror onto our meshes. */
    fun nudge(deltaOpacity: Double) {
        MapUtil.nudgeBuildingOpacity(deltaOpacity)
        applyOpacity(MapUtil.currentBuildingOpacity())
    }

    private fun applyOpacity(mapOpacity: Double) {
        MapUtil.setBuildingOpacity(mapOpacity) // MapLibre's gap-fillers
        OwnBuildings.setOpacity((mapOpacity - OWN_EXTRA).coerceIn(0.0, 1.0)) // our meshes
    }
}
