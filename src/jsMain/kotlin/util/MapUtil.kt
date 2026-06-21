package util

import World
import config.Config
import config.Sim
import config.Styles
import extension.*
import external.MapLibre
import kotlinx.browser.document
import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.ImageData
import system.display.Scene3D
import util.data.Cell
import util.data.Pos
import util.ui.LoadingOverlay
import kotlin.js.Json
import kotlin.math.log2
import kotlin.math.roundToInt

object MapUtil {
    // --- Open, keyless tile sources (no access token / billing) -------------
    // Street backdrop: hosted OpenFreeMap style. Satellite: Esri World Imagery
    // raster. Passability "shadow": OpenFreeMap vector tiles rendered as a
    // white-roads-on-black mask that we read back via WebGL.
    private const val OPENMAPTILES_URL = "https://tiles.openfreemap.org/planet"
    private const val STREET_STYLE_URL = "https://tiles.openfreemap.org/styles/positron"
    private const val ESRI_IMAGERY_TILES =
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"

    private val SATELLITE_STYLE = """{
        "version": 8,
        "glyphs": "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf",
        "sources": {
            "satellite": {
                "type": "raster",
                "tiles": ["$ESRI_IMAGERY_TILES"],
                "tileSize": 256,
                "attribution": "Imagery © Esri, Maxar, Earthstar Geographics"
            },
            "openmaptiles": { "type": "vector", "url": "$OPENMAPTILES_URL" }
        },
        "layers": [
            { "id": "satellite", "type": "raster", "source": "satellite" }
        ]
    }"""

    // Demo scenes: a plain gray backdrop so effects/portals read clearly. The satellite raster is
    // present but hidden (visibility:none) so a demo checkbox can toggle it on via setLayoutProperty.
    private val DEMO_STYLE = """{
        "version": 8,
        "glyphs": "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf",
        "sources": {
            "satellite": { "type": "raster", "tiles": ["$ESRI_IMAGERY_TILES"], "tileSize": 256 },
            "openmaptiles": { "type": "vector", "url": "$OPENMAPTILES_URL" }
        },
        "layers": [
            { "id": "bg", "type": "background", "paint": { "background-color": "#8c8c8c" } },
            { "id": "satellite", "type": "raster", "source": "satellite", "layout": { "visibility": "none" } }
        ]
    }"""

    // Grayscale passability mask, read back via readPixels to build the movement grid.
    // Brightness = walkability: white roads/paths (cheap) > bright-grey grass/park > darker-grey
    // default ground (high penalty) > black buildings & water (impassable). Layer order matters
    // (later paints over earlier): roads sit on top, so bridges/streets stay walkable.
    private val SHADOW_STYLE = """{
        "version": 8,
        "sources": {
            "openmaptiles": { "type": "vector", "url": "$OPENMAPTILES_URL" }
        },
        "layers": [
            { "id": "bg", "type": "background", "paint": { "background-color": "#555555" } },
            {
                "id": "landcover",
                "type": "fill",
                "source": "openmaptiles",
                "source-layer": "landcover",
                "paint": { "fill-color": ["match", ["get", "class"],
                    "wood", "#6e6e6e",
                    "wetland", "#5e5e5e",
                    ["grass", "farmland", "scrub"], "#9a9a9a",
                    ["sand", "rock", "ice"], "#cccccc",
                    "#9a9a9a"
                ] }
            },
            {
                "id": "landuse-green",
                "type": "fill",
                "source": "openmaptiles",
                "source-layer": "landuse",
                "filter": ["match", ["get", "class"],
                    ["park", "garden", "recreation_ground", "pitch", "grass", "cemetery"], true, false],
                "paint": { "fill-color": ["match", ["get", "class"],
                    ["pitch", "recreation_ground"], "#bdbdbd",
                    "#9a9a9a"
                ] }
            },
            {
                "id": "water",
                "type": "fill",
                "source": "openmaptiles",
                "source-layer": "water",
                "paint": { "fill-color": "#000000" }
            },
            {
                "id": "buildings",
                "type": "fill",
                "source": "openmaptiles",
                "source-layer": "building",
                "paint": { "fill-color": "#000000" }
            },
            {
                "id": "roads",
                "type": "line",
                "source": "openmaptiles",
                "source-layer": "transportation",
                "paint": {
                    "line-color": "#ffffff",
                    "line-width": ["interpolate", ["linear"], ["zoom"], 14, 6, 18, 24]
                }
            }
        ]
    }"""

