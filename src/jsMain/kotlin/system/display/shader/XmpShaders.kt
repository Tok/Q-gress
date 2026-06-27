package system.display.shader

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
        p.depthTest = false // always on top: a flat near-ground ring depth-tested vs the terrain z-fights/vanishes
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
        // Always on top. The volume is raymarched on the box's BACK faces (so it works inside or outside
        // the box), so the fragment's geometric depth is the FAR box face — which sits below the terrain.
        // Depth-testing that against the shared map depth buffer fails almost everywhere → the fireball
        // vanishes behind the ground. Proper building-only occlusion needs gl_FragDepth (sphere-entry
        // depth) + a building depth pass; deferred to the own-mesh building work.
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
            // Rotate each octave (iq's trick) so the fbm has no axis-aligned grid streaks — the old
            // straight `p *= 2.02` is what made the fireball read soft/blocky rather than turbulent.
            "const mat3 NROT = mat3(0.00, 0.80, 0.60, -0.80, 0.36, -0.48, -0.60, -0.48, 0.64);\n" +
            "float fbm(vec3 p){ float a = 0.5; float s = 0.0;" +
            " for (int i = 0; i < 5; i++){ s += a * noise(p); p = NROT * p * 2.02; a *= 0.5; } return s; }\n" +
            // dark smoke → ember → orange → bright fire → white-hot. Warm, not near-black: an explosion
            // reads as fire with a little smoke, not a black ball.
            "vec3 smokeColor(float h){ vec3 c = mix(vec3(0.05, 0.04, 0.05), vec3(0.28, 0.12, 0.07)," +
            " smoothstep(0.0, 0.3, h));\n" +
            " c = mix(c, vec3(0.95, 0.32, 0.06), smoothstep(0.3, 0.55, h));\n" +
            " c = mix(c, vec3(1.0, 0.62, 0.16), smoothstep(0.55, 0.78, h));\n" +
            " c = mix(c, vec3(1.0, 0.95, 0.82), smoothstep(0.78, 1.0, h)); return c; }\n"

    // Volumetric fire + smoke. Reconstruct the camera ray, intersect the rising fireball sphere
    // (uCenter, uRadius), and front-to-back composite a turbulent field. Two changes give it real 3D
    // structure instead of a soft blob: (1) the surface is **pyroclastically displaced** — fbm pushes
    // the unit-sphere boundary in/out, so the silhouette is lumpy billows, not a smooth ball; (2) each
    // sample is **lit** by the gradient of that field (a cheap normal + key light), so the billows
    // self-shade and read as volume. Output is un-premultiplied colour + alpha for NormalBlending.
    private const val VOLUME_FRAG =
        GLSL_NOISE +
            "uniform mat4 uInvProj; uniform vec2 uResolution; uniform vec3 uCenter;\n" +
            "uniform float uRadius; uniform float uProgress; uniform float uTime; uniform float uSeed;\n" +
            "uniform float uBright;\n" + // overall brightness (1.0 normal; ultra-strike a touch higher)
            "const int STEPS = 30;\n" +
            "const float NOISE_FREQ = 3.0;\n" +
            "const float DISPLACE = 1.3;\n" + // strong → lumpy, non-spherical billows (pyroclastic)
            "const float DENSITY_GAIN = 1.5;\n" +
            // The field morphs from an initial fireball (a ball) into a rising mushroom: a torus 'cap'
            // (the rising donut/vortex ring) that climbs + spreads with uProgress, plus a fattening
            // stem below it. lp is the unit-sphere local point (z up); fbm displaces it into billows.
            "float fireFull(vec3 lp){\n" +
            " vec3 q = lp * NOISE_FREQ + vec3(0.0, 0.0, -uTime * 1.3) + uSeed;\n" +
            " float ball = 1.0 - dot(lp, lp);\n" +
            " float rr = 0.2 + 0.45 * uProgress;\n" + // cap ring radius (spreads as it rises)
            " float tt = 0.55 - 0.18 * uProgress;\n" + // cap tube thickness (thins as it spreads)
            " float ch = -0.1 + 0.45 * uProgress;\n" + // cap height within the volume (climbs)
            " float cap = 1.0 - length(vec2(length(lp.xy) - rr, lp.z - ch)) / tt;\n" + // the donut
            " float stem = (1.0 - length(lp.xy) / 0.24) * clamp(0.5 - lp.z * 0.6, 0.0, 1.0);\n" +
            " float mush = max(cap, stem);\n" +
            " float shape = mix(ball, mush, smoothstep(0.12, 0.55, uProgress));\n" + // ball → mushroom
            " return shape + (fbm(q) - 0.5) * DISPLACE; }\n" +
            "void main(){\n" +
            " vec2 ndc = (gl_FragCoord.xy / uResolution) * 2.0 - 1.0;\n" +
            " vec4 nh = uInvProj * vec4(ndc, -1.0, 1.0); vec3 ro = nh.xyz / nh.w;\n" +
            " vec4 fh = uInvProj * vec4(ndc, 1.0, 1.0); vec3 rd = normalize(fh.xyz / fh.w - ro);\n" +
            " vec3 oc = ro - uCenter; float b = dot(oc, rd); float c = dot(oc, oc) - uRadius * uRadius;\n" +
            " float disc = b * b - c; if (disc < 0.0) discard;\n" +
            " float sq = sqrt(disc); float t0 = max(-b - sq, 0.0); float t1 = -b + sq; if (t1 <= t0) discard;\n" +
            " float dt = (t1 - t0) / float(STEPS); vec4 acc = vec4(0.0);\n" +
            " vec3 lgt = normalize(vec3(0.4, 0.5, 0.75));\n" + // key light: up + toward the camera
            " for (int i = 0; i < STEPS; i++){\n" +
            "  vec3 lp = (ro + rd * (t0 + (float(i) + 0.5) * dt) - uCenter) / uRadius;\n" +
            "  float full = fireFull(lp); if (full <= 0.0) continue;\n" + // outside the displaced surface
            "  float density = clamp(full * DENSITY_GAIN, 0.0, 1.0);\n" +
            "  if (density < 0.02) continue;\n" +
            "  float e = 0.08;\n" + // gradient → surface normal (only paid where there's something to shade)
            "  vec3 grad = vec3(fireFull(lp + vec3(e, 0.0, 0.0)) - full," +
            " fireFull(lp + vec3(0.0, e, 0.0)) - full, fireFull(lp + vec3(0.0, 0.0, e)) - full);\n" +
            "  vec3 nrm = normalize(-grad + vec3(0.0, 0.0, 1e-3));\n" + // outward normal (defaults to 'up' when flat)
            "  float diff = clamp(dot(nrm, lgt) * 0.5 + 0.6, 0.0, 1.2);\n" +
            "  float heat = clamp(full * 1.5 + 0.12 - uProgress * 0.7, 0.0, 1.0);\n" +
            "  float shade = mix(diff, 1.15, heat);\n" + // hot core stays bright/emissive; cool smoke is shaded
            "  vec3 col = smokeColor(heat) * shade;\n" +
            "  float a = density * 0.62 * (0.35 + 0.65 * heat);\n" + // cool smoke translucent, fire opaque
            "  acc.rgb += (1.0 - acc.a) * col * a; acc.a += (1.0 - acc.a) * a;\n" +
            "  if (acc.a > 0.97) break;\n" +
            " }\n" +
            // Gradual dissolve: cool the colour + thin the alpha across the back half of the life
            // (a long smoke tail), instead of a hard cut near the end.
            " acc.rgb *= mix(1.0, 0.45, smoothstep(0.35, 1.0, uProgress));\n" +
            " acc.a *= 1.0 - smoothstep(0.45, 1.0, uProgress);\n" +
            " if (acc.a < 0.01) discard;\n" +
            " gl_FragColor = vec4(acc.rgb / max(acc.a, 0.001) * uBright, acc.a); }"

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
