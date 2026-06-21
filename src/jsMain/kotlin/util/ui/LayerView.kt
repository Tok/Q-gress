package util.ui

import kotlinx.browser.document
import kotlinx.dom.addClass
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.get
import util.MapUtil

/**
 * Base-map view dropdown, split out of [util.HtmlUtil] (size limit). Grayscale satellite is the
 * default view; "Colored" reveals the satellite's real hues. (The old "Street" view was dropped —
 * its flat street tiles showed no buildings/portals, so it was useless.) Only the terrain is
 * recoloured — the 3D portals/agents render in a separate layer and stay faction-coloured.
 */
object LayerView {
    private const val DROPDOWN_ID = "layerSelect"
    private const val SATELLITE = "Satellite" // grayscale satellite (default)
    private const val COLORED = "Colored"

    fun createDropdown(): HTMLSelectElement {
        val select = document.createElement("select") as HTMLSelectElement
        select.id = DROPDOWN_ID
        select.addClass("topDrop", "amarillo")
        listOf(SATELLITE, COLORED).forEach { layer ->
            val opt = document.createElement("option") as HTMLOptionElement
            opt.text = layer
            opt.value = layer
            select.appendChild(opt)
        }
        select.onchange = { apply() }
        return select
    }

    /** Apply the currently-selected view to the maps (also the startup default = grayscale satellite). */
    fun apply() {
        MapUtil.showSatellite()
        MapUtil.setGrayscale(selected() != COLORED)
    }

    private fun selected(): String {
        val dropdown = document.getElementById(DROPDOWN_ID) as? HTMLSelectElement ?: return SATELLITE
        return dropdown[dropdown.selectedIndex]?.let { (it as HTMLOptionElement).value } ?: SATELLITE
    }
}
