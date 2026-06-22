package system.display

import World
import agent.Agent
import agent.Faction
import agent.NonFaction
import agent.StuckTracker
import agent.action.ActionItem
import config.Sim
import external.GLTFLoader
import external.MapLibre
import external.Three
import items.deployable.Mod
import items.deployable.ModType
import items.deployable.Shield
import items.level.LevelColor
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import portal.Field
import portal.Link
import portal.Octant
import portal.Portal
import portal.XmHeap
import portal.XmMap
import util.Debug
import util.SoundUtil
import util.data.Pos
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Renders the game world as a MapLibre custom layer (three.js): the three.js
 * camera is driven by the matrix MapLibre passes each frame, so the scene stays
 * glued to the map and works under pitch/rotate.
 *
 * The scene is built in METRES around an origin (the sim anchor center): a point
 * at three.js (x, y, z) is x m east, y m north, z m up. The model matrix turns
 * those metres into mercator units. Sim positions are anchor-space pixels, so a
 * `Pos` maps to metres via [metersPerPixel].
 *
 * The simulation stays 2D; [sync] is called each tick to (re)place 3D objects
 * from world state. Rendering happens continuously via the custom layer.
 */
// LargeClass suppressed: Scene3D is the renderer hub; subsystems are already extracted (ShatterFx,
// HackFx, FieldFx, CaptureFx, PortalChangeSound, Spawns, ShardAssets, GlassShader). The remaining
// size is mostly the self-contained demo/showcase code — extracting that is the tracked follow-up
// (PLAN.md → "extract the demo/showcase subsystem from Scene3D").
@Suppress("TooManyFunctions", "LargeClass")
object Scene3D {
    // Metres per CSS pixel at zoom 0 on the equator, for MapLibre's 512-px tiles
    // (earthCircumference / 512). NOTE: the common 156543.03392 value is for 256-px
    // tiles — using it here scaled the whole scene 2× too large.
    private const val METERS_PER_PIXEL_Z0 = 78271.516964

    // People (NPCs and players) are human-scale: a head-sized sphere at head
    // height, so a humanoid body can later be added beneath it. Players are
    // faction-coloured with an action indicator floating just above the head.
    private const val HEAD_R = 0.45
    private const val HEAD_Z = 1.6
    private const val XM_R = 0.7 // stray-XM mote radius (metres)
    private const val XM_Z = 1.2 // stray-XM floats just above the ground
    private const val NPC_DROP_S = 0.8 // seconds for an NPC to fall in from the sky on first appearance
    private const val NPC_DROP_HEIGHT = 70.0 // metres an NPC drops from
    private const val INDICATOR_Z = 5.0 // raised to clear the head now the indicator is ~3× bigger
    private const val INDICATOR_SIZE = 4.8 // action label above an agent (was 1.6 — barely visible)
    private const val LABEL_W = 22.0 // portal name/level billboard width (scene metres)
    private const val LABEL_GAP = 4.0 // gap above the orb top before the label
    private const val LABEL_CANVAS_W = 256 // label texture resolution (kept crisp; faction-neutral white)
    private const val LABEL_CANVAS_H = 96
    private const val POLE_R = 2.0
    private const val LINK_R = 0.7 // glass-pipe link radius (metres)
    private const val CORE_R_FRAC = 0.3 // bright inner-filament radius as a fraction of LINK_R
    private const val PORTAL_GROW_S = 0.5 // seconds for a new portal's orb to grow in
    private const val CAPTURE_SHATTER_WEIGHT = 0.22 // glass-shatter heaviness on capture (light — only the orb)
    private const val FIELD_FILL_S = 0.4 // seconds for a new control field to fill in
    private const val LEVEL_TWEEN_RATE = 0.18 // per-sync ease of the rendered level toward the real one
    private const val POLE_H = 22.5 // base pole height at L1; scales by φ per level
    private const val TOP_R = 7.0 // base orb radius
    private const val INNER_SHELL_FRAC = 0.89 // inner glass shell radius (× orb) — a thin wall (~2.5× thinner) matching the shards
    private const val PHI = 1.618 // golden ratio — pole grows by φ across the 8 levels

    // Resonators: 8 rubber slot-rings around the pole collar (just below the gasket), each holding a
    // colour-coded rod (the resonator) when filled.
    private const val RESO_RING_R = POLE_R * 0.42 // grommet ring radius
    private const val RESO_RING_TUBE = POLE_R * 0.13
    private const val RESO_ROD_R = POLE_R * 0.26
    private const val RESO_RADIUS_FRAC = 1.7 // slot distance from pole axis (× POLE_R) — spread so slots read distinct top-down
    private const val RESO_COLLAR_FRAC = 0.78 // collar height as a fraction of the pole height
    private const val RESO_ROD_LEN_FRAC = 0.22 // rod length as a fraction of the pole height
    private const val NEUTRAL_COLOR = "#bbbbbb"
    private const val MOD_R_FRAC = 0.16 // chrome mod radius (× orb radius)
    private const val MOD_RING_FRAC = 0.42 // tetrahedron vertex distance from orb centre (× orb radius)

    // Unit regular-tetrahedron vertices (magnitude √3); the 4 mod slots sit at these inside the orb.
    private val TETRA = arrayOf(
        doubleArrayOf(1.0, 1.0, 1.0),
        doubleArrayOf(1.0, -1.0, -1.0),
        doubleArrayOf(-1.0, 1.0, -1.0),
        doubleArrayOf(-1.0, -1.0, 1.0),
    )
    private const val HIGHLIGHT_COLOR = "#f0f0f0" // selection: off-tint grayscale (no new hues)
    private const val OVERLAY_Z = 0.2 // passability quad just above ground
    private const val MARKER_R = 10.0 // build-preview marker radius (metres)
    private const val SHOWCASE_SELECT_R = 55.0 // demo: click within this (sim px) selects a portal / min place gap
    private const val BORDER_COLOR = "#ffffff" // playable-area boundary (white — no non-faction hues)
    private const val BORDER_Z = 0.3
    private const val OUTSIDE_DIM = 0.4 // opacity of the dark mask greying out everything beyond the border
    private const val OUTSIDE_FAR = 12.0 // how far past the play area the dim mask extends (× the half-extent)
    private const val WALL_HEIGHT = 16.0 // upright play-area boundary wall height (scene metres)
    private const val WALL_THICK = 1.0 // boundary wall thickness (scene metres)
    const val CUSTOM_LAYER_ID = "qgress-3d" // MapLibre layer id for the three.js scene

