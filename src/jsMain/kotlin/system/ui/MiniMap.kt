package system.ui

import config.Sim
import external.MapLibre
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import kotlin.js.Json
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

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

    // Dim everything OUTSIDE the play-area box (the box is the hole) so the fixed playable region reads
    // clearly at any zoom: panning/zooming the map never makes the playable area look bigger or smaller —
    // it's always the one bright window in a darkened world.
    private const val MASK_LAYER = """{
        "id": "areaMaskFill", "type": "fill", "source": "areaMask",
        "paint": { "fill-color": "#000000", "fill-opacity": 0.55 }
    }"""

    // A world-spanning ring (web-mercator latitude limits); the play-area ring is punched out as a hole.
    private const val WORLD_RING = "[[-180,-85],[180,-85],[180,85],[-180,85],[-180,-85]]"

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
            val maskSrc: dynamic = js("({})")
            maskSrc.type = "geojson"
            maskSrc.data = areaMask(lng, lat)
            m.addSource("areaMask", maskSrc)
            m.addLayer(JSON.parse<Json>(MASK_LAYER)) // dim outside first, so the box line sits on top
            val src: dynamic = js("({})")
            src.type = "geojson"
            src.data = areaBox(lng, lat)
            m.addSource("areaBox", src)
            m.addLayer(JSON.parse<Json>(BOX_LAYER))
        }
        m.on("move") { updateBox() } // keep the play-area box + mask centred on the current view
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
        // jumpTo, not flyTo: confirmCenter() reads getCenter() immediately, so a long animated fly that
        // hadn't finished yet would confirm a mid-flight (wrong) location for world-gen.
        mini?.asDynamic()?.jumpTo(opts)
    }

    /** The currently-centred location — what will actually be generated. */
    fun confirmCenter(): Pair<Double, Double>? {
        val c = mini?.getCenter() ?: return null
        return (c.lng as Double) to (c.lat as Double)
    }

    /** Fired on every pan/zoom with the current centre — lets the onboarding readout track the live location. */
    var onMove: ((Double, Double) -> Unit)? = null

    private fun updateBox() {
        val m = mini ?: return
        val c = m.getCenter()
        val lng = c.lng as Double
        val lat = c.lat as Double
        m.getSource("areaBox")?.setData(areaBox(lng, lat))
        m.getSource("areaMask")?.setData(areaMask(lng, lat))
        onMove?.invoke(lng, lat)
    }

    /** A GeoJSON outline of the sim's play area (metres → degrees) centred at [lng]/[lat] — a
     *  rectangle, or a circle when the round field is selected (so the confirm screen matches). */
    private fun areaBox(lng: Double, lat: Double): dynamic =
        JSON.parse<Json>("""{"type":"Feature","geometry":{"type":"Polygon","coordinates":[${boxRing(lng, lat)}]}}""")

    /** The world, with the play-area [boxRing] punched out as a hole — fills the dimming mask. */
    private fun areaMask(lng: Double, lat: Double): dynamic =
        JSON.parse<Json>("""{"type":"Feature","geometry":{"type":"Polygon","coordinates":[$WORLD_RING,${boxRing(lng, lat)}]}}""")

    /** The play-area ring (a single GeoJSON linear ring) centred at [lng]/[lat]: a circle when the round
     *  field is selected, else the sim rectangle. Shared by the box outline and the dimming mask's hole. */
    private fun boxRing(lng: Double, lat: Double): String {
        val mpp = METERS_PER_PIXEL_Z0 * cos(lat * PI / 180.0) / 2.0.pow(ANCHOR_ZOOM)
        val degLng = METERS_PER_DEG_LAT * cos(lat * PI / 180.0)
        return if (Sim.roundField) {
            val rLat = Sim.fieldRadius() * mpp / METERS_PER_DEG_LAT
            val rLng = Sim.fieldRadius() * mpp / degLng
            val sb = StringBuilder()
            for (i in 0..CIRCLE_SEGMENTS) {
                val t = i.toDouble() / CIRCLE_SEGMENTS * 2.0 * PI
                if (i > 0) sb.append(",")
                sb.append("[${lng + rLng * cos(t)},${lat + rLat * sin(t)}]")
            }
            "[$sb]"
        } else {
            val dLat = (Sim.height / 2.0 * mpp) / METERS_PER_DEG_LAT
            val dLng = (Sim.width / 2.0 * mpp) / degLng
            "[[${lng - dLng},${lat - dLat}],[${lng + dLng},${lat - dLat}]," +
                "[${lng + dLng},${lat + dLat}],[${lng - dLng},${lat + dLat}],[${lng - dLng},${lat - dLat}]]"
        }
    }

    private const val CIRCLE_SEGMENTS = 48

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
