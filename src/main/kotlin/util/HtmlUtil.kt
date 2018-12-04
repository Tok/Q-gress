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
import extension.Canvas
import extension.Ctx
import extension.Grid
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.url.URL
import portal.Portal
import portal.XmMap
import system.Cycle
import system.display.VectorFields
import system.display.loading.Loading
import system.display.loading.LoadingText
import system.display.ui.ActionLimitsDisplay
import util.data.Cell
import util.data.Pos
import util.data.GeoCoords
import util.data.Line
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.addClass
import kotlin.dom.removeClass
import kotlin.js.Json

object HtmlUtil {
    private var intervalID = 0
    private const val PAUSE_BUTTON_ID = "pauseButton"
    private const val LOCATION_DROPDOWN_ID = "locationSelect"
    const val SOUND_CHECKBOX_ID = "soundCheckbox"

    fun isRunningInBrowser() = jsTypeOf(document) != "undefined"
    fun isNotRunningInBrowser() = !isRunningInBrowser()
    fun isLocal() = isRunningInBrowser() && document.location?.href?.contains("localhost") ?: false
    fun isQuickstart() = isRunningInBrowser() && (document.getElementById("quickstart") as HTMLInputElement).checked

    private fun tick() {
        if (!World.isReady) {
            return
        }

        val nextAgents = World.allAgents.map { it.act() }.toSet()
        XmMap.updateStrayXm()

        World.allAgents.clear()
        World.allAgents.addAll(nextAgents)

        World.allNonFaction.forEach { it.act() }
        window.requestAnimationFrame {
            DrawUtil.redraw()
            val factions = World.userFaction!! to World.userFaction?.enemy()!!
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
        val rootDiv = document.getElementById("root") as HTMLDivElement
        rootDiv.addClass("container")

        //Prepare all canvas..
        World.can = createCanvas("mainCanvas")
        World.bgCan = createCanvas("backgroundCanvas")
        World.uiCan = createCanvas("uiCanvas")
        World.uiCan.addEventListener("click", { event -> handleMouseClick(event) }, false)
        rootDiv.append(createCanvasDiv())

        val controlDiv = createControlDiv()
        val buttonDiv = document.createElement("div") as HTMLDivElement
        buttonDiv.addClass("buttonDiv")
        val pauseButton = createButton(PAUSE_BUTTON_ID, "topButton", "Stop") {
            intervalID = pauseHandler(intervalID) { tick() }
        }
        pauseButton.addClass("non", "amarillo")
        buttonDiv.append(pauseButton)

        val dropDown = createDropdown(LOCATION_DROPDOWN_ID) { mapChangeHandler() }
        val selectionName = getLocationNameFromUrl() ?: "unknown"
        setLocationDropdownSelection(dropDown, selectionName)
        buttonDiv.append(dropDown)

        buttonDiv.append(createSoundSpan())
        buttonDiv.append(createSatSpan())
        controlDiv.append(buttonDiv)

        rootDiv.append(controlDiv)
        controlDiv.addEventListener("mousemove", { event -> handleMouseMove(event) }, false)
        rootDiv.addEventListener("mousemove", { event -> handleMouseMove(event) }, false)

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
            button.id = faction.abbr.toLowerCase() + "Button"
            button.addClass(faction.abbr.toLowerCase(), "popupButton", "amarillo")
            button.innerText = faction.abbr.toUpperCase()
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
        quickstartCheck.disabled = true //FIXME
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

    private fun createSoundSpan(): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        val checkbox = document.createElement("input") as HTMLInputElement
        checkbox.id = SOUND_CHECKBOX_ID
        checkbox.type = "checkbox"
        checkbox.checked = Config.isSoundOn
        checkbox.addClass("checkbox")
        span.append(checkbox)
        val label = document.createElement("span") as HTMLSpanElement
        label.addClass("label", "topLabel")
        label.id = "soundLabel"
        label.innerHTML = "Sound"
        label.onclick = { checkbox.click() }
        span.append(label)
        return span
    }

    private fun createSatSpan(): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        val checkbox = document.createElement("input") as HTMLInputElement
        checkbox.id = "satCheckbox"
        checkbox.type = "checkbox"
        checkbox.checked = Config.isSatOn
        checkbox.addClass("checkbox")
        checkbox.onchange = { if (checkbox.checked) MapUtil.showSatelliteMap() else MapUtil.hideSatelliteMap() }
        span.append(checkbox)
        val label = document.createElement("span") as HTMLSpanElement
        label.addClass("label", "topLabel")
        label.id = "satLabel"
        label.innerHTML = "Satellite"
        label.onclick = { checkbox.click() }
        span.append(label)
        return span
    }

