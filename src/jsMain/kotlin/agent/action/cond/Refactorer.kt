package agent.action.cond

import agent.Agent
import agent.Faction
import agent.action.ActionItem
import items.deployable.Virus
import items.types.VirusType
import util.SoundUtil

/**
 * Using a virus (ENL → ADA Refactor, RES → JARVIS Virus) on an **enemy** portal flips it to the
 * agent's faction — `Portal.refactor` reassigns the resonators and drops the mods; the colour change
 * auto-animates via CaptureFx on the next sync.
 */
object Refactorer : ConditionalAction {
    override val actionItem = ActionItem.VIRUS

    override fun isActionPossible(agent: Agent): Boolean = agent.actionPortal.isEnemyOf(agent) && matchingVirus(agent) != null

    override fun performAction(agent: Agent): Agent {
        val virus = matchingVirus(agent) ?: return agent
        val portal = agent.actionPortal
        portal.refactor(agent)
        agent.inventory.items.remove(virus)
        SoundUtil.playVirusSound(portal.location, agent.faction)
        agent.action.start(ActionItem.VIRUS)
        return agent
    }

    private fun virusTypeFor(faction: Faction) = if (faction == Faction.ENL) VirusType.ADA_REFACTOR else VirusType.JARVIS_VIRUS

    private fun matchingVirus(agent: Agent): Virus? = agent.inventory.items.filterIsInstance<Virus>().firstOrNull { it.type == virusTypeFor(agent.faction) }
}
