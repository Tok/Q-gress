package system.display

import external.Cannon
import external.Three
import util.Util
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The physics shatter: glass shards (cannon-es rigid bodies reusing the flask GLB), the falling
 * rubber gasket, and the metal pole sinking into the ground. Split out of [Scene3D] (size limit);
 * also the natural home for the resonators-fall-out physics. [register] once, [shatter] per portal
 * destruction, [update] each frame while [hasActive].
 */
object ShatterFx {
    private const val SHARD_BRIGHT = 1.4 // shards share the orb GlassShader, a touch brighter so the pieces read
    private const val SHARD_FADE = 1.2 // seconds to fade out at end of life
    private const val SHARD_LIFE_MIN = 6.0
    private const val SHARD_LIFE_MAX = 10.0
    private const val SHARD_SPIN = 1.8 // max tumble rad/s — a gentle turn
    private const val SHARD_GROUP = 2 // shards collide with ground/pole only…
    private const val SHARD_MASK = 1 // …not each other (their boxes all sit at the orb centre)
    private const val SHARD_MASS = 1.0
    private const val GRAVITY = 20.0 // ~2× real g — shards drop with weight
    private const val POLE_SINK_S = 1.4 // seconds for the metal pole to sink away
    private const val POLE_R = 2.0 // for the gasket collider box

    private const val BLAST_WINDOW = 0.6 // s after a blast that shatter pieces still get pushed away
    private const val BLAST_SPEED = 18.0 // m/s base impulse magnitude at the cloud centre (energy ∝ level/distance via BlastModel)
    private const val BLAST_REF = 80.0 // distance falloff scale — pieces farther from the cloud centre get less push
    private const val BLAST_FLOOR = 0.4 // L1 keeps this fraction of full energy (L8 = 1.0)
    private const val BLAST_UP = 0.5 // upward arc as a fraction of the impulse magnitude (debris flies up-and-out)
    private val blastOrigin = doubleArrayOf(0.0, 0.0, 0.0) // the 3D mushroom-cloud centre, above the terrain
    private var blastLevel = 8
    private var blastTime = -1.0

    /** Record an XMP detonation whose 3D mushroom-cloud centre is [origin] (scene metres, above ground), of [level]. */
    fun recordBlast(origin: DoubleArray, level: Int = 8) {
        blastOrigin[0] = origin[0]
        blastOrigin[1] = origin[1]
        blastOrigin[2] = origin[2]
        blastLevel = level
        blastTime = now()
    }

    /**
     * Outward velocity (vx, vy, vz) pushing a piece at [x], [y], [z] away from a recent blast (else zero).
     * Shares [BlastModel]'s 3D cloud-centre + falloff law with the title, then biases the push **up** so
     * shards/resos arc up-and-out from the cloud centre instead of straight down into the ground.
     */
    private fun blastPush(x: Double, y: Double, z: Double): DoubleArray {
        if (blastTime < 0.0 || (now() - blastTime) / 1000.0 > BLAST_WINDOW) return doubleArrayOf(0.0, 0.0, 0.0)
        val imp = BlastModel.blastImpulse(blastOrigin, doubleArrayOf(x, y, z), blastLevel, BLAST_SPEED, BLAST_REF, BLAST_FLOOR)
        val mag = sqrt(imp[0] * imp[0] + imp[1] * imp[1] + imp[2] * imp[2])
        val hlen = sqrt(imp[0] * imp[0] + imp[1] * imp[1]) // horizontal radial out from the blast column
        val hx = if (hlen > 1e-6) imp[0] / hlen * mag else 0.0
        val hy = if (hlen > 1e-6) imp[1] / hlen * mag else 0.0
        return doubleArrayOf(hx, hy, mag * BLAST_UP) // out (horizontal) + a guaranteed up-arc
    }

