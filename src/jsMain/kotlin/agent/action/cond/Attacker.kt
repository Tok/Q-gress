package agent.action.cond

import agent.Agent
import agent.Inventory
import agent.action.ActionItem
import config.Constants
import items.XmpBurster
import system.display.Scene3D
import util.Util

object Attacker : ConditionalAction {
    override val actionItem = ActionItem.ATTACK

    override fun isActionPossible(agent: Agent) = agent.inventory.findXmps().count() >= attackXmps

    override fun performAction(agent: Agent): Agent {
        val xmps = xmpsForAttack(agent.inventory)
        doAttack(agent, xmps)
        agent.inventory.consumeXmps(xmps)
        if (xmps.isNotEmpty()) {
            // Detonate FIRST (records the blast origin) so the resonators/mods it destroys fly away from
            // it, then apply the damage that shatters them.
            Scene3D.playXmpBurst(agent.pos, xmps.maxOf { it.level.level }, sound = true)
            xmps.forEach { it.dealDamage(agent) } // apply resonator damage (was the now-removed Queues path)
        }
        val isDoItAgain = xmps.isNotEmpty() && Util.random() <= 1 / Constants.phi
        return if (isDoItAgain) performAction(agent) else agent
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
