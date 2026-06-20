package system.display

import external.Three

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

    /** Brushed metal for the portal pole (faction-tinted). */
    fun metal(color: String): dynamic = cache.getOrPut("m$color") {
        val p: dynamic = js("({})")
        p.color = color
        p.metalness = 0.9
        p.roughness = 0.35
        Three.MeshStandardMaterial(p)
    }

    /** The portal's glass orb (qlippostasis-style shader, faction-tinted). */
    fun glass(color: String): dynamic = cache.getOrPut("pg$color") { GlassShader.material(color) }

    /** Matte black rubber for the gasket between pole and orb. */
    fun rubber(): dynamic = cache.getOrPut("rubber") {
        val p: dynamic = js("({})")
        p.color = "#0a0a0a"
        p.metalness = 0.0
        p.roughness = 0.95
        Three.MeshStandardMaterial(p)
    }
}
