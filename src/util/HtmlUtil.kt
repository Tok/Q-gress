package util

import Canvas
import Ctx
import ImprovedNoise
import World
import agent.Agent
import agent.Faction
import agent.qvalue.QActions
import agent.qvalue.QDestinations
import agent.qvalue.QValue
import config.*
import config.Location
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.url.URL
import portal.Portal
import system.Cycle
import util.data.Cell
import util.data.Coords
import util.data.GeoCoords
import util.data.Line
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.addClass
import kotlin.dom.removeClass
import kotlin.js.Json

object HtmlUtil {
    private var intervalID = 0

    private const val FROG_COUNT_ID = "numberOfFrogs"
    private const val SMURF_COUNT_ID = "numberOfSmurfs"
    private const val PAUSE_BUTTON_ID = "pauseButton"
    private const val LOCATION_DROPDOWN_ID = "locationSelect"
    const val SOUND_CHECKBOX_ID = "soundCheckbox"

    private fun frogCount(): Int = (document.getElementById(FROG_COUNT_ID) as HTMLInputElement).valueAsNumber.toInt()
    private fun smurfCount(): Int = (document.getElementById(SMURF_COUNT_ID) as HTMLInputElement).valueAsNumber.toInt()

    private fun updateAgents(agents: MutableSet<Agent>, faction: Faction, nextAgents: Set<Agent>) {
        agents.clear()
        agents.addAll(nextAgents.filter { it.faction == faction })
    }

    private fun updateAgentCount(agents: MutableSet<Agent>, newCount: Int, creationFuncion: (Int) -> Agent) {
        if (newCount < agents.size) {
            World.allAgents.addAll(agents.take(newCount))
        } else {
            World.allAgents.addAll(agents)
            if (newCount > agents.size) {
                val diff = newCount - agents.size
                World.allAgents.addAll((1..diff).map { creationFuncion(it) }.toSet())
            }
        }
    }

    private fun tick() {
        if (!World.isReady) {
            return
        }
        World.allAgents.clear()
        updateAgentCount(World.frogs, frogCount()) { Agent.createFrog(World.grid) }
        updateAgentCount(World.smurfs, smurfCount()) { Agent.createSmurf(World.grid) }

        val nextAgents = World.allAgents.map { it.act() }.toSet()
        updateAgents(World.frogs, Faction.ENL, nextAgents)
        updateAgents(World.smurfs, Faction.RES, nextAgents)
        World.allNonFaction.forEach { it.act() }
        window.requestAnimationFrame {
            DrawUtil.redraw()
            val enlMu = World.calcTotalMu(Faction.ENL)
            val resMu = World.calcTotalMu(Faction.RES)
            Cycle.updateCheckpoints(World.tick, enlMu, resMu)
            DrawUtil.redrawUserInterface(World.tick, enlMu, resMu)
        }
        World.tick++
    }

    fun load() {
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
        val pauseButton = createButton(PAUSE_BUTTON_ID, "button", "Stop") {
            intervalID = pauseHandler(intervalID) { tick() }
        }
        buttonDiv.append(pauseButton)

        val dropDown = createDropdown(LOCATION_DROPDOWN_ID) { mapChangeHandler() }
        val selectionName = getLocationNameFromUrl() ?: "unknown"
        setLocationDropdownSelection(dropDown, selectionName)
        buttonDiv.append(dropDown)

        buttonDiv.append(createSoundSpan())
        buttonDiv.append(createSatSpan())
        controlDiv.append(buttonDiv)

        val actionSliderDiv = createSliderDiv("left-sliders", QActions.values(), "floatLeft", "Actions")
        controlDiv.append(actionSliderDiv)
        val destinationSliderDiv = createSliderDiv("right-sliders", QDestinations.values(), "floatRight", "Destinations")
        controlDiv.append(destinationSliderDiv)

        rootDiv.append(controlDiv)
        controlDiv.addEventListener("mousemove", { event -> handleMouseMove(event) }, false)
        rootDiv.addEventListener("mousemove", { event -> handleMouseMove(event) }, false)
        initWorld()
    }

