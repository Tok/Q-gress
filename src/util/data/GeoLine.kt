package util.data

import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sqrt

data class GeoLine(val from: GeoCoords, val to: GeoCoords) {
    fun calcXdiff(): Double = abs(from.lng - to.lng)
    fun calcYdiff(): Double = abs(from.lat - to.lat)
    fun calcLength(): Double = sqrt((calcXdiff() * calcXdiff()) + (calcYdiff() * calcYdiff()))

    fun doesIntersect(other: GeoLine): Boolean {
        // http://mathworld.wolfram.com/Line-LineIntersection.html
        val yFromDist = from.lat - other.from.lat
        val xFromDist = from.lng - other.from.lng
        val xDist = to.lng - from.lng
        val yDist = to.lat - from.lat
        val otherXDist = other.to.lng - other.from.lng
        val otherYDist = other.to.lat - other.from.lat
        val denominator = (otherYDist * xDist) - (otherXDist * yDist)
        if (denominator.toInt() == 0) {
            return false
        }
        val thisResult = ((xDist * yFromDist) - (yDist * xFromDist)) / denominator
        val otherResult = ((otherXDist * yFromDist) - (otherYDist * xFromDist)) / denominator
        val isOnThis = otherResult > 0 && otherResult < 1
        val isOnOther = thisResult > 0 && thisResult < 1
        return isOnThis && isOnOther
    }

    fun findClosestPointTo(geoPoint: GeoCoords): GeoCoords {
        val xDiff = (to.lng - from.lng)
        val yDiff = (to.lat - from.lat)
        check (xDiff != 0.0 || yDiff != 0.0)
        val u = ((geoPoint.lng - from.lng) * xDiff + (geoPoint.lat - from.lat) * yDiff) / (xDiff * xDiff + yDiff * yDiff)
        return when {
            u < 0 -> GeoCoords(from.lng, from.lat)
            u > 1 -> GeoCoords(to.lng, to.lat)
            else -> GeoCoords(round(from.lng + u * xDiff), round(from.lat + u * yDiff))
        }
    }
}
