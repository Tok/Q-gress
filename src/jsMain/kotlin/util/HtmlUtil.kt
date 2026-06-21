package util

import ImprovedNoise
import World
import agent.Agent
import agent.Faction
import agent.qvalue.QActions
import agent.qvalue.QDestinations
import agent.qvalue.QValue
import config.*
import config.Location
import config.Sim
import extension.Canvas
import extension.Ctx
import extension.Grid
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.addClass
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.url.URL
import portal.Portal
import portal.XmMap
import system.Cycle
import system.display.Scene3D
import util.data.GeoCoords
import util.data.Line
import util.data.Pos
import util.ui.Controls
import util.ui.Demo
import util.ui.Inspector
import util.ui.LayerView
import util.ui.LoadingOverlay
import util.ui.Onboarding
import kotlin.js.Json

@Suppress("UnusedParameter") // external JS global; param describes the contract
external fun encodeURIComponent(uri: String): String

object HtmlUtil {
    private var intervalID = 0
    private const val PAUSE_BUTTON_ID = "pauseButton"
    private const val LOCATION_LABEL_ID = "locationLabel"
    private const val MIN_WALKABILITY = 0.12 // below this the area is mostly water/blocked → unplayable

    // The actually-loaded location (set by setLoadedLocation) — named in the top bar, and the target
    // a Reset reloads onto.
    private var currentLng = Location.DEFAULT.lng
    private var currentLat = Location.DEFAULT.lat
    private var currentLocationName = Location.DEFAULT.displayName

    fun isRunningInBrowser() = jsTypeOf(document) != "undefined"
    fun isNotRunningInBrowser() = !isRunningInBrowser()
    fun isLocal() = isRunningInBrowser() && document.location?.href?.contains("localhost") ?: false
    fun isQuickstart() = Config.quickStart

    private fun tick() {
        if (!World.isReady) {
            return
        }

        // Iterate a snapshot so mid-tick recruiting can't mutate the set we're
        // looping (recruits are buffered in World.pendingAgents, flushed below).
        val nextAgents = World.allAgents.toList().map { it.act() }.toSet()
        XmMap.updateStrayXm()

        World.allAgents.clear()
        World.allAgents.addAll(nextAgents)
        World.flushPendingAgents()

        World.allNonFaction.forEach { it.act() }
        window.requestAnimationFrame {
            DrawUtil.redraw()
            val userFaction = World.userFactionOrThrow()
            val factions = userFaction to userFaction.enemy()
            val enlMu = World.calcTotalMu(Faction.ENL)
            val resMu = World.calcTotalMu(Faction.RES)
            Cycle.updateCheckpoints(World.tick, enlMu, resMu)
            val firstMu = if (factions.first == Faction.ENL) enlMu else resMu
            val secondMu = if (factions.first == Faction.RES) enlMu else resMu
            DrawUtil.redrawUserInterface(firstMu, secondMu, factions)
            World.tick++
        }
    }

