package system.ui

import World
import agent.Faction
import ai.FactionPolicies
import ai.HeuristicPolicy
import ai.Observation
import ai.OverridePolicy
import ai.SliderVector
import ai.llm.LlmParser
import ai.llm.LlmPolicy
import ai.llm.WebLlmClient
import ai.net.GenomeIO
import ai.net.Net
import ai.net.NetPolicy
import ai.net.NetStore
import config.Config
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.math.roundToInt

/**
 * The **BRAINS** footer tab — a side-by-side "who's driving each faction" view: the player's faction on the
 * LEFT, the opponent on the RIGHT. Each side renders a card tailored to that faction's current driver:
 * **Manual** (tune-it-yourself note), **Heuristic** (what the adaptive policy is doing + its live stance),
 * **Neural net** (architecture + fitness + the genome heatmap + the actions it favours), or **LLM** (the model
 * + backend + status, its latest prompt / reply / parsed actions). For a net it also hosts the **live
 * activation diagram** (repainted each frame) above the genome heatmap, plus the driving-input / peak-hidden
 * readouts — the former NET tab folded in here. The card text rebuilds per faction only when its state changes
 * (LLM reply / checkpoint); the activation canvas repaints every frame.
 */
object BrainsPanel {
    private const val GENOME_W = 320
    private const val GENOME_H = 90

    private var built = false
    private val cards = mutableMapOf<Faction, HTMLElement>()
    private val lastKey = mutableMapOf<Faction, String>()
    private var helpOpen = false // collapsible WebGPU-help state, preserved across card rebuilds
    private var rawOpen = false // collapsible raw prompt/reply state, preserved across card rebuilds
    private val activationCanvas = mutableMapOf<Faction, HTMLCanvasElement?>() // per NN faction; repainted each frame

    fun update() {
        if (!ensure()) return
        sides().forEach { faction ->
            renderIfChanged(faction)
            repaintActivation(faction) // live net activation (per frame, between the per-checkpoint card rebuilds)
        }
    }

    // Repaint the NN activation diagram each frame so the net is visibly "thinking" as the observation shifts.
    private fun repaintActivation(faction: Faction) {
        val canvas = activationCanvas[faction] ?: return
        val net = (driverOf(faction) as? NetPolicy)?.net ?: return
        val ctx = canvas.getContext("2d") as? CanvasRenderingContext2D ?: return
        NetVizPanel.paintActivation(ctx, net, faction, NetVizPanel.CW.toDouble(), NetVizPanel.CH.toDouble())
    }

    // Left = the player's faction (fallback ENL), right = the opponent — so the layout reads "us vs them".
    private fun sides(): List<Faction> {
        val left = World.userFaction ?: Faction.ENL
        return listOf(left, left.enemy())
    }

    private fun ensure(): Boolean {
        if (built) return true
        if (document.body == null) return false
        // The per-faction driver pickers ("AI vs AI") moved here from the top toolbar — you pick each side's
        // brain in the same tab that explains what each brain is doing.
        Footer.tab("brains").appendChild(DriverControls.toolbarGroup())
        val split = el("div", "brainsSplit")
        sides().forEach { faction ->
            val side = el("div", "brainsSide")
            side.appendChild(
                el("div", "brainsSideHead").also {
                    it.textContent = faction.abbr
                    it.asDynamic().style.color = faction.color
                },
            )
            val card = el("div", "brainsCard")
            cards[faction] = card
            side.appendChild(card)
            split.appendChild(side)
        }
        Footer.tab("brains").appendChild(split)
        built = true
        return true
    }

    private fun renderIfChanged(faction: Faction) {
        val card = cards[faction] ?: return
        val key = stateKey(faction)
        if (key == lastKey[faction]) return
        lastKey[faction] = key
        card.innerHTML = ""
        activationCanvas[faction] = null // dropped on rebuild; renderNet re-creates it when this side is a net
        when (val inner = driverOf(faction)) {
            is NetPolicy -> renderNet(card, faction, inner.net)
            is LlmPolicy -> renderLlm(card, inner)
            is HeuristicPolicy -> renderHeuristic(card, faction)
            else -> renderManual(card)
        }
    }

