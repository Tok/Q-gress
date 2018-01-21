import agent.Agent
import agent.Faction
import agent.NonFaction
import config.Dimensions
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ImageData
import portal.Portal
import util.HtmlUtil
import util.MapUtil
import util.PathUtil
import util.Util
import util.data.Cell
import util.data.Complex
import util.data.Coords
import kotlin.browser.window
import kotlin.math.sqrt

typealias Ctx = CanvasRenderingContext2D
typealias Canvas = HTMLCanvasElement
object World {
    var tick: Int = 0
    var isReady = false
    fun reload() = HtmlUtil.load(false)

    lateinit var can: Canvas
    fun ctx() = HtmlUtil.getContext2D(can)

    lateinit var bgCan: Canvas
    fun bgCtx() = HtmlUtil.getContext2D(bgCan)

    lateinit var uiCan: Canvas
    fun uiCtx() = HtmlUtil.getContext2D(uiCan)

    //var center: JSON = MapUtil.INITIAL_MAP_CENTER
    var mousePos: Coords? = null
    var speed: Int = 100

    fun w() = can.width
    fun shadowW() = w() / PathUtil.RESOLUTION
    fun h() = can.height
    fun shadowH() = h() / PathUtil.RESOLUTION
    fun diagonalLength() = sqrt((can.width * can.width).toDouble() + (can.height * can.height)).toInt()
    fun totalArea() = can.width * can.height

    lateinit var noiseMap: Array<DoubleArray>
    lateinit var noiseImage: ImageData
    lateinit var shadowStreetMap: ImageData
    lateinit var grid: Map<Coords, Cell>
    fun passableCells(): Map<Coords, Cell> = grid.filter { it.value.isPassable }
    fun wellPassableCells(): Map<Coords, Cell> = grid.filter { it.value.isPassableInAllDirections() }
    private fun passableOnScreen(): Map<Coords, Cell> = wellPassableCells().filterNot { it.key.isOffGrid() }
    fun passableInActionArea(): Map<Coords, Cell> = passableOnScreen()
            .filterNot { it.key.y * PathUtil.RESOLUTION < Dimensions.topActionOffset }
            .filterNot { it.key.y * PathUtil.RESOLUTION > (window.innerHeight - Dimensions.botActionOffset) }

    val allAgents: MutableSet<Agent> = mutableSetOf()
    fun countAgents() = allAgents.count()
    fun countAgents(fact: Faction) = allAgents.count { it.faction.equals(fact) }

    val allNonFaction: MutableSet<NonFaction> = mutableSetOf()
    fun countNonFaction() = allNonFaction.count()

    val allPortals: MutableList<Portal> = mutableListOf()
    fun enlPortals() = allPortals.filter { it.owner?.faction == Faction.ENL }
    fun resPortals() = allPortals.filter { it.owner?.faction == Faction.RES }
    fun unclaimedPortals() = allPortals.filter { it.owner == null }
    fun factionPortals(fact: Faction) = allPortals.filter { it.owner?.faction == fact }
    fun countPortals() = allPortals.count()
    fun countPortals(fact: Faction) = factionPortals(fact).count()

    fun allLinks() = allPortals.flatMap { it.links }
    fun countLinks() = allLinks().count()
    fun countLinks(fact: Faction) = allLinks().filter { it.owner.faction == fact }.count()

    fun allFields() = allPortals.flatMap { it.fields }
    fun countFields() = allFields().count()
    fun countFields(fact: Faction) = allFields().filter { it.owner.faction == fact }.count()

    fun allLines() = allLinks().map { it.getLine() }

    fun calcTotalMu(fact: Faction) = allFields().filter { it.owner.faction == fact }.map { it.calculateMu() }.sum()

    fun imageDataIndex(x: Int, y: Int, w: Int) = (x + (y * w)) * 4
    fun createNoiseImage(noiseMap: Array<DoubleArray>, w: Int, h: Int, alpha: Double = 1.0): ImageData {
        fun setPixel(imageData: ImageData, x: Int, y: Int, r: Int, g: Int, b: Int) {
            val index = imageDataIndex(x, y, imageData.width)
            imageData.data.set(arrayOf(r.toByte(), b.toByte(), g.toByte(), (Byte.MAX_VALUE * alpha).toByte()), index)
        }
        with(World) {
            val imageData: ImageData = bgCtx().createImageData(w.toDouble(), h.toDouble())
            for (x in 0 until w) {
                for (y in 0 until h) {
                    val rawNoise = noiseMap[x][y]
                    val noisepoint = ((1 + (-1 * rawNoise)) * 0.5 * Byte.MAX_VALUE.toInt()).toInt() //- 96
                    setPixel(imageData, x, y, noisepoint, noisepoint, noisepoint)
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
                    val rawNoise = streetMap.get(imageDataIndex(x, y, imageData.width))
                    val index = imageDataIndex(x, h - 1 - y, imageData.width)
                    imageData.data.set(arrayOf(rawNoise, rawNoise, rawNoise, (Byte.MAX_VALUE * 1.0).toByte()), index)
                }
            }
            return imageData
        }
    }
}
