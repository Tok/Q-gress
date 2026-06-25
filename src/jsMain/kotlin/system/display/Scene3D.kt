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
import items.level.XmpLevel
import kotlinx.browser.window
import portal.Field
import portal.Link
import portal.Octant
import portal.Portal
import portal.XmHeap
import portal.XmMap
import util.BuildingShake
import util.Debug
import util.SoundUtil
import util.data.Pos
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
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
    private const val XM_R = HEAD_R / 1.618 // stray-XM mote radius: 1.618× (φ) smaller than an agent head
    private const val XM_Z = 1.2 // stray-XM floats just above the ground
    private const val NPC_DROP_S = 1.7 // seconds for an NPC to fall in from the sky on first appearance
    private const val NPC_DROP_HEIGHT = 650.0 // metres an NPC drops from (well off-screen → a long plunge in)
    private const val INDICATOR_Z = 5.0 // raised to clear the head now the indicator is ~3× bigger
    private const val INDICATOR_SIZE = 4.8 // action label above an agent (was 1.6 — barely visible)
    private const val INDICATOR_THICK = 1.2 // action-coin thickness (the extruded "wheel" depth)
    private const val COIN_BODY_OPACITY = 0.5 // the coin body (rim + underside) is see-through; the top icon stays solid
    private const val ENERGY_BAR_R = 0.55 // energy-bar cylinder radius
    private const val ENERGY_BAR_GAP = 1.2 // gap between the coin and the bar above it
    private const val ENERGY_BAR_OPACITY = 0.9
    private const val ENERGY_BAR_FILL_FRAC = 1.06 // fill slightly fatter than the black backing so it reads
    private const val ENERGY_BAR_EPS = 0.08 // fill overhangs the backing caps by this → no z-fighting when full

    // tallest bar (at max XM capacity); coin foreshortens flat so on-screen it reads taller
    private const val ENERGY_BAR_MAX_H = INDICATOR_SIZE * 3.0
    private const val MAX_XM_CAPACITY = 14400 // Agent.xmCapacity at L16+ (where capacity stops growing)
    private const val NAME_RING_GAP = 2.0 // gap above the orb top for the hovered-portal name ring (PortalNameTicker)
    private const val POLE_R = 2.0
    private const val LINK_R = 0.7 // link pipe radius (metres)
    private const val PORTAL_GROW_S = 0.7 // seconds for a new portal to inflate in (pole rises, orb pops)
    private const val RESO_POP_DELAY = 0.3 // resonators start popping in once the pole is ~30% up
    private const val CAPTURE_SHATTER_WEIGHT = 0.22 // glass-shatter heaviness on capture (light — only the orb)
    private const val FIELD_FILL_S = 0.4 // seconds for a new control field to fill in
    private const val LEVEL_TWEEN_RATE = 0.18 // per-sync ease of the rendered level toward the real one
    private const val POLE_H = 22.5 // base pole height at L1; scales by φ per level
    private const val TOP_R = 7.0 // base orb radius
    private const val INNER_SHELL_FRAC = 0.89 // inner glass shell radius (× orb) — a thin wall (~2.5× thinner) matching the shards
    private const val PHI = 1.618 // golden ratio — used for the shield bubble radius

    // Resonators: 8 rubber slot-rings around the pole collar (just below the gasket), each holding a
    // colour-coded rod (the resonator) when filled.
    private const val RESO_RING_R = POLE_R * 0.42 // grommet ring radius
    private const val RESO_RING_TUBE = POLE_R * 0.13
    private const val GROMMET_COLOR = "#0a0a0a" // black rubber grommet (matches the gasket) when it falls
    private const val RESO_ROD_R = POLE_R * 0.26
    private const val RESO_RADIUS_FRAC = 1.7 // slot distance from pole axis (× POLE_R) — spread so slots read distinct top-down
    private const val RESO_COLLAR_FRAC = 0.78 // collar height as a fraction of the pole height
    private const val RESO_ROD_LEN_FRAC = 0.22 // rod length as a fraction of the pole height
    private const val NEUTRAL_COLOR = "#bbbbbb"
    private const val MOD_R_FRAC = 0.16 // chrome mod radius (× orb radius)
    private const val MOD_SCALE = 1.2 // scale the mod solids up a touch (they read bland at base size)
    private val MOD_WIRE_SCALES = doubleArrayOf(1.01, 1.05) // two concentric edge cages → a bolder glowing wire
    private const val MAX_SHIELD_SHELLS = 4 // up to 4 shields per portal → 4 concentric bubbles
    private const val SHIELD_SHELL_STEP = 0.09 // each shield shell sits this much larger than the last (× radius)
    private const val SHIELD_WAVE_RANGE_FRAC = 0.6 // a blast ripples shields within this × the XMP's range
    private const val DAMAGE_NUMBER_GAP = 2.0 // start the damage number this far above the flask top
    private const val MAX_BUILDING_COLLIDERS = 1500 // cap on static building boxes added to the FX worlds

    // tetrahedron vertex distance from orb centre (× orb radius); nudged out so mods clear the link joint
    private const val MOD_RING_FRAC = 0.55

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
    private const val WALL_THICK = 4.0 // boundary wall thickness (scene metres) — a wall with depth, not a film
    private const val ELLIPSE_SEGMENTS = 72 // round-field boundary outline resolution
    private const val HEIGHT_N = 33 // terrain elevation grid resolution (N×N samples over the play area)
    const val CUSTOM_LAYER_ID = "qgress-3d" // MapLibre layer id for the three.js scene

    // Currently selected entity, as "portal:<id>" / "agent:<name>" (see pick()).
    var selected: String? = null
        set(value) {
            field = value
            refreshNameTicker() // selecting a portal spins up its 3D name ring (cleared on deselect / agent)
        }

    private var scene: Three.Scene? = null
    private var camera: Three.Camera? = null
    private var renderer: Three.WebGLRenderer? = null

    private var originMerc: dynamic = null
    private var metersScale = 1.0 // mercator units per metre at the origin
    private var terrainMap: MapLibre.Map? = null // for DEM elevation sampling

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

    /** Draw the play-area boundary (wall + outline + dim mask). Off for the title scene. */
    var showBorder = true

    // false (default): the sim shares the map depth buffer, so 3D buildings occlude portals/agents
    // (realistic). true (accessibility): clear depth first → the whole sim draws over buildings so
    // actions are never hidden. XMP/explosions stay depthTest=false either way (always on top).
    var drawOverBuildings = false

    // Set by TitleSim before the layer is added → onAdd loads the 3D wordmark + calls this once it's in
    // (e.g. to hide the DOM wordmark). Null in-game (no 3D wordmark there).
    var titleWordmarkOnReady: (() -> Unit)? = null

    // Glass-orb shatter fracture variants (loaded once from the GLB via ShardAssets).
    private var flaskVariants: List<List<dynamic>> = emptyList()
    private var flaskScale = 1.0 // scale a flask variant to ≈ the portal top sphere
    private var showcaseGroup: dynamic = null // demo-scene placed portals (not cleared by sync)
    private class Showcase(
        val group: dynamic,
        val parts: Array<dynamic>,
        val pos: Pos,
        var level: Int,
        val color: String,
        var hackAge: Double,
        var growAge: Double,
    ) {
        var hackGlyph: Boolean = false // the active hack is a (stronger) glyph hack
    }
    private val showcases = mutableListOf<Showcase>()
    private class DemoLink(val a: Showcase, val b: Showcase, val pipe: dynamic)
    private val demoLinks = mutableListOf<DemoLink>() // demo link pipes, so they can't outlive their portals
    private var selectedShowcase: Showcase? = null // demo: the portal the action buttons act on
    private var demoCursor: dynamic = null // demo: ground ring under the mouse (place vs select)
    private var lastFrameMs = 0.0 // for per-frame effect dt
    private var animClockMs = 0.0 // sim-scaled animation clock (advances by dt × animationSpeed each frame)

    /** Simulation speed multiplier (set by the speed control); scales all visual animations. */
    var animationSpeed = 1.0

    /** The sim-scaled animation clock in ms — FX read this instead of wall-clock so they track sim speed. */
    fun animMs() = animClockMs

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

    // EdgesGeometry per mod shape (cached): only the real polygon edges, not the triangulation.
    private val modEdgesCache = mutableMapOf<ModType, dynamic>()
    private fun modEdges(type: ModType): dynamic = modEdgesCache.getOrPut(type) { Three.EdgesGeometry(modGeoFor(type)) }
    private val pentaGeo: dynamic by lazy {
        Three.CylinderGeometry(TOP_R * MOD_R_FRAC, TOP_R * MOD_R_FRAC, TOP_R * MOD_R_FRAC * 0.55, 5)
    } // heat-sink radiator
    private val cubeGeo: dynamic by lazy {
        Three.BoxGeometry(TOP_R * MOD_R_FRAC * 1.1, TOP_R * MOD_R_FRAC * 1.1, TOP_R * MOD_R_FRAC * 1.1)
    } // link amp
    private val shieldGeo: dynamic by lazy { Three.SphereGeometry(TOP_R * PHI, 24, 18) } // shield bubble at φ× the orb
    private val gasketGeo: dynamic by lazy { Three.TorusGeometry(POLE_R * 1.15, POLE_R * 0.4, 10, 20) } // rubber donut
    private val linkGeo: dynamic by lazy { Three.CylinderGeometry(LINK_R, LINK_R, 1.0, 8) } // unit pipe (scaled to length)
    private val linkJointGeo: dynamic by lazy { Three.SphereGeometry(LINK_R * 1.5, 12, 12) } // ball-joint, a bit fatter than the pipe
    private val resoRingGeo: dynamic by lazy { Three.TorusGeometry(RESO_RING_R, RESO_RING_TUBE, 8, 14) } // rubber slot grommet
    private val indicatorGeo: dynamic by lazy {
        // action coin: a short cylinder (icon on the round faces)
        Three.CylinderGeometry(INDICATOR_SIZE / 2.0, INDICATOR_SIZE / 2.0, INDICATOR_THICK, 28)
    }
    private val resoRodGeo: dynamic by lazy { Three.CylinderGeometry(RESO_ROD_R, RESO_ROD_R, 1.0, 8) } // unit rod, scaled to length
    private val resoCapGeo: dynamic by lazy { Three.CircleGeometry(RESO_ROD_R * 0.92, 16) } // energy "surface" disc on the rod top
    private val materialCache = mutableMapOf<String, dynamic>()
    private val spriteCache = mutableMapOf<String, dynamic>()

    fun register(map: MapLibre.Map, originLng: Double, originLat: Double, anchorZoom: Double) {
        terrainMap = map
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
        newScene.add(Three.AmbientLight(0xffffff, 0.5)) // lowered so the moving sun's shadows read
        // The directional "sun" + shadows are set up by SunController.register below (needs the renderer).

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
        DamageNumberFx.register(newScene)
        OwnBuildings.register(newScene) // our own play-area building meshes (replace MapLibre's after gen)
        PortalNameTicker.register(newScene) // hovered-portal name ring (own group; spun each frame)
        BoltFx.register(newScene)
        XmFx.register(newScene)
        showcaseGroup = Three.Group()
        newScene.add(showcaseGroup)
        scene = newScene
        buildBorder()
        loadShatterAssets()
        titleWordmarkOnReady?.let { TitleWordmark.load(it) } // 3D title letters (own scene/pass; set by TitleSim)

        val params: dynamic = js("({})")
        params.canvas = map.getCanvas()
        params.context = gl
        params.antialias = true
        renderer = Three.WebGLRenderer(params).also { it.autoClear = false }
        // Moving sun + real shadows (portals/buildings → ground). Span ≈ play-area half-width in metres.
        val span = maxOf(Sim.width, Sim.height) / 2.0 * metersPerPixel
        val centre = Pos(Sim.width / 2, Sim.height / 2)
        SunController.register(newScene.asDynamic(), renderer.asDynamic(), span, groundZ(centre))
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
        // Advance the sim-scaled animation clock every frame (so plasma fields shimmer even when no
        // transient FX run). dt + the clock are scaled by [animationSpeed] so every animation — hack
        // spin, deploy/shatter, build-in, control-field shimmer — speeds up/slows with the sim speed.
        val nowMs = js("performance.now()") as Double
        val rawDt = if (lastFrameMs <= 0.0) 0.016 else ((nowMs - lastFrameMs) / 1000.0).coerceIn(0.0, 0.1)
        lastFrameMs = nowMs
        SunController.advance(rawDt) // real-time sun arc (independent of sim speed); speed set by Title/Html
        val dt = rawDt * animationSpeed
        animClockMs += dt * 1000.0
        PlasmaShader.setTime(animClockMs / 1000.0) // animate control fields (on the scaled clock)
        updateShields(animClockMs / 1000.0) // shield hex/rim animation + per-portal blast-absorb ripple
        // Decay building bobs back to rest (cheap when idle). Our own meshes bob in scene space; MapLibre's
        // fill-extrusion bobs via feature-state — only one is the live building set.
        if (OwnBuildings.REPLACE_BUILDINGS) {
            OwnBuildings.updateBobs(animClockMs / 1000.0)
            if (OwnBuildings.PARALLEL_MODE) BuildingShake.update(animClockMs / 1000.0) // MapLibre gap-fillers settle too
        } else {
            BuildingShake.update(animClockMs / 1000.0)
        }
        updateEffects(map, dt, invProj)
        PortalNameTicker.update(dt) // spin the hovered portal's name ring (no-op when nothing is hovered)
        tumbleModTetras() // gentle continuous tumble of the mod tetrahedra
        updateTitleWordmark(invProj, dt) // camera-lock the 3D title letters (no-op until loaded)
        VectorFieldOverlay.sync() // paced flow-field sweep; driven here (continuous loop) so it animates through world-gen too
        activeRenderer.resetState()
        // Realistic by default: keep the depth the map layers wrote so 3D buildings occlude the sim.
        // The accessibility toggle clears it first so portals/agents/links draw over buildings + terrain
        // (XMP/indicators that opt out with depthTest=false stay on top in both modes).
        if (drawOverBuildings) activeRenderer.clearDepth()
        activeRenderer.render(activeScene, cam)
        TitleWordmark.renderOverlay(activeRenderer, cam) // title letters: own pass, depth cleared → in front + self-occluding
        map.triggerRepaint()
    }

    private fun hasActiveEffects() = ShatterFx.hasActive() ||
        DamageNumberFx.hasActive() ||
        showcasesAnimating() ||
        HackFx.hasActive() ||
        DeployFx.hasActive() ||
        XmFx.hasActive() ||
        XmpBurst.hasActive() ||
        FieldFx.hasActive() ||
        BoltFx.hasActive()

    // Advance whatever transient FX are live (each self-guards). Split out of render() to keep it simple.
    private fun updateEffects(map: MapLibre.Map, dt: Double, invProj: dynamic) {
        if (!hasActiveEffects()) return
        if (ShatterFx.hasActive()) ShatterFx.update(dt)
        if (DamageNumberFx.hasActive()) DamageNumberFx.update(dt)
        if (BoltFx.hasActive()) BoltFx.update(dt)
        if (showcasesAnimating()) updateShowcases(dt)
        if (HackFx.hasActive()) HackFx.update()
        if (DeployFx.hasActive()) DeployFx.update()
        if (XmFx.hasActive()) XmFx.update()
        if (FieldFx.hasActive()) FieldFx.update(dt)
        if (XmpBurst.hasActive()) {
            val canvas = map.getCanvas()
            XmpBurst.setView(invProj, canvas.width as Double, canvas.height as Double)
            XmpBurst.update(dt)
        }
    }

    /** Rebuild the 3D objects from world state. Called once per simulation tick. */
    fun sync() {
        scene ?: return
        Spawns.beginSync()
        HackFx.resetBindings() // re-bound below as each portal's reso group is rebuilt
        DeployFx.resetBindings()
        modTetras.clear() // rebuilt by buildMods below
        shieldMats.clear() // rebuilt by addShieldShells below
        clear(portalsGroup)
        World.allPortals.forEach { addPortal(it) }
        syncPoleColliders()
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
        refreshNameTicker() // keep the selected portal's name ring positioned (level-ups / terrain resample)
        teardownGone(Spawns.endSync())
    }

    // Mark the playable area: a white outline plus a dark mask greying out everything beyond it.
    private fun buildBorder() {
        val group = borderGroup ?: return
        clear(group)
        if (!showBorder) return // title scene hides the play-area boundary
        val hx = sceneX(Pos(Sim.width, 0)) // play-area half-extents (scene metres); sceneY flips sim-y → +hy is the top edge
        val hy = sceneY(Pos(0, 0))
        val (gMin, gMax) = groundZRange()
        val wallH = (gMax - gMin) + WALL_HEIGHT // span the terrain range so the wall clears it everywhere
        if (Sim.roundField) {
            val r = minOf(hx, hy) // true circle = the smaller half-extent (matches the grid mask)
            PlayAreaMask.buildRoundMask(group, r, r, BORDER_Z - 0.05, OUTSIDE_DIM)
            PlayAreaMask.buildRoundWall(group, r, r, gMin, wallH)
            val rad = Sim.fieldRadius()
            val pts = (0..ELLIPSE_SEGMENTS).map {
                val t = it.toDouble() / ELLIPSE_SEGMENTS * 2.0 * PI
                val sp = Pos((Sim.width / 2.0 + rad * cos(t)).toInt(), (Sim.height / 2.0 + rad * sin(t)).toInt())
                Three.Vector3(sceneX(sp), sceneY(sp), groundZ(sp) + BORDER_Z) // outline follows the terrain edge
            }.toTypedArray()
            group.add(Three.Line(Three.BufferGeometry().setFromPoints(pts), lineMaterial(BORDER_COLOR)))
            return
        }
        PlayAreaMask.build(group, hx, hy, OUTSIDE_FAR * maxOf(hx, hy), BORDER_Z - 0.05, OUTSIDE_DIM)
        PlayAreaMask.buildWalls(group, hx, hy, wallH, WALL_THICK, gMin)
        val corners = arrayOf(Pos(0, 0), Pos(Sim.width, 0), Pos(Sim.width, Sim.height), Pos(0, Sim.height), Pos(0, 0))
        val points = corners.map { Three.Vector3(sceneX(it), sceneY(it), groundZ(it) + BORDER_Z) }.toTypedArray()
        group.add(Three.Line(Three.BufferGeometry().setFromPoints(points), lineMaterial(BORDER_COLOR)))
    }

    private fun groundZRange(): Pair<Double, Double> {
        if (!heightsReady) return 0.0 to 0.0
        var mn = heights[0]
        var mx = heights[0]
        for (h in heights) {
            if (h < mn) mn = h
            if (h > mx) mx = h
        }
        return mn to mx
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

    /** Drop the resonator parts when the portal shatters: every slot's rubber grommet o-ring + the
     *  colour-coded rod in each filled slot, each as a brief rigid body (like the glass shards). */
    private fun dropResonators(location: Pos, level: Double, resos: Map<Octant, Int>) {
        val poleH = poleHeight(level)
        val collarZ = groundZ(location) + poleH * RESO_COLLAR_FRAC
        val rodLen = poleH * RESO_ROD_LEN_FRAC
        val ringR = POLE_R * RESO_RADIUS_FRAC
        val x = sceneX(location)
        val y = sceneY(location)
        Octant.values().forEachIndexed { i, octant ->
            val ang = i * PI / 4.0
            val rx = x + ringR * cos(ang)
            val ry = y + ringR * sin(ang)
            ShatterFx.spawnFallingChunk(resoRingGeo, rx, ry, collarZ, 1.0, RESO_RING_R + RESO_RING_TUBE, GROMMET_COLOR)
            resos[octant]?.let { lvl ->
                ShatterFx.spawnFallingRod(resoRodGeo, rx, ry, collarZ + rodLen / 2.0, RESO_ROD_R, rodLen, LevelColor.map[lvl] ?: "#ffffff")
                // the top o-ring rides the rod top → tumble it out with the rod
                ShatterFx.spawnFallingChunk(resoRingGeo, rx, ry, collarZ + rodLen, 1.0, RESO_RING_R + RESO_RING_TUBE, GROMMET_COLOR)
            }
        }
    }

    /** Drop a single resonator rod from its slot — used as each reso is destroyed during an attack. */
    fun dropResonator(location: Pos, level: Int, octantIndex: Int, resoLevel: Int) {
        val poleH = poleHeight(level.toDouble())
        val rodLen = poleH * RESO_ROD_LEN_FRAC
        val ringR = POLE_R * RESO_RADIUS_FRAC
        val ang = octantIndex * PI / 4.0
        val rx = sceneX(location) + ringR * cos(ang)
        val ry = sceneY(location) + ringR * sin(ang)
        val collarZ = groundZ(location) + poleH * RESO_COLLAR_FRAC
        ShatterFx.spawnFallingRod(
            resoRodGeo,
            rx,
            ry,
            collarZ + rodLen / 2.0,
            RESO_ROD_R,
            rodLen,
            LevelColor.map[resoLevel] ?: "#ffffff",
        )
        // the reso's top o-ring rides the rod top → it falls out with the rod
        ShatterFx.spawnFallingChunk(resoRingGeo, rx, ry, collarZ + rodLen, 1.0, RESO_RING_R + RESO_RING_TUBE, GROMMET_COLOR)
    }

    /** Portal defense: a retaliation bolt arcs from the portal orb ([from]/[fromLevel]) to the attacker
     *  ([to], at head height), coloured by the owning faction. */
    fun fireBolt(from: Pos, fromLevel: Int, to: Pos, color: String) {
        scene ?: return
        val start = doubleArrayOf(sceneX(from), sceneY(from), groundZ(from) + orbCenterZ(fromLevel.coerceAtLeast(1).toDouble()))
        val end = doubleArrayOf(sceneX(to), sceneY(to), groundZ(to) + HEAD_Z)
        BoltFx.fire(start, end, color)
    }

    /** A collected stray-XM mote flies from [from] (the heap) to [to] (the agent) before vanishing. */
    fun collectXmFx(from: Pos, to: Pos) {
        scene ?: return
        XmFx.spawn(
            doubleArrayOf(sceneX(from), sceneY(from), groundZ(from) + XM_Z),
            doubleArrayOf(
                sceneX(to),
                sceneY(to),
                groundZ(to) + HEAD_Z,
            ),
        )
    }

    /** Hack/glyph loot: [count] motes drop from the portal's orb top down to the hacking agent. */
    fun rewardFx(portalLocation: Pos, level: Int, to: Pos, count: Int) {
        scene ?: return
        val top = doubleArrayOf(
            sceneX(portalLocation),
            sceneY(portalLocation),
            groundZ(portalLocation) + orbCenterZ(level.coerceAtLeast(1).toDouble()),
        )
        val dst = doubleArrayOf(sceneX(to), sceneY(to), groundZ(to) + HEAD_Z)
        repeat(count.coerceAtMost(8)) { XmFx.spawn(top, dst) }
    }

    /** Drop the deployed mods out of the orb when a portal is neutralized / removed. */
    fun dropMods(location: Pos, level: Int, mods: List<Mod>) {
        if (mods.isEmpty()) return
        val lv = level.toDouble()
        val s = orbScale(lv)
        val half = TOP_R * MOD_R_FRAC * s
        val gz = groundZ(location)
        mods.forEach { mod ->
            ShatterFx.spawnFallingChunk(
                modGeoFor(mod.modType()),
                sceneX(location),
                sceneY(location),
                gz + orbCenterZ(lv),
                s,
                half,
                mod.rarity.color,
            )
        }
    }

    /**
     * Fire an XMP detonation at a location, scaled by burster [level] (1..8). See [XmpBurst].
     * [sound] = false when the caller already plays the attack sound (the game's Queues path).
     */
    fun playXmpBurst(
        location: Pos,
        level: Int,
        sound: Boolean = true,
        squishXY: Double = 1.0,
        bright: Double = 1.0,
        ultra: Boolean = false,
    ) {
        scene ?: return
        val sx = sceneX(location)
        val sy = sceneY(location)
        val gz = groundZ(location)
        XmpBurst.play(sx, sy, gz, level, squishXY, bright) // the burst sits on the terrain and rises within its own volume
        // One shared blast origin: the mushroom-cloud centre, above the terrain, rising with level. Both
        // the gameplay shatter and the title wordmark fly their pieces out from it via BlastModel.
        val origin = doubleArrayOf(sx, sy, gz + BlastModel.cloudHeight(level))
        ShatterFx.recordBlast(origin, level) // new shatter pieces arc up-and-out, energy ∝ level / distance
        ShatterFx.applyBlast(origin, level) // …and pieces already mid-fall (shards/resos/mods/o-rings/gasket) get re-flung
        DamageNumberFx.applyBlast(origin, level) // already-falling damage digits get flung too
        TitleWordmark.flash(origin, level) // title letters get shoved (no-op until loaded)
        triggerShieldWaves(location, level) // nearby shields ripple as they absorb the blast
        // Buildings within the XMP's blast radius bob + settle (US rocks harder). Our own (visible) meshes
        // shake in scene space (sx/sy); in PARALLEL_MODE we ALSO shake MapLibre's gap-filler buildings via
        // feature-state (now addressable since the openmaptiles source has generateId). The MapLibre fallback
        // (no own meshes) shakes them alone.
        if (OwnBuildings.REPLACE_BUILDINGS) {
            OwnBuildings.applyBlast(sx, sy, XmpLevel.valueOf(level).rangeM.toDouble(), level, ultra, animClockMs / 1000.0)
            if (OwnBuildings.PARALLEL_MODE) {
                val ll = simPosToLngLat(location)
                BuildingShake.blast(ll[0], ll[1], XmpLevel.valueOf(level).rangeM, level, ultra, animClockMs / 1000.0)
            }
        } else {
            val ll = simPosToLngLat(location)
            BuildingShake.blast(ll[0], ll[1], XmpLevel.valueOf(level).rangeM, level, ultra, animClockMs / 1000.0)
        }
        if (sound) {
            if (ultra) SoundUtil.playUltraStrike(location) else SoundUtil.playXmpSound(location, level)
        }
    }

    /**
     * Build static box colliders from the rendered building footprints (lng/lat polygons + render_height)
     * so falling debris/digits land on roofs instead of dropping through buildings. One axis-aligned box
     * per building (cheap); base at z=0 (the FX ground plane). Call once after world-gen ([feats] from
     * MapUtil's queryRenderedFeatures on the building layer).
     */
    fun buildBuildingColliders(feats: dynamic) {
        scene ?: return
        // Lift the debris/digit physics floors to the terrain so falling pieces land on the ground, not
        // hundreds of metres below it at sea level (z=0).
        val centreGroundZ = groundZ(Pos(Sim.width / 2, Sim.height / 2))
        DamageNumberFx.setGroundZ(centreGroundZ)
        ShatterFx.setGroundZ(centreGroundZ)
        val total = (feats.length as? Int) ?: return
        var added = 0
        var i = 0
        while (i < total && added < MAX_BUILDING_COLLIDERS) {
            val f = feats[i]
            i++
            // Default a missing/zero render_height to ~8 m (same as the extrusion default) so small or
            // un-tagged buildings still get a collider and debris can't fall straight through them.
            val h = (f.properties?.render_height as? Double)?.takeIf { it > 0.5 } ?: 8.0
            if (addBuildingBox(f.geometry, h)) added++
        }
    }

    private fun addBuildingBox(geom: dynamic, h: Double): Boolean {
        val bb = lngLatBBox(geom) ?: return false
        val c0 = lngLatToSceneXY(bb[0], bb[1]) // exact float corners (no integer-cell rounding)
        val c1 = lngLatToSceneXY(bb[2], bb[3])
        val cx = (c0[0] + c1[0]) / 2.0
        val cy = (c0[1] + c1[1]) / 2.0
        val hx = abs(c1[0] - c0[0]) / 2.0
        val hy = abs(c1[1] - c0[1]) / 2.0
        if (hx < 0.3 || hy < 0.3) return false // skip only truly degenerate footprints
        // Sit the collider on the terrain (base at the building's ground), not at sea level z=0 — else it
        // sits ~hundreds of metres below the actual building and debris/digits fall straight through.
        val cz = groundZAtLngLat((bb[0] + bb[2]) / 2.0, (bb[1] + bb[3]) / 2.0) + h / 2.0
        ShatterFx.addStaticBox(cx, cy, cz, hx, hy, h / 2.0)
        DamageNumberFx.addStaticBox(cx, cy, cz, hx, hy, h / 2.0)
        return true
    }

    // Lng/lat bounding box [minLng, minLat, maxLng, maxLat] of a GeoJSON polygon, or null if empty.
    private fun lngLatBBox(geom: dynamic): DoubleArray? {
        val coords = geom?.coordinates ?: return null
        val bb = doubleArrayOf(1e9, 1e9, -1e9, -1e9)
        var any = false
        fun walk(node: dynamic) {
            if (!isDynArray(node)) return
            if ((node.length as Int) >= 2 && !isDynArray(node[0])) {
                val lng = node[0] as Double
                val lat = node[1] as Double
                bb[0] = minOf(bb[0], lng)
                bb[1] = minOf(bb[1], lat)
                bb[2] = maxOf(bb[2], lng)
                bb[3] = maxOf(bb[3], lat)
                any = true
                return
            }
            var k = 0
            val n = node.length as Int
            while (k < n) {
                walk(node[k])
                k++
            }
        }
        walk(coords)
        return if (any) bb else null
    }

    private fun isDynArray(v: dynamic): Boolean = js("Array.isArray")(v) as Boolean

    /** Pop a 3D damage number ([amount] XM) from the top of [portal]'s flask (flies up, falls, fades). */
    fun showDamageNumber(portal: Portal, amount: Int) {
        scene ?: return
        val level = portal.getLevel().value.toDouble()
        val flaskTop = groundZ(portal.location) + orbCenterZ(level) + TOP_R * orbScale(level)
        DamageNumberFx.spawn(sceneX(portal.location), sceneY(portal.location), flaskTop + DAMAGE_NUMBER_GAP, portal.location, amount)
    }

    // Ripple any nearby shielded portal's bubble — it "absorbs" the blast. Survivors wave + settle;
    // shields that the blast destroys just vanish on the next sync, so a triggered-but-dead one is harmless.
    private fun triggerShieldWaves(location: Pos, level: Int) {
        val radius = XmpLevel.valueOf(level).rangeM * SHIELD_WAVE_RANGE_FRAC
        val seconds = animClockMs / 1000.0
        World.allPortals.forEach { p ->
            if (p.mods.values.any { it is Shield } && p.location.distanceTo(location) <= radius) {
                ShieldWave.hit(p.id, seconds)
            }
        }
    }

    private fun updateShields(seconds: Double) {
        if (shieldMats.isEmpty()) return
        ShieldShader.setTime(seconds) // animate the hex lattice / pulse (was never driven before)
        ShieldShader.setEye(GlassShader.eye()) // camera-tracking Fresnel rim
        shieldMats.forEach { (mat, id) -> ShieldShader.setWave(mat, ShieldWave.amplitudeFor(id, seconds)) }
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

    /** Exact (un-rounded) scene XY in metres for a lng/lat. [lngLatToSimPos] rounds to an integer sim
     *  cell (fine for portal points) which visibly distorts/offsets building footprints — use this for those. */
    internal fun lngLatToSceneXY(lng: Double, lat: Double): DoubleArray {
        val merc = MapLibre.asDynamic().MercatorCoordinate.fromLngLat(arrayOf(lng, lat), 0.0)
        val east = (merc.x as Double - (originMerc.x as Double)) / metersScale // metres east of origin = sceneX
        val south = (merc.y as Double - (originMerc.y as Double)) / metersScale
        return doubleArrayOf(east, -south) // sceneY = -(south) (the scene's Y is up)
    }

    /** Inverse of [lngLatToSimPos]: a sim Pos → ground [lng, lat] (web-mercator unproject). */
    private fun simPosToLngLat(pos: Pos): DoubleArray {
        val mx = (originMerc.x as Double) + sceneX(pos) * metersScale
        val my = (originMerc.y as Double) - sceneY(pos) * metersScale // sceneY flips sim-y → undo it
        return doubleArrayOf(mx * 360.0 - 180.0, atan(sinh(PI * (1.0 - 2.0 * my))) * 180.0 / PI)
    }

    // --- Terrain heights: a coarse elevation grid sampled from the DEM; objects sit on it. ---
    private val heights = DoubleArray(HEIGHT_N * HEIGHT_N)
    private var heightsReady = false

    /** Whether the terrain height grid has been sampled — buildings must wait for it so they sit on the
     *  terrain (groundZ returns 0 until then, which would drop them to ground level). */
    fun terrainReady() = heightsReady

    /** Re-sample the elevation grid + rebuild the border on it (on terrain toggle / build). Retries cover DEM load. */
    fun onTerrainChanged() {
        sampleHeights()
        rebuildBorder()
        window.setTimeout({ resampleTerrain() }, 1500)
        window.setTimeout({ resampleTerrain() }, 4000)
    }

    private fun resampleTerrain() {
        sampleHeights()
        rebuildBorder()
    }

    private fun rebuildBorder() {
        val g = borderGroup ?: return
        clear(g)
        buildBorder()
    }

    private fun sampleHeights() {
        val map = terrainMap ?: return
        var any = false
        for (j in 0 until HEIGHT_N) {
            for (i in 0 until HEIGHT_N) {
                val px = i.toDouble() / (HEIGHT_N - 1) * Sim.width
                val py = j.toDouble() / (HEIGHT_N - 1) * Sim.height
                val ll = simPosToLngLat(Pos(px.toInt(), py.toInt()))
                // Null past the DEM source maxzoom (z15) or before tiles load — keep the last good
                // sample rather than zeroing it (which would sink objects to sea level on a re-sample).
                val e = map.asDynamic().queryTerrainElevation(arrayOf(ll[0], ll[1])) as? Double
                if (e != null) {
                    any = true
                    heights[j * HEIGHT_N + i] = e
                }
            }
        }
        if (any) heightsReady = true
    }

    /** Terrain elevation (scene metres) at an arbitrary [lng]/[lat] — works OUTSIDE the play-area height
     *  grid (for buildings streamed in as the camera flies elsewhere) by sampling the live DEM directly.
     *  Falls back to the sampled grid (then flat) when the DEM tile isn't loaded there yet. */
    fun groundZAtLngLat(lng: Double, lat: Double): Double {
        val m = terrainMap
        if (m != null) {
            val e = m.asDynamic().queryTerrainElevation(arrayOf(lng, lat)) as? Double
            if (e != null) return e
        }
        return groundZ(lngLatToSimPos(lng, lat))
    }

    /** Terrain elevation (scene metres) under a sim [pos]; 0 until the DEM has sampled (flat fallback). */
    fun groundZ(pos: Pos): Double {
        if (!heightsReady) return 0.0
        val gx = (pos.x / Sim.width * (HEIGHT_N - 1)).coerceIn(0.0, (HEIGHT_N - 1).toDouble())
        val gy = (pos.y / Sim.height * (HEIGHT_N - 1)).coerceIn(0.0, (HEIGHT_N - 1).toDouble())
        val i0 = gx.toInt().coerceAtMost(HEIGHT_N - 2)
        val j0 = gy.toInt().coerceAtMost(HEIGHT_N - 2)
        val fx = gx - i0
        val fy = gy - j0
        val h00 = heights[j0 * HEIGHT_N + i0]
        val h10 = heights[j0 * HEIGHT_N + i0 + 1]
        val h01 = heights[(j0 + 1) * HEIGHT_N + i0]
        val h11 = heights[(j0 + 1) * HEIGHT_N + i0 + 1]
        return (h00 * (1 - fx) + h10 * fx) * (1 - fy) + (h01 * (1 - fx) + h11 * fx) * fy
    }

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

    // Feed the live camera eye/forward/up to the 3D title wordmark so it can lock in front of the camera.
    private fun updateTitleWordmark(invProj: dynamic, dt: Double) {
        if (camera == null) return
        val eye = GlassShader.eye()
        val mid = unproject(invProj, 0.0, 0.0, 0.0)
        val far = unproject(invProj, 0.0, 0.0, 1.0)
        val top = unproject(invProj, 0.0, 1.0, 0.0)
        TitleWordmark.update(
            eye,
            doubleArrayOf(far[0] - eye[0], far[1] - eye[1], far[2] - eye[2]),
            doubleArrayOf(top[0] - mid[0], top[1] - mid[1], top[2] - mid[2]),
            dt,
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
    // Generous per-level growth so the level difference reads clearly: orb 0.45→1.6, pole 1.0→2.2×.
    private fun orbScale(level: Double) = 0.45 + (level.coerceIn(1.0, 8.0) - 1.0) / 7.0 * 1.15 // orb radius
    private fun poleScale(level: Double) = 1.0 + (level.coerceIn(1.0, 8.0) - 1.0) / 7.0 * 1.2 // pole height: 1 → 2.2
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
        val gz = groundZ(portal.location) // terrain height under this portal
        // Colour change → the OLD orb shatters in place, the new one pops back in (reform factor).
        // A *neutral* orb never shatters (no resonators = nothing there): only a captured (faction-
        // coloured) orb breaks, so capturing a neutral portal grows the faction orb in with no white
        // glass. Neutralising a portal still shatters its coloured orb.
        CaptureFx.check(id, baseColor) { old ->
            if (old == NEUTRAL_COLOR) return@check
            val lv = displayedLevel[id] ?: level
            ShatterFx.shatterOrb(
                sceneX(portal.location),
                sceneY(portal.location),
                gz + orbCenterZ(lv),
                orbScale(lv),
                old,
                flaskVariants,
                flaskScale,
            )
            SoundUtil.playGlassShatterSound(portal.location, CAPTURE_SHATTER_WEIGHT)
        }
        val reform = CaptureFx.reformFactor(id)
        // octant → (reso level, health 0..1) — both real-time, so the rod's energy bar tracks its charge.
        val resos = portal.resoMap().mapValues { Pair(it.value.getLevel(), it.value.calcHealthPercent() / 100.0) }
        // Selection keeps the faction hue but lights the orb brighter (no neutral-looking white tint);
        // buildPortal derives that from id == selected.
        val parts = buildPortal(portalsGroup, portal.location, level, baseColor, id, resos)
        buildMods(parts[0], portal) // chrome mods + shield bubble inside/around the orb (if shielded)
        HackFx.bind(id, parts[3]) // spin the collar if this portal is being hacked
        // Build-in: the pole rises and the orb grows from the ground; [reform] re-pops the orb only.
        val g = Spawns.appear(id, PORTAL_GROW_S)
        if (g < 1.0) {
            applyBuildGrow(level, g, parts, reform, gz)
        } else if (reform < 1.0) {
            applyBuildGrow(level, 1.0, parts, reform, gz) // orb pops back in after a capture
        }
    }

    // Refresh portal-pole colliders in the FX physics worlds so falling debris/digits hit the poles.
    private fun syncPoleColliders() {
        val specs = World.allPortals.map { p ->
            val gz = groundZ(p.location)
            val topZ = gz + poleHeight(p.getLevel().toInt().toDouble())
            doubleArrayOf(sceneX(p.location), sceneY(p.location), gz, topZ, POLE_R)
        }.toTypedArray()
        ShatterFx.setPoleColliders(specs)
        DamageNumberFx.setPoleColliders(specs)
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
        resos: Map<Octant, Pair<Int, Double>> = emptyMap(),
    ): Array<dynamic> {
        val x = sceneX(location)
        val y = sceneY(location)
        val gz = groundZ(location) // sit the whole portal on the terrain
        val poleH = poleHeight(level)
        val s = orbScale(level)
        // Selection lights the orb brighter (faction hue kept). Demo portals (id == null) never highlight.
        val glassMat = if (id != null && id == selected) Materials.glassBright(color) else Materials.glass(color)
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
                // The grommet is part of the reso: it rides INSIDE the pivot at the rod's bottom, so it
                // centrifuges out with the rod on a hack instead of staying stuck to the pole collar.
                val ring = Three.Mesh(resoRingGeo, Materials.rubber())
                ring.asDynamic().position.set(0.0, 0.0, -rodLen) // rod bottom, in pivot-local space
                pivot.asDynamic().add(ring)
                // A second black o-ring at the reso's TOP joint. It rides INSIDE the pivot (local origin = the
                // rod-top joint), so it TILTS with the rod on a hack (stays attached to the rod's top) and
                // tumbles out with the reso when the portal shatters / the reso is destroyed.
                val topRing = Three.Mesh(resoRingGeo, Materials.rubber())
                topRing.asDynamic().position.set(0.0, 0.0, 0.0)
                pivot.asDynamic().add(topRing)
                group.asDynamic().add(pivot)
            } else {
                // Empty slot: a bare grommet sits flat on the collar (nothing to swing).
                val ring = Three.Mesh(resoRingGeo, Materials.rubber())
                ring.asDynamic().position.set(ox, oy, 0.0)
                group.asDynamic().add(ring)
            }
        }
        group.asDynamic().position.set(x, y, gz + poleH * RESO_COLLAR_FRAC)
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

    // Shield bubble materials + their portal id, rebuilt each sync → driven per frame by ShieldWave (ripple).
    private val shieldMats = mutableListOf<Pair<dynamic, String>>()

    // Slowly tumble each mod tetrahedron on incommensurate sine drifts → a gentle, never-repeating spin
    // that keeps changing direction. Time-driven so it's smooth across the per-sync rebuild.
    private fun tumbleModTetras() {
        if (modTetras.isEmpty()) return
        val t = animClockMs / 1000.0
        modTetras.forEach { g ->
            g.rotation.x = sin(t * 0.11) * PI
            g.rotation.y = sin(t * 0.13 + 1.3) * PI
            g.rotation.z = sin(t * 0.17 + 2.6) * PI
        }
    }

    private fun modGeoFor(type: ModType): dynamic = when (type) {
        ModType.SHIELD -> dodecaGeo
        ModType.HEAT_SINK -> pentaGeo
        ModType.LINK_AMP -> cubeGeo
    }

    /** Rise the pole + grow the orb from the ground for the build-in animation ([g] = 0→1). */
    private fun applyBuildGrow(level: Double, g: Double, parts: Array<dynamic>, reform: Double = 1.0, gz: Double = 0.0) {
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

    private fun activeShowcase() = selectedShowcase ?: showcases.lastOrNull()

    private fun showcaseNear(location: Pos): Showcase? = showcases.minByOrNull { it.pos.distanceTo(location) }?.takeIf {
        it.pos.distanceTo(location) <
            SHOWCASE_SELECT_R
    }

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
        val resos = Octant.values().associateWith { Pair(level, 1.0) } // demo: a full set, full health
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
            it.hackAge = if (glyph) HackFx.GLYPH_SPIN_S else HackFx.HACK_S
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
        val pipe = Three.Mesh(linkGeo, Materials.linkGlass(a.color))
        orientTube(pipe.asDynamic(), pa, pb)
        grp.add(pipe)
        demoLinks.add(DemoLink(a, b, pipe)) // tracked so it's removed if either portal goes
    }

    /** Demo: remove any link pipes touching [sc] — no link may dangle without both end portals. */
    private fun dropDemoLinksFor(sc: Showcase) {
        demoLinks.filter { it.a === sc || it.b === sc }.forEach {
            showcaseGroup?.remove(it.pipe)
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
                val dur = if (sc.hackGlyph) HackFx.GLYPH_SPIN_S else HackFx.HACK_S
                HackFx.spinShowcase(sc.parts[3], dur - sc.hackAge, dir, sc.hackGlyph)
            }
        }
    }

    private fun showcasesAnimating() = showcases.any { it.growAge < PORTAL_GROW_S || it.hackAge > 0.0 }

    private fun addAgent(agent: Agent) {
        val x = sceneX(agent.pos)
        val y = sceneY(agent.pos)
        val gz = groundZ(agent.pos) // walk on the terrain
        val id = "agent:${agent.name}"
        val color = if (selected == id) HIGHLIGHT_COLOR else agent.faction.color
        val sphere = Three.Mesh(headGeo, Materials.solid(color))
        place(sphere.asDynamic(), x, y, gz + HEAD_Z)
        tag(sphere.asDynamic(), id)
        agentsGroup.add(sphere)
        // Action indicator: a 3D coin/wheel (icon on the round faces) hovering above the head.
        val coin = Three.Mesh(indicatorGeo, indicatorMaterial(agent.action.item, agent.faction))
        coin.asDynamic().rotation.x = PI / 2 // stand the cylinder's faces up (axis → world Z)
        place(coin.asDynamic(), x, y, gz + INDICATOR_Z)
        indicatorsGroup.add(coin)
        addEnergyBar(x, y, gz, agent) // XM gauge to the side (height scales with the agent's level)
        if (Debug.enabled && StuckTracker.isStuck(agent.key())) addStuckMarker(x, y, gz)
    }

    // ?debug: a vivid marker floating over an entity flagged as stuck/looping (see StuckTracker).
    private val stuckGeo: dynamic by lazy { Three.SphereGeometry(2.2, 8, 8) }
    private fun addStuckMarker(x: Double, y: Double, gz: Double = 0.0) {
        val marker = Three.Mesh(stuckGeo, Materials.solid("#ff2d2d"))
        place(marker.asDynamic(), x, y, gz + INDICATOR_Z + 3.5)
        indicatorsGroup.add(marker)
    }

    private fun addNpc(npc: NonFaction) {
        // Hide NPCs outside the play area (beyond the inner edge of the boundary wall). Passability isn't
        // computed out there (to save CPU), so NPCs drift/clip along the border — don't render those, nor
        // any inside/walking through the wall. Works for round (inscribed circle) and rectangular fields.
        if (!Sim.isInPlayArea(npc.pos.x, npc.pos.y)) return
        val sphere = Three.Mesh(headGeo, Materials.solid(NEUTRAL_COLOR))
        val gz = groundZ(npc.pos)
        if (Debug.enabled && StuckTracker.isStuck("npc:${npc.id}")) addStuckMarker(sceneX(npc.pos), sceneY(npc.pos), gz)
        // Marble drop-in: on first appearance the NPC falls from the sky (accelerating, 1−f²) to head
        // height. Per-NPC start height (by id) so a crowd reads as scattered marbles, not a flat sheet.
        val f = Spawns.appearRaw("npc:${npc.id}", NPC_DROP_S)
        val h = NPC_DROP_HEIGHT * (0.55 + 0.45 * ((npc.id * 37) % 100) / 100.0)
        val z = gz + HEAD_Z + h * (1.0 - f * f)
        place(sphere.asDynamic(), sceneX(npc.pos), sceneY(npc.pos), z)
        npcsGroup.add(sphere)
    }

    /** A stray-XM heap: a small additive glow mote, scaled a touch by how much XM it holds. */
    private fun addXm(pos: Pos, heap: XmHeap) {
        val mote = Three.Mesh(xmGeo, Materials.xmGlow())
        val s = 0.85 + (heap.xm / 300.0).coerceIn(0.0, 1.0) * 0.5 // bigger heaps glow a touch larger
        mote.asDynamic().scale.set(s, s, s)
        place(mote.asDynamic(), sceneX(pos), sceneY(pos), groundZ(pos) + XM_Z)
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
        val pipe = Three.Mesh(linkGeo, Materials.linkGlass(color)) // one translucent glass pipe (no plasma core)
        orientTube(pipe.asDynamic(), a, b)
        linksGroup.add(pipe)
        // Bright near-opaque ball-joints at each orb: round the pipe ends + hide their cut faces.
        listOf(a, b).forEach { end ->
            val joint = Three.Mesh(linkJointGeo, Materials.linkNode(color))
            joint.asDynamic().position.set(end[0], end[1], end[2])
            linksGroup.add(joint)
        }
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

    private fun orbPos(portal: Portal): DoubleArray = doubleArrayOf(
        sceneX(portal.location),
        sceneY(portal.location),
        groundZ(portal.location) + orbCenterZ(displayedOrbLevel(portal)),
    )

    /** Stable id for a field, independent of which corner is "origin" (its three portals, sorted). */
    private fun fieldId(field: Field): String =
        listOf(field.origin.id, field.primaryAnchor.id, field.secondaryAnchor.id).sorted().joinToString("|", "field:")

    private fun lineMaterial(color: String): dynamic = materialCache.getOrPut("l$color") {
        val p: dynamic = js("({})")
        p.color = color
        Three.LineBasicMaterial(p)
    }

    // Coin materials [side, top-cap, bottom-cap]: the top icon is fully opaque; the rim + underside are
    // see-through (COIN_BODY_OPACITY) so the coin reads as a translucent token with a solid face. The
    // XM gauge is a separate bar to the side (addEnergyBar) so the action icon stays clear. depthTest is
    // ON so portals occlude the coin (it sits in the depth-cleared sim pass, so buildings/terrain still
    // don't — it reads in front of the city).
    private fun indicatorMaterial(item: ActionItem, faction: Faction): dynamic = spriteCache.getOrPut("coin:" + item.text + faction.abbr) {
        val tex = Three.CanvasTexture(ActionItem.getHiResIcon(item, faction)) // hi-res → crisp
        val top = coinFace(tex, 1.0)
        val bottom = coinFace(tex, COIN_BODY_OPACITY)
        val rimParams: dynamic = js("({})")
        rimParams.color = faction.color
        rimParams.depthTest = true
        rimParams.transparent = true
        rimParams.opacity = COIN_BODY_OPACITY
        arrayOf(Three.MeshBasicMaterial(rimParams), top, bottom) // CylinderGeometry groups: 0 = side, 1 = top, 2 = bottom
    }

    private fun coinFace(tex: dynamic, opacity: Double): dynamic {
        val p: dynamic = js("({})")
        p.map = tex
        p.depthTest = true // portals occlude the coin; the depth-cleared pass keeps it over buildings
        p.transparent = true
        p.opacity = opacity
        return Three.MeshBasicMaterial(p)
    }

    // XM (energy) gauge: a vertical cylinder beside the action coin. Its full height scales with the
    // agent's LEVEL — higher-level agents have a bigger XM capacity (Ingress: +1000 XM per level), so a
    // taller bar reads as "more capacity". A faction-coloured fill rises from the bottom for current XM;
    // the drained top is black (empty = all black, full = all colour). depthTest matches the coin.
    private val energyBarGeo: dynamic by lazy { Three.CylinderGeometry(ENERGY_BAR_R, ENERGY_BAR_R, 1.0, 12) }

    private fun energyMat(color: String): dynamic = spriteCache.getOrPut("energy:$color") {
        val p: dynamic = js("({})")
        p.color = color
        p.depthTest = true
        p.transparent = true
        p.opacity = ENERGY_BAR_OPACITY
        Three.MeshBasicMaterial(p)
    }

    private fun addEnergyBar(x: Double, y: Double, gz: Double, agent: Agent) {
        val cap = agent.xmCapacity()
        val pct = (agent.xm.toDouble() / cap).coerceIn(0.0, 1.0)
        // Bar height scales with XM capacity: at the max capacity (L16+) it's 5× the coin; shorter below.
        val h = ENERGY_BAR_MAX_H * (cap.toDouble() / MAX_XM_CAPACITY)
        val bottom = gz + INDICATOR_Z + INDICATOR_THICK / 2.0 + ENERGY_BAR_GAP // stand it centered just above the coin
        val zc = bottom + h / 2.0
        // Black backing for the whole capacity…
        val back = Three.Mesh(energyBarGeo, energyMat("#0a0a0a"))
        back.asDynamic().rotation.x = PI / 2 // cylinder axis (Y) → world Z (stand it up)
        back.asDynamic().scale.set(1.0, h, 1.0)
        place(back.asDynamic(), x, y, zc)
        indicatorsGroup.add(back)
        // …faction-coloured fill rising from the bottom for the current XM. It's a hair fatter + longer
        // than the backing so its caps clear the backing's (no z-fighting at the top when full).
        val fillH = h * pct
        if (fillH > 0.01) {
            val fill = Three.Mesh(energyBarGeo, energyMat(agent.faction.color))
            fill.asDynamic().rotation.x = PI / 2
            fill.asDynamic().scale.set(ENERGY_BAR_FILL_FRAC, fillH + ENERGY_BAR_EPS * 2.0, ENERGY_BAR_FILL_FRAC)
            place(fill.asDynamic(), x, y, bottom + fillH / 2.0)
            indicatorsGroup.add(fill)
        }
    }

    // Show the spinning name ticker for the selected portal (or hide it if nothing/an agent is selected).
    // Driven by the [selected] setter (immediate on click) and re-run each [sync] (keeps it positioned as
    // the portal levels up / terrain resamples). The title never selects portals, so no names appear there.
    private fun refreshNameTicker() {
        val sel = selected
        val portal = if (sel != null && sel.startsWith("portal:")) {
            World.allPortals.firstOrNull { "portal:${it.id}" == sel }
        } else {
            null
        }
        if (portal == null) {
            PortalNameTicker.hide()
            return
        }
        val level = portal.getLevel().toInt().toDouble()
        val orbR = TOP_R * orbScale(level)
        val z = groundZ(portal.location) + orbCenterZ(level) + orbR + NAME_RING_GAP // top of the over-portal stack
        PortalNameTicker.show(
            "portal:${portal.id}",
            portal.name,
            sceneX(portal.location),
            sceneY(portal.location),
            z,
            orbR,
        )
    }
}
