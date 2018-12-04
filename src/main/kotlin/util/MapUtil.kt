package util

import World
import config.Config
import config.Styles
import extension.*
import external.MapBox
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.ImageData
import org.w3c.dom.get
import util.data.Cell
import util.data.Pos
import kotlin.browser.document
import kotlin.dom.addClass
import kotlin.dom.removeClass
import kotlin.js.Json

object MapUtil {
    private fun initInitialMapbox(): MapBox =
        js("new mapboxgl.Map({'container':'initialMap','style':'mapbox://styles/zirteq/cjazhkywuppf42rnx453i73z5'});")

    private fun initMapbox(): MapBox =
        js("new mapboxgl.Map({'container':'map','style':'mapbox://styles/zirteq/cjb19u1dy02a82slyklj33o6g'});")

    private fun initShadowMap(): MapBox =
        js("new mapboxgl.Map({'container':'shadowMap','style':'mapbox://styles/zirteq/cjaq7lw9e2y7u2rn7u6xskobn'});")

    private val GEO_CTRL_LITERAL =
        "new mapboxgl.GeolocateControl({'positionOptions':{'enableHighAccuracy':true,'zoom':18},'trackUserLocation':false})"

    private const val INITIAL_MAP = "initialMap"
    private const val MAP = "map"
    private const val SHADOW_MAP = "shadowMap"
    private const val INVISIBLE = "invisible"

    private const val ZOOM = 18
    private const val MIN_ZOOM = 18
    private const val MAX_ZOOM = 18

    private var map: MapBox? = null
    private var initMap: MapBox? = null
    private var shadowMap: MapBox? = null

    fun loadMaps(center: Json, callback: (Grid) -> Unit) {
        document.getElementById(MAP)?.addClass(INVISIBLE)
        document.getElementById(SHADOW_MAP)?.addClass(INVISIBLE)
        loadInitialMap(center, fun(initMap: MapBox) {
            loadMap(initMap, callback)
        })
    }

    private fun loadInitialMap(center: Json, callback: (MapBox) -> Unit) {
        document.getElementById(INITIAL_MAP)?.removeClass(INVISIBLE)
        fun addLayers() {
            if (Styles.use3DBuildings) {
                initMap!!.addLayer(buildingLayerConfig())
            }
        }
        if (initMap == null) {
            initMap = initInitialMapbox()
            initMap!!.on("load") { addLayers(); callback(initMap!!) }
            initMap!!.setMinZoom(MIN_ZOOM)
            initMap!!.setMaxZoom(MAX_ZOOM)
            initMap!!.setZoom(ZOOM)
            initMap!!.setCenter(center)
        } else {
            initMap!!.on("moveend") { addLayers(); callback(initMap!!) }
            val options: Json = JSON.parse("""{"center": [$center], "zoom": 18}""".trimMargin())
            initMap!!.jumpTo(options)
        }
    }

    //https://www.mapbox.com/mapbox-gl-js/api/
    private fun loadMap(initMap: MapBox, callback: (Grid) -> Unit) {
        val center = initMap.getCenter()
        document.getElementById(MAP)?.removeClass(INVISIBLE)
        if (map == null) {
            map = initMapbox()
            map!!.on("load", fun() { loadShadowMap(center, callback) })
            map!!.addControl(js(GEO_CTRL_LITERAL))
            map!!.setMinZoom(MIN_ZOOM)
            map!!.setMaxZoom(MAX_ZOOM)
            map!!.setZoom(ZOOM)
            map!!.setCenter(center)
        } else {
            map!!.on("moveend", fun() { loadShadowMap(center, callback) })
            val lng = center["lng"]
            val lat = center["lat"]
            val options: Json = JSON.parse("""{"center": [$lng,$lat],"zoom": 18}""".trimMargin())
            map!!.jumpTo(options)
        }
    }

    private fun loadShadowMap(center: Json, callback: (Grid) -> Unit) {
        document.getElementById(SHADOW_MAP)?.remove()
        val div = document.createElement("div") as HTMLDivElement
        div.id = SHADOW_MAP
        div.addClass(SHADOW_MAP, "top")
        document.body?.append(div)
        shadowMap = initShadowMap()
        shadowMap!!.on("load", fun() { addGrid(callback) })
        shadowMap!!.setMinZoom(MIN_ZOOM)
        shadowMap!!.setMaxZoom(MAX_ZOOM)
        shadowMap!!.setZoom(ZOOM)
        shadowMap!!.setCenter(center)
    }

