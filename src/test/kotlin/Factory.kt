import agent.Agent
import portal.Link
import portal.Portal
import util.data.Cell
import util.data.Coords

object Factory {
    fun coords() = Coords(0, 0)
    fun cell() = Cell(coords(), true, 0)
    fun grid() = mapOf(coords() to cell())
    fun frog() = Agent.createFrog(grid())
    fun smurf() = Agent.createSmurf(grid())
    fun agent() = frog()
    fun linker() = agent()
    fun portal() = Portal.createRandom()
    fun portalPair() = portal() to portal()
    fun portalTriple() = Triple(portal(), portal(), portal())
    fun link() = Link.create(portal(), portal(), linker())
}
