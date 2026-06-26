package util.ui

import agent.Faction
import ai.Observation
import ai.SliderVector
import ai.net.Net
import org.w3c.dom.CanvasRenderingContext2D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow

/**
 * Neural-net **visualization** helpers (PLAN Phase 6.2 payoff) — now hosted inside the **BRAINS** tab rather
 * than a standalone NET tab. [paintActivation] draws the live activation diagram for a net-driven faction
 * (every layer as a column of nodes — the [Observation] inputs, each hidden layer, the [SliderVector] outputs
 * — wired by edges whose brightness tracks each connection's live contribution; the strongest outputs ringed
 * + labelled, the top one's incoming edges lit as the "chosen path"). [paintGenome] draws the flat genome as a
 * sign/magnitude heatmap. Both take a dpr-scaled context + CSS-px size, so the caller owns the canvas.
 */
object NetVizPanel {
    const val CW = 480 // default activation-canvas width (CSS px)
    const val CH = 460 // default activation-canvas height (CSS px)
    private const val TOP = 46.0
    private const val BOT = 26.0
    private const val LEFT = 104.0 // room for the input labels on the left
    private const val RIGHT = 140.0 // room for the output labels on the right
    private const val NODE_R = 5.0
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

    private const val GENOME_COLS = 56 // weights per row in the heatmap

    // One rendered layer: its activations, its column x, and the y-centre of each node.
    private class Layer(val acts: DoubleArray, val x: Double, val ys: DoubleArray)

    /** The two text readouts the old NET sidebar showed beyond arch/fitness/top-actions. */
    class NetStats(val drivingInput: String, val peakHidden: String)

    /** Compute [NetStats] for [faction]'s [net] from a fresh forward pass (the strongest input + hidden neuron). */
    fun stats(net: Net, faction: Faction): NetStats {
        val trace = net.forwardTraced(Observation.observe(faction))
        val driveIdx = trace.input.indices.maxByOrNull { trace.input[it] } ?: 0
        val driving = "${IN_LABELS.getOrElse(driveIdx) { "f$driveIdx" }} ${pct(trace.input[driveIdx])}"
        return NetStats(driving, peakHidden(trace.hiddens))
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
        val v = hiddens[bestLayer][bestIndex]
        return "L${bestLayer + 1} #$bestIndex ${if (v >= 0) "+" else ""}${(v.asDynamic().toFixed(2) as String)}"
    }

    /** Draw [net]'s live activation diagram for [faction] into a dpr-scaled [ctx] sized [w]×[h] (CSS px). */
    fun paintActivation(ctx: CanvasRenderingContext2D, net: Net, faction: Faction, w: Double, h: Double) {
        val trace = net.forwardTraced(Observation.observe(faction))
        ctx.clearRect(0.0, 0.0, w, h)
        val acts = buildList {
            add(trace.input)
            addAll(trace.hiddens)
            add(trace.output)
        }
        val cols = acts.size
        val layers = acts.indices.map { Layer(acts[it], colX(it, cols, w), layerYs(acts[it].size, h)) }
        captions(ctx, faction, layers)
        for (c in 0 until cols - 1) {
            edges(ctx, faction, layers[c], layers[c + 1]) { from, to -> net.weight(c, from, to) }
        }
        val top = topOutputs(trace.output)
        val lastTransition = cols - 2 // last hidden → output
        litPath(ctx, faction, layers[lastTransition], layers.last(), top.firstOrNull() ?: -1) { hIdx, o ->
            net.weight(lastTransition, hIdx, o)
        }
        val hiddenSigned = net.arch.activation.signed
        layers.forEachIndexed { i, layer -> nodes(ctx, faction, layer, signed = i != 0 && i != cols - 1 && hiddenSigned) }
        inputLabels(ctx, faction, layers.first())
        outputLabels(ctx, faction, layers.last(), top)
    }

    /**
     * Paint [net]'s flat genome as a sign/magnitude heatmap into a dpr-scaled [ctx] (so [w]/[h] are CSS px):
     * [posColor] for positive weights, a cool grey-blue for negative, alpha = magnitude. Reused by the TRAIN
     * tab's champion preview ([TrainerPanel]).
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

    private fun colX(index: Int, cols: Int, w: Double): Double =
        if (cols <= 1) LEFT else LEFT + (w - LEFT - RIGHT) * (index.toDouble() / (cols - 1))

    private fun layerYs(n: Int, h: Double): DoubleArray = DoubleArray(n) { TOP + (h - TOP - BOT) * ((it + 0.5) / n) }

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
        for (hIdx in hidden.acts.indices) max = maxOf(max, abs(hidden.acts[hIdx] * weight(hIdx, topOut)))
        ctx.lineWidth = 1.8
        ctx.strokeStyle = faction.color
        for (hIdx in hidden.acts.indices) {
            val mag = (abs(hidden.acts[hIdx] * weight(hIdx, topOut)) / max).pow(1.2)
            if (mag < 0.15) continue
            ctx.globalAlpha = (0.35 + mag * 0.55).coerceAtMost(0.9)
            edge(ctx, hidden.x, hidden.ys[hIdx], output.x, output.ys[topOut])
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

    private fun pct(value: Double): String = "${(value * 100.0).toInt()}%"
}
