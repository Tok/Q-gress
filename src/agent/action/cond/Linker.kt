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

    override fun isActionPossible(agent: Agent) = agent.actionPortal.canLinkOut(agent) &&
            !hasNoKeys(agent) &&
            targetOptions(agent).isNotEmpty()

    override fun performAction(agent: Agent): Agent {
        agent.action.start(actionItem)
        val linkOptions: List<Portal> = targetOptions(agent)
        val linkTarget = Util.shuffle(linkOptions).first()
        agent.actionPortal.createLink(agent, linkTarget)
        agent.action.end()
        return agent
    }

    private fun hasNoKeys(agent: Agent) = agent.keySet().isNullOrEmpty()

    private fun hasCrossLink(line: Line) = World.allLines().none { it -> it.doesIntersect(line) }
    private fun targetOptions(agent: Agent) =
            agent.actionPortal.findLinkableForKeys(agent)?.filter {
                it != agent.actionPortal && it.owner != null && !it.isDeprecated()
            }?.distinct()?.filterNot {
                val linkLine = Link.create(agent.actionPortal, it, agent)?.getLine()
                if (linkLine != null) hasCrossLink(linkLine) else false
            }.orEmpty()
}
