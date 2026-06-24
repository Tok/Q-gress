package util.data

import World
import config.Config
import config.Constants
import config.Dim
import config.Sim
import extension.Grid
import util.HtmlUtil
import util.Util
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Pos(val x: Double, val y: Double) {
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

    fun isOffGrid() = x < 0 || y < 0 || x >= World.shadowW() || y >= World.shadowH()
    fun isOffScreen() = x < 0 || y < 0 || x >= World.w() || y >= World.h()
    fun xDiff(other: Pos) = x - other.x
    fun yDiff(other: Pos) = y - other.y
    fun distanceTo(other: Pos): Double {
        val xPow = xDiff(other) * xDiff(other)
        val yPow = yDiff(other) * yDiff(other)
        return abs(sqrt(xPow + yPow))
    }

    fun toShadow() = Pos((x / res).toInt(), (y / res).toInt())
    fun fromShadow() = Pos((x * res).toInt(), (y * res).toInt())

    fun surrounding(w: Int, h: Int): List<Pos> = listOf(
        Pos(x - 1.0, y - 1.0),
        Pos(x, y - 1.0),
        Pos(x + 1.0, y - 1.0),
        Pos(x - 1.0, y),
        Pos(x + 1.0, y),
        Pos(x - 1.0, y + 1.0),
        Pos(x, y + 1.0),
        Pos(x + 1.0, y + 1.0),
    ).filter {
        it.x >= 0.0 && it.x <= (w - 1.0) && it.y >= 0.0 && it.y <= (h - 1.0)
    }

    fun randomNearPoint(radius: Int): Pos {
        val r = radius * Util.random()
        val t = Constants.tau * Util.random()
        return Pos(x + (r * cos(t)).toInt(), y + (r * sin(t)).toInt())
    }

    fun toGeo(): GeoCoords {
        val latitude = minLat + (x * pixelPartLat)
        val longitude = minLng - (y * pixelPartLng)
        return GeoCoords(longitude, latitude) // longitude = -Y, latitude = X
    }

    private fun isCloseForClick(location: Pos) = Line(location, this).length() < Dim.portalRadius * 2
    private fun isClose(location: Pos) = Line(location, this).length() < Dim.minDistanceBetweenPortals
    private fun findClosePortalsForClick() = World.allPortals.filter { isCloseForClick(it.location) }
    private fun findClosePortals() = World.allPortals.filter { isClose(it.location) }
    fun hasClosePortalForClick() = findClosePortalsForClick().isNotEmpty()
    fun hasClosePortal() = findClosePortals().isNotEmpty()
    fun isPassable() = World.grid[toShadow()]?.isPassable ?: false
    fun findClosestPortal() = findClosePortals().first()
    fun isBuildable(): Boolean {
        val r = Dim.minDistancePortalToImpassable.toInt()
        return isPassable() &&
            Sim.isInsideField(x, y) &&
            !hasClosePortal() &&
            World.grid[Pos(x - r, y).toShadow()]?.isPassable ?: false &&
            World.grid[Pos(x + r, y).toShadow()]?.isPassable ?: false &&
            World.grid[Pos(x, y - r).toShadow()]?.isPassable ?: false &&
            World.grid[Pos(x, y + r).toShadow()]?.isPassable ?: false
    }

    override fun toString() = "X$x:Y$y"
    override fun equals(other: Any?) = other is Pos && x == other.x && y == other.y
    override fun hashCode() = (x.hashCode() * 31) + y.hashCode()

    companion object {
        val res = Config.pathResolution
        private const val defaultLat = 47.4220454 // X
        private const val defaultLng = 9.3733032 // -Y
        private const val latDist = 0.002
        private val lngDist = latDist * Dim.height / Dim.width
        private const val minLat = defaultLat - latDist
        private val minLng = defaultLng + lngDist
        private val pixelPartLat = latDist / Dim.width
        private val pixelPartLng = lngDist / Dim.height

        private val xMax = Dim.maxDeploymentRange.toInt() * 2
        private fun createRandomNoOffset() = Pos(Util.randomInt(Sim.width), Util.randomInt(Sim.height))
        private fun createRandom(): Pos {
            val x = Sim.leftOffset + Util.randomInt((Sim.width - Sim.leftOffset - Sim.rightOffset).toInt())
            val y = Sim.topOffset + Util.randomInt((Sim.height - Sim.topOffset - Sim.botOffset).toInt())
            return Pos(x.toInt(), y.toInt())
        }

        /**
         * Valid portal-spawn cells (passable, inside the deploy margin, not too close to an existing
         * portal) as offset world positions — ONE grid scan; callers sample from the result. (Calling
         * this per candidate is what made best-candidate placement freeze world-gen.)
         */
        fun portalCandidates(): List<Pos> {
            val offset = res / 2
            return World.passableInActionArea()
                .filterNot { it.key.fromShadow().x < Dim.maxDeploymentRange }
                .filterNot { it.key.fromShadow().x > World.simW() - Dim.maxDeploymentRange }
                .filterNot { it.key.fromShadow().hasClosePortal() }
                .map {
                    val p = it.key.fromShadow()
                    Pos(p.x + offset, p.y + offset)
                }
                .filter { Sim.isInsideField(it.x, it.y) } // round field → only spawn within the inscribed ellipse
        }

        fun createRandomForPortal(): Pos {
            if (HtmlUtil.isNotRunningInBrowser()) {
                return Pos(Util.randomInt(Sim.width), Util.randomInt(Sim.height))
            }
            val candidates = portalCandidates()
            check(candidates.isNotEmpty()) // map is blocked or there is no more space left.
            return candidates[(Util.random() * candidates.size).toInt()]
        }

        // Cache the passable-cell key list per grid (rebuilt only when the grid reference changes — once
        // per world / headless match). Picking a random passable cell was an O(cells) full shuffle +
        // allocation on every call (every recruit), which dominated headless-match cost; this makes it O(1)
        // after the first call (and fixes spawning on impassable cells — the old shuffle keyed off all cells).
        private var cachedGrid: Grid? = null
        private var cachedPassableKeys: List<Pos> = emptyList()

        private fun passableKeys(grid: Grid): List<Pos> {
            if (cachedGrid !== grid) {
                cachedGrid = grid
                cachedPassableKeys = grid.filterValues { it.isPassable }.keys.toList()
            }
            return cachedPassableKeys
        }

        fun createRandomPassable(grid: Grid) = createRandomPassable(grid, 10)
        private fun createRandomPassable(grid: Grid, retries: Int): Pos {
            if (HtmlUtil.isNotRunningInBrowser()) {
                val keys = passableKeys(grid)
                if (keys.isEmpty()) return Pos(0, 0)
                // Grid keys are SHADOW cells; agents/portals live in SIM space. Return the cell centre in
                // sim coords (× res), else headless spawns cluster in a shadow-sized corner of the map and
                // never reach the (sim-space) portals — no gameplay, no MU (broke AI training).
                val cell = keys[(Util.random() * keys.size).toInt()]
                return Pos(cell.x * res + res / 2, cell.y * res + res / 2)
            }
            check(grid.isNotEmpty())
            val random = createRandomNoOffset()
            return if (grid[random.toShadow()]?.isPassable ?: false) {
                random
            } else {
                if (retries > 0) {
                    createRandomPassable(grid, retries - 1)
                } else {
                    console.warn("Blocked Position: $random")
                    random // FIXME workaround..
                }
            }
        }
    }
}
