package util.ui

import agent.Faction
import config.Location
import kotlinx.browser.document
import kotlinx.dom.addClass
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement

/**
 * Pre-load onboarding screens, shown in order before any map loads: **faction** first, then
 * **location**. Each step navigates via the URL (?faction, then ?lng/&lat/&name) so the existing
 * reload-based flow carries the choices; once both are present the world loads (see HtmlUtil.load).
 * The location step previews the picked place on the globe inset ([MiniMap]).
 */
object Onboarding {
    private const val SCREEN_ID = "onboard"

    /** Step 1 — pick a faction. [onPick] should persist it (e.g. navigate with ?faction). */
    fun showFaction(onPick: (Faction) -> Unit) {
        val screen = screen("CHOOSE YOUR FACTION")
        val row = div("onboardRow")
        listOf(Faction.ENL, Faction.RES).forEach { f ->
            val btn = document.createElement("button") as HTMLButtonElement
            btn.addClass(f.abbr.lowercase(), "popupButton", "amarillo")
            btn.textContent = f.abbr
            btn.onclick = { onPick(f) }
            row.appendChild(btn)
        }
        screen.appendChild(row)
        document.body?.appendChild(screen)
    }

    /** Step 2 — pick a location (preset list + globe preview). [onStart] receives the chosen place. */
    fun showLocation(onStart: (Location) -> Unit) {
        val screen = screen("CHOOSE A LOCATION")
        val select = document.createElement("select") as HTMLSelectElement
        select.addClass("topDrop", "amarillo")
        Location.values().forEach { loc ->
            val opt = document.createElement("option") as HTMLOptionElement
            opt.text = loc.displayName
            opt.value = loc.name
            select.appendChild(opt)
        }
        select.onchange = {
            val loc = Location.valueOf(select.value)
            MiniMap.setCenter(loc.lng, loc.lat)
        }
        screen.appendChild(select)

        val mapHolder = div("onboardMap")
        screen.appendChild(mapHolder)

        val start = document.createElement("button") as HTMLButtonElement
        start.addClass("topButton", "amarillo", "onboardStart")
        start.textContent = "Start"
        start.onclick = { onStart(Location.valueOf(select.value)) }
        screen.appendChild(start)

        document.body?.appendChild(screen)
        MiniMap.create(mapHolder, Location.DEFAULT.lng, Location.DEFAULT.lat)
    }

    private fun screen(titleText: String): HTMLElement {
        document.getElementById(SCREEN_ID)?.remove()
        val s = div("onboardScreen")
        s.id = SCREEN_ID
        val title = div("onboardTitle")
        title.textContent = titleText
        s.appendChild(title)
        return s
    }

    private fun div(cls: String): HTMLElement {
        val e = document.createElement("div") as HTMLElement
        e.className = cls
        return e
    }
}