    // Currently selected entity, as "portal:<id>" / "agent:<name>" (see pick()).
    var selected: String? = null

    private var scene: Three.Scene? = null
    private var camera: Three.Camera? = null
    private var renderer: Three.WebGLRenderer? = null

    private var originMerc: dynamic = null
    private var metersScale = 1.0 // mercator units per metre at the origin

    var metersPerPixel = 1.0
        private set

    // Category groups, cleared & rebuilt each sync.
    private var portalsGroup: dynamic = null
    private var agentsGroup: dynamic = null
    private var indicatorsGroup: dynamic = null // action-indicator sprites (excluded from raycast)
    private var npcsGroup: dynamic = null
    private var xmGroup: dynamic = null // stray collectible XM motes
    private var linksGroup: dynamic = null
    private var fieldsGroup: dynamic = null
    private var markerGroup: dynamic = null // build-preview marker
    private var borderGroup: dynamic = null // playable-area boundary outline

    // Glass-orb shatter fracture variants (loaded once from the GLB via ShardAssets).
    private var flaskVariants: List<List<dynamic>> = emptyList()
    private var flaskScale = 1.0 // scale a flask variant to ≈ the portal top sphere
    private var showcaseGroup: dynamic = null // demo-scene placed portals (not cleared by sync)
    private class Showcase(val group: dynamic, val parts: Array<dynamic>, val pos: Pos, var level: Int, val color: String, var hackAge: Double, var growAge: Double) {
        var hackGlyph: Boolean = false // the active hack is a (stronger) glyph hack
    }
    private val showcases = mutableListOf<Showcase>()
    private class DemoLink(val a: Showcase, val b: Showcase, val tube: dynamic, val core: dynamic)
    private val demoLinks = mutableListOf<DemoLink>() // demo link pipes, so they can't outlive their portals
    private var selectedShowcase: Showcase? = null // demo: the portal the action buttons act on
    private var demoCursor: dynamic = null // demo: ground ring under the mouse (place vs select)
    private var lastFrameMs = 0.0 // for per-frame effect dt

    /** Last-known shape of a control field (centroid + 3 centroid-relative vertices), for its dissolve. */
    private class FieldRecord(val cx: Double, val cy: Double, val cz: Double, val rel: Array<DoubleArray>, val color: String)

    private val fieldRecords = mutableMapOf<String, FieldRecord>()
    private val displayedLevel = mutableMapOf<String, Double>() // per-portal eased level (for level-up tween)

    // Shared geometries (created lazily once three.js is loaded).
    private val headGeo: dynamic by lazy { Three.SphereGeometry(HEAD_R, 10, 10) }
    private val xmGeo: dynamic by lazy { Three.SphereGeometry(XM_R, 8, 8) } // stray-XM mote
    private val poleGeo: dynamic by lazy { Three.CylinderGeometry(POLE_R, POLE_R, POLE_H, 12) } // metal pole
    private val topGeo: dynamic by lazy { Three.SphereGeometry(TOP_R, 20, 16) } // glass orb (scaled per level)
    private val dodecaGeo: dynamic by lazy { Three.DodecahedronGeometry(TOP_R * MOD_R_FRAC) } // shield mod
    private val pentaGeo: dynamic by lazy { Three.CylinderGeometry(TOP_R * MOD_R_FRAC, TOP_R * MOD_R_FRAC, TOP_R * MOD_R_FRAC * 0.55, 5) } // heat-sink radiator
    private val cubeGeo: dynamic by lazy { Three.BoxGeometry(TOP_R * MOD_R_FRAC * 1.1, TOP_R * MOD_R_FRAC * 1.1, TOP_R * MOD_R_FRAC * 1.1) } // link amp
    private val shieldGeo: dynamic by lazy { Three.SphereGeometry(TOP_R * PHI, 24, 18) } // shield bubble at φ× the orb
    private val gasketGeo: dynamic by lazy { Three.TorusGeometry(POLE_R * 1.15, POLE_R * 0.4, 10, 20) } // rubber donut
    private val linkGeo: dynamic by lazy { Three.CylinderGeometry(LINK_R, LINK_R, 1.0, 8) } // unit glass tube (scaled to length)
    private val coreGeo: dynamic by lazy { Three.CylinderGeometry(LINK_R * CORE_R_FRAC, LINK_R * CORE_R_FRAC, 1.0, 6) } // bright filament inside the tube
    private val resoRingGeo: dynamic by lazy { Three.TorusGeometry(RESO_RING_R, RESO_RING_TUBE, 8, 14) } // rubber slot grommet
    private val resoRodGeo: dynamic by lazy { Three.CylinderGeometry(RESO_ROD_R, RESO_ROD_R, 1.0, 8) } // unit rod, scaled to length
    private val materialCache = mutableMapOf<String, dynamic>()
    private val spriteCache = mutableMapOf<String, dynamic>()

    fun register(map: MapLibre.Map, originLng: Double, originLat: Double, anchorZoom: Double) {
        originMerc = MapLibre.asDynamic().MercatorCoordinate.fromLngLat(arrayOf(originLng, originLat), 0.0)
        metersScale = originMerc.meterInMercatorCoordinateUnits() as Double
        metersPerPixel = METERS_PER_PIXEL_Z0 * cos(originLat * PI / 180.0) / 2.0.pow(anchorZoom)
        map.addLayer(buildCustomLayer(map))
    }

    private fun buildCustomLayer(map: MapLibre.Map): dynamic {
        val layer: dynamic = js("({})")
        layer.id = CUSTOM_LAYER_ID
        layer.type = "custom"
        layer.renderingMode = "3d"
        layer.onAdd = { _: dynamic, gl: dynamic -> onAdd(map, gl) }
        layer.render = { _: dynamic, args: dynamic -> render(map, args) }
        return layer
    }

    private fun onAdd(map: MapLibre.Map, gl: dynamic) {
        val newScene = Three.Scene()
        camera = Three.Camera()
        newScene.add(Three.AmbientLight(0xffffff, 0.9))
        val sun = Three.DirectionalLight(0xffffff, 0.5)
        sun.asDynamic().position.set(60.0, 90.0, 140.0)
        newScene.add(sun)

        PassabilityOverlay.register(newScene)
        VectorFieldOverlay.register(newScene)
        portalsGroup = Three.Group().also { newScene.add(it) }
        fieldsGroup = Three.Group().also { newScene.add(it) }
        linksGroup = Three.Group().also { newScene.add(it) }
        npcsGroup = Three.Group().also { newScene.add(it) }
        xmGroup = Three.Group().also { newScene.add(it) }
        agentsGroup = Three.Group().also { newScene.add(it) }
        indicatorsGroup = Three.Group().also { newScene.add(it) }
        markerGroup = Three.Group().also { newScene.add(it) }
        borderGroup = Three.Group().also { newScene.add(it) }
        XmpBurst.register(newScene)
        FieldFx.register(newScene)
        ShatterFx.register(newScene)
        showcaseGroup = Three.Group()
        newScene.add(showcaseGroup)
        scene = newScene
        buildBorder()
        loadShatterAssets()

        val params: dynamic = js("({})")
        params.canvas = map.getCanvas()
        params.context = gl
        params.antialias = true
        renderer = Three.WebGLRenderer(params).also { it.autoClear = false }
    }