    private fun mapOptions(container: String, style: dynamic, preserveBuffer: Boolean): dynamic {
        val opts: dynamic = js("({})")
        opts.container = container
        opts.style = style
        opts.preserveDrawingBuffer = preserveBuffer
        // Collapse the required tile attribution to a small "ⓘ" that expands on click (the compliant
        // way to make it unobtrusive — OSM/Esri terms require attribution, so we don't suppress it).
        opts.attributionControl = js("({ compact: true })")
        return opts
    }

    private fun initInitialMapbox(): MapLibre.Map {
        val style = if (demoMode) DEMO_STYLE else SATELLITE_STYLE
        return MapLibre.Map(mapOptions(INITIAL_MAP, JSON.parse<Json>(style), false))
    }

    private fun initMapbox(): MapLibre.Map = MapLibre.Map(mapOptions(MAP, STREET_STYLE_URL, false))

    // preserveDrawingBuffer is required so the rendered street mask can be read
    // back with gl.readPixels (otherwise the buffer is cleared after compositing).
    private fun initShadowMap(): MapLibre.Map = MapLibre.Map(mapOptions(SHADOW_MAP, JSON.parse<Json>(SHADOW_STYLE), true))

    private const val INITIAL_MAP = "initialMap"
    private const val MAP = "map"
    private const val SHADOW_MAP = "shadowMap"
    private const val INVISIBLE = "invisible"

    // The grid/sim is built at ZOOM (the anchor). Min/max are a free range for the
    // camera; zooming just scales the canvas layer — the grid is never rebuilt.
    private const val ZOOM = 18
    private const val MIN_ZOOM = 3
    private const val MAX_ZOOM = 21

    // The grid is read from the shadow map at ZOOM over a Sim-sized canvas (SCALE× the screen).
    // The visible maps start zoomed out to frame that whole play area; the anchor stays at ZOOM.
    // Framed so the chosen play-area size fits; read at call time since the size is picked at onboarding.
    private fun displayZoomForSize() = (ZOOM - log2(Sim.scale)).roundToInt()
    private const val DEMO_ZOOM = 19 // demos frame one central object, so sit closer than the game
    private var demoMode = false
    private fun displayZoom() = if (demoMode) DEMO_ZOOM else displayZoomForSize()
    private const val DEFAULT_PITCH = 50.0 // tilt the visible maps so the 3D scene reads as 3D
    private const val MAX_PITCH = 85.0 // MapLibre's ceiling; lets you tilt to near-horizon (and to 0 = top-down)

    private var map: MapLibre.Map? = null
    private var initMap: MapLibre.Map? = null
    private var shadowMap: MapLibre.Map? = null

    // Camera-follow anchor: the simulation lives in a fixed pixel space built at
    // this map view. We transform the canvas layer to follow the live camera.
    private var anchorCenter: dynamic = null
    private var anchorZoom = 0.0

    private fun referenceMap(): MapLibre.Map? = initMap ?: map

    /** Capture the view the grid/sim was built at (call once the grid is ready). */
    private fun captureAnchor() {
        val m = referenceMap() ?: return
        anchorCenter = m.getCenter()
        anchorZoom = ZOOM.toDouble() // grid is read at ZOOM regardless of the (framed) display zoom
    }

    fun rotateBy(degrees: Double) {
        initMap?.let { it.setBearing(it.getBearing() + degrees) }
        map?.let { it.setBearing(it.getBearing() + degrees) }
    }

    fun pitchBy(degrees: Double) {
        initMap?.let { it.setPitch((it.getPitch() + degrees).coerceIn(0.0, MAX_PITCH)) }
        map?.let { it.setPitch((it.getPitch() + degrees).coerceIn(0.0, MAX_PITCH)) }
    }

    fun panBy(dx: Double, dy: Double) {
        val opts: dynamic = js("({animate: false})")
        val offset = arrayOf(dx, dy)
        initMap?.panBy(offset, opts)
        map?.panBy(offset, opts)
    }