    fun load() {
        if (isNotRunningInBrowser()) return
        if (Controls.isUnsupported()) {
            Controls.showUnsupportedNotice()
            return
        }
        val rootDiv = document.getElementById("root") as HTMLDivElement
        rootDiv.addClass("container")

        // Prepare all canvas..
        World.can = createCanvas("mainCanvas")
        World.bgCan = createCanvas("backgroundCanvas")
        World.uiCan = createCanvas("uiCanvas")
        rootDiv.append(createCanvasDiv())

        // /demo harness (hash-routed): a menu of effect demo scenes, separate from the game.
        // Reload on any hash change so switching game ⇄ demo (or between scenes) re-bootstraps.
        window.addEventListener("hashchange", { window.location.reload() })
        val demoScene = Demo.route(window.location.hash)
        if (demoScene != null) {
            loadDemoScene()
            return
        }

        // Map clicks/hover are wired to MapLibre after the map loads (see onMapload → bindInteractions).
        val controlDiv = createControlDiv()
        val buttonDiv = document.createElement("div") as HTMLDivElement
        buttonDiv.addClass("buttonDiv")
        val pauseButton = createButton(PAUSE_BUTTON_ID, "topButton", "Stop") {
            intervalID = pauseHandler(intervalID) { tick() }
        }
        pauseButton.addClass("non", "amarillo")
        buttonDiv.append(pauseButton)

        buttonDiv.append(createLocationLabel()) // names the actual loaded location (set by setLoadedLocation)
        buttonDiv.append(createMenuSpan()) // New Game / Reset
        buttonDiv.append(createSearchSpan())
        buttonDiv.append(createVolumeSpan())
        buttonDiv.append(LayerView.createDropdown())
        buttonDiv.append(createCheckbox("passabilityToggle", "Passability") { Scene3D.setPassabilityVisible(it) })
        buttonDiv.append(createCheckbox("vectorFieldToggle", "Vectors") { Scene3D.setVectorFieldVisible(it) })
        controlDiv.append(buttonDiv)

        rootDiv.append(controlDiv)

        Controls.addLegend()

        startOnboardingOrWorld()
    }

    /**
     * Gate the world load behind the onboarding order: **faction → map-size → location → load**, run
     * as in-memory screens (no reloads). `?local=true` auto-starts; a faction+lng/lat deep link loads
     * directly (e.g. the in-game location dropdown's reload). Demo scenes returned earlier, before this.
     */
    private fun startOnboardingOrWorld() {
        val faction = getFactionFromUrl()
        val urlCenter = getLngLatFromUrl()
        when {
            isAutoStartFromUrl() -> {
                chooseUserFaction(faction ?: Faction.random())
                setLoadedLocation(urlCenter?.lng ?: Location.DEFAULT.lng, urlCenter?.lat ?: Location.DEFAULT.lat, getLocationNameFromUrl() ?: Location.DEFAULT.displayName)
                initWorld(centerOrDefault())
            }
            faction != null && urlCenter != null -> { // deep link → straight to load
                chooseUserFaction(faction)
                setLoadedLocation(urlCenter.lng, urlCenter.lat, getLocationNameFromUrl() ?: "Custom location")
                initWorld(centerOrDefault())
            }
            else -> runOnboarding()
        }
    }

    private fun runOnboarding() {
        Onboarding.showFaction { f ->
            chooseUserFaction(f)
            Onboarding.showMapSize(Config.startPortals) { w, h, portals, quick ->
                Sim.setSize(w, h) // size first, so the location screen's play-area box is the real size
                Config.startPortals = portals
                Config.quickStart = quick
                Onboarding.showLocation { lng, lat, name ->
                    setLoadedLocation(lng, lat, name)
                    initWorld(centerJson(lng, lat))
                }
            }
        }
    }

    private fun centerOrDefault(): Json = getLngLatFromUrl()?.toJson() ?: Location.DEFAULT.toJSON()

    private fun centerJson(lng: Double, lat: Double): Json = JSON.parse("[$lng,$lat]")

