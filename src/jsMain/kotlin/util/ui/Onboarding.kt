package util.ui

import agent.Faction
import config.Location
import config.Sim
import kotlinx.browser.document
import kotlinx.dom.addClass
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
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

    /** Step 3 — map size (width/height, with presets) + portal density. [onStart] gets w, h, portals. */
    fun showMapSize(defaultPortals: Int, onStart: (Int, Int, Int) -> Unit) {
        val screen = screen("MAP SIZE & PORTALS")
        val widthInput = numberInput(Sim.width)
        val heightInput = numberInput(Sim.height)

        val presets = div("onboardRow")
        listOf("Small" to Sim.SMALL_SCALE, "Normal" to Sim.NORMAL_SCALE, "Large" to Sim.LARGE_SCALE).forEach { (label, sc) ->
            presets.appendChild(
                button(label, "onboardPreset") {
                    widthInput.value = Sim.presetWidth(sc).toString()
                    heightInput.value = Sim.presetHeight(sc).toString()
                },
            )
        }
        screen.appendChild(presets)

        val fields = div("onboardRow")
        fields.appendChild(labeledInput("Width", widthInput))
        fields.appendChild(labeledInput("Height", heightInput))
        val portalsInput = numberInput(defaultPortals)
        fields.appendChild(labeledInput("Portals", portalsInput))
        screen.appendChild(fields)

        val warn = div("onboardWarn")
        warn.textContent = "Larger maps take longer to generate and use more processing whenever portals spawn."
        screen.appendChild(warn)

        screen.appendChild(
            button("Start", "topButton amarillo onboardStart") {
                onStart(
                    widthInput.value.toIntOrNull() ?: Sim.width,
                    heightInput.value.toIntOrNull() ?: Sim.height,
                    portalsInput.value.toIntOrNull() ?: defaultPortals,
                )
            },
        )
        document.body?.appendChild(screen)
    }

    private fun button(label: String, classes: String, onClick: () -> Unit): HTMLButtonElement {
        val b = document.createElement("button") as HTMLButtonElement
        classes.split(" ").forEach { b.addClass(it) }
        b.textContent = label
        b.onclick = { onClick() }
        return b
    }

    private fun numberInput(value: Int): HTMLInputElement {
        val i = document.createElement("input") as HTMLInputElement
        i.type = "number"
        i.value = value.toString()
        i.addClass("onboardInput", "coda")
        return i
    }

    private fun labeledInput(label: String, input: HTMLInputElement): HTMLElement {
        val wrap = div("onboardField")
        val l = div("onboardFieldLabel")
        l.textContent = label
        wrap.appendChild(l)
        wrap.appendChild(input)
        return wrap
    }

    /** Remove the current onboarding screen (the last step loads the world without a reload). */
    fun close() {
        document.getElementById(SCREEN_ID)?.remove()
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
