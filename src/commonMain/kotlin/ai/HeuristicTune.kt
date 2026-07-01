package ai

import agent.qvalue.QActions
import agent.qvalue.QDestinations

/**
 * The adaptive driver's pure observation→sliders mapping (PLAN Phase 6), extracted from [HeuristicPolicy]
 * into the shared functional core. [HeuristicPolicy] keeps the World/checkpoint plumbing and delegates the
 * actual tuning here, so this is JVM-unit-tested + Kover-covered and reusable by any heuristic driver.
 */
object HeuristicTune {
    /**
     * Map an [Observation] vector ([obs]) to a [SliderVector]. Reads the MU dominance (slot 1) and our
     * average XM (slot 11); every `with` clamps to 0..1, so over-shoot (e.g. a full-deficit attack at 1.4)
     * just saturates. Untouched slots keep the uniform default so the vector reads like a tuned panel.
     */
    fun tune(obs: DoubleArray): SliderVector {
        val muShare = obs.getOrElse(Observation.SLOT_MU_SHARE) { 0.5 }
        val avgXm = obs.getOrElse(Observation.SLOT_AVG_XM) { 0.5 }
        val ahead = ((muShare - 0.5) * 2.0).coerceIn(0.0, 1.0) // 0 even … 1 fully dominant
        val behind = ((0.5 - muShare) * 2.0).coerceIn(0.0, 1.0) // 0 even … 1 fully shut out
        val lowXm = (1.0 - avgXm).coerceIn(0.0, 1.0)
        return SliderVector.uniform()
            // Always build toward fields (the MU objective); lean harder into it when ahead (consolidate).
            .with(QActions.LINK, 0.6 + ahead * 0.3)
            .with(QActions.DEPLOY, 0.5 + ahead * 0.2)
            .with(QActions.CAPTURE, 0.5)
            .with(QActions.RECHARGE, 0.2 + ahead * 0.3)
            // Refuel when XM runs low so assaults can be sustained.
            .with(QActions.HACK, 0.35 + lowXm * 0.4)
            .with(QActions.GLYPH, 0.3 + lowXm * 0.3)
            // Press the attack when behind — tear down the leader's fielded portals.
            .with(QActions.ATTACK, 0.2 + behind * 1.2)
            .with(QActions.VIRUS, 0.1 + behind * 0.5)
            .with(QActions.MOVE_ELSEWHERE, 0.05)
            // Destinations: chase open ground when ahead, hunt enemy portals when behind.
            .with(QDestinations.MOVE_TO_UNCAPTURED, 0.5 + ahead * 0.3)
            .with(QDestinations.MOVE_TO_MOST_FRIENDLY, 0.3 + ahead * 0.3)
            .with(QDestinations.MOVE_TO_WEAK_ENEMY, 0.15 + behind * 0.6)
            .with(QDestinations.MOVE_TO_STRONG_ENEMY, 0.05 + behind * 0.4)
            .with(QDestinations.MOVE_TO_NEAR_ENEMY, 0.1 + behind * 0.3)
            .with(QDestinations.MOVE_TO_NEAR, 0.2)
            .with(QDestinations.MOVE_TO_RANDOM, 0.1)
    }
}
