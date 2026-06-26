package agent.action.cond

import World
import agent.Agent
import agent.Balance
import agent.Faction
import agent.NonFaction
import agent.action.ActionItem
import config.Config
import system.Com
import util.SoundUtil
import util.Util

/**
 * Recruiting — a portal-INDEPENDENT action: the agent walks up to a *random* NPC, the two stand together for a
 * short "meeting" ([ActionItem.RECRUIT] duration), then it resolves. [performAction] only *starts* it (picks
 * the target, pays the XM, heads off); [Agent] drives the walk-up + meeting per tick and calls [resolve] at the
 * end. Success rolls [recruitmentChance] (diminishing as the roster fills) and, with the anti-snowball
 * [selectionWeight], helps the SMALLER team grow faster.
 */
object Recruiter : ConditionalAction {
    override val actionItem = ActionItem.RECRUIT

    // Recruiting now costs XM, so an agent must have energy to spare — this makes
    // growing the roster compete with linking/deploying instead of being free.
    override fun isActionPossible(agent: Agent) = World.canRecruitMore(agent.faction) && agent.xm >= Config.recruitmentXmCost

    /**
     * How strongly an agent weighs recruiting this tick — NO LONGER a tuning slider (it was too snowbally to
     * hand anyone a crank). A fixed base ([Config.recruitWeight]) × [Balance.recruitFactor]: the SMALLER team
     * recruits more often, the LARGER less, so team sizes self-balance. (Future: an agent skill + items — e.g.
     * "beer" — could scale this; see PLAN.)
     */
    fun selectionWeight(faction: Faction): Double = Config.recruitWeight * Balance.recruitFactor(faction)

    // Success chance just diminishes as the faction fills toward its cap (rushing the cap yields ever-smaller
    // returns); the anti-snowball balancing now lives in [selectionWeight], not here, to avoid double-counting.
    private fun recruitmentChance(faction: Faction): Double {
        val fillRatio = World.countAgents(faction).toDouble() / Config.maxFor(faction)
        return Config.recruitmentBaseChance * (1.0 - fillRatio)
    }

    /** Start a recruit: pick a random NPC, pay the XM up front, and head over (Agent drives the walk + meeting). */
    override fun performAction(agent: Agent): Agent {
        val npc = NonFaction.findRandom() ?: return agent // no NPCs (never, given MIN_NONFACTION) → caller re-selects
        agent.removeXm(Config.recruitmentXmCost)
        agent.recruitTargetId = npc.id
        agent.destination = npc.pos
        agent.action.start(actionItem)
        return agent
    }

    /**
     * Finish a recruit once the agent has stood with [npc] for the meeting: roll success (diminishing as the
     * roster fills), spawn a teammate + replace the NPC on success, and play the success / fail sound. Clears
     * the agent's recruit state and ends the action either way.
     */
    fun resolve(agent: Agent, npc: NonFaction): Agent {
        if (Util.random() < recruitmentChance(agent.faction)) {
            World.allNonFaction.remove(npc)
            World.allNonFaction.add(NonFaction.create(World.grid)) // 1-for-1 replacement → population never depletes
            val newAgent = when (agent.faction) {
                Faction.ENL -> Agent.createFrog(World.grid)
                Faction.RES -> Agent.createSmurf(World.grid)
            }
            Com.addMessage("$newAgent has completed the training.")
            World.pendingAgents.add(newAgent) // flushed after the agent loop (avoids CME)
            SoundUtil.playRecruitSuccess(agent.pos)
        } else {
            Com.addMessage("A recruit turned down the ${agent.faction.nickName}s.")
            SoundUtil.playRecruitFail(agent.pos)
        }
        agent.recruitTargetId = null
        agent.action.end()
        return agent
    }
}
