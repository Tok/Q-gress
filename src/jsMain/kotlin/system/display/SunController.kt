package system.display

import external.Three
import kotlin.math.cos
import kotlin.math.sin

/**
 * A moving "sun": one directional light that arcs across the sky, re-lighting the scene and (with the
 * renderer's shadow map on) casting real shadows from the portals + our own building meshes onto a
 * ground shadow-plane. It is NOT a real solar sim — just a light we sweep for spectacle: fast during the
 * intro, then easing to a slow drift. [register] once (sets up the light, shadow camera + ground
 * receiver, enables shadows on the renderer); [advance] each frame; [setSpeed] to retarget the drift.
 */
object SunController {
    private const val ELEV = 0.62 // sun elevation above the horizon (rad ≈ 35°) — long, readable shadows
    private const val FAST = 0.85 // intro azimuth speed (rad/s)
    private const val SLOW = 0.12 // settled drift speed (rad/s) — a full sweep ≈ 52s so shadows visibly move in-game
    private const val SPEED_EASE = 0.4 // per-second easing of the live speed toward its target
    private const val INTENSITY = 1.15 // directional strength (ambient is lowered so shadows read)
    private const val SHADOW_MAP = 2048

    private var az = 0.6 // current azimuth
    private var speed = FAST
    private var targetSpeed = FAST
    private var dist = 1000.0
    private var sun: dynamic = null

    /** Set up the sun, shadow camera + ground receiver. [spanMeters] ≈ play-area half-width; [groundZ]
     *  the height of the shadow-catching plane (the play-area ground). */
    fun register(scene: dynamic, renderer: dynamic, spanMeters: Double, groundZ: Double) {
        val r = spanMeters.coerceAtLeast(100.0)
        dist = r * 2.2

        renderer.shadowMap.enabled = true
        renderer.shadowMap.type = 1 // PCFShadowMap — crisper than PCFSoft (harder edges)

        val light = Three.DirectionalLight(0xffffff, INTENSITY)
        light.asDynamic().castShadow = true
        val shadow = light.asDynamic().shadow
        shadow.mapSize.width = SHADOW_MAP
        shadow.mapSize.height = SHADOW_MAP
        shadow.bias = -0.0004
        val cam = shadow.camera // an OrthographicCamera three positions at the light each frame
        cam.left = -r * 1.3
        cam.right = r * 1.3
        cam.top = r * 1.3
        cam.bottom = -r * 1.3
        cam.near = 1.0
        cam.far = dist * 2.5
        cam.updateProjectionMatrix()
        scene.add(light) // default target is (0,0,0) = sim centre → the sun always aims at the play area
        sun = light
        placeSun()

        // Transparent ground plane that only shows the shadows cast onto it (buildings + portals).
        val matOpts: dynamic = js("({})")
        matOpts.opacity = 0.55 // darker = harder-reading shadows
        matOpts.transparent = true
        val plane = Three.Mesh(Three.PlaneGeometry(r * 5.0, r * 5.0), Three.ShadowMaterial(matOpts))
        plane.asDynamic().position.z = groundZ + 0.05 // just above the ground so shadows sit on it
        plane.asDynamic().receiveShadow = true
        scene.add(plane)
    }

    /** Retarget the drift speed (FAST during the intro, SLOW once settled). */
    fun setSpeed(fast: Boolean) {
        targetSpeed = if (fast) FAST else SLOW
    }

    /** Advance the sun by real-time [dt] seconds (independent of sim speed). */
    fun advance(dt: Double) {
        sun ?: return
        speed += (targetSpeed - speed) * (SPEED_EASE * dt).coerceIn(0.0, 1.0)
        az += speed * dt
        placeSun()
    }

    private fun placeSun() {
        val light = sun ?: return // already dynamic — no .asDynamic() (it would throw)
        val ce = cos(ELEV)
        light.position.set(dist * ce * cos(az), dist * ce * sin(az), dist * sin(ELEV))
    }
}
