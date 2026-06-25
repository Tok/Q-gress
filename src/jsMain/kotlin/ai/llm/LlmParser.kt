package ai.llm

import ai.SliderVector
import kotlin.js.Json

/**
 * Reads an LLM completion back into a [SliderVector] (PLAN Phase 6.3) — tolerantly, because models wrap JSON
 * in prose and stray out of range. Extracts the first {...} object, parses it, and for each known slider id
 * takes its numeric value (clamped to 0..1 by [SliderVector.with]); missing ids keep the uniform default.
 * Returns null when nothing usable is found, so [LlmPolicy] can fall back. Pure (platform `JSON`).
 */
object LlmParser {
    fun parse(text: String): SliderVector? {
        val json = extractObject(text) ?: return null
        val obj = runCatching { JSON.parse<Json>(json) }.getOrNull() ?: return null
        var vector = SliderVector.uniform()
        var matched = 0
        SliderVector.ORDER.forEach { q ->
            val raw = obj[q.id]
            if (raw != null && jsTypeOf(raw) == "number") {
                vector = vector.with(q, raw.unsafeCast<Double>())
                matched++
            }
        }
        return vector.takeIf { matched > 0 } // a parsed object with no known slider keys is not a usable answer
    }

    // The first balanced-looking {...} span (the model may surround the JSON with prose).
    private fun extractObject(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start in 0 until end) text.substring(start, end + 1) else null
    }
}
