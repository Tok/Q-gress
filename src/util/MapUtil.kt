package util

import Canvas
import Ctx
import World
import config.Config
import config.Styles
import external.MapBox
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.ImageData
import org.w3c.dom.get
import util.data.Cell
import util.data.Coords
import kotlin.browser.document
import kotlin.dom.addClass
import kotlin.dom.removeClass
import kotlin.js.Json

object MapUtil {
    fun initInitialMapbox() = js("new mapboxgl.Map({'container':'initialMap','style':'mapbox://styles/zirteq/cjazhkywuppf42rnx453i73z5'});")
    fun initMapbox() = js("new mapboxgl.Map({'container':'map','style':'mapbox://styles/zirteq/cjb19u1dy02a82slyklj33o6g'});")
    fun initShadowMap() = js("new mapboxgl.Map({'container':'shadowMap','style':'mapbox://styles/zirteq/cjaq7lw9e2y7u2rn7u6xskobn'});")
    val GEO_CTRL_LITERAL = "new mapboxgl.GeolocateControl({'positionOptions':{'enableHighAccuracy':true,'zoom':18},'trackUserLocation':false})"

    val INITIAL_MAP = "initialMap"
    val MAP = "map"
    val SHADOW_MAP = "shadowMap"
    val INVISIBLE = "invisible"

    val ZOOM = 18
    val MIN_ZOOM = 18
    val MAX_ZOOM = 18

    var map: MapBox? = null
    var initMap: MapBox? = null
    var shadowMap: MapBox? = null

    fun loadMaps(center: Json, callback: (Map<Coords, Cell>) -> Unit) {
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
            initMap!!.on("load", fun() { addLayers(); callback(initMap!!) })
            initMap!!.setMinZoom(MIN_ZOOM)
            initMap!!.setMaxZoom(MAX_ZOOM)
            initMap!!.setZoom(ZOOM)
            initMap!!.setCenter(center)
        } else {
            initMap!!.on("moveend", fun() { addLayers(); callback(initMap!!) })
            val options: Json = JSON.parse("""{"center": [$center], "zoom": 18}""".trimMargin())
            initMap!!.jumpTo(options)
        }
    }

    //https://www.mapbox.com/mapbox-gl-js/api/
    private fun loadMap(initMap: MapBox, callback: (Map<Coords, Cell>) -> Unit) {
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
            val lng = center.get("lng")
            val lat = center.get("lat")
            val options: Json = JSON.parse("""{"center": [$lng,$lat],"zoom": 18}""".trimMargin())
            map!!.jumpTo(options)
        }
    }

    private fun loadShadowMap(center: Json, callback: (Map<Coords, Cell>) -> Unit) {
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

    fun addGrid(callback: (Map<Coords, Cell>) -> Unit) {
        val maps = document.getElementsByClassName("mapboxgl-canvas")
        val shadowMapCan: dynamic = maps.get(2) //!
        val gl: dynamic = shadowMapCan.getContext("webgl")
        val width = gl.canvas.width
        val height = gl.canvas.height
        val rawBuf = Uint8Array((width * height * 4) as Int)
        gl.readPixels(0, 0, width, height, gl.RGBA, gl.UNSIGNED_BYTE, rawBuf)
        val imageData: ImageData = World.createStreetImage(rawBuf, width, height)
        World.shadowStreetMap = imageData
        val grid = createGrid(imageData, width, height)
        document.getElementById(SHADOW_MAP)?.addClass(INVISIBLE)
        callback(grid)
    }

    private fun buildingLayerConfig(): Json {
        return JSON.parse("""{
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
        }""")
    }

    val OFFSCREEN_CELL_ROWS = 10

    fun createGrid(imageData: ImageData, width: Int, height: Int): Map<Coords, Cell> {
        val w = width / PathUtil.RESOLUTION
        val h = height / PathUtil.RESOLUTION
        fun isOffScreen(pos: Coords) = pos.x < 0 || pos.y < 0 || pos.x >= w || pos.y >= h
        fun nextRow(tempCtx: Ctx, h: Int, x: Int): List<Pair<Coords, Cell>> {
            return (-OFFSCREEN_CELL_ROWS..(h + OFFSCREEN_CELL_ROWS) - 1).map { y ->
                val pos = Coords(x, y)
                if (isOffScreen(pos)) {
                    val isPassable = true
                    val penalty = 80
                    pos to Cell(pos, isPassable, penalty)
                } else {
                    val scaledPixel = tempCtx.getImageData(x.toDouble(), y.toDouble(), 1.0, 1.0).data.get(0)
                    val passabilityOffset = 32
                    val isPassable = scaledPixel > passabilityOffset
                    val penalty = PathUtil.MIN_HEAT + ((255 - scaledPixel) * (PathUtil.MAX_HEAT - PathUtil.MIN_HEAT) / 255)
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

        (0..Config.shadowBlurCount).forEach { tempCan.blur() }
        tempCtx.drawImage(unscaledCan, 0.0, 0.0, w.toDouble(), h.toDouble())
        val rawGrid = (-OFFSCREEN_CELL_ROWS..(w + OFFSCREEN_CELL_ROWS) - 1).flatMap { x ->
            nextRow(tempCtx, h, x)
        }.toMap()
        return rawGrid
    }

    fun showSateliteMap() {
        (document.getElementById(INITIAL_MAP))?.addClass(INVISIBLE)
        (document.getElementById(MAP))?.removeClass(INVISIBLE)
    }

    fun hideSateliteMap() {
        (document.getElementById(INITIAL_MAP))?.removeClass(INVISIBLE)
        (document.getElementById(MAP))?.addClass(INVISIBLE)
    }
}