    /**
     * "Home": fly the camera back over the play area, framed and **top-down** (pitch 0, bearing 0) —
     * so a player who has panned/rotated away can instantly find the action again. Centers on the
     * grid anchor at the framed display zoom.
     */
    fun goHome() {
        val center = anchorCenter ?: return
        val opts: dynamic = js("({ pitch: 0.0, bearing: 0.0, duration: 900 })")
        opts.center = center
        opts.zoom = displayZoom()
        initMap?.asDynamic()?.flyTo(opts)
        map?.asDynamic()?.flyTo(opts)
    }

    /** Register the 3D scene (three.js custom layer) on the base map, anchored at the grid view. */
    fun enable3D() {
        val targetMap = initMap ?: return
        val center = anchorCenter ?: return
        Scene3D.register(targetMap, center.lng as Double, center.lat as Double, anchorZoom)
        setupNavigation(targetMap)
    }

    /**
     * Hand navigation to MapLibre's own (standard) handlers: left-drag pans, right-drag rotates
     * AND tilts, wheel zooms, all unrestricted — plus a NavigationControl block (zoom + compass +
     * pitch visualiser). The 3D scene is a custom layer glued to this map, so it follows for free.
     */
    private fun setupNavigation(m: MapLibre.Map) {
        m.setMaxPitch(MAX_PITCH)
        val opts: dynamic = js("({ visualizePitch: true, showCompass: true, showZoom: true })")
        // Bottom-right keeps it clear of the top control bar.
        m.addControl(MapLibre.NavigationControl(opts), "bottom-right")
    }

    /** Wire map-native click/hover (only after [enable3D]); MapLibre tells click from drag for us. */
    fun bindInteractions(onClick: (dynamic) -> Unit, onMove: (dynamic) -> Unit) {
        val m = initMap ?: return
        m.onEvent("click", onClick)
        m.onEvent("mousemove", onMove)
    }

    /** Wire just a map click (demo scenes); MapLibre distinguishes a click from a nav drag. */
    fun bindClick(onClick: (dynamic) -> Unit) {
        initMap?.onEvent("click", onClick)
    }

    /** Portal demo: LMB places, RMB removes (suppressing the browser context menu). */
    fun bindPortalDemo(onPlace: (dynamic) -> Unit, onRemove: (dynamic) -> Unit, onMove: (dynamic) -> Unit) {
        val m = initMap ?: return
        m.onEvent("click", onPlace)
        m.onEvent("mousemove", onMove)
        m.onEvent("contextmenu") { event ->
            event.preventDefault()
            onRemove(event)
        }
    }

    /** Ground sim position under a MapLibre mouse event (uses its lng/lat — pitch/zoom safe). */
    fun eventToSimPos(event: dynamic): Pos? {
        val lngLat = event.lngLat ?: return null
        return Scene3D.lngLatToSimPos(lngLat.lng as Double, lngLat.lat as Double)
    }

    fun loadMaps(center: Json, demo: Boolean = false, callback: (Grid) -> Unit) {
        demoMode = demo
        if (!demo) LoadingOverlay.stage(LoadingOverlay.PCT_MAP, "Loading map…")
        document.getElementById(MAP)?.addClass(INVISIBLE)
        document.getElementById(SHADOW_MAP)?.addClass(INVISIBLE)
        loadInitialMap(center, fun(initMap: MapLibre.Map) {
            loadMap(initMap, callback)
        })
    }

    private var demoBuildingsAdded = false

    /**
     * Demo scenes only: toggle the (hidden-by-default) satellite raster over the gray backdrop.
     * With satellite on we also bring up the 3D buildings (beneath the custom 3D layer) for depth.
     */
    fun setDemoSatellite(on: Boolean) {
        val m = initMap ?: return
        m.setLayoutProperty("satellite", "visibility", if (on) "visible" else "none")
        if (on && !demoBuildingsAdded) {
            m.addLayer(buildingLayerConfig(), Scene3D.CUSTOM_LAYER_ID) // under the portals/agents
            demoBuildingsAdded = true
        } else if (demoBuildingsAdded) {
            m.setLayoutProperty("3d-buildings", "visibility", if (on) "visible" else "none")
        }
    }

