package agent.action.cond

import agent.Agent
import agent.Inventory
import agent.action.ActionItem
import config.Config
import config.Constants
import items.UltraStrike
import items.XmpBurster
import items.level.XmpLevel
import system.effect.Fx
import util.Util

object Attacker : ConditionalAction {
    override val actionItem = ActionItem.ATTACK

    // Only attack once the agent has hoarded enough XMPs to actually make a dent (taking a portal down
    // needs many bursts) — so agents commit to a real assault instead of one blast then wandering off.
    override fun isActionPossible(agent: Agent) = agent.inventory.findXmps().count() >= Config.attackXmpThreshold()

    override fun performAction(agent: Agent): Agent {
        // 1) Strip the portal's shields with Ultra-Strikes FIRST, so the bursts below aren't soaked up by
        //    shield mitigation (the reason shielded portals "barely change" — XMPs were hitting through a wall).
        stripShields(agent)
        // 2) Sustained burst assault: keep firing volleys until the portal falls, the agent runs dry on
        //    XMPs/XM, or a safety cap — a single volley barely scratches a defended portal.
        var volleys = 0
        while (volleys < maxVolleys && agent.xm > 0 && isWorthAttacking(agent)) {
            val xmps = xmpsForAttack(agent.inventory)
            if (xmps.isEmpty()) break
            fireVolley(agent, xmps)
            agent.inventory.consumeXmps(xmps)
            volleys++
        }
        return agent
    }

    // Worth firing while the target is still an enemy portal that has resonators left to destroy.
    private fun isWorthAttacking(agent: Agent): Boolean {
        val portal = agent.actionPortal
        return agent.inventory.findXmps().isNotEmpty() &&
            portal.owner?.faction != agent.faction &&
            portal.numberOfResosLeft() > 0
    }

    // Spend Ultra-Strikes up front to knock the portal's mods (shields) off. US barely dent resonators but
    // excel at stripping mods — this is what they're FOR. Each shield removed lifts the mitigation that was
    // gutting burst damage. Stops when the portal is clear of mods, or the agent is out of US / XM.
    private fun stripShields(agent: Agent) {
        val portal = agent.actionPortal
        val rangeLevel = agent.inventory.findXmps().maxByOrNull { it.level.level }?.level ?: XmpLevel.EIGHT
        var strikes = 0
        var us = affordableUltraStrike(agent)
        while (us != null && strikes < maxVolleys && portal.modCount() > 0) {
            agent.removeXm(us.level.xmCost)
            Fx.sink.playXmpBurst(agent.pos, us.level.level, sound = true)
            XmpBurster.knockMods(portal, agent.pos, rangeLevel, ultra = true, agent)
            agent.inventory.consumeUltraStrikes(listOf(us))
            strikes++
            us = affordableUltraStrike(agent) // xm dropped → re-check what we can still fire
        }
    }

    // The best Ultra-Strike the agent can currently afford to fire, or null.
    private fun affordableUltraStrike(agent: Agent): UltraStrike? =
        agent.inventory.findUltraStrikes().filter { agent.xm >= it.level.xmCost }.maxByOrNull { it.level.level }

    private fun fireVolley(agent: Agent, xmps: List<XmpBurster>) {
        doAttack(agent, xmps)
        // Detonate FIRST (records the blast origin) so the resonators/mods it destroys fly away from it.
        val topLevel = xmps.maxByOrNull { it.level.level }?.level ?: XmpLevel.ONE
        Fx.sink.playXmpBurst(agent.pos, topLevel.level, sound = true)
        val damage = xmps.sumOf { it.dealDamage(agent) } // resonator damage (summed for one floating number)
        if (damage > 0) Fx.sink.showDamageNumber(agent.actionPortal, damage)
        // Bursts also chip at any mods the opening Ultra-Strike salvo didn't get (or that got redeployed).
        XmpBurster.knockMods(agent.actionPortal, agent.pos, topLevel, ultra = false, agent)
        agent.actionPortal.retaliate(agent) // the attacked portal zaps back (drains the attacker → a real cost)
    }

    // Firing costs XM at the authentic per-level rate (XmpLevel.xmCost). Agents sustain assaults by managing
    // energy — collecting stray XM (Seeker) + recharging — not by us discounting the cost.
    private fun doAttack(agent: Agent, xmps: List<XmpBurster>) {
        xmps.forEach { agent.removeXm(it.level.xmCost) }
    }

    private const val minAttackXmps = 10
    private const val maxAttackXmps = (minAttackXmps * Constants.phi).toInt()
    private const val maxVolleys = 12 // safety cap on one assault (XMPs run out well before this anyway)
    private fun attackXmpCount() = minAttackXmps + Util.randomInt(maxAttackXmps - minAttackXmps)
    private fun xmpsForAttack(inv: Inventory) = inv.findXmps().sortedByDescending { it.level }.take(attackXmpCount())
}
