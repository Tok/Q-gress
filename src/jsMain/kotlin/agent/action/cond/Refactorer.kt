package agent.action.cond

import agent.Agent
import agent.action.ActionItem
import items.deployable.Virus
import system.audio.Sound

/**
 * Using a flip item (JARVIS Virus or ADA Refactor — **either faction may carry and use either one**)
 * flips a portal to the **item's** target faction ([items.types.VirusType.flipsTo]), not the agent's:
 * `Portal.refactor` re-owns the portal + all of its slot content (resonators + mods stay) and tears down
 * its links/fields. So an agent can attack-flip an **enemy** portal to its own colour with the matching
 * item, OR friendly-flip its **own** portal to the enemy colour with the off-colour item. Only
 * faction-owned portals can be flipped (never a neutral one), and only once per flip-immunity window
 * ([Portal.isFlippable]). The orb re-skins to the new colour without the capture shatter.
 */
object Refactorer : ConditionalAction {
    override val actionItem = ActionItem.VIRUS

    override fun isActionPossible(agent: Agent): Boolean = usableVirus(agent) != null

    override fun performAction(agent: Agent): Agent {
        val virus = usableVirus(agent) ?: return agent
        val portal = agent.actionPortal
        portal.refactor(agent, virus.type.flipsTo)
        agent.inventory.items.remove(virus)
        Sound.playVirusSound(portal.location, virus.type.flipsTo) // glitch sweep pitched to the NEW colour
        agent.action.start(ActionItem.VIRUS)
        return agent
    }

    /** The first held virus that would actually flip [agent]'s current portal: it must be faction-owned
     *  (neutral portals can't be flipped), off the flip-immunity cooldown, and the virus's target faction
     *  must differ from the portal's current one (can't flip a portal to the colour it already is). */
    private fun usableVirus(agent: Agent): Virus? {
        val portal = agent.actionPortal
        val ownerFaction = portal.owner?.faction ?: return null // neutral → not flippable
        if (!portal.isFlippable()) return null
        return agent.inventory.items.filterIsInstance<Virus>().firstOrNull { it.type.flipsTo != ownerFaction }
    }
}
