package system.ui

import World
import agent.Faction
import ai.DomSliderPolicy
import ai.FactionPolicies
import ai.HeuristicPolicy
import ai.llm.LlmPolicy
import ai.llm.WebLlmClient
import ai.net.ChampionLibrary
import ai.net.NetArch
import ai.net.NetPolicy
import ai.net.NetStore
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import util.GameUrl
import util.Rng
import util.RngStream

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
    private val netArch = mutableMapOf<Faction, String>() // each faction's chosen net-arch key ("16-16") or RANDOM_ARCH
    private val resolvedRandom = mutableMapOf<Faction, NetArch>() // a RANDOM pick resolved to a concrete arch (cached)

    const val RANDOM_ARCH = "random" // the "let the seed pick a baked arch" option (the NN-driver default)
    private const val ARCH_SALT = 0x51A5 // decorrelate the arch pick from the world seed's other draws

    /** Whether the experimental LLM driver is unlocked (the onboarding opt-in; carried in the start URL). */
    var experimentalLlm = false
        private set

    /** Onboarding sets this from the "Show experimental LLM driver" checkbox; it rides the URL via [GameUrl]. */
    fun setExperimentalLlm(on: Boolean) {
        experimentalLlm = on
    }

    // The LLM option is offered only when unlocked — the onboarding flag (pre-reload) or `?exp=true` (in-game).
    private fun llmEnabled(): Boolean = experimentalLlm || GameUrl.experimentalLlm()

    /** Record [faction]'s chosen driver (onboarding) so the start-URL carries it across the reload. */
    fun select(faction: Faction, value: String) {
        pending[faction] = value
    }

    /** The driver for [faction]: the onboarding pick, else the start-URL (`?enl=…&res=…`), else the default. */
    fun chosen(faction: Faction): String = pending[faction] ?: GameUrl.driver(faction) ?: DEFAULT

    /** Record [faction]'s chosen net architecture (a `"16-16"` key or [RANDOM_ARCH]); re-resolves any RANDOM. */
    fun selectArch(faction: Faction, key: String) {
        netArch[faction] = key
        resolvedRandom.remove(faction) // a fresh pick re-resolves next apply
    }

    /** The chosen net-arch key for [faction]: onboarding pick, else the start-URL (`?enlarch/?resarch`), else RANDOM. */
    fun chosenArch(faction: Faction): String = netArch[faction] ?: GameUrl.netArch(faction) ?: RANDOM_ARCH

    private fun archKey(arch: NetArch): String = arch.hiddens.joinToString("-")

    // A signed 2-decimal fitness for the arch dropdown, e.g. +2.00 / -0.50.
    private fun signed(v: Double): String {
        val fixed = v.asDynamic().toFixed(2) as String
        return if (v >= 0) "+$fixed" else fixed
    }

    private fun parseArchKey(key: String): NetArch? {
        val parts = key.split("-")
        val hiddens = parts.mapNotNull { it.toIntOrNull() }
        return if (hiddens.size == parts.size && hiddens.all { it > 0 }) NetArch(hiddens) else null
    }

    // The concrete arch to install for [faction]. RANDOM resolves off the WORLD SEED (a throwaway stream, so the
    // live RNG sequence is untouched) → a shared `?seed=…` link reproduces the same pick, while a fresh no-seed
    // game randomizes. Cached per faction so it stays put across re-applies within a game.
    private fun resolvedArch(faction: Faction): NetArch {
        val choice = chosenArch(faction)
        if (choice != RANDOM_ARCH) return parseArchKey(choice) ?: NetArch.DEFAULT
        return resolvedRandom.getOrPut(faction) {
            val archs = ChampionLibrary.bakedArchs()
            if (archs.isEmpty()) {
                NetArch.DEFAULT
            } else {
                val rng = RngStream().apply { seed(Rng.currentSeed() xor ARCH_SALT xor faction.ordinal) }
                archs[rng.randomInt(archs.size - 1)]
            }
        }
    }

    /** Install each faction's chosen driver up front so the picked brains play from the first tick — the
     *  visible pickers now live in the BRAINS tab ([toolbarGroup]) and build lazily, so this decouples the
     *  policy install from the DOM. Safe to re-run; [picker] re-applies on (re)build. */
    fun installDefaults() = Faction.all().forEach { apply(it, chosen(it)) }

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
        sel.appendChild(option("llm", "LLM (experimental)", disabled = !llmEnabled())) // unlocked only via the opt-in
        val choice = chosen(faction) // onboarding pick / start-URL / default
        sel.value = choice
        selects[faction] = sel
        val archSel = archPicker(faction)
        val modelSel = modelPicker(faction)
        apply(faction, choice) // install it up front so the chosen brain plays from the first tick
        sel.onchange = {
            apply(faction, sel.value)
            setVisible(archSel, sel.value == "net") // the arch picker only matters for the neural-net driver
            setVisible(modelSel, sel.value == "llm") // the model picker only matters for the LLM driver
            null
        }
        setVisible(archSel, choice == "net")
        setVisible(modelSel, choice == "llm")
        wrap.appendChild(sel)
        wrap.appendChild(archSel)
        wrap.appendChild(modelSel)
        return wrap
    }

    /**
     * Per-faction **net-architecture** picker (shown only while the Neural-net driver is selected) — pick the
     * baked champion arch, or **Random** (the seed-picked default, so AI-vs-AI stays varied). Changing it
     * reinstalls the net driver with the chosen arch's champion. Shared by the toolbar picker + onboarding.
     */
    fun archPicker(faction: Faction): HTMLSelectElement {
        val sel = document.createElement("select") as HTMLSelectElement
        sel.className = "aiDriverSelect aiArchSelect"
        sel.appendChild(option(RANDOM_ARCH, "Random arch", disabled = false))
        ChampionLibrary.bakedArchs().forEach { arch ->
            // e.g. "16×8  +2.00" — the held-out fitness (net checkpoints led vs the baseline) it was baked with.
            val fit = ChampionLibrary.fitnessFor(arch)?.let { "  ${signed(it)}" } ?: ""
            sel.appendChild(option(archKey(arch), arch.hiddens.joinToString("×") + fit, disabled = false))
        }
        sel.value = chosenArch(faction)
        sel.onchange = {
            selectArch(faction, sel.value)
            if (selects[faction]?.value == "net") apply(faction, "net") // swap to the new arch's champion live
            null
        }
        return sel
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
            "net" -> FactionPolicies.set(faction, NetPolicy(NetStore.loadNet(resolvedArch(faction)), faction))
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
}
