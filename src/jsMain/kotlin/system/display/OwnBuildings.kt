package system.display

import config.Config
import external.Three
import items.Combat
import util.data.Pos
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Our OWN play-area building meshes: extruded prisms rebuilt from the OpenFreeMap vector-tile building
 * footprints (fetched + decoded by [util.BuildingTiles], so we get ALL of them — MapLibre's query APIs
 * only returned a fraction). Replacing the GPU fill-extrusion with real three.js meshes means WE
 * control them — they take real sun shadows, shake per-mesh (no native feature-id needed, so it works
 * on the title too), seed debris colliders, and open the door to walkable roofs later. Each building is
 * keyed by a synthetic id (its footprint centroid), so even tiles with no id work.
 *
 * Matches the MapLibre look at rest (translucent gray, [render_height]). [register] once; [addFeatures]
 * with decoded building features (idempotent — only new keys are meshed); [setInflate] drives the
 * grow-in (per-mesh z-scale) in step with world-gen; [applyBlast]/[updateBobs] do the XMP shake;
 * [clear] on teardown.
 */
object OwnBuildings {
    /**
     * Master switch for the own-mesh building replacement.
     *
     * ON: we get a COMPLETE footprint set by fetching + decoding the OpenFreeMap `.pbf` vector tiles
     * ourselves (see [util.BuildingTiles]) — MapLibre's own query APIs only ever returned a far-flung
     * fraction. With our own meshes we control them: real sun shadows (cast + receive), per-mesh shake
     * (no native feature-id needed, so it works on the title too), debris colliders, and the door open
     * for walkable roofs / more. MapLibre's fill-extrusion layer is hidden (opacity 0) once ours are in.
     * Flip to `false` to fall back to MapLibre's buildings (no building-cast shadows, MapLibre shake).
     */
    const val REPLACE_BUILDINGS = true

    const val COLOR = "#333333"
    const val OPACITY = 0.9
    private const val MAX_BUILDINGS = 8000 // safety cap (separate mesh per building; perf is fine at our scale)
    private const val DEFAULT_HEIGHT = 8.0 // when a footprint has no render_height

    // XMP/US shake (mirrors util.BuildingShake's feel, but applied to our meshes in scene space).
    private const val SHAKE_DURATION = 2.0 // seconds to settle back to rest
    private const val SHAKE_FREQ = 12.0 // wobble speed (rad/s)
    private const val SHAKE_BASE_AMP_M = 7.0 // peak bob (m) at point-blank (clamped to the building's own height)
    private const val SHAKE_REF_HEIGHT_M = 12.0 // taller than this → progressively less bob ("more mass")
    private const val SHAKE_BOB_MAX_FRAC = 0.5 // never bob more than this × the building's height (no sinking)
    private const val SHAKE_ULTRA_MULT = 3.0 // an Ultra-Strike rocks buildings WAY harder than an XMP
    private const val SHAKE_ULTRA_BOB_MAX_FRAC = 0.85 // …and is allowed to bob them much further
    private const val SHAKE_MAX = 120 // safety cap on buildings animated at once

    private var group: dynamic = null
    private var material: dynamic = null
    private val keys = mutableSetOf<String>() // synthetic per-building id → already meshed
    private val blds = mutableListOf<Bld>() // every meshed building (grow-in + shake)
    private val activeBobs = mutableListOf<Bld>() // currently shaking (subset of blds)
    private var inflate = 1.0
    private var sampled = false // one-time position diagnostic

