package agent.action.cond

import World
import agent.Agent
import agent.action.ActionItem
import portal.Link
import portal.Portal
import util.Util
import util.data.Line

object Linker : ConditionalAction {
    override val actionItem = ActionItem.LINK

    override fun isActionPossible(agent: Agent): Boolean {
        val canLinkOut = agent.actionPortal.canLinkOut(agent)
        val hasKeys = hasFriendlyKeys(agent)
        val hasTargets = targetOptions(agent).isNotEmpty()
        return canLinkOut && hasKeys && hasTargets
    }

    override fun performAction(agent: Agent): Agent {
        agent.action.start(actionItem)
        val linkOptions: List<Portal> = targetOptions(agent)
        val linkTarget = Util.shuffle(linkOptions).first()
        agent.actionPortal.createLink(agent, linkTarget)
        return agent
    }

    private fun hasFriendlyKeys(agent: Agent) = agent.keySet().any { it.isFriendlyToOwner() }
    private fun linkable(agent: Agent) = agent.actionPortal.findLinkableForKeys(agent).distinct()
    private fun hasNoCrossLinks(newline: Line) = World.allLines().none { it.doesIntersect(newline) }
    private fun targetOptions(agent: Agent) = linkable(agent).filter {
        it != agent.actionPortal && it.owner != null && !it.isDeprecated()
    }.filter {
        val linkLine = Link.create(agent.actionPortal, it, agent)?.getLine()
        linkLine != null && hasNoCrossLinks(linkLine)
    }
}
