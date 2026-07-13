package agent.action.cond

import agent.Agent
import agent.action.ActionItem
import portal.Portal
import kotlin.math.min

/**
 * Recharge, as in Ingress: refill a friendly portal's resonators from the agent's own XM. The target is either
 * the damaged friendly portal the agent is **standing at** (no key needed) or a badly-hurt **remote** one it
 * holds a key for ([Portal.findChargeable] — the emergency hold); one action feeds each resonator up to
 * [RECHARGE_XM_PER_RESO], the agent's XM split evenly across them. Pairs with [Recycler]: recharging drains
 * the XM bar toward the low mark, a power-cube recycle refills it, and the two alternate — that
 * back-and-forth is how a faction holds its portals against decay.
 */
object Recharger : ConditionalAction {
    override val actionItem = ActionItem.RECHARGE

    private const val RECHARGE_XM_PER_RESO = 1000 // authentic: one recharge tops each resonator up by ≤ 1000 XM

    override fun isActionPossible(agent: Agent) = !agent.isXmLow() && rechargeResos(agent).isNotEmpty()

    override fun performAction(agent: Agent): Agent {
        agent.action.start(actionItem)
        val resos = rechargeResos(agent)
        val perReso = min(RECHARGE_XM_PER_RESO, agent.xm / resos.count())
        resos.forEach { it.recharge(agent, perReso) }
        return agent
    }

    private fun neediestChargeable(agent: Agent) = Portal.findChargeable(agent, agent.keySet()).minByOrNull { it.calcHealth() }

    private fun rechargeResos(agent: Agent) = neediestChargeable(agent)?.slots?.mapNotNull { it.value.resonator }.orEmpty()
}
