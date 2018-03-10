package util

import Canvas
import Ctx
import ImprovedNoise
import World
import agent.Agent
import agent.Faction
import agent.QValue
import config.*
import config.Location
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.url.URL
import portal.Portal
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
    var intervalID = 0

    val FROG_COUNT_ID = "numberOfFrogs"
    val SMURF_COUNT_ID = "numberOfSmurfs"
    val SPEED_ID = "speed"
    val PAUSE_BUTTON_ID = "pauseButton"
    val LOCATION_DROPDOWN_ID = "locationSelect"
    val VOLUME_SLIDER_ID = "volumeSlider"

    private fun speedSetting(): Int = (document.getElementById(SPEED_ID) as HTMLInputElement).valueAsNumber.toInt()
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
        updateAgentCount(World.frogs, frogCount(), { _ -> Agent.createFrog(World.grid) })
        updateAgentCount(World.smurfs, smurfCount(), { _ -> Agent.createSmurf(World.grid) })
        World.allAgents.sortedBy { Util.random() } //TODO remove if necessary

        val nextAgents = World.allAgents.map { it.act() }.toSet() //actual tick execution
        updateAgents(World.frogs, Faction.ENL, nextAgents)
        updateAgents(World.smurfs, Faction.RES, nextAgents)

        World.allNonFaction.forEach { it.act() }

        window.requestAnimationFrame({
            DrawUtil.redraw()
            DrawUtil.redrawUserInterface()
        })
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

        val canvasDiv = document.createElement("div") as HTMLDivElement
        canvasDiv.append(World.uiCan)
        canvasDiv.append(World.bgCan)
        canvasDiv.append(World.can)

        rootDiv.append(canvasDiv)

        val controlDiv = document.createElement("div") as HTMLDivElement
        controlDiv.addClass("controls")

        val speed = 100
        val minSpeed = 100
        val maxSpeed = 300
        val speedSlider = createSliderDiv("speedSlider", speed, maxSpeed, SPEED_ID, "% Speed", minSpeed)
        speedSlider.oninput = { World.speed = speedSetting(); Unit }
        controlDiv.append(speedSlider)
        controlDiv.append(createSliderDiv("frogSlider", Config.startFrogs, Config.maxFrogs,
                FROG_COUNT_ID, " Frogs", 0))
        controlDiv.append(createSliderDiv("smurfSlider", Config.startSmurfs, Config.maxSmurfs,
                SMURF_COUNT_ID, " Smurfs", 0))
        val buttonDiv = document.createElement("div") as HTMLDivElement
        val pauseButton = createButton("button", "Stop", {
            intervalID = pauseHandler(intervalID, { tick() })
        })
        pauseButton.id = PAUSE_BUTTON_ID
        buttonDiv.append(pauseButton)

        val dropDown = createDropdown(LOCATION_DROPDOWN_ID, { mapChangeHandler() })
        val selectionName = getLocationNameFromUrl() ?: "unknown"
        setLocationDropdownSelection(dropDown, selectionName)
        buttonDiv.append(dropDown)

        val volumeSlider = document.createElement("input") as HTMLInputElement
        with(volumeSlider) {
            id = VOLUME_SLIDER_ID
            type = "range"
            min = "0"
            max = "100"
            value = "80"
            addClass("slider", "volumeSlider")
            val volumeSliderValue = document.createElement("span") as HTMLSpanElement
            volumeSliderValue.addClass("label")
            oninput = { _ -> volumeSliderValue.innerHTML = value + "% SOUND VOLUME"; null }
            volumeSliderValue.innerHTML = value + "% VOLUME"
            buttonDiv.append(volumeSlider)
            buttonDiv.append(volumeSliderValue)
        }

        val satCheckbox = document.createElement("input") as HTMLInputElement
        satCheckbox.type = "checkbox"
        satCheckbox.addClass("checkbox")
        satCheckbox.onchange = { if (satCheckbox.checked) MapUtil.showSateliteMap() else MapUtil.hideSateliteMap() }
        buttonDiv.append(satCheckbox)
        val label = document.createElement("span") as HTMLSpanElement
        label.addClass("label")
        label.id = "satCheckLabel"
        label.innerHTML = "Satellite Map"
        buttonDiv.append(label)

        controlDiv.append(buttonDiv)

        val qDiv = document.createElement("div") as HTMLDivElement
        qDiv.addClass("qValues")
        QValue.values().forEach { qValue ->
            val sliderDiv = document.createElement("div") as HTMLDivElement
            Faction.factionValues().forEach { faction ->
                val slider = document.createElement("input") as HTMLInputElement
                slider.id = qValue.sliderId + faction.nickName
                slider.type = "range"
                slider.min = "0.00"
                slider.max = "1.00"
                slider.step = "0.01"
                slider.value = "0.50"
                slider.addClass("slider", "qSlider", faction.abbr.toLowerCase() + "Slider")
                val sliderValue = document.createElement("span") as HTMLSpanElement
                sliderValue.addClass("qSliderLabel", faction.abbr.toLowerCase() + "Label")
                slider.oninput = { _ -> sliderValue.innerHTML = qDisplay(slider.value); null }
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
        controlDiv.append(qDiv)

        rootDiv.append(controlDiv)
        controlDiv.addEventListener("mousemove", { event -> handleMouseMove(event) }, false)
        rootDiv.addEventListener("mousemove", { event -> handleMouseMove(event) }, false)

        initWorld()
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
        val w = Dimensions.width
        val h = Dimensions.height
        World.noiseMap = ImprovedNoise.generateEdgeMap(w, h)
        World.noiseImage = World.createNoiseImage(World.noiseMap, w, h, noiseAlpha)
        resetInterval()
        World.resetAllCanvas()
        val maybeCenter = getSelectedCenterFromUrl()
        val center = if (!maybeCenter.toString().equals("0,0")) maybeCenter else Location.random().toJSON()
        MapUtil.loadMaps(center, onMapload())
    }

    private fun resetInterval() {
        intervalID = if (Config.isAutostart) {
            document.defaultView?.setInterval({ tick() }, Time.minTickInterval) ?: 0
        } else 0
    }

    fun pauseHandler(intervalID: Int, tickFunction: () -> Unit): Int {
        val pauseButton = document.getElementById(PAUSE_BUTTON_ID) as HTMLButtonElement
        if (intervalID != -1) {
            pauseButton.innerText = "Start"
            document.defaultView?.clearInterval(intervalID)
            return -1
        } else {
            pauseButton.innerText = "Stop"
            return document.defaultView?.setInterval({ tickFunction() }, Time.minTickInterval) ?: 0
        }
    }

    fun isBlockedByMapbox(pos: Coords) = isInMapboxArea(pos) || isInOsmArea(pos)
    fun isBlockedForVector(pos: Coords) = isBlockedByMapbox(pos)

    private fun isInPositionArea(pos: Coords): Boolean {
        val w = Dimensions.width
        val size = 52
        val area = Line(Coords(w - size, 0), Coords(w, size))
        return pos.x > area.from.x && pos.x <= area.to.x &&
                pos.y > area.from.y && pos.y <= area.to.y
    }

    private fun isInMapboxArea(pos: Coords): Boolean {
        val area = Line(Coords(-20, Dimensions.height - 40), Coords(90, Dimensions.height))
        return pos.x > area.from.x && pos.x <= area.to.x &&
                pos.y > area.from.y && pos.y <= area.to.y
    }

    private fun isInOsmArea(pos: Coords): Boolean {
        val w = Dimensions.width
        val area = Line(Coords(w - 280, Dimensions.height - 30), Coords(w, Dimensions.height))
        return pos.x > area.from.x && pos.x <= area.to.x &&
                pos.y > area.from.y && pos.y <= area.to.y
    }

    private fun handleMouseClick(event: Event) {
        if (event is MouseEvent) {
            val pos = findMousePosition(World.uiCan, event)
            if (pos.hasClosePortalForClick()) {
                SoundUtil.playPortalRemovalSound(pos)
                document.defaultView?.setTimeout(pos.findClosestPortal().destroy(World.tick), 0)
            } else if (pos.isBuildable()) {
                SoundUtil.playPortalCreationSound(pos)
                document.defaultView?.setTimeout(World.allPortals.add(Portal.create(pos)), 0)
            } else {
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
        slider.oninput = { _ -> sliderValue.innerHTML = slider.value + suffix; null }
        div.appendChild(slider)
        div.appendChild(sliderValue)
        sliderValue.innerHTML = slider.value + suffix
        return div
    }

    private fun createButton(className: String, text: String, callback: ((Event) -> Unit)?): HTMLButtonElement {
        val button = document.createElement("BUTTON") as HTMLButtonElement
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
        canvas.width = Dimensions.width
        canvas.height = Dimensions.height
        return canvas
    }

    private fun createOffscreenCanvas(w: Int, h: Int): Canvas {
        val canvas = document.createElement("canvas") as Canvas
        canvas.width = w
        canvas.height = h
        return canvas
    }

    fun prerender(w: Int, h: Int, drawFun: (CanvasRenderingContext2D) -> Unit): Canvas {
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

    private fun onMapload() =
            fun(grid: Map<Coords, Cell>) {
                World.grid = grid
                if (World.grid.isEmpty()) {
                    println("ERROR: Grid is empty!")
                }
                DrawUtil.drawGrid()
                DrawUtil.drawActionLimits(false)
                createAgentsAndPortals({
                    DrawUtil.drawLoadingText("Ready.")
                    World.isReady = true
                })
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
        val newUrl = if (url?.contains(token) ?: false) {
            url?.split(token)!![0] + token
        } else {
            target
        }
        return addParameters(newUrl, lng, lat, name)
    }

    fun isLocal() = document.location?.href?.contains("localhost") ?: false

    private fun getCenterFromDropdown(): Json {
        val dropdown = document.getElementById(LOCATION_DROPDOWN_ID) as HTMLSelectElement
        val selection = dropdown.get(dropdown.selectedIndex) as HTMLOptionElement
        return JSON.parse(selection.value)
    }

    private fun getLocationNameFromDropdown(): String {
        val dropdown = document.getElementById(LOCATION_DROPDOWN_ID) as HTMLSelectElement
        val selection = dropdown.get(dropdown.selectedIndex) as HTMLOptionElement
        return selection.text
    }

    private fun setLocationDropdownSelection(dropdown: HTMLSelectElement, name: String) {
        val cleanName = name.replace("%20", " ")
        var hasMatch = false
        (0..dropdown.options.length - 1).forEach {
            val option = dropdown.options[it] as HTMLOptionElement
            if (option.label.equals(cleanName)) {
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
        return if (geo != null) geo.toJson() else getCenterFromDropdown()
    }

    private fun getLocationNameFromUrl(): String? {
        val url = URL(document.location?.href ?: "")
        return url.searchParams.get("name")
    }

    private fun getLngLatFromUrl(): GeoCoords? {
        val url = URL(document.location?.href ?: "")
        val lngString = url.searchParams.get("lng")
        val latString = url.searchParams.get("lat")
        return if (lngString != null && latString != null) {
            GeoCoords(lngString.toDouble(), latString.toDouble())
        } else {
            null
        }
    }

    private fun addParameters(url: String, lng: String, lat: String, name: String): String {
        return url + "?lng=" + lng + "&lat=" + lat + "&name=" + name
    }
}
