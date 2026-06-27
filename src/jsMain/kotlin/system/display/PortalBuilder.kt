package system.display

import external.Three
import items.deployable.Mod
import items.deployable.ModType
import items.deployable.Shield
import items.level.LevelColor
import portal.Octant
import portal.Portal
import system.display.Scene3D.INDICATOR_Z
import system.display.Scene3D.MOD_R_FRAC
import system.display.Scene3D.NEUTRAL_COLOR
import system.display.Scene3D.PHI
import system.display.Scene3D.POLE_H
import system.display.Scene3D.POLE_R
import system.display.Scene3D.RESO_COLLAR_FRAC
import system.display.Scene3D.RESO_RADIUS_FRAC
import system.display.Scene3D.RESO_ROD_LEN_FRAC
import system.display.Scene3D.TOP_R
import system.display.Scene3D.gasketGeo
import system.display.Scene3D.groundZ
import system.display.Scene3D.place
import system.display.Scene3D.poleGeo
import system.display.Scene3D.resoCapGeo
import system.display.Scene3D.resoRingGeo
import system.display.Scene3D.resoRodGeo
import system.display.Scene3D.sceneX
import system.display.Scene3D.sceneY
import system.display.Scene3D.tag
import system.display.fx.DeployFx
import system.display.shader.ShieldShader
import util.data.Pos
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Builds the three.js mesh for a portal — the metal pole + rubber gasket + glass orb, its 8 resonator
 * slot-rings/rods, the deployed-mod tetrahedron and shield bubbles — plus the per-level scale math and the
 * build-in grow animation. Split out of [Scene3D] (a `LargeClass`): the scene owns the groups + per-tick
 * sync and calls [buildPortal]/[applyBuildGrow]; this owns the portal geometry. Shared scene primitives
 * (coordinate transforms, the portal dimensions) are imported from [Scene3D].
 */
object PortalBuilder {
    private const val INNER_SHELL_FRAC = 0.89 // inner glass shell radius (× orb) — a thin wall matching the shards
    private const val RESO_POP_DELAY = 0.3 // resonators start popping in once the pole is ~30% up
    private const val MOD_SCALE = 1.2 // scale the mod solids up a touch (they read bland at base size)
    private val MOD_WIRE_SCALES = doubleArrayOf(1.01, 1.05) // two concentric edge cages → a bolder glowing wire
    private const val MAX_SHIELD_SHELLS = 4 // up to 4 shields per portal → 4 concentric bubbles
    private const val SHIELD_SHELL_STEP = 0.09 // each shield shell sits this much larger than the last (× radius)
    private const val MOD_RING_FRAC = 0.55 // tetra vertex distance from orb centre (× orb radius)

    // Unit regular-tetrahedron vertices (magnitude √3); the 4 mod slots sit at these inside the orb.
    private val TETRA = arrayOf(
        doubleArrayOf(1.0, 1.0, 1.0),
        doubleArrayOf(1.0, -1.0, -1.0),
        doubleArrayOf(-1.0, 1.0, -1.0),
        doubleArrayOf(-1.0, -1.0, 1.0),
    )

    // Portal geometries (created lazily once three.js is loaded). poleGeo/gasketGeo/resoRingGeo/resoRodGeo
    // are shared with Scene3D's shatter effects, so they live there and are imported above.
    private val topGeo: dynamic by lazy { Three.SphereGeometry(TOP_R, 20, 16) } // glass orb (scaled per level)
    private val dodecaGeo: dynamic by lazy { Three.DodecahedronGeometry(TOP_R * MOD_R_FRAC) } // shield mod

