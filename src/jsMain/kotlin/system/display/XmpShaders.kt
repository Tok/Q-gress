package system.display

import external.Three

/**
 * GLSL ES source for the XMP "micro-nuke" detonation (see [Scene3D.playXmpBurst]).
 *
 * three.js ShaderMaterial auto-injects precision plus the standard
 * `projectionMatrix` / `modelViewMatrix` / `position` / `uv` / `normal` declarations,
 * so each program below declares only its own varyings and uniforms.
 *
 * Three programs share one uniforms set (`uTime`, `uProgress` 0..1, `uSeed`):
 *  - [CAP_FRAG]  rolling fireball cap (torus) — a smoke vortex rotating around the tube,
 *               coloured by an ember→white-hot heat ramp with magenta/cyan synthwave fringe.
 *  - [CORE_FRAG] hot turbulent core (sphere) with a white flash spike in the first instants.
 *  - [RING_FRAG] neon ground shockwave ring (flat quad) racing outward over the terrain.
 */
object XmpShaders {
    /**
     * Build a transient-FX ShaderMaterial. [additive] true → glowing core/ring (light adds up);
     * false → normal alpha blend so the smoke's soot reads as genuinely *dark*. Depth test/write
     * are off (these float over the scene and must not z-fight the ground).
     */
    fun material(vert: String, frag: String, uni: dynamic, additive: Boolean = true): dynamic {
        val p: dynamic = js("({})")
        p.vertexShader = vert
        p.fragmentShader = frag
        p.uniforms = uni
        p.transparent = true
        p.depthWrite = false
        p.depthTest = false
        p.blending = if (additive) Three.AdditiveBlending else Three.NormalBlending
        p.side = 2 // DoubleSide — back faces fill out the volume
        return Three.ShaderMaterial(p)
    }

    // Vertex passing uv only (ground ring) and uv + local position (curved fireball surfaces).
    const val UV_VERT =
        "varying vec2 vUv;\n" +
            "void main() { vUv = uv; gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }"
    const val SURFACE_VERT =
        "varying vec2 vUv;\nvarying vec3 vPos;\n" +
            "void main() { vUv = uv; vPos = position;" +
            " gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }"

    // Shared value noise + fbm (iq) and the nuke heat ramp (ember → orange → white-hot).
    private const val GLSL_NOISE =
        "float hash(vec3 p){ p = fract(p * 0.3183099 + vec3(0.71, 0.113, 0.419)); p *= 17.0;" +
            " return fract(p.x * p.y * p.z * (p.x + p.y + p.z)); }\n" +
            "float noise(vec3 x){ vec3 i = floor(x); vec3 f = fract(x); f = f * f * (3.0 - 2.0 * f);\n" +
            " return mix(mix(mix(hash(i + vec3(0.0, 0.0, 0.0)), hash(i + vec3(1.0, 0.0, 0.0)), f.x)," +
            " mix(hash(i + vec3(0.0, 1.0, 0.0)), hash(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),\n" +
            " mix(mix(hash(i + vec3(0.0, 0.0, 1.0)), hash(i + vec3(1.0, 0.0, 1.0)), f.x)," +
            " mix(hash(i + vec3(0.0, 1.0, 1.0)), hash(i + vec3(1.0, 1.0, 1.0)), f.x), f.y), f.z); }\n" +
            "float fbm(vec3 p){ float a = 0.5; float s = 0.0;" +
            " for (int i = 0; i < 5; i++){ s += a * noise(p); p *= 2.02; a *= 0.5; } return s; }\n" +
            "vec3 heatToColor(float h){ vec3 c = mix(vec3(0.0), vec3(1.0, 0.3, 0.0), clamp(h * 4.0 - 0.5, 0.0, 1.0));\n" +
            " c = mix(c, vec3(1.0, 0.85, 0.35), clamp(h * 4.0 - 1.6, 0.0, 1.0));\n" +
            " c = mix(c, vec3(1.0, 0.97, 0.88), clamp(h * 6.0 - 3.4, 0.0, 1.0)); return c; }\n" +
            // Dirty detonation smoke (alpha-blended): black soot → gray → ember → fire → white-hot.
            "vec3 smokeColor(float h){ vec3 c = mix(vec3(0.015, 0.015, 0.02), vec3(0.16, 0.15, 0.18)," +
            " smoothstep(0.0, 0.34, h));\n" +
            " c = mix(c, vec3(0.5, 0.18, 0.05), smoothstep(0.34, 0.55, h));\n" +
            " c = mix(c, vec3(1.0, 0.5, 0.12), smoothstep(0.55, 0.78, h));\n" +
            " c = mix(c, vec3(1.0, 0.92, 0.72), smoothstep(0.84, 1.0, h)); return c; }\n"