    private fun loadInitialMap(center: Json, callback: (MapLibre.Map) -> Unit) {
        document.getElementById(INITIAL_MAP)?.removeClass(INVISIBLE)
        fun addLayers(targetMap: MapLibre.Map) {
            if (!demoMode && Styles.use3DBuildings) { // demos use the bare gray style, no buildings
                targetMap.addLayer(buildingLayerConfig())
            }
        }
        val existing = initMap
        if (existing == null) {
            val newMap = initInitialMapbox()
            initMap = newMap
            newMap.on("load") {
                addLayers(newMap)
                callback(newMap)
            }
            newMap.setMinZoom(MIN_ZOOM)
            newMap.setMaxZoom(MAX_ZOOM)
            newMap.setZoom(displayZoom())
            newMap.setCenter(center)
            newMap.setPitch(DEFAULT_PITCH)
        } else {
            existing.on("moveend") {
                addLayers(existing)
                callback(existing)
            }
            val options: Json = JSON.parse("""{"center": [$center], "zoom": ${displayZoom()}}""")
            existing.jumpTo(options)
        }
    }

    // https://maplibre.org/maplibre-gl-js/docs/API/
    private fun loadMap(initMap: MapLibre.Map, callback: (Grid) -> Unit) {
        if (!demoMode) LoadingOverlay.stage(LoadingOverlay.PCT_STREET, "Loading street tiles…")
        val center = initMap.getCenter()
        document.getElementById(MAP)?.removeClass(INVISIBLE)
        val existing = map
        if (existing == null) {
            val newMap = initMapbox()
            map = newMap
            newMap.on("load", fun() {
                loadShadowMap(center, callback)
            })
            val geoCtrl: dynamic = js("({})")
            geoCtrl.positionOptions = js("({enableHighAccuracy: true})")
            geoCtrl.trackUserLocation = false
            newMap.addControl(MapLibre.GeolocateControl(geoCtrl))
            newMap.setMinZoom(MIN_ZOOM)
            newMap.setMaxZoom(MAX_ZOOM)
            newMap.setZoom(displayZoom())
            newMap.setCenter(center)
            newMap.setPitch(DEFAULT_PITCH)
        } else {
            existing.on("moveend", fun() {
                loadShadowMap(center, callback)
            })
            val lng = center["lng"]
            val lat = center["lat"]
            val options: Json = JSON.parse("""{"center": [$lng,$lat],"zoom": ${displayZoom()}}""")
            existing.jumpTo(options)
        }
    }

    private fun loadShadowMap(center: Json, callback: (Grid) -> Unit) {
        if (!demoMode) LoadingOverlay.stage(LoadingOverlay.PCT_SHADOW, "Rendering passability map…")
        document.getElementById(SHADOW_MAP)?.remove()
        val div = document.createElement("div") as HTMLDivElement
        div.id = SHADOW_MAP
        div.addClass(SHADOW_MAP, "top")
        // Render the shadow map over the whole Sim area (SCALE× the screen) at full ZOOM detail,
        // so the readback grid covers the larger play area.
        div.style.width = "${Sim.width}px"
        div.style.height = "${Sim.height}px"
        document.body?.append(div)
        val newMap = initShadowMap()
        shadowMap = newMap
        // Wait for 'idle' (all tiles loaded AND fully rendered), not 'load',
        // before reading pixels — otherwise the street mask can be incomplete.
        newMap.once("idle", fun() {
            addGrid(callback)
        })
        newMap.setMinZoom(MIN_ZOOM)
        newMap.setMaxZoom(MAX_ZOOM)
        newMap.setZoom(ZOOM)
        newMap.setCenter(center)
    }

    private fun addGrid(callback: (Grid) -> Unit) {
        if (!demoMode) LoadingOverlay.stage(LoadingOverlay.PCT_GRID, "Reading street grid…")
        // Select the shadow map's own canvas robustly (query within its
        // container) instead of relying on a fragile global canvas index.
        val container = document.getElementById(SHADOW_MAP)
        val shadowMapCan: dynamic = container?.asDynamic()?.querySelector("canvas.maplibregl-canvas")
        // MapLibre renders with WebGL2; fall back to WebGL1 just in case.
        val gl: dynamic = shadowMapCan.getContext("webgl2") ?: shadowMapCan.getContext("webgl")
        val width = gl.canvas.width as Int
        val height = gl.canvas.height as Int
        val rawBuf = Uint8Array((width * height * 4))
        gl.readPixels(0, 0, width, height, gl.RGBA, gl.UNSIGNED_BYTE, rawBuf)
        val imageData: ImageData = World.createStreetImage(rawBuf, width, height)
        val grid = createGrid(imageData, width, height)
        shadowMap?.let { PortalNames.build(it) } // query POI/street names while the tiles are loaded
        teardownShadowMap() // grid + names are read — destroy the shadow map to free its WebGL context
        captureAnchor()
        callback(grid)
    }

