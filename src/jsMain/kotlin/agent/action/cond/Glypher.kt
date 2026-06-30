package agent.action.cond

import agent.Agent
import agent.action.ActionItem
import agent.action.HackTiming
import items.QgressItem
import items.rewardMote
import system.audio.Snd
import system.effect.Fx

object Glypher : ConditionalAction {
    override val actionItem = ActionItem.GLYPH

    override fun isActionPossible(agent: Agent) = Hacker.isActionPossible(agent)

    override fun performAction(agent: Agent): Agent {
        agent.action.start(actionItem)
        val glyphResult = agent.actionPortal.tryGlyph(agent)
        // Glyph spin grows with portal level (and, later, agent skill); sound synced to that spin, keyed
        // by portal so a re-hack interrupts it; slots drive the per-reso clicks.
        val id = "portal:${agent.actionPortal.id}"
        val level = agent.actionPortal.getLevel().toInt()
        val spin = HackTiming.glyphDuration(level)
        val slots = IntArray(8)
        agent.actionPortal.resoMap().forEach { (oct, reso) -> slots[oct.ordinal] = reso.getLevel() }
        Snd.sink.glyph(id, agent.actionPortal.location, level, spin, agent.faction, slots)
        // Glyph hacking gets the stronger collar animation (faster, wider, longer).
        Fx.sink.recordHack(id, agent.faction, glyph = true, spin)
        val newStuff: List<QgressItem>? = glyphResult.items
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
        return agent
    }
}
