package system.ui

import World
import agent.Faction
import ai.DomSliderPolicy
import ai.FactionPolicies
import ai.HeuristicPolicy
import ai.llm.LlmPolicy
import ai.llm.WebLlmClient
import ai.net.ChampionLibrary
import ai.net.GenomeIO
import ai.net.NetArch
import ai.net.NetPolicy
import ai.net.NetStore
import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.files.File
import org.w3c.files.FileReader
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

    // Each faction's chosen hidden-layer widths — a width string ("16") or [RANDOM_LAYER]; two dropdowns, not one
    // flat "13-X-Y-17" list. The combined value rides the URL as "h1-h2" (e.g. "16-8", "r-8"); see [chosenArch].
    private val netH1 = mutableMapOf<Faction, String>()
    private val netH2 = mutableMapOf<Faction, String>()
    private val resolvedRandom = mutableMapOf<Faction, NetArch>() // a RANDOM pick resolved to a concrete arch (cached)
    private val archSelects = mutableMapOf<Faction, Pair<HTMLSelectElement, HTMLSelectElement>>() // live H1/H2 <select>s
    private val archInfo = mutableMapOf<Faction, HTMLElement>() // the resolved-arch fitness/provenance label

    const val RANDOM_LAYER = "r" // a hidden-layer dropdown set to "Rnd" — the seed picks that layer's width
    private val ARCH_WIDTHS = listOf(4, 8, 16, 24, 32) // the per-layer width choices (matches the baked sweep)
    private const val DEFAULT_WIDTH = 16
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

    /** Record [faction]'s chosen width for hidden [layer] (1 or 2) — a width string or [RANDOM_LAYER]; re-resolves. */
    fun selectLayer(faction: Faction, layer: Int, value: String) {
        (if (layer == 1) netH1 else netH2)[faction] = value
        resolvedRandom.remove(faction) // a fresh pick re-resolves next apply
    }

    // [layer]'s chosen width: the picker value, else the start-URL ("h1-h2" / legacy "random"), else RANDOM.
    private fun chosenLayer(faction: Faction, layer: Int): String {
        val stored = (if (layer == 1) netH1 else netH2)[faction]
        return stored ?: urlLayer(faction, layer) ?: RANDOM_LAYER
    }

    private fun urlLayer(faction: Faction, layer: Int): String? {
        val arch = GameUrl.netArch(faction) ?: return null
        if (arch == "random" || arch == RANDOM_LAYER) return RANDOM_LAYER // legacy whole-arch random
        return arch.split("-").getOrNull(layer - 1)
    }

    /** The combined arch key for [faction] (`"h1-h2"`, e.g. `"16-8"` / `"r-8"`) — rides the shared URL. */
    fun chosenArch(faction: Faction): String = "${chosenLayer(faction, 1)}-${chosenLayer(faction, 2)}"

    // Set both hidden-layer widths from a concrete [arch] (an uploaded net) so the picker + resolve track it.
    private fun setArch(faction: Faction, arch: NetArch) {
        selectLayer(faction, 1, arch.hiddens.getOrElse(0) { DEFAULT_WIDTH }.toString())
        selectLayer(faction, 2, arch.hiddens.getOrElse(1) { DEFAULT_WIDTH }.toString())
    }

    // A signed 2-decimal fitness for the arch info label, e.g. +2.00 / -0.50.
    private fun signed(v: Double): String {
        val fixed = v.asDynamic().toFixed(2) as String
        return if (v >= 0) "+$fixed" else fixed
    }

    // The concrete arch to install for [faction], resolving each RANDOM layer off the WORLD SEED (a throwaway
    // stream, so the live RNG sequence is untouched) → a shared `?seed=…` link reproduces the same pick. Cached
    // per faction so it stays put across re-applies within a game.
    private fun resolvedArch(faction: Faction): NetArch =
        resolvedRandom.getOrPut(faction) { NetArch(listOf(resolveLayer(faction, 1), resolveLayer(faction, 2))) }

    private fun resolveLayer(faction: Faction, layer: Int): Int {
        val v = chosenLayer(faction, layer)
        if (v != RANDOM_LAYER) return v.toIntOrNull() ?: DEFAULT_WIDTH // a concrete (incl. uploaded non-sweep) width
        val rng = RngStream().apply { seed(Rng.currentSeed() xor ARCH_SALT xor faction.ordinal xor (layer shl 12)) }
        return ARCH_WIDTHS[rng.randomInt(ARCH_WIDTHS.size - 1)]
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
        Faction.all().forEachIndexed { i, faction ->
            if (i > 0) group.appendChild(el("span", "driverDivider").also { it.textContent = "|" }) // ENL | RES
            group.appendChild(picker(faction))
        }
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
        val loadNet = loadNetControl(faction) // upload + activate a shared net (net-only, next to the arch picker)
        val modelSel = modelPicker(faction)
        apply(faction, choice) // install it up front so the chosen brain plays from the first tick
        sel.onchange = {
            apply(faction, sel.value)
            setVisible(archSel, sel.value == "net") // the arch picker only matters for the neural-net driver
            setVisible(loadNet, sel.value == "net")
            setVisible(modelSel, sel.value == "llm") // the model picker only matters for the LLM driver
            null
        }
        setVisible(archSel, choice == "net")
        setVisible(loadNet, choice == "net")
        setVisible(modelSel, choice == "llm")
        wrap.appendChild(sel)
        wrap.appendChild(loadNet) // loading is an ALTERNATIVE to the two dropdowns, so it sits before them
        wrap.appendChild(archSel)
        wrap.appendChild(modelSel)
        return wrap
    }

    /**
     * Per-faction **net-architecture** picker (shown only while the Neural-net driver is selected) — **two**
     * dropdowns, one per hidden layer (width **4 / 8 / 16 / 24 / 32** or **Rnd**, seed-picked so AI-vs-AI stays
     * varied), plus a small label with the resolved arch + its baked fitness. Changing either reinstalls the net
     * driver with the chosen arch's champion. Shared by the toolbar picker + onboarding.
     */
    fun archPicker(faction: Faction): HTMLElement {
        val wrap = el("span", "aiArchPickers")
        val s1 = layerSelect(faction, 1)
        val s2 = layerSelect(faction, 2)
        archSelects[faction] = s1 to s2
        val info = el("span", "aiArchInfo")
        archInfo[faction] = info
        wrap.appendChild(s1)
        wrap.appendChild(el("span", "aiArchTimes").also { it.textContent = "×" })
        wrap.appendChild(s2)
        wrap.appendChild(info)
        refreshArchInfo(faction)
        return wrap
    }

    private fun layerSelect(faction: Faction, layer: Int): HTMLSelectElement {
        val sel = document.createElement("select") as HTMLSelectElement
        sel.className = "aiDriverSelect aiArchSelect"
        sel.title = "Hidden layer $layer width"
        sel.appendChild(option(RANDOM_LAYER, "Rnd", disabled = false))
        ARCH_WIDTHS.forEach { sel.appendChild(option(it.toString(), it.toString(), disabled = false)) }
        sel.value = chosenLayer(faction, layer)
        sel.onchange = {
            selectLayer(faction, layer, sel.value)
            refreshArchInfo(faction)
            if (selects[faction]?.value == "net") apply(faction, "net") // swap to the new arch's champion live
            null
        }
        return sel
    }

    // Update the arch label beside a faction's two dropdowns. If either layer is Rnd we just show "random" (the
    // concrete pick is resolved on apply, and can be re-rolled/switched during the game — we don't reveal it here);
    // with both widths fixed, show the arch + its baked fitness (e.g. "16×8 · +12.00"). Faction-coloured.
    private fun refreshArchInfo(faction: Faction) {
        val info = archInfo[faction] ?: return
        info.asDynamic().style.color = faction.color
        if (chosenLayer(faction, 1) == RANDOM_LAYER || chosenLayer(faction, 2) == RANDOM_LAYER) {
            info.textContent = "random"
            info.title = "A baked champion is seed-picked at match start"
            return
        }
        val arch = resolvedArch(faction)
        val dims = arch.hiddens.joinToString("×")
        val prov = ChampionLibrary.provenanceFor(arch)
        if (prov.isUser) {
            info.textContent = "$dims · ${clip(prov.filename ?: "user net", 11)}"
            info.title = "User net — ${prov.filename ?: "?"}${prov.installedAt?.let { " · loaded $it" } ?: ""}"
        } else {
            info.textContent = "$dims · ${ChampionLibrary.fitnessFor(arch)?.let { signed(it) } ?: "—"}"
            info.title = "Repo default champion (fitness = net checkpoints led vs the heuristic baseline)"
        }
    }

    private fun clip(s: String, max: Int): String = if (s.length <= max) s else s.take(max - 1) + "…"

    /**
     * A **"Load net…"** upload control (shared by the in-game picker + the onboarding teams grid) — imports a
     * shared genome JSON (as downloaded from the trainer), validates it for this build, installs it as its
     * architecture's champion (persisted via [ChampionLibrary.installChampion]), and activates it as [faction]'s
     * Neural-net brain. Shown alongside the arch picker (Neural-net only). [onActivated] lets the caller sync its
     * own UI (e.g. the onboarding driver buttons).
     */
    fun loadNetControl(faction: Faction, onActivated: () -> Unit = {}): HTMLElement {
        val wrap = el("span", "aiLoadNet")
        val status = el("span", "aiLoadNetStatus")
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = "application/json,.json"
        input.className = "invisible"
        input.onchange = {
            (input.files?.item(0))?.let { readNet(faction, it, status, onActivated) }
            input.value = "" // allow re-loading the same file
            null
        }
        val btn = el("button", "aiDriverSelect aiLoadNetBtn") as HTMLButtonElement
        btn.type = "button"
        btn.textContent = "Load net…"
        btn.title = "Import a shared net JSON and use it as this side's brain"
        btn.onclick = {
            input.click()
            null
        }
        wrap.appendChild(btn)
        wrap.appendChild(input)
        wrap.appendChild(status)
        return wrap
    }

    private fun readNet(faction: Faction, file: File, status: HTMLElement, onActivated: () -> Unit) {
        val reader = FileReader()
        reader.onload = {
            val text = reader.result as? String ?: ""
            runCatching {
                val net = GenomeIO.decode(text) // validate the genome for this build's net I/O
                // register + persist it as its arch's champion, tagged with the filename + load time (provenance)
                ChampionLibrary.installChampion(text, file.name, kotlin.js.Date().toLocaleString())
                net.arch
            }.onSuccess { arch ->
                activateLoadedNet(faction, arch, onActivated)
                status.textContent = "✓ ${file.name} → ${arch.hiddens.joinToString("×")}"
                status.asDynamic().style.color = "#a0a0a0" // neutral (R==G==B)
            }.onFailure {
                status.textContent = "✗ ${it.message ?: "not a valid net JSON"}"
                status.asDynamic().style.color = "#c0392b" // semantic error red
            }
            null
        }
        reader.readAsText(file)
    }

    private fun activateLoadedNet(faction: Faction, arch: NetArch, onActivated: () -> Unit) {
        setArch(faction, arch)
        syncArchSelects(faction, arch)
        select(faction, "net") // pending pick (onboarding carries it on the start URL)
        selects[faction]?.let {
            it.value = "net"
            apply(faction, "net") // in-game: activate the uploaded champion live
        }
        reflect(faction, "net")
        onActivated()
    }

    // Reflect a concrete [arch] onto the faction's two dropdowns + info label (an uploaded net). A non-sweep width
    // has no matching <option>, so the select just won't show it — but [resolveLayer] still reads the real width.
    private fun syncArchSelects(faction: Faction, arch: NetArch) {
        archSelects[faction]?.let { (s1, s2) ->
            s1.value = arch.hiddens.getOrElse(0) { DEFAULT_WIDTH }.toString()
            s2.value = arch.hiddens.getOrElse(1) { DEFAULT_WIDTH }.toString()
        }
        refreshArchInfo(faction)
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
