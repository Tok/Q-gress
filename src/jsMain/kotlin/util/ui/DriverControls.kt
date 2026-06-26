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
    private val pending = mutableMapOf<Faction, String>() // the onboarding driver pick, before the start-reload
    private val selects = mutableMapOf<Faction, HTMLSelectElement>() // live pickers, so others can reflect a change
    private val llmModel = mutableMapOf<Faction, String>() // each faction's chosen LLM model id
    private val llmClients = mutableMapOf<Faction, WebLlmClient>() // one client per faction (its own model load)

    /** Record [faction]'s chosen driver (onboarding) so the start-URL carries it across the reload. */
    fun select(faction: Faction, value: String) {
        pending[faction] = value
    }

    /** The driver for [faction]: the onboarding pick, else the start-URL (`?enl=…&res=…`), else the default. */
    fun chosen(faction: Faction): String = pending[faction] ?: GameUrl.driver(faction) ?: DEFAULT

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
        val modelSel = modelPicker(faction)
        apply(faction, choice) // install it up front so the chosen brain plays from the first tick
        sel.onchange = {
            apply(faction, sel.value)
            setVisible(modelSel, sel.value == "llm") // the model picker only matters for the LLM driver
            null
        }
        setVisible(modelSel, choice == "llm")
        wrap.appendChild(sel)
        wrap.appendChild(modelSel)
        return wrap
    }

    // Per-faction LLM model picker (shown only while the LLM driver is selected) — pick a different model per
    // side for varied LLM-vs-LLM matches. Changing it reinstalls the LLM driver with a fresh client.
    private fun modelPicker(faction: Faction): HTMLSelectElement {
        val sel = document.createElement("select") as HTMLSelectElement
        sel.className = "aiDriverSelect aiModelSelect"
        WebLlmClient.MODELS.forEach { (label, id) ->
            val o = document.createElement("option") as HTMLOptionElement
            o.value = id
            o.textContent = label
            sel.appendChild(o)
        }
        sel.value = llmModel[faction] ?: WebLlmClient.DEFAULT_MODEL
        sel.onchange = {
            llmModel[faction] = sel.value
            if (selects[faction]?.value == "llm") apply(faction, "llm") // swap to the new model live
            null
        }
        return sel
    }

    private fun setVisible(el: HTMLElement, visible: Boolean) {
        el.asDynamic().style.display = if (visible) "" else "none"
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
            "llm" -> FactionPolicies.set(faction, LlmPolicy(faction, llmClientFor(faction)))
            else -> FactionPolicies.set(faction, DomSliderPolicy(faction))
        }
    }

    // One WebLlmClient per faction, recreated when its chosen model changes (each loads its own weights).
    private fun llmClientFor(faction: Faction): WebLlmClient {
        val model = llmModel.getOrPut(faction) { WebLlmClient.DEFAULT_MODEL }
        val existing = llmClients[faction]
        if (existing != null && existing.requestedModel == model) return existing
        return WebLlmClient(model).also { llmClients[faction] = it }
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