    /**
     * A NEW explosion ([origin] = cloud centre, [level]) flings every piece that's already mid-fall —
     * shards, dropped resonators, mods, o-rings, the gasket — not just ones spawned during this blast.
     */
    fun applyBlast(origin: DoubleArray, level: Int) {
        if (activeShards.isEmpty()) return
        activeShards.forEach { s ->
            val p = s.body.asDynamic().position
            val imp = BlastModel.blastImpulse(
                origin,
                doubleArrayOf(p.x as Double, p.y as Double, p.z as Double),
                level,
                BLAST_SPEED,
                BLAST_REF,
                BLAST_FLOOR,
            )
            val v = s.body.asDynamic().velocity
            v.set((v.x as Double) + imp[0], (v.y as Double) + imp[1], (v.z as Double) + imp[2] * BLAST_UP)
        }
    }

    private fun now() = Scene3D.animMs() // sim-scaled clock so FX track sim speed

    private var world: Cannon.World? = null
    private var group: dynamic = null // shards + sinking poles (not cleared by sync)
    private var shatterRot = doubleArrayOf(0.0, 0.0, 0.0) // per-shatter random orientation
    private val activeShards = mutableListOf<Shard>()
    private val sinkingPoles = mutableListOf<SinkPole>()

    private class Shard(
        val mesh: dynamic,
        val mat: dynamic,
        val body: Cannon.Body,
        var age: Double,
        val life: Double,
        val setFade: (Double) -> Unit,
    )
    private class SinkPole(val mesh: dynamic, val poleH: Double, var age: Double)

    /** Create the physics world + the shards group (once, when the scene is set up). */
    fun register(scene: Three.Scene) {
        group = Three.Group().also { scene.add(it) }
        world = createWorld()
    }

    /** Add a static building box (scene-space centre + half-extents) so falling debris lands on it. */
    fun addStaticBox(cx: Double, cy: Double, cz: Double, hx: Double, hy: Double, hz: Double) {
        val w = world ?: return
        val opts: dynamic = js("({ mass: 0 })")
        opts.position = Cannon.Vec3(cx, cy, cz)
        opts.shape = Cannon.Box(Cannon.Vec3(hx, hy, hz))
        w.addBody(Cannon.Body(opts))
    }

    private val poleBodies = mutableListOf<Cannon.Body>()

    /** Replace the portal-pole colliders so falling debris hits the poles instead of passing through.
     *  [specs] = [cx, cy, baseZ, topZ, radius] per pole; rebuilt each sync (poles grow with level / sink). */
    fun setPoleColliders(specs: Array<DoubleArray>) {
        val w = world ?: return
        poleBodies.forEach { w.removeBody(it) }
        poleBodies.clear()
        specs.forEach { s ->
            val h = s[3] - s[2]
            if (h > 0.5) {
                val opts: dynamic = js("({ mass: 0 })")
                opts.position = Cannon.Vec3(s[0], s[1], s[2] + h / 2.0)
                opts.shape = Cannon.Box(Cannon.Vec3(s[4], s[4], h / 2.0)) // square box ≈ the round pole
                Cannon.Body(opts).also {
                    w.addBody(it)
                    poleBodies.add(it)
                }
            }
        }
    }

    fun hasActive() = activeShards.isNotEmpty() || sinkingPoles.isNotEmpty()

    fun update(dt: Double) {
        if (activeShards.isNotEmpty()) updateShards(dt)
        if (sinkingPoles.isNotEmpty()) updateSinkingPoles(dt)
    }

    /**
     * Shatter a portal: [variants]/[flaskScale] from ShardAssets, [poleGeo]/[gasketGeo] reused from
     * Scene3D. Shards spawn at the orb centre [orbZ] (scaled by [orbScale]); the pole sinks; the
     * gasket drops freely (no static collider for them to rest on).
     */
    @Suppress("LongParameterList") // a shatter just needs this much geometry from the caller
    fun shatter(
        x: Double,
        y: Double,
        poleH: Double,
        poleScaleVal: Double,
        orbZ: Double,
        orbScale: Double,
        color: String,
        variants: List<List<dynamic>>,
        flaskScale: Double,
        poleGeo: dynamic,
        gasketGeo: dynamic,
    ) {
        val w = world ?: return
        shatterRot = doubleArrayOf(Util.random() * 2.0 * PI, Util.random() * 2.0 * PI, Util.random() * 2.0 * PI)
        if (variants.isNotEmpty()) {
            val variant = variants[(Util.random() * variants.size).toInt()]
            variant.forEach { holder -> spawnShard(w, holder, doubleArrayOf(x, y, orbZ), flaskScale * orbScale, color) }
        }
        spawnSinkingPole(poleGeo, x, y, poleH, poleScaleVal)
        spawnGasket(w, gasketGeo, x, y, poleH)
    }

