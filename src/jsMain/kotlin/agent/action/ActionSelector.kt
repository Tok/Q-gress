package agent.action

import agent.Agent
import agent.Balance
import agent.Faction
import agent.action.cond.*
import agent.qvalue.QActions
import agent.qvalue.QValue
import ai.FactionPolicies
import config.Config
import util.Rng

object ActionSelector {
    fun doSomethingElse(agent: Agent): Agent {
        // Leader tempo handicap: an agent of the LEADING faction wanders (a neutral, non-contributing move)
        // instead of acting, with probability leadShare × Config.leaderDistraction — so being ahead costs
        // tempo and the trailing side keeps its focus. Off (0) by default; an anti-runaway balance lever.
        if (Rng.random() < Balance.leadShare(agent.faction) * Config.leaderDistraction) return agent.moveElsewhere()
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

    // When the agent has a PRODUCTIVE action available (hack/deploy/attack/link/capture/glyph/recycle/recharge),
    // it acts on the 17 sliders — MOVE_ELSEWHERE competes too (AI-weighted relocation). When NOTHING productive is
    // possible (portals burnt out, nothing to do) it's IDLE: a capped few per faction RECRUIT a nearby NPC
    // ([Recruiter.canRecruit]) — recruiting is the idle thing, not a slider — and the rest roam to find work. The
    // [act] productive-check is what makes recruiting actually fire: MOVE_ELSEWHERE is always weakly positive, so a
    // plain Rng.select fallback would never trigger (the agent would just keep moving instead of ever recruiting).
    private fun act(agent: Agent, productive: List<Pair<Double, () -> Agent>>): Agent {
        if (productive.any { it.first > 0.0 }) {
            val withMove = productive + (q(agent.faction, QActions.MOVE_ELSEWHERE) to { agent.moveElsewhere() })
            return Rng.select(withMove, idle(agent)).invoke()
        }
        return idle(agent).invoke()
    }

    // Idle behaviour (no productive action AT the agent's current portal). Recruiting is the LAST resort: only an
    // agent already parked AT a portal with nothing to do (burnt out / fully built / hack on cooldown) recruits — and
    // only a capped few per faction. An agent that ISN'T at a portal still has work to seek, so it heads off to FIND
    // a portal ([moveElsewhere]) rather than recruit while there's hacking/capturing to be done. This keeps agents
    // from recruiting at game-start (they spawn away from portals → go capture first). Both render coin-less.
    private fun idle(agent: Agent): () -> Agent = {
        if (agent.isAtActionPortal() && Recruiter.canRecruit(agent)) Recruiter.performAction(agent) else agent.moveElsewhere()
    }

    private fun doAnywhereAction(agent: Agent): Agent = act(agent, actionsForAnywhere(agent))
    private fun doNeutralPortalAction(agent: Agent): Agent = act(agent, actionsForNeutralPortals(agent))
    private fun doFriendlyPortalAction(agent: Agent): Agent = act(agent, actionsForFriendlyPortals(agent))
    private fun doEnemyPortalAction(agent: Agent): Agent = act(agent, actionsForEnemyPortals(agent))

    // The PRODUCTIVE "anywhere" actions (NOT MOVE_ELSEWHERE — that's added by [act] only when productive work
    // exists, and is the idle roam otherwise).
    private fun actionsForAnywhere(agent: Agent): List<Pair<Double, () -> Agent>> {
        val recycleQ = if (Recycler.isActionPossible(agent)) q(agent.faction, QActions.RECYCLE) else -1.0
        val rechargeQ = if (Recharger.isActionPossible(agent)) q(agent.faction, QActions.RECHARGE) else -1.0
        return listOf(
            recycleQ to { Recycler.performAction(agent) },
            rechargeQ to { Recharger.performAction(agent) },
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
