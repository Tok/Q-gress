package agent.action.cond

import World
import agent.Agent
import agent.Faction
import agent.NonFaction
import agent.action.ActionItem
import system.Com
import util.Util

object Recruiter : ConditionalAction {
    override val actionItem = ActionItem.RECRUIT

    override fun isActionPossible(agent: Agent) = World.canRecruitMore(agent.faction)

    override fun performAction(agent: Agent): Agent {
        agent.action.start(actionItem)
        val npc = NonFaction.findNearestTo(agent.pos)
        if (Util.random() < NonFaction.changeToBeRecruited) {
            World.allNonFaction.remove(npc)
            val newAgent = when (agent.faction) {
                Faction.ENL -> Agent.createFrog(World.grid)
                Faction.RES -> Agent.createSmurf(World.grid)
                else -> throw IllegalStateException("$agent is ${agent.faction} NPC.")
            }
            Com.addMessage("$newAgent has completed the tutorial.")
            World.allAgents.add(newAgent)
        }
        agent.destination = NonFaction.findNearestTo(agent.pos).pos
        return agent
    }
}
