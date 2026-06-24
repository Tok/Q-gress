package agent

import World
import config.Config

/**
 * Anti-snowball / comeback balance — the faction that's behind gets better odds to turn the board, so
 * neither **recruiting** nor **combat** runs away with the game (the recruit-rush + can't-retake-a-portal
 * dynamics that left the board static and lopsided). Pure reads over [World]; deterministic.
 */
object Balance {

    /**
     * Recruit-chance multiplier for [faction]: `< 1` when it already has the LARGER roster, `> 1` when it's
     * the SMALLER team — so the bigger faction recruits less and the smaller more, and team sizes
     * self-balance. Clamped to [[Config.recruitFactorMin], [Config.recruitFactorMax]].
     */
    fun recruitFactor(faction: Faction): Double {
        val mine = World.countAgents(faction)
        val theirs = World.countAgents(faction.enemy())
        return ((theirs + 1).toDouble() / (mine + 1)).coerceIn(Config.recruitFactorMin, Config.recruitFactorMax)
    }

    /**
     * Resonator-damage multiplier (`≥ 1`) for an attacker of [faction]: the side BEHIND on portals hits
     * harder (up to `1 + [Config.comebackAttackBonus]` at a full deficit), so a losing faction can claw
     * portals back — "attacking easier than defending". Exactly `1.0` when even or ahead (no penalty for
     * leading; the underdog is helped, the leader isn't punished).
     */
    fun attackBoost(faction: Faction): Double {
        val mine = World.countPortals(faction)
        val theirs = World.countPortals(faction.enemy())
        val total = (mine + theirs).coerceAtLeast(1)
        val deficit = ((theirs - mine).toDouble() / total).coerceAtLeast(0.0)
        return 1.0 + Config.comebackAttackBonus * deficit
    }
}