    /**
     * Just the orb's glass shards (no pole/gasket) — for a **capture**: the portal stays standing
     * while its orb re-skins to the new faction, so only the old-colour orb shards fly off.
     */
    @Suppress("LongParameterList") // position + size + colour + the shard assets
    fun shatterOrb(x: Double, y: Double, orbZ: Double, orbScale: Double, color: String, variants: List<List<dynamic>>, flaskScale: Double) {
        val w = world ?: return
        if (variants.isEmpty()) return
        shatterRot = doubleArrayOf(Util.random() * 2.0 * PI, Util.random() * 2.0 * PI, Util.random() * 2.0 * PI)
        val variant = variants[(Util.random() * variants.size).toInt()]
        variant.forEach { holder -> spawnShard(w, holder, doubleArrayOf(x, y, orbZ), flaskScale * orbScale, color) }
    }

    private fun spawnSinkingPole(poleGeo: dynamic, x: Double, y: Double, poleH: Double, poleScaleVal: Double) {
        val pole = Three.Mesh(poleGeo, Materials.metal())
        pole.asDynamic().rotation.x = PI / 2 // Y-axis cylinder → vertical
        pole.asDynamic().scale.set(1.0, poleScaleVal, 1.0)
        pole.asDynamic().position.set(x, y, poleH / 2)
        group.add(pole)
        sinkingPoles.add(SinkPole(pole, poleH, 0.0))
    }

    private fun updateSinkingPoles(dt: Double) {
        val iter = sinkingPoles.iterator()
        while (iter.hasNext()) {
            val sp = iter.next()
            sp.age += dt
            val sunk = (sp.age / POLE_SINK_S).coerceIn(0.0, 1.0)
            sp.mesh.position.z = sp.poleH / 2.0 - sunk * (sp.poleH + 5.0) // fully below ground by the end
            if (sp.age >= POLE_SINK_S) {
                group.remove(sp.mesh)
                iter.remove()
            }
        }
    }

    private fun spawnGasket(world: Cannon.World, gasketGeo: dynamic, x: Double, y: Double, poleH: Double) {
        val p: dynamic = js("({})")
        p.color = "#0a0a0a"
        p.metalness = 0.0
        p.roughness = 0.95
        p.transparent = true
        p.opacity = 1.0
        val mat = Three.MeshStandardMaterial(p)
        val mesh = Three.Mesh(gasketGeo, mat)
        val opts: dynamic = js("({})")
        opts.mass = SHARD_MASS
        opts.position = Cannon.Vec3(x, y, poleH)
        opts.shape = Cannon.Box(Cannon.Vec3(POLE_R * 1.5, POLE_R * 1.5, POLE_R * 0.5))
        opts.linearDamping = 0.05
        opts.angularDamping = 0.3
        val body = Cannon.Body(opts)
        val push = blastPush(x, y, poleH)
        body.asDynamic().velocity.set((Util.random() - 0.5) * 3.0 + push[0], (Util.random() - 0.5) * 3.0 + push[1], -1.0 + push[2])
        body.asDynamic().angularVelocity.set(randSpin() * 0.4, randSpin() * 0.4, randSpin() * 0.4)
        world.addBody(body)
        group.add(mesh)
        activeShards.add(
            Shard(mesh, mat, body, 0.0, SHARD_LIFE_MIN + Util.random() * (SHARD_LIFE_MAX - SHARD_LIFE_MIN)) { f ->
                mat.asDynamic().opacity =
                    f
            },
        )
    }

