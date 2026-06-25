package ai.llm

import ai.SliderVector

/**
 * Builds the LLM prompt from an [ai.Observation] vector (PLAN Phase 6.3): describes the match from the
 * faction's point of view and asks for a JSON object of behaviour-slider weights — the same 17 sliders a net
 * outputs ([SliderVector.ORDER]). Pure + deterministic, so it's unit-tested directly; [LlmParser] reads the
 * reply back into a [SliderVector].
 */
object LlmPrompt {
    // One label per Observation slot (in observe() order).
    private val OBS = listOf(
        "cycle progress", "your Mind-Unit share", "your portals", "enemy portals", "neutral portals",
        "your link share", "your field share", "your roster fill", "enemy roster fill",
        "your avg level", "enemy avg level", "your avg XM", "enemy avg XM",
    )

    fun build(obs: DoubleArray): String {
        val state = OBS.indices.joinToString("\n") { "- ${OBS[it]}: ${fmt(obs.getOrElse(it) { 0.0 })}" }
        val sliders = SliderVector.ORDER.joinToString("\n") { "- ${it.id}: ${it.description}" }
        return buildString {
            append("You are the strategist for one faction in an Ingress-like territory game. ")
            append("Goal: maximize your Mind Units — the area of the control fields you hold.\n\n")
            append("Current state (each value is 0..1):\n").append(state).append("\n\n")
            append("Set a behaviour weight in [0,1] for each action below (higher = do it more often):\n")
            append(sliders).append("\n\n")
            append("Respond with ONLY a JSON object mapping each id to its weight, e.g. ")
            append("{\"link\":0.9,\"deploy\":0.6}. No explanation — JSON only.")
        }
    }

    private fun fmt(value: Double): String = ((value * 100).toInt() / 100.0).toString()
}