    // EdgesGeometry per mod shape (cached): only the real polygon edges, not the triangulation.
    private val modEdgesCache = mutableMapOf<ModType, dynamic>()
    private fun modEdges(type: ModType): dynamic = modEdgesCache.getOrPut(type) { Three.EdgesGeometry(modGeoFor(type)) }
    private val pentaGeo: dynamic by lazy {
        Three.CylinderGeometry(TOP_R * MOD_R_FRAC, TOP_R * MOD_R_FRAC, TOP_R * MOD_R_FRAC * 0.55, 5)
    } // heat-sink radiator
    private val cubeGeo: dynamic by lazy {
        Three.BoxGeometry(TOP_R * MOD_R_FRAC * 1.1, TOP_R * MOD_R_FRAC * 1.1, TOP_R * MOD_R_FRAC * 1.1)
    } // link amp
    private val multihackGeo: dynamic by lazy {
        // A hollow square ring: a torus with a 4-sided cross-section (radialSeg 4) and 4 corners (tubularSeg 4).
        Three.TorusGeometry(TOP_R * MOD_R_FRAC * 0.95, TOP_R * MOD_R_FRAC * 0.28, 4, 4)
    } // multi-hack
    private val shieldGeo: dynamic by lazy { Three.SphereGeometry(TOP_R * PHI, 24, 18) } // shield bubble at φ× the orb

    fun orbScale(level: Double) = 0.45 + (level.coerceIn(1.0, 8.0) - 1.0) / 7.0 * 1.15 // orb radius
    fun poleScale(level: Double) = 1.0 + (level.coerceIn(1.0, 8.0) - 1.0) / 7.0 * 1.2 // pole height: 1 → 2.2
    fun poleHeight(level: Double) = POLE_H * poleScale(level)
    fun orbCenterZ(level: Double) = poleHeight(level) + TOP_R * orbScale(level) // orb rests on the pole top

    /**
     * A portal: a metallic pole (taller with [level]), a black rubber gasket so the metal doesn't
     * touch the glass, and a round glass orb on top (bigger with [level]). [id] tags it for picking
     * (null = demo). Returns [orb, gasket, pole, resoGroup] so the demo can drop them when it shatters.
     */
    fun buildPortal(
        parent: dynamic,
        location: Pos,
        level: Double,
        color: String,
        id: String?,
        resos: Map<Octant, Pair<Int, Double>> = emptyMap(),
    ): Array<dynamic> {
        val x = sceneX(location)
        val y = sceneY(location)
        val gz = groundZ(location) // sit the whole portal on the terrain
        val poleH = poleHeight(level)
        val s = orbScale(level)
        // Selection lights the orb brighter (faction hue kept). Demo portals (id == null) never highlight.
        val glassMat = if (id != null && id == Scene3D.selected) Materials.glassBright(color) else Materials.glass(color)
        val pole = Three.Mesh(poleGeo, Materials.metal())
        pole.asDynamic().castShadow = true // the metal pole throws a real shadow (sun)
        pole.asDynamic().rotation.x = PI / 2 // Y-axis cylinder → vertical (Z up)
        pole.asDynamic().scale.set(1.0, poleScale(level), 1.0) // grow height (local Y) only
        place(pole.asDynamic(), x, y, gz + poleH / 2)
        val gasket = Three.Mesh(gasketGeo, Materials.rubber()) // torus in XY → flat ring around the pole top
        place(gasket.asDynamic(), x, y, gz + poleH)
        val orb = Three.Mesh(topGeo, glassMat)
        place(orb.asDynamic(), x, y, gz + orbCenterZ(level))
        orb.asDynamic().scale.set(s, s, s)
        // Double-shell: a concentric inner glass surface gives the orb real wall thickness — its
        // rim sits inside the outer rim, so the orb reads as a thick blown-glass vessel, not a film.
        // (Child of the orb, so it inherits the per-level scale + the grow-in tween for free.)
        val inner = Three.Mesh(topGeo, glassMat)
        inner.asDynamic().scale.set(INNER_SHELL_FRAC, INNER_SHELL_FRAC, INNER_SHELL_FRAC)
        orb.asDynamic().add(inner)
        id?.let {
            tag(pole.asDynamic(), it)
            tag(gasket.asDynamic(), it)
            tag(orb.asDynamic(), it)
        }
        parent.add(pole)
        parent.add(gasket)
        parent.add(orb)
        val resoGroup = buildResonators(parent, location, level, resos, id)
        return arrayOf(orb, gasket, pole, resoGroup)
    }

