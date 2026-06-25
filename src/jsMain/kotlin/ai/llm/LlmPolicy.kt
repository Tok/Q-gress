package ai.llm

import World
import agent.Faction
import agent.qvalue.QValue
import ai.FactionPolicy
import ai.HeuristicPolicy
import ai.Observation
import ai.SliderVector
import config.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * An **LLM faction driver** (PLAN Phase 6.3): once per scoring checkpoint it asks the [LlmClient] for a
 * slider vector (state → [LlmPrompt] → JSON → [LlmParser]), asynchronously. Inference is slow + async, so
 * [weight] never blocks — it returns the **last** LLM vector, falling back to an adaptive [HeuristicPolicy]
 * until the first reply lands (and whenever a reply is unparseable). At most one request per checkpoint.
 * Installs like any other driver via [ai.FactionPolicies]; the reasoning surface reads [lastPrompt]/[lastReply].
 */
class LlmPolicy(private val faction: Faction, val client: LlmClient, private val scope: CoroutineScope = MainScope()) : FactionPolicy {
    private val fallback = HeuristicPolicy(faction)
    private var vector: SliderVector? = null
    private var requestedCheckpoint = -1

    var lastPrompt: String = ""
        private set
    var lastReply: String = ""
        private set

    override fun weight(value: QValue): Double {
        request()
        return vector?.get(value) ?: fallback.weight(value)
    }

    override fun currentVector(): SliderVector? {
        request()
        return vector ?: fallback.currentVector()
    }

    // Fire one inference per checkpoint; the result updates [vector] when it resolves (never blocks weight()).
    private fun request() {
        val checkpoint = World.tick / Config.ticksPerCheckpoint
        if (checkpoint == requestedCheckpoint) return
        requestedCheckpoint = checkpoint
        val prompt = LlmPrompt.build(Observation.observe(faction))
        lastPrompt = prompt
        scope.launch {
            val reply = client.complete(prompt)
            lastReply = reply
            LlmParser.parse(reply)?.let { vector = it }
        }
    }
}