    /**
     * A resonator rod ([geo] = the unit Y-cylinder) dropping out of a shattered portal: starts
     * upright at the slot ([x], [y], [z]), then tumbles and fades. Builds its OWN material (the shared
     * [Materials.resonator] cache would fade live portals' rods if we mutated its opacity).
     */
    @Suppress("LongParameterList") // position + size + colour for one falling rod
    fun spawnFallingRod(geo: dynamic, x: Double, y: Double, z: Double, radius: Double, length: Double, color: String) {
        val w = world ?: return
        // Glassy, but with the energy bar EMPTY (fill 0) — a reso only drops out once destroyed/drained.
        val mat = GlassShader.material(color, SHARD_BRIGHT, 0.0)
        val mesh = Three.Mesh(geo, mat)
        mesh.asDynamic().scale.set(1.0, length, 1.0) // unit Y-cylinder → rod length
        val opts: dynamic = js("({})")
        opts.mass = SHARD_MASS
        opts.position = Cannon.Vec3(x, y, z)
        opts.shape = Cannon.Box(Cannon.Vec3(radius, length / 2.0, radius)) // length along local Y
        opts.linearDamping = 0.05
        opts.angularDamping = 0.25
        opts.collisionFilterGroup = SHARD_GROUP // rods rest on the ground, ignore each other/shards
        opts.collisionFilterMask = SHARD_MASK
        val body = Cannon.Body(opts)
        body.asDynamic().quaternion.setFromEuler(PI / 2, 0.0, 0.0) // upright like in the slot
        val push = blastPush(x, y, z)
        body.asDynamic().velocity.set(
            (Util.random() - 0.5) * 4.0 + push[0],
            (Util.random() - 0.5) * 4.0 + push[1],
            Util.random() * 2.0 + push[2],
        )
        body.asDynamic().angularVelocity.set(randSpin(), randSpin(), randSpin())
        w.addBody(body)
        group.add(mesh)
        activeShards.add(
            Shard(mesh, mat, body, 0.0, SHARD_LIFE_MIN + Util.random() * (SHARD_LIFE_MAX - SHARD_LIFE_MIN)) { f ->
                mat.uniforms.uFade.value = f // glass fade (was MeshStandard opacity)
            },
        )
    }

    /**
     * A slot-mod mesh ([geo] = dodeca / pentagon / cube) tumbling out of a shattered or neutralized
     * portal: starts at the orb centre ([x], [y], [z]) at [scale], with a small box collider ([half]),
     * then tumbles + fades. Builds its own emissive material (rarity [color]) so it can fade alone.
     */
    @Suppress("LongParameterList") // position + size + colour for one falling mod
    fun spawnFallingChunk(geo: dynamic, x: Double, y: Double, z: Double, scale: Double, half: Double, color: String) {
        val w = world ?: return
        val p: dynamic = js("({})")
        p.color = color
        p.emissive = color
        p.emissiveIntensity = 0.35
        p.metalness = 0.3
        p.roughness = 0.5
        p.transparent = true
        p.opacity = 1.0
        val mat = Three.MeshStandardMaterial(p)
        val mesh = Three.Mesh(geo, mat)
        mesh.asDynamic().scale.set(scale, scale, scale)
        val opts: dynamic = js("({})")
        opts.mass = SHARD_MASS
        opts.position = Cannon.Vec3(x, y, z)
        opts.shape = Cannon.Box(Cannon.Vec3(half, half, half))
        opts.linearDamping = 0.05
        opts.angularDamping = 0.25
        opts.collisionFilterGroup = SHARD_GROUP
        opts.collisionFilterMask = SHARD_MASK
        val body = Cannon.Body(opts)
        val push = blastPush(x, y, z)
        body.asDynamic().velocity.set(
            (Util.random() - 0.5) * 4.0 + push[0],
            (Util.random() - 0.5) * 4.0 + push[1],
            Util.random() * 2.0 + push[2],
        )
        body.asDynamic().angularVelocity.set(randSpin(), randSpin(), randSpin())
        w.addBody(body)
        group.add(mesh)
        activeShards.add(
            Shard(mesh, mat, body, 0.0, SHARD_LIFE_MIN + Util.random() * (SHARD_LIFE_MAX - SHARD_LIFE_MIN)) { f ->
                mat.asDynamic().opacity =
                    f
            },
        )
    }

