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
 * Recruiting — the **idle / fallback** thing an agent does when it has no gameplay action left (its portals are
 * burnt out, nothing to hack/deploy/attack), exactly like EXPLORE/roam. It is NOT a Q-value slider and NOT in the
 * action roulette — agent action selection is purely the 17 sliders. [agent.action.ActionSelector] routes an idle
 * agent here via [canRecruit] (else it explores). To stop EVERY idle agent recruiting, only [Config.maxConcurrentRecruiters]
 * per faction recruit at once (the rest explore). [performAction] heads to a nearby NPC; [Agent] drives the walk +
 * "meeting"; [resolve] rolls success — scaled by [Config.progressSpeed] (the "faster" lever) — and converts the NPC.
 */
object Recruiter : ConditionalAction {
    override val actionItem = ActionItem.RECRUIT

    /** Roster cap check (used as the [ConditionalAction] gate + by [canRecruit]). */
    override fun isActionPossible(agent: Agent) = World.canRecruitMore(agent.faction)

    /** Should an IDLE [agent] recruit (vs explore)? Only if the roster has room AND fewer than
     *  [Config.maxConcurrentRecruiters] of its faction are already recruiting — so idle agents split between
     *  recruiting (a capped few) and exploring (the rest), never all recruiting at once. */
    fun canRecruit(agent: Agent): Boolean {
        if (agent.action.item == ActionItem.RECRUIT) return false // already on it
        if (!World.canRecruitMore(agent.faction)) return false
        val active = World.allAgents.count { it.faction == agent.faction && it.action.item == ActionItem.RECRUIT }
        return active < Config.maxConcurrentRecruiters
    }

    /** Start the walk-up: head to the NEAREST NPC (short, reliable walk so a capped recruit slot frees up
     *  quickly) and the Agent drives the walk + meeting. */
    override fun performAction(agent: Agent): Agent {
        val npc = NonFaction.findNearestTo(agent.pos) ?: return agent // no NPCs (never, given MIN_NONFACTION)
        agent.recruitTargetId = npc.id
        agent.destination = npc.pos
        agent.action.start(actionItem)
        return agent
    }

    /** Per-meeting success chance, scaled by progress speed + the anti-snowball factor + roster headroom, and by
     *  the recruiter's personal aptitude ([agent.Skills.recruitingFactor]) — so progress speed makes recruiting
     *  FASTER without more agents doing it. */
    private fun recruitmentChance(agent: Agent): Double = BalanceMath.recruitChance(
        World.countAgents(agent.faction).toDouble() / Config.maxFor(agent.faction),
        Balance.recruitFactor(agent.faction),
        Config.progressSpeed,
        Config.recruitmentBaseChance,
    ) * agent.skills.recruitingFactor()

    /**
     * Finish a recruit once [agent] has stood with [npc] for the meeting: roll [recruitmentChance] and, on success,
     * turn the NPC into a teammate in place. Clears the agent's recruit state + ends the action either way (so it
     * returns to normal slider-driven behaviour and frees its recruiting slot).
     */
    fun resolve(agent: Agent, npc: NonFaction): Agent {
        if (World.canRecruitMore(agent.faction) && Rng.random() < recruitmentChance(agent)) {
            World.allNonFaction.remove(npc) // the NPC turns faction in place (not deleted + spawned elsewhere)
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
        } else {
            Com.addMessage("A recruit turned down the ${agent.faction.nickName}s.", Com.Importance.MINOR, agent.faction.color)
            Sound.playRecruitFail(agent.pos)
        }
        agent.recruitTargetId = null
        agent.action.end()
        return agent
    }
}