    private fun createSoundSpan(): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        val checkbox = document.createElement("input") as HTMLInputElement
        checkbox.id = SOUND_CHECKBOX_ID
        checkbox.type = "checkbox"
        checkbox.checked = true
        checkbox.addClass("checkbox")
        span.append(checkbox)
        val label = document.createElement("span") as HTMLSpanElement
        label.addClass("label")
        label.id = "soundLabel"
        label.innerHTML = "Sound"
        span.append(label)
        return span
    }

    private fun createSatSpan(): HTMLSpanElement {
        val span = document.createElement("span") as HTMLSpanElement
        val checkbox = document.createElement("input") as HTMLInputElement
        checkbox.id = "satCheckbox"
        checkbox.type = "checkbox"
        checkbox.checked = true
        checkbox.addClass("checkbox")
        checkbox.onchange = { if (checkbox.checked) MapUtil.showSatelliteMap() else MapUtil.hideSatelliteMap() }
        span.append(checkbox)
        val label = document.createElement("span") as HTMLSpanElement
        label.addClass("label")
        label.id = "satLabel"
        label.innerHTML = "Satellite"
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
        div.append(createSliderDiv("frogSlider", Config.startFrogs, Config.maxFrogs,
                FROG_COUNT_ID, " Frogs", 0))
        div.append(createSliderDiv("smurfSlider", Config.startSmurfs, Config.maxSmurfs,
                SMURF_COUNT_ID, " Smurfs", 0))
        return div
    }

    private fun createSliderDiv(id: String, qValues: List<QValue>, className: String, labelText: String): HTMLDivElement {
        val qDiv = document.createElement("div") as HTMLDivElement
        qDiv.id = id
        qDiv.addClass("qValues", "halfWidth", className)
        val destinationsLabel = document.createElement("div") as HTMLDivElement
        destinationsLabel.addClass("label", "qTitle")
        destinationsLabel.innerHTML = labelText
        qDiv.append(destinationsLabel)
        qValues.forEach { qValue ->
            val sliderDiv = document.createElement("div") as HTMLDivElement
            Faction.all().forEach { faction ->
                val slider = document.createElement("input") as HTMLInputElement
                slider.id = qValue.sliderId + faction.nickName
                slider.type = "range"
                slider.min = "0.00"
                slider.max = "1.00"
                slider.step = "0.01"
                slider.value = "0.20"
                slider.addClass("slider", "qSlider", faction.abbr.toLowerCase() + "Slider")
                val sliderValue = document.createElement("span") as HTMLSpanElement
                sliderValue.addClass("qSliderLabel", faction.abbr.toLowerCase() + "Label")
                slider.oninput = { sliderValue.innerHTML = qDisplay(slider.value); null }
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
        World.noiseMap = ImprovedNoise.generateEdgeMap(w, h)
        World.noiseImage = World.createNoiseImage(World.noiseMap, w, h, noiseAlpha)
        resetInterval()
        World.resetAllCanvas()
        val maybeCenter = getSelectedCenterFromUrl()
        val center = if (maybeCenter.toString() != "0,0") maybeCenter else Location.random().toJSON()
        MapUtil.loadMaps(center, onMapload())
    }

    private fun resetInterval() {
        intervalID = if (Config.isAutostart) {
            document.defaultView?.setInterval({ tick() }, Time.minTickInterval) ?: 0
        } else 0
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

    private fun isBlockedByMapbox(pos: Coords) = isInMapboxArea(pos) || isInOsmArea(pos)
    fun isBlockedForVector(pos: Coords) = isBlockedByMapbox(pos)

    private fun isInPositionArea(pos: Coords): Boolean {
        val w = Dim.width
        val size = 52
        val area = Line(Coords(w - size, 0), Coords(w, size))
        return pos.x > area.from.x && pos.x <= area.to.x &&
                pos.y > area.from.y && pos.y <= area.to.y
    }

    private fun isInMapboxArea(pos: Coords): Boolean {
        val area = Line(Coords(-20, Dim.height - 40), Coords(90, Dim.height))
        return pos.x > area.from.x && pos.x <= area.to.x &&
                pos.y > area.from.y && pos.y <= area.to.y
    }

    private fun isInOsmArea(pos: Coords): Boolean {
        val w = Dim.width
        val area = Line(Coords(w - 280, Dim.height - 30), Coords(w, Dim.height))
        return pos.x > area.from.x && pos.x <= area.to.x &&
                pos.y > area.from.y && pos.y <= area.to.y
    }

    private fun handleMouseClick(event: Event) {
        if (event is MouseEvent) {
            val pos = findMousePosition(World.uiCan, event)
            when {
                pos.hasClosePortalForClick() -> {
                    SoundUtil.playPortalRemovalSound(pos)
                    document.defaultView?.setTimeout(pos.findClosestPortal().destroy(World.tick), 0)
                }
                pos.isBuildable() -> {
                    document.defaultView?.setTimeout(World.allPortals.add(Portal.create(pos)), 0)
                }
                else -> {
                }
            }
        } else {
            println("WARN: Unhandled event: $event.")
        }
    }

    private fun handleMouseMove(event: Event) {
        val pos = findMousePosition(World.uiCan, event as MouseEvent)
        val isNotHandledByCanvas = isBlockedByMapbox(pos)
        if (isNotHandledByCanvas) {
            World.mousePos = null
            World.uiCan.addClass("unclickable")
        } else {
            World.mousePos = pos
            World.uiCan.removeClass("unclickable")
        }
    }

    private fun findMousePosition(canvas: HTMLCanvasElement, mouseEvent: MouseEvent): Coords {
        val rect = canvas.getBoundingClientRect()
        val scaleX = canvas.width / rect.width
        val scaleY = canvas.height / rect.height
        val x = (mouseEvent.clientX - rect.left) * scaleX
        val y = (mouseEvent.clientY - rect.top) * scaleY
        return Coords(x.toInt(), y.toInt())
    }

    private fun createSliderDiv(className: String, value: Int, max: Int,
                                id: String, suffix: String, min: Int = 0): HTMLDivElement {
        val div = document.createElement("div") as HTMLDivElement
        val slider = document.createElement("INPUT") as HTMLInputElement
        slider.id = id
        slider.type = "range"
        slider.min = min.toString()
        slider.max = max.toString()
        slider.value = value.toString()
        slider.addClass("slider", className)
        val sliderValue = document.createElement("span") as HTMLSpanElement
        sliderValue.addClass("label")
        slider.oninput = { sliderValue.innerHTML = slider.value + suffix; null }
        div.appendChild(slider)
        div.appendChild(sliderValue)
        sliderValue.innerHTML = slider.value + suffix
        return div
    }

    fun topActionOffset(): Int = document.getElementById("top-controls")?.clientHeight ?: 82
    fun leftSliderHeight(): Int = document.getElementById("left-sliders")?.clientHeight ?: 144
    fun leftSliderWidth(): Int = document.getElementById("left-sliders")?.clientWidth ?: 370
    fun rightSliderHeight(): Int = document.getElementById("right-sliders")?.clientHeight ?: 144
    fun rightSliderWidth(): Int = document.getElementById("right-sliders")?.clientWidth ?: 370

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
        World.allPortals.clear()
        fun createPortal(callback: () -> Unit, count: Int) {
            document.defaultView?.setTimeout(fun() {
                if (count > 0) {
                    val total = Config.startPortals
                    val realCount = total - count + 1
                    val newPortal = Portal.createRandom()
                    DrawUtil.drawLoadingText("Creating Portal ($realCount/$total)")
                    DrawUtil.drawVectorField(newPortal)
                    World.allPortals.add(newPortal)
                    createPortal(callback, count - 1)
                } else {
                    callback()
                }
            }, 0)
        }
        DrawUtil.drawLoadingText("Creating Portals..")
        createPortal(callback, Config.startPortals)
    }

    private fun createAgents(callback: () -> Unit) {
        DrawUtil.drawLoadingText("Creating Non-Faction..")
        DrawUtil.clearBackground()
        World.allNonFaction.clear()
        World.createNonFaction(callback, Config.startNonFaction)
    }

    private fun createAgentsAndPortals(callback: () -> Unit) = createPortals(fun() { createAgents(callback) })

    private fun isShowSatelliteMap() = (document.getElementById("satCheckbox") as HTMLInputElement).checked

    private fun onMapload() =
            fun(grid: Map<Coords, Cell>) {
                MapUtil.hideSatelliteMap()
                World.grid = grid
                if (World.grid.isEmpty()) {
                    println("ERROR: Grid is empty!")
                }
                DrawUtil.drawGrid()
                DrawUtil.drawActionLimits(false)
                createAgentsAndPortals {
                    DrawUtil.drawLoadingText("Ready.")
                    World.isReady = true
                    if (isShowSatelliteMap()) {
                        if (isShowSatelliteMap()) {
                            MapUtil.showSatelliteMap()
                        }
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
        return addParameters(newUrl, lng, lat, name)
    }

    fun isLocal() = document.location?.href?.contains("localhost") ?: false

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

    private fun getLocationNameFromUrl(): String? {
        val url = URL(document.location?.href ?: "")
        return url.searchParams.get("name")
    }

    private fun getLngLatFromUrl(): GeoCoords? {
        val url = URL(document.location?.href ?: "")
        val lngString = url.searchParams.get("lng")
        val latString = url.searchParams.get("lat")
        return GeoCoords.fromStrings(lngString, latString)
    }

    private fun addParameters(url: String, lng: String, lat: String, name: String): String {
        return "$url?lng=$lng&lat=$lat&name=$name"
    }
}
