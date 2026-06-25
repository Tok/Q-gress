package ai.llm

import ai.Observation
import ai.SliderVector
import kotlin.test.Test
import kotlin.test.assertTrue

/** The prompt builder mentions the state, every slider id, and demands JSON-only output. */
class LlmPromptTest {

    @Test
    fun describesStateSlidersAndAsksForJson() {
        val prompt = LlmPrompt.build(DoubleArray(Observation.SIZE) { 0.5 })

        assertTrue("Mind Units" in prompt, "states the objective")
        assertTrue("share" in prompt, "describes the state (e.g. Mind-Unit share)")
        SliderVector.ORDER.forEach { assertTrue(it.id in prompt, "lists slider '${it.id}'") }
        assertTrue("JSON" in prompt, "asks for JSON")
        assertTrue("link" in prompt && "0.9" in prompt, "shows the JSON shape via an example")
    }
}
