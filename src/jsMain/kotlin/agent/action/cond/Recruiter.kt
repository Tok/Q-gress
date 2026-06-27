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

    // Recruiting is free — it's persuading a bystander to join, not an energy-spending field action. The
    // anti-snowball balancing lives in [selectionWeight] (smaller team recruits more) + the diminishing
    // [recruitmentChance], not an XM gate. Only condition: there's still roster room.
    override fun isActionPossible(agent: Agent) = World.canRecruitMore(agent.faction)

    /**
     * How strongly an agent weighs recruiting this tick. A fixed base ([Config.recruitWeight]) ×
     * [Balance.recruitFactor] (the SMALLER team recruits more, so team sizes self-balance) × the global
     * [Config.progressSpeed] knob (how fast the game ramps — also drives AP gain). (Future: an agent skill +
     * items — e.g. "beer" — could scale this; see PLAN.)
     */
    fun selectionWeight(faction: Faction): Double = Config.recruitWeight * Balance.recruitFactor(faction) * Config.progressSpeed

    // Success chance just diminishes as the faction fills toward its cap (rushing the cap yields ever-smaller
    // returns); the anti-snowball balancing now lives in [selectionWeight], not here, to avoid double-counting.
    private fun recruitmentChance(faction: Faction): Double =
        recruitSuccessProbability(World.countAgents(faction).toDouble() / Config.maxFor(faction))

    /** Diminishing-returns curve (pure math in [agent.BalanceMath.recruitSuccessProbability]): base chance ×
     *  the roster's remaining headroom (`1 − fillRatio`), so a near-full roster recruits at ~0. */
    fun recruitSuccessProbability(fillRatio: Double): Double =
        agent.BalanceMath.recruitSuccessProbability(fillRatio, Config.recruitmentBaseChance)

    /** Start a recruit: pick a random NPC and head over (Agent drives the walk + meeting). Free — no XM cost. */
    override fun performAction(agent: Agent): Agent {
        val npc = NonFaction.findRandom() ?: return agent // no NPCs (never, given MIN_NONFACTION) → caller re-selects
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
            // The NPC turns faction: it becomes the new agent in place (not deleted + spawned elsewhere).
            World.allNonFaction.remove(npc)
            // Don't top the crowd back up on every recruit — let the NPC population shrink as agents recruit,
            // only refilling once it has been drawn down to its floor (so we never run out of recruits).
            if (World.countNonFaction() < Config.MIN_NONFACTION) World.allNonFaction.add(NonFaction.create(World.grid))
            val newAgent = when (agent.faction) {
                Faction.ENL -> Agent.createFrog(World.grid, npc.pos)
                Faction.RES -> Agent.createSmurf(World.grid, npc.pos)
            }
            Com.addMessage("$newAgent has completed the training.", Com.Importance.MAJOR, newAgent.faction.color)
            World.pendingAgents.add(newAgent) // flushed after the agent loop (avoids CME)
            SoundUtil.playRecruitSuccess(agent.pos)
        } else {
            Com.addMessage("A recruit turned down the ${agent.faction.nickName}s.", Com.Importance.MINOR, agent.faction.color)
            SoundUtil.playRecruitFail(agent.pos)
        }
        agent.recruitTargetId = null
        agent.action.end()
        return agent
    }
}
