package util

import Canvas
import Ctx
import World
import config.Config
import config.Styles
import external.MapBox
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.ImageData
import org.w3c.dom.get
import util.data.Cell
import util.data.Coords
import kotlin.browser.document

object MapUtil {
    val redSquare: JSON = JSON.parse("[9.373274, 47.422139]")
    val chlosterPlatz: JSON = JSON.parse("[9.3770000, 47.4240000]")
    val badRagaz: JSON = JSON.parse("[9.500324, 47.0024734]")
    val gollums: JSON = JSON.parse("[8.5952000, 47.3620000]")
    val escherWyss: JSON = JSON.parse("[8.5220562, 47.3907937]")
    val primeTower: JSON = JSON.parse("[8.5183064, 47.3867261]")
    val gizaPlateau: JSON = JSON.parse("[31.1320000, 29.9780000]")
    val eiffel: JSON = JSON.parse("[2.2948595, 48.858243]")
    val groundZero: JSON = JSON.parse("[-74.0123000, 40.7125000]")
    val INITIAL_MAP_CENTER = chlosterPlatz

    fun initInitialMapbox() = js("new mapboxgl.Map({'container':'initialMap','style':'mapbox://styles/zirteq/cjazhkywuppf42rnx453i73z5'});")
    fun initMapbox() = js("new mapboxgl.Map({'container':'map','style':'mapbox://styles/zirteq/cjb19u1dy02a82slyklj33o6g'});")
    fun initShadowMap() = js("new mapboxgl.Map({'container':'shadowMap','style':'mapbox://styles/zirteq/cjaq7lw9e2y7u2rn7u6xskobn'});")
    val GEO_CTRL_LITERAL = "new mapboxgl.GeolocateControl({'positionOptions':{'enableHighAccuracy':true,'zoom':18},'trackUserLocation':false})"

    val ZOOM = 18
    val MIN_ZOOM = 18
    val MAX_ZOOM = 18

    var map: MapBox? = null
    var initMap: MapBox? = null

    fun loadMaps(isFirstLoad: Boolean, callback: (Map<Coords, Cell>) -> Unit) {
        loadInitialMap(isFirstLoad, fun(initMap: MapBox) {
            loadMap(isFirstLoad, initMap, callback)
        })
    }

    private fun loadInitialMap(isFirstLoad: Boolean, onLoadCallback: (MapBox) -> Unit) {
        if (isFirstLoad || initMap == null) {
            initMap = initInitialMapbox()
        }
        with(initMap!!) {
            if (isFirstLoad || map == null) {
                //addControl(js(GEO_CTRL_LITERAL))
                setMinZoom(MIN_ZOOM)
                setMaxZoom(MAX_ZOOM)
                setZoom(ZOOM)
                setCenter(INITIAL_MAP_CENTER)
            } else {
                val currentMapCenter = map!!.getCenter()
                setCenter(currentMapCenter)
            }
        }
        fun addLayers() {
            if (Styles.use3DBuildings) {
                initMap!!.addLayer(buildingLayerConfig())
            }
        }
        if (initMap!!.loaded()) {
            addLayers()
            onLoadCallback(initMap!!)
        } else {
            initMap!!.on("load", fun() {
                addLayers()
                onLoadCallback(initMap!!)
            })
        }
    }

    //https://www.mapbox.com/mapbox-gl-js/api/
    private fun loadMap(isFirstLoad: Boolean, initMap: MapBox, callback: (Map<Coords, Cell>) -> Unit) {
        if (isFirstLoad || map == null) {
            map = initMapbox()
        }
        with(map!!) {
            addControl(js(GEO_CTRL_LITERAL))
            setMinZoom(MIN_ZOOM)
            setMaxZoom(MAX_ZOOM)
            setZoom(ZOOM)
            if (isFirstLoad) {
                setCenter(initMap.getCenter())
            }
        }
        DrawUtil.drawLoadingText("Loading map..")
        if (map!!.loaded()) {
            loadShadowMap(map!!.getCenter(), callback)
        } else {
            map!!.on("load", fun() {
                loadShadowMap(map!!.getCenter(), callback)
            })
        }
    }

    private fun loadShadowMap(center: JSON, callback: (Map<Coords, Cell>) -> Unit) {
        //println("DEBUG: loading shadow map.")
        DrawUtil.drawLoadingText("Creating grid..")
        val shadowMap = initShadowMap()
        with(shadowMap) {
            setMinZoom(MIN_ZOOM)
            setMaxZoom(MAX_ZOOM)
            setZoom(ZOOM)
            setCenter(center)
        }
        shadowMap.on("load", fun() {
            val maps = document.getElementsByClassName("mapboxgl-canvas")
            val shadowMapCan: dynamic = maps.get(2)

            val gl: dynamic = shadowMapCan.getContext("webgl")
            val width = gl.canvas.width
            val height = gl.canvas.height
            val rawBuf = Uint8Array((width * height * 4) as Int)
            gl.readPixels(0, 0, width, height, gl.RGBA, gl.UNSIGNED_BYTE, rawBuf)
            val imageData: ImageData = World.createStreetImage(rawBuf, gl.canvas.width, gl.canvas.height)
            World.shadowStreetMap = imageData

            document.getElementById("shadowMap")?.remove()
            val grid = createGrid(imageData, width, height)

            callback(grid)
        })
    }

    private fun buildingLayerConfig(): JSON {
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
}