    const val CAP_FRAG =
        GLSL_NOISE +
            "varying vec2 vUv;\nvarying vec3 vPos;\n" +
            "uniform float uTime; uniform float uProgress; uniform float uSeed;\n" +
            "void main(){ float roll = uTime * 2.4 + uProgress * 2.5;\n" +
            " float ta = vUv.y * 6.2831853 + roll; float ma = vUv.x * 6.2831853;\n" +
            " vec3 sp = vec3(cos(ma), sin(ta) * 0.6, sin(ma)) * 2.6 + vec3(uSeed, -uTime * 1.3, uSeed * 0.5);\n" +
            " float d = fbm(sp) + fbm(sp * 2.7 + uTime * 0.6) * 0.5; d *= 0.72;\n" +
            " float heat = clamp(d * 1.8 - uProgress * 0.7, 0.0, 1.0);\n" +
            " vec3 col = smokeColor(heat);\n" +
            " col += vec3(0.9, 0.06, 0.7) * smoothstep(0.5, 0.28, heat) * (1.0 - uProgress) * 0.5;\n" +
            " col += vec3(0.05, 0.5, 1.0) * smoothstep(0.3, 0.12, heat) * (1.0 - uProgress) * 0.4;\n" +
            " float a = clamp(d * 1.9 - 0.05, 0.0, 1.0) * (1.0 - uProgress * 0.8);\n" +
            " if (a < 0.02) discard; gl_FragColor = vec4(col, a); }"

    // Rising mushroom stem (tapered cylinder): turbulent smoke flowing upward, denser at the base.
    const val STEM_FRAG =
        GLSL_NOISE +
            "varying vec2 vUv;\nvarying vec3 vPos;\n" +
            "uniform float uTime; uniform float uProgress; uniform float uSeed;\n" +
            "void main(){ float ang = vUv.x * 6.2831853;\n" +
            " vec3 sp = vec3(cos(ang), vUv.y * 3.2 - uTime * 1.4, sin(ang)) * 2.0 + uSeed;\n" +
            " float d = fbm(sp) + fbm(sp * 2.4) * 0.4; d *= 0.74;\n" +
            " float heat = clamp(d * 1.7 - uProgress * 0.6 - vUv.y * 0.35, 0.0, 1.0);\n" +
            " vec3 col = smokeColor(heat);\n" +
            " col += vec3(0.9, 0.06, 0.7) * smoothstep(0.45, 0.25, heat) * (1.0 - uProgress) * 0.45;\n" +
            " float a = clamp(d * 1.9 - 0.1, 0.0, 1.0) * (1.0 - uProgress * 0.8) * smoothstep(1.0, 0.15, vUv.y);\n" +
            " if (a < 0.02) discard; gl_FragColor = vec4(col, a); }"

    const val CORE_FRAG =
        GLSL_NOISE +
            "varying vec2 vUv;\nvarying vec3 vPos;\n" +
            "uniform float uTime; uniform float uProgress; uniform float uSeed;\n" +
            "void main(){ vec3 q = vPos; float n = fbm(q * 2.7 + vec3(0.0, 0.0, -uTime * 1.6) + uSeed);\n" +
            " float r = length(q); float core = 1.0 - smoothstep(0.0, 1.0, r);\n" +
            " float heat = clamp(n * 1.1 + core * 0.9 - uProgress * 0.6, 0.0, 1.0);\n" +
            " float flash = 1.0 - smoothstep(0.0, 0.13, uProgress); heat = clamp(heat + flash * 0.9, 0.0, 1.0);\n" +
            " vec3 col = heatToColor(heat) * (1.0 + flash * 2.0); float edge = smoothstep(0.95, 0.3, r);\n" +
            " float a = (clamp(n * 1.2 - 0.1, 0.0, 1.0) * edge + flash * edge * 0.8) * (1.0 - uProgress);\n" +
            " if (a < 0.01) discard; gl_FragColor = vec4(col, a); }"

    const val RING_FRAG =
        "varying vec2 vUv;\nuniform float uProgress;\n" +
            "void main(){ vec2 p = vUv * 2.0 - 1.0; float d = length(p); if (d > 1.0) discard;\n" +
            " float ringR = uProgress; float w = 0.06 + uProgress * 0.06;\n" +
            " float ring = smoothstep(ringR - w, ringR, d) * (1.0 - smoothstep(ringR, ringR + w * 0.7, d));\n" +
            " vec3 col = mix(vec3(0.1, 0.75, 1.0), vec3(1.0, 0.1, 0.7), d);\n" +
            " float inner = (1.0 - smoothstep(0.0, max(ringR, 0.001), d)) * (1.0 - smoothstep(0.0, 0.22, uProgress));\n" +
            " float a = (ring * 1.5 + inner * 0.5) * (1.0 - uProgress);\n" +
            " if (a < 0.005) discard; gl_FragColor = vec4(col, a); }"
}
