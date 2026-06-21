package system.display

import World
import agent.Agent
import agent.Faction
import agent.NonFaction
import agent.action.ActionItem
import config.Sim
import external.Cannon
import external.GLTFLoader
import external.MapLibre
import external.Three
import kotlinx.browser.document
import portal.Field
import portal.Link
import portal.Portal
import util.SoundUtil
import util.Util
import util.data.Pos
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
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
@Suppress("TooManyFunctions")
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
    private const val INDICATOR_Z = 2.7
    private const val INDICATOR_SIZE = 1.6
    private const val POLE_R = 2.0
    private const val LINK_R = 0.7 // glass-pipe link radius (metres)
    private const val CORE_R_FRAC = 0.3 // bright inner-filament radius as a fraction of LINK_R
    private const val PORTAL_GROW_S = 0.5 // seconds for a new portal's orb to grow in
    private const val FIELD_FILL_S = 0.4 // seconds for a new control field to fill in
    private const val LEVEL_TWEEN_RATE = 0.18 // per-sync ease of the rendered level toward the real one
    private const val POLE_H = 22.5 // base pole height at L1; scales by φ per level
    private const val TOP_R = 7.0 // base orb radius
    private const val PHI = 1.618 // golden ratio — pole grows by φ across the 8 levels
    private const val NEUTRAL_COLOR = "#bbbbbb"
    private const val HIGHLIGHT_COLOR = "#f0f0f0" // selection: off-tint grayscale (no new hues)
    private const val OVERLAY_Z = 0.2 // passability quad just above ground
    private const val VECTOR_STRIDE = 2 // subsample the flow field every Nth cell
    private const val VECTOR_CONE_R = 1.1 // flow-arrow cone radius (metres)
    private const val VECTOR_CONE_H = 3.6 // flow-arrow cone length (metres)
    private const val MARKER_R = 10.0 // build-preview marker radius (metres)
    private const val BORDER_COLOR = "#22ddff" // playable-area boundary
    private const val BORDER_Z = 0.3
    private const val SHARD_OPACITY = 0.5 // translucent glass shards
    private const val SHARD_FADE = 1.2 // seconds to fade out at end of life
    private const val SHARD_LIFE_MIN = 3.0
    private const val SHARD_LIFE_MAX = 5.0
    private const val SHARD_SPIN = 6.0 // max tumble rad/s
    private const val SHARD_MASS = 1.0
    private const val GRAVITY = 9.8 // m/s²
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
    private var linksGroup: dynamic = null
    private var fieldsGroup: dynamic = null
    private var overlayGroup: dynamic = null // passability quad (static; toggled)
    private var vectorFieldGroup: dynamic = null // selected portal's flow field (toggled)
    private var markerGroup: dynamic = null // build-preview marker
    private var borderGroup: dynamic = null // playable-area boundary outline
    private var passabilityVisible = false
    private var vectorFieldVisible = false
    private var vectorFieldKey: String? = null // selection the flow field was last built for

    // Glass-orb shatter fracture variants (loaded once from the GLB via ShardAssets).
    private var flaskVariants: List<List<dynamic>> = emptyList()
    private var flaskScale = 1.0 // scale a flask variant to ≈ the portal top sphere
    private var shardsGroup: dynamic = null // transient shatter fragments (not cleared by sync)
    private var showcaseGroup: dynamic = null // demo-scene portal preview (not cleared by sync)
    private var showcaseMesh: dynamic = null
    private var showcaseOrb: dynamic = null // the preview's glass orb (removed when shattered)
    private var showcaseGasket: dynamic = null // the preview's static gasket (drops when shattered)
    private var showcaseLevel = 1
    private var physicsWorld: Cannon.World? = null // cannon-es world for the shards
    private val tempBodies = mutableListOf<Cannon.Body>() // static pole colliders, live only during a shatter
    private var shatterRot = doubleArrayOf(0.0, 0.0, 0.0) // per-shatter random orientation of the shards
    private val activeShards = mutableListOf<Shard>()
    private var lastFrameMs = 0.0 // for per-frame shard physics dt

    /** One in-flight glass fragment: a mesh driven by its cannon-es rigid [body], fading over [life]. */
    private class Shard(val mesh: dynamic, val mat: dynamic, val body: Cannon.Body, var age: Double, val life: Double)

    /** Last-known shape of a control field (centroid + 3 centroid-relative vertices), for its dissolve. */
    private class FieldRecord(val cx: Double, val cy: Double, val cz: Double, val rel: Array<DoubleArray>, val color: String)

    private val fieldRecords = mutableMapOf<String, FieldRecord>()
    private val displayedLevel = mutableMapOf<String, Double>() // per-portal eased level (for level-up tween)

    // Shared geometries (created lazily once three.js is loaded).
    private val headGeo: dynamic by lazy { Three.SphereGeometry(HEAD_R, 10, 10) }
    private val coneGeo: dynamic by lazy { Three.ConeGeometry(VECTOR_CONE_R, VECTOR_CONE_H, 6) }
    private val poleGeo: dynamic by lazy { Three.CylinderGeometry(POLE_R, POLE_R, POLE_H, 12) } // metal pole
    private val topGeo: dynamic by lazy { Three.SphereGeometry(TOP_R, 20, 16) } // glass orb (scaled per level)
    private val gasketGeo: dynamic by lazy { Three.TorusGeometry(POLE_R * 1.15, POLE_R * 0.4, 10, 20) } // rubber donut
    private val linkGeo: dynamic by lazy { Three.CylinderGeometry(LINK_R, LINK_R, 1.0, 8) } // unit glass tube (scaled to length)
    private val coreGeo: dynamic by lazy { Three.CylinderGeometry(LINK_R * CORE_R_FRAC, LINK_R * CORE_R_FRAC, 1.0, 6) } // bright filament inside the tube
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

        overlayGroup = Three.Group().also { newScene.add(it) }
        vectorFieldGroup = Three.Group().also { newScene.add(it) }
        portalsGroup = Three.Group().also { newScene.add(it) }
        fieldsGroup = Three.Group().also { newScene.add(it) }
        linksGroup = Three.Group().also { newScene.add(it) }
        npcsGroup = Three.Group().also { newScene.add(it) }
        agentsGroup = Three.Group().also { newScene.add(it) }
        indicatorsGroup = Three.Group().also { newScene.add(it) }
        markerGroup = Three.Group().also { newScene.add(it) }
        borderGroup = Three.Group().also { newScene.add(it) }
        shardsGroup = Three.Group().also { newScene.add(it) }
        XmpBurst.register(newScene)
        FieldFx.register(newScene)
        showcaseGroup = Three.Group()
        newScene.add(showcaseGroup)
        physicsWorld = createPhysicsWorld()
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
        feedCameraEye(cam.projectionMatrix) // camera-tracking glass rim (orbs + links)
        PlasmaShader.setTime((js("performance.now()") as Double) / 1000.0) // animate control fields
        if (activeShards.isNotEmpty() || XmpBurst.hasActive() || FieldFx.hasActive()) {
            val nowMs = js("performance.now()") as Double
            val dt = if (lastFrameMs <= 0.0) 0.016 else ((nowMs - lastFrameMs) / 1000.0).coerceIn(0.0, 0.1)
            lastFrameMs = nowMs
            if (activeShards.isNotEmpty()) updateShards(dt)
            if (FieldFx.hasActive()) FieldFx.update(dt)
            if (XmpBurst.hasActive()) {
                val invProj = Three.Matrix4().copy(cam.projectionMatrix).invert()
                val canvas = map.getCanvas()
                XmpBurst.setView(invProj, canvas.width as Double, canvas.height as Double)
                XmpBurst.update(dt)
            }
        } else {
            lastFrameMs = 0.0
        }
        activeRenderer.resetState()
        activeRenderer.render(activeScene, cam)
        map.triggerRepaint()
    }

    /**
     * Recover the camera eye in **sim space** from the combined sim→clip matrix and hand it to
     * [GlassShader]. MapLibre 5.24 exposes no free-camera API, and the whole perspective transform
     * is baked into [cam.projectionMatrix], so we solve for the camera centre directly: it is the
     * null vector of the 3×4 projection formed by the x, y and w rows of the 4×4 matrix
     * (the classic `P·C = 0` camera-centre, with the depth row dropped).
     */
    private fun feedCameraEye(matrix: dynamic) {
        val e = matrix.elements // three.js Matrix4: column-major, e[col*4 + row]

        // 3×3 determinant over columns (c0,c1,c2) using rows x(0), y(1), w(3).
        fun det3(c0: Int, c1: Int, c2: Int): Double {
            fun m(r: Int, c: Int): Double = e[c * 4 + r] as Double
            return m(0, c0) * (m(1, c1) * m(3, c2) - m(1, c2) * m(3, c1)) -
                m(0, c1) * (m(1, c0) * m(3, c2) - m(1, c2) * m(3, c0)) +
                m(0, c2) * (m(1, c0) * m(3, c1) - m(1, c1) * m(3, c0))
        }
        val cw = -det3(0, 1, 2)
        if (abs(cw) < 1e-9) return // degenerate (e.g. pre-first-frame) — keep the last eye
        GlassShader.setEye(det3(1, 2, 3) / cw, -det3(0, 2, 3) / cw, det3(0, 1, 3) / cw)
    }

    private fun updateShards(dt: Double) {
        physicsWorld?.step(1.0 / 60.0, dt, 3)
        val iter = activeShards.iterator()
        while (iter.hasNext()) {
            val s = iter.next()
            s.age += dt
            val bodyPos = s.body.asDynamic().position
            s.mesh.position.set(bodyPos.x as Double, bodyPos.y as Double, bodyPos.z as Double)
            val bodyQuat = s.body.asDynamic().quaternion
            s.mesh.quaternion.set(bodyQuat.x as Double, bodyQuat.y as Double, bodyQuat.z as Double, bodyQuat.w as Double)
            if (s.age > s.life - SHARD_FADE) {
                s.mat.opacity = SHARD_OPACITY * ((s.life - s.age) / SHARD_FADE).coerceIn(0.0, 1.0)
            }
            if (s.age >= s.life) {
                physicsWorld?.removeBody(s.body)
                shardsGroup.remove(s.mesh)
                s.mat.dispose()
                iter.remove()
            }
        }
        if (activeShards.isEmpty() && tempBodies.isNotEmpty()) { // shatter done → retire the pole colliders
            tempBodies.forEach { physicsWorld?.removeBody(it) }
            tempBodies.clear()
        }
    }

    /** Rebuild the 3D objects from world state. Called once per simulation tick. */
    fun sync() {
        scene ?: return
        Spawns.beginSync()
        clear(portalsGroup)
        World.allPortals.forEach { addPortal(it) }
        clear(fieldsGroup)
        World.allFields().forEach { addField(it) }
        clear(linksGroup)
        World.allLinks().forEach { addLink(it) }
        clear(npcsGroup)
        World.allNonFaction.forEach { addNpc(it) }
        clear(agentsGroup)
        clear(indicatorsGroup)
        World.allAgents.forEach { addAgent(it) }
        teardownGone(Spawns.endSync())
        // The selected portal's flow field is static, so only rebuild when the selection
        // changes (or visibility toggles) — not every tick.
        when {
            vectorFieldVisible && selected != vectorFieldKey -> {
                clear(vectorFieldGroup)
                buildVectorFieldArrows()
                vectorFieldKey = selected
            }
            !vectorFieldVisible && vectorFieldKey != null -> {
                clear(vectorFieldGroup)
                vectorFieldKey = null
            }
        }
    }

    /** Toggle the selected portal's flow-field arrows (rebuilt by [sync] when selection changes). */
    fun setVectorFieldVisible(visible: Boolean) {
        vectorFieldVisible = visible
    }

    private fun buildVectorFieldArrows() {
        val id = selected ?: return
        if (!id.startsWith("portal:")) return
        val portal = World.allPortals.find { "portal:${it.id}" == id } ?: return
        portal.vectors.forEach { (pos, vec) ->
            val gx = pos.x.toInt()
            val gy = pos.y.toInt()
            val mag = vec.magnitude
            if (gx % VECTOR_STRIDE != 0 || gy % VECTOR_STRIDE != 0 || mag == 0.0) return@forEach
            val pixel = pos.fromShadow()
            val angle = atan2(-vec.im / mag, vec.re / mag) // sim y is down → scene y is up
            val cone = Three.Mesh(coneGeo, hueMaterial(angle))
            cone.asDynamic().position.set(sceneX(pixel), sceneY(pixel), OVERLAY_Z)
            cone.asDynamic().rotation.z = angle - PI / 2 // cone apex (+Y) → flow direction
            vectorFieldGroup.add(cone)
        }
    }

    // Colour by flow direction (hue), bucketed to 15° so the material cache stays small.
    private fun hueMaterial(angle: Double): dynamic {
        val deg = ((angle * 180.0 / PI) + 360.0) % 360.0
        val bucket = (deg / 15.0).toInt()
        return materialCache.getOrPut("v$bucket") {
            val p: dynamic = js("({})")
            p.color = "hsl(${bucket * 15}, 90%, 55%)"
            Three.MeshBasicMaterial(p)
        }
    }

    // Outline the playable area (sim bounds) so the player can see where the world ends.
    private fun buildBorder() {
        val group = borderGroup ?: return
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
     * Shatter a removed portal's glass orb (called from Portal.remove). Shards reuse the flask
     * model, scaled to this [level]'s orb and spawned at its height. Each shatter picks a random
     * variant AND a random orientation for variability; a static pole collider keeps shards from
     * flying through the (metal) pole, and the rubber gasket drops too. The pole itself is metal,
     * so it does not shatter.
     */
    fun shatterPortal(location: Pos, color: String, level: Int) {
        val world = physicsWorld ?: return
        val x = sceneX(location)
        val y = sceneY(location)
        val lv = level.toDouble()
        val s = orbScale(lv)
        val poleH = poleHeight(lv)
        shatterRot = doubleArrayOf(Util.random() * 2.0 * PI, Util.random() * 2.0 * PI, Util.random() * 2.0 * PI)
        if (flaskVariants.isNotEmpty()) {
            val variant = flaskVariants[(Util.random() * flaskVariants.size).toInt()]
            variant.forEach { holder -> spawnShard(holder, doubleArrayOf(x, y, orbCenterZ(lv)), flaskScale * s, color, 2.0) }
        }
        addPoleObstacle(world, x, y, poleH)
        spawnGasket(world, x, y, poleH)
    }

    /** A static box collider standing in for the metal pole, so shards bounce off it (not through). */
    private fun addPoleObstacle(world: Cannon.World, x: Double, y: Double, poleH: Double) {
        val opts: dynamic = js("({ mass: 0 })")
        opts.position = Cannon.Vec3(x, y, poleH / 2)
        opts.shape = Cannon.Box(Cannon.Vec3(POLE_R, POLE_R, poleH / 2))
        val body = Cannon.Body(opts)
        world.addBody(body)
        tempBodies.add(body)
    }

    /** The rubber gasket as a falling rigid body when the portal is destroyed. */
    private fun spawnGasket(world: Cannon.World, x: Double, y: Double, poleH: Double) {
        val p: dynamic = js("({})")
        p.color = "#0a0a0a"
        p.metalness = 0.0
        p.roughness = 0.95
        p.transparent = true
        p.opacity = 1.0
        val mat = Three.MeshStandardMaterial(p)
        val mesh = Three.Mesh(gasketGeo, mat)
        val opts: dynamic = js("({})")
        opts.mass = SHARD_MASS
        opts.position = Cannon.Vec3(x, y, poleH)
        opts.shape = Cannon.Box(Cannon.Vec3(POLE_R * 1.5, POLE_R * 1.5, POLE_R * 0.5))
        opts.linearDamping = 0.05
        opts.angularDamping = 0.3
        val body = Cannon.Body(opts)
        body.asDynamic().velocity.set((Util.random() - 0.5) * 3.0, (Util.random() - 0.5) * 3.0, -1.0)
        body.asDynamic().angularVelocity.set(randSpin() * 0.4, randSpin() * 0.4, randSpin() * 0.4)
        world.addBody(body)
        shardsGroup.add(mesh)
        activeShards.add(Shard(mesh, mat, body, 0.0, SHARD_LIFE_MIN + Util.random() * (SHARD_LIFE_MAX - SHARD_LIFE_MIN)))
    }

    private fun spawnShard(holder: dynamic, pos: DoubleArray, scale: Double, color: String, burstH: Double) {
        val world = physicsWorld ?: return
        val mat = shardMaterial(color)
        val mesh = Three.Mesh(holder.geo, mat)
        mesh.asDynamic().scale.set(scale, scale, scale)
        val opts: dynamic = js("({})")
        opts.mass = SHARD_MASS
        opts.position = Cannon.Vec3(pos[0], pos[1], pos[2])
        opts.shape = Cannon.Box(
            Cannon.Vec3(
                ((holder.hx as Double) * scale).coerceAtLeast(0.1),
                ((holder.hy as Double) * scale).coerceAtLeast(0.1),
                ((holder.hz as Double) * scale).coerceAtLeast(0.1),
            ),
        )
        opts.linearDamping = 0.04
        opts.angularDamping = 0.2
        val body = Cannon.Body(opts)
        body.asDynamic().quaternion.setFromEuler(shatterRot[0], shatterRot[1], shatterRot[2]) // random per-shatter
        val a = Util.random() * 2.0 * PI
        val r = burstH * (0.02 + Util.random() * 0.06) // almost no outward push
        val up = burstH * Util.random() * 0.08 // the faintest pop; gravity takes over — they just drop
        body.asDynamic().velocity.set(cos(a) * r, sin(a) * r, up)
        body.asDynamic().angularVelocity.set(randSpin(), randSpin(), randSpin())
        world.addBody(body)
        shardsGroup.add(mesh)
        activeShards.add(Shard(mesh, mat, body, 0.0, SHARD_LIFE_MIN + Util.random() * (SHARD_LIFE_MAX - SHARD_LIFE_MIN)))
    }

    private fun createPhysicsWorld(): Cannon.World {
        val world = Cannon.World()
        world.asDynamic().gravity.set(0.0, 0.0, -GRAVITY)
        val groundOpts: dynamic = js("({ mass: 0 })") // static ground plane at z=0 (normal +Z)
        groundOpts.shape = Cannon.Plane()
        world.addBody(Cannon.Body(groundOpts))
        return world
    }

    private fun randSpin() = (Util.random() - 0.5) * 2.0 * SHARD_SPIN

    /** Fire an XMP detonation at a location, scaled by burster [level] (1..8). See [XmpBurst]. */
    fun playXmpBurst(location: Pos, level: Int) {
        scene ?: return
        XmpBurst.play(sceneX(location), sceneY(location), level)
        SoundUtil.playXmpSound(location, level)
    }

    private fun shardMaterial(color: String): dynamic {
        val p: dynamic = js("({})")
        p.color = color
        p.transparent = true
        p.opacity = SHARD_OPACITY
        p.metalness = 0.0
        p.roughness = 0.05
        p.emissive = color
        p.emissiveIntensity = 0.25 // glassy edge glow
        p.side = 2 // DoubleSide
        return Three.MeshStandardMaterial(p)
    }

    /** Place (or clear, when pos is null) the build-preview marker on the ground. */
    fun setBuildMarker(pos: Pos?, state: String) {
        val group = markerGroup ?: return
        group.clear()
        if (pos == null) return
        val color = when (state) {
            "build" -> "#ffffff"
            "portal" -> "#ff9900"
            else -> "#ff3333"
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
    fun setPassabilityVisible(visible: Boolean) {
        passabilityVisible = visible
        val group = overlayGroup ?: return
        group.clear()
        if (visible && World.isReady) group.add(buildPassabilityMesh())
    }

    private fun buildPassabilityMesh(): dynamic {
        val cols = Sim.width / Pos.res
        val rows = Sim.height / Pos.res
        val canvas = document.createElement("canvas").asDynamic()
        canvas.width = cols
        canvas.height = rows
        val ctx = canvas.getContext("2d")
        World.grid.forEach { (pos, cell) ->
            val gx = pos.x.toInt()
            val gy = pos.y.toInt()
            if (gx in 0 until cols && gy in 0 until rows) {
                ctx.fillStyle = cell.overlayColor()
                ctx.fillRect(gx, gy, 1, 1)
            }
        }
        val texture = Three.CanvasTexture(canvas)
        texture.asDynamic().magFilter = Three.NearestFilter
        texture.asDynamic().minFilter = Three.NearestFilter
        val matParams: dynamic = js("({})")
        matParams.map = texture
        matParams.transparent = true
        matParams.depthWrite = false
        val mesh = Three.Mesh(
            Three.PlaneGeometry(Sim.width * metersPerPixel, Sim.height * metersPerPixel),
            Three.MeshBasicMaterial(matParams),
        )
        mesh.asDynamic().position.set(0.0, 0.0, OVERLAY_Z)
        return mesh
    }

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

    private fun sceneX(pos: Pos) = (pos.x - Sim.width / 2.0) * metersPerPixel
    private fun sceneY(pos: Pos) = -(pos.y - Sim.height / 2.0) * metersPerPixel

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
        val baseColor = portal.owner?.faction?.color ?: NEUTRAL_COLOR
        val color = if (selected == id) HIGHLIGHT_COLOR else baseColor
        val level = tweenedLevel(id, portal.getLevel().toInt()) // eases on level-up
        val parts = buildPortal(portalsGroup, sceneX(portal.location), sceneY(portal.location), level, color, id)
        // Grow the glass orb in over its first moments (the pole/gasket appear immediately).
        val g = Spawns.appear(id, PORTAL_GROW_S)
        if (g < 1.0) {
            val s = orbScale(level) * g.coerceAtLeast(0.0)
            parts[0].scale.set(s, s, s)
        }
    }

    /**
     * A portal: a metallic pole (taller with [level]), a black rubber gasket so the metal doesn't
     * touch the glass, and a round glass orb on top (bigger with [level]). [id] tags it for picking
     * (null = demo). Returns [orb, gasket] so the demo can drop them when the portal shatters.
     */
    private fun buildPortal(parent: dynamic, x: Double, y: Double, level: Double, color: String, id: String?): Array<dynamic> {
        val poleH = poleHeight(level)
        val s = orbScale(level)
        val pole = Three.Mesh(poleGeo, Materials.metal())
        pole.asDynamic().rotation.x = PI / 2 // Y-axis cylinder → vertical (Z up)
        pole.asDynamic().scale.set(1.0, poleScale(level), 1.0) // grow height (local Y) only
        place(pole.asDynamic(), x, y, poleH / 2)
        val gasket = Three.Mesh(gasketGeo, Materials.rubber()) // torus in XY → flat ring around the pole top
        place(gasket.asDynamic(), x, y, poleH)
        val orb = Three.Mesh(topGeo, Materials.glass(color))
        place(orb.asDynamic(), x, y, orbCenterZ(level))
        orb.asDynamic().scale.set(s, s, s)
        id?.let {
            tag(pole.asDynamic(), it)
            tag(gasket.asDynamic(), it)
            tag(orb.asDynamic(), it)
        }
        parent.add(pole)
        parent.add(gasket)
        parent.add(orb)
        return arrayOf(orb, gasket)
    }

    /** Demo only (#demo/portal): show a single portal at [level] in a sync-immune group. */
    fun showcasePortal(location: Pos, level: Int, color: String) {
        val grp = showcaseGroup ?: return
        if (showcaseMesh != null) grp.remove(showcaseMesh)
        val group = Three.Group()
        val parts = buildPortal(group, sceneX(location), sceneY(location), level.toDouble(), color, null)
        grp.add(group)
        showcaseMesh = group
        showcaseOrb = parts[0]
        showcaseGasket = parts[1]
        showcaseLevel = level
    }

    /** Demo only: shatter the preview portal — its orb vanishes, the gasket drops, the pole stays. */
    fun shatterShowcase(location: Pos, color: String) {
        val g = showcaseMesh
        if (g != null) {
            if (showcaseOrb != null) g.remove(showcaseOrb)
            if (showcaseGasket != null) g.remove(showcaseGasket)
        }
        showcaseOrb = null
        showcaseGasket = null
        shatterPortal(location, color, showcaseLevel)
    }

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
    }

    private fun addNpc(npc: NonFaction) {
        val sphere = Three.Mesh(headGeo, Materials.solid(NEUTRAL_COLOR))
        place(sphere.asDynamic(), sceneX(npc.pos), sceneY(npc.pos), HEAD_Z)
        npcsGroup.add(sphere)
    }

    /**
     * A link is a thin glass pipe between the two portals' orbs (à la qlippostasis tubing): a
     * brighter glass shell ([Materials.linkGlass]) around an additive **plasma core** filament, so
     * the link still reads strongly even though the orb glass is near-transparent at pipe radius.
     */
    private fun addLink(link: Link) {
        val color = link.creator.faction.color
        val z0 = orbCenterZ(link.origin.getLevel().toInt().toDouble())
        val z1 = orbCenterZ(link.destination.getLevel().toInt().toDouble())
        val a = doubleArrayOf(sceneX(link.origin.location), sceneY(link.origin.location), z0)
        val b = doubleArrayOf(sceneX(link.destination.location), sceneY(link.destination.location), z1)
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
        }
    }

    private fun orbPos(portal: Portal): DoubleArray = doubleArrayOf(sceneX(portal.location), sceneY(portal.location), orbCenterZ(portal.getLevel().toInt().toDouble()))

    /** Stable id for a field, independent of which corner is "origin" (its three portals, sorted). */
    private fun fieldId(field: Field): String = listOf(field.origin.id, field.primaryAnchor.id, field.secondaryAnchor.id).sorted().joinToString("|", "field:")

    private fun lineMaterial(color: String): dynamic = materialCache.getOrPut("l$color") {
        val p: dynamic = js("({})")
        p.color = color
        Three.LineBasicMaterial(p)
    }

    private fun indicatorMaterial(item: ActionItem, faction: Faction): dynamic = spriteCache.getOrPut(item.text + faction.abbr) {
        val texture = Three.CanvasTexture(ActionItem.getIcon(item, faction))
        val p: dynamic = js("({})")
        p.map = texture
        p.depthTest = false
        p.transparent = true
        Three.SpriteMaterial(p)
    }
}
