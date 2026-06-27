package util.data

import World
import config.Constants
import config.Dim
import config.Sim
import util.Rng
import kotlin.math.cos
import kotlin.math.sin

/**
 * The JS-shell half of [Pos]: everything that reads the running world (grid, portals), the geo projection, or
 * the RNG. These were members of the old god-`Pos`; as extension functions in the same package the call sites
 * (`pos.toShadow()`, `pos.isPassable()`, …) are unchanged, while the pure geometry core stays in `commonMain`.
 */

fun Pos.isOffGrid() = x < 0 || y < 0 || x >= World.shadowW() || y >= World.shadowH()
fun Pos.isOffScreen() = x < 0 || y < 0 || x >= World.w() || y >= World.h()

fun Pos.toShadow() = Pos((x / Pos.res).toInt(), (y / Pos.res).toInt())
fun Pos.fromShadow() = Pos((x * Pos.res).toInt(), (y * Pos.res).toInt())

fun Pos.randomNearPoint(radius: Int): Pos {
    val r = radius * Rng.random()
    val t = Constants.tau * Rng.random()
    return Pos(x + (r * cos(t)).toInt(), y + (r * sin(t)).toInt())
}

// --- Geo projection (sim pixels → lng/lat), anchored at the reference origin -----------------------------
private const val DEFAULT_LAT = 47.4220454 // X
private const val DEFAULT_LNG = 9.3733032 // -Y
private const val LAT_DIST = 0.002
private const val MIN_LAT = DEFAULT_LAT - LAT_DIST
private val lngDist = LAT_DIST * Dim.height / Dim.width
private val minLng = DEFAULT_LNG + lngDist
private val pixelPartLat = LAT_DIST / Dim.width
private val pixelPartLng = lngDist / Dim.height

fun Pos.toGeo(): GeoCoords {
    val latitude = MIN_LAT + (x * pixelPartLat)
    val longitude = minLng - (y * pixelPartLng)
    return GeoCoords(longitude, latitude) // longitude = -Y, latitude = X
}

// --- Portal proximity / placement checks (read the live board) -------------------------------------------
private fun Pos.isCloseForClick(location: Pos) = Line(location, this).length() < Dim.portalRadius * 2
private fun Pos.isClose(location: Pos) = Line(location, this).length() < Dim.minDistanceBetweenPortals
private fun Pos.findClosePortalsForClick() = World.allPortals.filter { isCloseForClick(it.location) }
private fun Pos.findClosePortals() = World.allPortals.filter { isClose(it.location) }
fun Pos.hasClosePortalForClick() = findClosePortalsForClick().isNotEmpty()
fun Pos.hasClosePortal() = findClosePortals().isNotEmpty()
fun Pos.isPassable() = World.grid[toShadow()]?.isPassable ?: false
fun Pos.findClosestPortal() = findClosePortals().first()
fun Pos.isBuildable(): Boolean {
    val r = Dim.minDistancePortalToImpassable.toInt()
    return isPassable() &&
        Sim.isInsideField(x, y) &&
        !hasClosePortal() &&
        World.grid[Pos(x - r, y).toShadow()]?.isPassable ?: false &&
        World.grid[Pos(x + r, y).toShadow()]?.isPassable ?: false &&
        World.grid[Pos(x, y - r).toShadow()]?.isPassable ?: false &&
        World.grid[Pos(x, y + r).toShadow()]?.isPassable ?: false
}
