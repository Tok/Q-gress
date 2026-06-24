package ai.net

import kotlin.js.Json
import kotlin.js.json

/**
 * JSON (de)serialization for a [Net] genome (PLAN Phase 6.2) — so a trained net can be saved, committed as a
 * baked champion, pasted in, or persisted to `localStorage` and loaded back into the live game as a driver.
 *
 * Shape: `{"v":1,"hidden":16,"inputs":13,"outputs":17,"fitness":<num?>,"weights":[…]}`. The `inputs`/`outputs`
 * are stamped so [decode] can **reject a genome trained against a different [ai.Observation]/[ai.SliderVector]
 * layout** (a silently-misread genome would just play badly) rather than fail mysteriously later. Pure: uses
 * the platform `JSON` global (present in Node + the browser), no game state.
 */
object GenomeIO {
    const val VERSION = 1

    /** Serialize [genome] (a [hidden]-unit net's flat weights) with the current I/O dims, optional [fitness]. */
    fun encode(genome: DoubleArray, hidden: Int, fitness: Double? = null): String {
        val obj = json(
            "v" to VERSION,
            "hidden" to hidden,
            "inputs" to Net.INPUTS,
            "outputs" to Net.OUTPUTS,
            "weights" to genome.toTypedArray(),
        )
        if (fitness != null) obj["fitness"] = fitness
        return JSON.stringify(obj)
    }

    /** Serialize a built [net] (+ optional [fitness] tag). */
    fun encode(net: Net, fitness: Double? = null): String = encode(net.genome(), net.hidden, fitness)

    /** Parse a genome JSON into a [Net], validating the I/O dims + genome length for this build. */
    fun decode(text: String): Net {
        val obj = JSON.parse<Json>(text)
        val inputs = intField(obj, "inputs")
        val outputs = intField(obj, "outputs")
        require(inputs == Net.INPUTS) { "genome built for $inputs inputs, this build has ${Net.INPUTS}" }
        require(outputs == Net.OUTPUTS) { "genome built for $outputs outputs, this build has ${Net.OUTPUTS}" }
        val hidden = intField(obj, "hidden")
        val raw = obj["weights"] ?: error("genome JSON is missing 'weights'")
        val length = raw.asDynamic().length.unsafeCast<Int>()
        val weights = DoubleArray(length) { raw.asDynamic()[it].unsafeCast<Double>() }
        return Net.fromGenome(weights, hidden) // Net.init validates weights.size == genomeSize(hidden)
    }

    /** The fitness tag a genome was saved with, or null if absent/malformed. */
    fun fitnessOf(text: String): Double? = runCatching { JSON.parse<Json>(text)["fitness"].unsafeCast<Double?>() }.getOrNull()

    private fun intField(obj: Json, name: String): Int {
        val value = obj[name] ?: error("genome JSON is missing '$name'")
        require(jsTypeOf(value) == "number") { "genome JSON field '$name' must be a number" }
        return value.unsafeCast<Double>().toInt()
    }
}
