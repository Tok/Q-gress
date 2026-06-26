package system.display
import World
import config.Sim
import external.Three
import util.SoundUtil
import util.data.*
import kotlin.math.PI
import kotlin.math.atan2

/**
 * The flow-field arrows for a freshly-created portal — cones coloured by flow direction (hue),
 * subsampled. No toggle: a portal calls [flash] when its field is ready and the field shows briefly.
 *
 * Fields are now computed asynchronously (PathUtil.computeFieldAsync), so during world generation
 * many land in a rapid burst. A FIFO [queue] replays them as a paced sweep (each shown for at least
 * [MIN_SHOW_MS] before the next), and the last one lingers + fades out. Driven every frame by
 * [Scene3D.render] (a continuous loop), not the per-tick sync, so the sweep keeps animating through
 * the whole load. Split out of [Scene3D] (size limit). [register] once, [flash] on field-ready, [sync] per frame.
 */
object VectorFieldOverlay {
    private const val STRIDE = 2 // subsample the flow field every Nth cell
    private const val CONE_R = 1.1
    private const val CONE_H = 3.6
    private const val Z = 0.2 // just above the ground
    private const val FLASH_MS = 1200.0 // how long the last field lingers before fading out
    private const val FADE_MS = 450.0 // fade the arrows out over the final stretch (smooth, not abrupt)
    private const val MIN_SHOW_MS = 110.0 // min time each field stays up before the queue advances to the next

    private const val TERRAIN_WAIT_MS = 5000.0 // hold the sweep until DEM heights load (cones sit on the ground,
    // not at sea level → no "too low" arrows on a cold first build), then show anyway as a fallback.

    private var group: dynamic = null
    private val queue = ArrayDeque<String>() // portal ids whose fields are ready, awaiting their turn in the sweep
    private var currentId: String? = null
    private var shownAt = 0.0
    private var firstQueuedAt = 0.0 // when the first field was queued — drives the terrain-wait fallback
    private var builtKey: String? = null
    private val coneGeo: dynamic by lazy { Three.ConeGeometry(CONE_R, CONE_H, 6) }
    private val matCache = mutableMapOf<String, dynamic>()

    fun register(scene: Three.Scene) {
        group = Three.Group().also { scene.add(it) }
    }

    /** Off for the title scene — no flow-field flashes there. */
    var flashEnabled = true

    private const val MAX_QUEUE = 24 // bound the FIFO (re-flashing through the long people phase can over-feed it)

    /** Queue [portalId]'s (now-ready) flow field for the sweep; it shows briefly when its turn comes. */
    fun flash(portalId: String) {
        if (!flashEnabled) return
        if (currentId != portalId && queue.lastOrNull() != portalId && queue.size < MAX_QUEUE) {
            if (firstQueuedAt == 0.0) firstQueuedAt = now()
            queue.addLast(portalId)
        }
    }

    /** Advance the sweep (after [MIN_SHOW_MS]), rebuild on change, fade out once the queue is drained. */
    fun sync() {
        val g = group ?: return
        val age = now() - shownAt
        val dueForNext = currentId == null || age >= MIN_SHOW_MS
        // Don't place arrows until the terrain grid is sampled, or they'd sit at z=0 (sea level) — too low.
        // Fall back to showing them anyway after [TERRAIN_WAIT_MS] so a flat / DEM-less map still flashes.
        val terrainOk = Scene3D.terrainReady() || (firstQueuedAt > 0.0 && now() - firstQueuedAt > TERRAIN_WAIT_MS)
        if (dueForNext && queue.isNotEmpty() && terrainOk) {
            currentId = queue.removeFirst()
            shownAt = now()
            g.clear()
            build(g, currentId)
            builtKey = currentId
            // Paced audio through world-gen: a soft route-ready ping as each flow field lands (the long
            // field-prep tail was silent — portal-creation sounds only cover the spawn burst).
            if (!World.isReady) SoundUtil.playOffScreenLocationCreationSound()
        }
        if (currentId == null) return
        val shownFor = now() - shownAt
        if (queue.isEmpty() && shownFor >= FLASH_MS) { // nothing more queued and the last one expired → clear
            g.clear()
            currentId = null
            builtKey = null
            return
        }
        // Full opacity through the sweep; only the lingering last field fades over the final stretch.
        val alpha = ((FLASH_MS - shownFor) / FADE_MS).coerceIn(0.0, 1.0)
        matCache.values.forEach { it.opacity = alpha } // only the current field's cones are mounted
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
            if (!Sim.isInPlayArea(pixel.x, pixel.y)) return@forEach // only show arrows inside the play area

            val angle = atan2(-vec.im / mag, vec.re / mag) // sim y is down → scene y is up
            val cone = Three.Mesh(coneGeo, hueMaterial(angle))
            cone.asDynamic().position.set(Scene3D.sceneX(pixel), Scene3D.sceneY(pixel), Scene3D.groundZ(pixel) + Z)
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
            p.depthTest = false // visible over the 3D terrain
            Three.MeshBasicMaterial(p)
        }
    }
}
