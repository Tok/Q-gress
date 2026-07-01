package agent.action.cond

import agent.Agent
import agent.Inventory
import agent.action.ActionItem
import config.Config
import config.Constants
import items.UltraStrike
import items.XmpBurster
import items.level.XmpLevel
import portal.Portal
import system.effect.Fx
import util.Rng

object Attacker : ConditionalAction {
    override val actionItem = ActionItem.ATTACK

    // Only attack once the agent has hoarded enough XMPs to actually make a dent (taking a portal down
    // needs many bursts) — so agents commit to a real assault instead of one blast then wandering off.
    // AND it must have XM to fire: an agent drained to 0 XM can't loose a single burst, so without this
    // it would re-enter ATTACK every tick firing nothing (the 0-XM sibling of the dead-target loop —
    // performAction's volley loop is gated on `agent.xm > 0`). Requiring XM here makes attackPortal end
    // the action (→ re-select: hack/recharge/move to recover XM) and stops the selector re-offering it.
    override fun isActionPossible(agent: Agent) = agent.inventory.findXmps().count() >= Config.attackXmpThreshold() && agent.xm > 0

    override fun performAction(agent: Agent): Agent {
        // Pin the target for the whole assault. Taking a portal down calls Portal.destroy, which reassigns
        // agent.actionPortal to a random (far) portal — so re-reading agent.actionPortal mid-assault redirected
        // the volleys, the damage number, the mod-knock AND the retaliation bolt at that random portal (the
        // "zapped by a portal half a screen away" bug, and the loop kept firing on the far portal). Commit to
        // the portal the agent walked up to; when IT falls the loop stops.
        val target = agent.actionPortal
        // 1) Strip the portal's shields with Ultra-Strikes FIRST, so the bursts below aren't soaked up by
        //    shield mitigation (the reason shielded portals "barely change" — XMPs were hitting through a wall).
        stripShields(agent, target)
        // 2) Sustained burst assault: keep firing volleys until the portal falls, the agent runs dry on
        //    XMPs/XM, or a safety cap — a single volley barely scratches a defended portal.
        var volleys = 0
        while (volleys < maxVolleys && agent.xm > 0 && isWorthAttacking(agent, target)) {
            val xmps = xmpsForAttack(agent.inventory)
            if (xmps.isEmpty()) break
            fireVolley(agent, target, xmps)
            agent.inventory.consumeXmps(xmps)
            volleys++
        }
        return agent
    }

    // Worth firing while the target is still valid AND the agent still has XMPs to fire.
    private fun isWorthAttacking(agent: Agent, target: Portal) = agent.inventory.findXmps().isNotEmpty() && isTargetValid(agent, target)

    /**
     * The target is still a worthwhile assault: an enemy-held (or neutral) portal that still has resonators
     * to destroy. Goes false the moment the portal falls (resos gone — including right after this agent
     * takes it down) or flips to our own faction. The committed ATTACK action must then be abandoned (see
     * [Agent.attackPortal]) so the agent re-selects, instead of re-firing forever on a dead target while
     * still holding XMPs (the "stuck attacking nothing" symptom). [portal] defaults to the agent's current
     * action portal (the next-tick lifecycle check); the assault loop passes its pinned target explicitly.
     */
    fun isTargetValid(agent: Agent, portal: Portal = agent.actionPortal): Boolean =
        portal.owner?.faction != agent.faction && portal.numberOfResosLeft() > 0

    // Spend Ultra-Strikes up front to knock the portal's mods (shields) off. US barely dent resonators but
    // excel at stripping mods — this is what they're FOR. Each shield removed lifts the mitigation that was
    // gutting burst damage. Stops when the portal is clear of mods, or the agent is out of US / XM.
    private fun stripShields(agent: Agent, target: Portal) {
        val rangeLevel = agent.inventory.findXmps().maxByOrNull { it.level.level }?.level ?: XmpLevel.EIGHT
        var strikes = 0
        var us = affordableUltraStrike(agent)
        while (us != null && strikes < maxVolleys && target.modCount() > 0) {
            agent.removeXm(us.level.xmCost)
            Fx.sink.playXmpBurst(agent.pos, us.level.level, sound = true)
            XmpBurster.knockMods(target, agent.pos, rangeLevel, ultra = true, agent)
            agent.inventory.consumeUltraStrikes(listOf(us))
            strikes++
            us = affordableUltraStrike(agent) // xm dropped → re-check what we can still fire
        }
    }

    // The best Ultra-Strike the agent can currently afford to fire, or null.
    private fun affordableUltraStrike(agent: Agent): UltraStrike? =
        agent.inventory.findUltraStrikes().filter { agent.xm >= it.level.xmCost }.maxByOrNull { it.level.level }

    private fun fireVolley(agent: Agent, target: Portal, xmps: List<XmpBurster>) {
        doAttack(agent, xmps)
        // Detonate FIRST (records the blast origin) so the resonators/mods it destroys fly away from it.
        val topLevel = xmps.maxByOrNull { it.level.level }?.level ?: XmpLevel.ONE
        Fx.sink.playXmpBurst(agent.pos, topLevel.level, sound = true)
        val damage = xmps.sumOf { it.dealDamage(agent) } // resonator damage (summed for one floating number)
        if (damage > 0) Fx.sink.showDamageNumber(target, damage)
        // Bursts also chip at any mods the opening Ultra-Strike salvo didn't get (or that got redeployed).
        XmpBurster.knockMods(target, agent.pos, topLevel, ultra = false, agent)
        // The attacked portal zaps back (drains the attacker → a real cost). A destroyed target has owner == null,
        // so this no-ops — you're never zapped by a portal you just took down.
        target.retaliate(agent)
    }

    // Firing costs XM at the authentic per-level rate (XmpLevel.xmCost). Agents sustain assaults by managing
    // energy — collecting stray XM (Seeker) + recharging — not by us discounting the cost.
    private fun doAttack(agent: Agent, xmps: List<XmpBurster>) {
        xmps.forEach { agent.removeXm(it.level.xmCost) }
    }

    private const val minAttackXmps = 10
    private const val maxAttackXmps = (minAttackXmps * Constants.phi).toInt()
    private const val maxVolleys = 12 // safety cap on one assault (XMPs run out well before this anyway)
    private fun attackXmpCount() = minAttackXmps + Rng.randomInt(maxAttackXmps - minAttackXmps)
    private fun xmpsForAttack(inv: Inventory) = inv.findXmps().sortedByDescending { it.level }.take(attackXmpCount())
}