    private fun render(map: MapLibre.Map, args: dynamic) {
        val cam = camera ?: return
        val activeRenderer = renderer ?: return
        val activeScene = scene ?: return
        val mapMatrix = Three.Matrix4().fromArray(args.defaultProjectionData.mainMatrix)
        val modelMatrix = Three.Matrix4()
            .makeTranslation(originMerc.x as Double, originMerc.y as Double, originMerc.z as Double)
            .scale(Three.Vector3(metersScale, -metersScale, metersScale))
        cam.projectionMatrix = mapMatrix.multiply(modelMatrix)
        GlassShader.updateEye(cam.projectionMatrix) // camera-tracking glass rim (orbs + links)
        val invProj = Three.Matrix4().copy(cam.projectionMatrix).invert()
        updateAudioListener(invProj) // place the Web Audio listener at the live camera
        PlasmaShader.setTime((js("performance.now()") as Double) / 1000.0) // animate control fields
        if (hasActiveEffects()) {
            val nowMs = js("performance.now()") as Double
            val dt = if (lastFrameMs <= 0.0) 0.016 else ((nowMs - lastFrameMs) / 1000.0).coerceIn(0.0, 0.1)
            lastFrameMs = nowMs
            if (ShatterFx.hasActive()) ShatterFx.update(dt)
            if (showcasesAnimating()) updateShowcases(dt)
            if (HackFx.hasActive()) HackFx.update()
            if (DeployFx.hasActive()) DeployFx.update()
            if (FieldFx.hasActive()) FieldFx.update(dt)
            if (XmpBurst.hasActive()) {
                val canvas = map.getCanvas()
                XmpBurst.setView(invProj, canvas.width as Double, canvas.height as Double)
                XmpBurst.update(dt)
            }
        } else {
            lastFrameMs = 0.0
        }
        VectorFieldOverlay.sync() // paced flow-field sweep; driven here (continuous loop) so it animates through world-gen too
        activeRenderer.resetState()
        activeRenderer.render(activeScene, cam)
        map.triggerRepaint()
    }

    private fun hasActiveEffects() = ShatterFx.hasActive() || showcasesAnimating() || HackFx.hasActive() || DeployFx.hasActive() || XmpBurst.hasActive() || FieldFx.hasActive()

    /** Rebuild the 3D objects from world state. Called once per simulation tick. */
    fun sync() {
        scene ?: return
        Spawns.beginSync()
        HackFx.resetBindings() // re-bound below as each portal's reso group is rebuilt
        DeployFx.resetBindings()
        clear(portalsGroup)
        World.allPortals.forEach { addPortal(it) }
        clear(fieldsGroup)
        World.allFields().forEach { addField(it) }
        clear(linksGroup)
        World.allLinks().forEach { addLink(it) }
        clear(npcsGroup)
        World.allNonFaction.forEach { addNpc(it) }
        clear(xmGroup)
        XmMap.all().forEach { (pos, heap) -> addXm(pos, heap) }
        clear(agentsGroup)
        clear(indicatorsGroup)
        World.allAgents.forEach { addAgent(it) }
        teardownGone(Spawns.endSync())
    }

    // Mark the playable area: a white outline plus a dark mask greying out everything beyond it.
    private fun buildBorder() {
        val group = borderGroup ?: return
        val hx = sceneX(Pos(Sim.width, 0)) // play-area half-extents (scene metres); sceneY flips sim-y → +hy is the top edge
        val hy = sceneY(Pos(0, 0))
        PlayAreaMask.build(group, hx, hy, OUTSIDE_FAR * maxOf(hx, hy), BORDER_Z - 0.05, OUTSIDE_DIM)
        PlayAreaMask.buildWalls(group, hx, hy, WALL_HEIGHT, WALL_THICK, 0.0)
        val corners = arrayOf(Pos(0, 0), Pos(Sim.width, 0), Pos(Sim.width, Sim.height), Pos(0, Sim.height), Pos(0, 0))
        val points = corners.map { Three.Vector3(sceneX(it), sceneY(it), BORDER_Z) }.toTypedArray()
        group.add(Three.Line(Three.BufferGeometry().setFromPoints(points), lineMaterial(BORDER_COLOR)))
    }

    // Load the glass-orb fracture GLB once (async). shattered_flask = sphere shell-shard variants
    // (pieces named "<key>_chunkN"); one random variant per shatter, scaled to the orb's level.
    private fun loadShatterAssets() {
        val loader = GLTFLoader()
        loader.load(
            "models/shattered_flask.glb",
            { gltf ->
                flaskVariants = ShardAssets.parseVariants(gltf)
                flaskScale = ShardAssets.computeScale(flaskVariants.firstOrNull() ?: emptyList(), TOP_R * 2.0)
            },
            {},
            { e -> console.error("shattered_flask load failed: $e") },
        )
    }

    /**
     * Shatter a removed portal (from Portal.remove / the demo RMB): glass shards fly, the gasket
     * drops, the metal pole sinks, and each filled resonator rod ([resos] = octant→level) falls out
     * of its slot. The physics live in [ShatterFx]; we just hand it the geometry + positions.
     */
    fun shatterPortal(location: Pos, color: String, level: Int, resos: Map<Octant, Int> = emptyMap()) {
        val lv = level.toDouble()
        ShatterFx.shatter(
            sceneX(location), sceneY(location), poleHeight(lv), poleScale(lv), orbCenterZ(lv), orbScale(lv),
            color, flaskVariants, flaskScale, poleGeo, gasketGeo,
        )
        dropResonators(location, lv, resos)
    }

