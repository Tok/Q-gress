package agent.action.cond

import agent.Agent
import agent.action.ActionItem
import config.Config

object Recycler : ConditionalAction {
    override val actionItem = ActionItem.RECYCLE

    private const val SPACE_TRIGGER = 0.95 // recycle to free space once the inventory is ≥95% full
    private const val RECYCLE_BATCH = 50 // items dumped per recycle action when freeing space

    private fun hasCube(agent: Agent) = agent.inventory.findPowerCubes().isNotEmpty()
    private fun nearFull(agent: Agent) = agent.inventory.size() >= (Config.maxInventory * SPACE_TRIGGER).toInt()

    // Recycle is the agent's inventory management: tap a power cube for XM when drained ([Agent.isXmLow] — the
    // same mark below which [Recharger] stops, so recharge→recycle alternate), AND dump junk to free space when
    // the inventory is nearly full (so it can hack again).
    override fun isActionPossible(agent: Agent) = (agent.isXmLow() && hasCube(agent)) || nearFull(agent)

    override fun performAction(agent: Agent): Agent {
        agent.action.start(actionItem)
        if (agent.isXmLow()) {
            agent.inventory.findPowerCubes().firstOrNull()?.let { cube ->
                agent.addXm(cube.level.calculateRecycleXm())
                agent.inventory.consumeCubes(listOf(cube))
            }
        }
        if (nearFull(agent)) {
            agent.addXm(agent.inventory.recycleForSpace(RECYCLE_BATCH)) // freed slots + a little XM back
        }
        return agent
    }
}
