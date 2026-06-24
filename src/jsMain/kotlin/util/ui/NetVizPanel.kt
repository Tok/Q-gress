package util.ui

import agent.Faction
import ai.FactionPolicies
import ai.Observation
import ai.OverridePolicy
import ai.SliderVector
import ai.net.Net
import ai.net.NetPolicy
import kotlinx.browser.document
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow

/**
 * The **NET** footer tab (PLAN Phase 6.2 payoff): a live activation diagram of the neural-net driver. For
 * each net-driven faction it draws the three layers as columns of nodes — the [Observation] inputs, the
 * hidden neurons, and the [SliderVector] outputs — wired by edges whose brightness tracks each connection's
 * live contribution (weight × upstream activation). Node brightness tracks activation; the strongest outputs
 * (the actions the net favours right now) are ringed + labelled, and the top one's incoming edges are lit as
 * the "chosen path". Recomputed each frame from the current observation, so the net is visibly *thinking* as
 * the match shifts (behaviour itself still re-tunes only at checkpoint cadence).
 */
object NetVizPanel {
    private const val CW = 380
    private const val CH = 300
    private const val TOP = 30.0
    private const val BOT = 14.0
    private const val NODE_R = 4.0
    private const val TOP_ACTIONS = 3 // outputs to ring + label as the favoured actions
    private const val FONT = "Coda" // the HUD number/text face
    private const val TAU = 2.0 * PI
    private const val NEG_COLOR = "#6b7a99" // cool grey-blue for a negative (inhibitory) activation/contribution

    private val OUT_LABELS: List<String> = SliderVector.ORDER.map { it.description }

    // One label per Observation slot (in observe() order) — drawn left of the input column.
    private val IN_LABELS = listOf(
        "cycle", "mind units", "portals", "portals (foe)", "neutral",
        "links", "fields", "roster", "roster (foe)",
        "level", "level (foe)", "xm", "xm (foe)",
    )

    // One rendered layer: its activations, its column x, and the y-centre of each node.
    private class Layer(val acts: DoubleArray, val x: Double, val ys: DoubleArray)

    private var built = false
    private val canvases = mutableMapOf<Faction, HTMLCanvasElement>()
    private val blocks = mutableMapOf<Faction, HTMLElement>()
    private var hint: HTMLElement? = null

    fun update() {
        if (!ensure()) return
        var anyNet = false
        Faction.all().forEach { faction ->
            val net = netOf(faction)
            blocks[faction]?.let { setVisible(it, net != null) }
            if (net != null) {
                anyNet = true
                draw(faction, net)
            }
        }
        hint?.let { setVisible(it, !anyNet) }
    }

    // The net driving [faction] right now, unwrapping a player-override layer, or null if not net-driven.
    private fun netOf(faction: Faction): Net? {
        val policy = FactionPolicies.of(faction)
        val base = (policy as? OverridePolicy)?.inner ?: policy
        return (base as? NetPolicy)?.net
    }

    private fun draw(faction: Faction, net: Net) {
        val ctx = canvases[faction]?.getContext("2d") as? CanvasRenderingContext2D ?: return
        val trace = net.forwardTraced(Observation.observe(faction))
        ctx.clearRect(0.0, 0.0, CW.toDouble(), CH.toDouble())

        val input = Layer(trace.input, 46.0, layerYs(trace.input.size))
        val hidden = Layer(trace.hidden, CW * 0.46, layerYs(trace.hidden.size))
        val output = Layer(trace.output, CW * 0.74, layerYs(trace.output.size))

        captions(ctx, faction, input.x, hidden.x, output.x)
        edges(ctx, faction, input, hidden) { i, h -> net.inputWeight(i, h) }
        edges(ctx, faction, hidden, output) { h, o -> net.hiddenWeight(h, o) }

        val top = topOutputs(trace.output)
        litPath(ctx, faction, hidden, output, top.firstOrNull() ?: -1) { h, o -> net.hiddenWeight(h, o) }

        nodes(ctx, faction, input, signed = false)
        nodes(ctx, faction, hidden, signed = true)
        nodes(ctx, faction, output, signed = false)
        inputLabels(ctx, faction, input)
        outputLabels(ctx, faction, output, top)
    }

    // y-centre of node [i] of [n], spread evenly down the canvas.
    private fun layerYs(n: Int): DoubleArray = DoubleArray(n) { TOP + (CH - TOP - BOT) * ((it + 0.5) / n) }

    private fun captions(ctx: CanvasRenderingContext2D, faction: Faction, inX: Double, hidX: Double, outX: Double) {
        ctx.globalAlpha = 0.75
        ctx.fillStyle = faction.color
        ctx.font = "10px '$FONT'"
        ctx.asDynamic().textAlign = "center"
        ctx.fillText("observation", inX, 14.0)
        ctx.fillText("hidden", hidX, 14.0)
        ctx.fillText("sliders", outX, 14.0)
        ctx.globalAlpha = 1.0
    }

