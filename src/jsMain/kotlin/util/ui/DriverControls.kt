package util.ui

import World
import agent.Faction
import ai.DomSliderPolicy
import ai.FactionPolicies
import ai.HeuristicPolicy
import ai.llm.LlmPolicy
import ai.llm.WebLlmClient
import ai.net.NetPolicy
import ai.net.NetStore
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import util.GameUrl

/**
 * The per-faction **driver picker** (PLAN Phase 6) — lives in the footer header so it's reachable from any
 * tab. Each faction's behaviour is driven by **Manual** (the tuning sliders), the adaptive **Heuristic**, or
 * the trained **Neural net** (LLM pending 6.3). Neural net is the **default**, so the sim plays itself
 * (AI-vs-AI) out of the box; switch a faction to Manual to drive it with the sliders. (Headless/tests keep
 * the `DomSliderPolicy` default via `FactionPolicies`; this only installs the *live* drivers.)
 */
object DriverControls {
    const val DEFAULT = "net" // default brain: the trained neural net (AI vs AI out of the box)
    private val llmClient by lazy { WebLlmClient() } // shared across factions → one model load
    private val pending = mutableMapOf<Faction, String>() // the onboarding driver pick, before the start-reload
    private val selects = mutableMapOf<Faction, HTMLSelectElement>() // live pickers, so others can reflect a change

    /** Record [faction]'s chosen driver (onboarding) so the start-URL carries it across the reload. */
    fun select(faction: Faction, value: String) {
        pending[faction] = value
    }

    /** The driver for [faction]: the onboarding pick, else the start-URL (`?enl=…&res=…`), else the default. */
    fun chosen(faction: Faction): String = pending[faction] ?: GameUrl.driver(faction) ?: defaultFor(faction)

    // The user's faction defaults to the trained net; the OPPONENT defaults to the (experimental) LLM, so a
    // fresh game shows two different brains out of the box. Falls back to net for both until a side is chosen.
    private fun defaultFor(faction: Faction): String = if (World.userFaction != null && faction != World.userFaction) "llm" else DEFAULT

    // Manual only works for the user's own faction — DomSliderPolicy reads the visible tuning sliders, which
    // only drive the chosen faction. Offer it just for that side (mirrors the onboarding driver grid).
    private fun manualAllowed(faction: Faction): Boolean = World.userFaction == null || faction == World.userFaction

    /** Both faction pickers wrapped as a top-toolbar group (the "AI vs AI" control, reachable from anywhere). */
    fun toolbarGroup(): HTMLElement {
        val group = el("div", "toolbarGroup driverControls")
        group.appendChild(el("span", "driverControlsLabel").also { it.textContent = "AI" })
        Faction.all().forEach { group.appendChild(picker(it)) }
        return group
    }

    /** A faction-labelled driver `<select>` that installs (and defaults to) the Neural-net driver. */
    fun picker(faction: Faction): HTMLElement {
        val wrap = el("span", "footerDriver")
        wrap.appendChild(
            el("span", "footerDriverLabel").also {
                it.textContent = faction.abbr
                it.asDynamic().style.color = faction.color
            },
        )
        val sel = document.createElement("select") as HTMLSelectElement
        sel.className = "aiDriverSelect"
        sel.appendChild(option("manual", "Manual", disabled = !manualAllowed(faction)))
        sel.appendChild(option("heuristic", "Heuristic", disabled = false))
        sel.appendChild(option("net", "Neural net", disabled = false))
        sel.appendChild(option("llm", "LLM (experimental)", disabled = false))
        val choice = chosen(faction) // onboarding pick / start-URL / default
        sel.value = choice
        selects[faction] = sel
        apply(faction, choice) // install it up front so the chosen brain plays from the first tick
        sel.onchange = {
            apply(faction, sel.value)
            null
        }
        wrap.appendChild(sel)
        return wrap
    }

    /** Reflect an externally-installed driver in the picker (e.g. the trainer installing a champion) — the
     *  policy is set by the caller; this just syncs the displayed `<select>` so the UI doesn't read stale. */
    fun reflect(faction: Faction, value: String) {
        selects[faction]?.value = value
    }

    private fun apply(faction: Faction, value: String) {
        when (value) {
            "heuristic" -> FactionPolicies.set(faction, HeuristicPolicy(faction))
            "net" -> FactionPolicies.set(faction, NetPolicy(NetStore.loadNet(), faction))
            "llm" -> FactionPolicies.set(faction, LlmPolicy(faction, llmClient))
            else -> FactionPolicies.set(faction, DomSliderPolicy(faction))
        }
    }

    private fun option(value: String, label: String, disabled: Boolean): HTMLOptionElement {
        val o = document.createElement("option") as HTMLOptionElement
        o.value = value
        o.textContent = label
        o.disabled = disabled
        return o
    }

    private fun el(tag: String, cls: String): HTMLElement {
        val e = document.createElement(tag) as HTMLElement
        e.className = cls
        return e
    }
}
