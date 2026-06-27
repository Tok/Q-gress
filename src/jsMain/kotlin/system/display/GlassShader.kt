package system.display

import external.Three
import util.ColorUtil

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
 * opaque pass as a texture, which our custom layer doesn't expose — so we drop SSR (a follow-up).
 *
 * The Fresnel rim now **tracks the camera**: [updateEye] recovers the eye position (sim-space) each
 * frame from the map matrix, and the fragment shader computes the true per-fragment view direction
 * `uEye - worldPos`. Because the custom layer bakes the whole perspective transform into the
 * camera's `projectionMatrix` (the camera itself sits at the origin), `modelViewMatrix * position`
 * is already a **sim-space** position and `normalMatrix * normal` a sim-space normal — so the dot
 * is correct even under the link tubes' non-uniform Y scale, with no extra world-matrix juggling.
 */
object GlassShader {
    /** Brightness multiplier for the dedicated link variant (orb glass is near-transparent). */
    const val LINK_BRIGHT = 2.6

    /** Brightness multiplier for a selected portal's orb — keeps the faction hue, just glows brighter
     *  (so selection reads as "lit up", not as the neutral grayscale a white tint used to give). */
    const val SELECT_BRIGHT = 2.0

    // Camera eye in sim-space, shared by every glass material and refreshed per frame by updateEye().
    private val eyeUniform: dynamic = js("({ value: { x: 0.0, y: 0.0, z: 1.0e6 } })")

    /**
     * Recover the camera eye in **sim space** from the combined sim→clip [matrix] (three.js
     * `Matrix4`) and update the shared rim uniform; call once per frame. MapLibre 5.24 exposes no
     * free-camera API and the whole perspective is baked into that matrix, so we solve for the
     * camera centre directly: it is the null vector of the 3×4 projection formed by the x, y and w
     * rows of the 4×4 (the classic `P·C = 0` camera centre, with the depth row dropped).
     */
    fun updateEye(matrix: dynamic) {
        val e = matrix.elements // column-major, e[col*4 + row]

        // 3×3 determinant over columns (c0,c1,c2) using rows x(0), y(1), w(3).
        fun det3(c0: Int, c1: Int, c2: Int): Double {
            fun m(r: Int, c: Int): Double = e[c * 4 + r] as Double
            return m(0, c0) * (m(1, c1) * m(3, c2) - m(1, c2) * m(3, c1)) -
                m(0, c1) * (m(1, c0) * m(3, c2) - m(1, c2) * m(3, c0)) +
                m(0, c2) * (m(1, c0) * m(3, c1) - m(1, c1) * m(3, c0))
        }
        val cw = -det3(0, 1, 2)
        if (kotlin.math.abs(cw) < 1e-9) return // degenerate (e.g. pre-first-frame) — keep the last eye
        eyeUniform.value.x = det3(1, 2, 3) / cw
        eyeUniform.value.y = -det3(0, 2, 3) / cw
        eyeUniform.value.z = det3(0, 1, 3) / cw
    }

    /** The recovered camera eye in sim-space (metres), as last refreshed by [updateEye]. */
    fun eye(): DoubleArray = doubleArrayOf(
        eyeUniform.value.x as Double,
        eyeUniform.value.y as Double,
        eyeUniform.value.z as Double,
    )

    // Tunables (qlippostasis defaults, nudged for readability without its bloom/SSR).
    private const val RIM_STRENGTH = 2.6
    private const val RIM_POWER = 2.8
    private const val BASE_ALPHA = 0.08 // thin glass — mostly the rim/edges read, the body is see-through
    private const val SMUDGE_AMOUNT = 0.1
    private const val SMUDGE_SCALE = 0.6 // metres → our portals are ~10-20 m, not ~1 m
    private const val SMUDGE_THRESHOLD = 0.7
    private const val INTERIOR_LUM = 0.02
    private const val RIM_EMISSION = 0.6
    private const val INTERIOR_EMISSION = 0.05

    private const val VERT =
        "varying vec3 vWorldNormal;\nvarying vec3 vModelPos;\nvarying vec3 vWorldPos;\n" +
            "void main() { vModelPos = position;" +
            " vWorldPos = (modelViewMatrix * vec4(position, 1.0)).xyz;" + // sim-space here (see header)
            " vWorldNormal = normalize(normalMatrix * normal);" +
            " gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }"

