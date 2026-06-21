package util.ui

import config.Sim
import external.MapLibre
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import kotlin.js.Json
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

/**
 * Interactive location picker for the onboarding location screen: a MapLibre globe the player can
 * pan/zoom to find a spot (no Geolocation/location sharing). A centre pin + a white **play-area
 * box** (sized to the sim, re-centred on every move) show exactly what will be generated; the player
 * positions it and confirms. A button toggles globe/flat. [confirmCenter] returns the chosen centre.
 */
object MiniMap {
    private const val CONTAINER_ID = "miniMap"
    private const val OVERVIEW_ZOOM = 13 // close enough that the play-area box is visible from the start
    private const val METERS_PER_PIXEL_Z0 = 78271.516964 // 512-px tiles (matches Scene3D)
    private const val ANCHOR_ZOOM = 18.0 // the box is sized at the sim's anchor zoom
    private const val METERS_PER_DEG_LAT = 111320.0
    private const val ESRI_TILES =
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"

    private val STYLE = """{
        "version": 8,
        "sources": { "satellite": { "type": "raster", "tiles": ["$ESRI_TILES"], "tileSize": 256 } },
        "layers": [{ "id": "satellite", "type": "raster", "source": "satellite" }]
    }"""

    private const val BOX_LAYER = """{
        "id": "areaBoxLine", "type": "line", "source": "areaBox",
        "paint": { "line-color": "#ffffff", "line-width": 2 }
    }"""

    private var mini: MapLibre.Map? = null
    private var toggleBtn: HTMLElement? = null
    private var globe = true

    /** Build the interactive picker inside [parent], centred at [lng]/[lat]. No-op if already created. */
    fun create(parent: HTMLElement, lng: Double, lat: Double) {
        if (mini != null) return

        // Outer wrapper carries our styling; the map mounts in an inner div so MapLibre's own
        // `maplibregl-map { position: relative }` lands there, not on the wrapper.
        val wrapper = document.createElement("div") as HTMLElement
        wrapper.id = CONTAINER_ID
        wrapper.className = "miniMap"
        val inner = document.createElement("div") as HTMLElement
        inner.className = "miniMapCanvas"
        wrapper.appendChild(inner)
        val pin = document.createElement("div") as HTMLElement
        pin.className = "miniMapPin"
        wrapper.appendChild(pin)
        parent.appendChild(wrapper)

        val opts: dynamic = js("({})")
        opts.container = inner
        opts.style = JSON.parse<Json>(STYLE)
        opts.interactive = true // the player pans/zooms to find a location
        opts.attributionControl = false
        opts.center = arrayOf(lng, lat)
        opts.zoom = OVERVIEW_ZOOM
        val m = MapLibre.Map(opts)
        m.on("load") {
            applyProjection()
            val src: dynamic = js("({})")
            src.type = "geojson"
            src.data = areaBox(lng, lat)
            m.addSource("areaBox", src)
            m.addLayer(JSON.parse<Json>(BOX_LAYER))
        }
        m.on("move") { updateBox() } // keep the play-area box centred on the current view
        mini = m

        val btn = document.createElement("button") as HTMLElement
        btn.className = "miniMapToggle"
        btn.textContent = "FLAT"
        btn.asDynamic().onclick = { toggle() }
        wrapper.appendChild(btn)
        toggleBtn = btn
    }

    /** Fly to a preset location (recenters; the box follows). */
    fun setCenter(lng: Double, lat: Double) {
        val opts: dynamic = js("({})")
        opts.center = arrayOf(lng, lat)
        opts.zoom = OVERVIEW_ZOOM
        mini?.flyTo(opts)
    }

    /** The currently-centred location — what will actually be generated. */
    fun confirmCenter(): Pair<Double, Double>? {
        val c = mini?.getCenter() ?: return null
        return (c.lng as Double) to (c.lat as Double)
    }

    private fun updateBox() {
        val m = mini ?: return
        val c = m.getCenter()
        val source = m.getSource("areaBox") ?: return
        source.setData(areaBox(c.lng as Double, c.lat as Double))
    }

    /** A GeoJSON rectangle of the sim's play area (metres → degrees) centred at [lng]/[lat]. */
    private fun areaBox(lng: Double, lat: Double): dynamic {
        val mpp = METERS_PER_PIXEL_Z0 * cos(lat * PI / 180.0) / 2.0.pow(ANCHOR_ZOOM)
        val dLat = (Sim.height / 2.0 * mpp) / METERS_PER_DEG_LAT
        val dLng = (Sim.width / 2.0 * mpp) / (METERS_PER_DEG_LAT * cos(lat * PI / 180.0))
        val ring = "[[${lng - dLng},${lat - dLat}],[${lng + dLng},${lat - dLat}]," +
            "[${lng + dLng},${lat + dLat}],[${lng - dLng},${lat + dLat}],[${lng - dLng},${lat - dLat}]]"
        return JSON.parse<Json>("""{"type":"Feature","geometry":{"type":"Polygon","coordinates":[$ring]}}""")
    }

    private fun toggle() {
        globe = !globe
        applyProjection()
        toggleBtn?.textContent = if (globe) "FLAT" else "GLOBE"
    }

    private fun applyProjection() {
        val proj: dynamic = js("({})")
        proj.type = if (globe) "globe" else "mercator"
        mini?.setProjection(proj)
    }
}