    private fun createCheckbox(id: String, labelText: String, onChange: (Boolean) -> Unit): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        val check = document.createElement("input") as HTMLInputElement
        check.id = id
        check.type = "checkbox"
        check.addClass("checkbox")
        check.onchange = {
            onChange(check.checked)
            null
        }
        val label = document.createElement("span") as HTMLSpanElement
        label.addClass("label", "topLabel")
        label.innerHTML = labelText
        label.onclick = { check.click() }
        span.append(check)
        span.append(label)
        return span
    }

    private fun createVolumeSpan(): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        val label = document.createElement("span") as HTMLSpanElement
        label.addClass("label", "topLabel")
        label.id = "soundLabel"
        label.innerHTML = "Volume"
        span.append(label)
        val slider = document.createElement("input") as HTMLInputElement
        slider.id = "volumeSlider"
        slider.type = "range"
        slider.min = "0.0"
        slider.max = "1.0"
        slider.step = "0.05"
        slider.value = SoundUtil.DEFAULT_VOLUME.toString()
        slider.addClass("slider", "volumeSlider")
        slider.oninput = {
            SoundUtil.setMasterVolume(slider.valueAsNumber)
            null
        }
        span.append(slider)
        return span
    }

    private fun createCanvasDiv(): HTMLDivElement {
        val div = document.createElement("div") as HTMLDivElement
        div.id = Navigation.CANVAS_LAYER_ID // transformed to follow the map camera
        div.append(World.uiCan)
        div.append(World.bgCan)
        div.append(World.can)
        return div
    }

    private fun createControlDiv(): HTMLDivElement {
        val div = document.createElement("div") as HTMLDivElement
        div.id = "top-controls"
        div.addClass("controls")
        return div
    }

    private fun createSliderDiv(
        id: String,
        qValues: List<QValue>,
        className: String,
        labelText: String,
        userFaction: Faction,
    ): HTMLDivElement {
        val qDiv = document.createElement("div") as HTMLDivElement
        qDiv.id = id
        qDiv.addClass("qValues", className)
        qDiv.addClass("q-" + labelText.lowercase())
        val destinationsLabel = document.createElement("div") as HTMLDivElement
        destinationsLabel.addClass("label", "qTitle")
        destinationsLabel.innerHTML = labelText
        qDiv.append(destinationsLabel)
        qValues.forEach { qValue ->
            val sliderDiv = document.createElement("div") as HTMLDivElement
            val facts = listOf(userFaction, userFaction.enemy())
            facts.forEach { faction ->
                val slider = document.createElement("input") as HTMLInputElement
                slider.id = qValue.sliderId + faction.nickName
                slider.type = "range"
                slider.min = "0.00"
                slider.max = "1.00"
                slider.step = "0.01"
                slider.value = "0.10"
                slider.addClass("slider", "qSlider", faction.abbr.lowercase() + "Slider")
                val sliderValue = document.createElement("span") as HTMLSpanElement
                sliderValue.addClass("qSliderLabel", faction.abbr.lowercase() + "Label")
                if (faction != userFaction) {
                    slider.addClass("invisible")
                    sliderValue.addClass("invisible")
                } else {
                    slider.oninput = {
                        sliderValue.innerHTML = qDisplay(slider.value)
                        null
                    }
                }
                sliderValue.innerHTML = qDisplay(slider.value)
                sliderDiv.append(slider)
                sliderDiv.append(sliderValue)
            }
            val qSliderLabel = document.createElement("span") as HTMLSpanElement
            qSliderLabel.addClass("qSliderTextLabel")
            if (qValue.icon != null) {
                val sliderImg = document.createElement("img") as HTMLImageElement
                sliderImg.src = qValue.icon.toDataURL()
                qSliderLabel.innerHTML = sliderImg.outerHTML + " " + qValue.description
            } else {
                qSliderLabel.innerHTML = qValue.description
            }
            sliderDiv.append(qSliderLabel)
            qDiv.append(sliderDiv)
        }
        return qDiv
    }

    private fun qDisplay(qValue: String): String {
        val fixed = qValue.padEnd(4, '0')
        return when (fixed) {
            "0000" -> "0.00"
            "1000" -> "1.00"
            else -> fixed
        }
    }

    // Minimal bootstrap for the unified sandbox demo scene: just the 3D scene + the sandbox controls.
    private fun loadDemoScene() {
        World.userFaction = Faction.ENL
        World.resetAllCanvas()
        val center = Pos(Sim.width / 2, Sim.height / 2)
        MapUtil.loadMaps(Location.DEFAULT.toJSON(), demo = true, callback = fun(grid: Grid) {
            World.grid = grid
            World.isReady = true
            Navigation.setup()
            MapUtil.enable3D()
            // Unified sandbox: a placement toggle picks what LMB/RMB do.
            // Build mode: LMB places a portal · RMB shatters the nearest.
            // Effect mode: LMB detonates an XMP · RMB hacks (spins) the nearest portal's resonators.
            MapUtil.bindPortalDemo(
                fun(event: dynamic) {
                    val pos = MapUtil.eventToSimPos(event) ?: return
                    SoundUtil.enableAudio()
                    if (Demo.isPlacement()) {
                        Scene3D.placeShowcase(pos, Demo.portalLevel(), Demo.portalColorValue())
                        SoundUtil.playPortalCreationSound(pos)
                    } else {
                        Scene3D.playXmpBurst(pos, Demo.portalLevel())
                    }
                },
                fun(event: dynamic) {
                    val pos = MapUtil.eventToSimPos(event) ?: return
                    if (Demo.isPlacement()) {
                        Scene3D.removeShowcaseNear(pos)
                        SoundUtil.playGlassShatterSound(pos, 0.4, 0.9)
                    } else {
                        Scene3D.hackShowcaseNear(pos)
                    }
                },
            )
            intervalID = document.defaultView?.setInterval({ demoTick() }, Time.minTickInterval) ?: 0
            Demo.showControls(center)
        })
    }

    private fun demoTick() {
        if (World.isReady) window.requestAnimationFrame { DrawUtil.redraw() }
    }

    private fun initWorld(center: Json) {
        Onboarding.close() // dismiss the onboarding screen (it loads without a reload)
        // Staged loading overlay, up before the first tile request (the world build runs ~2 min on Big).
        LoadingOverlay.show()
        val noiseAlpha = 0.8
        val w = Dim.width
        val h = Dim.height
        SoundUtil.playNoiseGenSound()
        World.noiseMap = ImprovedNoise.generateEdgeMap(w, h)
        World.noiseImage = World.createNoiseImage(World.noiseMap, w, h, noiseAlpha)
        World.resetAllCanvas()
        MapUtil.loadMaps(center, callback = onMapload())
    }

    private fun closePopup() {
        (document.getElementById("popup") as? HTMLDivElement)?.addClass("invisible")
    }

    private fun isAutoStartFromUrl() = url().searchParams.get("local")?.toBoolean() ?: false

    private fun createQSliders(fact: Faction) {
        val actionSliderDiv = createSliderDiv("left-sliders", QActions.values(), "floatLeft", "Actions", fact)
        val destinationSliderDiv =
            createSliderDiv("right-sliders", QDestinations.values(), "floatRight", "Destinations", fact)
        val controlDiv = document.getElementById("top-controls") as HTMLDivElement
        controlDiv.append(actionSliderDiv)
        controlDiv.append(destinationSliderDiv)
    }

    private fun chooseUserFaction(fact: Faction) {
        SoundUtil.enableAudio() // first user gesture → resume audio (autoplay policy)
        closePopup()
        val pauseButton = document.getElementById(PAUSE_BUTTON_ID) as HTMLButtonElement
        pauseButton.addClass(fact.abbr.lowercase())
        if (World.userFaction != null) {
            console.warn("Faction ${World.userFaction} was already chosen.")
            return
        }
        World.userFaction = fact
        LoadingOverlay.setAccent(fact.color) // tint the loading screen with the chosen faction
    }

    private fun resetInterval() {
        intervalID = document.defaultView?.setInterval({ tick() }, Time.minTickInterval) ?: 0
    }

    private fun pauseHandler(intervalID: Int, tickFunction: () -> Unit): Int {
        val pauseButton = document.getElementById(PAUSE_BUTTON_ID) as HTMLButtonElement
        return if (intervalID != -1) {
            pauseButton.innerText = "Start"
            document.defaultView?.clearInterval(intervalID)
            -1
        } else {
            pauseButton.innerText = "Stop"
            document.defaultView?.setInterval({ tickFunction() }, Time.minTickInterval) ?: 0
        }
    }

    fun isBlockedByMapbox(pos: Pos) = isInMapboxArea(pos) || isInOsmArea(pos)

    private fun isInMapboxArea(pos: Pos): Boolean {
        val area = Line(Pos(-20, Dim.height - 40), Pos(90, Dim.height))
        return pos.x > area.from.x &&
            pos.x <= area.to.x &&
            pos.y > area.from.y &&
            pos.y <= area.to.y
    }

    private fun isInOsmArea(pos: Pos): Boolean {
        val w = Dim.width
        val area = Line(Pos(w - 280, Dim.height - 30), Pos(w, Dim.height))
        return pos.x > area.from.x &&
            pos.x <= area.to.x &&
            pos.y > area.from.y &&
            pos.y <= area.to.y
    }

    private fun onMapClick(event: dynamic) {
        SoundUtil.enableAudio() // first user gesture → resume audio (autoplay policy)
        // Ground point under the cursor; MapLibre fires "click" only for a click, not after a drag.
        val pos = MapUtil.eventToSimPos(event) ?: return
        when {
            // Click on/near a portal → select it for the inspector.
            pos.hasClosePortalForClick() -> Inspector.select("portal:" + pos.findClosestPortal().id)
            // Click empty buildable ground → build a portal there; deselect.
            pos.isBuildable() -> {
                Inspector.select(null)
                if (World.countPortals() < Config.maxPortals) {
                    document.defaultView?.setTimeout(World.allPortals.add(Portal.create(pos)), 0)
                } else {
                    SoundUtil.playFailSound()
                }
            }
            else -> Inspector.select(null)
        }
    }

    private fun onMapMove(event: dynamic) {
        val pos = MapUtil.eventToSimPos(event)
        if (pos == null) {
            Scene3D.setBuildMarker(null, "")
            return
        }
        val state = when {
            pos.hasClosePortalForClick() -> "portal"
            pos.isBuildable() -> "build"
            else -> "blocked"
        }
        Scene3D.setBuildMarker(pos, state)
    }

    private fun maybeWidth(id: String) = document.getElementById(id)?.clientWidth
    private fun maybeHeight(id: String) = document.getElementById(id)?.clientHeight
    fun topActionOffset(): Int = maybeHeight("top-controls") ?: 100
    fun leftSliderWidth() = maybeWidth("left-sliders") ?: 241
    fun leftSliderHeight() = maybeHeight("left-sliders") ?: 217
    fun rightSliderWidth() = maybeWidth("right-sliders") ?: 213
    fun rightSliderHeight() = maybeHeight("right-sliders") ?: 145

    private fun createButton(
        id: String,
        className: String,
        text: String,
        callback: ((Event) -> Unit)?,
    ): HTMLButtonElement {
        val button = document.createElement("BUTTON") as HTMLButtonElement
        button.id = id
        button.addClass(className)
        button.onclick = callback
        button.innerText = text
        return button
    }

    private fun createCanvas(className: String): Canvas {
        val canvas = document.createElement("canvas") as Canvas
        canvas.addClass("canvas", className)
        canvas.width = Dim.width
        canvas.height = Dim.height
        return canvas
    }

    private fun createOffscreenCanvas(w: Int, h: Int): Canvas {
        val canvas = document.createElement("canvas") as Canvas
        canvas.width = w
        canvas.height = h
        return canvas
    }

    fun preRender(w: Int, h: Int, drawFun: (CanvasRenderingContext2D) -> Unit): Canvas {
        val offscreen = createOffscreenCanvas(w, h)
        val offscreenCtx = getContext2D(offscreen)
        drawFun(offscreenCtx)
        return offscreen
    }

    fun getContext2D(canvas: Canvas): Ctx = canvas.getContext("2d") as Ctx

    private fun createPortals(callback: () -> Unit) {
        val total = Config.startPortals
        fun createPortal(callback: () -> Unit, count: Int) {
            document.defaultView?.setTimeout(fun() {
                if (count > 0) {
                    val newPortal = Portal.createRandom()
                    LoadingOverlay.building(
                        LoadingOverlay.PCT_WORLD,
                        LoadingOverlay.PCT_PEOPLE,
                        total - count + 1,
                        total,
                        "Creating portal ${newPortal.name}",
                    )
                    World.allPortals.add(newPortal)
                    // Render the spawning world behind the (now translucent) loading screen: the new
                    // portal grows in and its colour-coded flow vectors show.
                    Scene3D.selected = "portal:${newPortal.id}"
                    Scene3D.setVectorFieldVisible(true)
                    Scene3D.sync()
                    createPortal(callback, count - 1)
                } else {
                    callback()
                }
            }, 0)
        }
        LoadingOverlay.detail("Creating portals…")
        World.allPortals.clear()
        createPortal(callback, total)
    }

    private fun createAgents(callback: () -> Unit) {
        World.allAgents.clear()
        LoadingOverlay.detail("Creating agents…")
        (1..Config.startFrogs()).forEach {
            World.allAgents.add(Agent.createFrog(World.grid))
        }
        (1..Config.startSmurfs()).forEach {
            World.allAgents.add(Agent.createSmurf(World.grid))
        }
        World.allNonFaction.clear()
        World.createNonFaction(callback, Config.maxFor())
    }

    private fun createAgentsAndPortals(callback: () -> Unit) = createPortals(fun() {
        createAgents(callback)
    })

    private fun onMapload() = fun(grid: Grid) {
        World.grid = grid
        if (World.grid.isEmpty()) {
            console.error("Grid is empty!")
        }
        // Gate mostly-water / unplayable locations before the expensive world build. Auto-start
        // (dev/headless) is exempt so tests never block.
        if (World.walkability < MIN_WALKABILITY && !isAutoStartFromUrl()) {
            showUnplayableGate()
            return
        }
        // Anchor the 3D scene BEFORE spawning portals: Portal.create → PortalNames.nameFor projects
        // POI/street lng/lat through Scene3D.lngLatToSimPos, which throws until Scene3D is registered.
        // Registering first means portals actually adopt their real map names (else all fall back to
        // the random generator).
        MapUtil.enable3D()
        LoadingOverlay.stage(LoadingOverlay.PCT_WORLD, "Building world…")
        createAgentsAndPortals {
            LoadingOverlay.detail("Ready.")
            DrawUtil.clearBackground()
            // Clear the during-build vector preview so the game starts with nothing selected.
            Scene3D.selected = null
            Scene3D.setVectorFieldVisible(false)
            if (World.userFaction == null) {
                chooseUserFaction(Faction.random())
            }
            createQSliders(World.userFactionOrThrow())
            resetInterval()
            World.isReady = true
            LayerView.apply()
            Navigation.setup()
            MapUtil.bindInteractions(::onMapClick, ::onMapMove)
            LoadingOverlay.done()
        }
    }

    /** Record (and name in the top bar) the location the world actually loaded at. */
    private fun setLoadedLocation(lng: Double, lat: Double, name: String) {
        currentLng = lng
        currentLat = lat
        currentLocationName = name.replace("%20", " ").ifBlank { "Custom location" }
        document.getElementById(LOCATION_LABEL_ID)?.textContent = currentLocationName
    }

    private fun createLocationLabel(): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        span.id = LOCATION_LABEL_ID
        span.addClass("topLocation", "amarillo")
        span.textContent = currentLocationName
        return span
    }

    /** "Menu" button → a small popup with New Game (re-onboard) and Reset (reload this location). */
    private fun createMenuSpan(): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        span.addClass("menuSpan")
        val menu = document.createElement("div") as HTMLDivElement
        menu.addClass("gameMenu", "invisible")
        menu.append(createButton("menuNewGame", "menuItem amarillo", "New Game") { doNewGame() })
        menu.append(createButton("menuReset", "menuItem amarillo", "Reset") { doReset() })
        val button = createButton("menuButton", "topButton amarillo", "Menu") {
            menu.classList.toggle("invisible")
        }
        span.append(button)
        span.append(menu)
        return span
    }

    /** Block the build when the chosen area is mostly water/blocked; offer to pick another location. */
    private fun showUnplayableGate() {
        LoadingOverlay.done()
        val screen = document.createElement("div") as HTMLDivElement
        screen.id = "unplayableGate"
        screen.addClass("onboardScreen")
        val title = document.createElement("div") as HTMLDivElement
        title.addClass("onboardTitle")
        title.textContent = "Not enough ground to play"
        screen.append(title)
        val hint = document.createElement("div") as HTMLDivElement
        hint.addClass("onboardHint")
        hint.textContent = "“$currentLocationName” is only ${(World.walkability * 100).toInt()}% walkable — mostly water or blocked. Pick another location."
        screen.append(hint)
        screen.append(createButton("gateNewGame", "topButton amarillo onboardStart", "Choose another location") { doNewGame() })
        document.body?.append(screen)
    }

    /** New Game: drop all URL params and reload → the onboarding flow runs from scratch. */
    private fun doNewGame() {
        val loc = document.location
        document.location?.href = (loc?.origin ?: "") + (loc?.pathname ?: "/")
    }

    /** Reset: reload onto the current location (deep link) → a fresh world at the same place. */
    private fun doReset() {
        navigateToLocation(currentLng, currentLat, currentLocationName)
    }

    private const val LOCATION_SEARCH_ID = "locationSearch"

    // Free-form "play your hometown" search: geocode any place/address via the
    // keyless Nominatim (OpenStreetMap) service, then recenter through the same
    // URL flow the preset dropdown uses.
    private fun createSearchSpan(): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        val input = document.createElement("input") as HTMLInputElement
        input.id = LOCATION_SEARCH_ID
        input.type = "text"
        input.placeholder = "play a place…"
        input.addClass("topSearch", "coda")
        input.addEventListener("keydown", { event ->
            if ((event as KeyboardEvent).key == "Enter") handleLocationSearch(input.value)
        })
        val button = createButton("locationSearchButton", "topButton", "Go") {
            handleLocationSearch(input.value)
        }
        span.append(input)
        span.append(button)
        return span
    }

    private fun handleLocationSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + encodeURIComponent(trimmed)
        val request: dynamic = window.asDynamic().fetch(url)
        request.then { response: dynamic -> response.json() }
            .then { results: dynamic ->
                if (results.length > 0) {
                    val first = results[0]
                    val lng = (first.lon as String).toDouble()
                    val lat = (first.lat as String).toDouble()
                    navigateToLocation(lng, lat, trimmed)
                } else {
                    window.alert("No location found for \"$trimmed\".")
                }
            }
            .catch { error: dynamic -> console.error("Geocoding failed:", error) }
    }

    private fun navigateToLocation(lng: Double, lat: Double, name: String) {
        document.location?.href = createNewUrl(lng.toString(), lat.toString(), name)
    }

    // Build off the current origin + path so recentering works on any host
    // (local dev server, GitHub Pages, …) rather than a hard-coded port.
    private fun createNewUrl(lng: String, lat: String, name: String): String {
        val location = document.location
        val base = (location?.origin ?: "") + (location?.pathname ?: "/")
        val fact = World.userFaction?.abbr ?: ""
        return addParameters(base, fact, lng, lat, name, isQuickstart())
    }

    private fun url() = URL(document.location?.href ?: "")
    private fun getLocationNameFromUrl() = url().searchParams.get("name")
    private fun getLngLatFromUrl(): GeoCoords? {
        val url = url()
        val lngString = url.searchParams.get("lng")
        val latString = url.searchParams.get("lat")
        return GeoCoords.fromStrings(lngString, latString)
    }

    private fun getFactionFromUrl() = Faction.fromString(url().searchParams.get("faction"))

    private fun addParameters(
        url: String,
        faction: String,
        lng: String,
        lat: String,
        name: String,
        isQs: Boolean,
    ): String = "$url?faction=$faction&lng=$lng&lat=$lat&name=$name&quickstart=$isQs"
}
