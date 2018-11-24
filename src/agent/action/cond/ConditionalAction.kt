package agent.action.cond

import agent.Agent

interface ConditionalAction {
    fun isActionPossible(agent: Agent): Boolean
    fun performAction(agent: Agent): Agent
}