    /**
     * The shadow map is only needed at build time (passability readback + POI/street names). Destroy
     * the MapLibre instance (frees its WebGL context — otherwise it leaked, since rebuilds only ever
     * dropped its DOM container) and remove the container. A later rebuild re-creates it fresh.
     */
    private fun teardownShadowMap() {
        shadowMap?.asDynamic()?.remove()
        shadowMap = null
        document.getElementById(SHADOW_MAP)?.remove()
    }

    // 3D building extrusions from the OpenFreeMap (openmaptiles) vector tiles.
    // openmaptiles exposes render_height / render_min_height on the building layer.
    private fun buildingLayerConfig(): Json = JSON.parse(
        """{
            "id": "3d-buildings",
            "source": "openmaptiles",
            "source-layer": "building",
            "type": "fill-extrusion",
            "minzoom": 14,
            "paint": {
                "fill-extrusion-color": "#333333",
                "fill-extrusion-height": ["coalesce", ["get", "render_height"], 8],
                "fill-extrusion-base": ["coalesce", ["get", "render_min_height"], 0],
                "fill-extrusion-opacity": 0.9
            }
        }""",
    )

    const val OFFSCREEN_CELL_ROWS = 10

    private fun createGrid(imageData: ImageData, width: Int, height: Int): Grid {
        // Grid resolution follows the game canvas (CSS pixels), not the raw
        // WebGL readback (which is window × devicePixelRatio). The full readback
        // is downscaled into this grid below, so the grid stays aligned with the
        // visible map regardless of the display's pixel ratio.
        val w = Sim.width / Pos.res
        val h = Sim.height / Pos.res
        fun isOffScreen(pos: Pos) = pos.x < 0 || pos.y < 0 || pos.x >= w || pos.y >= h
        fun nextRow(tempCtx: Ctx, h: Int, x: Int): List<Pair<Pos, Cell>> = (-OFFSCREEN_CELL_ROWS until (h + OFFSCREEN_CELL_ROWS)).map { y ->
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
        val rawGrid: Grid = (-OFFSCREEN_CELL_ROWS until (w + OFFSCREEN_CELL_ROWS)).flatMap { x ->
            nextRow(tempCtx, h, x)
        }.toMap()
        // No closed-off areas: carve corridors so every passable island reaches the outside.
        val grid = GridConnectivity.connectIslands(rawGrid)
        World.walkability = GridConnectivity.walkability(grid, w, h)
        console.log("grid built: walkability ${(World.walkability * 100).toInt()}% (${GridConnectivity.components(rawGrid).size} islands connected)")
        return grid
    }

    // Layer switching: #initialMap holds the satellite style, #map the street style.
    fun showSatellite() {
        document.getElementById(INITIAL_MAP)?.removeClass(INVISIBLE)
        document.getElementById(MAP)?.addClass(INVISIBLE)
    }

    fun showStreet() {
        document.getElementById(INITIAL_MAP)?.addClass(INVISIBLE)
        document.getElementById(MAP)?.removeClass(INVISIBLE)
    }

    /**
     * Desaturate the satellite raster (terrain only — the 3D portals/agents render in a separate
     * custom layer and stay coloured). Grayscale is the default view; "Colored" turns it off.
     */
    fun setGrayscale(on: Boolean) {
        initMap?.setPaintProperty("satellite", "raster-saturation", if (on) -1.0 else 0.0)
    }

    /** Fade the 3D buildings (0 = invisible … 1 = solid) so crowded areas don't hide the action. */
    fun setBuildingOpacity(opacity: Double) {
        initMap?.setPaintProperty("3d-buildings", "fill-extrusion-opacity", opacity)
    }
}
