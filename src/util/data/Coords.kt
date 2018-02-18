package util.data

import World
import config.Dimensions
import util.PathUtil
import util.Util
import kotlin.math.abs
import kotlin.math.sqrt

data class Coords(val x: Int, val y: Int) {
    fun xx() = x.toDouble()
    fun yy() = y.toDouble()
    fun isOffGrid() = x < 0 || y < 0 || x >= World.shadowW() || y >= World.shadowH()
    fun xDiff(other: Coords) = x - other.x
    fun yDiff(other: Coords) = y - other.y
    fun distanceTo(other: Coords): Double {
        val xPow = xDiff(other) * xDiff(other)
        val yPow = yDiff(other) * yDiff(other)
        return abs(sqrt(xPow.toDouble() + yPow.toDouble()))
    }

    fun getSurrounding(w: Int, h: Int): List<Coords> = listOf(
            Coords(x - 1, y - 1),
            Coords(x, y - 1),
            Coords(x + 1, y - 1),
            Coords(x - 1, y),
            Coords(x + 1, y),
            Coords(x - 1, y + 1),
            Coords(x, y + 1),
            Coords(x + 1, y + 1)
    ).filter { it.x >= 0 && it.x < w && it.y >= 0 && it.y < h }

    fun toGeo(): GeoCoords {
        val latitude = minLat + (x * pixelPartLat)
        val longitude = minLng - (y * pixelPartLng)
        return GeoCoords(longitude, latitude) //longitude = -Y, latitude = X
    }

    private fun isCloseForClick(location: Coords) = Line(location, this).calcLength() < Dimensions.portalRadius * 2
    private fun isClose(location: Coords) = Line(location, this).calcLength() < Dimensions.minDistanceBetweenPortals
    private fun findClosePortalsForClick() = World.allPortals.filter { isCloseForClick(it.location) }
    private fun findClosePortals() = World.allPortals.filter { isClose(it.location) }
    fun hasClosePortalForClick() = findClosePortalsForClick().isNotEmpty()
    fun hasClosePortal() = findClosePortals().isNotEmpty()
    fun toShadowPos() = PathUtil.posToShadowPos(this)
    fun isPassable() = World.grid.isNotEmpty() && World.grid.get(toShadowPos())!!.isPassable
    fun findClosestPortal() = findClosePortals().first()
    fun isBuildable(): Boolean {
        val r = Dimensions.minDistancePortalToImpassable.toInt()
        return isPassable() && !hasClosePortal() &&
                World.grid.get(Coords(x - r, y).toShadowPos())?.isPassable ?: false &&
                World.grid.get(Coords(x + r, y).toShadowPos())?.isPassable ?: false &&
                World.grid.get(Coords(x, y - r).toShadowPos())?.isPassable ?: false &&
                World.grid.get(Coords(x, y + r).toShadowPos())?.isPassable ?: false
    }
    override fun toString() = "X$x:Y$y"
    override fun hashCode() = toString().hashCode() * 1337
    override fun equals(other: Any?) = other is Coords && x == other?.x && y == other?.y

    companion object {
        private val defaultLat = 47.4220454 //X
        private val defaultLng = 9.3733032 //-Y
        private val latDist = 0.002
        private val lngDist = latDist * World.can.height / World.can.width
        private val minLat = defaultLat - latDist
        private val minLng = defaultLng + lngDist
        private val pixelPartLat = latDist / World.can.width
        private val pixelPartLng = lngDist / World.can.height

        private val xMax = Dimensions.maxDeploymentRange.toInt() * 2
        private fun createRandomNoOffset() = Coords(Util.randomInt(World.can.width), Util.randomInt(World.can.height))
        private fun createRandom(): Coords {
            val x = Dimensions.leftOffset + Util.randomInt((World.can.width - Dimensions.leftOffset - Dimensions.rightOffset).toInt())
            val y = Dimensions.topOffset + Util.randomInt((World.can.height - Dimensions.topOffset - Dimensions.botOffset).toInt())
            return Coords(x.toInt(), y.toInt())
        }

        fun createRandomForPortal(): Coords {
            val grid = World.passableInActionArea()
                    .filterNot { PathUtil.shadowPosToPos(it.key).x < Dimensions.maxDeploymentRange }
                    .filterNot { PathUtil.shadowPosToPos(it.key).x > World.w() - Dimensions.maxDeploymentRange }
                    .filterNot { PathUtil.shadowPosToPos(it.key).hasClosePortal() }
            check(grid.isNotEmpty()) //map is blocked or there is no more space left.
            val randomCell = Util.shuffle(grid.toList()).first()
            val pos = PathUtil.shadowPosToPos(randomCell.first)
            val offset = PathUtil.RESOLUTION / 2
            return Coords(pos.x + offset, pos.y + offset)
        }

        fun createRandomPassable(grid: Map<Coords, Cell>) = createRandomPassable(grid, 10)
        private fun createRandomPassable(grid: Map<Coords, Cell>, retries: Int): Coords {
            check(grid.isNotEmpty())
            val random = createRandomNoOffset()
            if (grid.get(PathUtil.posToShadowPos(random))!!.isPassable) {
                return random
            } else {
                return if (retries > 0) {
                    createRandomPassable(grid, retries - 1)
                } else {
                    println("WARN: using blocked position: " + random)
                    random //FIXME workaround..
                }
            }
        }
    }
}
