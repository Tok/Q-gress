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
    const val SANDBOX = "sandbox"
    private const val NEUTRAL = "#bbbbbb"
    private const val PANEL_ID = "demoPanel"

    private var portalColor = Faction.ENL.color
    private var demoLevel = 8
    private var portalButtons: List<HTMLButtonElement> = emptyList()

    /** The selected level + colour (HtmlUtil wires the map clicks: LMB place · RMB remove). */
    fun portalLevel(): Int = demoLevel
    fun portalColorValue(): String = portalColor

    /** One unified sandbox scene. #demo and #demo/portal both route to it (xmp folded in). */
    fun route(hash: String): String? = when (hash.removePrefix("#").removePrefix("/").removeSuffix("/")) {
        "demo", "demo/portal", "demo/sandbox" -> SANDBOX
        else -> null
    }

    fun showControls(center: Pos) {
        val panel = document.createElement("div") as HTMLDivElement
        panel.id = PANEL_ID
        panel.addClass("demoPanel", "coda")
        buildSandboxControls(panel, center)
        panel.append(satelliteToggle()) // gray backdrop by default; opt in to satellite
        panel.append(link("#", "← Back to game", "demoBack"))
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

    private fun buildSandboxControls(panel: HTMLDivElement, center: Pos) {
        panel.append(titleEl("Portal Sandbox"))
        panel.append(hintEl("LMB place · RMB remove. Buttons act on the last-placed portal."))

        val lvlButtons = (1..8).map { level -> button("L$level", "demoButton") { selectPortalLevel(level) } }
        lvlButtons.forEach { panel.append(it) }
        portalButtons = lvlButtons
        selectPortalLevel(demoLevel)
        panel.append(button("ENL", "demoButton enl") { portalColor = Faction.ENL.color })
        panel.append(button("RES", "demoButton res") { portalColor = Faction.RES.color })
        panel.append(button("Neutral", "demoButton") { portalColor = NEUTRAL })

        // Explicit action buttons for every animation (act on the most recently placed portal).
        panel.append(button("Hack", "demoButton") { Scene3D.hackLastShowcase() })
        val xmp = button("Fire XMP (L$demoLevel)", "demoButton") { Scene3D.xmpAtLastShowcase(demoLevel) }
        xmpButton = xmp
        panel.append(xmp)
        panel.append(button("Upgrade", "demoButton") { Scene3D.stepLastShowcaseLevel(1) })
        panel.append(button("Downgrade", "demoButton") { Scene3D.stepLastShowcaseLevel(-1) })
        panel.append(button("Link", "demoButton") { Scene3D.linkLastShowcases() })

        Scene3D.placeShowcase(center, demoLevel, portalColor) // a starter portal in the middle
    }

    private var xmpButton: HTMLButtonElement? = null

    private fun selectPortalLevel(level: Int) {
        demoLevel = level
        portalButtons.forEachIndexed { i, b -> b.className = if (i + 1 == level) "demoButton demoSel" else "demoButton" }
        xmpButton?.innerHTML = "Fire XMP (L$level)"
    }

    private fun titleEl(text: String): HTMLDivElement {
        val div = document.createElement("div") as HTMLDivElement
        div.addClass("demoPanelTitle", "amarillo")
        div.innerHTML = text
        return div
    }

    private fun hintEl(text: String): HTMLDivElement {
        val div = document.createElement("div") as HTMLDivElement
        div.addClass("demoHint", "coda")
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
