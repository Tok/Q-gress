package agent.action.cond

import agent.Agent
import agent.action.ActionItem
import items.QgressItem
import items.rewardMote
import system.display.HackFx
import system.display.Scene3D
import system.effect.Fx
import util.HackSound

object Hacker : ConditionalAction {
    override val actionItem = ActionItem.HACK

    // Can't hack for more items when the inventory is full (Ingress rule) — the agent must spend or recycle first.
    override fun isActionPossible(agent: Agent) = !agent.inventory.isFull() && agent.actionPortal.canHack(agent)

    override fun performAction(agent: Agent): Agent {
        agent.action.start(actionItem)
        val hackResult = agent.actionPortal.tryHack(agent)
        // Sound keyed by portal + synced to the collar spin (re-hack interrupts it). Wall-clock length =
        // spin time / sim speed so it tracks the sim-scaled animation; slots drive the per-reso clicks.
        val id = "portal:${agent.actionPortal.id}"
        val sp = Scene3D.animationSpeed.coerceAtLeast(0.1)
        val slots = IntArray(8)
        agent.actionPortal.resoMap().forEach { (oct, reso) -> slots[oct.ordinal] = reso.getLevel() }
        HackSound.hack(id, agent.actionPortal.location, HackFx.HACK_S / sp, agent.faction, slots)
        // ENL spins clockwise, RES counter-clockwise; a plain hack (not a glyph).
        Fx.sink.recordHack(id, agent.faction, glyph = false, HackFx.HACK_S)
        val newStuff: List<QgressItem>? = hackResult.items
        if (newStuff != null) {
            agent.inventory.items.addAll(newStuff)
            if (newStuff.isNotEmpty()) {
                Fx.sink.rewardFx(
                    agent.actionPortal.location,
                    agent.actionPortal.getLevel().toInt(),
                    agent.pos,
                    newStuff.map { it.rewardMote(agent.faction) },
                )
            }
        }
        agent.actionPortal.retaliate(agent) // an enemy portal zaps the intruder (no-op on friendly/neutral)
        return agent
    }
}
