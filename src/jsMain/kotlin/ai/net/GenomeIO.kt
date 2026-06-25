package ai.net

import kotlin.js.Json
import kotlin.js.json

/**
 * JSON (de)serialization for a [Net] genome + its [NetArch] (PLAN Phase 6.2) — so a trained net of any shape
 * can be saved, committed as a baked champion, pasted in, or persisted to `localStorage` and loaded back as a
 * driver. Shape: `{"v":1,"inputs":13,"outputs":17,"arch":{"hiddens":[16,16],"bias":true,"activation":"TANH"},
 * "fitness":<num?>,"weights":[…]}`. The `inputs`/`outputs` are stamped so [decode] rejects a genome trained
 * against a different [ai.Observation]/[ai.SliderVector] layout. **Back-compatible**: an old single-layer
 * genome with a bare `"hidden":16` (no `arch`) still decodes. Pure (platform `JSON`).
 */
object GenomeIO {
    const val VERSION = 1

    /** Serialize [genome] under [arch] (with the current I/O dims, optional [fitness]). */
    fun encode(genome: DoubleArray, arch: NetArch, fitness: Double? = null): String {
        val archJson = json(
            "hiddens" to arch.hiddens.toTypedArray(),
            "bias" to arch.bias,
            "activation" to arch.activation.name,
        )
        val obj = json(
            "v" to VERSION,
            "inputs" to Net.INPUTS,
            "outputs" to Net.OUTPUTS,
            "arch" to archJson,
            "weights" to genome.toTypedArray(),
        )
        if (fitness != null) obj["fitness"] = fitness
        return JSON.stringify(obj)
    }

    /** Convenience: serialize a single-hidden-layer genome of [hidden] units. */
    fun encode(genome: DoubleArray, hidden: Int, fitness: Double? = null): String = encode(genome, NetArch(listOf(hidden)), fitness)

    /** Serialize a built [net] (+ optional [fitness] tag). */
    fun encode(net: Net, fitness: Double? = null): String = encode(net.genome(), net.arch, fitness)

    /** Parse a genome JSON into a [Net], validating the I/O dims + genome length for this build. */
    fun decode(text: String): Net {
        val obj = JSON.parse<Json>(text)
        require(intField(obj, "inputs") == Net.INPUTS) { "genome built for different inputs than this build" }
        require(intField(obj, "outputs") == Net.OUTPUTS) { "genome built for different outputs than this build" }
        val arch = readArch(obj)
        val raw = obj["weights"] ?: error("genome JSON is missing 'weights'")
        val length = raw.asDynamic().length.unsafeCast<Int>()
        val weights = DoubleArray(length) { raw.asDynamic()[it].unsafeCast<Double>() }
        return Net.fromGenome(weights, arch) // Net.init validates weights.size == arch.genomeSize()
    }

    /** The fitness tag a genome was saved with, or null if absent/malformed. */
    fun fitnessOf(text: String): Double? = runCatching { JSON.parse<Json>(text)["fitness"].unsafeCast<Double?>() }.getOrNull()

    private fun readArch(obj: Json): NetArch {
        val arch = obj["arch"]
        if (arch != null) {
            val raw = arch.asDynamic().hiddens
            val count = raw.length.unsafeCast<Int>()
            val hiddens = (0 until count).map { raw[it].unsafeCast<Double>().toInt() }
            val bias = (arch.asDynamic().bias as? Boolean) ?: true
            val activation = Activation.from(arch.asDynamic().activation as? String)
            return NetArch(hiddens, bias, activation)
        }
        return NetArch(listOf(intField(obj, "hidden"))) // back-compat: a pre-multilayer single "hidden" int
    }

    private fun intField(obj: Json, name: String): Int {
        val value = obj[name] ?: error("genome JSON is missing '$name'")
        require(jsTypeOf(value) == "number") { "genome JSON field '$name' must be a number" }
        return value.unsafeCast<Double>().toInt()
    }
}
