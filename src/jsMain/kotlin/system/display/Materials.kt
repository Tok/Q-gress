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
        p.envMapIntensity = 1.4
        Three.MeshStandardMaterial(p)
    }

    /** The portal's glass orb (qlippostasis-style shader, faction-tinted). */
    fun glass(color: String): dynamic = cache.getOrPut("pg$color") { GlassShader.material(color) }

    /** Brighter glass variant for the thin link tubes (the orb glass reads too faint at pipe radius). */
    fun linkGlass(color: String): dynamic = cache.getOrPut("lg$color") { GlassShader.material(color, GlassShader.LINK_BRIGHT) }

    /** Additive emissive filament running down the centre of a link tube — the "plasma core". */
    fun linkCore(color: String): dynamic = cache.getOrPut("lc$color") {
        val p: dynamic = js("({})")
        p.color = color
        p.transparent = true
        p.opacity = 0.85
        p.blending = Three.AdditiveBlending
        p.depthWrite = false
        Three.MeshBasicMaterial(p)
    }

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

    /** A resonator rod, coloured by its level (the rarity/level colour); slight emissive so it reads. */
    fun resonator(color: String): dynamic = cache.getOrPut("reso$color") {
        val p: dynamic = js("({})")
        p.color = color
        p.metalness = 0.3
        p.roughness = 0.5
        p.emissive = color
        p.emissiveIntensity = 0.35
        Three.MeshStandardMaterial(p)
    }

    private fun buildEnv(): dynamic {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = 8
        canvas.height = 64
        val ctx = canvas.getContext("2d").asDynamic()
        val grad = ctx.createLinearGradient(0.0, 0.0, 0.0, 64.0)
        grad.addColorStop(0.0, "#f4f6fb") // sky
        grad.addColorStop(0.55, "#9aa0aa")
        grad.addColorStop(1.0, "#2a2c30") // ground
        ctx.fillStyle = grad
        ctx.fillRect(0.0, 0.0, 8.0, 64.0)
        val tex = Three.CanvasTexture(canvas)
        tex.asDynamic().mapping = Three.EquirectangularReflectionMapping
        return tex
    }
}