    private fun driverOf(faction: Faction) = FactionPolicies.of(faction).let { (it as? OverridePolicy)?.inner ?: it }

    // A coarse signature that changes whenever the card's content should: driver type, the checkpoint (net /
    // heuristic re-tune cadence), and the LLM's status + latest reply.
    private fun stateKey(faction: Faction): String {
        val checkpoint = World.tick / Config.ticksPerCheckpoint
        return when (val d = driverOf(faction)) {
            is NetPolicy -> "net:${d.net.arch.label()}:$checkpoint"
            is LlmPolicy -> "llm:${(d.client as? WebLlmClient)?.status}:${d.lastReply}:${d.lastReplyCheckpoint}:${WebLlmClient.gpuReport()}"
            is HeuristicPolicy -> "heuristic:$checkpoint"
            else -> "manual"
        }
    }

    // --- per-driver cards ----------------------------------------------------

    private fun renderManual(card: HTMLElement) {
        card.appendChild(driverTitle("Manual"))
        card.appendChild(p("You drive this faction yourself — set its behaviour with the TUNE-tab sliders."))
    }

    private fun renderHeuristic(card: HTMLElement, faction: Faction) {
        card.appendChild(driverTitle("Heuristic"))
        card.appendChild(
            p(
                "A hand-written adaptive policy. Each checkpoint it re-reads the board and shifts the sliders: " +
                    "press the attack when behind on MU, consolidate into links/fields when ahead, and hack/glyph " +
                    "to refuel when XM runs low.",
            ),
        )
        val obs = Observation.observe(faction)
        val muShare = obs.getOrElse(1) { 0.5 }
        val avgXm = obs.getOrElse(11) { 0.5 }
        val stance = when {
            muShare > 0.55 -> "ahead → consolidating into fields"
            muShare < 0.45 -> "behind → pressing the attack"
            else -> "even → building toward fields"
        }
        val fuel = if (avgXm < 0.4) " · low XM → hacking to refuel" else ""
        card.appendChild(kv("Stance", stance + fuel))
        card.appendChild(topActions(faction))
    }

    private fun renderNet(card: HTMLElement, faction: Faction, net: Net) {
        card.appendChild(driverTitle("Neural net")) // title outside the pane (consistent across tabs)
        // The NN summary readout in its own glass pane (the part that needs to read clearly over the map).
        val summary = el("div", "footerGlass")
        summary.appendChild(kv("Architecture", net.arch.label()))
        summary.appendChild(kv("Fitness", GenomeIO.fitnessOf(NetStore.activeJson())?.let { "+${it.roundToInt()} MU" } ?: "—"))
        summary.appendChild(kv("Re-tunes", retuneCadence()))
        val stats = NetVizPanel.stats(net, faction)
        summary.appendChild(kv("Driving input", stats.drivingInput))
        summary.appendChild(kv("Peak hidden", stats.peakHidden))
        summary.appendChild(topActions(faction))
        card.appendChild(summary)
        // Live activation diagram ABOVE the genome (per-frame repaint in update()) — no pane (label is a title).
        card.appendChild(el("div", "brainsKey").also { it.textContent = "live activation (the net thinking)" })
        val act = dprCanvas(NetVizPanel.CW, NetVizPanel.CH)
        activationCanvas[faction] = act
        card.appendChild(el("div", "netVizDiagram").also { it.appendChild(act) })
        (act.getContext("2d") as? CanvasRenderingContext2D)?.let {
            NetVizPanel.paintActivation(it, net, faction, NetVizPanel.CW.toDouble(), NetVizPanel.CH.toDouble())
        }
        // …then the genome heatmap in its own small glass pane (the label stays outside it).
        card.appendChild(el("div", "brainsKey").also { it.textContent = "genome (weights — sign × magnitude)" })
        val gen = canvas(GENOME_W, GENOME_H)
        card.appendChild(el("div", "footerGlass brainsGenomePane").also { it.appendChild(gen) })
        (gen.getContext("2d") as? CanvasRenderingContext2D)?.let {
            NetVizPanel.paintGenome(it, net, GENOME_W.toDouble(), GENOME_H.toDouble(), faction.color)
        }
    }

