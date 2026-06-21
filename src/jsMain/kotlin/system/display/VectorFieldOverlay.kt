package system.display

import World
import external.Three
import kotlin.math.PI
import kotlin.math.atan2

/**
 * Debug overlay: the selected portal's flow-field arrows — cones coloured by flow direction (hue),
 * subsampled. Split out of [Scene3D] (size limit). Rebuilt only when the selection or visibility
 * changes (the field is static per portal). [register] once, [setVisible] to toggle, [sync] each tick.
 */
object VectorFieldOverlay {
    private const val STRIDE = 2 // subsample the flow field every Nth cell
    private const val CONE_R = 1.1
    private const val CONE_H = 3.6
    private const val Z = 0.2 // just above the ground

    private var group: dynamic = null
    private var visible = false
    private var builtKey: String? = null
    private val coneGeo: dynamic by lazy { Three.ConeGeometry(CONE_R, CONE_H, 6) }
    private val matCache = mutableMapOf<String, dynamic>()

    fun register(scene: Three.Scene) {
        group = Three.Group().also { scene.add(it) }
    }

    fun setVisible(show: Boolean) {
        visible = show
    }

    /** Rebuild the arrows only when the [selected] entity or visibility changed. */
    fun sync(selected: String?) {
        val g = group ?: return
        when {
            visible && selected != builtKey -> {
                g.clear()
                build(g, selected)
                builtKey = selected
            }
            !visible && builtKey != null -> {
                g.clear()
                builtKey = null
            }
        }
    }

    private fun build(g: dynamic, selected: String?) {
        val id = selected ?: return
        if (!id.startsWith("portal:")) return
        val portal = World.allPortals.find { "portal:${it.id}" == id } ?: return
        portal.vectors.forEach { (pos, vec) ->
            val gx = pos.x.toInt()
            val gy = pos.y.toInt()
            val mag = vec.magnitude
            if (gx % STRIDE != 0 || gy % STRIDE != 0 || mag == 0.0) return@forEach
            val pixel = pos.fromShadow()
            val angle = atan2(-vec.im / mag, vec.re / mag) // sim y is down → scene y is up
            val cone = Three.Mesh(coneGeo, hueMaterial(angle))
            cone.asDynamic().position.set(Scene3D.sceneX(pixel), Scene3D.sceneY(pixel), Z)
            cone.asDynamic().rotation.z = angle - PI / 2 // cone apex (+Y) → flow direction
            g.add(cone)
        }
    }

    // Colour by flow direction (hue), bucketed to 15° so the material cache stays small.
    private fun hueMaterial(angle: Double): dynamic {
        val deg = ((angle * 180.0 / PI) + 360.0) % 360.0
        val bucket = (deg / 15.0).toInt()
        return matCache.getOrPut("v$bucket") {
            val p: dynamic = js("({})")
            p.color = "hsl(${bucket * 15}, 90%, 55%)"
            Three.MeshBasicMaterial(p)
        }
    }
}