    /** Drop each filled resonator rod from its collar slot when the portal shatters. */
    private fun dropResonators(location: Pos, level: Double, resos: Map<Octant, Int>) {
        if (resos.isEmpty()) return
        val poleH = poleHeight(level)
        val collarZ = poleH * RESO_COLLAR_FRAC
        val rodLen = poleH * RESO_ROD_LEN_FRAC
        val ringR = POLE_R * RESO_RADIUS_FRAC
        val x = sceneX(location)
        val y = sceneY(location)
        Octant.values().forEachIndexed { i, octant ->
            val lvl = resos[octant] ?: return@forEachIndexed
            val ang = i * PI / 4.0
            val color = LevelColor.map[lvl] ?: "#ffffff"
            ShatterFx.spawnFallingRod(resoRodGeo, x + ringR * cos(ang), y + ringR * sin(ang), collarZ + rodLen / 2.0, RESO_ROD_R, rodLen, color)
        }
    }

    /** Drop a single resonator rod from its slot — used as each reso is destroyed during an attack. */
    fun dropResonator(location: Pos, level: Int, octantIndex: Int, resoLevel: Int) {
        val poleH = poleHeight(level.toDouble())
        val rodLen = poleH * RESO_ROD_LEN_FRAC
        val ringR = POLE_R * RESO_RADIUS_FRAC
        val ang = octantIndex * PI / 4.0
        ShatterFx.spawnFallingRod(
            resoRodGeo,
            sceneX(location) + ringR * cos(ang),
            sceneY(location) + ringR * sin(ang),
            poleH * RESO_COLLAR_FRAC + rodLen / 2.0,
            RESO_ROD_R,
            rodLen,
            LevelColor.map[resoLevel] ?: "#ffffff",
        )
    }

    /** Drop the deployed mods out of the orb when a portal is neutralized / removed. */
    fun dropMods(location: Pos, level: Int, mods: List<Mod>) {
        if (mods.isEmpty()) return
        val lv = level.toDouble()
        val s = orbScale(lv)
        val half = TOP_R * MOD_R_FRAC * s
        mods.forEach { mod ->
            ShatterFx.spawnFallingChunk(modGeoFor(mod.modType()), sceneX(location), sceneY(location), orbCenterZ(lv), s, half, mod.rarity.color)
        }
    }

    /**
     * Fire an XMP detonation at a location, scaled by burster [level] (1..8). See [XmpBurst].
     * [sound] = false when the caller already plays the attack sound (the game's Queues path).
     */
    fun playXmpBurst(location: Pos, level: Int, sound: Boolean = true) {
        scene ?: return
        XmpBurst.play(sceneX(location), sceneY(location), level)
        if (sound) SoundUtil.playXmpSound(location, level)
    }

    /** Place (or clear, when pos is null) the build-preview marker on the ground. */
    fun setBuildMarker(pos: Pos?, state: String) {
        val group = markerGroup ?: return
        group.clear()
        if (pos == null) return
        val color = when (state) { // grayscale only — colour is reserved for faction things
            "build" -> "#ffffff"
            "portal" -> "#999999"
            else -> "#555555"
        }
        val mesh = Three.Mesh(Three.RingGeometry(MARKER_R * 0.6, MARKER_R, 24), markerMaterial(color))
        mesh.asDynamic().position.set(sceneX(pos), sceneY(pos), OVERLAY_Z)
        group.add(mesh)
    }

    private fun markerMaterial(color: String): dynamic = materialCache.getOrPut("m$color") {
        val p: dynamic = js("({})")
        p.color = color
        p.transparent = true
        p.opacity = 0.8
        p.side = 2 // DoubleSide
        p.depthWrite = false
        Three.MeshBasicMaterial(p)
    }

    /** Toggle the passability overlay (a textured ground quad built from the movement grid). */
    private fun clear(group: dynamic) {
        group?.clear()
    }

    /**
     * Convert a ground lng/lat (e.g. from map.unproject of a click) back to a sim Pos,
     * the inverse of the Pos→scene bridge. Used for click selection / ground actions, which
     * is robust under pitch (MapLibre's unproject handles the camera) unlike raycasting the
     * map's custom projection matrix.
     */
    fun lngLatToSimPos(lng: Double, lat: Double): Pos {
        val merc = MapLibre.asDynamic().MercatorCoordinate.fromLngLat(arrayOf(lng, lat), 0.0)
        val eastMeters = (merc.x as Double - (originMerc.x as Double)) / metersScale
        val southMeters = (merc.y as Double - (originMerc.y as Double)) / metersScale
        val px = eastMeters / metersPerPixel + Sim.width / 2.0
        val py = southMeters / metersPerPixel + Sim.height / 2.0
        return Pos(px.toInt(), py.toInt())
    }

    internal fun sceneX(pos: Pos) = (pos.x - Sim.width / 2.0) * metersPerPixel
    internal fun sceneY(pos: Pos) = -(pos.y - Sim.height / 2.0) * metersPerPixel

    /**
     * Drive the Web Audio listener from the live camera each frame: position = the recovered eye,
     * orientation = the view forward + screen-up, all in sim-space metres (the same space the
     * per-sound [PannerNode]s live in). This gives true 3D audio — distance attenuation, front/back,
     * elevation — that tracks rotate/pitch/zoom. (Replaces the old screen-projected stereo pan.)
     */
    private fun updateAudioListener(invProj: dynamic) {
        if (camera == null) return
        val eye = GlassShader.eye()
        val mid = unproject(invProj, 0.0, 0.0, 0.0)
        val far = unproject(invProj, 0.0, 0.0, 1.0)
        val top = unproject(invProj, 0.0, 1.0, 0.0)
        SoundUtil.updateListener(
            eye,
            doubleArrayOf(far[0] - eye[0], far[1] - eye[1], far[2] - eye[2]), // forward (normalised in SoundUtil)
            doubleArrayOf(top[0] - mid[0], top[1] - mid[1], top[2] - mid[2]), // up
        )
    }

    /** Unproject a normalised-device point (x, y, z ∈ [−1, 1]) back to sim-space via [invProj]. */
    private fun unproject(invProj: dynamic, x: Double, y: Double, z: Double): DoubleArray {
        val v = Three.Vector3(x, y, z).asDynamic()
        v.applyMatrix4(invProj)
        return doubleArrayOf(v.x as Double, v.y as Double, v.z as Double)
    }

    private fun place(obj: dynamic, x: Double, y: Double, z: Double) {
        obj.position.set(x, y, z)
    }

    private fun tag(obj: dynamic, id: String) {
        val data: dynamic = js("({})")
        data.qid = id
        obj.userData = data
    }

