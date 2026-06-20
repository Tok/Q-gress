package util.data

import World
import config.Config
import config.Constants
import config.Dim
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
        Pos(x + 1.0, y + 1.0)
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
        return GeoCoords(longitude, latitude) //longitude = -Y, latitude = X
    }

    private fun isCloseForClick(location: Pos) = Line(location, this).length() < Dim.portalRadius * 2
    private fun isClose(location: Pos) = Line(location, this).length() < Dim.minDistanceBetweenPortals
    private fun findClosePortalsForClick() = World.allPortals.filter { isCloseForClick(it.location) }
    private fun findClosePortals() = World.allPortals.filter { isClose(it.location) }
    fun hasClosePortalForClick() = findClosePortalsForClick().isNotEmpty()
    fun hasClosePortal() = findClosePortals().isNotEmpty()
    fun isPassable() = World.grid.isNotEmpty() && World.grid[toShadow()]!!.isPassable
    fun findClosestPortal() = findClosePortals().first()
    fun isBuildable(): Boolean {
        val r = Dim.minDistancePortalToImpassable.toInt()
        return isPassable() && !hasClosePortal() &&
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
        private const val defaultLat = 47.4220454 //X
        private const val defaultLng = 9.3733032 //-Y
        private const val latDist = 0.002
        private val lngDist = latDist * Dim.height / Dim.width
        private const val minLat = defaultLat - latDist
        private val minLng = defaultLng + lngDist
        private val pixelPartLat = latDist / Dim.width
        private val pixelPartLng = lngDist / Dim.height

        private val xMax = Dim.maxDeploymentRange.toInt() * 2
        private fun createRandomNoOffset() = Pos(Util.randomInt(Dim.width), Util.randomInt(Dim.height))
        private fun createRandom(): Pos {
            val x = Dim.leftOffset + Util.randomInt((Dim.width - Dim.leftOffset - Dim.rightOffset).toInt())
            val y = Dim.topOffset + Util.randomInt((Dim.height - Dim.topOffset - Dim.botOffset).toInt())
            return Pos(x.toInt(), y.toInt())
        }

        fun createRandomForPortal(): Pos {
            if (HtmlUtil.isNotRunningInBrowser()) {
                return Pos(Util.randomInt(Dim.width), Util.randomInt(Dim.height))
            } else {
                val grid = World.passableInActionArea()
                    .filterNot { it.key.fromShadow().x < Dim.maxDeploymentRange }
                    .filterNot { it.key.fromShadow().x > World.w() - Dim.maxDeploymentRange }
                    .filterNot { it.key.fromShadow().hasClosePortal() }
                check(grid.isNotEmpty()) //map is blocked or there is no more space left.
                val randomCell = Util.shuffle(grid.toList()).first()
                val pos = randomCell.first.fromShadow()
                val offset = res / 2
                return Pos(pos.x + offset, pos.y + offset)
            }
        }

        fun createRandomPassable(grid: Grid) = createRandomPassable(grid, 10)
        private fun createRandomPassable(grid: Grid, retries: Int): Pos {
            if (HtmlUtil.isNotRunningInBrowser()) {
                return if (grid.isEmpty()) Pos(0, 0)
                else Util.shuffle(grid.keys).first()
            }
            check(grid.isNotEmpty())
            val random = createRandomNoOffset()
            return if (grid[random.toShadow()]!!.isPassable) {
                random
            } else {
                if (retries > 0) {
                    createRandomPassable(grid, retries - 1)
                } else {
                    console.warn("Blocked Position: $random")
                    random //FIXME workaround..
                }
            }
        }
    }
}
