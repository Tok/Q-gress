package system.display

import external.Three

/**
 * Abstract "lab-glass" material, ported from qlippostasis's `glass.gdshader` and adapted for our
 * MapLibre custom-layer camera. The whole world should read as one glass apparatus — portals
 * (flask/mushroom), the tubes between them, and the shatter shards all share this look.
 *
 * Faithful to the original where we can: a **Fresnel rim** (bright at grazing angles) plus manual
 * emission, **procedural model-space smudges** (vnoise "fingerprints on glass"), `DoubleSide` so
 * the far wall shows through, and no depth write. The vessel is grayscale; the only colour is the
 * per-instance **tint** (the faction colour — the "Queen-Scale" exception).
 *
 * One unavoidable difference: Godot's screen-space refraction (`hint_screen_texture`) needs the
 * opaque pass as a texture, which our custom layer doesn't expose — so we approximate the view
 * direction from the world-space normal's verticality (Z is up) instead of the true camera ray.
 * Good enough for the rim; true SSR is a follow-up.
 */
object GlassShader {
    // Tunables (qlippostasis defaults, nudged for readability without its bloom/SSR).
    private const val RIM_STRENGTH = 2.2
    private const val RIM_POWER = 2.6
    private const val BASE_ALPHA = 0.34 // well above Godot's 0.04 — must read over a busy satellite map (no bloom)
    private const val SMUDGE_AMOUNT = 0.12
    private const val SMUDGE_SCALE = 0.6 // metres → our portals are ~10-20 m, not ~1 m
    private const val SMUDGE_THRESHOLD = 0.7
    private const val INTERIOR_LUM = 0.14
    private const val RIM_EMISSION = 0.7
    private const val INTERIOR_EMISSION = 0.28

    private const val VERT =
        "varying vec3 vWorldNormal;\nvarying vec3 vModelPos;\n" +
            "void main() { vModelPos = position; vWorldNormal = normalize(normalMatrix * normal);" +
            " gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }"

    private val FRAG =
        "varying vec3 vWorldNormal;\nvarying vec3 vModelPos;\n" +
            "uniform vec3 uTint;\n" +
            "float hash(vec3 p){ p = fract(p * vec3(0.1031, 0.1030, 0.0973)); p += dot(p, p.yxz + 33.33);" +
            " return fract((p.x + p.y) * p.z); }\n" +
            "float vnoise(vec3 p){ vec3 i = floor(p); vec3 f = fract(p); f = f * f * (3.0 - 2.0 * f);\n" +
            " return mix(mix(mix(hash(i), hash(i + vec3(1,0,0)), f.x), mix(hash(i + vec3(0,1,0))," +
            " hash(i + vec3(1,1,0)), f.x), f.y), mix(mix(hash(i + vec3(0,0,1)), hash(i + vec3(1,0,1)), f.x)," +
            " mix(hash(i + vec3(0,1,1)), hash(i + vec3(1,1,1)), f.x), f.y), f.z); }\n" +
            "void main(){\n" +
            " float ndv = abs(normalize(vWorldNormal).z);\n" + // 1 facing camera (top-down), 0 at silhouette
            " float fres = pow(1.0 - ndv, ${RIM_POWER.glsl()});\n" +
            " float rim = fres * ${RIM_STRENGTH.glsl()};\n" +
            " float n = vnoise(vModelPos * ${SMUDGE_SCALE.glsl()});\n" +
            " float smudge = smoothstep(${SMUDGE_THRESHOLD.glsl()}, ${(SMUDGE_THRESHOLD + 0.16).glsl()}, n)" +
            " * ${SMUDGE_AMOUNT.glsl()};\n" +
            " float bright = clamp(rim + smudge * 0.4 + ${INTERIOR_LUM.glsl()}, 0.0, 1.0);\n" +
            " vec3 col = vec3(bright) * uTint + uTint * (rim * ${RIM_EMISSION.glsl()} + ${INTERIOR_EMISSION.glsl()});\n" +
            " float alpha = clamp(${BASE_ALPHA.glsl()} + rim * 0.55 + smudge * 0.5, 0.04, 0.95);\n" +
            " gl_FragColor = vec4(col, alpha); }"

    /** A glass [Three.ShaderMaterial] tinted with [hexColor] (e.g. a faction colour "#03DC03"). */
    fun material(hexColor: String): dynamic {
        val rgb = hexToRgb(hexColor)
        val uni: dynamic = js("({ uTint: { value: null } })")
        uni.uTint.value = js("({ x: 0.0, y: 0.0, z: 0.0 })")
        uni.uTint.value.x = rgb[0]
        uni.uTint.value.y = rgb[1]
        uni.uTint.value.z = rgb[2]
        val p: dynamic = js("({})")
        p.vertexShader = VERT
        p.fragmentShader = FRAG
        p.uniforms = uni
        p.transparent = true
        p.depthWrite = false
        p.blending = Three.NormalBlending
        p.side = 2 // DoubleSide — the far wall shows through, the "thick glass" cue
        return Three.ShaderMaterial(p)
    }

    private fun hexToRgb(hex: String): DoubleArray {
        val h = hex.removePrefix("#")
        val r = h.substring(0, 2).toInt(16) / 255.0
        val g = h.substring(2, 4).toInt(16) / 255.0
        val b = h.substring(4, 6).toInt(16) / 255.0
        return doubleArrayOf(r, g, b)
    }
}

/** Render a Double as a GLSL float literal (always with a decimal point). */
private fun Double.glsl(): String {
    val s = this.toString()
    return if (s.contains('.') || s.contains('e') || s.contains('E')) s else "$s.0"
}
