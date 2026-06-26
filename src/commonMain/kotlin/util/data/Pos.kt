package util.data

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A 2D point in sim space. The **pure geometry** core lives here in the shared functional core (`commonMain`):
 * coordinates + distance/neighbour math, JVM-unit-tested + Kover-covered. Everything that needs the running
 * world — grid lookups (`toShadow`/`fromShadow`/`isPassable`), portal proximity, the geo projection
 * (`toGeo`), RNG (`randomNearPoint`) and the spawn factories — lives in the JS shell as extension functions
 * (`util/data/PosExt.kt`) and the `Positions` object, so call sites like `pos.toShadow()` are unchanged.
 */
data class Pos(val x: Double, val y: Double) {
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

    fun xDiff(other: Pos) = x - other.x
    fun yDiff(other: Pos) = y - other.y
    fun distanceTo(other: Pos): Double {
        val xPow = xDiff(other) * xDiff(other)
        val yPow = yDiff(other) * yDiff(other)
        return abs(sqrt(xPow + yPow))
    }

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

    override fun toString() = "X$x:Y$y"
    override fun equals(other: Any?) = other is Pos && x == other.x && y == other.y
    override fun hashCode() = (x.hashCode() * 31) + y.hashCode()

    companion object {
        // Grid resolution: sim pixels per shadow/grid cell. A pure constant (was Config.pathResolution, whose
        // only consumer was this); kept as `Pos.res` since that's the established API across the codebase.
        const val res = 10
    }
}
