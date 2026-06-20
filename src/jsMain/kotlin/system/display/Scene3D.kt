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
import items.level.XmpLevel
import kotlinx.browser.document
import portal.Field
import portal.Link
import portal.Portal
import util.SoundUtil
import util.Util
import util.data.Pos
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

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
    private const val POLE_H = 22.5
    private const val TOP_R = 7.0
    private const val NEUTRAL_COLOR = "#bbbbbb"
    private const val HIGHLIGHT_COLOR = "#f0f0f0" // selection: off-tint grayscale (no new hues)
    private const val OVERLAY_Z = 0.2 // passability quad just above ground
    private const val VECTOR_STRIDE = 2 // subsample the flow field every Nth cell
    private const val VECTOR_CONE_R = 1.1 // flow-arrow cone radius (metres)
    private const val VECTOR_CONE_H = 3.6 // flow-arrow cone length (metres)
    private const val MARKER_R = 10.0 // build-preview marker radius (metres)
    private const val BORDER_COLOR = "#22ddff" // playable-area boundary
    private const val BORDER_Z = 0.3
    private const val SHARD_OPACITY = 0.6
    private const val SHARD_FADE = 1.2 // seconds to fade out at end of life
    private const val SHARD_LIFE_MIN = 3.0
    private const val SHARD_LIFE_MAX = 5.0
    private const val SHARD_SPIN = 6.0 // max tumble rad/s
    private const val SHARD_MASS = 1.0
    private const val GRAVITY = 9.8 // m/s²
    private const val SHARD_TARGET_PATH = 3.0 // pole-shard target size (metres)
    private const val POLE_SHARD_COUNT = 12
    private const val XMP_RANGE_SCALE = 0.5 // XmpLevel.rangeM → scene-metre blast radius
    private const val XMP_LIFE_BASE = 0.9 // seconds; total detonation lifetime at level 0
    private const val XMP_LIFE_PER_LEVEL = 0.06 // bigger bursters linger a little longer
    private const val RING_Z = 0.28 // ground shockwave quad height (above passability overlay)

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

    // Glass-shatter fracture assets (loaded once from GLBs). Each "shard holder" is a JS object
    // { geo, cx, cy, cz } — a shared geometry + its local centroid (for the burst direction).
    private var flaskVariants: List<List<dynamic>> = emptyList() // sephira/sphere shell-shard variants
    private var pathShards: List<dynamic> = emptyList() // pole/rod shard library
    private var flaskScale = 1.0 // scale a flask variant to ≈ the portal top sphere
    private var pathScale = 1.0 // scale a pole shard to ≈ SHARD_TARGET_PATH
    private var shardsGroup: dynamic = null // transient shatter fragments (not cleared by sync)
    private var burstsGroup: dynamic = null // transient XMP shockwaves
    private var physicsWorld: Cannon.World? = null // cannon-es world for the shards
    private val activeShards = mutableListOf<Shard>()
    private val activeBursts = mutableListOf<Burst>()
    private var lastFrameMs = 0.0 // for per-frame shard physics dt

    /** One in-flight glass fragment: a mesh driven by its cannon-es rigid [body], fading over [life]. */
    private class Shard(val mesh: dynamic, val mat: dynamic, val body: Cannon.Body, var age: Double, val life: Double)

    /**
     * A synthwave micro-nuke detonation. [meshes] = (rolling fireball cap torus, hot core sphere,
     * ground shockwave ring); all three ShaderMaterials share [uni] (uTime/uProgress/uSeed).
     * [geom] = (sceneX, sceneY, blast-radius m, life s). Animated by [age] in updateBursts.
     */
    private class Burst(val meshes: Array<dynamic>, val uni: dynamic, val geom: DoubleArray, var age: Double)

    // Shared geometries/materials (created lazily once three.js is loaded).
    private val headGeo: dynamic by lazy { Three.SphereGeometry(HEAD_R, 10, 10) }
    private val poleGeo: dynamic by lazy { Three.CylinderGeometry(POLE_R, POLE_R, POLE_H, 8) }
    private val topGeo: dynamic by lazy { Three.SphereGeometry(TOP_R, 16, 16) }
    private val coneGeo: dynamic by lazy { Three.ConeGeometry(VECTOR_CONE_R, VECTOR_CONE_H, 6) }
    private val burstGeo: dynamic by lazy { Three.SphereGeometry(1.0, 24, 16) } // unit sphere → XMP hot core
    private val torusGeo: dynamic by lazy { Three.TorusGeometry(1.0, 0.42, 16, 48) } // unit donut → rolling cap
    private val ringQuadGeo: dynamic by lazy { Three.PlaneGeometry(2.0, 2.0) } // flat quad → ground shockwave

    // Unit cylinder (taller toward the cap) baked Y-up→Z-up once → the rising mushroom stem.
    private val stemGeo: dynamic by lazy {
        val g = Three.CylinderGeometry(1.0, 0.5, 1.0, 12).asDynamic()
        g.rotateX(PI / 2) // bake Y-up cylinder to Z-up so the stem rises along +Z
        g
    }
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
        layer.id = "qgress-3d"
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
        burstsGroup = Three.Group().also { newScene.add(it) }
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
        if (activeShards.isNotEmpty() || activeBursts.isNotEmpty()) {
            val nowMs = js("performance.now()") as Double
            val dt = if (lastFrameMs <= 0.0) 0.016 else ((nowMs - lastFrameMs) / 1000.0).coerceIn(0.0, 0.1)
            lastFrameMs = nowMs
            if (activeShards.isNotEmpty()) updateShards(dt)
            if (activeBursts.isNotEmpty()) updateBursts(dt)
        } else {
            lastFrameMs = 0.0
        }
        activeRenderer.resetState()
        activeRenderer.render(activeScene, cam)
        map.triggerRepaint()
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
    }

    /** Rebuild the 3D objects from world state. Called once per simulation tick. */
    fun sync() {
        scene ?: return
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

    // Load the glass-shard fracture GLBs once (async). shattered_flask = sphere shell-shard
    // variants (pieces named "<key>_chunkN"); glass_shards = a small rod-shard library.
    private fun loadShatterAssets() {
        val loader = GLTFLoader()
        loader.load(
            "models/shattered_flask.glb",
            { gltf ->
                flaskVariants = parseVariants(gltf)
                flaskScale = computeScale(flaskVariants.firstOrNull() ?: emptyList(), TOP_R * 2.0)
            },
            {},
            { e -> console.error("shattered_flask load failed: $e") },
        )
        loader.load(
            "models/glass_shards.glb",
            { gltf ->
                pathShards = collectShards(gltf)
                pathScale = computeScale(pathShards.take(1), SHARD_TARGET_PATH)
            },
            {},
            { e -> console.error("glass_shards load failed: $e") },
        )
    }

    // Union bbox of the pieces → uniform scale so they span `target` metres.
    private fun computeScale(pieces: List<dynamic>, target: Double): Double {
        if (pieces.isEmpty()) return 1.0
        var loX = Double.MAX_VALUE
        var loY = Double.MAX_VALUE
        var loZ = Double.MAX_VALUE
        var hiX = -Double.MAX_VALUE
        var hiY = -Double.MAX_VALUE
        var hiZ = -Double.MAX_VALUE
        pieces.forEach { h ->
            val bb = h.geo.boundingBox
            loX = minOf(loX, bb.min.x as Double)
            hiX = maxOf(hiX, bb.max.x as Double)
            loY = minOf(loY, bb.min.y as Double)
            hiY = maxOf(hiY, bb.max.y as Double)
            loZ = minOf(loZ, bb.min.z as Double)
            hiZ = maxOf(hiZ, bb.max.z as Double)
        }
        val d = maxOf(hiX - loX, hiY - loY, hiZ - loZ)
        return if (d > 0.0) target / d else 1.0
    }

    /** Shatter a removed portal into glass fragments at its location (called from Portal.remove). */
    fun shatterPortal(location: Pos, color: String) {
        scene ?: return
        val x = sceneX(location)
        val y = sceneY(location)
        if (flaskVariants.isNotEmpty()) { // top sphere → shell shards
            val variant = flaskVariants[(Util.random() * flaskVariants.size).toInt()]
            variant.forEach { holder -> spawnShard(holder, doubleArrayOf(x, y, POLE_H), flaskScale, color, 6.0) }
        }
        if (pathShards.isNotEmpty()) { // pole → rod shards along its length
            for (i in 0 until POLE_SHARD_COUNT) {
                val holder = pathShards[(Util.random() * pathShards.size).toInt()]
                spawnShard(holder, doubleArrayOf(x, y, POLE_H * (i + 0.5) / POLE_SHARD_COUNT), pathScale, color, 5.0)
            }
        }
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
        val a = Util.random() * 2.0 * PI
        val r = burstH * (0.1 + Util.random() * 0.25) // gentle outward drift; gravity does the rest
        val up = burstH * (0.05 + Util.random() * 0.3) // a small pop up, then it mostly falls
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

    /**
     * Fire a synthwave micro-nuke at a location, scaled by burster [level] (1..8): a rising
     * mushroom — turbulent stem column → rolling fireball cap (torus) with a hot flashing core —
     * plus an expanding neon ground shockwave ring. All four meshes share one uniforms object
     * (uTime/uProgress/uSeed) animated by updateBursts. Meshes order: stem, cap, core, ring.
     */
    fun playXmpBurst(location: Pos, level: Int) {
        scene ?: return
        val rangeM = XmpLevel.values().find { it.level == level }?.rangeM ?: XmpLevel.ONE.rangeM
        val maxR = rangeM * XMP_RANGE_SCALE
        val cx = sceneX(location)
        val cy = sceneY(location)
        val uni: dynamic = js("({ uTime: { value: 0.0 }, uProgress: { value: 0.0 }, uSeed: { value: 0.0 } })")
        uni.uSeed.value = Util.random() * 10.0
        val stem = Three.Mesh(stemGeo, XmpShaders.material(XmpShaders.SURFACE_VERT, XmpShaders.STEM_FRAG, uni, additive = false))
        val cap = Three.Mesh(torusGeo, XmpShaders.material(XmpShaders.SURFACE_VERT, XmpShaders.CAP_FRAG, uni, additive = false))
        val core = Three.Mesh(burstGeo, XmpShaders.material(XmpShaders.SURFACE_VERT, XmpShaders.CORE_FRAG, uni))
        val ring = Three.Mesh(ringQuadGeo, XmpShaders.material(XmpShaders.UV_VERT, XmpShaders.RING_FRAG, uni))
        ring.asDynamic().position.set(cx, cy, RING_Z)
        // Render order (depthTest is off): ground ring, then smoke, then the glowing core on top.
        ring.asDynamic().renderOrder = 1
        stem.asDynamic().renderOrder = 2
        cap.asDynamic().renderOrder = 3
        core.asDynamic().renderOrder = 4
        burstsGroup.add(stem)
        burstsGroup.add(cap)
        burstsGroup.add(core)
        burstsGroup.add(ring)
        val life = XMP_LIFE_BASE + level * XMP_LIFE_PER_LEVEL
        activeBursts.add(Burst(arrayOf(stem, cap, core, ring), uni, doubleArrayOf(cx, cy, maxR, life), 0.0))
        SoundUtil.playXmpSound(location, level)
    }

    private fun updateBursts(dt: Double) {
        val iter = activeBursts.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.age += dt
            val cx = b.geom[0]
            val cy = b.geom[1]
            val maxR = b.geom[2]
            val life = b.geom[3]
            val f = (b.age / life).coerceIn(0.0, 1.0)
            b.uni.uProgress.value = f
            b.uni.uTime.value = b.age
            val ease = 1.0 - (1.0 - f) * (1.0 - f) // easeOutQuad
            val rise = maxR * (0.12 + 0.6 * f * f) // mushroom climb (cap clears the stem)
            val capR = maxR * (0.1 + 0.2 * ease) // a head, not a giant donut
            val capH = rise + capR * 0.5
            val flat = 1.0 - 0.4 * smoothstep01(0.4, 1.0, f) // cap flattens as it mushrooms
            // Stem: a tapered column from the ground up to the cap.
            val stem = b.meshes[0]
            val stemR = maxR * (0.06 + 0.04 * ease)
            val stemH = capH.coerceAtLeast(0.01)
            stem.scale.set(stemR, stemR, stemH)
            stem.position.set(cx, cy, stemH * 0.5)
            val cap = b.meshes[1]
            cap.scale.set(capR, capR, capR * flat)
            cap.position.set(cx, cy, capH)
            val core = b.meshes[2]
            val coreR = maxR * (0.1 + 0.16 * ease)
            core.scale.set(coreR, coreR, coreR)
            core.position.set(cx, cy, capH)
            b.meshes[3].scale.set(maxR, maxR, 1.0)
            if (b.age >= life) {
                for (m in b.meshes) {
                    burstsGroup.remove(m)
                    m.material.dispose()
                }
                iter.remove()
            }
        }
    }

    private fun smoothstep01(edge0: Double, edge1: Double, x: Double): Double {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0.0, 1.0)
        return t * t * (3.0 - 2.0 * t)
    }

    private fun shardMaterial(color: String): dynamic {
        val p: dynamic = js("({})")
        p.color = color
        p.transparent = true
        p.opacity = SHARD_OPACITY
        p.metalness = 0.0
        p.roughness = 0.1
        p.emissive = color
        p.emissiveIntensity = 0.2
        p.side = 2 // DoubleSide
        return Three.MeshStandardMaterial(p)
    }

    private fun collectShards(gltf: dynamic): List<dynamic> {
        val out = mutableListOf<dynamic>()
        gltf.scene.traverse({ obj: dynamic -> if (obj.geometry != null) out.add(makeShard(obj)) })
        return out
    }

    private fun parseVariants(gltf: dynamic): List<List<dynamic>> {
        val groups = mutableMapOf<String, MutableList<dynamic>>()
        gltf.scene.traverse({ obj: dynamic ->
            if (obj.geometry != null) {
                val name = obj.name as String
                val cut = name.indexOf("_chunk")
                val key = if (cut > 0) name.substring(0, cut) else name
                groups.getOrPut(key) { mutableListOf() }.add(makeShard(obj))
            }
        })
        return groups.values.map { it.toList() }
    }

    private fun makeShard(mesh: dynamic): dynamic {
        val geo = mesh.geometry
        geo.rotateX(PI / 2) // bake model Y-up → scene Z-up so the rigid-body quaternion drives the mesh
        geo.computeBoundingBox()
        val bb = geo.boundingBox
        val holder: dynamic = js("({})")
        holder.geo = geo
        holder.hx = ((bb.max.x as Double) - (bb.min.x as Double)) / 2.0 // half-extents for the box collider
        holder.hy = ((bb.max.y as Double) - (bb.min.y as Double)) / 2.0
        holder.hz = ((bb.max.z as Double) - (bb.min.z as Double)) / 2.0
        return holder
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

    private fun addPortal(portal: Portal) {
        val x = sceneX(portal.location)
        val y = sceneY(portal.location)
        val id = "portal:${portal.id}"
        val baseColor = portal.owner?.faction?.color ?: NEUTRAL_COLOR
        val color = if (selected == id) HIGHLIGHT_COLOR else baseColor
        val pole = Three.Mesh(poleGeo, glassMaterial(color))
        pole.asDynamic().rotation.x = PI / 2 // Y-axis cylinder → vertical (Z up)
        place(pole.asDynamic(), x, y, POLE_H / 2)
        tag(pole.asDynamic(), id)
        portalsGroup.add(pole)
        val top = Three.Mesh(topGeo, glassMaterial(color))
        place(top.asDynamic(), x, y, POLE_H)
        tag(top.asDynamic(), id)
        portalsGroup.add(top)
    }

    private fun addAgent(agent: Agent) {
        val x = sceneX(agent.pos)
        val y = sceneY(agent.pos)
        val id = "agent:${agent.name}"
        val color = if (selected == id) HIGHLIGHT_COLOR else agent.faction.color
        val sphere = Three.Mesh(headGeo, solidMaterial(color))
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
        val sphere = Three.Mesh(headGeo, solidMaterial(NEUTRAL_COLOR))
        place(sphere.asDynamic(), sceneX(npc.pos), sceneY(npc.pos), HEAD_Z)
        npcsGroup.add(sphere)
    }

    private fun addLink(link: Link) {
        val points = arrayOf(
            Three.Vector3(sceneX(link.origin.location), sceneY(link.origin.location), POLE_H),
            Three.Vector3(sceneX(link.destination.location), sceneY(link.destination.location), POLE_H),
        )
        val geo = Three.BufferGeometry().setFromPoints(points)
        linksGroup.add(Three.Line(geo, lineMaterial(link.creator.faction.color)))
    }

    private fun addField(field: Field) {
        val points = arrayOf(
            Three.Vector3(sceneX(field.origin.location), sceneY(field.origin.location), POLE_H),
            Three.Vector3(sceneX(field.primaryAnchor.location), sceneY(field.primaryAnchor.location), POLE_H),
            Three.Vector3(sceneX(field.secondaryAnchor.location), sceneY(field.secondaryAnchor.location), POLE_H),
        )
        val geo = Three.BufferGeometry().setFromPoints(points)
        fieldsGroup.add(Three.Mesh(geo, fieldMaterial(field.owner.faction.color)))
    }

    private fun solidMaterial(color: String): dynamic = materialCache.getOrPut("s$color") {
        val p: dynamic = js("({})")
        p.color = color
        Three.MeshStandardMaterial(p)
    }

    // Translucent, faintly glowing glass for portals. (A future "shatter" can swap geometry;
    // XMP hits can later spawn 3D explosion effects.)
    private fun glassMaterial(color: String): dynamic = materialCache.getOrPut("g$color") {
        val p: dynamic = js("({})")
        p.color = color
        p.transparent = true
        p.opacity = 0.45
        p.metalness = 0.0
        p.roughness = 0.08
        p.emissive = color
        p.emissiveIntensity = 0.18
        p.side = 2 // DoubleSide so the far glass surface shows through
        Three.MeshStandardMaterial(p)
    }

    private fun lineMaterial(color: String): dynamic = materialCache.getOrPut("l$color") {
        val p: dynamic = js("({})")
        p.color = color
        Three.LineBasicMaterial(p)
    }

    private fun fieldMaterial(color: String): dynamic = materialCache.getOrPut("f$color") {
        val p: dynamic = js("({})")
        p.color = color
        p.transparent = true
        p.opacity = 0.22
        p.side = 2 // DoubleSide
        Three.MeshBasicMaterial(p)
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
