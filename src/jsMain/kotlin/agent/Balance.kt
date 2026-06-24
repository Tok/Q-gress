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
     * Resonator-damage multiplier (`≥ 1`) for an attacker of [faction] — the comeback rubber-band. The side
     * that's BEHIND hits much harder so it can tear down the leader's fielded, link-mitigated portals
     * (collapsing a field = a big MU swing = a lead change), preventing a runaway leader. Scales with the
     * WORSE of the MU and portal deficits (so a faction shut out on either gets help), STEEPLY (deficit²) so
     * it's gentle when close but powerful when near-defeated: up to `1 + COMEBACK_MAX × dynamism` (≈ 2.8×
     * damage at default). Exactly `1.0` when even or ahead — the underdog is helped, the leader isn't punished.
     */
    fun attackBoost(faction: Faction): Double {
        val muDeficit = shareDeficit(World.calcTotalMu(faction), World.calcTotalMu(faction.enemy()))
        val portalDeficit = shareDeficit(World.countPortals(faction), World.countPortals(faction.enemy()))
        val deficit = maxOf(muDeficit, portalDeficit)
        return 1.0 + Config.comebackMax * Config.comebackAttackBonus() * deficit * deficit
    }

    // How far [mine] is behind [theirs] as a 0..1 share of the total (0 = even/ahead, 1 = fully shut out).
    private fun shareDeficit(mine: Int, theirs: Int): Double {
        val total = (mine + theirs).toDouble()
        return if (total <= 0.0) 0.0 else ((theirs - mine) / total).coerceIn(0.0, 1.0)
    }
}
