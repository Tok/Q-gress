package system.display.fx

import external.Three
import system.display.Scene3D
import kotlin.math.PI
import kotlin.math.sin

/**
 * A short **white-steam puff** released from a portal's flask top when an agent **burns it out** (over-hacks
 * it) — a one-shot release, like steam venting from an industrial smoke stack, NOT a persistent status plume.
 * White (to read distinctly from the fiery XMP explosions). A burst of a few soft puffs rise, drift, expand
 * and fade over ~1.5 s. Absolute-time driven (tracks sim speed via [Scene3D.animMs]).
 */
object SmokeFx {
    private const val PUFFS_PER_BURST = 5
    private const val PUFF_LIFE_S = 1.5
    private const val RISE = 16.0 // metres a puff climbs over its life
    private const val GROW = 2.8 // radius growth factor over life
    private const val START_R = 2.2
    private const val DRIFT = 3.5 // lateral wander amplitude
    private const val STAGGER_S = 0.06 // delay between puffs in a burst
    private const val PEAK_ALPHA = 0.45 // steam is wispy, never fully opaque
    private const val MAX_PUFFS = 160 // safety cap
    private const val TAU = 6.28318

    private var group: dynamic = null
    private val geo: dynamic by lazy { Three.SphereGeometry(START_R, 7, 7) }

    private class Puff(val mesh: dynamic, val from: DoubleArray, val start: Double, val seed: Double)

    private val puffs = mutableListOf<Puff>()

    fun register(scene: Three.Scene) {
        group = Three.Group().also { scene.add(it) }
        puffs.clear()
    }

    fun hasActive() = puffs.isNotEmpty()

    /** Release a one-shot steam burst from [top] (the flask-top scene position). */
    fun puff(top: DoubleArray) {
        val g = group ?: return
        val now = Scene3D.animMs() / 1000.0
        repeat(PUFFS_PER_BURST) { i ->
            if (puffs.size >= MAX_PUFFS) return
            val seed = (puffs.size * 0.61 + i * 0.27) % 1.0 // vary per puff without Math.random (resume-safe)
            val mesh = Three.Mesh(geo, steamMaterial(seed))
            mesh.asDynamic().position.set(top[0], top[1], top[2])
            g.add(mesh)
            puffs.add(Puff(mesh, top, now + i * STAGGER_S, seed))
        }
    }

    fun update() {
        val now = Scene3D.animMs() / 1000.0
        val iter = puffs.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            val t = ((now - p.start) / PUFF_LIFE_S).coerceIn(0.0, 1.0)
            val wob = sin(t * PI * 2 + p.seed * TAU) * DRIFT * t
            p.mesh.position.set(p.from[0] + wob, p.from[1] + wob * 0.5, p.from[2] + RISE * t)
            val s = 1.0 + GROW * t
            p.mesh.scale.set(s, s, s)
            p.mesh.material.uniforms.uAlpha.value = sin(t * PI) * PEAK_ALPHA // swell in, fade out
            if (t >= 1.0) {
                group.remove(p.mesh)
                iter.remove()
            }
        }
    }

    private fun steamMaterial(seed: Double): dynamic {
        val uni: dynamic = js("({ uAlpha: { value: 0.0 }, uSeed: { value: 0.0 } })")
        uni.uSeed.value = seed
        val p: dynamic = js("({})")
        p.vertexShader = VERT
        p.fragmentShader = FRAG
        p.uniforms = uni
        p.transparent = true
        p.depthWrite = false
        return Three.ShaderMaterial(p)
    }

    private const val VERT =
        "varying vec3 vN;\nvarying vec3 vView;\nvarying vec3 vPos;\n" +
            "void main(){ vN = normalize(normalMatrix * normal); vec4 mv = modelViewMatrix * vec4(position, 1.0);" +
            " vView = -mv.xyz; vPos = position; gl_Position = projectionMatrix * mv; }"

    // White steam: soft (rim-faded) puff with a touch of hash break-up so it doesn't read as a hard sphere.
    private const val FRAG =
        "precision mediump float;\nvarying vec3 vN;\nvarying vec3 vView;\nvarying vec3 vPos;\n" +
            "uniform float uAlpha;\nuniform float uSeed;\n" +
            "float hash(vec3 p){ return fract(sin(dot(p, vec3(12.9898, 78.233, 37.719))) * 43758.5453); }\n" +
            "void main(){ float facing = abs(dot(normalize(vN), normalize(vView)));\n" +
            " float n = hash(floor(vPos * 0.7) + uSeed * 7.0);\n" +
            " float a = uAlpha * pow(facing, 1.5) * (0.7 + 0.3 * n);\n" +
            " vec3 col = mix(vec3(0.82), vec3(0.98), n);\n" +
            " gl_FragColor = vec4(col, a); }"
}
