package portal

import Ctx
import agent.Agent
import agent.Faction
import config.Dim
import util.Util
import util.data.Line

data class Link(val origin: Portal, val destination: Portal, val creator: Agent) {
    fun getLine() = Line(origin.location, destination.location)
    fun isConnectedTo(portal: Portal) = destination == portal || origin == portal

    fun draw(ctx: Ctx) {
        val byHealth = listOf(origin, destination).sortedBy { it.calcHealth() }
        val minTransparency = 0.2
        val lowHpTransparency = Util.clipDouble(byHealth.last().calcHealth() * 0.01, minTransparency, 1.0)
        val highHpTransparency = Util.clipDouble(byHealth.first().calcHealth() * 0.01, minTransparency, 1.0)
        val gradient = ctx.createLinearGradient(origin.x(), origin.y(), destination.x(), destination.y())
        if (origin.calcHealth() < destination.calcHealth()) {
            gradient.addColorStop(0.0, creator.faction.fieldStyle + highHpTransparency + ")")
            gradient.addColorStop(1.0, creator.faction.fieldStyle + lowHpTransparency + ")")
        } else {
            gradient.addColorStop(0.0, creator.faction.fieldStyle + lowHpTransparency + ")")
            gradient.addColorStop(1.0, creator.faction.fieldStyle + highHpTransparency + ")")
        }
        with(ctx) {
            strokeStyle = gradient
            lineWidth = Dim.linkLineWidth
            beginPath()
            moveTo(getLine().from.x, getLine().from.y)
            lineTo(getLine().to.x, getLine().to.y)
            closePath()
            stroke()
        }
    }

    override fun toString() = origin.toString() + " --> " + destination.toString()
    //equals and hashCode symmetrical
    override fun equals(other: Any?) = other is Link &&
            (origin == other.origin && destination == other.destination ||
                    origin == other.destination && destination == other.origin)
    override fun hashCode() = origin.hashCode() + destination.hashCode()

    companion object {
        const val destroyAp = 187
        fun isNotExisting(link: Link): Boolean = World.allLinks().none { it == link }

        fun create(origin: Portal, destination: Portal, linker: Agent): Link? {
            check(origin != destination)
            check(linker.faction != Faction.NONE)
            val newLink = Link(origin, destination, linker)
            if (isNotExisting(newLink)) {
                return newLink
            }
            return null
        }
    }
}
