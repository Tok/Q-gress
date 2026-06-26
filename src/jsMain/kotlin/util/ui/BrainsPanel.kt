package util.ui

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
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.math.roundToInt

/**
 * The **BRAINS** footer tab — a side-by-side "who's driving each faction" view: the player's faction on the
 * LEFT, the opponent on the RIGHT. Each side renders a card tailored to that faction's current driver:
 * **Manual** (tune-it-yourself note), **Heuristic** (what the adaptive policy is doing + its live stance),
 * **Neural net** (architecture + fitness + the genome heatmap + the actions it favours), or **LLM** (the model
 * + backend + status, and its latest prompt / reply / parsed actions). It complements the NET tab (the live
 * activation diagram) and the AI tab (observation bars + slider history) by putting one clear driver summary
 * per faction in one place. Rebuilt per faction only when its state actually changes (LLM reply / checkpoint).
 */
object BrainsPanel {
    private const val GENOME_W = 320
    private const val GENOME_H = 90

    private var built = false
    private val cards = mutableMapOf<Faction, HTMLElement>()
    private val lastKey = mutableMapOf<Faction, String>()
    private var helpOpen = false // collapsible WebGPU-help state, preserved across card rebuilds

    fun update() {
        if (!ensure()) return
        sides().forEach { faction -> renderIfChanged(faction) }
    }

    // Left = the player's faction (fallback ENL), right = the opponent — so the layout reads "us vs them".
    private fun sides(): List<Faction> {
        val left = World.userFaction ?: Faction.ENL
        return listOf(left, left.enemy())
    }

    private fun ensure(): Boolean {
        if (built) return true
        if (document.body == null) return false
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
            is LlmPolicy -> "llm:${(d.client as? WebLlmClient)?.status}:${d.lastReply}:${WebLlmClient.gpuReport()}"
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
        card.appendChild(driverTitle("Neural net"))
        card.appendChild(
            p(
                "A trained neural net maps the ${Observation.SIZE} world observations → ${SliderVector.SIZE} behaviour sliders, re-tuned each checkpoint.",
            ),
        )
        card.appendChild(kv("Architecture", net.arch.label()))
        card.appendChild(kv("Fitness", GenomeIO.fitnessOf(NetStore.activeJson())?.let { "+${it.roundToInt()} MU" } ?: "—"))
        val canvas = canvas(GENOME_W, GENOME_H)
        card.appendChild(el("div", "brainsKey").also { it.textContent = "genome (weights — sign × magnitude)" })
        card.appendChild(canvas)
        (canvas.getContext("2d") as? CanvasRenderingContext2D)?.let {
            NetVizPanel.paintGenome(it, net, GENOME_W.toDouble(), GENOME_H.toDouble(), faction.color)
        }
        card.appendChild(topActions(faction))
        card.appendChild(p("See the NET tab for the live activation diagram."))
    }

    private fun renderLlm(card: HTMLElement, policy: LlmPolicy) {
        card.appendChild(driverTitle("LLM (experimental)"))
        card.appendChild(p("An in-browser LLM is asked each checkpoint for a slider vector (heuristic fallback until it replies)."))
        val client = policy.client as? WebLlmClient
        card.appendChild(kv("Model", client?.modelId ?: "mock"))
        card.appendChild(kv("Backend", "WebLLM (MLC) · WebGPU, in-browser"))
        card.appendChild(kv("Status", client?.status ?: "mock"))
        // GPU capability readout (one line) — the adapter's max-buffer limits are the closest thing to a VRAM
        // gauge the web exposes. The verbose troubleshooting lives in the collapsible help below.
        card.appendChild(kv("GPU", WebLlmClient.gpuReport()))
        card.appendChild(webGpuHelp())
        card.appendChild(el("div", "brainsKey").also { it.textContent = "prompt" })
        card.appendChild(pre(policy.lastPrompt))
        card.appendChild(el("div", "brainsKey").also { it.textContent = "reply" })
        card.appendChild(pre(policy.lastReply.ifBlank { "(warming up — heuristic fallback meanwhile)" }))
        val parsed = LlmParser.parse(policy.lastReply)
        card.appendChild(
            kv(
                "Chose",
                if (parsed == null) {
                    "unparsed → heuristic fallback"
                } else {
                    SliderVector.ORDER.sortedByDescending { parsed[it] }.take(3)
                        .joinToString(", ") { "${it.description} ${(parsed[it] * 100).toInt()}%" }
                },
            ),
        )
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
        details.appendChild(
            p(
                "The LLM needs WebGPU on a real GPU. If the GPU line above shows a software/hidden adapter or " +
                    "WebGPU errors, it'll fall back to the heuristic. Paste these into the address bar (they can't " +
                    "be links) and reload:",
            ),
        )
        details.appendChild(
            pre(
                "chrome://gpu                                 (check the Vulkan/WebGPU row names your GPU)\n" +
                    "chrome://flags/#enable-unsafe-webgpu\n" +
                    "chrome://flags/#enable-vulkan\n" +
                    "chrome://flags/#ignore-gpu-blocklist",
            ),
        )
        details.appendChild(
            p(
                "On Brave, also turn Shields' fingerprinting off for this page (it can hide the GPU). On " +
                    "Linux/NVIDIA, if WebGPU is stuck on \"SwiftShader\", launching the browser with " +
                    "--disable-gpu-sandbox often forces hardware Vulkan.",
            ),
        )
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

    private fun el(tag: String, cls: String): HTMLElement {
        val e = document.createElement(tag) as HTMLElement
        e.className = cls
        return e
    }
}
