package portal

import World
import agent.Agent
import util.data.Line

data class Link(val origin: Portal, val destination: Portal, val creator: Agent) {
    fun getLine() = Line(origin.location, destination.location)
    fun isConnectedTo(portal: Portal) = destination == portal || origin == portal

    override fun toString() = origin.toString() + " --> " + destination.toString()

    // equals and hashCode symmetrical
    override fun equals(other: Any?) = other is Link &&
        (
            origin == other.origin &&
                destination == other.destination ||
                origin == other.destination &&
                destination == other.origin
            )

    override fun hashCode() = origin.hashCode() + destination.hashCode()

    companion object {
        const val destroyAp = 187
        fun isNotExisting(link: Link): Boolean = World.allLinks().none { it == link }

        fun create(origin: Portal, destination: Portal, linker: Agent): Link? {
            check(origin != destination)
            val newLink = Link(origin, destination, linker)
            if (isNotExisting(newLink)) {
                return newLink
            }
            return null
        }
    }
}
