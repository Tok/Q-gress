package system.display.shader

import external.Three
import util.ColorUtil

/**
 * Animated plasma material for control fields — a faction-tinted energy sheet whose brightness
 * ripples over the triangle (summed sine waves in scene-metre space). All field materials share one
 * `uTime` uniform, advanced each frame by [setTime] from [Scene3D].
 *
 * (Field create/teardown animations + sounds are a planned follow-up — they need a persistent
 * per-field lifecycle, shared with the portal growth animation.)
 */
object PlasmaShader {
    private val timeUniform: dynamic = js("({ value: 0.0 })")
    private val cache = mutableMapOf<String, dynamic>()

    /** Advance the shared animation clock (seconds); call once per frame. */
    fun setTime(t: Double) {
        timeUniform.value = t
    }

    /** A plasma [Three.ShaderMaterial] tinted with [hexColor] (faction colour). Cached per colour. */
    fun material(hexColor: String): dynamic = cache.getOrPut(hexColor) {
        val rgb = ColorUtil.hexToRgb(hexColor)
        val uni: dynamic = js("({ uTint: { value: { x: 0.0, y: 0.0, z: 0.0 } } })")
        uni.uTint.value.x = rgb[0]
        uni.uTint.value.y = rgb[1]
        uni.uTint.value.z = rgb[2]
        uni.uTime = timeUniform // shared across all plasma materials
        val p: dynamic = js("({})")
        p.vertexShader = VERT
        p.fragmentShader = FRAG
        p.uniforms = uni
        p.transparent = true
        p.depthWrite = false
        p.side = 2 // DoubleSide
        p.blending = Three.NormalBlending
        Three.ShaderMaterial(p)
    }

    // Each field vertex carries its portal's health (0..1) in the `aHealth` attribute; the GPU interpolates it
    // across the triangle so the sheet fades toward a low-health corner (a weak link reads near-transparent)
    // and stays full near a healthy one. MIN_HEALTH_ALPHA is the floor so a dying field never fully vanishes.
    private const val VERT =
        "attribute float aHealth;\n" +
            "varying vec3 vPos;\nvarying float vHealth;\n" +
            "void main() { vPos = position; vHealth = aHealth;" +
            " gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }"

    private const val FRAG =
        "varying vec3 vPos;\nvarying float vHealth;\nuniform float uTime;\nuniform vec3 uTint;\n" +
            "const float MIN_HEALTH_ALPHA = 0.1;\n" +
            "void main(){ vec2 p = vPos.xy * 0.08;\n" +
            " float v = sin(p.x + uTime) + sin(p.y * 1.1 - uTime * 1.3)" +
            " + sin((p.x + p.y) * 0.7 + uTime * 0.8) + sin(length(p) * 1.6 - uTime * 1.2);\n" +
            " v = v * 0.25 + 0.5;\n" +
            " vec3 col = uTint * (0.45 + v * 0.85);\n" +
            " float a = (0.16 + v * 0.2) * mix(MIN_HEALTH_ALPHA, 1.0, clamp(vHealth, 0.0, 1.0));\n" +
            " gl_FragColor = vec4(col, a); }"
}
