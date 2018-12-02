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
import util.data.Coords

object Factory {
    fun coords() = Coords(0, 0)
    fun cell() = Cell(coords(), true, 0)
    fun grid() = mapOf(coords() to cell())
    fun frog() = Agent.createFrog(grid())
    fun smurf() = Agent.createSmurf(grid())
    fun nonFactionAgent() = frog().copy(faction = Faction.NONE)
    fun agent() = frog()
    fun linker() = agent()
    fun owner() = agent()
    fun deployer() = agent()
    fun portal() = Portal.createRandom()
    fun portalPair() = portal() to portal()
    fun portalTriple() = Triple(portal(), portal(), portal())
    fun link() = Link.create(portal(), portal(), linker())
    fun inventory() = Inventory.empty()
    fun portalKey() = PortalKey(portal(), owner())
    fun xmpBurster() = XmpBurster.create(owner(), 8)
    fun resonator() = Resonator.create(owner(), 8)
    fun powerCube() = PowerCube.create(owner(), 8)
    fun slot() = ResonatorSlot.create()
    fun deployedSlot() = slot().deployReso(deployer(), resonator(), Dim.minDeploymentRange.toInt())
}
