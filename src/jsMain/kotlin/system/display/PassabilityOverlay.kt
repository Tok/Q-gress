package system.display
import World
import external.Three
import util.data.overlayColor

/**
 * The walkability/terrain overlay: a ground quad textured one texel per grid cell, coloured by
 * [util.data.Cell.overlayColor] (white road → grey high-penalty ground → dark blocked). The draped-mesh
 * plumbing lives in [GridOverlay]; this owns the "Terrain" display view's group + toggle. [register] once,
 * then [setVisible] to toggle.
 */
object PassabilityOverlay {
    private var group: dynamic = null
    private var visible = false

    fun register(scene: Three.Scene) {
        group = Three.Group().also { scene.add(it) }
    }

    fun setVisible(show: Boolean) {
        visible = show
        val g = group ?: return
        g.clear()
        if (show && World.isReady) g.add(GridOverlay.buildMesh { it.overlayColor() })
    }

    /** Re-drape against the current DEM — call when the terrain (re)samples ([Scene3D.onTerrainChanged]); the
     *  mesh bakes `groundZ` at build time, so a visible overlay otherwise keeps its flat/stale heights. */
    fun refresh() {
        if (visible) setVisible(true)
    }

    fun isVisible() = visible
}
