package ai.net

import kotlin.js.Json

/**
 * The baked champion **library** (PLAN Phase 6.2 / docs/NN.md) — one pre-trained genome per [NetArch] the
 * TRAIN tab offers (two hidden layers, each width in 4/8/16/24/32 → 25 combos), so the onboarding per-arch
 * pick and a "random architecture" NN match both have a real champion to install.
 *
 * The genomes live in the JSON resource **`src/jsMain/resources/champions.json`** (bundled via
 * [championsData]) — NOT hardcoded in Kotlin — so the library is a shareable / replaceable / versioned
 * artifact: regenerate it with `scripts/bake-champs.sh`, diff it, commit it; or [installLibrary] a downloaded
 * one at runtime. Each entry is a [GenomeIO] genome object trained headless (`ai.net.ChampionBake`) against
 * the adaptive [ai.HeuristicPolicy] baseline. [jsonFor] falls back to the default champion for any arch not in
 * the library, and [Champion] delegates its default genome here.
 */
object ChampionLibrary {
    /**
     * The `champions.json` schema version this build understands. Bump it (and re-bake) whenever the net I/O
     * contract ([NetArch.INPUTS]/[NetArch.OUTPUTS]) or the library shape changes, so an incompatible loaded
     * library is refused rather than silently mis-decoded (PLAN icebox: genome/action-set versioning).
     */
    const val SCHEMA_VERSION = 1

    // The bundled library (our own baked champions.json — trusted, so a schema mismatch only warns). A loaded
    // or pasted library overrides it at runtime (see [installLibrary]); a mismatch there is refused (throws).
    private val bundled: Library = Library.of(championsData, strict = false)
    private var loaded: Library? = null
    private fun active(): Library = loaded ?: bundled

    /** The default architecture's label (from the active library) — the fallback champion (`13 → 16 → 16 → 17`). */
    val DEFAULT_LABEL: String get() = active().defaultLabel

    /** The baked genome JSON for [arch], or the default champion when that arch isn't in the library. */
    fun jsonFor(arch: NetArch): String = active().jsonFor(arch.label())

    /** The default champion JSON (the `13 → 16 → 16 → 17` net). */
    fun defaultJson(): String = active().defaultJson()

    /** The architectures with a baked champion (for the onboarding per-arch pick / random-arch NN matches). */
    fun bakedArchs(): List<NetArch> = active().archs()

    /** The held-out fitness the [arch]'s baked champion was tagged with (net checkpoints led vs the baseline), or null. */
    fun fitnessFor(arch: NetArch): Double? = active().fitnessFor(arch.label())

    /** The active library serialized back to `champions.json` shape — for a "Download library" export. */
    fun exportJson(): String = active().exportJson()

    /**
     * Replace the active library from a `champions.json` string. Validates the schema version + net I/O dims
     * and throws [IllegalStateException] on a mismatch (so the UI can report it). Blank input resets to bundled.
     */
    fun installLibrary(json: String) {
        loaded = if (json.isBlank()) null else Library.of(JSON.parse<Json>(json), strict = true)
    }

    /** Drop any loaded override, reverting to the bundled library. */
    fun reset() {
        loaded = null
    }

    @Suppress("UnusedParameter") // `o` is referenced inside the js() intrinsic, invisible to detekt
    private fun keysOf(o: dynamic): Array<String> = js("Object.keys(o)")

    /** A champion library over a parsed `champions.json` doc (`{ schemaVersion, inputs, outputs, default, champions }`). */
    private class Library private constructor(private val doc: dynamic) {
        val defaultLabel: String = doc["default"] as String

        private fun entry(label: String): dynamic = doc.champions[label]
        private fun entryOrDefault(label: String): dynamic = entry(label) ?: doc.champions[defaultLabel]

        fun jsonFor(label: String): String = JSON.stringify(entryOrDefault(label))
        fun defaultJson(): String = JSON.stringify(doc.champions[defaultLabel])
        fun archs(): List<NetArch> = keysOf(doc.champions).mapNotNull { label ->
            runCatching { GenomeIO.decode(JSON.stringify(entry(label))).arch }.getOrNull()
        }
        fun fitnessFor(label: String): Double? {
            val e = entry(label) ?: return null
            return GenomeIO.fitnessOf(JSON.stringify(e))
        }
        fun exportJson(): String = JSON.stringify(doc)

        companion object {
            fun of(doc: dynamic, strict: Boolean): Library {
                val v = doc["schemaVersion"] as? Int
                val inputs = doc["inputs"] as? Int
                val outputs = doc["outputs"] as? Int
                if (v != SCHEMA_VERSION || inputs != NetArch.INPUTS || outputs != NetArch.OUTPUTS) {
                    val msg = "champion library schema mismatch: v=$v (want $SCHEMA_VERSION), " +
                        "io=$inputs→$outputs (want ${NetArch.INPUTS}→${NetArch.OUTPUTS})"
                    if (strict) error(msg) else console.warn(msg)
                }
                return Library(doc)
            }
        }
    }
}