    // Both the net and the LLM re-tune the sliders once per scoring checkpoint — surface that cadence.
    private fun retuneCadence(): String = "every checkpoint (${Config.ticksPerCheckpoint} ticks)"

    // The LLM asks the model once per checkpoint; show the cadence + which checkpoint last actually replied
    // (inference is slow + async, so it can lag the current checkpoint) — so the turn frequency is visible.
    private fun turnCadence(policy: LlmPolicy): String {
        val now = World.tick / Config.ticksPerCheckpoint
        val last = if (policy.lastReplyCheckpoint < 0) "none yet" else "checkpoint ${policy.lastReplyCheckpoint}"
        return "1 / checkpoint (${Config.ticksPerCheckpoint} ticks) · now at $now · last reply: $last"
    }

    // A device-pixel-resolution canvas → crisp activation lines/labels on HiDPI (CSS size w×h, store ×dpr).
    private fun dprCanvas(w: Int, h: Int): HTMLCanvasElement {
        val c = document.createElement("canvas") as HTMLCanvasElement
        val dpr = window.devicePixelRatio.takeIf { it > 0.0 } ?: 1.0
        c.width = (w * dpr).toInt()
        c.height = (h * dpr).toInt()
        c.style.width = "${w}px"
        c.style.height = "${h}px"
        c.className = "brainsActivation"
        (c.getContext("2d") as? CanvasRenderingContext2D)?.scale(dpr, dpr)
        return c
    }

    private fun renderLlm(card: HTMLElement, policy: LlmPolicy) {
        card.appendChild(driverTitle("LLM (experimental)"))
        val client = policy.client as? WebLlmClient
        val ready = client?.status == "ready"
        card.appendChild(kv("Model", client?.modelId ?: "mock"))
        card.appendChild(kv("Status", client?.status ?: "mock"))
        card.appendChild(kv("Turns", turnCadence(policy)))
        card.appendChild(kv("GPU", WebLlmClient.gpuReport()))
        // The headline once it's running: what the LLM chose this checkpoint (big + bold = the main readout).
        val parsed = LlmParser.parse(policy.lastReply)
        val chose = when {
            parsed != null -> SliderVector.ORDER.sortedByDescending { parsed[it] }
                .take(3).joinToString(", ") { "${it.description} ${(parsed[it] * 100).toInt()}%" }
            ready -> "(reply not parseable — heuristic fallback)"
            else -> "warming up — heuristic fallback meanwhile…"
        }
        card.appendChild(el("div", "brainsKey").also { it.textContent = "chose" })
        card.appendChild(el("div", "brainsChose").also { it.textContent = chose })
        // Raw prompt/reply tucked into a compact collapsed disclosure so they don't dominate the card.
        card.appendChild(rawIo(policy))
        // Setup help is only useful until it actually runs — drop it once the model is ready.
        if (!ready) card.appendChild(webGpuHelp())
    }

    private fun rawIo(policy: LlmPolicy): HTMLElement {
        val details = document.createElement("details") as HTMLElement
        details.className = "brainsHelp"
        details.asDynamic().open = rawOpen
        details.asDynamic().ontoggle = {
            rawOpen = details.asDynamic().open as Boolean
            null
        }
        val summary = document.createElement("summary") as HTMLElement
        summary.className = "brainsHelpSummary"
        summary.textContent = "raw prompt / reply"
        details.appendChild(summary)
        details.appendChild(el("div", "brainsKey").also { it.textContent = "prompt" })
        details.appendChild(pre(policy.lastPrompt.ifBlank { "(none yet)" }))
        details.appendChild(el("div", "brainsKey").also { it.textContent = "reply" })
        details.appendChild(pre(policy.lastReply.ifBlank { "(none yet)" }))
        return details
    }

