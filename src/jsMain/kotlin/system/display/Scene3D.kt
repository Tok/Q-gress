package system.display

import external.MapLibre
import external.Three
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

/**
 * Renders the game's 3D scene as a MapLibre custom layer (three.js), with the
 * three.js camera driven by the matrix MapLibre passes each frame so the scene
 * stays glued to the map (and works under pitch/rotate).
 *
 * The scene is built in METERS around an origin (the sim anchor center): an
 * object at three.js (x, y, z) sits x metres east, y metres north and z metres
 * up from the origin. The model matrix converts those metres into the map's
 * mercator units.
 *
 * Stage 1 spike: a single test box at the origin to validate the interop.
 */
object Scene3D {
    private const val EARTH_CIRCUMFERENCE = 156543.03392 // metres per pixel at zoom 0, equator

    private var scene: Three.Scene? = null
    private var camera: Three.Camera? = null
    private var renderer: Three.WebGLRenderer? = null

    private var originMerc: dynamic = null
    private var metersScale = 1.0 // mercator units per metre at the origin

    // Sim pixels → metres at the anchor zoom (sim positions are anchor-space pixels).
    var metersPerPixel = 1.0
        private set

    fun register(map: MapLibre.Map, originLng: Double, originLat: Double, anchorZoom: Double) {
        originMerc = MapLibre.asDynamic().MercatorCoordinate.fromLngLat(arrayOf(originLng, originLat), 0.0)
        metersScale = originMerc.meterInMercatorCoordinateUnits() as Double
        metersPerPixel = EARTH_CIRCUMFERENCE * cos(originLat * PI / 180.0) / 2.0.pow(anchorZoom)
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

        newScene.add(Three.AmbientLight(0xffffff, 0.85))
        val sun = Three.DirectionalLight(0xffffff, 0.5)
        sun.asDynamic().position.set(50.0, 80.0, 120.0)
        newScene.add(sun)

        // Spike: a 50 m red box centred 25 m above the origin.
        val box = Three.Mesh(
            Three.BoxGeometry(50, 50, 50),
            Three.MeshStandardMaterial(js("({ color: 0xff3333 })")),
        )
        box.asDynamic().position.set(0.0, 0.0, 25.0)
        newScene.add(box)
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
}
