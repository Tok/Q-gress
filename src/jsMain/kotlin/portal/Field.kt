package portal

import World
import agent.Agent
import util.data.Line
import kotlin.math.max
import kotlin.math.sqrt

data class Field private constructor(
    val origin: Portal,
    val primaryAnchor: Portal,
    val secondaryAnchor: Portal,
    val owner: Agent,
) {
    private val idSet: LinkedHashSet<Portal> = linkedSetOf(origin, primaryAnchor, secondaryAnchor)
    fun weakestPortal() = idSet.toList().sortedBy { it.calcHealth() }.last()
    fun strongestAnchors() = idSet.toList().sortedBy { it.calcHealth() }.take(2)
    fun findFurthestFrom(portal: Portal) = idSet.toList().sortedBy { Line(portal.location, it.location).length() }.first()

    fun isConnectedTo(portal: Portal) = idSet.contains(portal)

    fun calculateMu(): Int = calculateArea() // FIXME use noise map
    fun isCoveringPortal(portal: Portal): Boolean {
        val isPortalPart = isConnectedTo(portal)
        if (isPortalPart) {
            return false
        }

        val dXtoSecondary = portal.x() - secondaryAnchor.x()
        val dYtoSecondary = portal.y() - secondaryAnchor.y()
        val dXSecondaryToPrimary = secondaryAnchor.x() - primaryAnchor.x()
        val dYPrimaryToSecondary = primaryAnchor.y() - secondaryAnchor.y()
        val d = (dYPrimaryToSecondary * (origin.x() - secondaryAnchor.x())) +
            (dXSecondaryToPrimary * (origin.y() - secondaryAnchor.y()))
        val s = (dYPrimaryToSecondary * dXtoSecondary) +
            (dXSecondaryToPrimary * dYtoSecondary)
        val t = ((secondaryAnchor.y() - origin.y()) * dXtoSecondary) +
            ((origin.x() - secondaryAnchor.x()) * dYtoSecondary)
        if (d < 0) return s < 0 && t < 0 && s + t > d
        return s > 0 && t > 0 && s + t < d
    }

    fun calculateArea(): Int {
        // https://en.wikipedia.org/wiki/Heron%27s_formula
        val a = Line(origin.location, primaryAnchor.location).length()
        val b = Line(origin.location, secondaryAnchor.location).length()
        val c = Line(primaryAnchor.location, secondaryAnchor.location).length()
        val s = (a + b + c) / 2 // semiperimeter
        val area = sqrt(s * (s - a) * (s - b) * (s - c)).toInt()
        return max(1, area / 100) // FIXME
    }

    override fun toString() = calculateArea().toString() + "MU"

    // equals and hashCode symmetrical!
    override fun equals(other: Any?) = other is Field && idSet.containsAll(other.idSet)

    override fun hashCode() = idSet.map { it.hashCode() / 3 }.sum()

    companion object {
        const val destroyAp = 750
        fun isPossible(
            origin: Portal,
            primaryAnchor: Portal,
            secondaryAnchor: Portal,
        ): Boolean = World.allPortals.flatMap { it.fields }.none {
            it.idSet == linkedSetOf(origin, primaryAnchor, secondaryAnchor)
        }

        fun create(origin: Portal, primaryAnchor: Portal, secondaryAnchor: Portal, owner: Agent): Field? {
            check(origin != primaryAnchor && origin != secondaryAnchor && primaryAnchor != secondaryAnchor)
            if (isPossible(origin, primaryAnchor, secondaryAnchor)) {
                return Field(origin, primaryAnchor, secondaryAnchor, owner)
            }
            return null
        }
    }
}
