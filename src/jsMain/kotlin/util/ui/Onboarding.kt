package util.ui

import agent.Faction
import config.Config
import config.Locations
import config.Sim
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import util.SoundUtil

/**
 * Pre-load onboarding screens, shown in order before any map loads: **faction → map-size →
 * location**, chained in-memory (no reloads) by `HtmlUtil.runOnboarding`. The location step is last
 * so its play-area box reflects the already-chosen map size; it previews on the globe ([MiniMap]).
 */
object Onboarding {
    private const val SCREEN_ID = "onboard"

    /** Step 1 — pick a faction. [onPick] receives it. */
    fun showFaction(onPick: (Faction) -> Unit) {
        document.getElementById(SCREEN_ID)?.remove()
        val screen = div("onboardScreen")
        screen.id = SCREEN_ID
        screen.addClass("titleScreen") // transparent vignette → the real Scene3D demo shows behind it
        val brand = div("titleBrand") // the big Q-GRESS wordmark — first thing a player sees
        brand.textContent = "Q-GRESS"
        screen.appendChild(brand)
        // The CTA is its own glass pane (a call-to-action, not a subtitle): prompt + the two buttons.
        val cta = div("factionCta")
        val label = div("factionCtaLabel")
        label.textContent = "Choose your faction"
        cta.appendChild(label)
        val row = div("onboardRow")
        listOf(Faction.ENL, Faction.RES).forEach { f ->
            val btn = document.createElement("button") as HTMLButtonElement
            btn.addClass(f.abbr.lowercase(), "popupButton", "displayFont")
            btn.textContent = f.abbr
            btn.onclick = {
                TitleSim.stop() // stop the auto-driver; the scene is wiped by the onboarding reload
                onPick(f)
            }
            row.appendChild(btn)
        }
        cta.appendChild(row)
        cta.style.opacity = "0" // fade the faction menu in ~1s after the title letters are visible
        cta.style.transition = "opacity 0.7s ease"
        screen.appendChild(cta)
        screen.appendChild(createTitleVolume()) // an annoyed player can turn it down right away
        screen.appendChild(createGithubLink()) // thin "source on GitHub" footer link
        document.body?.appendChild(screen)
        val revealCta = { cta.style.opacity = "1" } // idempotent
        TitleSim.onTitleReady = { window.setTimeout({ revealCta() }, CTA_DELAY_MS) } // 1s after the letters land
        TitleSim.start() // the real Scene3D demo (portals hacking/XMPing/linking) behind the menu
        window.setTimeout({ revealCta() }, CTA_FALLBACK_MS) // safety net if the wordmark never loads
    }

    private const val CTA_DELAY_MS = 1000 // faction menu appears this long after the title letters are visible
    private const val CTA_FALLBACK_MS = 5000 // …but show it by now regardless (font load failure etc.)

    private const val GITHUB_ICON =
        "<svg viewBox=\"0 0 16 16\" width=\"14\" height=\"14\" fill=\"currentColor\" aria-hidden=\"true\">" +
            "<path d=\"M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49" +
            "-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82" +
            ".72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15" +
            "-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82" +
            ".44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2" +
            " 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z\"/></svg>"

    private fun createGithubLink(): HTMLElement {
        val a = document.createElement("a") as HTMLAnchorElement
        a.addClass("titleGithub")
        a.href = "https://github.com/Tok/Q-gress"
        a.target = "_blank"
        a.rel = "noopener"
        a.innerHTML = "$GITHUB_ICON<span>Source on GitHub</span>"
        return a
    }

    /** A small volume slider shown on the title screen (the storm starts loud). */
    private fun createTitleVolume(): HTMLElement {
        val wrap = div("titleVolume")
        val label = div("titleVolumeLabel")
        label.textContent = "Volume"
        val slider = document.createElement("input") as HTMLInputElement
        slider.type = "range"
        slider.min = "0.0"
        slider.max = "1.0"
        slider.step = "0.05"
        slider.value = "1.0"
        slider.addClass("slider", "volumeSlider")
        slider.oninput = {
            SoundUtil.setMasterVolume(slider.valueAsNumber)
            null
        }
        wrap.appendChild(label)
        wrap.appendChild(slider)
        return wrap
    }