    // Level is a Double so a level-up can ease between integer levels (see tweenedLevel).
    private fun orbScale(level: Double) = 0.5 + (level.coerceIn(1.0, 8.0) - 1.0) / 7.0 * 0.8 // orb radius
    private fun poleScale(level: Double) = PHI.pow((level.coerceIn(1.0, 8.0) - 1.0) / 7.0) // pole height: 1 → φ
    private fun poleHeight(level: Double) = POLE_H * poleScale(level)
    private fun orbCenterZ(level: Double) = poleHeight(level) + TOP_R * orbScale(level) // orb rests on the pole top

    /** Per-portal rendered level, eased toward the real level each sync so a level-up tweens smoothly. */
    private fun tweenedLevel(id: String, target: Int): Double {
        val cur = displayedLevel[id]
        val next = if (cur == null) target.toDouble() else cur + (target - cur) * LEVEL_TWEEN_RATE
        displayedLevel[id] = next
        return next
    }

    private fun addPortal(portal: Portal) {
        val id = "portal:${portal.id}"
        PortalChangeSound.check(id, portal) // upgrade / downgrade / neutralize sounds on state change
        val baseColor = portal.owner?.faction?.color ?: NEUTRAL_COLOR
        val level = tweenedLevel(id, portal.getLevel().toInt()) // eases on level-up
        // Capture: the old-colour orb shatters in place and the new one pops back in (reform factor).
        CaptureFx.check(id, baseColor) { old ->
            val lv = displayedLevel[id] ?: level
            ShatterFx.shatterOrb(sceneX(portal.location), sceneY(portal.location), orbCenterZ(lv), orbScale(lv), old, flaskVariants, flaskScale)
            SoundUtil.playGlassShatterSound(portal.location, CAPTURE_SHATTER_WEIGHT)
        }
        val reform = CaptureFx.reformFactor(id)
        val resos = portal.resoMap().mapValues { it.value.getLevel() } // octant → reso level (real-time)
        // Selection keeps the faction hue but lights the orb brighter (no neutral-looking white tint);
        // buildPortal derives that from id == selected.
        val parts = buildPortal(portalsGroup, portal.location, level, baseColor, id, resos)
        addPortalLabel(portal, level)
        buildMods(parts[0], portal) // chrome mods + shield bubble inside/around the orb (if shielded)
        HackFx.bind(id, parts[3]) // spin the collar if this portal is being hacked
        // Build-in: the pole rises and the orb grows from the ground; [reform] re-pops the orb only.
        val g = Spawns.appear(id, PORTAL_GROW_S)
        if (g < 1.0) {
            applyBuildGrow(level, g, parts, reform)
        } else if (reform < 1.0) {
            applyBuildGrow(level, 1.0, parts, reform) // orb pops back in after a capture
        }
    }

    /**
     * A portal: a metallic pole (taller with [level]), a black rubber gasket so the metal doesn't
     * touch the glass, and a round glass orb on top (bigger with [level]). [id] tags it for picking
     * (null = demo). Returns [orb, gasket] so the demo can drop them when the portal shatters.
     */
    private fun buildPortal(
        parent: dynamic,
        location: Pos,
        level: Double,
        color: String,
        id: String?,
        resos: Map<Octant, Int> = emptyMap(),
    ): Array<dynamic> {
        val x = sceneX(location)
        val y = sceneY(location)
        val poleH = poleHeight(level)
        val s = orbScale(level)
        // Selection lights the orb brighter (faction hue kept). Demo portals (id == null) never highlight.
        val glassMat = if (id != null && id == selected) Materials.glassBright(color) else Materials.glass(color)
        val pole = Three.Mesh(poleGeo, Materials.metal())
        pole.asDynamic().rotation.x = PI / 2 // Y-axis cylinder → vertical (Z up)
        pole.asDynamic().scale.set(1.0, poleScale(level), 1.0) // grow height (local Y) only
        place(pole.asDynamic(), x, y, poleH / 2)
        val gasket = Three.Mesh(gasketGeo, Materials.rubber()) // torus in XY → flat ring around the pole top
        place(gasket.asDynamic(), x, y, poleH)
        val orb = Three.Mesh(topGeo, glassMat)
        place(orb.asDynamic(), x, y, orbCenterZ(level))
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
        val resoGroup = buildResonators(parent, x, y, level, resos, id)
        return arrayOf(orb, gasket, pole, resoGroup)
    }

    /** 8 rubber slot-rings around the pole collar; a colour-coded rod stands in each filled slot. */
    private fun buildResonators(parent: dynamic, x: Double, y: Double, level: Double, resos: Map<Octant, Int>, id: String? = null): dynamic {
        val group = Three.Group()
        val poleH = poleHeight(level)
        val rodLen = poleH * RESO_ROD_LEN_FRAC
        val ringR = POLE_R * RESO_RADIUS_FRAC
        Octant.values().forEachIndexed { i, octant ->
            val ang = i * PI / 4.0
            val ox = ringR * cos(ang)
            val oy = ringR * sin(ang)
            val lvl = resos[octant]
            if (lvl != null) {
                // Rod hangs from a pivot/joint at its TOP, so a hack swings its loose bottom end
                // radially outward (centrifuge) while the top stays put. Tagged for the hack update.
                val pivot = Three.Group()
                pivot.asDynamic().position.set(ox, oy, rodLen) // joint at the rod top
                pivot.asDynamic().userData.isRodPivot = true
                pivot.asDynamic().userData.baseAngle = ang
                pivot.asDynamic().userData.targetX = ox // DeployFx slides the rod out to here on deploy
                pivot.asDynamic().userData.targetY = oy
                id?.let { DeployFx.bind(it, octant, pivot.asDynamic()) } // lerp it in if just deployed
                val rod = Three.Mesh(resoRodGeo, Materials.resonator(LevelColor.map[lvl] ?: "#ffffff"))
                rod.asDynamic().rotation.x = PI / 2 // unit Y-cylinder → vertical
                rod.asDynamic().scale.set(1.0, rodLen, 1.0)
                rod.asDynamic().position.set(0.0, 0.0, -rodLen / 2.0) // hangs down to the grommet
                pivot.asDynamic().add(rod)
                // The grommet is part of the reso: it rides INSIDE the pivot at the rod's bottom, so it
                // centrifuges out with the rod on a hack instead of staying stuck to the pole collar.
                val ring = Three.Mesh(resoRingGeo, Materials.rubber())
                ring.asDynamic().position.set(0.0, 0.0, -rodLen) // rod bottom, in pivot-local space
                pivot.asDynamic().add(ring)
                group.asDynamic().add(pivot)
            } else {
                // Empty slot: a bare grommet sits flat on the collar (nothing to swing).
                val ring = Three.Mesh(resoRingGeo, Materials.rubber())
                ring.asDynamic().position.set(ox, oy, 0.0)
                group.asDynamic().add(ring)
            }
        }
        group.asDynamic().position.set(x, y, poleH * RESO_COLLAR_FRAC)
        parent.add(group)
        return group
    }

