package system.display

import external.Three
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement

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

    /** Resonator rods: a glowing energy rod with a vertical bar — [fill] (0..1 health) lights it
     *  bottom→top (glows in colour), dim + see-through above the line. Cached per colour + fill octile. */
    fun resonator(color: String, fill: Double): dynamic = cache.getOrPut("reso$color${(fill * 8).toInt()}") {
        // Solid-ish glowing rod: front faces only + depth write so we don't see through to its inside.
        val m = GlassShader.material(color, GlassShader.LINK_BRIGHT, fill)
        m.depthWrite = true
        m.side = 0 // Three.FrontSide
        m
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
