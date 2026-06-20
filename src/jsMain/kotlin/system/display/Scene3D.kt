package system.display

import World
import agent.Agent
import agent.Faction
import agent.NonFaction
import agent.action.ActionItem
import config.Colors
import config.Sim
import external.GLTFLoader
import external.MapLibre
import external.Three
import items.level.XmpLevel
import kotlinx.browser.document
import portal.Field
import portal.Link
import portal.Portal
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
    private const val GRAVITY = 9.8 // m/s²
    private const val SHARD_TARGET_PATH = 3.0 // pole-shard target size (metres)
    private const val POLE_SHARD_COUNT = 12
    private const val XMP_OPACITY = 0.4
    private const val XMP_LIFE = 0.6 // seconds for the shockwave to expand + fade
    private const val XMP_RANGE_SCALE = 0.5 // XmpLevel.rangeM → scene-metre radius

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
    private val activeShards = mutableListOf<Shard>()
    private val activeBursts = mutableListOf<Burst>()
    private var lastFrameMs = 0.0 // for per-frame shard physics dt

    /** One in-flight glass fragment: a mesh with velocity + tumble (3 each), fading over its life. */
    private class Shard(
        val mesh: dynamic,
        val mat: dynamic,
        val vel: DoubleArray,
        val spin: DoubleArray,
        var age: Double,
        val life: Double,
    )

    /** An expanding XMP shockwave dome: grows to maxR while fading over its life. */
    private class Burst(val mesh: dynamic, val mat: dynamic, val maxR: Double, var age: Double, val life: Double)

    // Shared geometries/materials (created lazily once three.js is loaded).
    private val headGeo: dynamic by lazy { Three.SphereGeometry(HEAD_R, 10, 10) }
    private val poleGeo: dynamic by lazy { Three.CylinderGeometry(POLE_R, POLE_R, POLE_H, 8) }
    private val topGeo: dynamic by lazy { Three.SphereGeometry(TOP_R, 16, 16) }
    private val coneGeo: dynamic by lazy { Three.ConeGeometry(VECTOR_CONE_R, VECTOR_CONE_H, 6) }
    private val burstGeo: dynamic by lazy { Three.SphereGeometry(1.0, 24, 16) } // unit sphere, scaled per burst
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
        val iter = activeShards.iterator()
        while (iter.hasNext()) {
            val s = iter.next()
            s.age += dt
            s.vel[2] -= GRAVITY * dt
            val pos = s.mesh.position
            pos.x = (pos.x as Double) + s.vel[0] * dt
            pos.y = (pos.y as Double) + s.vel[1] * dt
            pos.z = (pos.z as Double) + s.vel[2] * dt
            if ((pos.z as Double) <= 0.0) { // landed on the terrain
                pos.z = 0.0
                s.vel[2] = 0.0
                s.vel[0] *= 0.4
                s.vel[1] *= 0.4
            }
            val rot = s.mesh.rotation
            rot.x = (rot.x as Double) + s.spin[0] * dt
            rot.y = (rot.y as Double) + s.spin[1] * dt
            rot.z = (rot.z as Double) + s.spin[2] * dt
            if (s.age > s.life - SHARD_FADE) {
                s.mat.opacity = SHARD_OPACITY * ((s.life - s.age) / SHARD_FADE).coerceIn(0.0, 1.0)
            }
            if (s.age >= s.life) {
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
        val mat = shardMaterial(color)
        val mesh = Three.Mesh(holder.geo, mat)
        val d = mesh.asDynamic()
        d.scale.set(scale, scale, scale)
        d.rotation.set(PI / 2, Util.random() * 2.0 * PI, Util.random() * 2.0 * PI) // model Y-up → scene Z-up + spin
        d.position.set(pos[0], pos[1], pos[2])
        val a = Util.random() * 2.0 * PI
        val r = burstH * (0.4 + Util.random() * 0.6)
        val vel = doubleArrayOf(cos(a) * r, sin(a) * r, burstH * (0.5 + Util.random()))
        val spin = doubleArrayOf(randSpin(), randSpin(), randSpin())
        activeShards.add(Shard(mesh, mat, vel, spin, 0.0, SHARD_LIFE_MIN + Util.random() * (SHARD_LIFE_MAX - SHARD_LIFE_MIN)))
        shardsGroup.add(mesh)
    }

    private fun randSpin() = (Util.random() - 0.5) * 2.0 * SHARD_SPIN

    /** Play an expanding XMP shockwave dome at a location, sized by the burster level (1..8). */
    fun playXmpBurst(location: Pos, level: Int) {
        scene ?: return
        val rangeM = XmpLevel.values().find { it.level == level }?.rangeM ?: XmpLevel.ONE.rangeM
        val p: dynamic = js("({})")
        p.color = Colors.damage
        p.transparent = true
        p.opacity = XMP_OPACITY
        p.side = 2 // DoubleSide
        p.depthWrite = false
        val mat = Three.MeshBasicMaterial(p)
        val mesh = Three.Mesh(burstGeo, mat)
        mesh.asDynamic().position.set(sceneX(location), sceneY(location), 0.0)
        burstsGroup.add(mesh)
        activeBursts.add(Burst(mesh, mat, rangeM * XMP_RANGE_SCALE, 0.0, XMP_LIFE))
    }

    private fun updateBursts(dt: Double) {
        val iter = activeBursts.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.age += dt
            val f = (b.age / b.life).coerceIn(0.0, 1.0)
            val r = (b.maxR * f).coerceAtLeast(0.01)
            b.mesh.scale.set(r, r, r)
            b.mat.opacity = XMP_OPACITY * (1.0 - f)
            if (b.age >= b.life) {
                burstsGroup.remove(b.mesh)
                b.mat.dispose()
                iter.remove()
            }
        }
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
        geo.computeBoundingBox()
        val bb = geo.boundingBox
        val holder: dynamic = js("({})")
        holder.geo = geo
        holder.cx = ((bb.min.x as Double) + (bb.max.x as Double)) / 2.0
        holder.cy = ((bb.min.y as Double) + (bb.max.y as Double)) / 2.0
        holder.cz = ((bb.min.z as Double) + (bb.max.z as Double)) / 2.0
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
