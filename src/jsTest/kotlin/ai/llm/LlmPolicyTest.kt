package ai.llm

import World
import agent.Faction
import agent.qvalue.QActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The LLM driver's cadence: it adopts the parsed LLM vector and falls back to the heuristic on a bad reply.
 * Uses an Unconfined scope + a synchronous [MockLlmClient] so the fire-and-forget request resolves inline.
 */
class LlmPolicyTest {

    @BeforeTest
    fun reset() {
        World.tick = 0
    }

    @Test
    fun adoptsTheLlmVectorOnceItResponds() {
        val policy = LlmPolicy(
            Faction.ENL,
            MockLlmClient("""{"link":0.9,"attack":0.05}"""),
            CoroutineScope(Dispatchers.Unconfined),
        )
        // weight() fires the request; under Unconfined + a synchronous client it resolves before returning.
        assertEquals(0.9, policy.weight(QActions.LINK))
        assertEquals(0.05, policy.weight(QActions.ATTACK))
    }

    @Test
    fun fallsBackToHeuristicWhenTheReplyIsUnparseable() {
        val policy = LlmPolicy(Faction.ENL, MockLlmClient("no json here"), CoroutineScope(Dispatchers.Unconfined))
        val weight = policy.weight(QActions.LINK)
        assertTrue(weight in 0.0..1.0, "a valid weight from the heuristic fallback")
        assertTrue(policy.currentVector() != null, "fallback keeps a usable vector")
    }
}
