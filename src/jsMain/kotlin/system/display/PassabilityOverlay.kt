package system.display

import World
import config.Sim
import external.Three
import kotlinx.browser.document
import util.data.Pos

/**
 * The walkability/terrain overlay: a ground quad textured one texel per grid cell, coloured by
 * [util.data.Cell.overlayColor] (white road → grey high-penalty ground → dark blocked). Split out
 * of [Scene3D] (size limit) and the home for the "Terrain" display view. [register] once, then
 * [setVisible] to toggle.
 */
object PassabilityOverlay {
    private const val OVERLAY_Z = 0.2 // just above the ground plane
    private var group: dynamic = null
    private var visible = false

    fun register(scene: Three.Scene) {
        group = Three.Group().also { scene.add(it) }
    }

    fun setVisible(show: Boolean) {
        visible = show
        val g = group ?: return
        g.clear()
        if (show && World.isReady) g.add(buildMesh())
    }

    fun isVisible() = visible

    private fun buildMesh(): dynamic {
        val cols = Sim.width / Pos.res
        val rows = Sim.height / Pos.res
        val canvas = document.createElement("canvas").asDynamic()
        canvas.width = cols
        canvas.height = rows
        val ctx = canvas.getContext("2d")
        World.grid.forEach { (pos, cell) ->
            val gx = pos.x.toInt()
            val gy = pos.y.toInt()
            val pixel = pos.fromShadow()
            // Only paint cells inside the play area (clips to the circle when round). Outside cells stay
            // transparent — their passability is still in World.grid for routing, just not displayed.
            if (gx in 0 until cols && gy in 0 until rows && Sim.isInPlayArea(pixel.x, pixel.y)) {
                ctx.fillStyle = cell.overlayColor()
                ctx.fillRect(gx, gy, 1, 1)
            }
        }
        val texture = Three.CanvasTexture(canvas)
        texture.asDynamic().magFilter = Three.NearestFilter
        texture.asDynamic().minFilter = Three.NearestFilter
        val matParams: dynamic = js("({})")
        matParams.map = texture
        matParams.transparent = true
        matParams.depthWrite = false
        matParams.depthTest = false // a flat quad sits under the 3D terrain — draw it on top instead
        val mesh = Three.Mesh(
            Three.PlaneGeometry(Sim.width * Scene3D.metersPerPixel, Sim.height * Scene3D.metersPerPixel),
            Three.MeshBasicMaterial(matParams),
        )
        mesh.asDynamic().position.set(0.0, 0.0, OVERLAY_Z)
        return mesh
    }
}
