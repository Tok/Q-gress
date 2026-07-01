package util

import World
import agent.Faction
import config.Config
import config.Sim
import kotlinx.browser.document
import org.w3c.dom.url.URL
import system.ui.DriverControls
import system.ui.encodeURIComponent
import system.ui.panel.TuningPanel
import util.data.GeoCoords

/**
 * Read/write of the game's start state in the page URL — the shareable link. Split out of [Bootstrap]
 * (which only keeps the DOM-y navigation/clipboard bits). One link round-trips faction, location,
 * play size, portal/NPC counts, round field, quickstart, the RNG **seed** (so the world replays
 * deterministically), and the per-faction **tuning** sliders.
 */
object GameUrl {
    private fun url() = URL(document.location?.href ?: "")
    private fun param(key: String) = url().searchParams.get(key)

    fun name() = param("name")
    fun seed(): Int? = param("seed")?.toIntOrNull()
    fun tune(): String? = param("tune")
    fun portals(): Int? = param("portals")?.toIntOrNull()
    fun npcMultiplier(): Double? = param("npcmult")?.toDoubleOrNull()
    fun startStage(): config.StartStage? = config.StartStage.fromString(param("start"))
    fun round(): Boolean? = param("round")?.toBoolean()
    fun isAutoStart() = param("local")?.toBoolean() ?: false
    fun isReadOnly() = param("readonly")?.toBoolean() ?: false
    fun faction() = Faction.fromString(param("faction"))

    /** The chosen AI/Human driver for [faction] (`?enl=…&res=…`: manual/heuristic/net/llm), or null. */
    fun driver(faction: Faction): String? = param(if (faction == Faction.ENL) "enl" else "res")

    /** The chosen neural-net architecture for [faction] (`?enlarch=…&resarch=…`: a `"16-16"` key or `random`), or null. */
    fun netArch(faction: Faction): String? = param(if (faction == Faction.ENL) "enlarch" else "resarch")

    /** Whether the experimental LLM driver was unlocked at onboarding (`?exp=true`). */
    fun experimentalLlm(): Boolean = param("exp")?.toBoolean() ?: false

    fun size(): Pair<Int, Int>? {
        val w = param("w")?.toIntOrNull()
        val h = param("h")?.toIntOrNull()
        return if (w != null && h != null) w to h else null
    }

    fun lngLat(): GeoCoords? = GeoCoords.fromStrings(param("lng"), param("lat"))

    /** Shareable link reproducing the exact current world (location + size + counts + seed + tuning). */
    fun forShare(lng: Double, lat: Double, name: String): String = build(lng.toString(), lat.toString(), name, Rng.currentSeed())

    /** Navigation link (reset / preset): same location + size, but a FRESH world (no seed). */
    fun forNavigation(lng: Double, lat: Double, name: String): String = build(lng.toString(), lat.toString(), name, null)

    // Build off the current origin + path so it works on any host (local dev, GitHub Pages, …).
    private fun build(lng: String, lat: String, name: String, seed: Int?): String {
        val location = document.location
        val base = (location?.origin ?: "") + (location?.pathname ?: "/")
        val fact = World.userFaction?.abbr ?: ""
        val seedPart = if (seed != null) "&seed=$seed" else ""
        val tune = TuningPanel.exportTuning() // both factions' sliders (empty until the panel is built)
        val tunePart = if (tune.isNotEmpty()) "&tune=${encodeURIComponent(tune)}" else ""
        // Per-faction driver picks (AI vs AI by default) + the experimental-LLM unlock — carried across the reload.
        val drivers = "&enl=${DriverControls.chosen(
            Faction.ENL,
        )}&res=${DriverControls.chosen(Faction.RES)}&exp=${DriverControls.experimentalLlm}" +
            "&enlarch=${DriverControls.chosenArch(Faction.ENL)}&resarch=${DriverControls.chosenArch(Faction.RES)}"
        // NPC *count* is auto-derived at world-gen; only the player's density multiplier is carried.
        return "$base?faction=$fact&lng=$lng&lat=$lat&name=${encodeURIComponent(name)}" +
            "&w=${Sim.width}&h=${Sim.height}&portals=${Config.startPortals}&npcmult=${Config.npcMultiplier}" +
            "&round=${Sim.roundField}&start=${Config.startStage.name.lowercase()}$drivers$seedPart$tunePart"
    }
}
