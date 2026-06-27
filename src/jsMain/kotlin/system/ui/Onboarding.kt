package system.ui

import agent.Faction
import config.Config
import config.Locations
import config.Sim
import config.StartStage
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
import system.audio.Sound
import system.display.TitleWordmark
import kotlin.math.abs

/**
 * Pre-load onboarding screens, shown in order before any map loads: **faction → map-size →
 * location**, chained in-memory (no reloads) by `HtmlUtil.runOnboarding`. The location step is last
 * so its play-area box reflects the already-chosen map size; it previews on the globe ([MiniMap]).
 */
object Onboarding {
    private const val SCREEN_ID = "onboard"
    private const val NAME_KEEP_EPS = 0.05 // ~5 km: keep the picked place's name within this of the confirmed centre

    /** Step 1 — pick a faction. [onPick] receives it. */
    fun showFaction(onPick: (Faction) -> Unit) {
        currentBack = null // first step — nothing to go back to
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
                // Keep the title sim running behind the rest of onboarding (shaded), but pop the 3D
                // wordmark out (it'd sit under the selection pane) and muffle the audio.
                TitleWordmark.setVisible(false)
                Sound.setMuffled(true)
                onPick(f)
            }
            row.appendChild(btn)
        }
        cta.appendChild(row)
        cta.style.opacity = "0" // fade the faction menu in ~1s after the title letters are visible
        cta.style.transition = "opacity 0.7s ease"
        screen.appendChild(cta)
        screen.appendChild(createGithubLink()) // thin "source on GitHub" footer link
        screen.appendChild(createTitleCredit()) // © credit next to the GitHub link
        document.body?.appendChild(screen)
        val revealCta = { cta.style.opacity = "1" } // idempotent
        if (TitleWordmark.isLoaded()) {
            // Returning to the title (Esc back): the 3D letters already exist + the sim is still running —
            // keep the flat 2D brand hidden, re-show the 3D wordmark, and reveal the menu right away.
            brand.style.display = "none"
            TitleWordmark.setVisible(true)
            revealCta()
        } else {
            TitleSim.onTitleReady = { window.setTimeout({ revealCta() }, CTA_DELAY_MS) } // 1s after the letters land
            TitleSim.start() // the real Scene3D demo (portals hacking/XMPing/linking) behind the menu
            window.setTimeout({ revealCta() }, CTA_FALLBACK_MS) // safety net if the wordmark never loads
        }
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

    private const val CREDIT_START_YEAR = 2018 // first commit; end year is the live current year (see below)

    /** Thin "© Zirteq 2018–<current year>" credit in the title footer, beside the GitHub link. */
    private fun createTitleCredit(): HTMLElement {
        val credit = div("titleCredit")
        val now = kotlin.js.Date().getFullYear()
        credit.textContent = "© Zirteq $CREDIT_START_YEAR–$now"
        return credit
    }

    /**
     * Step 3 — location. Three modes: **Home** (Geolocation), **Random** (default; re-rolls from the
     * preset list), **Select** (reveals the preset list). The globe flies to each new choice; the
     * player can pan/zoom to fine-tune, then confirms the play-area box. [onStart] gets lng, lat, name.
     */
    fun showLocation(onBack: () -> Unit, onStart: (Double, Double, String) -> Unit) {
        currentBack = onBack // Esc → back to map size
        val screen = screen("LOCATION SETUP")
        var currentName = ""
        // The coords of the currently-named selection. The user pans/zooms to fine-tune the play-area box
        // AFTER picking a place, so the confirmed centre can drift from it — if it drifts far, the name no
        // longer describes the spot (the "Brandenburg Gate but actually elsewhere" bug), so we drop it.
        var selectedLng = 0.0
        var selectedLat = 0.0

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
            selectedLng = lng
            selectedLat = lat
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

        val confirm = button("Confirm location →", "topButton displayFont onboardStart") {
            MiniMap.confirmCenter()?.let { (lng, lat) ->
                // Keep the selected name only if the confirmed centre is still near it; a preset that the
                // centre snapped onto wins; otherwise it's a custom spot — don't mislabel it.
                val kept = abs(lng - selectedLng) < NAME_KEEP_EPS && abs(lat - selectedLat) < NAME_KEEP_EPS
                val name = Locations.byCoords(lng, lat)?.displayName ?: if (kept) currentName else "Custom location"
                onStart(lng, lat, name)
            }
        }
        screen.appendChild(navRow(onBack, confirm))

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

    /**
     * Step 2 — **who plays?** Pick a brain per side (Human / Heuristic / Neural net / LLM), defaulting to
     * **net vs net** (AI vs AI). The picks are stashed in [DriverControls] so the start-URL carries them
     * across the reload; the live toolbar pickers then install them. [onNext] continues to map size.
     */
    fun showDrivers(userFaction: Faction, onBack: () -> Unit, onNext: () -> Unit) {
        currentBack = onBack // Esc → back to faction pick
        val screen = screen("TEAMS SETUP")
        val intro = div("onboardWarn")
        intro.textContent = "Pick a brain for each side — the default is neural net vs neural net (AI vs AI). " +
            "Set your side to Human to drive it yourself with the tuning sliders."
        screen.appendChild(intro)
        // LLM is hidden by default so new players don't pick the heavy/experimental driver by accident; an
        // opt-in checkbox (below the grid, just above Next) reveals it and rebuilds the grid.
        val holder = div("driverGridHolder")
        var showLlm = false
        fun rebuild() {
            holder.innerHTML = ""
            holder.appendChild(driverGrid(userFaction, showLlm))
        }
        screen.appendChild(holder)
        rebuild()
        screen.appendChild(
            experimentalToggle { on ->
                showLlm = on
                DriverControls.setExperimentalLlm(on) // unlock it in-game too (rides the start URL as ?exp)
                rebuild()
            },
        )
        screen.appendChild(navRow(onBack, button("Next →", "topButton displayFont onboardStart") { onNext() }))
    }

    // Opt-in for the experimental in-browser LLM driver — off by default so it isn't an obvious pick.
    private fun experimentalToggle(onChange: (Boolean) -> Unit): HTMLElement {
        val row = div("driverExperimental")
        val label = document.createElement("label") as HTMLElement
        label.addClass("driverExperimentalLabel")
        val box = document.createElement("input") as HTMLInputElement
        box.type = "checkbox"
        box.checked = false
        box.onchange = { onChange(box.checked) }
        label.appendChild(box)
        label.appendChild(div("driverExperimentalText").also { it.textContent = "Show experimental LLM driver" })
        row.appendChild(label)
        row.appendChild(
            div("driverExperimentalNote").also {
                it.textContent = "In-browser LLM — experimental. Needs WebGPU (may require experimental Chrome flags, " +
                    "e.g. enable-unsafe-webgpu) and a capable GPU; it can be slow or unstable."
            },
        )
        return row
    }

    // The driver picker as an aligned grid: a coloured faction label per row, then a column per option
    // (Human / Heuristic / Neural net [/ LLM]). The opponent's Human cell is left empty so columns line up.
    private fun driverGrid(userFaction: Faction, showLlm: Boolean): HTMLElement {
        val grid = div("driverGrid")
        val options = buildList {
            add("manual" to "Human")
            add("heuristic" to "Heuristic")
            add("net" to "Neural net")
            if (showLlm) add("llm" to "LLM")
        }
        grid.style.setProperty("grid-template-columns", "auto repeat(${options.size}, minmax(108px, 1fr))") // visible count
        listOf(userFaction to true, userFaction.enemy() to false).forEach { (faction, isYou) ->
            // If LLM was picked then hidden again, don't let that stale pick ride the start URL.
            if (!showLlm && DriverControls.chosen(faction) == "llm") DriverControls.select(faction, DriverControls.DEFAULT)
            grid.appendChild(
                div("driverLabel").also {
                    it.textContent = (if (isYou) "You · " else "Against · ") + faction.abbr
                    it.style.color = faction.color
                },
            )
            val current = DriverControls.chosen(faction)
            DriverControls.select(faction, current) // seed the pick so it rides the URL even with no click
            val btns = mutableListOf<HTMLButtonElement>()
            options.forEach { (value, text) ->
                // Only YOUR side offers Human (manual sliders) — no enemy slider UI, so the opponent is AI-only.
                if (value == "manual" && !isYou) {
                    grid.appendChild(div("driverEmpty")) // keep the column aligned under "Human"
                    return@forEach
                }
                lateinit var btn: HTMLButtonElement
                btn = button(text, "onboardPreset driverBtn") {
                    DriverControls.select(faction, value)
                    btns.forEach { it.removeClass("onboardActive") }
                    btn.addClass("onboardActive")
                }
                if (value == current) btn.addClass("onboardActive")
                btns.add(btn)
                grid.appendChild(btn)
            }
        }
        return grid
    }

    /** Step 3 — map size + portal density + start stage. [onStart] gets w, h, portals, startStage.
     *  (NPC population isn't a player choice — it's auto-derived from map size + location; see Config.) */
    fun showMapSize(defaultPortals: Int, onBack: () -> Unit, onStart: (Int, Int, Int, StartStage) -> Unit) {
        currentBack = onBack // Esc → back to faction pick
        val screen = screen("MAP SETUP")
        val widthInput = numberInput(Sim.width)
        val heightInput = numberInput(Sim.height)
        val portalsInput = numberInput(defaultPortals)

        val presets = div("onboardRow")
        val presetBtns = mutableListOf<HTMLButtonElement>()
        val applyPreset = mutableListOf<() -> Unit>()
        // Portal count scales with map size (Small 5 · Normal 8 · Large 13); people scale automatically.
        listOf(
            Triple("Small", Sim.SMALL_SCALE, 5),
            Triple("Normal", Sim.NORMAL_SCALE, 8),
            Triple("Large", Sim.LARGE_SCALE, 13),
        ).forEach { (label, sc, portals) ->
            lateinit var btn: HTMLButtonElement
            val apply: () -> Unit = {
                widthInput.value = Sim.presetWidth(sc).toString()
                heightInput.value = Sim.presetHeight(sc).toString()
                portalsInput.value = portals.toString()
                presetBtns.forEach { it.removeClass("onboardActive") }
                btn.addClass("onboardActive")
            }
            btn = button(label, "onboardPreset") { apply() }
            presetBtns.add(btn)
            applyPreset.add(apply)
            presets.appendChild(btn)
        }
        screen.appendChild(presets)
        // Default to Small: actually APPLY its size/portals to the inputs (not just highlight it) so hitting
        // Next without touching a preset uses Small (the lightest/fastest default).
        applyPreset.getOrNull(0)?.invoke()

        val fields = div("onboardRow")
        fields.appendChild(labeledInput("Width", widthInput))
        fields.appendChild(labeledInput("Height", heightInput))
        fields.appendChild(labeledInput("Portals", portalsInput))
        screen.appendChild(fields)

        val npcSlider = npcDensityRow(screen) // ×1.0–×3.0 multiplier on the auto-derived population

        val stagePick = stageRow(screen, Config.startStage)
        val roundCheck = checkRow(screen, "Round field (play inside an inscribed ellipse)", Sim.roundField)

        val warn = div("onboardWarn")
        warn.textContent = "Larger maps take longer to generate and use more processing whenever portals spawn."
        screen.appendChild(warn)

        val next = button("Next →", "topButton displayFont onboardStart") {
            Sim.roundField = roundCheck.checked
            Config.npcMultiplier = npcSlider.valueAsNumber // carried into the game via the reload URL
            val w = widthInput.value.toIntOrNull() ?: Sim.width
            val h = heightInput.value.toIntOrNull() ?: Sim.height
            // Round → square the map so the inscribed circle is large (≈ doubles the radius vs a
            // wide rectangle) and the round arena actually fills the space.
            val side = maxOf(w, h)
            onStart(
                if (roundCheck.checked) side else w,
                if (roundCheck.checked) side else h,
                portalsInput.value.toIntOrNull() ?: defaultPortals,
                stagePick(),
            )
        }
        screen.appendChild(navRow(onBack, next))
    }

    // The wizard footer: a [← Back | <forward> →] row. Back invokes the screen's onBack (same as Esc).
    private fun navRow(onBack: () -> Unit, forward: HTMLButtonElement): HTMLElement {
        val row = div("onboardNav")
        row.appendChild(button("← Back", "topButton displayFont onboardBack") { onBack() })
        row.appendChild(forward)
        return row
    }

    private fun button(label: String, classes: String, onClick: () -> Unit): HTMLButtonElement {
        val b = document.createElement("button") as HTMLButtonElement
        classes.split(" ").forEach { b.addClass(it) }
        b.textContent = label
        b.onclick = { onClick() }
        return b
    }

    /** NPC-density row (label + ×0.1–×3.0 slider) appended to [screen]; returns the slider input. */
    private fun npcDensityRow(screen: HTMLElement): HTMLInputElement {
        val row = div("onboardRow")
        val label = div("onboardSliderLabel")
        fun text(v: Double) = "NPCs ×${(v * 10).toInt() / 10.0}"
        label.textContent = text(Config.npcMultiplier)
        val slider = document.createElement("input") as HTMLInputElement
        slider.type = "range"
        slider.min = "0.1" // allow thinning the crowd (×0.1, ×0.5 …), not just boosting it; floored at 30 NPCs
        slider.max = "3.0"
        slider.step = "0.1"
        slider.value = Config.npcMultiplier.toString()
        slider.addClass("slider")
        slider.oninput = {
            label.textContent = text(slider.valueAsNumber)
            null
        }
        row.appendChild(label)
        row.appendChild(slider)
        screen.appendChild(row)
        return slider
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

    /** A 3-button start-stage selector [Start | Mid | End] appended to [screen]; returns a getter for the
     *  current pick (default [initial]). Start = lean cold open, Mid = a game in motion, End = full late game. */
    private fun stageRow(screen: HTMLElement, initial: StartStage): () -> StartStage {
        var selected = initial
        val row = div("onboardRow")
        row.appendChild(div("onboardStageLabel").also { it.textContent = "Start at" })
        val btns = mutableListOf<HTMLButtonElement>()
        listOf(StartStage.START to "Start", StartStage.MID to "Mid", StartStage.END to "End").forEach { (stage, text) ->
            lateinit var btn: HTMLButtonElement
            btn = button(text, "onboardPreset") {
                selected = stage
                btns.forEach { it.removeClass("onboardActive") }
                btn.addClass("onboardActive")
            }
            if (stage == initial) btn.addClass("onboardActive")
            btns.add(btn)
            row.appendChild(btn)
        }
        screen.appendChild(row)
        return { selected }
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
        currentBack = null
        document.getElementById(SCREEN_ID)?.remove()
    }

    // Where Esc goes from the current step (null on the first step). Set by each show* below.
    private var currentBack: (() -> Unit)? = null

    /** True while an onboarding screen is up (so Esc can mean "go back" instead of "close a panel"). */
    fun isShowing() = document.getElementById(SCREEN_ID) != null

    /** Step back one onboarding screen (Esc); no-op on the first step. */
    fun back() {
        currentBack?.invoke()
    }

    // Builds the full-screen shaded overlay (the title sim shows through, dimmed) + a glass panel, and
    // appends the overlay to the body. Returns the PANEL — callers add their controls into it.
    private fun screen(titleText: String): HTMLElement {
        document.getElementById(SCREEN_ID)?.remove()
        val s = div("onboardScreen")
        s.id = SCREEN_ID
        val panel = div("onboardPanel")
        val title = div("onboardTitle")
        title.textContent = titleText
        panel.appendChild(title)
        s.appendChild(panel)
        document.body?.appendChild(s)
        return panel
    }

    private fun div(cls: String): HTMLElement {
        val e = document.createElement("div") as HTMLElement
        e.className = cls
        return e
    }
}