    // One meshed building: its mesh + the data the grow-in and shake need.
    private class Bld(val mesh: dynamic, val baseZ: Double, val cx: Double, val cy: Double, val height: Double) {
        var bobAmp = 0.0
        var bobEnd = 0.0
        var bobPhase = 0.0
    }

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
        blds.clear()
        activeBobs.clear()
        keys.clear()
    }

    /** Mesh any not-yet-seen buildings from [feats] (decoded building features). Idempotent. */
    fun addFeatures(feats: dynamic) {
        val g = group ?: return
        val mat = material ?: return
        val total = (feats.length as? Int) ?: return
        var i = 0
        while (i < total && keys.size < MAX_BUILDINGS) {
            val bld = meshFeature(feats[i], mat)
            i++
            if (bld != null) {
                blds.add(bld)
                g.add(bld.mesh)
            }
        }
        console.log("OwnBuildings: $total source features → ${keys.size} meshed (cap $MAX_BUILDINGS)")
    }

    // Build the building for one feature, registering its key; null if degenerate or already meshed.
    private fun meshFeature(f: dynamic, mat: dynamic): Bld? {
        val ring = outerRing(f.geometry) ?: return null
        val key = ringKey(ring)
        if (key in keys) return null
        val h = (f.properties?.render_height as? Double)?.takeIf { it > 0.5 } ?: DEFAULT_HEIGHT
        val minH = (f.properties?.render_min_height as? Double) ?: 0.0
        val centre = ringCentreSim(ring)
        val baseZ = Scene3D.groundZ(centre) + minH
        val mesh = buildMesh(ring, h, minH, mat, baseZ) ?: return null
        keys.add(key)
        val xy = ringCentreSceneXY(ring)
        return Bld(mesh, baseZ, xy[0], xy[1], h)
    }

    /** Grow-in: scale every building's height by [factor] (0 = flat, 1 = full), matching the gen sweep. */
    fun setInflate(factor: Double) {
        inflate = factor.coerceIn(0.0, 1.0)
        val s = if (inflate < 0.001) 0.001 else inflate
        blds.forEach { it.mesh.scale.z = s }
    }

    // One detonation's precomputed terms (kept off the per-building hot path + parameter lists).
    private class Blast(
        val cx: Double,
        val cy: Double,
        val radiusM: Double,
        val levelGain: Double,
        val intensity: Double,
        val clampFrac: Double,
        val now: Double,
    )

    /** An XMP (or [ultra]-strike) of [level] at scene-metre [cx]/[cy] with blast [radiusM] m: bob every
     *  meshed building within range (taller = less bob, clamped so a short one isn't sunk). [now] = clock s. */
    fun applyBlast(cx: Double, cy: Double, radiusM: Double, level: Int, ultra: Boolean, now: Double) {
        if (radiusM <= 1.0) return
        val blast = Blast(
            cx,
            cy,
            radiusM,
            levelGain = 0.4 + 0.6 * (level.coerceIn(1, 8) / 8.0),
            intensity = if (ultra) SHAKE_ULTRA_MULT else 1.0,
            clampFrac = if (ultra) SHAKE_ULTRA_BOB_MAX_FRAC else SHAKE_BOB_MAX_FRAC,
            now = now,
        )
        var shaken = 0
        for (b in blds) {
            if (shaken >= SHAKE_MAX) break
            if (bob(b, blast)) shaken++
        }
    }

    // Bob one building for a blast (range + mass falloff); false if it's out of range / too far to move.
    private fun bob(b: Bld, blast: Blast): Boolean {
        val d = hypot(b.cx - blast.cx, b.cy - blast.cy)
        if (d > blast.radiusM) return false
        val falloff = Combat.rangeFalloff(d / blast.radiusM) // quintile; 0 beyond range
        if (falloff <= 0.0) return false
        val mass = SHAKE_REF_HEIGHT_M / maxOf(b.height, SHAKE_REF_HEIGHT_M) // taller → smaller bob
        val amp = minOf(
            SHAKE_BASE_AMP_M * blast.levelGain * falloff * mass * blast.intensity * Config.buildingShakeMultiplier,
            b.height * blast.clampFrac,
        )
        if (amp <= 0.0) return false
        b.bobAmp = amp
        b.bobEnd = blast.now + SHAKE_DURATION
        b.bobPhase = (b.cx * 7.0 + b.cy * 13.0) % (2.0 * PI)
        if (b !in activeBobs) activeBobs.add(b)
        return true
    }

    /** Advance every live bob (decaying wobble on mesh z); drop finished ones. Call once per frame. */
    fun updateBobs(now: Double) {
        if (activeBobs.isEmpty()) return
        val done = mutableListOf<Bld>()
        for (b in activeBobs) {
            val remain = b.bobEnd - now
            if (remain <= 0.0) {
                b.mesh.position.z = b.baseZ
                done.add(b)
            } else {
                val env = remain / SHAKE_DURATION // 1 → 0
                b.mesh.position.z = b.baseZ + b.bobAmp * env * env * sin(now * SHAKE_FREQ + b.bobPhase)
            }
        }
        done.forEach { activeBobs.remove(it) }
    }

    private fun buildMesh(ring: Array<DoubleArray>, h: Double, minH: Double, mat: dynamic, baseZ: Double): dynamic {
        val shape = Three.Shape()
        var started = false
        var pointCount = 0
        ring.forEach { ll ->
            val xy = Scene3D.lngLatToSceneXY(ll[0], ll[1]) // exact float — int rounding would distort the footprint
            if (!started) {
                shape.asDynamic().moveTo(xy[0], xy[1])
                started = true
            } else {
                shape.asDynamic().lineTo(xy[0], xy[1])
            }
            pointCount++
        }
        if (pointCount < 3) return js("null") // degenerate
        val opts: dynamic = js("({ bevelEnabled: false, steps: 1 })")
        opts.depth = (h - minH).coerceAtLeast(0.5)
        val geo = Three.ExtrudeGeometry(shape, opts)
        val mesh = Three.Mesh(geo, mat)
        if (!sampled) { // one-time diagnostic: compare to where portals/the map sit
            sampled = true
            val s = Scene3D.lngLatToSceneXY(ring[0][0], ring[0][1])
            console.log(
                "OwnBuildings sample: lnglat=${ring[0][0]},${ring[0][1]} → sceneXY=${s[0]},${s[1]} baseZ=$baseZ depth=${opts.depth}",
            )
        }
        // The shape carries absolute scene XY; sit it on the terrain at the building's base.
        mesh.asDynamic().position.set(0.0, 0.0, baseZ)
        mesh.asDynamic().scale.z = if (inflate < 0.001) 0.001 else inflate
        mesh.asDynamic().castShadow = true
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
        val (lng, lat) = ringCentreLngLat(ring)
        return Scene3D.lngLatToSimPos(lng, lat)
    }

    private fun ringCentreSceneXY(ring: Array<DoubleArray>): DoubleArray {
        val (lng, lat) = ringCentreLngLat(ring)
        return Scene3D.lngLatToSceneXY(lng, lat)
    }

    private fun ringCentreLngLat(ring: Array<DoubleArray>): Pair<Double, Double> {
        var sx = 0.0
        var sy = 0.0
        ring.forEach {
            sx += it[0]
            sy += it[1]
        }
        return (sx / ring.size) to (sy / ring.size)
    }

    // Synthetic id from the footprint centroid (~1 m precision) — dedups across re-queries, no tile id needed.
    private fun ringKey(ring: Array<DoubleArray>): String {
        val (lng, lat) = ringCentreLngLat(ring)
        val cx = (lng * 100000.0).toInt()
        val cy = (lat * 100000.0).toInt()
        return "$cx,$cy"
    }

    private fun isArr(v: dynamic): Boolean = js("Array.isArray")(v) as Boolean
}
