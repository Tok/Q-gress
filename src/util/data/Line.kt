package util.data

import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sqrt

data class Line(val from: Coords, val to: Coords) {
    fun key(): String = from.toString() + "<--->" + to.toString()
    fun calcXdiff(): Double = abs(from.x.toDouble() - to.x)
    fun calcYdiff(): Double = abs(from.y.toDouble() - to.y)
    fun calcLength(): Double = sqrt((calcXdiff() * calcXdiff()) + (calcYdiff() * calcYdiff()))
    fun calcTaxiLength(): Int = (calcXdiff() + calcYdiff()).toInt()
    fun center(): Coords = Coords((from.x + to.x) / 2, (from.y + to.y) / 2)

    fun doesIntersect(other: Line): Boolean {
        // http://mathworld.wolfram.com/Line-LineIntersection.html
        val yFromDist = from.y - other.from.y
        val xFromDist = from.x - other.from.x
        val xDist = to.x - from.x
        val yDist = to.y - from.y
        val otherXDist = other.to.x.toDouble() - other.from.x
        val otherYDist = other.to.y.toDouble() - other.from.y
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

    fun findClosestPointTo(point: Coords): Coords {
        val xDiff = (to.x - from.x).toDouble()
        val yDiff = (to.y - from.y).toDouble()
        check (xDiff != 0.0 || yDiff != 0.0)
        val u = ((point.x - from.x) * xDiff + (point.y - from.y) * yDiff) / (xDiff * xDiff + yDiff * yDiff)
        return when {
            u < 0 -> Coords(from.x, from.y)
            u > 1 -> Coords(to.x, to.y)
            else -> Coords(round(from.x + u * xDiff).toInt(), round(from.y + u * yDiff).toInt())
        }
    }
}