    /**
     * Deployed shields: chrome mods in a tetrahedron inside the orb + a sci-fi shield bubble at φ× the
     * orb radius. Added as children of the [orb] so they inherit its per-level scale + grow-in tween.
     */
    private fun buildMods(orb: dynamic, portal: Portal) {
        val mods = portal.mods.values.toList()
        if (mods.isEmpty()) return
        val r = TOP_R * MOD_RING_FRAC / sqrt(3.0) // normalize the √3-magnitude tetra verts to the ring radius
        mods.forEachIndexed { i, mod ->
            val v = TETRA[i % TETRA.size]
            val mesh = Three.Mesh(modGeoFor(mod.modType()), Materials.resonator(mod.rarity.color))
            mesh.asDynamic().position.set(v[0] * r, v[1] * r, v[2] * r)
            if (mod.modType() == ModType.LINK_AMP) mesh.asDynamic().rotation.set(0.62, 0.62, 0.0) // cube on its diagonal
            orb.add(mesh) // orb is already dynamic (no .asDynamic())
        }
        if (mods.any { it is Shield }) { // the energy bubble reads "shielded"
            val color = portal.owner?.faction?.color ?: NEUTRAL_COLOR
            val bubble = Three.Mesh(shieldGeo, ShieldShader.material(color, portal.totalMitigation() / 100.0))
            orb.add(bubble)
        }
    }

    private fun modGeoFor(type: ModType): dynamic = when (type) {
        ModType.SHIELD -> dodecaGeo
        ModType.HEAT_SINK -> pentaGeo
        ModType.LINK_AMP -> cubeGeo
    }

    /** Rise the pole + grow the orb from the ground for the build-in animation ([g] = 0→1). */
    private fun applyBuildGrow(level: Double, g: Double, parts: Array<dynamic>, reform: Double = 1.0) {
        val gg = g.coerceAtLeast(0.0)
        val poleH = poleHeight(level)
        val s = orbScale(level) * gg * reform
        parts[2].scale.set(1.0, poleScale(level) * gg, 1.0) // pole
        parts[2].position.z = poleH * gg / 2.0
        parts[1].position.z = poleH * gg // gasket
        parts[0].scale.set(s, s, s) // orb
        parts[0].position.z = poleH * gg + TOP_R * s
        parts[3].scale.set(gg, gg, gg) // resonators grow in with the collar
        parts[3].position.z = poleH * gg * RESO_COLLAR_FRAC
    }

    private fun activeShowcase() = selectedShowcase ?: showcases.lastOrNull()

    private fun showcaseNear(location: Pos): Showcase? = showcases.minByOrNull { it.pos.distanceTo(location) }?.takeIf { it.pos.distanceTo(location) < SHOWCASE_SELECT_R }

    /** Demo LMB: select the portal under the cursor, or place a new one if the spot is clear (so two
     *  portals can't be stacked and existing ones can be picked, not replaced). Returns true if it
     *  placed a new portal (the caller plays the create sound). */
    fun clickShowcase(location: Pos, level: Int, color: String): Boolean {
        val near = showcaseNear(location)
        if (near != null) {
            selectedShowcase = near
            return false
        }
        placeShowcase(location, level, color)
        return true
    }

    /** Place a portal at [location]/[level] in the sync-immune demo group, and select it. */
    fun placeShowcase(location: Pos, level: Int, color: String) {
        val grp = showcaseGroup ?: return
        val group = Three.Group()
        val resos = Octant.values().associateWith { level } // demo: show a full set at the placed level
        val parts = buildPortal(group, location, level.toDouble(), color, null, resos)
        applyBuildGrow(level.toDouble(), 0.0, parts) // start collapsed; grows in via updateShowcases
        grp.add(group)
        val sc = Showcase(group, parts, location, level, color, 0.0, 0.0)
        showcases.add(sc)
        selectedShowcase = sc
    }

    /** Demo RMB: shatter + remove the placed portal nearest [location]. */
    fun removeShowcaseNear(location: Pos) {
        val target = showcases.minByOrNull { it.pos.distanceTo(location) } ?: return
        showcaseGroup?.remove(target.group)
        showcases.remove(target)
        dropDemoLinksFor(target)
        if (target === selectedShowcase) selectedShowcase = null
        val resos = Octant.values().associateWith { target.level } // demo shows a full set → all fall
        shatterPortal(target.pos, target.color, target.level, resos)
    }

    /** Demo (Hack button): spin the active (selected, else last) portal's resonator collar. */
    fun hackActiveShowcase(glyph: Boolean = false) {
        activeShowcase()?.let {
            it.hackAge = if (glyph) HackFx.GLYPH_SPIN_S else HackFx.SPIN_S
            it.hackGlyph = glyph
        }
    }

    /** Demo (XMP buttons): detonate a level-[level] XMP fireball at the active portal. */
    fun xmpActiveShowcase(level: Int) {
        activeShowcase()?.let { playXmpBurst(it.pos, level) }
    }

    /** Demo (Upgrade/Downgrade): re-place the active portal at level±[delta] (grows in at the new size). */
    fun stepLastShowcaseLevel(delta: Int) {
        val target = activeShowcase() ?: return
        val newLevel = (target.level + delta).coerceIn(1, 8)
        if (newLevel == target.level) return
        val pos = target.pos
        val up = newLevel > target.level
        showcaseGroup?.remove(target.group)
        showcases.remove(target)
        dropDemoLinksFor(target) // the old showcase object is gone — drop its link pipes
        placeShowcase(pos, newLevel, target.color) // re-places + re-selects
        if (up) SoundUtil.playUpgradeSound(pos, newLevel) else SoundUtil.playDowngradeSound(pos, newLevel)
    }

    /** Demo: move the ground cursor ring to [location] (null hides it); colour shows select vs place. */
    fun updateDemoCursor(location: Pos?) {
        val grp = showcaseGroup ?: return
        val cursor = demoCursor ?: run {
            val r = SHOWCASE_SELECT_R * metersPerPixel
            Three.Mesh(Three.RingGeometry(r * 0.82, r, 28), markerMaterial(NEUTRAL_COLOR)).also {
                demoCursor = it
                grp.add(it)
            }
        }
        if (location == null) {
            cursor.visible = false
            return
        }
        cursor.visible = true
        cursor.asDynamic().position.set(sceneX(location), sceneY(location), OVERLAY_Z)
        cursor.asDynamic().material.color.set(if (showcaseNear(location) != null) HIGHLIGHT_COLOR else NEUTRAL_COLOR)
    }

