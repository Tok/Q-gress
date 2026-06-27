package system.display

import agent.Faction
import external.Three
import portal.Octant
import system.audio.Sound
import system.display.fx.HackFx
import util.data.Pos

/**
 * The title-screen **demo/showcase sandbox**, split out of [Scene3D] (which was a `LargeClass`): a separate
 * set of placed portals that live in their own group and are **immune to the live `sync`**, so the player can
 * place / select / hack / detonate / link / flip them freely while the real world ticks behind. Owns its own
 * group + animation state; calls back into [Scene3D] for the shared build primitives (portal/resonator meshes,
 * coordinate transforms, effects) that the live renderer and the demo share.
 */
object Showcases {
    private const val SELECT_R = 55.0 // demo: click within this (sim px) selects a portal / min place gap
    private const val FLIP_S = 1.0 // demo: seconds for a virus-flip orb colour morph

    private var group: dynamic = null // demo-scene placed portals (not cleared by sync)
    private var cursor: dynamic = null // ground ring under the mouse (place vs select)

    private class Showcase(
        val obj: dynamic,
        val parts: Array<dynamic>,
        val pos: Pos,
        var level: Int,
        var color: String, // mutable: a virus flip morphs it to the new faction colour
        var hackAge: Double,
        var growAge: Double,
    ) {
        var hackGlyph: Boolean = false // the active hack is a (stronger) glyph hack
        var flipAge: Double = 0.0 // seconds left in a virus-flip colour morph (counts down)
        var flipFrom: String? = null // the colour the orb is morphing FROM during a flip
    }

    private class DemoLink(val a: Showcase, val b: Showcase, val pipe: dynamic)

    private val placed = mutableListOf<Showcase>()
    private val demoLinks = mutableListOf<DemoLink>() // demo link pipes, so they can't outlive their portals
    private var selected: Showcase? = null // the portal the action buttons act on

    /** Attach a fresh demo group to a (re)built scene and drop any state from the old one. */
    fun register(scene: dynamic) {
        group = Three.Group()
        scene.add(group)
        placed.clear()
        demoLinks.clear()
        selected = null
        cursor = null
    }

    private fun active() = selected ?: placed.lastOrNull()

    private fun near(location: Pos): Showcase? = placed.minByOrNull { it.pos.distanceTo(location) }?.takeIf {
        it.pos.distanceTo(location) < SELECT_R
    }

    /** Demo LMB: select the portal under the cursor, or place a new one if the spot is clear (so two
     *  portals can't be stacked and existing ones can be picked, not replaced). Returns true if it
     *  placed a new portal (the caller plays the create sound). */
    fun click(location: Pos, level: Int, color: String): Boolean {
        val hit = near(location)
        if (hit != null) {
            selected = hit
            return false
        }
        place(location, level, color)
        return true
    }

    /** Place a portal at [location]/[level] in the sync-immune demo group, and select it. */
    fun place(location: Pos, level: Int, color: String) {
        val grp = group ?: return
        val obj = Three.Group()
        val resos = Octant.values().associateWith { Pair(level, 1.0) } // demo: a full set, full health
        val parts = Scene3D.buildPortal(obj, location, level.toDouble(), color, null, resos)
        Scene3D.applyBuildGrow(level.toDouble(), 0.0, parts) // start collapsed; grows in via update
        grp.add(obj)
        val sc = Showcase(obj, parts, location, level, color, 0.0, 0.0)
        placed.add(sc)
        selected = sc
    }

    /** Demo RMB: shatter + remove the placed portal nearest [location]. */
    fun removeNear(location: Pos) {
        val target = placed.minByOrNull { it.pos.distanceTo(location) } ?: return
        group?.remove(target.obj)
        placed.remove(target)
        dropLinksFor(target)
        if (target === selected) selected = null
        val resos = Octant.values().associateWith { target.level } // demo shows a full set → all fall
        Scene3D.shatterPortal(target.pos, target.color, target.level, resos)
    }

    /** Demo (Hack button): spin the active (selected, else last) portal's resonator collar. */
    fun hackActive(glyph: Boolean = false) {
        active()?.let {
            it.hackAge = if (glyph) HackFx.GLYPH_SPIN_S else HackFx.HACK_S
            it.hackGlyph = glyph
        }
    }

    /** Demo (XMP buttons): detonate a level-[level] XMP fireball at the active portal. */
    fun xmpActive(level: Int) {
        active()?.let { Scene3D.playXmpBurst(it.pos, level) }
    }

    /** Demo (Burnout button): vent the white steam puff + hiss from the active portal's flask top. */
    fun burnoutActive() {
        active()?.let { Scene3D.steamPuff(it.pos, it.level) }
    }

