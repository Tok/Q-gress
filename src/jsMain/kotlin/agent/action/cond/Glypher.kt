package agent.action.cond

import agent.Agent
import agent.action.ActionItem
import items.QgressItem
import util.SoundUtil

object Glypher : ConditionalAction {
    override val actionItem = ActionItem.GLYPH

    override fun isActionPossible(agent: Agent) = Hacker.isActionPossible(agent)

    override fun performAction(agent: Agent): Agent {
        agent.action.start(actionItem)
        val glyphResult = agent.actionPortal.tryGlyph(agent)
        SoundUtil.playGlyphingSound(agent.actionPortal.location)
        val isSuccess = glyphResult.items != null
        if (isSuccess) {
            val newStuff: List<QgressItem> = glyphResult.items!!
            agent.inventory.items.addAll(newStuff)
        }
        return agent
    }
}
