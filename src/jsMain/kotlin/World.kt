import agent.Agent
import agent.Faction
import agent.NonFaction
import config.Config
import config.Dim
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
import system.display.Scene3D
import system.ui.LoadingOverlay
import util.HtmlUtil
import util.Util
import util.data.*
import kotlin.math.sqrt

object World {
    var tick: Int = 0
    var isReady = false
    var userFaction: Faction? = null

    fun userFactionOrThrow(): Faction = requireNotNull(userFaction) { "user faction has not been chosen yet" }

    // Offscreen factory canvas: its 2D context only allocates ImageData buffers for the passability
    // grid readback (see createStreetImage). Never displayed — the world renders in the three.js
    // custom layer and the HUD is DOM, so the old on-screen main/UI canvases are gone.
    lateinit var bgCan: Canvas
    fun bgCtx() = HtmlUtil.getContext2D(bgCan)

    // var center: JSON = MapUtil.INITIAL_MAP_CENTER
    var mousePos: Pos? = null

    // Screen extent — the fixed play area (Dim); used for on/off-screen tests and sound ratios.
    fun w() = Dim.width
    fun h() = Dim.height

    // Simulation/grid extent (Sim, larger than the screen) — world bounds & grid cells.
    fun simW() = Sim.width
    fun simH() = Sim.height
    fun shadowW() = Sim.width / Pos.res
    fun shadowH() = Sim.height / Pos.res
    fun diagonalLength() = sqrt((Dim.width.toDouble() * Dim.width) + (Dim.height.toDouble() * Dim.height)).toInt()
    fun totalArea() = Dim.width * Dim.height

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

    /**
     * Safety net for link/field integrity: drop any link or field that has become invalid — an endpoint
     * portal vanished from the board, went neutral, or no longer shares the others' faction (e.g. it was
     * virus-flipped the same moment a link was being made). The normal teardown
     * ([portal.Portal.destroy]/[portal.Portal.refactor]) already does this; this guards against any path
     * that slips past it, so a link can never dangle to a portal that's gone or now enemy. Run each tick.
     */
    fun pruneInvalidLinksAndFields() {
        val present = allPortals.toHashSet()
        allPortals.forEach { portal ->
            portal.links.retainAll { sameFactionPresent(it.origin, it.destination, present) }
            portal.fields.retainAll {
                sameFactionPresent(it.origin, it.primaryAnchor, present) && sameFactionPresent(it.origin, it.secondaryAnchor, present)
            }
        }
    }

    // Both portals are still on the board AND owned by the same (non-null) faction — the link/field invariant.
    private fun sameFactionPresent(a: Portal, b: Portal, present: Set<Portal>): Boolean {
        val factionA = a.owner?.faction
        return a in present && b in present && factionA != null && factionA == b.owner?.faction
    }

    fun calcTotalMu(fact: Faction) = allFields().filter { it.owner.faction == fact }.map { it.calculateMu() }.sum()

    private fun imageDataIndex(x: Int, y: Int, w: Int) = (x + (y * w)) * 4

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
                if (HtmlUtil.isRunningInBrowser()) {
                    Scene3D.sync() // render each NPC as created → serial drop-in
                    // (Flow fields flash once per portal when each portal's field is ready — not per NPC.)
                }
                createNonFaction(callback, count - 1)
            } else {
                callback()
            }
        }, 0)
    }
}
