package agent.action.cond

import agent.Agent
import agent.action.ActionItem
import items.deployable.Virus
import util.SoundUtil

/**
 * Using a virus (ADA Refactor or JARVIS Virus — **either faction may carry and use either one**) on an
 * **enemy** portal flips it to the agent's faction: `Portal.refactor` re-owns the portal and all of its
 * slot content (resonators + mods stay) and tears down its links/fields. The orb re-skins to the new
 * colour without the capture shatter.
 */
object Refactorer : ConditionalAction {
    override val actionItem = ActionItem.VIRUS

    override fun isActionPossible(agent: Agent): Boolean = agent.actionPortal.isEnemyOf(agent) && anyVirus(agent) != null

    override fun performAction(agent: Agent): Agent {
        val virus = anyVirus(agent) ?: return agent
        val portal = agent.actionPortal
        portal.refactor(agent)
        agent.inventory.items.remove(virus)
        SoundUtil.playVirusSound(portal.location, agent.faction)
        agent.action.start(ActionItem.VIRUS)
        return agent
    }

    private fun anyVirus(agent: Agent): Virus? = agent.inventory.items.filterIsInstance<Virus>().firstOrNull()
}
