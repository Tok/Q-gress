package agent.action.cond

import agent.Agent
import agent.action.ActionItem
import items.QgressItem
import system.display.HackFx
import system.display.Scene3D
import util.HackSound
import util.HtmlUtil

object Glypher : ConditionalAction {
    override val actionItem = ActionItem.GLYPH

    override fun isActionPossible(agent: Agent) = Hacker.isActionPossible(agent)

    override fun performAction(agent: Agent): Agent {
        agent.action.start(actionItem)
        val glyphResult = agent.actionPortal.tryGlyph(agent)
        // Glyph spin grows with portal level (and, later, agent skill); whir is synced to that spin
        // and keyed by portal so a re-hack interrupts it.
        val id = "portal:${agent.actionPortal.id}"
        val level = agent.actionPortal.getLevel().toInt()
        val spin = HackFx.glyphDuration(level)
        val sp = Scene3D.animationSpeed.coerceAtLeast(0.1)
        HackSound.glyph(id, agent.actionPortal.location, level, spin / sp)
        // Glyph hacking gets the stronger collar animation (faster, wider, longer).
        HackFx.record(id, agent.faction, glyph = true, spin)
        val newStuff: List<QgressItem>? = glyphResult.items
        if (newStuff != null) {
            agent.inventory.items.addAll(newStuff)
            if (newStuff.isNotEmpty() && HtmlUtil.isRunningInBrowser()) {
                Scene3D.rewardFx(agent.actionPortal.location, agent.actionPortal.getLevel().toInt(), agent.pos, newStuff.size)
            }
        }
        return agent
    }
}
