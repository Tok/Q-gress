import agent.Agent
import agent.Faction
import agent.Inventory
import config.Dim
import items.PowerCube
import items.XmpBurster
import items.deployable.Resonator
import portal.Link
import portal.Portal
import portal.PortalKey
import portal.ResonatorSlot
import util.data.Cell
import util.data.Pos
import portal.Octant

object Factory {
    fun coords() = Pos(0, 0)
    fun cell() = Cell(coords(), true, 0)
    fun grid() = mapOf(coords() to cell())
    fun frog() = Agent.createFrog(grid())
    fun smurf() = Agent.createSmurf(grid())
    fun agent() = frog()
    fun agent(faction: Faction) = if (faction == Faction.ENL) frog() else smurf()
    fun linker() = agent()
    fun owner() = agent()
    fun deployer() = agent()
    fun portal() = Portal.createRandom()
    fun portal(faction: Faction): Portal {
        val portal = portal()
        val agent = agent(faction)
        portal.owner = agent
        val reso = Resonator.create(agent, 1)
        portal.slots[Octant.N]!!.deployReso(agent, reso, Dim.maxDeploymentRange.toInt())
        return portal
    }
    fun portalPair() = portal() to portal()
    fun portalTriple() = Triple(portal(), portal(), portal())
    fun link() = Link.create(portal(), portal(), linker())
    fun inventory() = Inventory.empty()
    fun portalKey() = PortalKey(portal(), owner())
    fun xmpBurster() = XmpBurster.create(owner(), 1)
    fun resonator(owner: Agent? = owner(), level: Int = 1) = Resonator.create(owner ?: owner(), level)
    fun powerCube() = PowerCube.create(owner(), 1)
    fun slot() = ResonatorSlot.create()
    fun deployedSlot() = slot().deployReso(deployer(), resonator(), Dim.minDeploymentRange.toInt())
}