    /** 8 rubber slot-rings around the pole collar; a colour-coded rod stands in each filled slot. */
    private fun buildResonators(
        parent: dynamic,
        location: Pos,
        level: Double,
        resos: Map<Octant, Pair<Int, Double>>,
        id: String? = null,
    ): dynamic {
        val x = sceneX(location)
        val y = sceneY(location)
        val gz = groundZ(location)
        val group = Three.Group()
        val poleH = poleHeight(level)
        val rodLen = poleH * RESO_ROD_LEN_FRAC
        val ringR = POLE_R * RESO_RADIUS_FRAC
        Octant.values().forEachIndexed { i, octant ->
            val ang = i * PI / 4.0
            val ox = ringR * cos(ang)
            val oy = ringR * sin(ang)
            val resoInfo = resos[octant]
            if (resoInfo != null) {
                val lvl = resoInfo.first
                val health = resoInfo.second
                // Rod hangs from a pivot/joint at its TOP, so a hack swings its loose bottom end
                // radially outward (centrifuge) while the top stays put. Tagged for the hack update.
                val pivot = Three.Group()
                pivot.asDynamic().position.set(ox, oy, rodLen) // joint at the rod top
                pivot.asDynamic().userData.isRodPivot = true
                pivot.asDynamic().userData.baseAngle = ang
                pivot.asDynamic().userData.energyFraction = health // hack splay scales with charge (empty/dead → none)
                pivot.asDynamic().userData.targetX = ox
                pivot.asDynamic().userData.targetY = oy
                // If just deployed, fly the rod in from the agent's position (DeployFx lerps + grows it).
                val from = id?.let { DeployFx.fromOf(it, octant) }
                if (id != null && from != null) {
                    pivot.asDynamic().userData.flyStartX = sceneX(from) - x // agent pos in the reso group's frame
                    pivot.asDynamic().userData.flyStartY = sceneY(from) - y
                    // Emerge from the agent's energy bar (above its head), not the ground — DeployFx then
                    // rises the rod straight up out of the bar before peeling off to the slot.
                    pivot.asDynamic().userData.flyStartZ = groundZ(from) + INDICATOR_Z - gz - poleH * RESO_COLLAR_FRAC
                    pivot.asDynamic().userData.targetZ = rodLen
                    DeployFx.bind(id, octant, pivot.asDynamic())
                }
                val rodMat = Materials.resonator(LevelColor.map[lvl] ?: "#ffffff", health)
                val rod = Three.Mesh(resoRodGeo, rodMat)
                rod.asDynamic().rotation.x = PI / 2 // unit Y-cylinder → vertical
                rod.asDynamic().scale.set(1.0, rodLen, 1.0)
                rod.asDynamic().position.set(0.0, 0.0, -rodLen / 2.0) // hangs down to the grommet
                pivot.asDynamic().add(rod)
                // A glowing "energy surface" disc sitting at the current FILL LEVEL, so the reso reads as
                // filled to that height (not just a fill line on the outside). The rod spans pivot-local
                // z ∈ [-rodLen, 0] (bottom→top), so the charged surface is at -rodLen·(1-fill). Use the rod
                // material's ACTUAL (stepped) uFill so the disc lines up with the bar exactly. renderOrder 1
                // draws it AFTER the now-non-depth-writing rods, so its glow sits on top instead of being
                // painted over or hidden behind a nearer resonator; depthTest still lets the pole hide it.
                val shownFill = (rodMat.uniforms.uFill.value as Double).coerceIn(0.0, 1.0)
                val cap = Three.Mesh(resoCapGeo, Materials.resonatorCap(LevelColor.map[lvl] ?: "#ffffff"))
                cap.asDynamic().position.set(0.0, 0.0, -rodLen * (1.0 - shownFill))
                cap.asDynamic().renderOrder = 1
                pivot.asDynamic().add(cap)
                // Slotted reso → rings ride the pivot (tilt with the rod on hack/glyph); mid-deploy → rings stay
                // at the pole so only the rod lerps in.
                addSlotRings(group, pivot.takeIf { from == null }, ox, oy, rodLen)
                group.asDynamic().add(pivot)
            } else {
                addSlotRings(group, null, ox, oy, rodLen) // empty slot → both rings fixed at the pole (no tilt)
            }
        }
        group.asDynamic().position.set(x, y, gz + poleH * RESO_COLLAR_FRAC)
        parent.add(group)
        return group
    }

