package agent.action.cond

import World
import agent.Agent
import agent.Faction
import agent.NonFaction
import agent.action.ActionItem
import config.Config
import system.Com
import util.Util

object Recruiter : ConditionalAction {
    override val actionItem = ActionItem.RECRUIT

    // Recruiting now costs XM, so an agent must have energy to spare — this makes
    // growing the roster compete with linking/deploying instead of being free.
    override fun isActionPossible(agent: Agent) = World.canRecruitMore(agent.faction) && agent.xm >= Config.recruitmentXmCost

    // Success chance diminishes as the faction fills toward its cap, so rushing
    // the cap yields ever-smaller returns rather than a guaranteed head start.
    private fun recruitmentChance(faction: Faction): Double {
        val fillRatio = World.countAgents(faction).toDouble() / Config.maxFor(faction)
        return Config.recruitmentBaseChance * (1.0 - fillRatio)
    }

    override fun performAction(agent: Agent): Agent {
        agent.action.start(actionItem)
        agent.removeXm(Config.recruitmentXmCost)
        val npc = NonFaction.findNearestTo(agent.pos)
        if (Util.random() < recruitmentChance(agent.faction)) {
            World.allNonFaction.remove(npc)
            World.allNonFaction.add(NonFaction.create(World.grid)) // 1-for-1 replacement → population never depletes
            val newAgent = when (agent.faction) {
                Faction.ENL -> Agent.createFrog(World.grid)
                Faction.RES -> Agent.createSmurf(World.grid)
            }
            Com.addMessage("$newAgent has completed the training.")
            World.pendingAgents.add(newAgent) // flushed after the agent loop (avoids CME)
        }
        agent.destination = NonFaction.findNearestTo(agent.pos).pos
        return agent
    }
}
