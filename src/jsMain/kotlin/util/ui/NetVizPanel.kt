package util.ui

import agent.Faction
import ai.FactionPolicies
import ai.Observation
import ai.OverridePolicy
import ai.SliderVector
import ai.net.GenomeIO
import ai.net.Net
import ai.net.NetPolicy
import ai.net.NetStore
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The **NET** footer tab (PLAN Phase 6.2 payoff): a live activation diagram of the neural-net driver, beside a
 * stats sidebar. For each net-driven faction it draws every layer as a column of nodes — the [Observation]
 * inputs, each hidden layer (any depth — `net.arch`), and the [SliderVector] outputs — wired by edges whose
 * brightness
 * tracks each connection's live contribution (weight × upstream activation). Node brightness tracks
 * activation; the strongest outputs (the actions the net favours right now) are ringed + labelled, and the
 * top one's incoming edges are lit as the "chosen path". Recomputed each frame from the current observation,
 * so the net is visibly *thinking* as the match shifts (behaviour itself still re-tunes per checkpoint). The
 * canvas renders at device-pixel resolution for crisp lines; best viewed with the footer maximized.
 */
object NetVizPanel {
    private const val CW = 540
    private const val CH = 560
    private const val TOP = 46.0
    private const val BOT = 26.0
    private const val LEFT = 104.0 // room for the input labels on the left
    private const val RIGHT = 140.0 // room for the output labels on the right
    private const val NODE_R = 5.5
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

    // The sidebar's value cells (updated each frame from the live trace).
    private class StatsView(
        val arch: HTMLElement,
        val fitness: HTMLElement,
        val drive: HTMLElement,
        val peakHidden: HTMLElement,
        val actions: List<HTMLElement>,
    )

    private const val GW = 540 // genome heatmap width
    private const val GH = 96 // genome heatmap height
    private const val GENOME_COLS = 56 // weights per row in the heatmap

    private var built = false
    private val canvases = mutableMapOf<Faction, HTMLCanvasElement>()
    private val genomeCanvases = mutableMapOf<Faction, HTMLCanvasElement>()
    private val genomeDrawnFor = mutableMapOf<Faction, Net>() // the net whose genome is currently painted
    private val blocks = mutableMapOf<Faction, HTMLElement>()
    private val stats = mutableMapOf<Faction, StatsView>()
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

        // Column activations: observation, every hidden layer, then the sliders.
        val acts = buildList {
            add(trace.input)
            addAll(trace.hiddens)
            add(trace.output)
        }
        val cols = acts.size
        val layers = acts.indices.map { Layer(acts[it], colX(it, cols), layerYs(acts[it].size)) }

        captions(ctx, faction, layers)
        for (c in 0 until cols - 1) {
            edges(ctx, faction, layers[c], layers[c + 1]) { from, to -> net.weight(c, from, to) }
        }

        val top = topOutputs(trace.output)
        val lastTransition = cols - 2 // last hidden → output
        litPath(ctx, faction, layers[lastTransition], layers.last(), top.firstOrNull() ?: -1) { h, o ->
            net.weight(lastTransition, h, o)
        }

