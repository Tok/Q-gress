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
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.url.URL
import portal.Portal
import portal.XmMap
import system.Cycle
import system.display.Scene3D
import system.display.loading.Loading
import system.display.loading.LoadingText
import system.display.ui.ActionLimitsDisplay
import util.data.GeoCoords
import util.data.Line
import util.data.Pos
import util.ui.Controls
import util.ui.Demo
import util.ui.Inspector
import kotlin.js.Json

@Suppress("UnusedParameter") // external JS global; param describes the contract
external fun encodeURIComponent(uri: String): String

object HtmlUtil {
    private var intervalID = 0
    private const val PAUSE_BUTTON_ID = "pauseButton"
    private const val LOCATION_DROPDOWN_ID = "locationSelect"

    fun isRunningInBrowser() = jsTypeOf(document) != "undefined"
    fun isNotRunningInBrowser() = !isRunningInBrowser()
    fun isLocal() = isRunningInBrowser() && document.location?.href?.contains("localhost") ?: false
    fun isQuickstart() = isRunningInBrowser() && (document.getElementById("quickstart") as HTMLInputElement).checked

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
            if (demoScene == Demo.MENU) Demo.showMenu() else loadDemoScene(demoScene)
            return
        }

        World.uiCan.addEventListener("click", { event -> handleMouseClick(event) }, false)
        val controlDiv = createControlDiv()
        val buttonDiv = document.createElement("div") as HTMLDivElement
        buttonDiv.addClass("buttonDiv")
        val pauseButton = createButton(PAUSE_BUTTON_ID, "topButton", "Stop") {
            intervalID = pauseHandler(intervalID) { tick() }
        }
        pauseButton.addClass("non", "amarillo")
        buttonDiv.append(pauseButton)

        val dropDown = createDropdown(LOCATION_DROPDOWN_ID) { mapChangeHandler() }
        // No location in the URL → show the default location (which initWorld also
        // centers on), so the dropdown matches what's actually rendered.
        val selectionName = getLocationNameFromUrl() ?: Location.DEFAULT.displayName
        setLocationDropdownSelection(dropDown, selectionName)
        buttonDiv.append(dropDown)

        buttonDiv.append(createSearchSpan())
        buttonDiv.append(createVolumeSpan())
        buttonDiv.append(createLayerDropdown())
        buttonDiv.append(createCheckbox("passabilityToggle", "Passability") { Scene3D.setPassabilityVisible(it) })
        buttonDiv.append(createCheckbox("vectorFieldToggle", "Vectors") { Scene3D.setVectorFieldVisible(it) })
        controlDiv.append(buttonDiv)

        rootDiv.append(controlDiv)
        controlDiv.addEventListener("mousemove", { event -> handleMouseMove(event) }, false)
        rootDiv.addEventListener("mousemove", { event -> handleMouseMove(event) }, false)

        Controls.addLegend()

        val popupId = "popup"
        rootDiv.append(createPopup(popupId))

        val maybeFaction = getFactionFromUrl()
        if (maybeFaction != null) {
            chooseUserFaction(maybeFaction)
        } else {
            if (isLocal()) {
                chooseUserFaction(Faction.random())
            }
        }

        initWorld()
    }

    private fun createPopup(id: String): HTMLDivElement {
        fun createButton(faction: Faction): HTMLButtonElement {
            val button = document.createElement("button") as HTMLButtonElement
            button.id = faction.abbr.lowercase() + "Button"
            button.addClass(faction.abbr.lowercase(), "popupButton", "amarillo")
            button.innerText = faction.abbr.uppercase()
            button.onclick = {
                chooseUserFaction(faction)
            }
            return button
        }

        val popupDiv = document.createElement("div") as HTMLDivElement
        popupDiv.id = id
        popupDiv.addClass("popup")

        val popupButtonDiv = document.createElement("div") as HTMLDivElement

        val enlButton = createButton(Faction.ENL)
        val resButton = createButton(Faction.RES)

        val quickstartDiv = document.createElement("div") as HTMLDivElement
        quickstartDiv.addClass("quickstartDiv")
        val quickstartCheck = document.createElement("input") as HTMLInputElement
        quickstartCheck.id = "quickstart"
        quickstartCheck.type = "checkbox"
        quickstartCheck.checked = isQuickstartFromUrl()
        quickstartCheck.addClass("checkbox")
        quickstartCheck.disabled = true // FIXME
        val quickstartLabel = document.createElement("span") as HTMLSpanElement
        quickstartLabel.addClass("coda", "loadLabel")
        quickstartLabel.id = "quickstartLabel"
        quickstartLabel.innerHTML = "Quick Start"
        quickstartLabel.onclick = { quickstartCheck.click() }

        popupButtonDiv.append(enlButton)
        popupButtonDiv.append(resButton)
        quickstartDiv.append(quickstartCheck)
        quickstartDiv.append(quickstartLabel)

        popupDiv.append(popupButtonDiv)
        popupDiv.append(quickstartDiv)
        return popupDiv
    }

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

    private const val LAYER_DROPDOWN_ID = "layerSelect"
    private const val LAYER_SATELLITE = "Satellite"
    private const val LAYER_STREET = "Street"

    // A small dropdown to choose the base map layer. Structured so more styles
    // can be added later (e.g. a dark/terrain style).
    private fun createLayerDropdown(): HTMLSelectElement {
        val select = document.createElement("select") as HTMLSelectElement
        select.id = LAYER_DROPDOWN_ID
        select.addClass("topDrop", "amarillo")
        listOf(LAYER_SATELLITE, LAYER_STREET).forEach { layer ->
            val opt = document.createElement("option") as HTMLOptionElement
            opt.text = layer
            opt.value = layer
            select.appendChild(opt)
        }
        select.onchange = { applySelectedLayer() }
        return select
    }

    private fun applySelectedLayer() {
        if (isShowSatelliteMap()) MapUtil.showSatellite() else MapUtil.showStreet()
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

    // Minimal bootstrap for a demo scene: the 3D scene + one central portal, no game.
    private fun loadDemoScene(scene: String) {
        World.userFaction = Faction.ENL
        World.resetAllCanvas()
        val center = Pos(Sim.width / 2, Sim.height / 2)
        MapUtil.loadMaps(Location.DEFAULT.toJSON(), fun(grid: Grid) {
            World.grid = grid
            World.isReady = true
            Navigation.setup()
            MapUtil.enable3D()
            document.defaultView?.setTimeout({ World.allPortals.add(Portal.create(center)) }, 0)
            intervalID = document.defaultView?.setInterval({ demoTick() }, Time.minTickInterval) ?: 0
            Demo.showControls(scene, center)
        })
    }

    private fun demoTick() {
        if (World.isReady) window.requestAnimationFrame { DrawUtil.redraw() }
    }

    private fun initWorld() {
        val noiseAlpha = 0.8
        val w = Dim.width
        val h = Dim.height
        SoundUtil.playNoiseGenSound()
        World.noiseMap = ImprovedNoise.generateEdgeMap(w, h)
        World.noiseImage = World.createNoiseImage(World.noiseMap, w, h, noiseAlpha)
        World.resetAllCanvas()
        ActionLimitsDisplay.drawTop()
        // Default (no explicit location) → a known, well-covered location rather
        // than the [0,0] "Unknown Location" sentinel (open ocean: no streets, so
        // the passability grid would be empty). Detect the sentinel by its
        // coordinates — a string compare on the JS array is unreliable here.
        val selected: dynamic = getSelectedCenterFromUrl()
        val hasRealCenter = selected != null && (selected[0] != 0.0 || selected[1] != 0.0)
        val center: Json = if (hasRealCenter) selected.unsafeCast<Json>() else Location.DEFAULT.toJSON()
        MapUtil.loadMaps(center, onMapload())
    }

    private fun closePopup() {
        val popup = document.getElementById("popup") as HTMLDivElement
        popup.addClass("invisible")
    }

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

    private fun handleMouseClick(event: Event) {
        SoundUtil.enableAudio() // first user gesture → resume audio (autoplay policy)
        if (event !is MouseEvent) {
            console.warn("Unhandled event: $event.")
            return
        }
        // Ground point under the cursor (pitch-safe via map.unproject).
        val pos = MapUtil.screenToSimPos(event.clientX.toDouble(), event.clientY.toDouble()) ?: return
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

    private fun handleMouseMove(event: Event) {
        if (event !is MouseEvent) return
        val pos = MapUtil.screenToSimPos(event.clientX.toDouble(), event.clientY.toDouble())
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

    private fun createLocationOptions(): List<HTMLOptionElement> = Location.values().map {
        val opt = document.createElement("option") as HTMLOptionElement
        opt.text = it.displayName
        opt.value = it.toJSONString()
        opt
    }

    private fun createDropdown(id: String, callback: ((Event) -> Unit)?): HTMLSelectElement {
        val select = document.createElement("select") as HTMLSelectElement
        select.id = id
        select.addClass("topDrop", "amarillo")
        select.onchange = callback
        createLocationOptions().forEach { select.appendChild(it) }
        return select
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
        fun createPortal(callback: () -> Unit, count: Int) {
            document.defaultView?.setTimeout(fun() {
                if (count > 0) {
                    val newPortal = Portal.createRandom()
                    Loading.draw()
                    LoadingText.draw("Creating Portal ${newPortal.name}")
                    World.allPortals.add(newPortal)
                    createPortal(callback, count - 1)
                } else {
                    callback()
                }
            }, 0)
        }
        LoadingText.draw("Creating Portals..")
        World.allPortals.clear()
        createPortal(callback, Config.startPortals)
    }

    private fun createAgents(callback: () -> Unit) {
        World.allAgents.clear()
        LoadingText.draw("Creating Frogs..")
        (1..Config.startFrogs()).forEach {
            World.allAgents.add(Agent.createFrog(World.grid))
        }

        LoadingText.draw("Creating Smurfs..")
        (1..Config.startSmurfs()).forEach {
            World.allAgents.add(Agent.createSmurf(World.grid))
        }

        LoadingText.draw("Creating Non-Faction..")
        World.allNonFaction.clear()
        World.createNonFaction(callback, Config.maxFor())
    }

    private fun createAgentsAndPortals(callback: () -> Unit) = createPortals(fun() {
        createAgents(callback)
    })

    fun isShowSatelliteMap(): Boolean {
        val dropdown = document.getElementById(LAYER_DROPDOWN_ID) as? HTMLSelectElement ?: return true
        return dropdown[dropdown.selectedIndex]?.let { (it as HTMLOptionElement).value } == LAYER_SATELLITE
    }

    private fun onMapload() = fun(grid: Grid) {
        World.grid = grid
        if (World.grid.isEmpty()) {
            console.error("Grid is empty!")
        }
        createAgentsAndPortals {
            LoadingText.draw("Ready.")
            DrawUtil.clearBackground()
            if (World.userFaction == null) {
                chooseUserFaction(Faction.random())
            }
            createQSliders(World.userFactionOrThrow())
            resetInterval()
            World.isReady = true
            applySelectedLayer()
            Navigation.setup()
            MapUtil.enable3D()
        }
    }

    private fun mapChangeHandler() {
        val center: dynamic = getCenterFromDropdown()
        val name = getLocationNameFromDropdown()
        navigateToLocation((center[0] as Number).toDouble(), (center[1] as Number).toDouble(), name)
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

    private fun getCenterFromDropdown(): Json {
        val dropdown = document.getElementById(LOCATION_DROPDOWN_ID) as HTMLSelectElement
        val selection = dropdown[dropdown.selectedIndex] as HTMLOptionElement
        return JSON.parse(selection.value)
    }

    private fun getLocationNameFromDropdown(): String {
        val dropdown = document.getElementById(LOCATION_DROPDOWN_ID) as HTMLSelectElement
        val selection = dropdown[dropdown.selectedIndex] as HTMLOptionElement
        return selection.text
    }

    private fun setLocationDropdownSelection(dropdown: HTMLSelectElement, name: String) {
        val cleanName = name.replace("%20", " ")
        var hasMatch = false
        (0 until dropdown.options.length).forEach {
            val option = dropdown.options[it] as HTMLOptionElement
            if (option.label == cleanName) {
                dropdown.selectedIndex = it
                hasMatch = true
            }
        }
        if (!hasMatch) {
            // A custom (searched) location: show its name and real coordinates.
            val geo = getLngLatFromUrl()
            val opt = document.createElement("option") as HTMLOptionElement
            opt.text = if (cleanName.isNotBlank() && cleanName != "unknown") cleanName else "Custom Location"
            opt.value = if (geo != null) "[${geo.lng},${geo.lat}]" else "[0.0,0.0]"
            dropdown.add(opt)
            dropdown.selectedIndex = dropdown.length - 1
        }
    }

    private fun getSelectedCenterFromUrl(): Json {
        val geo: GeoCoords? = getLngLatFromUrl()
        return geo?.toJson() ?: getCenterFromDropdown()
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

    private fun isQuickstartFromUrl() = url().searchParams.get("quickstart")?.toBoolean() ?: false

    private fun addParameters(
        url: String,
        faction: String,
        lng: String,
        lat: String,
        name: String,
        isQs: Boolean,
    ): String = "$url?faction=$faction&lng=$lng&lat=$lat&name=$name&quickstart=$isQs"
}