    // A bare rubber o-ring at [parent]-local ([x], [y], [z]). [parent] is the reso group for a pole-fixed ring
    // (empty slot / mid-deploy) or a rod pivot for a slotted reso's rings (so they tilt with the rod on a hack).
    private fun addRing(parent: dynamic, x: Double, y: Double, z: Double) {
        val ring = Three.Mesh(resoRingGeo, Materials.rubber())
        ring.asDynamic().position.set(x, y, z)
        parent.add(ring)
    }

    // A slot's lower + upper o-rings. With [pivot] non-null (a slotted reso) they ride the rod pivot at its
    // bottom + top joint, so they tilt with the rod on a hack/glyph; otherwise they're fixed at the pole
    // ([ox], [oy]) — the collar socket + an upper guide ring — for empty slots and resos mid-deploy.
    private fun addSlotRings(group: dynamic, pivot: dynamic?, ox: Double, oy: Double, rodLen: Double) {
        if (pivot != null) {
            addRing(pivot, 0.0, 0.0, -rodLen)
            addRing(pivot, 0.0, 0.0, 0.0)
        } else {
            addRing(group, ox, oy, 0.0)
            addRing(group, ox, oy, rodLen)
        }
    }

    /**
     * Deployed shields: chrome mods in a tetrahedron inside the orb + a sci-fi shield bubble at φ× the
     * orb radius. Added as children of the [orb] so they inherit its per-level scale + grow-in tween.
     */
    fun buildMods(orb: dynamic, portal: Portal) {
        val mods = portal.mods.values.toList()
        if (mods.isEmpty()) return
        val r = TOP_R * MOD_RING_FRAC / sqrt(3.0) // normalize the √3-magnitude tetra verts to the ring radius
        val tetra = Three.Group() // the whole mod tetrahedron — slowly tumbled per frame (see tumbleModTetras)
        mods.forEachIndexed { i, mod ->
            val v = TETRA[i % TETRA.size]
            val geo = modGeoFor(mod.modType())
            val mesh = Three.Mesh(geo, Materials.modSolid(mod.rarity.color)) // translucent + luminous (not chrome)
            mesh.asDynamic().position.set(v[0] * r, v[1] * r, v[2] * r)
            mesh.asDynamic().scale.set(MOD_SCALE, MOD_SCALE, MOD_SCALE) // a touch bigger so the mods read
            if (mod.modType() == ModType.LINK_AMP) mesh.asDynamic().rotation.set(0.62, 0.62, 0.0) // cube on its diagonal
            // Bold glowing edge cage: two concentric wire copies fake a thicker line (WebGL caps linewidth).
            MOD_WIRE_SCALES.forEach { ws ->
                val wire = Three.LineSegments(modEdges(mod.modType()), Materials.modWire(mod.rarity.color))
                wire.asDynamic().scale.set(ws, ws, ws)
                mesh.asDynamic().add(wire)
            }
            tetra.asDynamic().add(mesh)
        }
        orb.add(tetra) // orb is already dynamic (no .asDynamic())
        modTetras.add(tetra)
        addShieldShells(orb, portal, mods)
    }

