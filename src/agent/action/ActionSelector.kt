package agent.action

import agent.Agent
import agent.Faction
import agent.action.cond.*
import agent.qvalue.QActions
import agent.qvalue.QValue
import org.w3c.dom.HTMLInputElement
import util.Util
import kotlin.browser.window

object ActionSelector {
    fun doSomething(agent: Agent): Agent {
        val portalFaction = agent.actionPortal.owner?.faction
        return when {
            !agent.isAtActionPortal() -> doAnywhereAction(agent)
            portalFaction == agent.faction -> doFriendlyPortalAction(agent)
            portalFaction?.isEnemy(agent.faction) ?: false -> doEnemyPortalAction(agent)
            else -> doNeutralPortalAction(agent)
        }
    }

    fun q(faction: Faction, value: QValue): Double {
        val id = value.id + "Slider" + faction.nickName
        val slider = window.document.getElementById(id) as HTMLInputElement
        return slider.valueAsNumber * value.weight
    }

    private fun default(agent: Agent) = { agent.doNothing() }
    private fun doAnywhereAction(agent: Agent): Agent = Util.select(actionsForAnywhere(agent), default(agent)).invoke()
    private fun doNeutralPortalAction(agent: Agent): Agent = Util.select(actionsForNeutralPortals(agent), default(agent)).invoke()
    private fun doFriendlyPortalAction(agent: Agent): Agent = Util.select(actionsForFriendlyPortals(agent), default(agent)).invoke()
    private fun doEnemyPortalAction(agent: Agent): Agent = Util.select(actionsForEnemyPortals(agent), default(agent)).invoke()

    private fun actionsForAnywhere(agent: Agent): List<Pair<Double, () -> Agent>> {
        val moveElsewhereQ = q(agent.faction, QActions.MOVE_ELSEWHERE)
        val recycleQ = if (Recycler.isActionPossible(agent)) q(agent.faction, QActions.RECYCLE) else -1.0
        val rechargeQ = if (Recharger.isActionPossible(agent)) q(agent.faction, QActions.RECHARGE) else -1.0
        val recruitQ = if (Recruiter.isActionPossible(agent)) q(agent.faction, QActions.RECRUIT) else -1.0
        return listOf(
                moveElsewhereQ to { agent.moveElsewhere() },
                recycleQ to { Recycler.performAction(agent) },
                rechargeQ to { Recharger.performAction(agent) },
                recruitQ to { Recruiter.performAction(agent) }
        )
    }

    private fun actionsForPortals(agent: Agent): List<Pair<Double, () -> Agent>> {
        val basicValues = actionsForAnywhere(agent)
        val hackQ = if (Hacker.isActionPossible(agent)) q(agent.faction, QActions.HACK) else -1.0
        return basicValues + listOf(hackQ to { Hacker.performAction(agent) })
    }

    private fun actionsForNeutralPortals(agent: Agent): List<Pair<Double, () -> Agent>> {
        val basicValues = actionsForPortals(agent)
        val captureQ = if (Deployer.isActionPossible(agent)) q(agent.faction, QActions.CAPTURE) else -1.0
        return basicValues + listOf(captureQ to { agent.deployPortal(true) })
    }

    private fun actionsForFriendlyPortals(agent: Agent): List<Pair<Double, () -> Agent>> {
        val basicValues = actionsForPortals(agent)
        val deployQ = if (Deployer.isActionPossible(agent)) q(agent.faction, QActions.DEPLOY) else -1.0
        val linkQ = if (Linker.isActionPossible(agent)) q(agent.faction, QActions.LINK) else -1.0
        return basicValues + listOf(
                deployQ to { agent.deployPortal(true) },
                linkQ to { Linker.performAction(agent) }
        )
    }

    private fun actionsForEnemyPortals(agent: Agent): List<Pair<Double, () -> Agent>> {
        val basicValues = actionsForPortals(agent)
        val attackQ = if (Attacker.isActionPossible(agent)) q(agent.faction, QActions.ATTACK) else -1.0
        return basicValues + listOf(attackQ to { agent.attackPortal(true) })
    }
}