    // Collapsible WebGPU troubleshooting — tucked away so the LLM card isn't a wall of help text. The chrome://
    // entries are copy-paste only (browsers block navigating to chrome:// from a page, so they can't be links).
    private fun webGpuHelp(): HTMLElement {
        val details = document.createElement("details") as HTMLElement
        details.className = "brainsHelp"
        details.asDynamic().open = helpOpen
        details.asDynamic().ontoggle = {
            helpOpen = details.asDynamic().open as Boolean
            null
        }
        val summary = document.createElement("summary") as HTMLElement
        summary.className = "brainsHelpSummary"
        summary.textContent = "WebGPU won't run? (troubleshooting)"
        details.appendChild(summary)
        if (WebLlmClient.isChromiumLike()) {
            details.appendChild(
                p(
                    "The LLM needs WebGPU on a real GPU. If the GPU line above shows a software/hidden adapter " +
                        "or WebGPU errors, it falls back to the heuristic. Open these (browsers block clicking a " +
                        "chrome:// link, so right-click → copy, or paste into the address bar) and reload:",
                ),
            )
            val links = el("div", "brainsLinks")
            listOf(
                "chrome://gpu" to "chrome://gpu  — check the Vulkan/WebGPU row names your GPU",
                "chrome://flags/#enable-unsafe-webgpu" to "chrome://flags/#enable-unsafe-webgpu",
                "chrome://flags/#enable-vulkan" to "chrome://flags/#enable-vulkan",
                "chrome://flags/#ignore-gpu-blocklist" to "chrome://flags/#ignore-gpu-blocklist",
            ).forEach { (url, label) -> links.appendChild(chromeLink(url, label)) }
            details.appendChild(links)
            details.appendChild(
                p(
                    "On Brave, also turn Shields' fingerprinting off for this page (it can hide the GPU). On " +
                        "Linux/NVIDIA, if WebGPU is stuck on \"SwiftShader\", launching the browser with " +
                        "--disable-gpu-sandbox often forces hardware Vulkan.",
                ),
            )
        } else {
            details.appendChild(
                p(
                    "The LLM needs WebGPU on a real GPU, which works best in a recent Chromium-based browser " +
                        "(Chrome / Brave / Edge). Enable WebGPU in your browser's settings, or switch to one of " +
                        "those for the in-browser LLM; otherwise this side stays on the heuristic.",
                ),
            )
        }
        return details
    }

    // Top-3 sliders the driver currently favours (its installed vector) — the "what it's doing now" line.
    private fun topActions(faction: Faction): HTMLElement {
        val vector: SliderVector? = FactionPolicies.of(faction).currentVector()
        val text = if (vector == null) {
            "—"
        } else {
            SliderVector.ORDER.sortedByDescending { vector[it] }.take(3)
                .joinToString(", ") { "${it.description} ${(vector[it] * 100).toInt()}%" }
        }
        return kv("Favours", text)
    }

    // --- tiny DOM helpers ----------------------------------------------------

    private fun driverTitle(name: String) = el("div", "brainsDriver").also { it.textContent = name }
    private fun p(text: String) = el("div", "brainsText").also { it.textContent = text }

    private fun kv(key: String, value: String): HTMLElement {
        val row = el("div", "brainsKv")
        row.appendChild(el("span", "brainsKvKey").also { it.textContent = key })
        row.appendChild(el("span", "brainsKvVal").also { it.textContent = value })
        return row
    }

    private fun chromeLink(url: String, label: String): HTMLElement {
        val a = document.createElement("a") as org.w3c.dom.HTMLAnchorElement
        a.className = "brainsLink"
        a.href = url
        a.textContent = label
        return a
    }

    private fun pre(text: String): HTMLElement {
        val e = document.createElement("pre") as HTMLElement
        e.className = "brainsPre"
        e.textContent = text
        return e
    }

    private fun canvas(w: Int, h: Int): HTMLCanvasElement {
        val c = document.createElement("canvas") as HTMLCanvasElement
        c.width = w
        c.height = h
        c.className = "brainsGenome"
        return c
    }
}
