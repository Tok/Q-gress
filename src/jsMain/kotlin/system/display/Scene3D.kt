package system.display

import World
import agent.Agent
import agent.Faction
import agent.NonFaction
import agent.action.ActionItem
import config.Dim
import external.MapLibre
import external.Three
import portal.Field
import portal.Link
import portal.Portal
import util.data.Pos
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

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
    private const val METERS_PER_PIXEL_Z0 = 156543.03392 // at zoom 0, equator
    private const val AGENT_R = 6.0

    // NPCs (and later players) are human-scale: a head-sized sphere at head
    // height, so a humanoid body can later be added beneath it.
    private const val NPC_HEAD_R = 0.45
    private const val NPC_HEAD_Z = 1.6
    private const val POLE_R = 2.0
    private const val POLE_H = 45.0
    private const val TOP_R = 7.0
    private const val NEUTRAL_COLOR = "#bbbbbb"

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
    private var npcsGroup: dynamic = null
    private var linksGroup: dynamic = null
    private var fieldsGroup: dynamic = null

    // Shared geometries/materials (created lazily once three.js is loaded).
    private val agentGeo: dynamic by lazy { Three.SphereGeometry(AGENT_R, 12, 12) }
    private val npcGeo: dynamic by lazy { Three.SphereGeometry(NPC_HEAD_R, 10, 10) }
    private val poleGeo: dynamic by lazy { Three.CylinderGeometry(POLE_R, POLE_R, POLE_H, 8) }
    private val topGeo: dynamic by lazy { Three.SphereGeometry(TOP_R, 16, 16) }
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

        portalsGroup = Three.Group().also { newScene.add(it) }
        fieldsGroup = Three.Group().also { newScene.add(it) }
        linksGroup = Three.Group().also { newScene.add(it) }
        npcsGroup = Three.Group().also { newScene.add(it) }
        agentsGroup = Three.Group().also { newScene.add(it) }
        scene = newScene

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
        activeRenderer.resetState()
        activeRenderer.render(activeScene, cam)
        map.triggerRepaint()
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
        World.allAgents.forEach { addAgent(it) }
    }

    private fun clear(group: dynamic) {
        group?.clear()
    }

    private fun sceneX(pos: Pos) = (pos.x - Dim.width / 2.0) * metersPerPixel
    private fun sceneY(pos: Pos) = -(pos.y - Dim.height / 2.0) * metersPerPixel

    private fun place(obj: dynamic, x: Double, y: Double, z: Double) {
        obj.position.set(x, y, z)
    }

    private fun addPortal(portal: Portal) {
        val x = sceneX(portal.location)
        val y = sceneY(portal.location)
        val color = portal.owner?.faction?.color ?: NEUTRAL_COLOR
        val pole = Three.Mesh(poleGeo, solidMaterial(color))
        pole.asDynamic().rotation.x = PI / 2 // Y-axis cylinder → vertical (Z up)
        place(pole.asDynamic(), x, y, POLE_H / 2)
        portalsGroup.add(pole)
        val top = Three.Mesh(topGeo, solidMaterial(color))
        place(top.asDynamic(), x, y, POLE_H)
        portalsGroup.add(top)
    }

    private fun addAgent(agent: Agent) {
        val x = sceneX(agent.pos)
        val y = sceneY(agent.pos)
        val sphere = Three.Mesh(agentGeo, solidMaterial(agent.faction.color))
        place(sphere.asDynamic(), x, y, AGENT_R)
        agentsGroup.add(sphere)
        // Action indicator: a camera-facing billboard above the agent.
        val sprite = Three.Sprite(indicatorMaterial(agent.action.item, agent.faction))
        sprite.asDynamic().position.set(x, y, AGENT_R * 2 + 10)
        sprite.asDynamic().scale.set(10.0, 10.0, 1.0)
        agentsGroup.add(sprite)
    }

    private fun addNpc(npc: NonFaction) {
        val sphere = Three.Mesh(npcGeo, solidMaterial(NEUTRAL_COLOR))
        place(sphere.asDynamic(), sceneX(npc.pos), sceneY(npc.pos), NPC_HEAD_Z)
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
