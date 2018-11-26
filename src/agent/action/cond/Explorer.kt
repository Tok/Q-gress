package agent.action.cond

import World
import agent.Agent
import agent.action.ActionItem
import config.Config
import portal.Portal
import system.Com
import system.display.VectorFields
import util.Util
import util.data.Coords

object Explorer : ConditionalAction {
    override val actionItem = ActionItem.EXPLORE

    override fun isActionPossible(agent: Agent) = World.countPortals() < Config.maxPortals

    override fun performAction(agent: Agent): Agent {
        agent.action.start(actionItem)
        if (Util.random() <= portalDiscoveryChance) {
            val newPortal = Portal.createRandom()
            VectorFields.draw(newPortal)
            World.allPortals.add(newPortal)
            agent.destination = newPortal.location
            Com.addMessage("$agent discovered a new portal $newPortal.")
        } else {
            agent.destination = Coords.createRandomForPortal()
        }
        return agent
    }

    private const val portalDiscoveryChance = 0.2
}
