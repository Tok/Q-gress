package util

import Canvas
import Ctx
import ImprovedNoise
import World
import agent.Agent
import agent.Faction
import config.Config
import config.Location
import config.Time
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

        with(Config) {
            val maxSpeed = 500
            val speedSlider = createSliderDiv("speedSlider", 100, maxSpeed, SPEED_ID, "% Speed", 100)
            speedSlider.oninput = { World.speed = speedSetting(); Unit }
            controlDiv.append(speedSlider)
            controlDiv.append(createSliderDiv("frogSlider", startFrogs, maxFrogs,
                    FROG_COUNT_ID, " Frogs", 0))
            controlDiv.append(createSliderDiv("smurfSlider", startSmurfs, maxSmurfs,
                    SMURF_COUNT_ID, " Smurfs", 0))
            val buttonDiv = document.createElement("div") as HTMLDivElement
            buttonDiv.append(createButton("button", "Pause", {
                intervalID = pauseHandler(intervalID, { tick() })
            }))
            val dropDown = createDropdown("locationSelect", { mapChangeHandler() })
            val selectionName = getLocationNameFromUrl() ?: "unknown"
            setLocationDropdownSelection(dropDown, selectionName)
            buttonDiv.append(dropDown)
            controlDiv.append(buttonDiv)
        }
        rootDiv.append(controlDiv)

        controlDiv.addEventListener("mousemove", { event -> handleMouseMove(event) }, false)
        rootDiv.addEventListener("mousemove", { event -> handleMouseMove(event) }, false)

        window.addEventListener("resize", { document.location?.reload() /* FIXME */ }, false)

        initWorld()
    }

    private fun initWorld() {
        val noiseAlpha = 0.8
        val w = World.can.width
        val h = World.can.height
        World.noiseMap = ImprovedNoise.generateEdgeMap(w, h)
        World.noiseImage = World.createNoiseImage(World.noiseMap, w, h, noiseAlpha)
        resetInterval()
        World.resetAllCanvas()
        MapUtil.loadMaps(getSelectedCenterFromUrl(), onMapload())
    }

    private fun resetInterval() {
        intervalID = if (Config.isAutostart) {
            document.defaultView?.setInterval({ tick() }, Time.minTickInterval) ?: 0
        } else 0
    }

    private fun isNotHandledByCanvas(pos: Coords) = isInPositionArea(pos) || isInMapboxArea(pos) || isInOsmArea(pos)

    private fun isInPositionArea(pos: Coords): Boolean {
        val w = World.can.width
        val size = 52
        val area = Line(Coords(w - size, 0), Coords(w, size))
        return pos.x > area.from.x && pos.x <= area.to.x &&
                pos.y > area.from.y && pos.y <= area.to.y
    }

    private fun isInMapboxArea(pos: Coords): Boolean {
        val area = Line(Coords(0, World.can.height - 21), Coords(233, World.can.height))
        return pos.x > area.from.x && pos.x <= area.to.x &&
                pos.y > area.from.y && pos.y <= area.to.y
    }

    private fun isInOsmArea(pos: Coords): Boolean {
        val w = World.can.width
        val area = Line(Coords(w - 377, World.can.height - 34), Coords(w, World.can.height))
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
        val isNotHandledByCanvas = isNotHandledByCanvas(pos)
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
        sliderValue.addClass("sliderLabel")
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
        canvas.width = window.innerWidth
        canvas.height = window.innerHeight
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

    private fun pauseHandler(intervalID: Int, tickFunction: () -> Unit): Int {
        if (intervalID != -1) {
            document.defaultView?.clearInterval(intervalID)
            return -1
        } else {
            return document.defaultView?.setInterval({ tickFunction() }, Time.minTickInterval) ?: 0
        }
    }

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
        val lng = center.toString().split(",")[0]
        val lat = center.toString().split(",")[1]
        val currentUrl = document.location?.href
        val isLocal = currentUrl?.contains("localhost") ?: false
        val token = if (isLocal) { Config.localToken } else { Config.token }
        val targetUrl = if (isLocal) { Config.localLocation } else { Config.location }
        val url = if (currentUrl?.contains(token) ?: false) { currentUrl?.split(token)!![0] + token } else { targetUrl + token }
        return addParameters(url, lng, lat, name)
    }

    private fun getCenterFromDropdown(): Json {
        val select = document.getElementById("locationSelect") as HTMLSelectElement
        val selection = select.get(select.selectedIndex) as HTMLOptionElement
        selection.text
        return JSON.parse(selection.value)
    }

    private fun getLocationNameFromDropdown(): String {
        val select = document.getElementById("locationSelect") as HTMLSelectElement
        val selection = select.get(select.selectedIndex) as HTMLOptionElement
        return selection.text
    }

    private fun setLocationDropdownSelection(dropdown: HTMLSelectElement, name: String) {
        val cleanName = name.replace("%20", " ")
        (0..dropdown.options.length - 1).forEach {
            val option = dropdown.options[it] as HTMLOptionElement
            if (option.label.equals(cleanName)) {
                dropdown.selectedIndex = it
            }
        }
    }

    private fun getSelectedCenterFromUrl(): Json {
        val geo: GeoCoords? = getLngLatFromUrl()
        return if (geo != null) {
            geo.toJson()
        } else {
            return getCenterFromDropdown()
        }
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
