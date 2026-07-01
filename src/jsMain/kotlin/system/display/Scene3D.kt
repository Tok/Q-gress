package system.display

import World
import agent.Agent
import agent.Faction
import agent.NonFaction
import agent.StuckTracker
import agent.action.ActionIcons
import agent.action.ActionItem
import config.Sim
import external.GLTFLoader
import external.MapLibre
import external.Three
import items.RewardMote
import items.deployable.Mod
import items.deployable.Shield
import items.level.LevelColor
import items.level.XmpLevel
import kotlinx.browser.window
import org.khronos.webgl.Float32Array
import org.khronos.webgl.set
import portal.Field
import portal.Link
import portal.Octant
import portal.Portal
import portal.XmMap
import system.audio.BlastSound
import system.audio.PortalChangeSound
import system.audio.Sound
import system.audio.SteamSound
import system.building.BuildingShake
import system.display.fx.BoltFx
import system.display.fx.CaptureFx
import system.display.fx.DamageNumberFx
import system.display.fx.DeployFx
import system.display.fx.FieldFx
import system.display.fx.HackFx
import system.display.fx.RewardFx
import system.display.fx.ShatterFx
import system.display.fx.ShieldWave
import system.display.fx.SmokeFx
import system.display.fx.XmFx
import system.display.fx.XmpBurst
import system.display.shader.GlassShader
import system.display.shader.PlasmaShader
import system.display.shader.ShieldShader
import util.Debug
import util.data.Pos
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
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
// LargeClass suppressed (intentional): Scene3D is the scene-graph hub — it owns the three.js groups and the
// per-tick sync that places every entity into them, which is irreducibly large and cohesive. The cleanly
// separable concerns HAVE been extracted (Showcases demo sandbox, PortalBuilder mesh construction, plus
// ShatterFx/HackFx/FieldFx/CaptureFx/Spawns/ShardAssets/…). What remains — entity sync + effect dispatch —
// is tightly bound to the groups; relocating it behind a ~30-member internal API purely to beat the
// line cap would make the code worse, not better, so the suppress stays.
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
    internal const val INDICATOR_Z = 5.0 // raised to clear the head now the indicator is ~3× bigger
    private const val OFFSCREEN_POLE_H = 400.0 // metres: height of a debug off-map-destination marker pole
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
    internal const val POLE_R = 2.0
    private const val LINK_R = 0.7 // link pipe radius (metres)
    internal const val PORTAL_GROW_S = 0.7 // seconds for a new portal to inflate in (pole rises, orb pops)
    private const val CAPTURE_SHATTER_WEIGHT = 0.22 // glass-shatter heaviness on capture (light — only the orb)
    private const val FIELD_FILL_S = 0.4 // seconds for a new control field to fill in
    private const val MAX_REWARD_CUBES = 12 // cap the hack-loot cubes so a big haul doesn't swarm the screen
    private const val REWARD_STAGGER_S = 0.07 // stagger between reward cubes leaving the orb
    private const val LEVEL_TWEEN_RATE = 0.18 // per-sync ease of the rendered level toward the real one
    internal const val POLE_H = 22.5 // base pole height at L1; scales by φ per level
    internal const val TOP_R = 7.0 // base orb radius
    internal const val PHI = 1.618 // golden ratio — used for the shield bubble radius

    // Resonators: 8 rubber slot-rings around the pole collar (just below the gasket), each holding a
    // colour-coded rod (the resonator) when filled.
    private const val RESO_RING_R = POLE_R * 0.42 // grommet ring radius
    private const val RESO_RING_TUBE = POLE_R * 0.13
    private const val GROMMET_COLOR = "#0a0a0a" // black rubber grommet (matches the gasket) when it falls
    private const val RESO_ROD_R = POLE_R * 0.26
    internal const val RESO_RADIUS_FRAC = 1.7 // slot distance from pole axis (× POLE_R) — spread so slots read distinct top-down
    internal const val RESO_COLLAR_FRAC = 0.78 // collar height as a fraction of the pole height
    internal const val RESO_ROD_LEN_FRAC = 0.22 // rod length as a fraction of the pole height
    internal const val NEUTRAL_COLOR = "#bbbbbb"
    internal const val MOD_R_FRAC = 0.16 // chrome mod radius (× orb radius)
    private const val SHIELD_WAVE_RANGE_FRAC = 0.6 // a blast ripples shields within this × the XMP's range
    private const val DAMAGE_NUMBER_GAP = 2.0 // start the damage number this far above the flask top
    private const val MAX_BUILDING_COLLIDERS = 1500 // cap on static building boxes added to the FX worlds
    internal const val HIGHLIGHT_COLOR = "#f0f0f0" // selection: off-tint grayscale (no new hues)
    internal const val OVERLAY_Z = 0.2 // passability quad just above ground
    private const val MARKER_R = 10.0 // build-preview marker radius (metres)
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

    // A faction agent's persistent meshes: head sphere (agentsGroup) + XM-bar backing + optional action coin +
    // optional XM-bar fill (all indicatorsGroup), reused/repositioned/retinted across ticks instead of
    // clear+recreate. Tracked [headColor] (selection retint) + [coinItem] (the action the coin renders, null = no
    // coin) drive in-place material swaps + add/remove.
    private class AgentMeshes(
        val sphere: dynamic,
        val energyBack: dynamic,
        var coin: dynamic,
        var fill: dynamic,
        var headColor: String,
        var coinItem: ActionItem?,
    )

    // Persistent agent meshes keyed by "agent:<name>". Cleared in onAdd with the groups.
    private val agentMeshes = mutableMapOf<String, AgentMeshes>()

    // Persistent NPC head spheres keyed by npc.id — reused + repositioned across ticks instead of clear+recreate
    // every tick (722+ NPCs on a big map = a huge per-tick allocation). Cleared in onAdd with the group.
    private val npcMeshes = mutableMapOf<Int, dynamic>()

    // Persistent stray-XM motes keyed by heap cell (Pos) — reused + repositioned/rescaled across ticks instead
    // of clear+recreate every tick (the per-tick Object3D/UUID churn the PLAN flags). Cleared in onAdd with the group.
    private val xmMotes = mutableMapOf<Pos, dynamic>()
    private var xmGroup: dynamic = null // stray collectible XM motes
    private var linksGroup: dynamic = null

    // Persistent link meshes keyed by the (unordered) portal pair, so [syncLinks] reuses them across ticks
    // instead of clear+recreate every tick (the per-tick Object3D/UUID churn the PLAN flags). Cleared in onAdd
    // when the group is rebuilt.
    private val linkMeshes = mutableMapOf<String, LinkMeshes>()
    private var fieldsGroup: dynamic = null
    private var markerGroup: dynamic = null // build-preview marker
    private var borderGroup: dynamic = null // playable-area boundary outline
    private var debugGroup: dynamic = null // ?debug / menu toggle: NPC off-map destination markers
    private var stuckGroup: dynamic = null // ?debug: stuck/loop markers over agents + NPCs (cleared each sync)

    /** Draw the play-area boundary (wall + outline + dim mask). Off for the title scene. */
    var showBorder = true

    // Set by TitleSim before the layer is added → onAdd loads the 3D wordmark + calls this once it's in
    // (e.g. to hide the DOM wordmark). Null in-game (no 3D wordmark there).
    var titleWordmarkOnReady: (() -> Unit)? = null

    // Glass-orb shatter fracture variants (loaded once from the GLB via ShardAssets).
    private var flaskVariants: List<List<dynamic>> = emptyList()
    private var flaskScale = 1.0 // scale a flask variant to ≈ the portal top sphere
    private var lastFrameMs = 0.0 // for per-frame effect dt
    private var animClockMs = 0.0 // sim-scaled animation clock (advances by dt × animationSpeed each frame)

    /** Simulation speed multiplier (set by the speed control); scales all visual animations. */
    var animationSpeed = 1.0

    /** The sim-scaled animation clock in ms — FX read this instead of wall-clock so they track sim speed. */
    fun animMs() = animClockMs

    /** Last-known shape of a control field (centroid + 3 centroid-relative vertices), for its dissolve. */
    private class FieldRecord(val cx: Double, val cy: Double, val cz: Double, val rel: Array<DoubleArray>, val color: String)

    private val fieldRecords = mutableMapOf<String, FieldRecord>()

    // A field's persistent plasma mesh + its current owner colour, so [syncFields] reuses the mesh across ticks
    // (updating its 3 vertices / per-vertex health / fill scale in place) instead of allocating a new
    // BufferGeometry + Mesh every tick. [color] is tracked so a recaptured field re-tints without a new mesh.
    private class FieldMesh(val mesh: dynamic, var color: String)

    // Persistent field meshes keyed by [fieldId]. Cleared in onAdd with the group.
    private val fieldMeshes = mutableMapOf<String, FieldMesh>()
    private val displayedLevel = mutableMapOf<String, Double>() // per-portal eased level (for level-up tween)

    // Shared geometries (created lazily once three.js is loaded).
    private val headGeo: dynamic by lazy { Three.SphereGeometry(HEAD_R, 10, 10) }
    private val xmGeo: dynamic by lazy { Three.SphereGeometry(XM_R, 8, 8) } // stray-XM mote
    internal val poleGeo: dynamic by lazy { Three.CylinderGeometry(POLE_R, POLE_R, POLE_H, 12) } // metal pole
    internal val gasketGeo: dynamic by lazy { Three.TorusGeometry(POLE_R * 1.15, POLE_R * 0.4, 10, 20) } // rubber donut

    // Unit pipe (scaled to length), 16 sides so the silhouette reads round; MSAA (the Graphics anti-aliasing
    // toggle, set on the MapLibre context) smooths its edges.
    internal val linkGeo: dynamic by lazy { Three.CylinderGeometry(LINK_R, LINK_R, 1.0, 16) }
    private val linkJointGeo: dynamic by lazy { Three.SphereGeometry(LINK_R * 1.5, 12, 12) } // ball-joint, a bit fatter than the pipe
    internal val resoRingGeo: dynamic by lazy { Three.TorusGeometry(RESO_RING_R, RESO_RING_TUBE, 8, 14) } // rubber slot grommet
    private val indicatorGeo: dynamic by lazy {
        // action coin: a short cylinder (icon on the round faces)
        Three.CylinderGeometry(INDICATOR_SIZE / 2.0, INDICATOR_SIZE / 2.0, INDICATOR_THICK, 28)
    }
    internal val resoRodGeo: dynamic by lazy { Three.CylinderGeometry(RESO_ROD_R, RESO_ROD_R, 1.0, 8) } // unit rod, scaled to length
    internal val resoCapGeo: dynamic by lazy { Three.CircleGeometry(RESO_ROD_R * 0.92, 16) } // energy "surface" disc on the rod top
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
        MovementPenaltyOverlay.register(newScene)
        VectorFieldOverlay.register(newScene)
        portalsGroup = Three.Group().also { newScene.add(it) }
        fieldsGroup = Three.Group().also { newScene.add(it) }
        fieldMeshes.clear() // the persistent field-mesh cache belongs to the OLD group — drop it with the rebuild
        linksGroup = Three.Group().also { newScene.add(it) }
        linkMeshes.clear() // the persistent link-mesh cache belongs to the OLD group — drop it with the rebuild
        npcsGroup = Three.Group().also { newScene.add(it) }
        npcMeshes.clear() // persistent NPC cache belongs to the OLD group — drop it with the rebuild
        xmGroup = Three.Group().also { newScene.add(it) }
        xmMotes.clear() // persistent XM-mote cache belongs to the OLD group — drop it with the rebuild
        agentsGroup = Three.Group().also { newScene.add(it) }
        indicatorsGroup = Three.Group().also { newScene.add(it) }
        agentMeshes.clear() // persistent agent cache belongs to the OLD groups — drop it with the rebuild
        markerGroup = Three.Group().also { newScene.add(it) }
        borderGroup = Three.Group().also { newScene.add(it) }
        debugGroup = Three.Group().also { newScene.add(it) }
        stuckGroup = Three.Group().also { newScene.add(it) }
        XmpBurst.register(newScene)
        FieldFx.register(newScene)
        ShatterFx.register(newScene)
        DamageNumberFx.register(newScene)
        OwnBuildings.register(newScene) // our own play-area building meshes (replace MapLibre's after gen)
        PortalNameTicker.register(newScene) // hovered-portal name ring (own group; spun each frame)
        BoltFx.register(newScene)
        XmFx.register(newScene)
        RewardFx.register(newScene)
        SmokeFx.register(newScene)
        Showcases.register(newScene)
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
        val dt = rawDt * animationSpeed
        SunController.advance(dt) // arc on the sim-scaled clock too → obeys speed + freezes on pause (animationSpeed 0)
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
        PortalBuilder.tumbleModTetras() // gentle continuous tumble of the mod tetrahedra
        updateTitleWordmark(invProj, dt) // camera-lock the 3D title letters (no-op until loaded)
        VectorFieldOverlay.sync() // paced flow-field sweep; driven here (continuous loop) so it animates through world-gen too
        activeRenderer.resetState()
        // Keep the depth the map layers wrote so 3D buildings occlude the sim (XMP/indicators that opt out
        // with depthTest=false stay on top regardless).
        activeRenderer.render(activeScene, cam)
        TitleWordmark.renderOverlay(activeRenderer, cam) // title letters: own pass, depth cleared → in front + self-occluding
        map.triggerRepaint()
    }

    private fun hasActiveEffects() = ShatterFx.hasActive() ||
        DamageNumberFx.hasActive() ||
        Showcases.animating() ||
        HackFx.hasActive() ||
        DeployFx.hasActive() ||
        XmFx.hasActive() ||
        RewardFx.hasActive() ||
        SmokeFx.hasActive() ||
        XmpBurst.hasActive() ||
        FieldFx.hasActive() ||
        BoltFx.hasActive()

    // Advance whatever transient FX are live (each self-guards). Split out of render() to keep it simple.
    private fun updateEffects(map: MapLibre.Map, dt: Double, invProj: dynamic) {
        if (!hasActiveEffects()) return
        if (ShatterFx.hasActive()) ShatterFx.update(dt)
        if (DamageNumberFx.hasActive()) DamageNumberFx.update(dt)
        if (BoltFx.hasActive()) BoltFx.update(dt)
        if (Showcases.animating()) Showcases.update(dt)
        if (HackFx.hasActive()) HackFx.update()
        if (DeployFx.hasActive()) DeployFx.update()
        if (XmFx.hasActive()) XmFx.update()
        if (RewardFx.hasActive()) RewardFx.update()
        if (SmokeFx.hasActive()) SmokeFx.update()
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
        PortalBuilder.resetSyncState() // mod tetras + shield mats are rebuilt by PortalBuilder.buildMods below
        clear(stuckGroup) // ?debug markers: cleared up front so syncNpcs + syncAgents can re-add this tick's
        clear(portalsGroup)
        World.allPortals.forEach { addPortal(it) }
        syncPoleColliders()
        syncFields() // persistent: reuse field plasma meshes across ticks (no clear+recreate)
        syncLinks() // persistent: reuse link meshes across ticks (no clear+recreate)
        syncNpcs() // persistent: reuse NPC spheres across ticks (no clear+recreate)
        syncXm() // persistent: reuse XM motes across ticks (no clear+recreate)
        syncAgents() // persistent: reuse agent head + coin + XM-bar meshes across ticks (no clear+recreate)
        refreshNameTicker() // keep the selected portal's name ring positioned (level-ups / terrain resample)
        buildOffscreenDebug() // ?debug / menu: NPC off-map destination markers
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
        // The boundary wall sits JUST OUTSIDE the play area now: its INNER edge is the actual radius (the old
        // outer edge), and it extends a thickness further out. So the white lines are the radius (inner) + the
        // wall's outer rim, and the dim mask starts beyond the wall.
        if (Sim.roundField) {
            val r = minOf(hx, hy) // true circle = the smaller half-extent (matches the grid mask)
            val thick = r * PlayAreaMask.WALL_THICK_FRAC
            PlayAreaMask.buildRoundMask(group, r + thick, r + thick, BORDER_Z - 0.05, OUTSIDE_DIM) // dim beyond the wall
            PlayAreaMask.buildRoundWall(group, r, r, gMin, wallH) // inner edge = r, extends outward
            val rad = Sim.fieldRadius()
            group.add(terrainRing(rad)) // inner edge = the actual play radius
            group.add(terrainRing(rad * (1.0 + PlayAreaMask.WALL_THICK_FRAC))) // outer rim of the wall, further out
            return
        }
        PlayAreaMask.build(group, hx + WALL_THICK, hy + WALL_THICK, OUTSIDE_FAR * maxOf(hx, hy), BORDER_Z - 0.05, OUTSIDE_DIM)
        PlayAreaMask.buildWalls(group, hx, hy, wallH, WALL_THICK, gMin)
        group.add(rectOutline(0.0)) // inner outline = the actual play boundary
        group.add(rectOutline(WALL_THICK)) // outer rim of the wall, a thickness further out
    }

    // The rectangular play-area outline, optionally pushed [offset] scene-metres outward from each edge (0 =
    // the play boundary; WALL_THICK = the wall's outer rim). Each corner sits on the terrain (groundZ).
    private fun rectOutline(offset: Double): dynamic {
        val corners = listOf(Pos(0, 0), Pos(Sim.width, 0), Pos(Sim.width, Sim.height), Pos(0, Sim.height), Pos(0, 0))
        val pts = corners.map { p ->
            val sx = sceneX(p)
            val sy = sceneY(p)
            Three.Vector3(sx + (if (sx >= 0) offset else -offset), sy + (if (sy >= 0) offset else -offset), groundZ(p) + BORDER_Z)
        }.toTypedArray()
        return Three.Line(Three.BufferGeometry().setFromPoints(pts), lineMaterial(BORDER_COLOR))
    }

    // A white outline circle of sim-radius [rad] around the play-area centre, each point dropped onto the
    // terrain (groundZ) so the line hugs the ground — used for both the outer + inner edges of the round wall.
    private fun terrainRing(rad: Double): dynamic {
        val pts = (0..ELLIPSE_SEGMENTS).map {
            val t = it.toDouble() / ELLIPSE_SEGMENTS * 2.0 * PI
            val sp = Pos((Sim.width / 2.0 + rad * cos(t)).toInt(), (Sim.height / 2.0 + rad * sin(t)).toInt())
            Three.Vector3(sceneX(sp), sceneY(sp), groundZ(sp) + BORDER_Z)
        }.toTypedArray()
        return Three.Line(Three.BufferGeometry().setFromPoints(pts), lineMaterial(BORDER_COLOR))
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
     * Lift the shared shard physics floor to the terrain UNDER this blast, so debris rests on the *local* ground
     * rather than the play-area-centre height that [buildBuildingColliders] sets once at world-gen. On varied
     * terrain a blast far from centre would otherwise sink below / float above the ground. Blasts are localized
     * and shards settle in ~2 s, so moving the shared plane per blast is fine; before the DEM loads, groundZ is 0
     * (the old sea-level default). Building-roof colliders are already per-building terrain-aware.
     */
    private fun liftShardFloor(location: Pos) {
        ShatterFx.setGroundZ(groundZ(location))
    }

    /**
     * Shatter a removed portal (from Portal.remove / the demo RMB): glass shards fly, the gasket
     * drops, the metal pole sinks, and each filled resonator rod ([resos] = octant→level) falls out
     * of its slot. The physics live in [ShatterFx]; we just hand it the geometry + positions.
     */
    fun shatterPortal(location: Pos, color: String, level: Int, resos: Map<Octant, Int> = emptyMap()) {
        liftShardFloor(location)
        val lv = level.toDouble()
        ShatterFx.shatter(
            sceneX(
                location,
            ),
            sceneY(
                location,
            ),
            PortalBuilder.poleHeight(lv), PortalBuilder.poleScale(lv), PortalBuilder.orbCenterZ(lv), PortalBuilder.orbScale(lv),
            color, flaskVariants, flaskScale, poleGeo, gasketGeo,
        )
        dropResonators(location, lv, resos)
    }

    /** Drop the resonator parts when the portal shatters: EVERY slot's two rubber o-rings (the lower pole
     *  socket + the upper ring) plus the colour-coded rod in each filled slot, each a brief rigid body. */
    private fun dropResonators(location: Pos, level: Double, resos: Map<Octant, Int>) {
        val poleH = PortalBuilder.poleHeight(level)
        val collarZ = groundZ(location) + poleH * RESO_COLLAR_FRAC
        val rodLen = poleH * RESO_ROD_LEN_FRAC
        val ringR = POLE_R * RESO_RADIUS_FRAC
        val x = sceneX(location)
        val y = sceneY(location)
        Octant.values().forEachIndexed { i, octant ->
            val ang = i * PI / 4.0
            val rx = x + ringR * cos(ang)
            val ry = y + ringR * sin(ang)
            // Both o-rings fall for every slot (filled or empty): the lower socket and the upper ring.
            ShatterFx.spawnFallingChunk(
                resoRingGeo,
                rx,
                ry,
                collarZ,
                1.0,
                RESO_RING_R + RESO_RING_TUBE,
                GROMMET_COLOR,
                ShatterFx.Bounce.RUBBER,
            )
            ShatterFx.spawnFallingChunk(
                resoRingGeo,
                rx,
                ry,
                collarZ + rodLen,
                1.0,
                RESO_RING_R + RESO_RING_TUBE,
                GROMMET_COLOR,
                ShatterFx.Bounce.RUBBER,
            )
            resos[octant]?.let { lvl ->
                ShatterFx.spawnFallingRod(resoRodGeo, rx, ry, collarZ + rodLen / 2.0, RESO_ROD_R, rodLen, LevelColor.map[lvl] ?: "#ffffff")
            }
        }
    }

    /** Drop a single resonator rod from its slot — used as each reso is destroyed during an attack. Only the
     *  ROD falls; the pole's o-ring cage stays (the slot just reverts to empty, still showing its two rings). */
    fun dropResonator(location: Pos, level: Int, octantIndex: Int, resoLevel: Int) {
        liftShardFloor(location)
        val poleH = PortalBuilder.poleHeight(level.toDouble())
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
    }

    /** Portal defense: a retaliation bolt arcs from the portal orb ([from]/[fromLevel]) to the attacker
     *  ([to], at head height), coloured by the owning faction. */
    fun fireBolt(from: Pos, fromLevel: Int, to: Pos, color: String) {
        scene ?: return
        val start = doubleArrayOf(
            sceneX(from),
            sceneY(from),
            groundZ(from) + PortalBuilder.orbCenterZ(fromLevel.coerceAtLeast(1).toDouble()),
        )
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

    /** Hack/glyph loot: one mote per [motes] entry (a cube, or a faction sphere for viruses) arcs from the orb to the agent. */
    fun rewardFx(portalLocation: Pos, level: Int, to: Pos, motes: List<RewardMote>) {
        scene ?: return
        val top = doubleArrayOf(
            sceneX(portalLocation),
            sceneY(portalLocation),
            groundZ(portalLocation) + PortalBuilder.orbCenterZ(level.coerceAtLeast(1).toDouble()),
        )
        val dst = doubleArrayOf(sceneX(to), sceneY(to), groundZ(to) + HEAD_Z)
        motes.take(MAX_REWARD_CUBES).forEachIndexed { i, m -> RewardFx.spawn(top, dst, m, i * REWARD_STAGGER_S) }
    }

    /** A burned-out portal vents a one-shot white-steam puff from its flask top (+ a subtle hiss). */
    fun steamPuff(portalLocation: Pos, level: Int) {
        scene ?: return
        val lvl = level.coerceAtLeast(1).toDouble()
        val flaskTop = doubleArrayOf(
            sceneX(portalLocation),
            sceneY(portalLocation),
            groundZ(portalLocation) + PortalBuilder.orbCenterZ(lvl) + TOP_R * PortalBuilder.orbScale(lvl),
        )
        SmokeFx.puff(flaskTop)
        SteamSound.play(portalLocation)
    }

    /** Drop the deployed mods out of the orb when a portal is neutralized / removed. */
    fun dropMods(location: Pos, level: Int, mods: List<Mod>) {
        if (mods.isEmpty()) return
        liftShardFloor(location)
        val lv = level.toDouble()
        val s = PortalBuilder.orbScale(lv)
        val half = TOP_R * MOD_R_FRAC * s
        val gz = groundZ(location)
        mods.forEach { mod ->
            ShatterFx.spawnFallingChunk(
                PortalBuilder.modGeoFor(mod.modType()),
                sceneX(location),
                sceneY(location),
                gz + PortalBuilder.orbCenterZ(lv),
                s,
                half,
                mod.rarity.color,
                ShatterFx.Bounce.LIGHT, // shields etc. — real bounciness unknown, so just a bit
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
            if (ultra) BlastSound.playUltraStrike(location) else BlastSound.playXmpSound(location, level)
        }
    }

    /**
     * Build static box colliders from the rendered building footprints (lng/lat polygons + render_height)
     * so falling debris/digits land on roofs instead of dropping through buildings. One axis-aligned box
     * per building (cheap); base at z=0 (the FX ground plane). Call once after world-gen ([feats] from
     * MapController's queryRenderedFeatures on the building layer).
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
        val gz = groundZ(portal.location)
        DamageNumberFx.setGroundZ(gz) // digits rest on the local terrain under this portal, not the centre height
        val flaskTop = gz + PortalBuilder.orbCenterZ(level) + TOP_R * PortalBuilder.orbScale(level)
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
        if (PortalBuilder.shieldMats.isEmpty()) return
        ShieldShader.setTime(seconds) // animate the hex lattice / pulse (was never driven before)
        ShieldShader.setEye(GlassShader.eye()) // camera-tracking Fresnel rim
        PortalBuilder.shieldMats.forEach { (mat, id) -> ShieldShader.setWave(mat, ShieldWave.amplitudeFor(id, seconds)) }
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

    internal fun markerMaterial(color: String): dynamic = materialCache.getOrPut("m$color") {
        val p: dynamic = js("({})")
        p.color = color
        p.transparent = true
        p.opacity = 0.8
        p.side = 2 // DoubleSide
        p.depthWrite = false
        Three.MeshBasicMaterial(p)
    }

    /** Lerp [from]→[to] (both "#rrggbb") component-wise in RGB at [t] (0..1), returning a "#rrggbb" string. */
    internal fun blendColor(from: String, to: String, t: Double): String {
        val a = from.removePrefix("#")
        val b = to.removePrefix("#")
        fun chan(i: Int): String {
            val av = a.substring(i, i + 2).toInt(16)
            val bv = b.substring(i, i + 2).toInt(16)
            val v = (av + (bv - av) * t).roundToInt().coerceIn(0, 255)
            return v.toString(16).padStart(2, '0')
        }
        return "#" + chan(0) + chan(2) + chan(4)
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

    /** Inverse of [lngLatToSimPos]: a sim Pos → ground [lng, lat] (web-mercator unproject). Public so the
     *  camera ([system.map.MapCamera]) can fly/follow an entity by its sim position. */
    fun simPosToLngLat(pos: Pos): DoubleArray {
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
        PassabilityOverlay.refresh() // re-drape the walkability overlay onto the freshly-sampled heights
        MovementPenaltyOverlay.refresh()
        window.setTimeout({ resampleTerrain() }, 1500)
        window.setTimeout({ resampleTerrain() }, 4000)
    }

    private fun resampleTerrain() {
        sampleHeights()
        rebuildBorder()
        PassabilityOverlay.refresh()
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
        Sound.updateListener(
            eye,
            doubleArrayOf(far[0] - eye[0], far[1] - eye[1], far[2] - eye[2]), // forward (normalised in Sound)
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

    internal fun place(obj: dynamic, x: Double, y: Double, z: Double) {
        obj.position.set(x, y, z)
    }

    internal fun tag(obj: dynamic, id: String) {
        val data: dynamic = js("({})")
        data.qid = id
        obj.userData = data
    }

    // Level is a Double so a level-up can ease between integer levels (see tweenedLevel).
    // Generous per-level growth so the level difference reads clearly: orb 0.45→1.6, pole 1.0→2.2×.

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
                gz + PortalBuilder.orbCenterZ(lv),
                PortalBuilder.orbScale(lv),
                old,
                flaskVariants,
                flaskScale,
            )
            BlastSound.playGlassShatterSound(portal.location, CAPTURE_SHATTER_WEIGHT)
        }
        val reform = CaptureFx.reformFactor(id)
        // A virus flip morphs the orb from the old faction colour to the new one (no shatter); everywhere
        // else this is just baseColor. Lerp in RGB so green↔blue passes through a believable midpoint.
        val orbColor = CaptureFx.recolorFrom(id)?.let { blendColor(it, baseColor, CaptureFx.recolorT(id)) } ?: baseColor
        // octant → (reso level, health 0..1) — both real-time, so the rod's energy bar tracks its charge.
        val resos = portal.resoMap().mapValues { Pair(it.value.getLevel(), it.value.calcHealthPercent() / 100.0) }
        // Selection keeps the faction hue but lights the orb brighter (no neutral-looking white tint);
        // buildPortal derives that from id == selected.
        val parts = PortalBuilder.buildPortal(portalsGroup, portal.location, level, orbColor, id, resos)
        PortalBuilder.buildMods(parts[0], portal) // chrome mods + shield bubble inside/around the orb (if shielded)
        HackFx.bind(id, parts[3]) // spin the collar if this portal is being hacked
        // Build-in: the pole rises and the orb grows from the ground; [reform] re-pops the orb only.
        val g = Spawns.appear(id, PORTAL_GROW_S)
        if (g < 1.0) {
            PortalBuilder.applyBuildGrow(level, g, parts, reform, gz)
        } else if (reform < 1.0) {
            PortalBuilder.applyBuildGrow(level, 1.0, parts, reform, gz) // orb pops back in after a capture
        }
    }

    // Refresh portal-pole colliders in the FX physics worlds so falling debris/digits hit the poles.
    private fun syncPoleColliders() {
        val specs = World.allPortals.map { p ->
            val gz = groundZ(p.location)
            val topZ = gz + PortalBuilder.poleHeight(p.getLevel().toInt().toDouble())
            doubleArrayOf(sceneX(p.location), sceneY(p.location), gz, topZ, POLE_R)
        }.toTypedArray()
        ShatterFx.setPoleColliders(specs)
        DamageNumberFx.setPoleColliders(specs)
    }

    /**
     * Reconcile the faction-agent meshes with [World.allAgents] WITHOUT clear+recreate every tick: reuse each
     * agent's head sphere (agentsGroup) + XM-bar backing + optional action coin + optional XM-bar fill
     * (indicatorsGroup), repositioning + retinting in place, creating only new agents and removing gone ones.
     * Keyed by "agent:<name>". Render-only (the sim position is untouched), so it never affects gameplay/pathing.
     */
    private fun syncAgents() {
        val ag = agentsGroup ?: return
        val ind = indicatorsGroup ?: return
        val present = mutableSetOf<String>()
        World.allAgents.forEach { agent ->
            val id = "agent:${agent.name}"
            present.add(id)
            val x = sceneX(agent.pos)
            val y = sceneY(agent.pos)
            val gz = groundZ(agent.pos) // walk on the terrain
            val color = if (selected == id) HIGHLIGHT_COLOR else agent.faction.color
            val rec = agentMeshes[id] ?: createAgentMeshes(ag, ind, id, color).also { agentMeshes[id] = it }
            syncAgentHead(rec, agent, color, x, y, gz)
            syncAgentCoin(ind, rec, agent, x, y, gz)
            syncEnergyBar(ind, rec, agent, x, y, gz)
            if (Debug.enabled && StuckTracker.isStuck(agent.key())) addStuckMarker(x, y, gz)
        }
        (agentMeshes.keys - present).forEach { id ->
            val rec = agentMeshes.remove(id) ?: return@forEach
            ag.remove(rec.sphere)
            ind.remove(rec.energyBack)
            val c = rec.coin
            if (c != null) ind.remove(c)
            val f = rec.fill
            if (f != null) ind.remove(f)
        }
    }

    // Retint the head on selection change (selection lights the orb brighter, keeps the faction hue) + reposition
    // it with its idle-fallback tell: RECRUIT bobs in place, EXPLORE runs a small circle; every other action sits
    // still (its coin says what it's doing) — so a coin-less bobbing agent at 0 m/s reads as plainly mid-recruit /
    // roaming, while a coin-bearing one stuck at 0 m/s is sus.
    private fun syncAgentHead(rec: AgentMeshes, agent: Agent, color: String, x: Double, y: Double, gz: Double) {
        if (rec.headColor != color) {
            rec.sphere.material = Materials.solid(color)
            rec.headColor = color
        }
        val phase = animMs() / 130.0
        val bob = if (agent.action.item == ActionItem.RECRUIT) abs(sin(phase)) * HEAD_R * 2.6 else 0.0
        val circleR = if (agent.action.item == ActionItem.EXPLORE) HEAD_R * 1.5 else 0.0
        place(rec.sphere, x + cos(phase) * circleR, y + sin(phase) * circleR, gz + HEAD_Z + bob)
    }

    private fun createAgentMeshes(ag: dynamic, ind: dynamic, id: String, color: String): AgentMeshes {
        val sphere = Three.Mesh(headGeo, Materials.solid(color))
        tag(sphere.asDynamic(), id)
        ag.add(sphere)
        val back = Three.Mesh(energyBarGeo, energyMat("#0a0a0a")) // black backing for the whole XM capacity
        back.asDynamic().rotation.x = PI / 2 // cylinder axis (Y) → world Z (stand it up)
        ind.add(back)
        return AgentMeshes(sphere.asDynamic(), back.asDynamic(), null, null, color, null)
    }

    // The action indicator: a 3D coin/wheel (icon on the round faces) hovering above the head — EXCEPT for the idle
    // FALLBACK actions (recruiting + exploring, see ActionItem.isFallback) which show NO coin (just the head-bob).
    // Added / removed / reskinned as the action changes (WAIT isn't a fallback — it shows an empty coin).
    private fun syncAgentCoin(ind: dynamic, rec: AgentMeshes, agent: Agent, x: Double, y: Double, gz: Double) {
        val want = if (agent.action.item.isFallback) null else agent.action.item
        if (rec.coinItem != want) {
            val old = rec.coin
            if (old != null) {
                ind.remove(old)
                rec.coin = null
            }
            if (want != null) {
                val coin = Three.Mesh(indicatorGeo, indicatorMaterial(want, agent.faction))
                coin.asDynamic().rotation.x = PI / 2 // stand the cylinder's faces up (axis → world Z)
                ind.add(coin)
                rec.coin = coin.asDynamic()
            }
            rec.coinItem = want
        }
        val c = rec.coin
        if (c != null) place(c, x, y, gz + INDICATOR_Z)
    }

    // ?debug: a vivid marker floating over an entity flagged as stuck/looping (see StuckTracker).
    private val stuckGeo: dynamic by lazy { Three.SphereGeometry(2.2, 8, 8) }
    private fun addStuckMarker(x: Double, y: Double, gz: Double = 0.0) {
        val g = stuckGroup ?: return
        val marker = Three.Mesh(stuckGeo, Materials.solid("#ff2d2d"))
        place(marker.asDynamic(), x, y, gz + INDICATOR_Z + 3.5)
        g.add(marker)
    }

    // Debug viz: a tall cyan pole (+ orb on top) at each NPC off-map destination
    // ([NonFaction.offscreenDestinations]), so their spread around the border is visible at a glance. Toggled
    // by `?debug` or the "NPC destinations" menu checkbox; rebuilt each sync (the ring rarely changes, cheap).
    var showOffscreenDebug = false
    private val offscreenPoleGeo: dynamic by lazy { Three.CylinderGeometry(2.0, 2.0, OFFSCREEN_POLE_H, 8) }
    private val offscreenOrbGeo: dynamic by lazy { Three.SphereGeometry(8.0, 12, 12) }
    private fun buildOffscreenDebug() {
        val g = debugGroup ?: return
        g.clear()
        if (!(Debug.enabled || showOffscreenDebug)) return
        NonFaction.offscreenDestinations().forEach { p ->
            val base = groundZ(p)
            val pole = Three.Mesh(offscreenPoleGeo, Materials.solid("#00e5ff"))
            pole.asDynamic().rotation.x = PI / 2.0 // stand the Y-axis cylinder up on world Z
            place(pole.asDynamic(), sceneX(p), sceneY(p), base + OFFSCREEN_POLE_H / 2.0) // centred → base at ground
            g.add(pole)
            val orb = Three.Mesh(offscreenOrbGeo, Materials.solid("#00e5ff"))
            place(orb.asDynamic(), sceneX(p), sceneY(p), base + OFFSCREEN_POLE_H)
            g.add(orb)
        }
    }

    /**
     * Reconcile the NPC head spheres with [World.allNonFaction] WITHOUT clear+recreate every tick: reuse each
     * NPC's sphere (just reposition it), create only new ones, and remove the gone (recruited NPCs) + any that
     * have left the play area. NPCs outside the play area aren't rendered (passability isn't computed out there,
     * so they drift/clip along the border). The marble drop-in (fall from the sky, accelerating 1−f²) only runs
     * while the world is first populating; once it's running NPCs appear in place.
     */
    private fun syncNpcs() {
        val group = npcsGroup ?: return
        val present = mutableSetOf<Int>()
        World.allNonFaction.forEach { npc ->
            if (!Sim.isInPlayArea(npc.pos.x, npc.pos.y)) return@forEach // culled outside the play area (removed below if it was shown)
            present.add(npc.id)
            val gz = groundZ(npc.pos)
            if (Debug.enabled && StuckTracker.isStuck("npc:${npc.id}")) addStuckMarker(sceneX(npc.pos), sceneY(npc.pos), gz)
            val f = if (World.isReady) 1.0 else Spawns.appearRaw("npc:${npc.id}", NPC_DROP_S)
            val h = NPC_DROP_HEIGHT * (0.55 + 0.45 * ((npc.id * 37) % 100) / 100.0)
            val z = gz + HEAD_Z + h * (1.0 - f * f)
            val sphere = npcMeshes.getOrPut(npc.id) { Three.Mesh(headGeo, Materials.solid(NEUTRAL_COLOR)).also { group.add(it) } }
            place(sphere, sceneX(npc.pos), sceneY(npc.pos), z)
        }
        (npcMeshes.keys - present).forEach { id ->
            val m = npcMeshes.remove(id) ?: return@forEach
            group.remove(m)
        }
    }

    /**
     * Reconcile the stray-XM motes with [XmMap] WITHOUT clear+recreate every tick: reuse each heap's mote (just
     * reposition + rescale to its current XM), create only new heaps, and remove the collected/vanished ones.
     * Keyed by the heap's cell [Pos], which is stable across ticks even though World rebuilds the heap objects.
     * A heap is a small additive glow mote, scaled a touch by how much XM it holds.
     */
    private fun syncXm() {
        val group = xmGroup ?: return
        val present = mutableSetOf<Pos>()
        XmMap.all().forEach { (pos, heap) ->
            present.add(pos)
            val mote = xmMotes.getOrPut(pos) { Three.Mesh(xmGeo, Materials.xmGlow()).also { group.add(it) } }
            val s = 0.85 + (heap.xm / 300.0).coerceIn(0.0, 1.0) * 0.5 // bigger heaps glow a touch larger
            mote.asDynamic().scale.set(s, s, s)
            place(mote.asDynamic(), sceneX(pos), sceneY(pos), groundZ(pos) + XM_Z)
        }
        (xmMotes.keys - present).forEach { pos ->
            val m = xmMotes.remove(pos) ?: return@forEach
            group.remove(m)
        }
    }

    // A link's persistent meshes: the glass pipe + the two near-opaque ball-joints at each orb. [color] is the
    // creator faction's colour, tracked so [syncLinks] can re-tint if a pair flips faction (recapture + relink).
    private class LinkMeshes(val pipe: dynamic, val jointA: dynamic, val jointB: dynamic, var color: String)

    // A stable, order-independent key for a link's portal pair (Link.equals is symmetric), so the same physical
    // link reuses its meshes across ticks even though World rebuilds the Link objects.
    private fun linkKey(link: Link): String {
        val ids = listOf(link.origin.id, link.destination.id).sorted()
        return ids[0] + " " + ids[1]
    }

    /**
     * Reconcile the link meshes with [World.allLinks] WITHOUT clearing + recreating them every tick: reuse the
     * existing meshes (just re-orient them — both ends ride the tweened orb height), create only genuinely new
     * links, and remove only vanished ones. A link is a thin glass pipe ([Materials.linkGlass]) between the two
     * portals' orbs, capped by bright ball-joints that round the pipe ends + hide their cut faces.
     */
    private fun syncLinks() {
        val group = linksGroup ?: return
        val present = mutableSetOf<String>()
        World.allLinks().forEach { link ->
            val key = linkKey(link)
            present.add(key)
            val color = link.creator.faction.color
            val a = orbPos(link.origin)
            val b = orbPos(link.destination)
            val existing = linkMeshes[key]
            if (existing == null) {
                linkMeshes[key] = createLinkMeshes(group, a, b, color)
            } else {
                if (existing.color != color) recolorLink(existing, color)
                orientTube(existing.pipe, a, b)
                existing.jointA.position.set(a[0], a[1], a[2])
                existing.jointB.position.set(b[0], b[1], b[2])
            }
        }
        (linkMeshes.keys - present).forEach { key ->
            val lm = linkMeshes.remove(key) ?: return@forEach
            group.remove(lm.pipe)
            group.remove(lm.jointA)
            group.remove(lm.jointB)
        }
    }

    private fun createLinkMeshes(group: dynamic, a: DoubleArray, b: DoubleArray, color: String): LinkMeshes {
        val pipe = Three.Mesh(linkGeo, Materials.linkGlass(color))
        orientTube(pipe.asDynamic(), a, b)
        group.add(pipe)
        val jointA = Three.Mesh(linkJointGeo, Materials.linkNode(color)).asDynamic()
        jointA.position.set(a[0], a[1], a[2])
        group.add(jointA)
        val jointB = Three.Mesh(linkJointGeo, Materials.linkNode(color)).asDynamic()
        jointB.position.set(b[0], b[1], b[2])
        group.add(jointB)
        return LinkMeshes(pipe.asDynamic(), jointA, jointB, color)
    }

    private fun recolorLink(lm: LinkMeshes, color: String) {
        lm.pipe.material = Materials.linkGlass(color)
        lm.jointA.material = Materials.linkNode(color)
        lm.jointB.material = Materials.linkNode(color)
        lm.color = color
    }

    /** Place a unit (Y-axis) cylinder so it spans [a]→[b]: midpoint, Y-scaled to length, Y rotated to dir. */
    internal fun orientTube(mesh: dynamic, a: DoubleArray, b: DoubleArray) {
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

    /**
     * Reconcile the control-field plasma sheets with [World.allFields] WITHOUT clear+recreate every tick: reuse
     * each field's mesh (rewrite its 3 vertices + per-vertex health + fill scale in place), create only new
     * fields, and remove vanished ones ([teardownGone] plays the dissolve). Keyed by [fieldId] (its three
     * portals, sorted). A field is an animated plasma sheet across the portals' orbs; it fills in on creation.
     */
    private fun syncFields() {
        val group = fieldsGroup ?: return
        val present = mutableSetOf<String>()
        World.allFields().forEach { field ->
            val fid = fieldId(field)
            present.add(fid)
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
            fieldRecords[fid] = FieldRecord(cx, cy, cz, rel, color) // remembered for the teardown dissolve
            val existing = fieldMeshes[fid]
            val mesh = if (existing == null) {
                val created = createFieldMesh(group, rel, color, field)
                fieldMeshes[fid] = FieldMesh(created, color)
                created
            } else {
                if (existing.color != color) { // recapture: re-tint the plasma without a new mesh
                    existing.mesh.material = PlasmaShader.material(color)
                    existing.color = color
                }
                updateFieldGeometry(existing.mesh, rel, field)
                existing.mesh
            }
            mesh.asDynamic().position.set(cx, cy, cz)
            val g = Spawns.appear(fid, FIELD_FILL_S).coerceIn(0.0, 1.0) // fill-in on creation; back to full scale after
            mesh.asDynamic().scale.set(g, g, g)
        }
        (fieldMeshes.keys - present).forEach { fid ->
            val fm = fieldMeshes.remove(fid) ?: return@forEach
            group.remove(fm.mesh)
        }
    }

    private fun createFieldMesh(group: dynamic, rel: Array<DoubleArray>, color: String, field: Field): dynamic {
        val geo = Three.BufferGeometry().setFromPoints(rel.map { Three.Vector3(it[0], it[1], it[2]) }.toTypedArray())
        // Per-vertex health (0..1): the plasma fades toward a low-health portal corner (PlasmaShader.aHealth).
        val health = Float32Array(3)
        health[0] = (field.origin.calcHealth() / 100.0).toFloat()
        health[1] = (field.primaryAnchor.calcHealth() / 100.0).toFloat()
        health[2] = (field.secondaryAnchor.calcHealth() / 100.0).toFloat()
        geo.asDynamic().setAttribute("aHealth", Three.Float32BufferAttribute(health, 1))
        val mesh = Three.Mesh(geo, PlasmaShader.material(color))
        mesh.asDynamic().frustumCulled = false // vertices move with the orbs; skip stale-bounds culling on the reused mesh
        group.add(mesh)
        return mesh
    }

    // Rewrite the reused field mesh's 3 vertices (orbs may have tweened) + per-vertex health (portals took damage).
    private fun updateFieldGeometry(mesh: dynamic, rel: Array<DoubleArray>, field: Field) {
        val pos = mesh.geometry.attributes.position
        for (i in 0..2) pos.setXYZ(i, rel[i][0], rel[i][1], rel[i][2])
        pos.needsUpdate = true
        val health = mesh.geometry.attributes.aHealth
        health.setX(0, (field.origin.calcHealth() / 100.0))
        health.setX(1, (field.primaryAnchor.calcHealth() / 100.0))
        health.setX(2, (field.secondaryAnchor.calcHealth() / 100.0))
        health.needsUpdate = true
    }

    /** Spawn the dissolve effect for any fields that vanished this sync (and play the collapse sound). */
    private fun teardownGone(gone: Set<String>) {
        gone.forEach { id ->
            val rec = if (id.startsWith("field:")) fieldRecords.remove(id) else null
            if (rec != null) {
                FieldFx.dissolve(rec.cx, rec.cy, rec.cz, rec.rel, rec.color)
                Sound.playFieldDownSound()
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
        groundZ(portal.location) + PortalBuilder.orbCenterZ(displayedOrbLevel(portal)),
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
        val tex = Three.CanvasTexture(ActionIcons.getHiResIcon(item, faction)) // hi-res → crisp
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

    // Reposition + rescale the reused XM bar: the black backing to the agent's capacity (bar height scales with
    // XM capacity — at the max, L16+, it's 5× the coin; shorter below), and the faction-coloured fill to its
    // current XM (created/removed as the bar fills/empties).
    private fun syncEnergyBar(ind: dynamic, rec: AgentMeshes, agent: Agent, x: Double, y: Double, gz: Double) {
        val cap = agent.xmCapacity()
        val pct = (agent.xm.toDouble() / cap).coerceIn(0.0, 1.0)
        val h = ENERGY_BAR_MAX_H * (cap.toDouble() / MAX_XM_CAPACITY)
        val bottom = gz + INDICATOR_Z + INDICATOR_THICK / 2.0 + ENERGY_BAR_GAP // centered just above the coin
        rec.energyBack.scale.set(1.0, h, 1.0)
        place(rec.energyBack, x, y, bottom + h / 2.0)
        val fillH = h * pct
        if (fillH > 0.01) {
            var fill = rec.fill
            if (fill == null) {
                val f = Three.Mesh(energyBarGeo, energyMat(agent.faction.color))
                f.asDynamic().rotation.x = PI / 2
                ind.add(f)
                fill = f.asDynamic()
                rec.fill = fill
            }
            // A hair fatter + longer than the backing so its caps clear the backing's (no z-fighting when full).
            fill.scale.set(ENERGY_BAR_FILL_FRAC, fillH + ENERGY_BAR_EPS * 2.0, ENERGY_BAR_FILL_FRAC)
            place(fill, x, y, bottom + fillH / 2.0)
        } else {
            val f = rec.fill
            if (f != null) {
                ind.remove(f)
                rec.fill = null
            }
        }
    }

    // Spin a name ring above EVERY portal (re-run each [sync] so rings track level-ups / terrain resamples and
    // new/removed portals are reconciled). Gated by PortalNameTicker.enabled — the menu "Portal names" toggle,
    // off on the title. The ticker only rebuilds a ring's letters when its name changes, so this is cheap.
    private fun refreshNameTicker() {
        if (!PortalNameTicker.enabled) {
            PortalNameTicker.sync(emptyList())
            return
        }
        val views = World.allPortals.map { portal ->
            val level = portal.getLevel().toInt().toDouble()
            val orbR = TOP_R * PortalBuilder.orbScale(level)
            val z = groundZ(portal.location) + PortalBuilder.orbCenterZ(level) + orbR + NAME_RING_GAP // top of the over-portal stack
            PortalNameTicker.NameView(
                "portal:${portal.id}",
                portal.name,
                sceneX(portal.location),
                sceneY(portal.location),
                z,
                orbR,
            )
        }
        PortalNameTicker.sync(views)
    }
}
