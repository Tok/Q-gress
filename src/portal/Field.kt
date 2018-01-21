package portal

import Ctx
import World
import agent.Agent
import config.Constants
import config.Styles
import items.deployable.Resonator
import util.data.Coords
import util.data.Line
import kotlin.math.max
import kotlin.math.sqrt

data class Field private constructor(val origin: Portal, val primaryAnchor: Portal, val secondaryAnchor: Portal,
                                     val owner: Agent) {
    val idSet: LinkedHashSet<Portal> = linkedSetOf(origin, primaryAnchor, secondaryAnchor)
    fun weakestPortal() = idSet.toList().sortedBy { it.calcHealth() }.last()
    fun strongestAnchors() = idSet.toList().sortedBy { it.calcHealth() }.take(2)
    fun findFurthestFrom(portal: Portal) = idSet.toList().sortedBy { Line(portal.location ,it.location).calcLength() }.first()

    fun calculateMu(): Int = calculateArea() //FIXME use noise map
    fun isCoveringPortal(portal: Portal): Boolean {
        val isPortalPart = idSet.contains(portal)
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
                moveTo(one.xx(), one.yy())
                lineTo(two.xx(), two.yy())
                lineTo(three.xx(), three.yy())
                fill()
                closePath()
            }
        }

        fun drawRadial(portal: Portal, first: Coords, second: Coords) {
            fun calcStyle(health: Int) = owner.faction.fieldStyle + (Styles.fieldTransparency * health / 100) + ")"
            val originHp = calcStyle(portal.calcHealth())
            with(ctx) {
                val r = Line(portal.location, Line(first, second).findClosestPointTo(portal.location)).calcLength()
                val gradient = World.ctx().createRadialGradient( //FIXME switch back to linear
                        portal.x(), portal.y(), r * (Constants.phi - 1),
                        portal.x(), portal.y(), r
                )
                //val point = Line(first, second).findClosestPointTo(portal.location)
                //val gradient = World.ctx().createLinearGradient(portal.x(), portal.y(), point.xx(), point.yy())
                gradient.addColorStop(0.0, originHp)
                gradient.addColorStop(1.0, fullStyle)
                fillStyle = gradient
                beginPath()
                moveTo(portal.x(), portal.y())
                lineTo(first.xx(), first.yy())
                lineTo(second.xx(), second.yy())
                fill()
                closePath()
            }
        }

        val originAndPrimary = Line(origin.location, primaryAnchor.location).center()
        val primaryAndSecondary = Line(primaryAnchor.location, secondaryAnchor.location).center()
        val secondaryAndOrigin = Line(secondaryAnchor.location, origin.location).center()

        drawCenter(originAndPrimary, primaryAndSecondary, secondaryAndOrigin)
        drawRadial(origin, originAndPrimary, secondaryAndOrigin)
        drawRadial(primaryAnchor, originAndPrimary, primaryAndSecondary)
        drawRadial(secondaryAnchor, secondaryAndOrigin, primaryAndSecondary)
    }

    override fun equals(other: Any?) = other is Field && idSet.containsAll(other.idSet)
    override fun toString() = calculateArea().toString() + "MU"

    companion object {
        fun isPossible(origin: Portal,
                       primaryAnchor: Portal, secondaryAnchor: Portal): Boolean {
            return World.allPortals.flatMap { it.fields }.filter {
                it.idSet == linkedSetOf(origin, primaryAnchor, secondaryAnchor)
            }.isEmpty()
        }

        fun create(origin: Portal, primaryAnchor: Portal, secondaryAnchor: Portal, owner: Agent): Field? {
            if (isPossible(origin, primaryAnchor, secondaryAnchor)) {
                return Field(origin, primaryAnchor, secondaryAnchor, owner)
            }
            return null
        }
    }
}
