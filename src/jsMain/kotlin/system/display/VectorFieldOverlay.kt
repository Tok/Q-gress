package system.display

import World
import external.Three
import kotlin.math.PI
import kotlin.math.atan2

/**
 * The flow-field arrows for the **newest** portal — cones coloured by flow direction (hue),
 * subsampled. No toggle: a portal calls [flash] when it's created and the field shows for ~a second
 * (during gameplay) or until the next portal flashes (during world generation, when they chain).
 * Split out of [Scene3D] (size limit). [register] once, [flash] on portal creation, [sync] each tick.
 */
object VectorFieldOverlay {
    private const val STRIDE = 2 // subsample the flow field every Nth cell
    private const val CONE_R = 1.1
    private const val CONE_H = 3.6
    private const val Z = 0.2 // just above the ground
    private const val FLASH_MS = 1200.0 // how long a new portal's field stays up
    private const val FADE_MS = 450.0 // fade the arrows out over the final stretch (smooth, not abrupt)

    private var group: dynamic = null
    private var flashedId: String? = null
    private var flashEnd = 0.0
    private var builtKey: String? = null
    private val coneGeo: dynamic by lazy { Three.ConeGeometry(CONE_R, CONE_H, 6) }
    private val matCache = mutableMapOf<String, dynamic>()

    fun register(scene: Three.Scene) {
        group = Three.Group().also { scene.add(it) }
    }

    /** Briefly show [portalId]'s flow field (auto-hides after ~a second; replaced by the next portal). */
    fun flash(portalId: String) {
        flashedId = portalId
        flashEnd = now() + FLASH_MS
    }

    /** Rebuild on a new portal, fade out over the final stretch, then clear once the flash expires. */
    fun sync() {
        val g = group ?: return
        val id = flashedId
        val remaining = flashEnd - now()
        if (id != null && remaining > 0.0) {
            if (id != builtKey) {
                g.clear()
                build(g, id)
                builtKey = id
            }
            val alpha = (remaining / FADE_MS).coerceIn(0.0, 1.0)
            matCache.values.forEach { it.opacity = alpha } // only the current field's cones are mounted
        } else if (builtKey != null) {
            g.clear()
            builtKey = null
        }
    }

    private fun now() = js("performance.now()") as Double

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
            p.transparent = true // so the field can fade out
            Three.MeshBasicMaterial(p)
        }
    }
}
