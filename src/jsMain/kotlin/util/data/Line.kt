package util.data

import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sqrt

data class Line(val from: Pos, val to: Pos) {
    constructor(fromX: Double, fromY: Double, toX: Double, toY: Double) :
            this(Pos(fromX, fromY), Pos(toX, toY))

    constructor(fromX: Int, fromY: Int, toX: Int, toY: Int) :
            this(Pos(fromX, fromY), Pos(toX, toY))

    val fromX = from.x
    val fromY = from.y
    val toX = to.x
    val toY = to.y
    val key: String = from.toString() + "<--->" + to.toString()
    val w = abs(from.x - to.x)
    val h = abs(from.y - to.y)
    fun length() = sqrt((w * w) + (h * h))
    fun calcTaxiLength(): Int = (w + h).toInt()
    fun center(): Pos = Pos((from.x + to.x) / 2, (from.y + to.y) / 2)

    fun doesIntersect(other: Line): Boolean {
        // http://mathworld.wolfram.com/Line-LineIntersection.html
        val yFromDist = from.y - other.from.y
        val xFromDist = from.x - other.from.x
        val xDist = to.x - from.x
        val yDist = to.y - from.y
        val otherXDist = other.to.x - other.from.x
        val otherYDist = other.to.y - other.from.y
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

    fun findClosestPointTo(point: Pos): Pos {
        val xDiff = (to.x - from.x)
        val yDiff = (to.y - from.y)
        check(xDiff != 0.0 || yDiff != 0.0)
        val u = ((point.x - from.x) * xDiff + (point.y - from.y) * yDiff) / (xDiff * xDiff + yDiff * yDiff)
        return when {
            u < 0 -> Pos(from.x, from.y)
            u > 1 -> Pos(to.x, to.y)
            else -> Pos(round(from.x + u * xDiff).toInt(), round(from.y + u * yDiff).toInt())
        }
    }

    fun isValidArea() =
        this.from.x <= this.to.x && this.from.y <= this.to.y //tests if 'from' is top left and 'to' is bottom right

    fun isPointInArea(point: Pos) = isValidArea()
            && point.x >= this.from.x
            && point.y >= this.from.y
            && point.x <= this.to.x
            && point.y <= this.to.y

    override fun toString() = from.toString() + "-" + to.toString()

    companion object {
        fun create(fromX: Int, fromY: Int, toX: Int, toY: Int) = Line(Pos(fromX, fromY), Pos(toX, toY))
    }
}
