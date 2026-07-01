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
    fun recruitFactor(faction: Faction): Double = BalanceMath.recruitFactor(
        World.countAgents(faction),
        World.countAgents(faction.enemy()),
        Config.recruitFactorMin,
        Config.recruitFactorMax,
    )

    /**
     * Resonator-damage multiplier (`≥ 1`) for an attacker of [faction] — the comeback rubber-band. The side
     * that's BEHIND hits much harder so it can tear down the leader's fielded, link-mitigated portals
     * (collapsing a field = a big MU swing = a lead change), preventing a runaway leader. Scales with the
     * WORSE of the MU and portal deficits (so a faction shut out on either gets help), STEEPLY (deficit²) so
     * it's gentle when close but powerful when near-defeated: up to `1 + COMEBACK_MAX × dynamism` (≈ 2.8×
     * damage at default). Exactly `1.0` when even or ahead — the underdog is helped, the leader isn't punished.
     */
    fun attackBoost(faction: Faction): Double {
        val muDeficit = BalanceMath.shareDeficit(World.calcTotalMu(faction), World.calcTotalMu(faction.enemy()))
        val portalDeficit = BalanceMath.shareDeficit(World.countPortals(faction), World.countPortals(faction.enemy()))
        val deficit = maxOf(muDeficit, portalDeficit)
        return BalanceMath.attackBoost(deficit, Config.comebackMax, Config.comebackAttackBonus())
    }

    /**
     * How far [faction] is AHEAD on Mind Units, as a 0..1 share (0 = even/behind, 1 = total dominance).
     * Drives dominance decay ([Portal.erodeByDominance]): the further ahead a faction is, the faster its own
     * resonators erode — an over-extended empire crumbles, opening the board so the lead can change.
     */
    fun leadShare(faction: Faction): Double =
        BalanceMath.shareDeficit(World.calcTotalMu(faction.enemy()), World.calcTotalMu(faction)) // enemy deficit = our lead
}
