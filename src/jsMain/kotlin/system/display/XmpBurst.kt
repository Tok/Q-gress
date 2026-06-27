package system.display

import external.Three
import items.level.XmpLevel
import system.display.shader.XmpShaders
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
    private const val LIFE_BASE = 1.4 // longer life so the mushroom rises + dissolves gradually
    private const val LIFE_PER_LEVEL = 0.1
    private const val RING_Z = 0.28 // ground shockwave height
    private const val RING_TUBE = 0.045 // donut tube radius ÷ ring radius (thin shockwave ring)
    private const val BOX_SCALE = 1.15 // bounding box edge ÷ maxR (must contain the risen mushroom)
    private const val BOX_Z = 0.55 // box centre height ÷ maxR (raised — the cap climbs)

    private var group: dynamic = null
    private val active = mutableListOf<Burst>()

    // Shared per-frame view uniforms (same object referenced by every fireball material).
    private val gInvProj: dynamic = js("({ value: null })")
    private val gRes: dynamic = js("({ value: { x: 1.0, y: 1.0 } })")

    /** A detonation: [meshes] = (fireball box, ground ring); [geom] = (x, y, maxR, life); [squishXY]
     *  flattens the footprint (ultra-strike: narrower box + smaller ring, same height). See update. */
    private class Burst(val meshes: Array<dynamic>, val uni: dynamic, val geom: DoubleArray, var age: Double, val squishXY: Double)

    private val boxGeo: dynamic by lazy { Three.BoxGeometry(1.0, 1.0, 1.0) }
    private val ringGeo: dynamic by lazy { Three.TorusGeometry(1.0, RING_TUBE, 8, 56) } // unit donut, scaled to radius

    // A fresh additive donut material per burst (its colour + opacity animate independently).
    private fun ringMaterial(): dynamic {
        val p: dynamic = js("({})")
        p.color = "#19bfff"
        p.transparent = true
        p.opacity = 1.0
        p.depthTest = false // always on top: a flat near-ground ring depth-tested vs the terrain z-fights/vanishes
        p.depthWrite = false
        p.blending = Three.AdditiveBlending
        return Three.MeshBasicMaterial(p)
    }

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

    /**
     * Fire a detonation at scene-metre [cx], [cy] on a ground at [baseZ], scaled by burster [level] (1..8).
     * [squishXY] flattens the footprint (1.0 = full; ~0.5 = ultra-strike: narrower mushroom + smaller
     * ring, same height); [bright] scales the fireball brightness (ultra-strike reads a touch hotter).
     */
    fun play(cx: Double, cy: Double, baseZ: Double, level: Int, squishXY: Double = 1.0, bright: Double = 1.0) {
        val grp = group ?: return
        val rangeM = XmpLevel.values().find { it.level == level }?.rangeM ?: XmpLevel.ONE.rangeM
        val maxR = rangeM * RANGE_SCALE
        val uni: dynamic = js(
            "({ uTime: { value: 0.0 }, uProgress: { value: 0.0 }, uSeed: { value: 0.0 }," +
                " uRadius: { value: 0.0 }, uBright: { value: 1.0 }, uCenter: { value: { x: 0.0, y: 0.0, z: 0.0 } } })",
        )
        uni.uSeed.value = Util.random() * 10.0
        uni.uBright.value = bright
        uni.uCenter.value.x = cx
        uni.uCenter.value.y = cy
        uni.uInvProj = gInvProj // shared per-frame view uniforms
        uni.uResolution = gRes
        val box = Three.Mesh(boxGeo, XmpShaders.volumeMaterial(uni))
        val bs = maxR * BOX_SCALE
        box.asDynamic().scale.set(bs * squishXY, bs * squishXY, bs) // squish the footprint, keep the height
        box.asDynamic().position.set(cx, cy, baseZ + maxR * BOX_Z)
        box.asDynamic().renderOrder = 2
        val ring = Three.Mesh(ringGeo, ringMaterial()) // a 3D donut sitting on the terrain at the blast
        ring.asDynamic().position.set(cx, cy, baseZ + RING_Z)
        ring.asDynamic().scale.set(0.1, 0.1, 0.1)
        ring.asDynamic().renderOrder = 1
        grp.add(box)
        grp.add(ring)
        active.add(Burst(arrayOf(box, ring), uni, doubleArrayOf(cx, cy, maxR, LIFE_BASE + level * LIFE_PER_LEVEL, baseZ), 0.0, squishXY))
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
            val r = maxR * (0.03 + 0.34 * (1.0 - (1.0 - f) * (1.0 - f))) // tiny pop → fast expand
            b.uni.uRadius.value = r
            // Anchor the sphere bottom at the ground (centre = baseZ + radius) so the BASE stays on the
            // terrain; the mushroom rises *within* the volume via the shader's cap + stem, not by lifting off.
            b.uni.uCenter.value.z = b.geom[4] + r
            // Ground shockwave donut: expands to maxR, fades out, cyan → magenta (matches the old ring).
            val rr = maxR * (1.0 - (1.0 - f) * (1.0 - f)) * b.squishXY // smaller ring for ultra-strike
            b.meshes[1].scale.set(rr, rr, rr)
            val mat = b.meshes[1].material
            mat.color.setRGB(0.1 + 0.9 * f, 0.75 - 0.65 * f, 1.0 - 0.3 * f)
            mat.opacity = (1.0 - f).coerceIn(0.0, 1.0)
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
