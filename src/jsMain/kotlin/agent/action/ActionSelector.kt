package agent.action

import agent.Agent
import agent.Balance
import agent.Faction
import agent.Movement
import agent.action.cond.*
import agent.qvalue.QActions
import agent.qvalue.QValue
import ai.FactionPolicies
import config.Config
import util.Util

object ActionSelector {
    fun doSomethingElse(agent: Agent): Agent {
        // Leader tempo handicap: an agent of the LEADING faction wanders (a neutral, non-contributing move)
        // instead of acting, with probability leadShare × Config.leaderDistraction — so being ahead costs
        // tempo and the trailing side keeps its focus. Off (0) by default; an anti-runaway balance lever.
        if (Util.random() < Balance.leadShare(agent.faction) * Config.leaderDistraction) return agent.moveElsewhere()
        val portalFaction = agent.actionPortal.owner?.faction
        return when {
            !agent.isAtActionPortal() -> doAnywhereAction(agent)
            portalFaction == agent.faction -> doFriendlyPortalAction(agent)
            portalFaction == agent.faction.enemy() -> doEnemyPortalAction(agent)
            else -> doNeutralPortalAction(agent)
        }
    }

    // The faction's behaviour-slider weighting for [value] × the QValue's own weight. The weighting comes
    // from the faction's installed FactionPolicy (Phase 6.0) — by default DomSliderPolicy (the tuning
    // sliders, or 0.1 headless), so this is unchanged from the old inline DOM read.
    fun q(faction: Faction, value: QValue): Double = FactionPolicies.of(faction).weight(value) * value.weight

    // No-idle fallback: when nothing productive is eligible (portal on cooldown, empty inventory, capped
    // roster), the agent ROAMS open ground — a portal-independent stroll that sweeps up stray XM faster than
    // standing still, so it can act again sooner. Recruiting stays a weighted option in the lists above.
    private fun fallback(agent: Agent) = { Movement.wander(agent) }
    private fun doAnywhereAction(agent: Agent): Agent = Util.select(actionsForAnywhere(agent), fallback(agent)).invoke()
    private fun doNeutralPortalAction(agent: Agent): Agent = Util.select(actionsForNeutralPortals(agent), fallback(agent)).invoke()

    private fun doFriendlyPortalAction(agent: Agent): Agent = Util.select(actionsForFriendlyPortals(agent), fallback(agent)).invoke()

    private fun doEnemyPortalAction(agent: Agent): Agent = Util.select(actionsForEnemyPortals(agent), fallback(agent)).invoke()

    private fun actionsForAnywhere(agent: Agent): List<Pair<Double, () -> Agent>> {
        val moveElsewhereQ = q(agent.faction, QActions.MOVE_ELSEWHERE)
        val recycleQ = if (Recycler.isActionPossible(agent)) q(agent.faction, QActions.RECYCLE) else -1.0
        val rechargeQ = if (Recharger.isActionPossible(agent)) q(agent.faction, QActions.RECHARGE) else -1.0
        val recruitQ = if (Recruiter.isActionPossible(agent)) Recruiter.selectionWeight(agent.faction) else -1.0
        return listOf(
            moveElsewhereQ to { agent.moveElsewhere() },
            recycleQ to { Recycler.performAction(agent) },
            rechargeQ to { Recharger.performAction(agent) },
            recruitQ to { Recruiter.performAction(agent) },
        )
    }

    private fun actionsForPortals(agent: Agent): List<Pair<Double, () -> Agent>> {
        val basicValues = actionsForAnywhere(agent)
        val hackQ = if (Hacker.isActionPossible(agent)) q(agent.faction, QActions.HACK) else -1.0
        val glyphQ = if (Glypher.isActionPossible(agent)) q(agent.faction, QActions.GLYPH) else -1.0
        return basicValues + listOf(
            hackQ to { Hacker.performAction(agent) },
            glyphQ to { Glypher.performAction(agent) },
        )
    }

    private fun actionsForNeutralPortals(agent: Agent): List<Pair<Double, () -> Agent>> {
        val basicValues = actionsForPortals(agent)
        val captureQ = if (Deployer.isActionPossible(agent)) q(agent.faction, QActions.CAPTURE) else -1.0
        return basicValues + listOf(
            captureQ to { agent.capturePortal(true) },
        )
    }

    private fun actionsForFriendlyPortals(agent: Agent): List<Pair<Double, () -> Agent>> {
        val basicValues = actionsForPortals(agent)
        val deployQ = if (Deployer.isActionPossible(agent)) q(agent.faction, QActions.DEPLOY) else -1.0
        val linkQ = if (Linker.isActionPossible(agent)) q(agent.faction, QActions.LINK) else -1.0
        return basicValues + listOf(
            deployQ to { agent.deployPortal(true) },
            linkQ to { Linker.performAction(agent) },
        )
    }

    private fun actionsForEnemyPortals(agent: Agent): List<Pair<Double, () -> Agent>> {
        val basicValues = actionsForPortals(agent)
        val attackQ = if (Attacker.isActionPossible(agent)) q(agent.faction, QActions.ATTACK) else -1.0
        val virusQ = if (Refactorer.isActionPossible(agent)) q(agent.faction, QActions.VIRUS) else -1.0
        return basicValues + listOf(
            attackQ to { agent.attackPortal(true) },
            virusQ to { Refactorer.performAction(agent) },
        )
    }
}
