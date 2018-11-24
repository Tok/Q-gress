package agent.action.cond

import agent.Agent
import agent.action.ActionItem

interface ConditionalAction {
    val actionItem: ActionItem
    fun isActionPossible(agent: Agent): Boolean
    fun performAction(agent: Agent): Agent
}
