package portal

import World
import agent.Agent
import util.data.Line

data class Field private constructor(val origin: Portal, val primaryAnchor: Portal, val secondaryAnchor: Portal, val owner: Agent) {
    private val idSet: LinkedHashSet<Portal> = linkedSetOf(origin, primaryAnchor, secondaryAnchor)
    fun weakestPortal() = idSet.toList().sortedBy { it.calcHealth() }.last()
    fun strongestAnchors() = idSet.toList().sortedBy { it.calcHealth() }.take(2)
    fun findFurthestFrom(portal: Portal) = idSet.toList().sortedBy { Line(portal.location, it.location).length() }.first()

    fun isConnectedTo(portal: Portal) = idSet.contains(portal)

    fun calculateMu(): Int = calculateArea() // FIXME use noise map

    /** Whether this field's triangle covers [portal] (and [portal] isn't one of its own anchors). */
    fun isCoveringPortal(portal: Portal): Boolean = !isConnectedTo(portal) &&
        FieldMath.isInsideTriangle(portal.location, origin.location, primaryAnchor.location, secondaryAnchor.location)

    fun calculateArea(): Int = FieldMath.triangleAreaMu(origin.location, primaryAnchor.location, secondaryAnchor.location)

    override fun toString() = calculateArea().toString() + "MU"

    // equals and hashCode symmetrical!
    override fun equals(other: Any?) = other is Field && idSet.containsAll(other.idSet)

    override fun hashCode() = idSet.map { it.hashCode() / 3 }.sum()

    companion object {
        const val destroyAp = 750
        fun isPossible(origin: Portal, primaryAnchor: Portal, secondaryAnchor: Portal): Boolean =
            World.allPortals.flatMap { it.fields }.none {
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
