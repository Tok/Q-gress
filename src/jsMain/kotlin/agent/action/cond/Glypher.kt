package agent.action.cond

import agent.Agent
import agent.action.ActionItem
import items.QgressItem
import system.display.HackFx
import util.SoundUtil

object Glypher : ConditionalAction {
    override val actionItem = ActionItem.GLYPH

    override fun isActionPossible(agent: Agent) = Hacker.isActionPossible(agent)

    override fun performAction(agent: Agent): Agent {
        agent.action.start(actionItem)
        val glyphResult = agent.actionPortal.tryGlyph(agent)
        SoundUtil.playGlyphingSound(agent.actionPortal.location, agent.actionPortal.getLevel().toInt())
        // Glyph hacking gets the stronger collar animation (faster, wider, longer).
        HackFx.record("portal:${agent.actionPortal.id}", agent.faction, glyph = true)
        val newStuff: List<QgressItem>? = glyphResult.items
        if (newStuff != null) {
            agent.inventory.items.addAll(newStuff)
        }
        return agent
    }
}