    private fun addGrid(callback: (Grid) -> Unit) {
        val maps = document.getElementsByClassName("mapboxgl-canvas")
        val shadowMapCan: dynamic = maps[2] //!
        val gl: dynamic = shadowMapCan.getContext("webgl")
        val width = gl.canvas.width as Int
        val height = gl.canvas.height as Int
        val rawBuf = Uint8Array((width * height * 4))
        gl.readPixels(0, 0, width, height, gl.RGBA, gl.UNSIGNED_BYTE, rawBuf)
        val imageData: ImageData = World.createStreetImage(rawBuf, width, height)
        World.shadowStreetMap = imageData
        val grid = createGrid(imageData, width, height)
        document.getElementById(SHADOW_MAP)?.addClass(INVISIBLE)
        callback(grid)
    }

    private fun buildingLayerConfig(): Json {
        return JSON.parse(
            """{
            "id": "3d-buildings",
            "source": "composite",
            "source-layer": "building",
            "filter": ["==", "extrude", "true"],
            "type": "fill-extrusion",
            "minzoom": 15,
            "paint": {
                "fill-extrusion-color": "#333333",
                "fill-extrusion-height": ["interpolate", ["linear"], ["zoom"], 15, 0, 15.05, ["get", "height"]],
                "fill-extrusion-base": ["interpolate", ["linear"], ["zoom"], 15, 0, 15.05, ["get", "min_height"]],
                "fill-extrusion-opacity": 0.9
            }
        }"""
        )
    }

    const val OFFSCREEN_CELL_ROWS = 10

    private fun createGrid(imageData: ImageData, width: Int, height: Int): Grid {
        val w = width / Pos.res
        val h = height / Pos.res
        fun isOffScreen(pos: Pos) = pos.x < 0 || pos.y < 0 || pos.x >= w || pos.y >= h
        fun nextRow(tempCtx: Ctx, h: Int, x: Int): List<Pair<Pos, Cell>> {
            return (-OFFSCREEN_CELL_ROWS until (h + OFFSCREEN_CELL_ROWS)).map { y ->
                val pos = Pos(x, y)
                if (isOffScreen(pos)) {
                    val isPassable = true
                    val penalty = 80
                    pos to Cell(pos, isPassable, penalty)
                } else {
                    val scaledPixel = tempCtx.getImageData(x, y, 1, 1).data[0]
                    val passabilityOffset = 32
                    val isPassable = scaledPixel > passabilityOffset
                    val penalty =
                        PathUtil.MIN_HEAT + ((255 - scaledPixel) * (PathUtil.MAX_HEAT - PathUtil.MIN_HEAT) / 255)
                    pos to Cell(pos, isPassable, penalty)
                }
            }
        }

        val unscaledCan = document.createElement("canvas") as Canvas
        val unscaledCtx = unscaledCan.getContext("2d") as Ctx
        unscaledCan.width = width
        unscaledCan.height = height
        unscaledCtx.putImageData(imageData, 0.0, 0.0)

        val tempCan = document.createElement("canvas") as Canvas
        val tempCtx = tempCan.getContext("2d") as Ctx
        tempCan.width = w
        tempCan.height = h

        (0..Config.shadowBlurCount).forEach { _ -> tempCan.blur() }
        tempCtx.drawImage(unscaledCan, 0, 0, w, h)
        return (-OFFSCREEN_CELL_ROWS until (w + OFFSCREEN_CELL_ROWS)).flatMap { x ->
            nextRow(tempCtx, h, x)
        }.toMap()
    }

    fun showSatelliteMap() {
        (document.getElementById(INITIAL_MAP))?.addClass(INVISIBLE)
        (document.getElementById(MAP))?.removeClass(INVISIBLE)
    }

    fun hideSatelliteMap() {
        (document.getElementById(INITIAL_MAP))?.removeClass(INVISIBLE)
        (document.getElementById(MAP))?.addClass(INVISIBLE)
    }
}
