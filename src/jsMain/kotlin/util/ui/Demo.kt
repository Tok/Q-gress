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

    private var portalColor = NEUTRAL // the demo has no faction preference — defaults to Neutral
    private var demoLevel = 8
    private var xmpClickMode = false // when on, LMB detonates an XMP at the click point (vs place/select)
    private var xmpBlastLevel = 8
    private var portalButtons: List<HTMLButtonElement> = emptyList()
    private var factionButtons: List<HTMLButtonElement> = emptyList()
    private var xmpButtons: List<HTMLButtonElement> = emptyList()
    private val factionColors = listOf(Faction.ENL.color, Faction.RES.color, NEUTRAL)
    private val factionBaseClass = listOf("demoButton enl", "demoButton res", "demoButton")

    /** The selected portal level + colour the map uses when placing (LMB). */
    fun portalLevel(): Int = demoLevel
    fun portalColorValue(): String = portalColor

    /** XMP-on-click mode + the blast level a map click detonates (see the demo LMB handler). */
    fun xmpOnClick(): Boolean = xmpClickMode
    fun xmpLevel(): Int = xmpBlastLevel

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
        panel.append(hintEl("Map: LMB place / select a portal · RMB shatter it. The buttons below act on the selected portal."))

        panel.append(labelEl("Place level"))
        val lvlButtons = (1..8).map { level -> button("L$level", "demoButton demoMini") { selectPortalLevel(level) } }
        panel.append(rowOf(lvlButtons))
        portalButtons = lvlButtons
        selectPortalLevel(demoLevel)
        // Faction tint of placed portals (also sets the hack spin direction). Defaults to Neutral.
        factionButtons = listOf(
            button("ENL", factionBaseClass[0]) { selectFaction(0) },
            button("RES", factionBaseClass[1]) { selectFaction(1) },
            button("Neutral", factionBaseClass[2]) { selectFaction(2) },
        )
        panel.append(rowOf(factionButtons))
        selectFaction(2)

        panel.append(labelEl("Selected portal"))
        panel.append(
            rowOf(
                listOf(
                    button("Upgrade", "demoButton") { Scene3D.stepLastShowcaseLevel(1) },
                    button("Downgrade", "demoButton") { Scene3D.stepLastShowcaseLevel(-1) },
                    button("Link", "demoButton") { Scene3D.linkLastShowcases() },
                    button("Hack", "demoButton") { Scene3D.hackActiveShowcase(false) },
                    button("Glyph", "demoButton") { Scene3D.hackActiveShowcase(true) },
                ),
            ),
        )

        panel.append(labelEl("XMP"))
        panel.append(hintEl("Pick a blast level (fires at the selected portal). Toggle on to detonate at the click point instead."))
        panel.append(xmpClickToggle())
        val xmpRow = (1..8).map { level ->
            button("X$level", "demoButton demoMini") {
                selectXmpLevel(level)
                Scene3D.xmpActiveShowcase(level)
            }
        }
        panel.append(rowOf(xmpRow))
        xmpButtons = xmpRow
        selectXmpLevel(xmpBlastLevel)

        Scene3D.placeShowcase(center, demoLevel, portalColor) // a starter portal in the middle
    }

    /** Lay buttons out in a horizontal wrapping row (compact — for the L1-8 / X1-8 selectors). */
    private fun rowOf(buttons: List<HTMLButtonElement>): HTMLDivElement {
        val row = document.createElement("div") as HTMLDivElement
        row.addClass("demoRow")
        buttons.forEach { row.append(it) }
        return row
    }

    private fun selectPortalLevel(level: Int) {
        demoLevel = level
        portalButtons.forEachIndexed { i, b ->
            b.className = if (i + 1 == level) "demoButton demoMini demoSel" else "demoButton demoMini"
        }
    }

    private fun selectXmpLevel(level: Int) {
        xmpBlastLevel = level
        xmpButtons.forEachIndexed { i, b ->
            b.className = if (i + 1 == level) "demoButton demoMini demoSel" else "demoButton demoMini"
        }
    }

    /** A "Fire XMP on click" checkbox — when ticked, an LMB on the map detonates an XMP at that point. */
    private fun xmpClickToggle(): HTMLLabelElement {
        val label = document.createElement("label") as HTMLLabelElement
        label.addClass("demoToggle", "coda")
        val check = document.createElement("input") as HTMLInputElement
        check.type = "checkbox"
        check.checked = xmpClickMode
        check.onchange = {
            xmpClickMode = check.checked
            null
        }
        label.append(check)
        label.append(document.createTextNode(" Fire XMP on click"))
        return label
    }

    private fun selectFaction(idx: Int) {
        portalColor = factionColors[idx]
        factionButtons.forEachIndexed { i, b ->
            b.className = if (i == idx) "${factionBaseClass[i]} demoSel" else factionBaseClass[i]
        }
    }

    private fun titleEl(text: String): HTMLDivElement {
        val div = document.createElement("div") as HTMLDivElement
        div.addClass("demoPanelTitle", "amarillo")
        div.innerHTML = text
        return div
    }

    /** A full-width section label that breaks the button row (e.g. "Fire XMP"). */
    private fun labelEl(text: String): HTMLDivElement {
        val div = document.createElement("div") as HTMLDivElement
        div.addClass("demoLabel", "coda")
        div.innerHTML = text
        return div
    }

    /** A full-width instructional line. */
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
