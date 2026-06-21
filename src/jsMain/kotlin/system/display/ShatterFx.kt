package system.display

import external.Cannon
import external.Three
import util.Util
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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

    private var world: Cannon.World? = null
    private var group: dynamic = null // shards + sinking poles (not cleared by sync)
    private var shatterRot = doubleArrayOf(0.0, 0.0, 0.0) // per-shatter random orientation
    private val activeShards = mutableListOf<Shard>()
    private val sinkingPoles = mutableListOf<SinkPole>()

    private class Shard(val mesh: dynamic, val mat: dynamic, val body: Cannon.Body, var age: Double, val life: Double, val setFade: (Double) -> Unit)
    private class SinkPole(val mesh: dynamic, val poleH: Double, var age: Double)

    /** Create the physics world + the shards group (once, when the scene is set up). */
    fun register(scene: Three.Scene) {
        group = Three.Group().also { scene.add(it) }
        world = createWorld()
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
        body.asDynamic().velocity.set((Util.random() - 0.5) * 3.0, (Util.random() - 0.5) * 3.0, -1.0)
        body.asDynamic().angularVelocity.set(randSpin() * 0.4, randSpin() * 0.4, randSpin() * 0.4)
        world.addBody(body)
        group.add(mesh)
        activeShards.add(
            Shard(mesh, mat, body, 0.0, SHARD_LIFE_MIN + Util.random() * (SHARD_LIFE_MAX - SHARD_LIFE_MIN)) { f -> mat.asDynamic().opacity = f },
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
        val r = 0.02 + Util.random() * 0.06 // almost no outward push
        val up = Util.random() * 0.08 // the faintest pop; gravity takes over
        body.asDynamic().velocity.set(cos(a) * r, sin(a) * r, up)
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
