package agent.action.cond

import agent.Agent
import agent.action.ActionItem
import items.QgressItem
import system.display.HackFx
import util.SoundUtil

object Hacker : ConditionalAction {
    override val actionItem = ActionItem.HACK

    override fun isActionPossible(agent: Agent) = agent.actionPortal.canHack(agent)

    override fun performAction(agent: Agent): Agent {
        agent.action.start(actionItem)
        val hackResult = agent.actionPortal.tryHack(agent)
        SoundUtil.playHackingSound(agent.actionPortal.location, agent.actionPortal.getLevel().toInt())
        // ENL spins clockwise, RES counter-clockwise; a plain hack (not a glyph).
        HackFx.record("portal:${agent.actionPortal.id}", agent.faction, glyph = false)
        val newStuff: List<QgressItem>? = hackResult.items
        if (newStuff != null) {
            agent.inventory.items.addAll(newStuff)
        }
        return agent
    }
}
