package system.display

import external.Three
import items.level.XmpLevel
import util.Util

/**
 * The XMP detonation effect: a **volumetric raymarched** fireball — emissive fire in the hot core
 * fading to dark/black smoke — that rises and dissipates, plus a flat neon ground shockwave ring.
 *
 * The fireball is a bounding box whose shader marches a 3D noise field (see [XmpShaders]). It needs
 * the inverse projection matrix + viewport each frame to reconstruct camera rays — [Scene3D] feeds
 * those via [setView] before [update]. Owns its own transient group + active list.
 */
object XmpBurst {
    private const val RANGE_SCALE = 0.5 // XmpLevel.rangeM → scene-metre blast radius
    private const val LIFE_BASE = 0.9
    private const val LIFE_PER_LEVEL = 0.06
    private const val RING_Z = 0.28 // ground shockwave height
    private const val BOX_SCALE = 0.95 // bounding box edge ÷ maxR (must contain the risen fireball)
    private const val BOX_Z = 0.45 // box centre height ÷ maxR

    private var group: dynamic = null
    private val active = mutableListOf<Burst>()

    // Shared per-frame view uniforms (same object referenced by every fireball material).
    private val gInvProj: dynamic = js("({ value: null })")
    private val gRes: dynamic = js("({ value: { x: 1.0, y: 1.0 } })")

    /** A detonation: [meshes] = (fireball box, ground ring); [geom] = (x, y, maxR, life); see update. */
    private class Burst(val meshes: Array<dynamic>, val uni: dynamic, val geom: DoubleArray, var age: Double)

    private val boxGeo: dynamic by lazy { Three.BoxGeometry(1.0, 1.0, 1.0) }
    private val ringQuadGeo: dynamic by lazy { Three.PlaneGeometry(2.0, 2.0) }

    fun register(scene: dynamic) {
        group = Three.Group()
        scene.add(group)
        active.clear()
    }

    fun hasActive() = active.isNotEmpty()

    /** Feed the live inverse projection matrix + drawing-buffer size (called each frame). */
    fun setView(invProj: dynamic, width: Double, height: Double) {
        gInvProj.value = invProj
        gRes.value.x = width
        gRes.value.y = height
    }

    /** Fire a detonation at scene-metre [cx], [cy], scaled by burster [level] (1..8). */
    fun play(cx: Double, cy: Double, level: Int) {
        val grp = group ?: return
        val rangeM = XmpLevel.values().find { it.level == level }?.rangeM ?: XmpLevel.ONE.rangeM
        val maxR = rangeM * RANGE_SCALE
        val uni: dynamic = js(
            "({ uTime: { value: 0.0 }, uProgress: { value: 0.0 }, uSeed: { value: 0.0 }," +
                " uRadius: { value: 0.0 }, uCenter: { value: { x: 0.0, y: 0.0, z: 0.0 } } })",
        )
        uni.uSeed.value = Util.random() * 10.0
        uni.uCenter.value.x = cx
        uni.uCenter.value.y = cy
        uni.uInvProj = gInvProj // shared per-frame view uniforms
        uni.uResolution = gRes
        val box = Three.Mesh(boxGeo, XmpShaders.volumeMaterial(uni))
        val bs = maxR * BOX_SCALE
        box.asDynamic().scale.set(bs, bs, bs)
        box.asDynamic().position.set(cx, cy, maxR * BOX_Z)
        box.asDynamic().renderOrder = 2
        val ring = Three.Mesh(ringQuadGeo, XmpShaders.material(XmpShaders.UV_VERT, XmpShaders.RING_FRAG, uni))
        ring.asDynamic().position.set(cx, cy, RING_Z)
        ring.asDynamic().scale.set(maxR, maxR, 1.0)
        ring.asDynamic().renderOrder = 1
        grp.add(box)
        grp.add(ring)
        active.add(Burst(arrayOf(box, ring), uni, doubleArrayOf(cx, cy, maxR, LIFE_BASE + level * LIFE_PER_LEVEL), 0.0))
    }

    fun update(dt: Double) {
        val grp = group ?: return
        val iter = active.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.age += dt
            val maxR = b.geom[2]
            val life = b.geom[3]
            val f = (b.age / life).coerceIn(0.0, 1.0)
            b.uni.uProgress.value = f
            b.uni.uTime.value = b.age
            b.uni.uCenter.value.z = maxR * (0.08 + 0.27 * f) // rises ~half as high as the old cap
            b.uni.uRadius.value = maxR * (0.12 + 0.2 * (1.0 - (1.0 - f) * (1.0 - f))) // grows to ~0.32 maxR
            if (b.age >= life) {
                for (m in b.meshes) {
                    grp.remove(m)
                    m.material.dispose()
                }
                iter.remove()
            }
        }
    }
}
