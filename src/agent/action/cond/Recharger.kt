package agent.action.cond

import agent.Agent
import agent.action.ActionItem
import portal.Portal

object Recharger : ConditionalAction {
    override val actionItem = ActionItem.RECYCLE

    override fun isActionPossible(agent: Agent) = agent.isXmFilled() &&
            chargeableKeys(agent).isNotEmpty() &&
            rechargeResos(agent).isNotEmpty()

    override fun performAction(agent: Agent): Agent {
        agent.action.start(actionItem)
        val resos = rechargeResos(agent)
        resos.forEach { it.recharge(agent, 1000 / resos.count()) }
        return agent
    }

    private fun chargeableKeys(agent: Agent) = Portal.findChargeableForKeys(agent, agent.keySet().orEmpty()).orEmpty()
    private fun lowestChargeablePortal(agent: Agent) = chargeableKeys(agent).sortedBy { it.calcHealth() }.first()
    private fun rechargeResos(agent: Agent) = lowestChargeablePortal(agent).resoSlots.mapNotNull { it.value.resonator }
}
