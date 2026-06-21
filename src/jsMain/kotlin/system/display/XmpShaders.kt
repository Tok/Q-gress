package system.display

import external.Three

/**
 * GLSL ES source for the XMP "micro-nuke" detonation (see [XmpBurst]).
 *
 * The fireball is a **volumetric raymarch**: a bounding box mesh whose fragment shader reconstructs
 * the camera ray (unproject `gl_FragCoord` through `uInvProj`, fed each frame from the map's
 * projection), intersects a rising sphere, and marches a turbulent fbm field — emissive fire in the
 * hot core fading to dark/black smoke at the edges and over time. This gives real 3D structure
 * (not a textured donut surface). [RING_FRAG] is the flat neon ground shockwave.
 */
object XmpShaders {
    /** Build a transient additive/alpha FX ShaderMaterial (used for the ground ring). */
    fun material(vert: String, frag: String, uni: dynamic, additive: Boolean = true): dynamic {
        val p: dynamic = js("({})")
        p.vertexShader = vert
        p.fragmentShader = frag
        p.uniforms = uni
        p.transparent = true
        p.depthWrite = false
        p.depthTest = false
        p.blending = if (additive) Three.AdditiveBlending else Three.NormalBlending
        p.side = 2 // DoubleSide
        return Three.ShaderMaterial(p)
    }

    /** The volumetric fireball material: alpha-blended (so soot reads dark), BackSide so one
     * fragment per covered pixel marches the volume whether the camera is outside or inside the box. */
    fun volumeMaterial(uni: dynamic): dynamic {
        val p: dynamic = js("({})")
        p.vertexShader = RAYMARCH_VERT
        p.fragmentShader = VOLUME_FRAG
        p.uniforms = uni
        p.transparent = true
        p.depthWrite = false
        p.depthTest = false
        p.blending = Three.NormalBlending
        p.side = 1 // BackSide
        return Three.ShaderMaterial(p)
    }

    const val UV_VERT =
        "varying vec2 vUv;\n" +
            "void main() { vUv = uv; gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }"

    const val RAYMARCH_VERT =
        "void main() { gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }"

    // Shared value noise + fbm (iq) and the detonation colour ramp (black soot → ember → white-hot).
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
            // black soot → gray → ember → fire → white-hot
            "vec3 smokeColor(float h){ vec3 c = mix(vec3(0.012, 0.012, 0.016), vec3(0.12, 0.11, 0.13)," +
            " smoothstep(0.0, 0.32, h));\n" +
            " c = mix(c, vec3(0.55, 0.16, 0.03), smoothstep(0.32, 0.52, h));\n" +
            " c = mix(c, vec3(1.0, 0.5, 0.1), smoothstep(0.52, 0.74, h));\n" +
            " c = mix(c, vec3(1.0, 0.93, 0.7), smoothstep(0.82, 1.0, h)); return c; }\n"

    // Volumetric fire + smoke. Reconstruct the camera ray, intersect the rising fireball sphere
    // (uCenter, uRadius), and front-to-back composite a turbulent fbm density (emissive core, dark
    // sooty edges). Output is un-premultiplied colour + alpha for NormalBlending over the scene.
    private const val VOLUME_FRAG =
        GLSL_NOISE +
            "uniform mat4 uInvProj; uniform vec2 uResolution; uniform vec3 uCenter;\n" +
            "uniform float uRadius; uniform float uProgress; uniform float uTime; uniform float uSeed;\n" +
            "void main(){\n" +
            " vec2 ndc = (gl_FragCoord.xy / uResolution) * 2.0 - 1.0;\n" +
            " vec4 nh = uInvProj * vec4(ndc, -1.0, 1.0); vec3 ro = nh.xyz / nh.w;\n" +
            " vec4 fh = uInvProj * vec4(ndc, 1.0, 1.0); vec3 rd = normalize(fh.xyz / fh.w - ro);\n" +
            " vec3 oc = ro - uCenter; float b = dot(oc, rd); float c = dot(oc, oc) - uRadius * uRadius;\n" +
            " float disc = b * b - c; if (disc < 0.0) discard;\n" +
            " float sq = sqrt(disc); float t0 = max(-b - sq, 0.0); float t1 = -b + sq; if (t1 <= t0) discard;\n" +
            " float dt = (t1 - t0) / 26.0; vec4 acc = vec4(0.0);\n" +
            " for (int i = 0; i < 26; i++){\n" +
            "  vec3 lp = (ro + rd * (t0 + (float(i) + 0.5) * dt) - uCenter) / uRadius;\n" +
            "  float fall = 1.0 - dot(lp, lp); if (fall < 0.0) continue;\n" +
            "  vec3 q = lp * 2.6 + vec3(0.0, 0.0, -uTime * 1.1) + uSeed;\n" +
            "  float d = fbm(q) + fbm(q * 2.3 + uTime * 0.5) * 0.5; d *= 0.72;\n" +
            "  float density = clamp(d * 1.5 + fall * 0.5 - 0.62, 0.0, 1.0) * fall;\n" +
            "  if (density < 0.01) continue;\n" +
            "  float heat = clamp(fall * 1.1 + d * 0.4 - uProgress * 0.85, 0.0, 1.0);\n" +
            "  float a = density * 0.55;\n" +
            "  acc.rgb += (1.0 - acc.a) * smokeColor(heat) * a; acc.a += (1.0 - acc.a) * a;\n" +
            "  if (acc.a > 0.97) break;\n" +
            " }\n" +
            " acc.a *= 1.0 - smoothstep(0.65, 1.0, uProgress);\n" +
            " if (acc.a < 0.01) discard;\n" +
            " gl_FragColor = vec4(acc.rgb / max(acc.a, 0.001), acc.a); }"

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