    // Up to MAX_SHIELD_SHELLS concentric energy bubbles (one per deployed shield), each a touch larger
    // than the last → a layered shield that also reads with depth. Stay put (not in the mod tumble).
    private fun addShieldShells(orb: dynamic, portal: Portal, mods: List<Mod>) {
        val shells = mods.count { it is Shield }.coerceIn(0, MAX_SHIELD_SHELLS)
        if (shells == 0) return
        val color = portal.owner?.faction?.color ?: NEUTRAL_COLOR
        val baseIntensity = portal.totalMitigation() / 100.0
        repeat(shells) { i ->
            val mat = ShieldShader.material(color, baseIntensity * (1.0 - i * 0.12))
            val bubble = Three.Mesh(shieldGeo, mat)
            val s = 1.0 + i * SHIELD_SHELL_STEP
            bubble.asDynamic().scale.set(s, s, s)
            orb.add(bubble)
            shieldMats.add(Pair(mat, portal.id)) // so ShieldWave can ripple it; Pair() not `to` (dynamic receiver)
        }
    }

    private val modTetras = mutableListOf<dynamic>() // mod tetrahedra, rebuilt each sync, tumbled each frame

    /** Shield bubble materials + their portal id, rebuilt each sync → driven per frame by ShieldWave (ripple). */
    val shieldMats = mutableListOf<Pair<dynamic, String>>()

    /** Drop the per-sync mod/shield state (the scene rebuilds it each sync). */
    fun resetSyncState() {
        modTetras.clear()
        shieldMats.clear()
    }

    // Slowly tumble each mod tetrahedron on incommensurate sine drifts → a gentle, never-repeating spin
    // that keeps changing direction. Time-driven so it's smooth across the per-sync rebuild.
    fun tumbleModTetras() {
        if (modTetras.isEmpty()) return
        val t = Scene3D.animMs() / 1000.0
        modTetras.forEach { g ->
            g.rotation.x = sin(t * 0.11) * PI
            g.rotation.y = sin(t * 0.13 + 1.3) * PI
            g.rotation.z = sin(t * 0.17 + 2.6) * PI
        }
    }

    fun modGeoFor(type: ModType): dynamic = when (type) {
        ModType.SHIELD -> dodecaGeo
        ModType.HEAT_SINK -> pentaGeo
        ModType.LINK_AMP -> cubeGeo
        ModType.MULTIHACK -> multihackGeo
    }

    /** Rise the pole + grow the orb from the ground for the build-in animation ([g] = 0→1). */
    fun applyBuildGrow(level: Double, g: Double, parts: Array<dynamic>, reform: Double = 1.0, gz: Double = 0.0) {
        val gg = g.coerceIn(0.0, 1.0)
        val poleP = easeOutCubic(gg) // the pole shoots up and settles
        val orbP = easeOutBack(gg) // the orb inflates past full size, then settles (juicy pop)
        val resoP = easeOutBack(((gg - RESO_POP_DELAY) / (1.0 - RESO_POP_DELAY)).coerceIn(0.0, 1.0)) // pop in after the pole
        val poleH = poleHeight(level)
        val s = orbScale(level) * orbP * easeOutBack(reform.coerceIn(0.0, 1.0)) // capture re-pop also overshoots
        parts[2].scale.set(1.0, poleScale(level) * poleP, 1.0) // pole
        parts[2].position.z = gz + poleH * poleP / 2.0
        parts[1].position.z = gz + poleH * poleP // gasket
        parts[0].scale.set(s, s, s) // orb
        parts[0].position.z = gz + poleH * poleP + TOP_R * s
        parts[3].scale.set(resoP, resoP, resoP) // resonators grow in with the collar
        parts[3].position.z = gz + poleH * poleP * RESO_COLLAR_FRAC
    }

    private fun easeOutCubic(t: Double) = 1.0 - (1.0 - t).pow(3)

    // Back-ease: overshoots ~10% past 1.0 before settling — the classic "pop/inflate" feel.
    private fun easeOutBack(t: Double): Double {
        val c1 = 1.70158
        val u = t - 1.0
        return 1.0 + (c1 + 1.0) * u * u * u + c1 * u * u
    }
}