    private fun createCanvasDiv(): HTMLDivElement {
        val div = document.createElement("div") as HTMLDivElement
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

    private fun createSliderDiv(id: String, qValues: List<QValue>, className: String,
                                labelText: String, userFaction: Faction): HTMLDivElement {
        val qDiv = document.createElement("div") as HTMLDivElement
        qDiv.id = id
        qDiv.addClass("qValues", className)
        qDiv.addClass("q-" + labelText.toLowerCase())
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
                slider.addClass("slider", "qSlider", faction.abbr.toLowerCase() + "Slider")
                val sliderValue = document.createElement("span") as HTMLSpanElement
                sliderValue.addClass("qSliderLabel", faction.abbr.toLowerCase() + "Label")
                if (faction != userFaction) {
                    slider.addClass("invisible")
                    sliderValue.addClass("invisible")
                } else {
                    slider.oninput = { sliderValue.innerHTML = qDisplay(slider.value); null }
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

    private fun initWorld() {
        val noiseAlpha = 0.8
        val w = Dim.width
        val h = Dim.height
        SoundUtil.playNoiseGenSound()
        World.noiseMap = ImprovedNoise.generateEdgeMap(w, h)
        World.noiseImage = World.createNoiseImage(World.noiseMap, w, h, noiseAlpha)
        World.resetAllCanvas()
        ActionLimitsDisplay.drawTop()
        val maybeCenter = getSelectedCenterFromUrl()
        val center = if (maybeCenter.toString() != "0,0") maybeCenter else Location.random().toJSON()
        MapUtil.loadMaps(center, onMapload())
    }

    private fun closePopup() {
        val popup = document.getElementById("popup") as HTMLDivElement
        popup.addClass("invisible")
    }

    private fun createQSliders(fact: Faction) {
        val actionSliderDiv = createSliderDiv("left-sliders", QActions.values(), "floatLeft", "Actions", fact)
        val destinationSliderDiv = createSliderDiv("right-sliders", QDestinations.values(), "floatRight", "Destinations", fact)
        val controlDiv = document.getElementById("top-controls") as HTMLDivElement
        controlDiv.append(actionSliderDiv)
        controlDiv.append(destinationSliderDiv)
    }

    private fun chooseUserFaction(fact: Faction) {
        closePopup()
        val pauseButton = document.getElementById(PAUSE_BUTTON_ID) as HTMLButtonElement
        pauseButton.addClass(fact.abbr.toLowerCase())
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
        return pos.x > area.from.x && pos.x <= area.to.x &&
                pos.y > area.from.y && pos.y <= area.to.y
    }

    private fun isInOsmArea(pos: Pos): Boolean {
        val w = Dim.width
        val area = Line(Pos(w - 280, Dim.height - 30), Pos(w, Dim.height))
        return pos.x > area.from.x && pos.x <= area.to.x &&
                pos.y > area.from.y && pos.y <= area.to.y
    }

    private fun handleMouseClick(event: Event) {
        if (event is MouseEvent) {
            val pos = findMousePosition(World.uiCan, event)
            when {
                pos.hasClosePortalForClick() -> {
                    if (World.countPortals() > Config.minPortals) {
                        SoundUtil.playPortalRemovalSound(pos)
                        document.defaultView?.setTimeout(pos.findClosestPortal().remove(), 0)
                    } else {
                        SoundUtil.playFailSound()
                    }
                }
                pos.isBuildable() -> {
                    if (World.countPortals() < Config.maxPortals) {
                        document.defaultView?.setTimeout(World.allPortals.add(Portal.create(pos)), 0)
                    } else {
                        SoundUtil.playFailSound()
                    }
                }
                else -> {
                }
            }
        } else {
            console.warn("Unhandled event: $event.")
        }
    }

    private fun handleMouseMove(event: Event) {
        val pos = findMousePosition(World.uiCan, event as MouseEvent)
        if (ActionLimitsDisplay.isBlocked(pos)) {
            World.mousePos = null
            World.uiCan.addClass("unclickable")
        } else {
            World.mousePos = pos
            World.uiCan.removeClass("unclickable")
        }
    }

    private fun findMousePosition(canvas: HTMLCanvasElement, mouseEvent: MouseEvent): Pos {
        val rect = canvas.getBoundingClientRect()
        val scaleX = canvas.width / rect.width
        val scaleY = canvas.height / rect.height
        val x = (mouseEvent.clientX - rect.left) * scaleX
        val y = (mouseEvent.clientY - rect.top) * scaleY
        return Pos(x.toInt(), y.toInt())
    }

    private fun maybeWidth(id: String) = document.getElementById(id)?.clientWidth
    private fun maybeHeight(id: String) = document.getElementById(id)?.clientHeight
    fun topActionOffset(): Int = maybeHeight("top-controls") ?: 100
    fun leftSliderWidth() = maybeWidth("left-sliders") ?: 241
    fun leftSliderHeight() = maybeHeight("left-sliders") ?: 217
    fun rightSliderWidth() = maybeWidth("right-sliders") ?: 213
    fun rightSliderHeight() = maybeHeight("right-sliders") ?: 145

    private fun createButton(id: String, className: String, text: String, callback: ((Event) -> Unit)?): HTMLButtonElement {
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
                    VectorFields.draw(newPortal)
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
        World.createNonFaction(callback, Config.maxFor(Faction.NONE))
    }

    private fun createAgentsAndPortals(callback: () -> Unit) = createPortals(fun() { createAgents(callback) })

    fun isShowSatelliteMap() = (document.getElementById("satCheckbox") as HTMLInputElement).checked

    private fun onMapload() =
            fun(grid: Grid) {
                World.grid = grid
                if (World.grid.isEmpty()) {
                    console.error("Grid is empty!")
                }
                DrawUtil.drawGrid()
                createAgentsAndPortals {
                    LoadingText.draw("Ready.")
                    DrawUtil.clearBackground()
                    if (World.userFaction == null) {
                        chooseUserFaction(Faction.random())
                    }
                    createQSliders(World.userFaction!!)
                    resetInterval()
                    World.isReady = true
                    if (isShowSatelliteMap()) {
                        MapUtil.showSatelliteMap()
                    }
                }
            }

    private fun mapChangeHandler() {
        val center: Json = getCenterFromDropdown()
        val name = getLocationNameFromDropdown()
        document.location?.href = createNewUrl(center, name)
    }

    private fun createNewUrl(center: Json, name: String = "unknown"): String {
        val split = center.toString().split(",")
        val lng = split[0]
        val lat = split[1]
        val url = document.location?.href
        val token = Constants.token()
        val target = Constants.targetUrl() + token
        val newUrl = if (url?.contains(token) == true) {
            url.split(token)[0] + token
        } else {
            target
        }
        val fact = World.userFaction?.abbr ?: ""
        return addParameters(newUrl, fact, lng, lat, name, isQuickstart())
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
            val opt = document.createElement("option") as HTMLOptionElement
            opt.text = "Unknown Location"
            opt.value = "[0.0,0.0]"
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

    private fun getFactionFromUrl() =
            Faction.fromString(url().searchParams.get("faction"))
    private fun isQuickstartFromUrl() =
            url().searchParams.get("quickstart")?.toBoolean() ?: false

    private fun addParameters(url: String, faction: String, lng: String, lat: String,
                              name: String, isQs: Boolean): String {
        return "$url?faction=$faction&lng=$lng&lat=$lat&name=$name&quickstart=$isQs"
    }
}
