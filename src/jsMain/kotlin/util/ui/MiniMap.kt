package util.ui

import external.MapLibre
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import kotlin.js.Json

/**
 * A small overview inset in the corner: a second MapLibre map rendered as a **globe** (MapLibre 5's
 * globe projection), synced to the main camera so the played location always sits at the inset's
 * centre — hence a single static pin marks "you are here" without any per-frame projection. A small
 * button toggles the inset between globe and a flat (mercator) overview.
 *
 * The inset map is non-interactive (`interactive:false`); it is a passive overview, not a control.
 */
object MiniMap {
    private const val CONTAINER_ID = "miniMap"
    private const val MINI_ZOOM = 1.4 // low enough that the globe reads as a sphere at inset size
    private const val ESRI_TILES =
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"

    // Bare satellite raster — reads well on a globe and needs no glyphs/sprites.
    private val STYLE = """{
        "version": 8,
        "sources": { "satellite": { "type": "raster", "tiles": ["$ESRI_TILES"], "tileSize": 256 } },
        "layers": [{ "id": "satellite", "type": "raster", "source": "satellite" }]
    }"""

    private var mini: MapLibre.Map? = null
    private var toggleBtn: HTMLElement? = null
    private var globe = true

    /**
     * Build the globe inset inside [parent], centred at [lng]/[lat] — a standalone location preview
     * for the onboarding location screen (recenter with [setCenter]). No-op if already created.
     */
    fun create(parent: HTMLElement, lng: Double, lat: Double) {
        if (mini != null) return

        // Outer wrapper carries our fixed/circular styling; the map mounts in an inner div so
        // MapLibre's own `maplibregl-map { position: relative }` lands there, not on the wrapper
        // (its stylesheet loads after ours and would otherwise win on equal specificity).
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
        opts.interactive = false // passive preview — never grabs gestures
        opts.attributionControl = false
        opts.center = arrayOf(lng, lat)
        opts.zoom = MINI_ZOOM
        val m = MapLibre.Map(opts)
        m.on("load") { applyProjection() }
        mini = m

        val btn = document.createElement("button") as HTMLElement
        btn.className = "miniMapToggle"
        btn.textContent = "FLAT"
        btn.asDynamic().onclick = { toggle() }
        wrapper.appendChild(btn)
        toggleBtn = btn
    }

    /** Recenter the preview globe (e.g. when the user picks a different location). */
    fun setCenter(lng: Double, lat: Double) {
        mini?.setCenter(arrayOf(lng, lat))
    }

    private fun toggle() {
        globe = !globe
        applyProjection()
        toggleBtn?.textContent = if (globe) "FLAT" else "GLOBE" // label = the projection it switches to
    }

    private fun applyProjection() {
        val proj: dynamic = js("({})")
        proj.type = if (globe) "globe" else "mercator"
        mini?.setProjection(proj)
    }
}
