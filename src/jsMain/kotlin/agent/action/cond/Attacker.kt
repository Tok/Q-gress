package agent.action.cond

import agent.Agent
import agent.Inventory
import agent.action.ActionItem
import config.Constants
import items.XmpBurster
import items.level.XmpLevel
import system.display.Scene3D
import util.Util

object Attacker : ConditionalAction {
    override val actionItem = ActionItem.ATTACK

    // Only attack once the agent has hoarded enough XMPs to actually make a dent (taking a portal down
    // needs many bursts) — so agents commit to a real assault instead of one blast then wandering off.
    override fun isActionPossible(agent: Agent) = agent.inventory.findXmps().count() >= attackXmps

    override fun performAction(agent: Agent): Agent {
        // Sustained assault: keep firing volleys into the portal until it falls, the agent runs dry on
        // XMPs/XM, or a safety cap — instead of a single volley + a coin-flip to continue. This is what
        // makes portals actually flip (a lone volley barely scratches a defended portal).
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

    private fun fireVolley(agent: Agent, xmps: List<XmpBurster>) {
        doAttack(agent, xmps)
        // Detonate FIRST (records the blast origin) so the resonators/mods it destroys fly away from it.
        val topLevel = xmps.maxByOrNull { it.level.level }?.level ?: XmpLevel.ONE
        Scene3D.playXmpBurst(agent.pos, topLevel.level, sound = true)
        xmps.forEach { it.dealDamage(agent) } // resonator damage
        // One mod knock-out roll per volley (XMPs strip shields slowly; Ultra-Strikes would be far better).
        XmpBurster.knockMods(agent.actionPortal, agent.pos, topLevel, ultra = false, agent)
        agent.actionPortal.retaliate(agent) // the attacked portal zaps back (drains the attacker → a real cost)
    }

    private fun doAttack(agent: Agent, xmps: List<XmpBurster>) {
        xmps.forEach { xmpBurster ->
            when (xmpBurster.level.level) {
                1 -> agent.removeXm(10)
                2 -> agent.removeXm(20)
                3 -> agent.removeXm(70)
                4 -> agent.removeXm(140)
                5 -> agent.removeXm(250)
                6 -> agent.removeXm(360)
                7 -> agent.removeXm(490)
                else -> agent.removeXm(640)
            }
        }
    }

    private const val minAttackXmps = 10
    private const val maxAttackXmps = (minAttackXmps * Constants.phi).toInt()
    private const val attackXmps = 30 // hoard needed to start an assault (was 50 — too rarely reached → few flips)
    private const val maxVolleys = 12 // safety cap on one assault (XMPs run out well before this anyway)
    private fun attackXmpCount() = minAttackXmps + Util.randomInt(maxAttackXmps - minAttackXmps)
    private fun xmpsForAttack(inv: Inventory) = inv.findXmps().sortedByDescending { it.level }.take(attackXmpCount())
}
