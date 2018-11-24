package agent.action.cond

import agent.Agent
import agent.action.ActionItem
import items.PowerCube

object Recycler : ConditionalAction {
    override val actionItem = ActionItem.RECYCLE

    override fun isActionPossible(agent: Agent) = agent.xm < agent.xmCapacity() / 10

    override fun performAction(agent: Agent): Agent {
        agent.action.start(actionItem)

        //TODO improve
        val cubes: List<PowerCube> = agent.inventory.findPowerCubes()
        if (cubes.isNotEmpty()) {
            val cube: PowerCube = cubes.first()
            agent.addXm(cube.level.calculateRecycleXm())
            agent.inventory.consumeCubes(listOf(cube))
        }
        return agent
    }
}
