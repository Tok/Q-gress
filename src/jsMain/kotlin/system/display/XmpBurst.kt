package system.display

import external.Three
import items.level.XmpLevel
import util.Util
import kotlin.math.PI

/**
 * The XMP detonation effect (the "micro-nuke"): a rising mushroom — turbulent stem column → rolling
 * fireball cap (torus) with a hot flashing core — plus an expanding neon ground shockwave ring.
 * Owns its own transient group + active list; [Scene3D] drives it (register/play/update).
 *
 * NOTE: this surface-shaded torus/sphere build is being replaced by a volumetric (raymarched)
 * fire-and-smoke fireball — see XmpShaders.
 */
object XmpBurst {
    private const val RANGE_SCALE = 0.5 // XmpLevel.rangeM → scene-metre blast radius
    private const val LIFE_BASE = 0.9 // seconds; total detonation lifetime at level 0
    private const val LIFE_PER_LEVEL = 0.06 // bigger bursters linger a little longer
    private const val RING_Z = 0.28 // ground shockwave quad height (above passability overlay)

    private var group: dynamic = null
    private val active = mutableListOf<Burst>()

    /** A detonation: [meshes] share [uni]; [geom] = (x, y, radius m, life s); animated by [age]. */
    private class Burst(val meshes: Array<dynamic>, val uni: dynamic, val geom: DoubleArray, var age: Double)

    private val burstGeo: dynamic by lazy { Three.SphereGeometry(1.0, 24, 16) } // unit sphere → hot core
    private val torusGeo: dynamic by lazy { Three.TorusGeometry(1.0, 0.42, 16, 48) } // unit donut → rolling cap
    private val ringQuadGeo: dynamic by lazy { Three.PlaneGeometry(2.0, 2.0) } // flat quad → ground shockwave
    private val stemGeo: dynamic by lazy {
        val g = Three.CylinderGeometry(1.0, 0.5, 1.0, 12).asDynamic()
        g.rotateX(PI / 2) // bake Y-up cylinder to Z-up so the stem rises along +Z
        g
    }

    /** Attach to the scene's transient group (call once per scene). */
    fun register(scene: dynamic) {
        group = Three.Group()
        scene.add(group)
        active.clear()
    }

    fun hasActive() = active.isNotEmpty()

    /** Fire a detonation at scene-metre [cx], [cy], scaled by burster [level] (1..8). */
    fun play(cx: Double, cy: Double, level: Int) {
        val grp = group ?: return
        val rangeM = XmpLevel.values().find { it.level == level }?.rangeM ?: XmpLevel.ONE.rangeM
        val maxR = rangeM * RANGE_SCALE
        val uni: dynamic = js("({ uTime: { value: 0.0 }, uProgress: { value: 0.0 }, uSeed: { value: 0.0 } })")
        uni.uSeed.value = Util.random() * 10.0
        val stem = Three.Mesh(stemGeo, XmpShaders.material(XmpShaders.SURFACE_VERT, XmpShaders.STEM_FRAG, uni, additive = false))
        val cap = Three.Mesh(torusGeo, XmpShaders.material(XmpShaders.SURFACE_VERT, XmpShaders.CAP_FRAG, uni, additive = false))
        val core = Three.Mesh(burstGeo, XmpShaders.material(XmpShaders.SURFACE_VERT, XmpShaders.CORE_FRAG, uni))
        val ring = Three.Mesh(ringQuadGeo, XmpShaders.material(XmpShaders.UV_VERT, XmpShaders.RING_FRAG, uni))
        ring.asDynamic().position.set(cx, cy, RING_Z)
        // Render order (depthTest is off): ground ring, then smoke, then the glowing core on top.
        ring.asDynamic().renderOrder = 1
        stem.asDynamic().renderOrder = 2
        cap.asDynamic().renderOrder = 3
        core.asDynamic().renderOrder = 4
        grp.add(stem)
        grp.add(cap)
        grp.add(core)
        grp.add(ring)
        val life = LIFE_BASE + level * LIFE_PER_LEVEL
        active.add(Burst(arrayOf(stem, cap, core, ring), uni, doubleArrayOf(cx, cy, maxR, life), 0.0))
    }

    fun update(dt: Double) {
        val grp = group ?: return
        val iter = active.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.age += dt
            val cx = b.geom[0]
            val cy = b.geom[1]
            val maxR = b.geom[2]
            val life = b.geom[3]
            val f = (b.age / life).coerceIn(0.0, 1.0)
            b.uni.uProgress.value = f
            b.uni.uTime.value = b.age
            val ease = 1.0 - (1.0 - f) * (1.0 - f) // easeOutQuad
            val rise = maxR * (0.12 + 0.6 * f * f) // mushroom climb (cap clears the stem)
            val capR = maxR * (0.1 + 0.2 * ease) // a head, not a giant donut
            val capH = rise + capR * 0.5
            val flat = 1.0 - 0.4 * smoothstep01(0.4, 1.0, f) // cap flattens as it mushrooms
            val stem = b.meshes[0]
            val stemR = maxR * (0.06 + 0.04 * ease)
            val stemH = capH.coerceAtLeast(0.01)
            stem.scale.set(stemR, stemR, stemH)
            stem.position.set(cx, cy, stemH * 0.5)
            val cap = b.meshes[1]
            cap.scale.set(capR, capR, capR * flat)
            cap.position.set(cx, cy, capH)
            val core = b.meshes[2]
            val coreR = maxR * (0.1 + 0.16 * ease)
            core.scale.set(coreR, coreR, coreR)
            core.position.set(cx, cy, capH)
            b.meshes[3].scale.set(maxR, maxR, 1.0)
            if (b.age >= life) {
                for (m in b.meshes) {
                    grp.remove(m)
                    m.material.dispose()
                }
                iter.remove()
            }
        }
    }

    private fun smoothstep01(edge0: Double, edge1: Double, x: Double): Double {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0.0, 1.0)
        return t * t * (3.0 - 2.0 * t)
    }
}