    private fun spawnShard(world: Cannon.World, holder: dynamic, pos: DoubleArray, scale: Double, color: String) {
        val mat = GlassShader.material(color, SHARD_BRIGHT)
        val mesh = Three.Mesh(holder.geo, mat)
        mesh.asDynamic().scale.set(scale, scale, scale)
        val opts: dynamic = js("({})")
        opts.mass = SHARD_MASS
        opts.position = Cannon.Vec3(pos[0], pos[1], pos[2])
        opts.shape = Cannon.Box(
            Cannon.Vec3(
                ((holder.hx as Double) * scale).coerceAtLeast(0.1),
                ((holder.hy as Double) * scale).coerceAtLeast(0.1),
                ((holder.hz as Double) * scale).coerceAtLeast(0.1),
            ),
        )
        opts.linearDamping = 0.04
        opts.angularDamping = 0.2
        // Shards don't collide with each other — their boxes all sit at the orb centre, so mutual
        // overlap would make the solver eject them explosively. They still rest on the ground.
        opts.collisionFilterGroup = SHARD_GROUP
        opts.collisionFilterMask = SHARD_MASK
        val body = Cannon.Body(opts)
        body.asDynamic().quaternion.setFromEuler(shatterRot[0], shatterRot[1], shatterRot[2])
        val a = Util.random() * 2.0 * PI
        val r = 0.02 + Util.random() * 0.06 // almost no outward push of their own
        val up = Util.random() * 0.08 // the faintest self-pop; the blast + gravity take over
        val push = blastPush(pos[0], pos[1], pos[2])
        body.asDynamic().velocity.set(cos(a) * r + push[0], sin(a) * r + push[1], up + push[2])
        body.asDynamic().angularVelocity.set(randSpin(), randSpin(), randSpin())
        world.addBody(body)
        group.add(mesh)
        activeShards.add(
            Shard(mesh, mat, body, 0.0, SHARD_LIFE_MIN + Util.random() * (SHARD_LIFE_MAX - SHARD_LIFE_MIN)) { f ->
                mat.uniforms.uFade.value = f // mat is already dynamic — no .asDynamic()
            },
        )
    }

    private fun updateShards(dt: Double) {
        world?.step(1.0 / 60.0, dt, 3)
        val iter = activeShards.iterator()
        while (iter.hasNext()) {
            val s = iter.next()
            s.age += dt
            val bodyPos = s.body.asDynamic().position
            s.mesh.position.set(bodyPos.x as Double, bodyPos.y as Double, bodyPos.z as Double)
            val bodyQuat = s.body.asDynamic().quaternion
            s.mesh.quaternion.set(bodyQuat.x as Double, bodyQuat.y as Double, bodyQuat.z as Double, bodyQuat.w as Double)
            if (s.age > s.life - SHARD_FADE) {
                s.setFade(((s.life - s.age) / SHARD_FADE).coerceIn(0.0, 1.0))
            }
            if (s.age >= s.life) {
                world?.removeBody(s.body)
                group.remove(s.mesh)
                s.mat.dispose()
                iter.remove()
            }
        }
    }

    private fun randSpin() = (Util.random() - 0.5) * 2.0 * SHARD_SPIN

    private fun createWorld(): Cannon.World {
        val w = Cannon.World()
        w.asDynamic().gravity.set(0.0, 0.0, -GRAVITY)
        val groundOpts: dynamic = js("({ mass: 0 })") // static ground plane at z=0 (normal +Z)
        groundOpts.shape = Cannon.Plane()
        w.addBody(Cannon.Body(groundOpts))
        return w
    }
}