    /**
     * Step 3 — location. Three modes: **Home** (Geolocation), **Random** (default; re-rolls from the
     * preset list), **Select** (reveals the preset list). The globe flies to each new choice; the
     * player can pan/zoom to fine-tune, then confirms the play-area box. [onStart] gets lng, lat, name.
     */
    fun showLocation(onStart: (Double, Double, String) -> Unit) {
        val screen = screen("CHOOSE A LOCATION")
        var currentName = ""

        val select = document.createElement("select") as HTMLSelectElement
        select.addClass("topDrop", "displayFont", "invisible") // shown only in Select mode
        Locations.all().forEach { loc ->
            val opt = document.createElement("option") as HTMLOptionElement
            opt.text = loc.displayName
            opt.value = loc.name
            select.appendChild(opt)
        }
        fun fly(lng: Double, lat: Double, name: String) {
            currentName = name
            MiniMap.setCenter(lng, lat)
        }
        select.onchange = {
            Locations.byName(select.value)?.let { fly(it.lng, it.lat, it.displayName) }
        }

        val modes = div("onboardRow")
        val homeBtn = modeButton("Home")
        val randomBtn = modeButton("Random")
        val selectBtn = modeButton("Select")
        val all = listOf(homeBtn, randomBtn, selectBtn)
        fun activate(btn: HTMLButtonElement) {
            all.forEach { it.removeClass("onboardActive") }
            btn.addClass("onboardActive")
        }
        fun rollRandom() {
            val loc = Locations.random()
            select.value = loc.name
            fly(loc.lng, loc.lat, loc.displayName)
        }
        homeBtn.onclick = {
            activate(homeBtn)
            select.addClass("invisible")
            useGeolocation { lng, lat -> fly(lng, lat, "Your location") }
        }
        randomBtn.onclick = {
            activate(randomBtn)
            select.addClass("invisible")
            rollRandom()
        }
        selectBtn.onclick = {
            activate(selectBtn)
            select.removeClass("invisible")
        }
        all.forEach { modes.appendChild(it) }
        screen.appendChild(modes)
        screen.appendChild(select)

        val mapHolder = div("onboardMap")
        screen.appendChild(mapHolder)
        val hint = div("onboardHint")
        hint.textContent = "Pan/zoom to position the white play-area box, then confirm."
        screen.appendChild(hint)

        screen.appendChild(
            button("Confirm location", "topButton displayFont onboardStart") {
                MiniMap.confirmCenter()?.let { onStart(it.first, it.second, currentName) }
            },
        )
        document.body?.appendChild(screen)

        // Default mode: Random — open the globe on a random preset.
        val initial = Locations.random()
        select.value = initial.name
        currentName = initial.displayName
        activate(randomBtn)
        MiniMap.create(mapHolder, initial.lng, initial.lat)
    }

    private fun modeButton(label: String): HTMLButtonElement {
        val b = document.createElement("button") as HTMLButtonElement
        b.addClass("onboardPreset")
        b.textContent = label
        return b
    }

    /** Center on the player's location via the Geolocation API (no preset). Falls back with an alert. */
    private fun useGeolocation(onLoc: (Double, Double) -> Unit) {
        if (js("typeof navigator === 'undefined' || !navigator.geolocation").unsafeCast<Boolean>()) {
            window.alert("Geolocation isn't available — pick a location instead.")
            return
        }
        window.asDynamic().navigator.geolocation.getCurrentPosition(
            { pos: dynamic -> onLoc(pos.coords.longitude as Double, pos.coords.latitude as Double) },
            { _: dynamic -> window.alert("Couldn't get your location — pick a location instead.") },
        )
    }

    /** Step 2 — map size + portal density + people + quick-start. [onStart] gets w, h, portals, npc, quickStart. */
    fun showMapSize(defaultPortals: Int, onStart: (Int, Int, Int, Int, Boolean) -> Unit) {
        val screen = screen("MAP SIZE & PORTALS")
        val widthInput = numberInput(Sim.width)
        val heightInput = numberInput(Sim.height)
        val portalsInput = numberInput(defaultPortals)
        val npcInput = numberInput(Config.maxFor())

        val presets = div("onboardRow")
        // Portal + people counts scale with map size (Small 5/250 · Normal 8/500 · Large 13/900).
        val npcFor = mapOf("Small" to 250, "Normal" to 500, "Large" to 900)
        listOf(
            Triple("Small", Sim.SMALL_SCALE, 5),
            Triple("Normal", Sim.NORMAL_SCALE, 8),
            Triple("Large", Sim.LARGE_SCALE, 13),
        ).forEach { (label, sc, portals) ->
            presets.appendChild(
                button(label, "onboardPreset") {
                    widthInput.value = Sim.presetWidth(sc).toString()
                    heightInput.value = Sim.presetHeight(sc).toString()
                    portalsInput.value = portals.toString()
                    npcInput.value = (npcFor[label] ?: 500).toString()
                },
            )
        }
        screen.appendChild(presets)

        val fields = div("onboardRow")
        fields.appendChild(labeledInput("Width", widthInput))
        fields.appendChild(labeledInput("Height", heightInput))
        fields.appendChild(labeledInput("Portals", portalsInput))
        fields.appendChild(labeledInput("People", npcInput))
        screen.appendChild(fields)

        val quickCheck = checkRow(screen, " Quick start (full roster + AP for a fast early game)", true)
        val roundCheck = checkRow(screen, " Round field (play inside an inscribed ellipse)", Sim.roundField)

        val warn = div("onboardWarn")
        warn.textContent = "Larger maps take longer to generate and use more processing whenever portals spawn."
        screen.appendChild(warn)

        screen.appendChild(
            button("Start", "topButton displayFont onboardStart") {
                Sim.roundField = roundCheck.checked
                val w = widthInput.value.toIntOrNull() ?: Sim.width
                val h = heightInput.value.toIntOrNull() ?: Sim.height
                // Round → square the map so the inscribed circle is large (≈ doubles the radius vs a
                // wide rectangle) and the round arena actually fills the space.
                val side = maxOf(w, h)
                onStart(
                    if (roundCheck.checked) side else w,
                    if (roundCheck.checked) side else h,
                    portalsInput.value.toIntOrNull() ?: defaultPortals,
                    npcInput.value.toIntOrNull() ?: Config.maxFor(),
                    quickCheck.checked,
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

    private fun checkRow(screen: HTMLElement, label: String, checked: Boolean): HTMLInputElement {
        val check = document.createElement("input") as HTMLInputElement
        check.type = "checkbox"
        check.checked = checked
        check.addClass("checkbox")
        val row = div("onboardCheck")
        row.appendChild(check)
        row.appendChild(document.createTextNode(label))
        screen.appendChild(row)
        return check
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
