package agent.action.cond

import agent.Agent
import agent.Inventory
import agent.action.ActionItem
import config.Constants
import items.XmpBurster
import system.Queues
import util.Util

object Attacker : ConditionalAction {
    override val actionItem = ActionItem.ATTACK

    override fun isActionPossible(agent: Agent) = agent.inventory.findXmps().count() >= attackXmps

    override fun performAction(agent: Agent) = performAction(agent, 1)
    private fun performAction(agent: Agent, i: Int): Agent {
        val xmps = xmpsForAttack(agent.inventory)
        doAttack(agent, xmps)
        agent.inventory.consumeXmps(xmps)
        Queues.registerAttack(agent, xmps, i)
        val isDoItAgain = xmps.isNotEmpty() && Util.random() <= 1 / Constants.phi
        return if (isDoItAgain) performAction(agent, i + 1) else agent
    }

    private fun doAttack(agent: Agent, xmps: List<XmpBurster>) {
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
    }

    private const val minAttackXmps = 10
    private const val maxAttackXmps = (minAttackXmps * Constants.phi).toInt()
    private const val attackXmps = 50
    private fun attackXmpCount() = minAttackXmps + Util.randomInt(maxAttackXmps - minAttackXmps)
    private fun xmpsForAttack(inv: Inventory) = inv.findXmps().sortedByDescending { it.level }.take(attackXmpCount())
}