    private val FRAG =
        "varying vec3 vWorldNormal;\nvarying vec3 vModelPos;\nvarying vec3 vWorldPos;\n" +
            "uniform vec3 uTint;\nuniform vec3 uEye;\nuniform float uBright;\nuniform float uFade;\nuniform float uFill;\n" +
            "float hash(vec3 p){ p = fract(p * vec3(0.1031, 0.1030, 0.0973)); p += dot(p, p.yxz + 33.33);" +
            " return fract((p.x + p.y) * p.z); }\n" +
            "float vnoise(vec3 p){ vec3 i = floor(p); vec3 f = fract(p); f = f * f * (3.0 - 2.0 * f);\n" +
            " return mix(mix(mix(hash(i), hash(i + vec3(1,0,0)), f.x), mix(hash(i + vec3(0,1,0))," +
            " hash(i + vec3(1,1,0)), f.x), f.y), mix(mix(hash(i + vec3(0,0,1)), hash(i + vec3(1,0,1)), f.x)," +
            " mix(hash(i + vec3(0,1,1)), hash(i + vec3(1,1,1)), f.x), f.y), f.z); }\n" +
            "void main(){\n" +
            // True view direction from the per-frame camera eye. abs(dot) → a thin silhouette RING
            // perpendicular to V (the glass edge lights up wherever the surface turns away).
            " vec3 V = normalize(uEye - vWorldPos);\n" +
            " float ndv = abs(dot(normalize(vWorldNormal), V));\n" +
            " float fres = pow(1.0 - ndv, ${RIM_POWER.glsl()});\n" +
            " float rim = fres * ${RIM_STRENGTH.glsl()};\n" +
            " float n = vnoise(vModelPos * ${SMUDGE_SCALE.glsl()});\n" +
            " float smudge = smoothstep(${SMUDGE_THRESHOLD.glsl()}, ${(SMUDGE_THRESHOLD + 0.16).glsl()}, n)" +
            " * ${SMUDGE_AMOUNT.glsl()};\n" +
            " float bright = clamp(rim + smudge * 0.4 + ${INTERIOR_LUM.glsl()}, 0.0, 1.0);\n" +
            " vec3 col = (vec3(bright) * uTint + uTint * (rim * ${RIM_EMISSION.glsl()} + ${INTERIOR_EMISSION.glsl()})) * uBright;\n" +
            " float alpha = clamp((${BASE_ALPHA.glsl()} + rim * 0.55 + smudge * 0.5) * uBright, 0.04, 0.97) * uFade;\n" +
            // Resonator energy rod: uFill in [0,1] is the charge (bottom→top); the default sentinel (2.0)
            // means "not a resonator" so orbs/links/shards skip this. The lit part GLOWS in the tint colour
            // (built straight from uTint, NOT uBright — so a light tint like L1 yellow can't blow out to
            // white) and is mostly opaque; above the fill line it stays coloured but dim + see-through.
            " if (uFill <= 1.0001) { float fillT = clamp(vModelPos.y + 0.5, 0.0, 1.0); bool full = fillT <= uFill;\n" +
            "   col = uTint * (full ? 0.8 : 0.16) + uTint * rim * 0.5;\n" +
            "   alpha = (full ? clamp(0.72 + rim * 0.25, 0.0, 0.95) : clamp(0.16 + rim * 0.18, 0.0, 0.5)) * uFade; }\n" +
            " gl_FragColor = vec4(col, alpha); }"

    /**
     * A glass [Three.ShaderMaterial] tinted with [hexColor] (e.g. a faction colour "#03DC03").
     * [bright] scales emission + opacity above the near-transparent orb default — use [LINK_BRIGHT]
     * for the thin link tubes, which would otherwise read too faint. Each call returns a fresh
     * instance, so its `uFade` uniform (1 → 0) can be driven independently — e.g. shatter shards
     * fading out at end of life.
     */
    fun material(hexColor: String, bright: Double = 1.0, fill: Double = 2.0): dynamic {
        val rgb = ColorUtil.hexToRgb(hexColor)
        val uni: dynamic = js("({ uTint: { value: null }, uBright: { value: 1.0 }, uFade: { value: 1.0 }, uFill: { value: 2.0 } })")
        uni.uTint.value = js("({ x: 0.0, y: 0.0, z: 0.0 })")
        uni.uTint.value.x = rgb[0]
        uni.uTint.value.y = rgb[1]
        uni.uTint.value.z = rgb[2]
        uni.uBright.value = bright
        uni.uFill.value = fill // 0..1 = resonator energy bar (bottom→top); 2 (default) = not a resonator
        uni.uEye = eyeUniform // shared, refreshed each frame by updateEye()
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
}
