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
    private const val OVERLAY_Z = 0.4 // sits this far above the terrain surface (per-vertex), clear of z-fighting
    private const val SEG = 96 // plane subdivisions per axis — enough to follow the DEM smoothly
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
        matParams.depthTest = false // always draw it on top (no terrain mesh to depth-test against); drapes via geometry
        val mesh = Three.Mesh(terrainGeometry(), Three.MeshBasicMaterial(matParams))
        mesh.asDynamic().position.set(0.0, 0.0, 0.0) // height is baked into the vertices (groundZ + OVERLAY_Z)
        return mesh
    }

    // A subdivided ground plane whose vertices are lifted onto the terrain DEM (Scene3D.groundZ), so the
    // overlay drapes over hills/valleys instead of floating as a flat quad. UVs are PlaneGeometry's defaults,
    // so the per-cell texture maps exactly as it did when flat — only z changes. Falls back to flat (groundZ
    // returns 0) until the terrain heights are ready.
    private fun terrainGeometry(): dynamic {
        val mpp = Scene3D.metersPerPixel
        val geo = Three.asDynamic().PlaneGeometry(Sim.width * mpp, Sim.height * mpp, SEG, SEG) // 4-arg ctor (segments)
        val pos = geo.asDynamic().attributes.position
        val count = pos.count as Int
        for (i in 0 until count) {
            val sx = pos.getX(i) as Double
            val sy = pos.getY(i) as Double
            // Invert Scene3D.sceneX/sceneY (mesh sits at the origin, unrotated) → sim coords → DEM height.
            val simX = sx / mpp + Sim.width / 2.0
            val simY = -sy / mpp + Sim.height / 2.0
            pos.setZ(i, Scene3D.groundZ(Pos(simX, simY)) + OVERLAY_Z)
        }
        pos.needsUpdate = true
        return geo
    }
}
