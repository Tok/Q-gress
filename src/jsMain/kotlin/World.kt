import agent.Agent
import agent.Faction
import agent.NonFaction
import config.Config
import config.Sim
import extension.Canvas
import extension.Grid
import extension.clear
import kotlinx.browser.document
import kotlinx.dom.clear
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.ImageData
import portal.Portal
import util.HtmlUtil
import util.Util
import util.data.Pos
import util.ui.LoadingOverlay
import kotlin.math.sqrt

object World {
    var tick: Int = 0
    var isReady = false
    var userFaction: Faction? = null

    fun userFactionOrThrow(): Faction = requireNotNull(userFaction) { "user faction has not been chosen yet" }

    lateinit var can: Canvas
    fun ctx() = HtmlUtil.getContext2D(can)

    lateinit var bgCan: Canvas
    fun bgCtx() = HtmlUtil.getContext2D(bgCan)

    lateinit var uiCan: Canvas
    fun uiCtx() = HtmlUtil.getContext2D(uiCan)

    fun resetAllCanvas() {
        can.clear()
        ctx().clear(can)
        bgCan.clear()
        bgCtx().clear(bgCan)
        uiCan.clear()
        uiCtx().clear(uiCan)
    }

    // var center: JSON = MapUtil.INITIAL_MAP_CENTER
    var mousePos: Pos? = null

    // Screen (canvas) extent — HUD/drawing.
    fun w() = can.width
    fun h() = can.height

    // Simulation/grid extent (Sim, larger than the screen) — world bounds & grid cells.
    fun simW() = Sim.width
    fun simH() = Sim.height
    fun shadowW() = Sim.width / Pos.res
    fun shadowH() = Sim.height / Pos.res
    fun diagonalLength() = sqrt((can.width * can.width).toDouble() + (can.height * can.height)).toInt()
    fun totalArea() = can.width * can.height

    lateinit var noiseMap: Array<DoubleArray>
    lateinit var noiseImage: ImageData
    var shadowStreetMap: ImageData? = null

    lateinit var grid: Grid
    var walkability = 0.0 // fraction of on-screen cells that are passable (set at grid build)

    fun passableCells(): Grid = grid.filter { it.value.isPassable }
    private fun wellPassableCells(): Grid = grid.filter { it.value.isPassableInAllDirections() }
    private fun passableOnScreen(): Grid = wellPassableCells().filterNot { it.key.isOffGrid() }

    // Portals may spawn anywhere passable in the (larger) play area; the old screen-region
    // block was a 2D-era restriction.
    fun passableInActionArea(): Grid = passableOnScreen()

    val allAgents: MutableSet<Agent> = mutableSetOf()

    // Agents recruited mid-tick are buffered here and flushed after the agent
    // loop, so we never mutate allAgents while iterating it (avoids CME).
    val pendingAgents: MutableList<Agent> = mutableListOf()

    fun flushPendingAgents() {
        if (pendingAgents.isNotEmpty()) {
            allAgents.addAll(pendingAgents)
            pendingAgents.clear()
        }
    }

    val frogs = allAgents.filter { it.faction == Faction.ENL }.toSet()
    val smurfs = allAgents.filter { it.faction == Faction.RES }.toSet()
    fun countAgents() = allAgents.count()
    fun countAgents(fact: Faction) = allAgents.count { it.faction == fact }
    fun canRecruitMore(fact: Faction) = countAgents(fact) < Config.maxFrogs

    val allNonFaction: MutableSet<NonFaction> = mutableSetOf()
    fun countNonFaction() = allNonFaction.count()

    val allPortals: MutableList<Portal> = mutableListOf()
    fun randomPortal() = allPortals[(Util.random() * (World.allPortals.size - 1)).toInt()]
    fun enlPortals() = allPortals.filter { it.owner?.faction == Faction.ENL }
    fun resPortals() = allPortals.filter { it.owner?.faction == Faction.RES }
    fun unclaimedPortals() = allPortals.filter { it.owner == null }
    fun factionPortals(fact: Faction) = allPortals.filter { it.owner?.faction == fact }
    fun countPortals() = allPortals.count()
    fun countPortals(fact: Faction) = factionPortals(fact).count()

    fun allLinks() = allPortals.flatMap { it.links }
    fun countLinks() = allLinks().count()
    fun countLinks(fact: Faction) = allLinks().filter { it.creator.faction == fact }.count()

    fun allFields() = allPortals.flatMap { it.fields }
    fun countFields() = allFields().count()
    fun countFields(fact: Faction) = allFields().filter { it.owner.faction == fact }.count()

    fun allLines() = allLinks().map { it.getLine() }

    fun calcTotalMu(fact: Faction) = allFields().filter { it.owner.faction == fact }.map { it.calculateMu() }.sum()

    private fun imageDataIndex(x: Int, y: Int, w: Int) = (x + (y * w)) * 4
    fun createNoiseImage(noiseMap: Array<DoubleArray>, w: Int, h: Int, alpha: Double = 1.0): ImageData {
        fun setPixel(imageData: ImageData, x: Int, y: Int, r: Int, g: Int, b: Int) {
            val index = imageDataIndex(x, y, imageData.width)
            imageData.data.set(arrayOf(r.toByte(), b.toByte(), g.toByte(), (Byte.MAX_VALUE * alpha).toInt().toByte()), index)
        }
        with(World) {
            val imageData: ImageData = bgCtx().createImageData(w.toDouble(), h.toDouble())
            for (x in 0 until w) {
                for (y in 0 until h) {
                    val rawNoise = noiseMap[x][y]
                    val noisePoint = ((1 + (-1 * rawNoise)) * 0.5 * Byte.MAX_VALUE.toInt()).toInt() // - 96
                    setPixel(imageData, x, y, noisePoint, noisePoint, noisePoint)
                }
            }
            return imageData
        }
    }

    fun createStreetImage(streetMap: Uint8Array, w: Int, h: Int): ImageData {
        with(World) {
            val imageData: ImageData = bgCtx().createImageData(w.toDouble(), h.toDouble())
            for (x in 0 until w) {
                for (y in 0 until h) {
                    val rawNoise = streetMap[imageDataIndex(x, y, imageData.width)]
                    val index = imageDataIndex(x, h - 1 - y, imageData.width)
                    imageData.data.set(arrayOf(rawNoise, rawNoise, rawNoise, Byte.MAX_VALUE), index)
                }
            }
            return imageData
        }
    }

    fun createNonFaction(callback: () -> Unit, count: Int) {
        document.defaultView?.setTimeout(fun() {
            if (count > 0) {
                val total = Config.maxFor()
                LoadingOverlay.building(LoadingOverlay.PCT_PEOPLE, 100, total - count + 1, total, "Creating people")
                val newNonFaction = NonFaction.create(World.grid)
                World.allNonFaction.add(newNonFaction)
                createNonFaction(callback, count - 1)
            } else {
                callback()
            }
        }, 0)
    }
}
