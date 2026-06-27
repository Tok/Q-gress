package system.display

import external.Three
import util.ColorUtil

/**
 * A classic sci-fi **shield bubble** for a shielded portal: a translucent sphere shell (rendered at
 * ~1.618× the orb radius) with a camera-tracking Fresnel rim and a faintly animated **hex-cell**
 * pattern that pulses. Additive + no depth write so it reads as an energy field around the vessel.
 *
 * Colour rule: an off-tint grayscale base lightly pushed toward the portal's faction colour (it's a
 * faction thing). [intensity] (0..1, from the portal's total mitigation) scales how dense/bright the
 * bubble reads. The time + camera-eye uniforms are shared and refreshed once per frame
 * ([setTime]/[setEye] from [Scene3D.render]); the eye reuses [GlassShader]'s recovered camera centre.
 */
object ShieldShader {
    private const val RIM_POWER = 2.2
    private const val HEX_SCALE = 2.5 // hex cells per radian of arc (lon/lat space → equilateral hexes)

    private val timeUniform: dynamic = js("({ value: 0.0 })")
    private val eyeUniform: dynamic = js("({ value: { x: 0.0, y: 0.0, z: 1.0e6 } })")

    fun setTime(seconds: Double) {
        timeUniform.value = seconds
    }

    fun setEye(eye: DoubleArray) {
        eyeUniform.value.x = eye[0]
        eyeUniform.value.y = eye[1]
        eyeUniform.value.z = eye[2]
    }

    /** Set a shield material's per-bubble ripple amplitude (0 = smooth sphere, 1 = full absorb wobble). */
    fun setWave(material: dynamic, amplitude: Double) {
        material.uniforms.uWave.value = amplitude
    }

    private const val VERT =
        "varying vec3 vWorldPos;\nvarying vec3 vNormal;\nvarying vec3 vModelPos;\n" +
            "uniform float uTime;\nuniform float uWave;\n" + // uWave (1→0) ripples the shell as a blast is absorbed
            "void main(){\n" +
            " vec3 p = position;\n" +
            // Liquid ripple while the shield soaks up a nearby (non-fatal) blast — surface waves along the
            // normal from crossing sines, scaled by uWave so it settles back to a smooth sphere.
            " float ripple = sin(position.x * 0.8 + position.z * 0.8 - uTime * 9.0) + sin(position.y * 1.6 - uTime * 7.0);\n" +
            " p += normal * uWave * ripple * 0.04 * length(position);\n" +
            " vModelPos = p;\n" +
            " vWorldPos = (modelViewMatrix * vec4(p, 1.0)).xyz;" + // sim-space (see GlassShader header)
            " vNormal = normalize(normalMatrix * normal);" +
            " gl_Position = projectionMatrix * modelViewMatrix * vec4(p, 1.0); }"

    private val FRAG =
        "varying vec3 vWorldPos;\nvarying vec3 vNormal;\nvarying vec3 vModelPos;\n" +
            "uniform vec3 uTint;\nuniform vec3 uEye;\nuniform float uTime;\nuniform float uIntensity;\nuniform float uWave;\n" +
            // Distance to the nearest hex-cell edge (small = on a line) — the classic shield lattice.
            "float hexGrid(vec2 p){ p.x *= 1.1547; p.y += mod(floor(p.x), 2.0) * 0.5;" +
            " p = abs(fract(p) - 0.5); return abs(max(p.x * 1.5 + p.y, p.y * 2.0) - 1.0); }\n" +
            "void main(){\n" +
            " vec3 V = normalize(uEye - vWorldPos);\n" +
            " float ndv = abs(dot(normalize(vNormal), V));\n" +
            " float fres = pow(1.0 - ndv, ${RIM_POWER.glsl()});\n" +
            // Near hemisphere bright, far hemisphere dim → the bubble reads as a 3D shell, not a flat decal.
            " float facing = gl_FrontFacing ? 1.0 : 0.4;\n" +
            // A soft body sheen that falls off toward the centre (where we look straight through) → volume.
            " float body = 0.25 + 0.45 * fres + 0.18 * (1.0 - ndv);\n" +
            " vec3 p = normalize(vModelPos);\n" +
            // sphere → (lon, lat) radians: equal arc length → equilateral hexes
            " vec2 uv = vec2(atan(p.y, p.x), asin(clamp(p.z, -1.0, 1.0)));\n" +
            " float h = hexGrid(uv * ${HEX_SCALE.glsl()} + vec2(uTime * 0.04, 0.0));\n" +
            " float line = smoothstep(0.05, 0.0, h);\n" + // crisp cell edge
            " float halo = smoothstep(0.20, 0.0, h);\n" + // soft glow around it (fake single-pass bloom)
            " float hex = line + halo * 0.5;\n" +
            " float pulse = 0.6 + 0.4 * sin(uTime * 2.0);\n" +
            " vec3 base = mix(vec3(0.65), uTint, 0.5);\n" + // off-tint grayscale leaning faction
            " float glow = fres * 1.8 + hex * 0.7 + body * 0.5 + uWave * 0.6;\n" + // flares brighter as it absorbs
            " vec3 col = clamp(base * glow * (0.5 + 0.8 * uIntensity) * pulse * facing, 0.0, 1.0);\n" +
            " col = col * col * (3.0 - 2.0 * col);\n" + // bloom-style tonemap (from the inspiration)
            " float alpha = clamp(fres * 0.55 + hex * 0.4 + body * 0.15 + uWave * 0.18, 0.0, 0.95) * (0.4 + 0.6 * uIntensity) * facing;\n" +
            " gl_FragColor = vec4(col, alpha); }"

    /** A shield-bubble material tinted by [hexColor]; [intensity] (0..1) scales density/brightness. */
    fun material(hexColor: String, intensity: Double): dynamic {
        val rgb = ColorUtil.hexToRgb(hexColor)
        val uni: dynamic = js("({ uTint: { value: null }, uIntensity: { value: 1.0 } })")
        uni.uTint.value = js("({ x: 0.0, y: 0.0, z: 0.0 })")
        uni.uTint.value.x = rgb[0]
        uni.uTint.value.y = rgb[1]
        uni.uTint.value.z = rgb[2]
        uni.uIntensity.value = intensity
        uni.uWave = js("({ value: 0.0 })") // per-material blast-absorption ripple (1→0); driven by ShieldWave
        uni.uTime = timeUniform // shared, refreshed each frame
        uni.uEye = eyeUniform // shared with GlassShader's recovered camera centre
        val p: dynamic = js("({})")
        p.vertexShader = VERT
        p.fragmentShader = FRAG
        p.uniforms = uni
        p.transparent = true
        p.depthWrite = false
        p.blending = Three.AdditiveBlending
        p.side = 2 // DoubleSide — see both faces of the bubble
        return Three.ShaderMaterial(p)
    }
}
