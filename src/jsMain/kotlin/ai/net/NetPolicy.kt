package ai.net

import World
import agent.Faction
import agent.qvalue.QValue
import ai.FactionPolicy
import ai.Observation
import ai.SliderVector
import config.Config

/**
 * A [FactionPolicy] driven by a [Net] (PLAN Phase 6.2): the net maps the live [Observation] for [faction]
 * to a [SliderVector], re-evaluated **once per scoring checkpoint** — the slow cadence the design calls for
 * (the net re-tunes the sliders periodically; it does not run per action). [weight] just reads the cached
 * vector between checkpoints, so installing this costs no more per-action than a plain slider read.
 */
class NetPolicy(private val net: Net, private val faction: Faction) : FactionPolicy {

    private var vector: SliderVector = SliderVector.uniform()
    private var evaluatedCheckpoint = -1

    override fun weight(value: QValue): Double {
        val checkpoint = World.tick / Config.ticksPerCheckpoint
        if (checkpoint != evaluatedCheckpoint) {
            evaluatedCheckpoint = checkpoint
            vector = SliderVector.decode(net.forward(Observation.observe(faction)))
        }
        return vector[value]
    }
}
