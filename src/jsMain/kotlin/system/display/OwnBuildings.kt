package system.display

import external.Three
import util.data.Pos

/**
 * Our OWN play-area building meshes: extruded prisms rebuilt from the MapLibre vector-tile building
 * footprints (queried via `querySourceFeatures`, so they're available even with the MapLibre building
 * layer hidden). Replacing the GPU fill-extrusion with real three.js meshes means WE control them —
 * they can take real shadows, be walked on, shaken individually, etc. — and sidesteps the vector-tile
 * feature-id limitation entirely: each building is keyed by a synthetic id (its footprint centroid),
 * so even tiles with no id work.
 *
 * Matches the MapLibre look at rest (translucent gray, [render_height]). [register] once; [addFeatures]
 * with queried building features (idempotent — only new keys are meshed); [setInflate] drives the
 * grow-in (per-mesh z-scale) in step with world-gen; [clear] on teardown.
 */
object OwnBuildings {
    const val COLOR = "#333333"
    const val OPACITY = 0.9
    private const val MAX_BUILDINGS = 8000 // safety cap (separate mesh per building; perf is fine at our scale)
    private const val DEFAULT_HEIGHT = 8.0 // when a footprint has no render_height

    private var group: dynamic = null
    private var material: dynamic = null
    private val keys = mutableSetOf<String>() // synthetic per-building id → already meshed
    private val meshes = mutableListOf<dynamic>() // for the grow-in z-scale
    private var inflate = 1.0

    fun register(scene: Three.Scene) {
        group = Three.Group().also { scene.add(it) }
        val p: dynamic = js("({})")
        p.color = COLOR
        p.transparent = true
        p.opacity = OPACITY
        p.metalness = 0.0
        p.roughness = 1.0
        material = Three.MeshStandardMaterial(p)
    }

    fun hasAny() = keys.isNotEmpty()

    /** Drop every meshed building (e.g. a fresh world). */
    fun clear() {
        val g = group ?: return
        g.clear()
        meshes.clear()
        keys.clear()
    }

    /** Mesh any not-yet-seen buildings from [feats] (queried building features). Idempotent. */
    fun addFeatures(feats: dynamic) {
        val g = group ?: return
        val mat = material ?: return
        val total = (feats.length as? Int) ?: return
        var i = 0
        while (i < total && keys.size < MAX_BUILDINGS) {
            val mesh = meshFeature(feats[i], mat)
            i++
            if (mesh != null) {
                meshes.add(mesh)
                g.add(mesh)
            }
        }
        console.log("OwnBuildings: $total source features → ${keys.size} meshed (cap $MAX_BUILDINGS)")
    }

    // Build the mesh for one feature, registering its key; null if degenerate or already meshed.
    private fun meshFeature(f: dynamic, mat: dynamic): dynamic {
        val ring = outerRing(f.geometry) ?: return null
        val key = ringKey(ring)
        if (key in keys) return null
        val h = (f.properties?.render_height as? Double)?.takeIf { it > 0.5 } ?: DEFAULT_HEIGHT
        val minH = (f.properties?.render_min_height as? Double) ?: 0.0
        val mesh = buildMesh(ring, h, minH, mat) ?: return null
        keys.add(key)
        return mesh
    }

    /** Grow-in: scale every building's height by [factor] (0 = flat, 1 = full), matching the gen sweep. */
    fun setInflate(factor: Double) {
        inflate = factor.coerceIn(0.0, 1.0)
        meshes.forEach { it.scale.z = if (inflate < 0.001) 0.001 else inflate }
    }

    private fun buildMesh(ring: Array<DoubleArray>, h: Double, minH: Double, mat: dynamic): dynamic {
        val shape = Three.Shape()
        var started = false
        var first = true
        ring.forEach { ll ->
            val xy = Scene3D.lngLatToSceneXY(ll[0], ll[1]) // exact float — int rounding would distort the footprint
            val x = xy[0]
            val y = xy[1]
            if (!started) {
                shape.asDynamic().moveTo(x, y)
                started = true
            } else {
                shape.asDynamic().lineTo(x, y)
                first = false
            }
        }
        if (first) return js("null") // degenerate (a single point)
        val opts: dynamic = js("({ bevelEnabled: false, steps: 1 })")
        opts.depth = (h - minH).coerceAtLeast(0.5)
        val geo = Three.ExtrudeGeometry(shape, opts)
        val mesh = Three.Mesh(geo, mat)
        // The shape carries absolute scene XY; sit it on the terrain at the building's base.
        val centre = ringCentreSim(ring)
        mesh.asDynamic().position.set(0.0, 0.0, Scene3D.groundZ(centre) + minH)
        mesh.asDynamic().scale.z = if (inflate < 0.001) 0.001 else inflate
        mesh.asDynamic().castShadow = true // ready for stage 2 (the sun)
        mesh.asDynamic().receiveShadow = true
        return mesh
    }

    // Outer ring as [lng,lat] points (handles Polygon + MultiPolygon; first polygon's outer ring).
    private fun outerRing(geom: dynamic): Array<DoubleArray>? {
        val c = geom?.coordinates ?: return null
        if (!isArr(c) || !isArr(c[0]) || !isArr(c[0][0])) return null
        val ring = if (isArr(c[0][0][0])) c[0][0] else c[0] // MultiPolygon → c[0][0]; Polygon → c[0]
        val n = (ring.length as? Int) ?: return null
        if (n < 3) return null
        return Array(n) { doubleArrayOf(ring[it][0] as Double, ring[it][1] as Double) }
    }

    private fun ringCentreSim(ring: Array<DoubleArray>): Pos {
        var sx = 0.0
        var sy = 0.0
        ring.forEach {
            sx += it[0]
            sy += it[1]
        }
        return Scene3D.lngLatToSimPos(sx / ring.size, sy / ring.size)
    }

    // Synthetic id from the footprint centroid (~1 m precision) — dedups across re-queries, no tile id needed.
    private fun ringKey(ring: Array<DoubleArray>): String {
        var sx = 0.0
        var sy = 0.0
        ring.forEach {
            sx += it[0]
            sy += it[1]
        }
        val cx = ((sx / ring.size) * 100000.0).toInt()
        val cy = ((sy / ring.size) * 100000.0).toInt()
        return "$cx,$cy"
    }

    private fun isArr(v: dynamic): Boolean = js("Array.isArray")(v) as Boolean
}