    /** Demo (Link): glass-pipe the two most recently placed portals' orbs. */
    fun linkLastShowcases() {
        val grp = showcaseGroup ?: return
        if (showcases.size < 2) return
        val a = showcases[showcases.size - 1]
        val b = showcases[showcases.size - 2]
        val pa = doubleArrayOf(sceneX(a.pos), sceneY(a.pos), orbCenterZ(a.level.toDouble()))
        val pb = doubleArrayOf(sceneX(b.pos), sceneY(b.pos), orbCenterZ(b.level.toDouble()))
        val tube = Three.Mesh(linkGeo, Materials.linkGlass(a.color))
        orientTube(tube.asDynamic(), pa, pb)
        grp.add(tube)
        val core = Three.Mesh(coreGeo, Materials.linkCore(a.color))
        orientTube(core.asDynamic(), pa, pb)
        grp.add(core)
        demoLinks.add(DemoLink(a, b, tube, core)) // tracked so it's removed if either portal goes
    }

    /** Demo: remove any link pipes touching [sc] — no link may dangle without both end portals. */
    private fun dropDemoLinksFor(sc: Showcase) {
        demoLinks.filter { it.a === sc || it.b === sc }.forEach {
            showcaseGroup?.remove(it.tube)
            showcaseGroup?.remove(it.core)
        }
        demoLinks.removeAll { it.a === sc || it.b === sc }
    }

    private fun updateShowcases(dt: Double) {
        showcases.forEach { sc ->
            if (sc.growAge < PORTAL_GROW_S) { // build-in: pole rises + orb grows from the ground
                sc.growAge += dt
                applyBuildGrow(sc.level.toDouble(), (sc.growAge / PORTAL_GROW_S).coerceIn(0.0, 1.0), sc.parts)
            }
            if (sc.hackAge > 0.0) { // hack: collar spins + rods centrifuge (same as live portals)
                sc.hackAge = (sc.hackAge - dt).coerceAtLeast(0.0)
                val dir = if (sc.color == Faction.ENL.color) -1.0 else 1.0 // ENL cw, else ccw
                val dur = if (sc.hackGlyph) HackFx.GLYPH_SPIN_S else HackFx.SPIN_S
                HackFx.spinShowcase(sc.parts[3], dur - sc.hackAge, dir, sc.hackGlyph)
            }
        }
    }

    private fun showcasesAnimating() = showcases.any { it.growAge < PORTAL_GROW_S || it.hackAge > 0.0 }

    private fun addAgent(agent: Agent) {
        val x = sceneX(agent.pos)
        val y = sceneY(agent.pos)
        val id = "agent:${agent.name}"
        val color = if (selected == id) HIGHLIGHT_COLOR else agent.faction.color
        val sphere = Three.Mesh(headGeo, Materials.solid(color))
        place(sphere.asDynamic(), x, y, HEAD_Z)
        tag(sphere.asDynamic(), id)
        agentsGroup.add(sphere)
        // Action indicator: a camera-facing billboard just above the head.
        val sprite = Three.Sprite(indicatorMaterial(agent.action.item, agent.faction))
        sprite.asDynamic().position.set(x, y, INDICATOR_Z)
        sprite.asDynamic().scale.set(INDICATOR_SIZE, INDICATOR_SIZE, 1.0)
        indicatorsGroup.add(sprite)
        if (Debug.enabled && StuckTracker.isStuck(agent.key())) addStuckMarker(x, y)
    }

    // ?debug: a vivid marker floating over an entity flagged as stuck/looping (see StuckTracker).
    private val stuckGeo: dynamic by lazy { Three.SphereGeometry(2.2, 8, 8) }
    private fun addStuckMarker(x: Double, y: Double) {
        val marker = Three.Mesh(stuckGeo, Materials.solid("#ff2d2d"))
        place(marker.asDynamic(), x, y, INDICATOR_Z + 3.5)
        indicatorsGroup.add(marker)
    }

    private fun addNpc(npc: NonFaction) {
        val sphere = Three.Mesh(headGeo, Materials.solid(NEUTRAL_COLOR))
        if (Debug.enabled && StuckTracker.isStuck("npc:${npc.id}")) addStuckMarker(sceneX(npc.pos), sceneY(npc.pos))
        // Marble drop-in: on first appearance the NPC falls from the sky (accelerating, 1−f²) to head
        // height. Per-NPC start height (by id) so a crowd reads as scattered marbles, not a flat sheet.
        val f = Spawns.appearRaw("npc:${npc.id}", NPC_DROP_S)
        val h = NPC_DROP_HEIGHT * (0.55 + 0.45 * ((npc.id * 37) % 100) / 100.0)
        val z = HEAD_Z + h * (1.0 - f * f)
        place(sphere.asDynamic(), sceneX(npc.pos), sceneY(npc.pos), z)
        npcsGroup.add(sphere)
    }

    /** A stray-XM heap: a small additive glow mote, scaled a touch by how much XM it holds. */
    private fun addXm(pos: Pos, heap: XmHeap) {
        val mote = Three.Mesh(xmGeo, Materials.xmGlow())
        val s = 0.85 + (heap.xm / 300.0).coerceIn(0.0, 1.0) * 0.5 // bigger heaps glow a touch larger
        mote.asDynamic().scale.set(s, s, s)
        place(mote.asDynamic(), sceneX(pos), sceneY(pos), XM_Z)
        xmGroup.add(mote)
    }

    /**
     * A link is a thin glass pipe between the two portals' orbs (à la qlippostasis tubing): a
     * brighter glass shell ([Materials.linkGlass]) around an additive **plasma core** filament, so
     * the link still reads strongly even though the orb glass is near-transparent at pipe radius.
     */
    private fun addLink(link: Link) {
        val color = link.creator.faction.color
        val a = orbPos(link.origin) // both ends ride the tweened orb height
        val b = orbPos(link.destination)
        val tube = Three.Mesh(linkGeo, Materials.linkGlass(color))
        orientTube(tube.asDynamic(), a, b)
        linksGroup.add(tube)
        val core = Three.Mesh(coreGeo, Materials.linkCore(color))
        orientTube(core.asDynamic(), a, b)
        linksGroup.add(core)
    }