    private fun edges(ctx: CanvasRenderingContext2D, faction: Faction, from: Layer, to: Layer, weight: (Int, Int) -> Double) {
        var max = 1e-6
        for (a in from.acts.indices) for (b in to.acts.indices) max = maxOf(max, abs(from.acts[a] * weight(a, b)))
        ctx.lineWidth = 0.7
        for (a in from.acts.indices) {
            for (b in to.acts.indices) {
                val contribution = from.acts[a] * weight(a, b)
                val mag = (abs(contribution) / max).pow(1.4)
                if (mag < 0.04) continue
                ctx.globalAlpha = mag * 0.5
                ctx.strokeStyle = if (contribution >= 0) faction.color else NEG_COLOR
                edge(ctx, from.x, from.ys[a], to.x, to.ys[b])
            }
        }
        ctx.globalAlpha = 1.0
    }

    // Light up the incoming edges of the single top output — the "chosen path".
    private fun litPath(
        ctx: CanvasRenderingContext2D,
        faction: Faction,
        hidden: Layer,
        output: Layer,
        topOut: Int,
        weight: (Int, Int) -> Double,
    ) {
        if (topOut < 0) return
        var max = 1e-6
        for (h in hidden.acts.indices) max = maxOf(max, abs(hidden.acts[h] * weight(h, topOut)))
        ctx.lineWidth = 1.4
        ctx.strokeStyle = faction.color
        for (h in hidden.acts.indices) {
            val mag = (abs(hidden.acts[h] * weight(h, topOut)) / max).pow(1.2)
            if (mag < 0.15) continue
            ctx.globalAlpha = (0.35 + mag * 0.55).coerceAtMost(0.9)
            edge(ctx, hidden.x, hidden.ys[h], output.x, output.ys[topOut])
        }
        ctx.globalAlpha = 1.0
    }

    private fun edge(ctx: CanvasRenderingContext2D, x0: Double, y0: Double, x1: Double, y1: Double) {
        ctx.beginPath()
        ctx.moveTo(x0, y0)
        ctx.lineTo(x1, y1)
        ctx.stroke()
    }

    private fun nodes(ctx: CanvasRenderingContext2D, faction: Faction, layer: Layer, signed: Boolean) {
        layer.acts.forEachIndexed { i, v ->
            val mag = if (signed) abs(v) else v.coerceIn(0.0, 1.0)
            ctx.globalAlpha = (0.18 + mag * 0.82).coerceIn(0.0, 1.0)
            ctx.fillStyle = if (signed && v < 0) NEG_COLOR else faction.color
            ctx.beginPath()
            ctx.arc(layer.x, layer.ys[i], NODE_R, 0.0, TAU)
            ctx.fill()
        }
        ctx.globalAlpha = 1.0
    }

    private fun inputLabels(ctx: CanvasRenderingContext2D, faction: Faction, layer: Layer) {
        ctx.font = "8px '$FONT'"
        ctx.asDynamic().textAlign = "right"
        ctx.fillStyle = faction.color
        ctx.globalAlpha = 0.65
        layer.ys.forEachIndexed { i, y -> ctx.fillText(IN_LABELS.getOrElse(i) { "f$i" }, layer.x - NODE_R - 4.0, y + 3.0) }
        ctx.globalAlpha = 1.0
    }

    private fun outputLabels(ctx: CanvasRenderingContext2D, faction: Faction, layer: Layer, top: List<Int>) {
        ctx.font = "9px '$FONT'"
        ctx.asDynamic().textAlign = "left"
        top.forEach { o ->
            ctx.globalAlpha = 0.95
            ctx.strokeStyle = faction.color
            ctx.lineWidth = 1.2
            ctx.beginPath()
            ctx.arc(layer.x, layer.ys[o], NODE_R + 2.5, 0.0, TAU)
            ctx.stroke()
            ctx.fillStyle = faction.color
            ctx.fillText("${OUT_LABELS[o]}  ${(layer.acts[o] * 100).toInt()}%", layer.x + 9.0, layer.ys[o] + 3.0)
        }
        ctx.globalAlpha = 1.0
    }

    private fun topOutputs(output: DoubleArray): List<Int> = output.indices.sortedByDescending { output[it] }.take(TOP_ACTIONS)

    private fun ensure(): Boolean {
        if (built) return true
        if (document.body == null) return false
        val panel = el("div", "netVizPanel")
        panel.asDynamic().style.display = "flex"
        panel.asDynamic().style.gap = "20px"
        panel.asDynamic().style.flexWrap = "wrap"
        Faction.all().forEach { faction -> panel.appendChild(factionBlock(faction)) }
        val hintEl = el("div", "netVizHint")
        hintEl.textContent = "Set a faction's driver to “Neural net” in the AI tab to watch it think."
        hintEl.asDynamic().style.opacity = "0.7"
        hint = hintEl
        panel.appendChild(hintEl)
        Footer.tab("net").appendChild(panel)
        built = true
        return true
    }

    private fun factionBlock(faction: Faction): HTMLElement {
        val block = el("div", "netVizCol")
        val head = el("div", "netVizHead")
        head.textContent = "${faction.abbr} · neural net"
        head.asDynamic().style.color = faction.color
        block.appendChild(head)
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = CW
        canvas.height = CH
        block.appendChild(canvas)
        canvases[faction] = canvas
        blocks[faction] = block
        return block
    }

    private fun setVisible(e: HTMLElement, visible: Boolean) {
        e.asDynamic().style.display = if (visible) "" else "none"
    }

    private fun el(tag: String, cls: String): HTMLElement {
        val e = document.createElement(tag) as HTMLElement
        e.className = cls
        return e
    }
}
