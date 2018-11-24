package agent.action.cond

import agent.Agent
import agent.Inventory
import system.Queues
import util.Util

object Attacker : ConditionalAction {
    private const val attackXmps = 50
    private fun xmpsForAttack(inv: Inventory) = inv.findXmps().sortedBy { it.level }.take((attackXmps * Util.random()).toInt())
    override fun isActionPossible(agent: Agent) = agent.inventory.findXmps().count() >= attackXmps
    override fun performAction(agent: Agent): Agent {
        val xmps = xmpsForAttack(agent.inventory)
        xmps.forEach { xmpBurster ->
            when (xmpBurster.level.level) {
                1 -> agent.removeXm(10)
                2 -> agent.removeXm(20)
                3 -> agent.removeXm(70)
                4 -> agent.removeXm(140)
                5 -> agent.removeXm(250)
                6 -> agent.removeXm(360)
                7 -> agent.removeXm(490)
                else -> agent.removeXm(640)
            }
        }
        Queues.registerAttack(agent, xmps)
        agent.inventory.consumeXmps(xmps)
        agent.action.end()
        return agent
    }

}