    /** Place a unit (Y-axis) cylinder so it spans [a]→[b]: midpoint, Y-scaled to length, Y rotated to dir. */
    private fun orientTube(mesh: dynamic, a: DoubleArray, b: DoubleArray) {
        val dx = b[0] - a[0]
        val dy = b[1] - a[1]
        val dz = b[2] - a[2]
        val len = sqrt(dx * dx + dy * dy + dz * dz)
        if (len < 0.001) return
        mesh.position.set((a[0] + b[0]) / 2.0, (a[1] + b[1]) / 2.0, (a[2] + b[2]) / 2.0)
        mesh.scale.set(1.0, len, 1.0)
        val quat = Three.Quaternion().setFromUnitVectors(Three.Vector3(0.0, 1.0, 0.0), Three.Vector3(dx / len, dy / len, dz / len))
        mesh.quaternion.copy(quat)
    }

    /** A control field is an animated plasma sheet across the three portals' orbs; it fills in on creation. */
    private fun addField(field: Field) {
        val a = orbPos(field.origin)
        val b = orbPos(field.primaryAnchor)
        val c = orbPos(field.secondaryAnchor)
        val cx = (a[0] + b[0] + c[0]) / 3.0
        val cy = (a[1] + b[1] + c[1]) / 3.0
        val cz = (a[2] + b[2] + c[2]) / 3.0
        // Vertices relative to the centroid so the mesh can scale in/out from its centre.
        val rel = arrayOf(
            doubleArrayOf(a[0] - cx, a[1] - cy, a[2] - cz),
            doubleArrayOf(b[0] - cx, b[1] - cy, b[2] - cz),
            doubleArrayOf(c[0] - cx, c[1] - cy, c[2] - cz),
        )
        val color = field.owner.faction.color
        val fid = fieldId(field)
        fieldRecords[fid] = FieldRecord(cx, cy, cz, rel, color) // remembered for the teardown dissolve
        val geo = Three.BufferGeometry().setFromPoints(rel.map { Three.Vector3(it[0], it[1], it[2]) }.toTypedArray())
        val mesh = Three.Mesh(geo, PlasmaShader.material(color))
        mesh.asDynamic().position.set(cx, cy, cz)
        val g = Spawns.appear(fid, FIELD_FILL_S)
        if (g < 1.0) mesh.asDynamic().scale.set(g.coerceAtLeast(0.0), g.coerceAtLeast(0.0), g.coerceAtLeast(0.0))
        fieldsGroup.add(mesh)
    }

    /** Spawn the dissolve effect for any fields that vanished this sync (and play the collapse sound). */
    private fun teardownGone(gone: Set<String>) {
        gone.forEach { id ->
            val rec = if (id.startsWith("field:")) fieldRecords.remove(id) else null
            if (rec != null) {
                FieldFx.dissolve(rec.cx, rec.cy, rec.cz, rec.rel, rec.color)
                SoundUtil.playFieldDownSound()
            }
            displayedLevel.remove(id) // forget removed portals' level tween
            CaptureFx.forget(id) // forget removed portals' capture/colour state
        }
    }

    // Use the eased/displayed level (not the raw level) so links + fields ride the orb as it tweens
    // up/down on a level change, instead of snapping to the final height while the orb is mid-tween.
    // (addPortal updates displayedLevel before addField/addLink run in sync, so this reads fresh.)
    private fun displayedOrbLevel(portal: Portal): Double = displayedLevel["portal:${portal.id}"] ?: portal.getLevel().toInt().toDouble()

    private fun orbPos(portal: Portal): DoubleArray = doubleArrayOf(sceneX(portal.location), sceneY(portal.location), orbCenterZ(displayedOrbLevel(portal)))

    /** Stable id for a field, independent of which corner is "origin" (its three portals, sorted). */
    private fun fieldId(field: Field): String = listOf(field.origin.id, field.primaryAnchor.id, field.secondaryAnchor.id).sorted().joinToString("|", "field:")

    private fun lineMaterial(color: String): dynamic = materialCache.getOrPut("l$color") {
        val p: dynamic = js("({})")
        p.color = color
        Three.LineBasicMaterial(p)
    }

    private fun indicatorMaterial(item: ActionItem, faction: Faction): dynamic = spriteCache.getOrPut(item.text + faction.abbr) {
        val texture = Three.CanvasTexture(ActionItem.getHiResIcon(item, faction)) // hi-res → crisp when scaled up
        val p: dynamic = js("({})")
        p.map = texture
        p.depthTest = false
        p.transparent = true
        Three.SpriteMaterial(p)
    }

    /** A camera-facing name + level billboard floating just above the portal's orb (cleared each sync). */
    private fun addPortalLabel(portal: Portal, level: Double) {
        val z = orbCenterZ(level) + TOP_R * orbScale(level) + LABEL_GAP
        val sprite = Three.Sprite(portalLabelMaterial(portal.name, portal.getLevel().toInt()))
        sprite.asDynamic().position.set(sceneX(portal.location), sceneY(portal.location), z)
        sprite.asDynamic().scale.set(LABEL_W, LABEL_W * LABEL_CANVAS_H / LABEL_CANVAS_W, 1.0)
        indicatorsGroup.add(sprite)
    }

    // Name + level label material, cached by (name, level) so the canvas/texture is drawn once.
    private fun portalLabelMaterial(name: String, level: Int): dynamic = spriteCache.getOrPut("plabel:$name|$level") {
        val p: dynamic = js("({})")
        p.map = Three.CanvasTexture(drawPortalLabel(name, level))
        p.depthTest = false
        p.transparent = true
        Three.SpriteMaterial(p)
    }

    // White, dark-outlined text on a transparent canvas (label is neutral UI — no faction hue).
    private fun drawPortalLabel(name: String, level: Int): HTMLCanvasElement {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = LABEL_CANVAS_W
        canvas.height = LABEL_CANVAS_H
        val ctx = canvas.getContext("2d").asDynamic()
        ctx.textAlign = "center"
        ctx.textBaseline = "middle"
        ctx.lineJoin = "round"
        val cx = LABEL_CANVAS_W / 2.0
        val shown = if (name.length > 18) name.take(17) + "…" else name
        fun line(text: String, y: Double, px: Int, fill: String) {
            ctx.font = "600 ${px}px 'Chakra Petch', sans-serif"
            ctx.lineWidth = 5.0
            ctx.strokeStyle = "rgba(0, 0, 0, 0.85)"
            ctx.strokeText(text, cx, y)
            ctx.fillStyle = fill
            ctx.fillText(text, cx, y)
        }
        line(shown, 30.0, 28, "#ffffff")
        line("L$level", 72.0, 34, "#e8e8e8")
        return canvas
    }
}