    /** Demo (Upgrade/Downgrade): re-place the active portal at level±[delta] (grows in at the new size). */
    fun stepLevel(delta: Int) {
        val target = active() ?: return
        val newLevel = (target.level + delta).coerceIn(1, 8)
        if (newLevel == target.level) return
        val pos = target.pos
        val up = newLevel > target.level
        group?.remove(target.obj)
        placed.remove(target)
        dropLinksFor(target) // the old showcase object is gone — drop its link pipes
        place(pos, newLevel, target.color) // re-places + re-selects
        if (up) Sound.playUpgradeSound(pos, newLevel) else Sound.playDowngradeSound(pos, newLevel)
    }

    /** Demo: move the ground cursor ring to [location] (null hides it); colour shows select vs place. */
    fun moveCursor(location: Pos?) {
        val grp = group ?: return
        val ring = cursor ?: run {
            val r = SELECT_R * Scene3D.metersPerPixel
            Three.Mesh(Three.RingGeometry(r * 0.82, r, 28), Scene3D.markerMaterial(Scene3D.NEUTRAL_COLOR)).also {
                cursor = it
                grp.add(it)
            }
        }
        if (location == null) {
            ring.visible = false
            return
        }
        ring.visible = true
        ring.asDynamic().position.set(Scene3D.sceneX(location), Scene3D.sceneY(location), Scene3D.OVERLAY_Z)
        ring.asDynamic().material.color.set(if (near(location) != null) Scene3D.HIGHLIGHT_COLOR else Scene3D.NEUTRAL_COLOR)
    }

    /** Demo (Link): glass-pipe the two most recently placed portals' orbs. */
    fun linkLast() {
        val grp = group ?: return
        if (placed.size < 2) return
        val a = placed[placed.size - 1]
        val b = placed[placed.size - 2]
        val pa = doubleArrayOf(Scene3D.sceneX(a.pos), Scene3D.sceneY(a.pos), Scene3D.orbCenterZ(a.level.toDouble()))
        val pb = doubleArrayOf(Scene3D.sceneX(b.pos), Scene3D.sceneY(b.pos), Scene3D.orbCenterZ(b.level.toDouble()))
        val pipe = Three.Mesh(Scene3D.linkGeo, Materials.linkGlass(a.color))
        Scene3D.orientTube(pipe.asDynamic(), pa, pb)
        grp.add(pipe)
        demoLinks.add(DemoLink(a, b, pipe)) // tracked so it's removed if either portal goes
    }

    /** Demo: remove any link pipes touching [sc] — no link may dangle without both end portals. */
    private fun dropLinksFor(sc: Showcase) {
        demoLinks.filter { it.a === sc || it.b === sc }.forEach {
            group?.remove(it.pipe)
        }
        demoLinks.removeAll { it.a === sc || it.b === sc }
    }

    /** Demo (ADA / JARVIS): flip the active showcase portal to [targetColor], morphing the orb (no shatter)
     *  the same way a live virus flip does, and play [faction]'s virus sound once. */
    fun refactorActive(targetColor: String, faction: Faction) {
        val sc = active() ?: return
        if (sc.color != targetColor) {
            sc.flipFrom = sc.color
            sc.flipAge = FLIP_S
            sc.color = targetColor
        }
        Sound.playVirusSound(sc.pos, faction)
    }

    fun update(dt: Double) {
        placed.forEach { sc ->
            if (sc.growAge < Scene3D.PORTAL_GROW_S) { // build-in: pole rises + orb grows from the ground
                sc.growAge += dt
                Scene3D.applyBuildGrow(sc.level.toDouble(), (sc.growAge / Scene3D.PORTAL_GROW_S).coerceIn(0.0, 1.0), sc.parts)
            }
            if (sc.hackAge > 0.0) { // hack: collar spins + rods centrifuge (same as live portals)
                sc.hackAge = (sc.hackAge - dt).coerceAtLeast(0.0)
                val dir = if (sc.color == Faction.ENL.color) -1.0 else 1.0 // ENL cw, else ccw
                val dur = if (sc.hackGlyph) HackFx.GLYPH_SPIN_S else HackFx.HACK_S
                HackFx.spinShowcase(sc.parts[3], dur - sc.hackAge, dir, sc.hackGlyph)
            }
            sc.flipFrom?.let { from ->
                // virus flip: morph the orb glass from old→new faction colour
                sc.flipAge = (sc.flipAge - dt).coerceAtLeast(0.0)
                val p = ((FLIP_S - sc.flipAge) / FLIP_S).coerceIn(0.0, 1.0)
                val mat = Materials.glass(Scene3D.blendColor(from, sc.color, p * p * (3.0 - 2.0 * p))) // smoothstep
                setOrbMaterial(sc.parts[0], mat)
                if (sc.flipAge <= 0.0) sc.flipFrom = null
            }
        }
    }

    /** Re-skin a showcase orb (outer shell + the concentric inner shell child) to [mat]. */
    private fun setOrbMaterial(orb: dynamic, mat: dynamic) {
        orb.material = mat
        val inner = orb.children[0] // the inner double-shell, added as the orb's child
        if (inner != null) inner.material = mat
    }

    fun animating() = placed.any { it.growAge < Scene3D.PORTAL_GROW_S || it.hackAge > 0.0 || it.flipFrom != null }
}
