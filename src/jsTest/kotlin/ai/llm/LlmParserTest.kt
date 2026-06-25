package ai.llm

import agent.qvalue.QActions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The tolerant LLM-reply → slider parser: clean JSON, JSON-in-prose, clamping, and rejecting junk. */
class LlmParserTest {

    @Test
    fun parsesACleanJsonObject() {
        val v = LlmParser.parse("""{"link":0.9,"attack":0.1}""") ?: error("expected a vector")
        assertEquals(0.9, v[QActions.LINK])
        assertEquals(0.1, v[QActions.ATTACK])
        assertEquals(0.1, v[QActions.DEPLOY], "an unspecified slider keeps the uniform default")
    }

    @Test
    fun toleratesProseAroundTheJson() {
        val v = LlmParser.parse("""Sure! Here you go: {"deploy": 0.7} — good luck.""") ?: error("expected a vector")
        assertEquals(0.7, v[QActions.DEPLOY])
    }

    @Test
    fun clampsOutOfRangeWeights() {
        val v = LlmParser.parse("""{"link":2.0,"attack":-1.0}""") ?: error("expected a vector")
        assertEquals(1.0, v[QActions.LINK])
        assertEquals(0.0, v[QActions.ATTACK])
    }

    @Test
    fun rejectsTextWithNoJson() {
        assertNull(LlmParser.parse("I think you should attack a lot."))
    }

    @Test
    fun rejectsJsonWithNoKnownSliders() {
        assertNull(LlmParser.parse("""{"strategy":"aggressive","foo":1}"""), "no usable slider keys → fall back")
    }

    @Test
    fun rejectsMalformedJson() {
        val parsed = LlmParser.parse("""{"link": }""")
        assertTrue(parsed == null, "unparseable JSON returns null")
    }
}
