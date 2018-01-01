import agent.Agent
import agent.Faction
import agent.NonFaction
import config.Config
import config.Time
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import portal.Portal
import util.*
import util.data.Cell
import util.data.Coords
import util.data.Line
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.addClass
import kotlin.dom.removeClass


fun main(args: Array<String>) {
    val FROG_COUNT_ID = "numberOfFrogs"
    val SMURF_COUNT_ID = "numberOfSmurfs"
    val SPEED_ID = "speed"

    val rootDiv = document.getElementById("root") as HTMLDivElement
    rootDiv.addClass("container")

    //Prepare all canvas..
    World.can = HtmlUtil.createCanvas("mainCanvas")
    World.bgCan = HtmlUtil.createCanvas("backgroundCanvas")
    World.uiCan = HtmlUtil.createCanvas("uiCanvas")
    World.uiCan.addEventListener("click", { event -> handleMouseClick(event) }, false)

    val canvasDiv = document.createElement("div") as HTMLDivElement
    canvasDiv.append(World.uiCan)
    canvasDiv.append(World.bgCan)
    canvasDiv.append(World.can)

    val w = World.can.width
    val h = World.can.height

    //Init world..
    val noiseAlpha = 0.8
    World.noiseMap = ImprovedNoise.generateEdgeMap(w, h)
    World.noiseImage = World.createNoiseImage(World.noiseMap, w, h, noiseAlpha)

    val frogs: MutableSet<Agent> = mutableSetOf()
    val smurfs: MutableSet<Agent> = mutableSetOf()

    fun speedSetting(): Int = (document.getElementById(SPEED_ID) as HTMLInputElement).valueAsNumber.toInt()
    fun frogCount(): Int = (document.getElementById(FROG_COUNT_ID) as HTMLInputElement).valueAsNumber.toInt()
    fun smurfCount(): Int = (document.getElementById(SMURF_COUNT_ID) as HTMLInputElement).valueAsNumber.toInt()

    fun updateAgents(agents: MutableSet<Agent>, faction: Faction, nextAgents: Set<Agent>) {
        agents.clear()
        agents.addAll(nextAgents.filter { it.faction == faction })
    }

    fun updateAgentCount(agents: MutableSet<Agent>, newCount: Int, creationFuncion: (Int) -> Agent) {
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

    fun tick() {
        if (!World.isReady) {
            return
        }
        World.allAgents.clear()
        updateAgentCount(frogs, frogCount(), { _ -> Agent.createFrog(World.grid) })
        updateAgentCount(smurfs, smurfCount(), { _ -> Agent.createSmurf(World.grid) })
        World.allAgents.sortedBy { Util.random() } //TODO remove if necessary

        val nextAgents = World.allAgents.map { it.act() }.toSet() //actual tick execution
        updateAgents(frogs, Faction.ENL, nextAgents)
        updateAgents(smurfs, Faction.RES, nextAgents)

        World.allNonFaction.forEach { it.act() }

        window.requestAnimationFrame({
            DrawUtil.redraw()
            DrawUtil.redrawUserInterface()
        })
        World.tick++
    }

    fun createPortals(callback: () -> Unit) {
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
        //DrawUtil.drawLoadingText(noiseImage, "Creating Portals..")
        createPortal(callback, Config.startPortals)
    }

    fun createAgents(callback: () -> Unit) {
        val batchSize = 1
        fun createNonFaction(callback: () -> Unit, count: Int) {
            document.defaultView?.setTimeout(fun() {
                if (count > 0) {
                    val realSize = kotlin.math.min(batchSize, count)
                    val total = Config.startNonFaction
                    val realCount = total - count + realSize
                    DrawUtil.drawLoadingText("Creating Non-Faction ($realCount/$total)")
                    (0..realSize).forEach {
                        val newNonFaction = NonFaction.create(World.grid)
                        World.allNonFaction.add(newNonFaction)
                    }
                    createNonFaction(callback, count - realSize)
                } else {
                    callback()
                }
            }, 0)
        }
        //DrawUtil.drawLoadingText(noiseImage, "Creating Non-Faction..")
        DrawUtil.clearBackground()
        createNonFaction(callback, Config.startNonFaction)
    }

    fun createAgentsAndPortals(callback: () -> Unit) = createPortals(fun() { createAgents(callback) })
    rootDiv.append(canvasDiv)

    val controlDiv = document.createElement("div") as HTMLDivElement
    controlDiv.addClass("controls")
    var intervalID: Int = if (Config.isAutostart) {
        document.defaultView?.setInterval({ tick() }, Time.minTickInterval) ?: 0
    } else 0
    with(Config) {
        val maxSpeed = 500
        val speedSlider = HtmlUtil.createSliderDiv("speedSlider", 100, maxSpeed, SPEED_ID, "% Speed", 100)
        speedSlider.oninput = { World.speed = speedSetting(); Unit }
        controlDiv.append(speedSlider)
        controlDiv.append(HtmlUtil.createSliderDiv("frogSlider", startFrogs, maxFrogs,
                FROG_COUNT_ID, " Frogs", 0))
        controlDiv.append(HtmlUtil.createSliderDiv("smurfSlider", startSmurfs, maxSmurfs,
                SMURF_COUNT_ID, " Smurfs", 0))
        controlDiv.append(HtmlUtil.createButtonDiv("button", "Pause", {
            intervalID = HtmlUtil.pauseHandler(intervalID, { tick() })
        }))
    }
    rootDiv.append(controlDiv)

    controlDiv.addEventListener("mousemove", { event -> handleMouseMove(event) }, false)
    rootDiv.addEventListener("mousemove", { event -> handleMouseMove(event) }, false)

    MapUtil.loadMap(fun(grid: Map<Coords, Cell>) {
        World.grid = grid
        if (World.grid.isEmpty()) {
            println("ERROR: Grid is empty!")
        }
        DrawUtil.drawGrid()
        createAgentsAndPortals({
            DrawUtil.drawLoadingText("Ready.")
            World.isReady = true
        })
    })

    window.addEventListener("resize", { document.location?.reload() /* FIXME */ }, false)
}

fun isNotHandledByCanvas(pos: Coords) = isInPositionArea(pos) || isInMapboxArea(pos) || isInOsmArea(pos)

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

fun handleMouseClick(event: Event) {
    if (event is MouseEvent) {
        val pos = findMousePosition(World.uiCan, event)
        if (pos.hasClosePortal()) {
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

fun handleMouseMove(event: Event) {
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

fun findMousePosition(canvas: HTMLCanvasElement, mouseEvent: MouseEvent): Coords {
    val rect = canvas.getBoundingClientRect()
    val scaleX = canvas.width / rect.width
    val scaleY = canvas.height / rect.height
    val x = (mouseEvent.clientX - rect.left) * scaleX
    val y = (mouseEvent.clientY - rect.top) * scaleY
    return Coords(x.toInt(), y.toInt())
}
