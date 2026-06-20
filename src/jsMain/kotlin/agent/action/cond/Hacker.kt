package agent.action.cond

import agent.Agent
import agent.action.ActionItem
import items.QgressItem
import util.SoundUtil

object Hacker : ConditionalAction {
    override val actionItem = ActionItem.HACK

    override fun isActionPossible(agent: Agent) = agent.actionPortal.canHack(agent)

    override fun performAction(agent: Agent): Agent {
        agent.action.start(actionItem)
        val hackResult = agent.actionPortal.tryHack(agent)
        SoundUtil.playHackingSound(agent.actionPortal.location)
        val isSuccess = hackResult.items != null
        if (isSuccess) {
            val newStuff: List<QgressItem> = hackResult.items!!
            agent.inventory.items.addAll(newStuff)
        }
        return agent
    }
}
