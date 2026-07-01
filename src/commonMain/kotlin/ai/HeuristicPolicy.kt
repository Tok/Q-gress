package ai

import World
import agent.Faction
import agent.qvalue.QValue
import config.Config

/**
 * The first **live AI driver** (PLAN Phase 6) — a hand-written adaptive policy, the stepping stone before a
 * trained [ai.net.NetPolicy] is loadable. It maps the live [Observation] for [faction] to a [SliderVector],
 * re-evaluated once per scoring checkpoint (the same slow cadence a net uses), so the tuning sliders visibly
 * re-tune as the match swings: behind on MU → press the attack and hunt enemy portals; ahead → consolidate
 * into links/fields and defend; low on XM → hack/glyph to refuel. The mapping itself is pure
 * ([HeuristicTune.tune], in commonMain), so it's unit-tested directly and doubles as the net's baseline foe.
 */
class HeuristicPolicy(private val faction: Faction) : FactionPolicy {

    private var vector: SliderVector = SliderVector.uniform()
    private var evaluatedCheckpoint = -1

    override fun weight(value: QValue): Double = ensureEvaluated()[value]

    override fun currentVector(): SliderVector = ensureEvaluated()

    private fun ensureEvaluated(): SliderVector {
        val checkpoint = World.tick / Config.ticksPerCheckpoint
        if (checkpoint != evaluatedCheckpoint) {
            evaluatedCheckpoint = checkpoint
            vector = HeuristicTune.tune(Observation.observe(faction))
        }
        return vector
    }
}
