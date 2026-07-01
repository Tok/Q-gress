package system.display
import World
import external.Three
import util.data.penaltyColor

/**
 * The movement-penalty (cost) overlay: same draped one-texel-per-cell mesh as [PassabilityOverlay] via
 * [GridOverlay], but coloured by [util.data.Cell.penaltyColor] — a white(0% penalty / fast road)→black(100%
 * penalty / impassable) greyscale — so the cost the flow fields route around reads at a glance.
 * [register] once, then [setVisible] to toggle (menu: "Movement penalty map").
 */
object MovementPenaltyOverlay {
    private var group: dynamic = null
    private var visible = false

    fun register(scene: Three.Scene) {
        group = Three.Group().also { scene.add(it) }
    }

    fun setVisible(show: Boolean) {
        visible = show
        val g = group ?: return
        g.clear()
        if (show && World.isReady) g.add(GridOverlay.buildMesh { it.penaltyColor() })
    }

    /** Re-drape against the freshly-sampled DEM (heights are baked into the mesh at build time). */
    fun refresh() {
        if (visible) setVisible(true)
    }

    fun isVisible() = visible
}
