package system.display

import external.Three
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import system.display.shader.GlassShader
import kotlin.math.roundToInt

/**
 * Cached three.js materials for the scene's solid entities and portal parts, split out of
 * [Scene3D] to keep that class under the size limit. Keyed by colour so a faction's material is
 * built once and shared. (Transient shatter-shard materials stay in [Scene3D] — each shard needs
 * its own instance so it can fade independently.)
 */
object Materials {
    private val cache = mutableMapOf<String, dynamic>()

    /** Flat lit material for agents/NPCs. */
    fun solid(color: String): dynamic = cache.getOrPut("s$color") {
        val p: dynamic = js("({})")
        p.color = color
        Three.MeshStandardMaterial(p)
    }

    // Sky→ground gradient reflected by the chrome (a bare metal with nothing to reflect is black).
    private val envTex: dynamic by lazy { buildEnv() }

    /** Shiny off-tint chrome for the portal pole (the orb carries the faction colour). */
    fun metal(): dynamic = cache.getOrPut("chrome") {
        val p: dynamic = js("({})")
        p.color = "#c9ccd2" // cool off-white chrome
        p.metalness = 0.95
        p.roughness = 0.12
        p.envMap = envTex
        p.envMapIntensity = 2.0 // brighter reflection so the chrome reads silver, not black
        Three.MeshStandardMaterial(p)
    }

    /** The portal's glass orb (qlippostasis-style shader, faction-tinted). */
    fun glass(color: String): dynamic = cache.getOrPut("pg$color") { GlassShader.material(color) }

    /** Brighter glass variant for the thin link tubes (the orb glass reads too faint at pipe radius). */
    fun linkGlass(color: String): dynamic = cache.getOrPut("lg$color") { GlassShader.material(color, GlassShader.LINK_BRIGHT) }

    /** The link end-joint: the SAME glass as the link pipe, just a touch brighter/denser (uBright scales
     *  opacity too) — so it reads as a slightly-less-transparent joint, not the only fully-opaque thing. */
    fun linkNode(color: String): dynamic = cache.getOrPut("ln$color") { GlassShader.material(color, GlassShader.LINK_BRIGHT * 2.2) }

    /** Slottable-mod fill: translucent + self-luminous (an energy look, not the old shiny chrome). */
    fun modSolid(color: String): dynamic = cache.getOrPut("modf$color") {
        val p: dynamic = js("({})")
        p.color = color
        p.emissive = color
        p.emissiveIntensity = 0.6
        p.metalness = 0.0
        p.roughness = 1.0
        p.transparent = true
        p.opacity = 0.72 // slotted mods read fairly solid (was 0.5 — too see-through)
        Three.MeshStandardMaterial(p)
    }

    /** Bold, bright wireframe cage over a mod (rarity-coloured); two concentric copies fake a thicker
     *  line since WebGL ignores LineBasicMaterial.linewidth — the "×2 bolder" edges. */
    fun modWire(color: String): dynamic = cache.getOrPut("modw$color") {
        val p: dynamic = js("({})")
        p.color = color
        p.transparent = true
        p.opacity = 0.95
        Three.LineBasicMaterial(p)
    }

    /** Selected-portal orb: the faction-tinted glass lit brighter (selection highlight — no hue change). */
    fun glassBright(color: String): dynamic = cache.getOrPut("pgs$color") { GlassShader.material(color, GlassShader.SELECT_BRIGHT) }

    /** Additive white glow for stray-XM motes (XM is neutral — no faction hue). */
    fun xmGlow(): dynamic = cache.getOrPut("xmGlow") {
        val p: dynamic = js("({})")
        p.color = "#dfeaff"
        p.transparent = true
        p.opacity = 0.85
        p.blending = Three.AdditiveBlending
        p.depthWrite = false
        Three.MeshBasicMaterial(p)
    }

    /** Matte black rubber for the gasket between pole and orb (and the resonator slot rings). */
    fun rubber(): dynamic = cache.getOrPut("rubber") {
        val p: dynamic = js("({})")
        p.color = "#0a0a0a"
        p.metalness = 0.0
        p.roughness = 0.95
        Three.MeshStandardMaterial(p)
    }

    // Quantise the energy-bar fill so a faction colour reuses a handful of materials instead of one per
    // reso per frame. 32 steps is fine enough to read smooth — and crucially the resonator CAP disc reads
    // this same (stepped) uFill, so the sharp disc lines up with the bar exactly; a coarser step used to
    // leave the cap visibly above/below the true level. The stored uFill is the ROUNDED step value (not the
    // first health to hit the bucket), so it's deterministic and within ~1/64 of the real charge.
    private const val RESO_FILL_STEPS = 32

    /** Resonator rods: a glowing energy rod with a vertical bar — [fill] (0..1 health) lights it
     *  bottom→top (glows in colour), dim + see-through above the line. Cached per colour + fill step. */
    fun resonator(color: String, fill: Double): dynamic {
        val step = (fill * RESO_FILL_STEPS).roundToInt().coerceIn(0, RESO_FILL_STEPS)
        return cache.getOrPut("reso$color$step") {
            // Front faces only, and NO depth write: a translucent rod that doesn't occlude the energy-surface
            // cap discs (which draw just after it) — so a far reso's cap can't be hidden behind a nearer rod.
            // The opaque pole still hides what's truly behind it. (Falling shatter rods use their own material.)
            val m = GlassShader.material(color, GlassShader.LINK_BRIGHT, step.toDouble() / RESO_FILL_STEPS)
            m.side = 0 // Three.FrontSide
            m
        }
    }

    /** Resonator "energy surface": a glowing disc the rod-build positions at the current FILL LEVEL (the
     *  top of the charged part), so the reso reads as filled with a rising/falling surface, not just a fill
     *  line on the outside. Additive + faction-coloured. Keeps `depthTest` (so the opaque pole/buildings
     *  hide it — no see-through bleed); the rod-build draws it just AFTER the (now non-depth-writing) rods
     *  via a positive `renderOrder`, so its glow sits on top of the glass rather than being painted over or
     *  hidden behind a nearer resonator. Cached per colour. */
    fun resonatorCap(color: String): dynamic = cache.getOrPut("resocap$color") {
        val p: dynamic = js("({})")
        p.color = color
        p.transparent = true
        p.opacity = 0.85
        p.blending = Three.AdditiveBlending // glows
        p.depthWrite = false
        p.side = 2 // DoubleSide — readable as the rod swings on a hack
        Three.MeshBasicMaterial(p)
    }

    private fun buildEnv(): dynamic {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = 8
        canvas.height = 64
        val ctx = canvas.getContext("2d").asDynamic()
        val grad = ctx.createLinearGradient(0.0, 0.0, 0.0, 64.0)
        grad.addColorStop(0.0, "#ffffff") // bright sky → the chrome catches a highlight
        grad.addColorStop(0.5, "#c4cad2")
        grad.addColorStop(1.0, "#5c616a") // lighter ground (was near-black, so the poles read black)
        ctx.fillStyle = grad
        ctx.fillRect(0.0, 0.0, 8.0, 64.0)
        val tex = Three.CanvasTexture(canvas)
        tex.asDynamic().mapping = Three.EquirectangularReflectionMapping
        return tex
    }
}
