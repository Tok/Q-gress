package portal

import Ctx
import World
import agent.Agent
import config.Styles
import util.data.Coords
import util.data.Line
import kotlin.math.max
import kotlin.math.sqrt

data class Field private constructor(val origin: Portal, val primaryAnchor: Portal, val secondaryAnchor: Portal,
                                     val owner: Agent) {
    private val idSet: LinkedHashSet<Portal> = linkedSetOf(origin, primaryAnchor, secondaryAnchor)
    fun weakestPortal() = idSet.toList().sortedBy { it.calcHealth() }.last()
    fun strongestAnchors() = idSet.toList().sortedBy { it.calcHealth() }.take(2)
    fun findFurthestFrom(portal: Portal) = idSet.toList().sortedBy { Line(portal.location, it.location).calcLength() }.first()
    fun isConnectedTo(portal: Portal) = idSet.contains(portal)

    fun calculateMu(): Int = calculateArea() //FIXME use noise map
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
        //https://en.wikipedia.org/wiki/Heron%27s_formula
        val a = Line(origin.location, primaryAnchor.location).calcLength()
        val b = Line(origin.location, secondaryAnchor.location).calcLength()
        val c = Line(primaryAnchor.location, secondaryAnchor.location).calcLength()
        val s = (a + b + c) / 2 //semiperimeter
        val area = sqrt(s * (s - a) * (s - b) * (s - c)).toInt()
        return max(1, area / 100) //FIXME
    }

    fun draw(ctx: Ctx) {
        //Field is drawn in 4 parts like a 'Triforce':
        //- One non transparent center field connecting the center-points of each three links
        //- from each of the three anchors to the center field, with radial gradient transparency for portal health
        val fullStyle = owner.faction.fieldStyle + Styles.fieldTransparency + ")"
        fun drawCenter(one: Coords, two: Coords, three: Coords) {
            with(ctx) {
                fillStyle = fullStyle
                beginPath()
                moveTo(one.x, one.y)
                lineTo(two.x, two.y)
                lineTo(three.x, three.y)
                fill()
                closePath()
            }
        }

        fun drawLinear(portal: Portal, first: Coords, second: Coords) {
            fun calcStyle(health: Int) = owner.faction.fieldStyle + (Styles.fieldTransparency * health / 100) + ")"
            val originHp = calcStyle(portal.calcHealth())
            with(ctx) {
                val point = Line(first, second).findClosestPointTo(portal.location)
                val gradient = World.ctx().createLinearGradient(portal.x(), portal.y(), point.x, point.y)
                gradient.addColorStop(0.1, originHp)
                gradient.addColorStop(1.0, fullStyle)
                fillStyle = gradient
                beginPath()
                moveTo(portal.x(), portal.y())
                lineTo(first.x, first.y)
                lineTo(second.x, second.y)
                fill()
                closePath()
            }
        }

        val originAndPrimary = Line(origin.location, primaryAnchor.location).center()
        val primaryAndSecondary = Line(primaryAnchor.location, secondaryAnchor.location).center()
        val secondaryAndOrigin = Line(secondaryAnchor.location, origin.location).center()

        drawCenter(originAndPrimary, primaryAndSecondary, secondaryAndOrigin)
        drawLinear(origin, originAndPrimary, secondaryAndOrigin)
        drawLinear(primaryAnchor, originAndPrimary, primaryAndSecondary)
        drawLinear(secondaryAnchor, secondaryAndOrigin, primaryAndSecondary)
    }

    override fun toString() = calculateArea().toString() + "MU"
    //equals and hashCode symmetrical
    override fun equals(other: Any?) = other is Field && idSet.containsAll(other.idSet)

    override fun hashCode() = idSet.map { it.hashCode() / 3 }.sum()

    companion object {
        const val destroyAp = 750
        fun isPossible(origin: Portal,
                       primaryAnchor: Portal, secondaryAnchor: Portal): Boolean {
            return World.allPortals.flatMap { it.fields }.none {
                it.idSet == linkedSetOf(origin, primaryAnchor, secondaryAnchor)
            }
        }

        fun create(origin: Portal, primaryAnchor: Portal, secondaryAnchor: Portal, owner: Agent): Field? {
            if (isPossible(origin, primaryAnchor, secondaryAnchor)) {
                return Field(origin, primaryAnchor, secondaryAnchor, owner)
            }
            return null
        }
    }
}
