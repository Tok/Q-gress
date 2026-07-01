package system.display
import World
import config.Sim
import external.Three
import kotlinx.browser.document
import util.data.*

/**
 * Shared builder for the draped grid overlays ([PassabilityOverlay] / [MovementPenaltyOverlay]): a ground quad
 * textured one texel per grid cell (coloured by the caller's `colorOf`) and lifted onto the terrain DEM
 * ([Scene3D.groundZ]) so it follows hills/valleys instead of floating flat. Each overlay owns its own group +
 * visibility toggle; this just makes the mesh so the two don't duplicate the canvas/geometry plumbing.
 */
internal object GridOverlay {
    private const val OVERLAY_Z = 0.4 // above the terrain surface (per-vertex), clear of z-fighting
    private const val SEG = 96 // plane subdivisions per axis — enough to follow the DEM smoothly

    /** A per-cell textured, terrain-draped mesh; [colorOf] gives each walkable/blocked cell its overlay colour. */
    fun buildMesh(colorOf: (Cell) -> String): dynamic {
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
            // Only paint cells inside the play area (clips to the circle when round); outside cells stay
            // transparent — still in World.grid for routing, just not displayed.
            if (gx in 0 until cols && gy in 0 until rows && Sim.isInPlayArea(pixel.x, pixel.y)) {
                ctx.fillStyle = colorOf(cell)
                ctx.fillRect(gx, gy, 1, 1)
            }
        }
        val texture = Three.CanvasTexture(canvas)
        // Bilinear so the one-texel-per-cell map reads as a smooth gradient, not blocky pixels; ClampToEdge
        // keeps the non-power-of-2 canvas a valid texture.
        texture.asDynamic().magFilter = Three.LinearFilter
        texture.asDynamic().minFilter = Three.LinearFilter
        texture.asDynamic().wrapS = Three.ClampToEdgeWrapping
        texture.asDynamic().wrapT = Three.ClampToEdgeWrapping
        val matParams: dynamic = js("({})")
        matParams.map = texture
        matParams.transparent = true
        matParams.depthWrite = false
        matParams.depthTest = false // always draw on top; drapes via geometry
        val mesh = Three.Mesh(terrainGeometry(), Three.MeshBasicMaterial(matParams))
        mesh.asDynamic().position.set(0.0, 0.0, 0.0) // height baked into the vertices (groundZ + OVERLAY_Z)
        return mesh
    }

    // A subdivided ground plane whose vertices are lifted onto the terrain DEM ([Scene3D.groundZ]) so the
    // overlay drapes over hills instead of floating flat. UVs are PlaneGeometry defaults, so the per-cell
    // texture maps exactly as when flat — only z changes. Flat (groundZ 0) until the DEM heights are ready.
    private fun terrainGeometry(): dynamic {
        val mpp = Scene3D.metersPerPixel
        val geo = Three.PlaneGeometry(Sim.width * mpp, Sim.height * mpp, SEG, SEG)
        val pos = geo.asDynamic().attributes.position
        val count = pos.count as Int
        for (i in 0 until count) {
            val sx = pos.getX(i) as Double
            val sy = pos.getY(i) as Double
            // Invert Scene3D.sceneX/sceneY (mesh at the origin, unrotated) → sim coords → DEM height.
            val simX = sx / mpp + Sim.width / 2.0
            val simY = -sy / mpp + Sim.height / 2.0
            pos.setZ(i, Scene3D.groundZ(Pos(simX, simY)) + OVERLAY_Z)
        }
        pos.needsUpdate = true
        return geo
    }
}