        val hiddenSigned = net.arch.activation.signed
        layers.forEachIndexed { i, layer -> nodes(ctx, faction, layer, signed = i != 0 && i != cols - 1 && hiddenSigned) }
        inputLabels(ctx, faction, layers.first())
        outputLabels(ctx, faction, layers.last(), top)
        updateStats(faction, net, trace, top)
        drawGenome(faction, net)
    }

    // The flat genome as a sign/magnitude heatmap (faction colour = positive, grey-blue = negative). Static
    // per net (weights don't change during a match), so it's painted once and only repainted when the net does.
    private fun drawGenome(faction: Faction, net: Net) {
        if (genomeDrawnFor[faction] === net) return
        genomeDrawnFor[faction] = net
        val ctx = genomeCanvases[faction]?.getContext("2d") as? CanvasRenderingContext2D ?: return
        paintGenome(ctx, net, GW.toDouble(), GH.toDouble(), faction.color)
    }

    /**
     * Paint [net]'s flat genome as a sign/magnitude heatmap into a dpr-scaled [ctx] (so [w]/[h] are CSS px):
     * [posColor] for positive weights, a cool grey-blue for negative, alpha = magnitude. Reused by the TRAIN
     * tab's champion preview ([TrainerPanel]) — the same readout the NET tab paints per faction.
     */
    fun paintGenome(ctx: CanvasRenderingContext2D, net: Net, w: Double, h: Double, posColor: String) {
        ctx.clearRect(0.0, 0.0, w, h)
        val genome = net.genome()
        val maxAbs = (genome.maxOfOrNull { abs(it) } ?: 1.0).coerceAtLeast(1e-6)
        val rows = (genome.size + GENOME_COLS - 1) / GENOME_COLS
        val cellW = w / GENOME_COLS
        val cellH = h / rows
        genome.forEachIndexed { i, weight ->
            ctx.globalAlpha = (abs(weight) / maxAbs).coerceIn(0.05, 1.0)
            ctx.fillStyle = if (weight >= 0) posColor else NEG_COLOR
            ctx.fillRect((i % GENOME_COLS) * cellW, (i / GENOME_COLS) * cellH, cellW + 0.5, cellH + 0.5)
        }
        ctx.globalAlpha = 1.0
    }

    // Even horizontal spread of [cols] columns across the canvas (between the label margins).
    private fun colX(index: Int, cols: Int): Double = if (cols <= 1) LEFT else LEFT + (CW - LEFT - RIGHT) * (index.toDouble() / (cols - 1))

    private fun updateStats(faction: Faction, net: Net, trace: Net.Trace, top: List<Int>) {
        val view = stats[faction] ?: return
        view.arch.textContent = net.arch.label()
        view.fitness.textContent = GenomeIO.fitnessOf(NetStore.activeJson())?.let { "+${it.roundToInt()} MU" } ?: "—"
        val driveIdx = trace.input.indices.maxByOrNull { trace.input[it] } ?: 0
        view.drive.textContent = "${IN_LABELS.getOrElse(driveIdx) { "f$driveIdx" }} ${pct(trace.input[driveIdx])}"
        view.peakHidden.textContent = peakHidden(trace.hiddens)
        view.actions.forEachIndexed { rank, valueEl ->
            val o = top.getOrNull(rank)
            valueEl.textContent = if (o == null) "—" else "${OUT_LABELS[o]} ${pct(trace.output[o])}"
        }
    }

    // The most-active hidden neuron across every hidden layer, labelled "L<layer> #<index> <±value>".
    private fun peakHidden(hiddens: List<DoubleArray>): String {
        var bestLayer = 0
        var bestIndex = 0
        var bestMag = -1.0
        hiddens.forEachIndexed { layer, acts ->
            acts.forEachIndexed { index, v ->
                if (abs(v) > bestMag) {
                    bestMag = abs(v)
                    bestLayer = layer
                    bestIndex = index
                }
            }
        }
        if (bestMag < 0) return "—"
        return "L${bestLayer + 1} #$bestIndex ${signed(hiddens[bestLayer][bestIndex])}"
    }

    // y-centre of node [i] of [n], spread evenly down the canvas.
    private fun layerYs(n: Int): DoubleArray = DoubleArray(n) { TOP + (CH - TOP - BOT) * ((it + 0.5) / n) }

    private fun captions(ctx: CanvasRenderingContext2D, faction: Faction, layers: List<Layer>) {
        ctx.globalAlpha = 0.8
        ctx.fillStyle = faction.color
        ctx.font = "13px '$FONT'"
        ctx.asDynamic().textAlign = "center"
        val last = layers.size - 1
        layers.forEachIndexed { i, layer ->
            val caption = when (i) {
                0 -> "observation"
                last -> "sliders"
                else -> "hidden $i"
            }
            ctx.fillText(caption, layer.x, 20.0)
        }
        ctx.globalAlpha = 1.0
    }

    private fun edges(ctx: CanvasRenderingContext2D, faction: Faction, from: Layer, to: Layer, weight: (Int, Int) -> Double) {
        var max = 1e-6
        for (a in from.acts.indices) for (b in to.acts.indices) max = maxOf(max, abs(from.acts[a] * weight(a, b)))
        ctx.lineWidth = 0.9
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
        ctx.lineWidth = 1.8
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
        ctx.font = "11px '$FONT'"
        ctx.asDynamic().textAlign = "right"
        ctx.fillStyle = faction.color
        ctx.globalAlpha = 0.7
        layer.ys.forEachIndexed { i, y -> ctx.fillText(IN_LABELS.getOrElse(i) { "f$i" }, layer.x - NODE_R - 7.0, y + 4.0) }
        ctx.globalAlpha = 1.0
    }

    private fun outputLabels(ctx: CanvasRenderingContext2D, faction: Faction, layer: Layer, top: List<Int>) {
        ctx.font = "12px '$FONT'"
        ctx.asDynamic().textAlign = "left"
        top.forEach { o ->
            ctx.globalAlpha = 0.95
            ctx.strokeStyle = faction.color
            ctx.lineWidth = 1.4
            ctx.beginPath()
            ctx.arc(layer.x, layer.ys[o], NODE_R + 3.0, 0.0, TAU)
            ctx.stroke()
            ctx.fillStyle = faction.color
            ctx.fillText("${OUT_LABELS[o]}  ${pct(layer.acts[o])}", layer.x + 12.0, layer.ys[o] + 4.0)
        }
        ctx.globalAlpha = 1.0
    }

    private fun topOutputs(output: DoubleArray): List<Int> = output.indices.sortedByDescending { output[it] }.take(TOP_ACTIONS)

    private fun ensure(): Boolean {
        if (built) return true
        if (document.body == null) return false
        val panel = el("div", "netVizPanel")
        Faction.all().forEach { faction -> panel.appendChild(factionBlock(faction)) }
        val hintEl = el("div", "netVizHint")
        hintEl.textContent = "Set a faction's driver to “Neural net” in the AI tab to watch it think."
        hint = hintEl
        panel.appendChild(hintEl)
        Footer.tab("net").appendChild(panel)
        built = true
        return true
    }

    private fun factionBlock(faction: Faction): HTMLElement {
        val block = el("div", "netVizCol")
        val head = el("div", "netVizHead").also {
            it.textContent = "${faction.abbr} · neural net"
            it.asDynamic().style.color = faction.color
        }
        block.appendChild(head)
        block.appendChild(statsFor(faction)) // a compact stat grid (≈4 per row) above the diagram
        // The activation diagram sits on its own darker off-tint glass pane (vs the busy blurred map showing
        // through the footer) so the faction-coloured connection lines read clearly.
        val diagram = el("div", "netVizDiagram")
        diagram.appendChild(dprCanvas(CW, CH).also { canvases[faction] = it })
        block.appendChild(diagram)
        block.appendChild(el("div", "netVizSub").also { it.textContent = "genome (weights — sign × magnitude)" })
        block.appendChild(dprCanvas(GW, GH).also { genomeCanvases[faction] = it })
        blocks[faction] = block
        return block
    }

    // A device-pixel-resolution canvas → crisp lines/fills on HiDPI displays (CSS size w×h, backing store ×dpr).
    private fun dprCanvas(w: Int, h: Int): HTMLCanvasElement {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        val dpr = window.devicePixelRatio.takeIf { it > 0.0 } ?: 1.0
        canvas.width = (w * dpr).toInt()
        canvas.height = (h * dpr).toInt()
        canvas.style.width = "${w}px"
        canvas.style.height = "${h}px"
        (canvas.getContext("2d") as? CanvasRenderingContext2D)?.scale(dpr, dpr)
        return canvas
    }

    private fun statsFor(faction: Faction): HTMLElement {
        val box = el("div", "netVizStats")
        val arch = statCell(box, "Network")
        val fitness = statCell(box, "Fitness")
        val drive = statCell(box, "Driving input")
        val peakHidden = statCell(box, "Peak hidden")
        val actions = listOf("Top action", "2nd", "3rd").map { label ->
            statCell(box, label).also { it.asDynamic().style.color = faction.color }
        }
        stats[faction] = StatsView(arch, fitness, drive, peakHidden, actions)
        return box
    }

    // One stat cell in the grid: a small key label over its (live-updated) value. Returns the value element.
    private fun statCell(parent: HTMLElement, key: String): HTMLElement {
        val cell = el("div", "netVizStatCell")
        cell.appendChild(el("span", "netVizStatKey").also { it.textContent = key })
        val value = el("span", "netVizStatVal")
        cell.appendChild(value)
        parent.appendChild(cell)
        return value
    }

    private fun setVisible(e: HTMLElement, visible: Boolean) {
        e.asDynamic().style.display = if (visible) "" else "none"
    }

    private fun pct(value: Double): String = "${(value * 100.0).roundToInt()}%"

    private fun signed(value: Double): String = (if (value >= 0) "+" else "") + (value.asDynamic().toFixed(2) as String)

    private fun el(tag: String, cls: String): HTMLElement {
        val e = document.createElement(tag) as HTMLElement
        e.className = cls
        return e
    }
}
