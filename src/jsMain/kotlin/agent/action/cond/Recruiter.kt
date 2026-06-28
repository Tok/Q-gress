package agent.action.cond

import World
import agent.Agent
import agent.Balance
import agent.BalanceMath
import agent.Faction
import agent.NonFaction
import agent.action.ActionItem
import config.Config
import system.Com
import system.audio.Sound
import system.audio.Tts
import util.Rng

/**
 * Recruiting — a faction-NEUTRAL **system process** (like the density-driven portal churn / the retired EXPLORE),
 * NOT an agent Q-action: agent action selection is purely the 17 sliders, never a code override. [system.Cycle]
 * drives recruiting each checkpoint at a rate from [expectedRecruits] (a base rate × the game-ramp [Config.progressSpeed]
 * × the anti-snowball [Balance.recruitFactor] × roster headroom). Each event [startRecruit] commandeers one agent to
 * walk up to a random NPC ([performAction] sets its RECRUIT action); [Agent] drives the walk + "meeting", and
 * [resolve] converts the NPC into a teammate at the end. To recruit FASTER, the lever is the rate (progress speed),
 * not the number of agents choosing it.
 */
object Recruiter : ConditionalAction {
    override val actionItem = ActionItem.RECRUIT

    /** Only condition: the faction's (size-scaled) roster isn't full yet ([World.canRecruitMore]). */
    override fun isActionPossible(agent: Agent) = World.canRecruitMore(agent.faction)

    /** Expected recruits for [faction] this checkpoint — the system-driven rate ([BalanceMath.recruitsPerCheckpoint]:
     *  base × progress speed × anti-snowball factor × roster headroom). */
    fun expectedRecruits(faction: Faction): Double = BalanceMath.recruitsPerCheckpoint(
        World.countAgents(faction).toDouble() / Config.maxFor(faction),
        Balance.recruitFactor(faction),
        Config.progressSpeed,
        Config.recruitRate,
    )

    /**
     * Commandeer one available agent of [faction] — weighted by recruiting aptitude ([agent.Skills.recruitingFactor],
     * so natural recruiters do it more) — to walk up to a random NPC. Returns false if the roster is full or no
     * free agent exists. Called by the system process, never the agent's own action roulette.
     */
    fun startRecruit(faction: Faction): Boolean {
        if (!World.canRecruitMore(faction)) return false
        val candidates = World.allAgents.filter { it.faction == faction && it.action.item != ActionItem.RECRUIT }
        if (candidates.isEmpty()) return false
        val agent = Rng.select(candidates.map { it.skills.recruitingFactor() to it }, candidates.first())
        return performAction(agent).recruitTargetId != null
    }

    /** Start the walk-up on [agent]: pick a random NPC and head over (Agent drives the walk + meeting). */
    override fun performAction(agent: Agent): Agent {
        val npc = NonFaction.findRandom() ?: return agent // no NPCs (never, given MIN_NONFACTION)
        agent.recruitTargetId = npc.id
        agent.destination = npc.pos
        agent.action.start(actionItem)
        return agent
    }

    /**
     * Finish a recruit once [agent] has stood with [npc] for the meeting: convert the NPC into a teammate in place.
     * The recruiting RATE is gated upstream by the system process, so a completed walk-up succeeds — unless the
     * roster filled in the meantime ([World.canRecruitMore]). Clears the agent's recruit state + ends the action.
     */
    fun resolve(agent: Agent, npc: NonFaction): Agent {
        if (World.canRecruitMore(agent.faction)) {
            // The NPC turns faction: it becomes the new agent in place (not deleted + spawned elsewhere).
            World.allNonFaction.remove(npc)
            // Let the NPC population draw down as agents recruit; only refill once it hits its floor (never run out).
            if (World.countNonFaction() < Config.MIN_NONFACTION) World.allNonFaction.add(NonFaction.create(World.grid))
            val newAgent = when (agent.faction) {
                Faction.ENL -> Agent.createFrog(World.grid, npc.pos)
                Faction.RES -> Agent.createSmurf(World.grid, npc.pos)
            }
            Com.addMessage("$newAgent has completed the training.", Com.Importance.MAJOR, newAgent.faction.color)
            World.pendingAgents.add(newAgent) // flushed after the agent loop (avoids CME)
            Sound.playRecruitSuccess(agent.pos)
            Tts.announceRecruitment(agent.faction) // VERBOSE TTS
        }
        agent.recruitTargetId = null
        agent.action.end()
        return agent
    }
}
