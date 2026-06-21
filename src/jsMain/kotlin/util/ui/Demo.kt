package util.ui

import agent.Faction
import kotlinx.browser.document
import kotlinx.dom.addClass
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLLabelElement
import system.display.Scene3D
import util.MapUtil
import util.data.Pos

/**
 * Demo harness (hash-routed, separate from the game): a menu of demo scenes for triggering and
 * tuning effects. `#demo` shows the menu; `#demo/<scene>` runs a scene (the 3D bootstrap is in
 * HtmlUtil.loadDemoScene). Add a scene by listing it in [SCENES] + handling it in [showControls].
 */
object Demo {
    const val MENU = "menu"
    private const val NEUTRAL = "#bbbbbb"
    private const val PANEL_ID = "demoPanel"

    // route → display title. Add future scenes here (building styles, field shaders, NPC tuning…).
    private val SCENES = listOf(
        "portal" to "Portals & Shatter",
        "xmp" to "XMP Effects",
    )

    private var portalColor = Faction.ENL.color
    private var demoLevel = 1
    private var xmpLevelSel = 1
    private var xmpButtons: List<HTMLButtonElement> = emptyList()
    private var portalButtons: List<HTMLButtonElement> = emptyList()

    /** The XMP level the user has selected (the xmp demo detonates this on click). */
    fun xmpLevel(): Int = xmpLevelSel

    /** The level + colour the portal demo places on LMB (HtmlUtil wires the map clicks). */
    fun portalLevel(): Int = demoLevel
    fun portalColorValue(): String = portalColor

    /** The demo route for a location hash, or null for the normal game. Accepts #demo and #/demo. */
    fun route(hash: String): String? = when (hash.removePrefix("#").removePrefix("/").removeSuffix("/")) {
        "demo" -> MENU
        "demo/portal" -> "portal"
        "demo/xmp" -> "xmp"
        else -> null
    }

    fun showMenu() {
        val menu = document.createElement("div") as HTMLDivElement
        menu.addClass("demoMenu", "coda")
        menu.append(titleEl("Demo Scenes"))
        SCENES.forEach { (route, title) -> menu.append(link("#demo/$route", title, "demoLink amarillo")) }
        menu.append(link("#", "← Back to game", "demoBack"))
        document.body?.append(menu)
    }

    fun showControls(scene: String, center: Pos) {
        val panel = document.createElement("div") as HTMLDivElement
        panel.id = PANEL_ID
        panel.addClass("demoPanel", "coda")
        when (scene) {
            "portal" -> buildPortalControls(panel, center)
            "xmp" -> buildXmpControls(panel)
        }
        panel.append(satelliteToggle()) // gray backdrop by default; opt in to satellite
        panel.append(link("#demo", "≡ Menu", "demoBack"))
        document.body?.append(panel)
    }

    /** A "Satellite" checkbox (on by default) — untick to render over a gray backdrop instead. */
    private fun satelliteToggle(): HTMLLabelElement {
        val label = document.createElement("label") as HTMLLabelElement
        label.addClass("demoToggle", "coda")
        val check = document.createElement("input") as HTMLInputElement
        check.type = "checkbox"
        check.checked = true
        MapUtil.setDemoSatellite(true) // satellite on by default
        check.onchange = {
            MapUtil.setDemoSatellite(check.checked)
            null
        }
        label.append(check)
        label.append(document.createTextNode(" Satellite"))
        return label
    }

    private fun buildPortalControls(panel: HTMLDivElement, center: Pos) {
        panel.append(titleEl("Portal — LMB place · RMB shatter"))
        val lvlButtons = (1..8).map { level -> button("L$level", "demoButton") { selectPortalLevel(level) } }
        lvlButtons.forEach { panel.append(it) }
        portalButtons = lvlButtons
        selectPortalLevel(demoLevel) // highlight the current selection
        panel.append(button("ENL", "demoButton enl") { portalColor = Faction.ENL.color })
        panel.append(button("RES", "demoButton res") { portalColor = Faction.RES.color })
        panel.append(button("Neutral", "demoButton") { portalColor = NEUTRAL })
        Scene3D.placeShowcase(center, demoLevel, portalColor) // a starter portal in the middle
    }

    private fun selectPortalLevel(level: Int) {
        demoLevel = level
        portalButtons.forEachIndexed { i, b -> b.className = if (i + 1 == level) "demoButton demoSel" else "demoButton" }
    }

    private fun buildXmpControls(panel: HTMLDivElement) {
        panel.append(titleEl("XMP — click map to detonate"))
        val btns = (1..8).map { level -> button("L$level", "demoButton") { selectXmp(level) } }
        btns.forEach { panel.append(it) }
        xmpButtons = btns
        selectXmp(xmpLevelSel) // highlight the current selection
    }

    private fun selectXmp(level: Int) {
        xmpLevelSel = level
        xmpButtons.forEachIndexed { i, b -> b.className = if (i + 1 == level) "demoButton demoSel" else "demoButton" }
    }

    private fun titleEl(text: String): HTMLDivElement {
        val div = document.createElement("div") as HTMLDivElement
        div.addClass("demoPanelTitle", "amarillo")
        div.innerHTML = text
        return div
    }

    private fun button(label: String, cssClass: String, onClick: () -> Unit): HTMLButtonElement {
        val element = document.createElement("button") as HTMLButtonElement
        element.className = cssClass
        element.innerHTML = label
        element.onclick = {
            onClick()
            null
        }
        return element
    }

    private fun link(href: String, label: String, cssClass: String): HTMLAnchorElement {
        val element = document.createElement("a") as HTMLAnchorElement
        element.href = href
        element.className = cssClass
        element.innerHTML = label
        return element
    }
}
