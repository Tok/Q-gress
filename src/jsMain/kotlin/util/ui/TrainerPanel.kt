package util.ui

import World
import agent.Faction
import ai.FactionPolicies
import ai.net.Activation
import ai.net.Evolution
import ai.net.EvolutionConfig
import ai.net.GenomeIO
import ai.net.Net
import ai.net.NetArch
import ai.net.NetPolicy
import ai.net.NetStore
import external.UPlot
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import system.HeadlessRun
import kotlin.math.roundToInt

/**
 * The **TRAIN** footer tab (PLAN Phase 6.5) — an in-browser neuro-evolution trainer. Pick a [NetArch] +
 * [EvolutionConfig], hit **Train**, and an [Evolution.Session] runs one generation per `setTimeout(…, 0)`
 * (so the UI never blocks), drawing a live fitness curve and a champion-genome preview. The run is bracketed by
 * [HeadlessRun] (parks the live world + silences FX; the tick loop pauses while it's active), so the headless
 * matches it spins up never disturb the live game — which resumes untouched when the run ends. The winner can be
 * **saved** to [NetStore] or **installed** as either faction's driver.
 *
 * Serious training stays headless (`Evolution.train`); the in-browser defaults are deliberately small so each
 * generation is sub-second on the live map.
 */
object TrainerPanel {
    private const val SEED = 12345 // fixed → a run is reproducible (re-train gives the same champion)
    private const val MATCH_TICKS = 601 // checkpoints at 0/300/600 — short, in-browser-friendly matches
    private const val PREVIEW_W = 540
    private const val PREVIEW_H = 96
    private const val CHART_W = 560
    private const val CHART_H = 220
    private const val PREVIEW_COLOR = "#cfe3ff" // faction-neutral accent for the champion preview / curve

    // Architecture presets offered in the picker (label → hidden-layer widths).
    private val ARCHES = listOf(
        "16 × 16" to listOf(16, 16),
        "16" to listOf(16),
        "24 × 24" to listOf(24, 24),
        "8" to listOf(8),
    )

    private var built = false
    private var running = false
    private var session: Evolution.Session? = null
    private var timeoutId = 0
    private val fitness = mutableListOf<Double>()

    private var popInput: HTMLInputElement? = null
    private var genInput: HTMLInputElement? = null
    private var mutInput: HTMLInputElement? = null
    private var archSelect: HTMLSelectElement? = null
    private var actSelect: HTMLSelectElement? = null
    private var runButton: HTMLButtonElement? = null
    private var statusEl: HTMLElement? = null
    private var previewCanvas: HTMLCanvasElement? = null
    private var previewCaption: HTMLElement? = null
    private var plot: UPlot? = null
    private val actionButtons = mutableListOf<HTMLButtonElement>()

    /** True while a training run is in flight — the tick loop pauses on this so matches don't fight the game. */
    fun isTraining(): Boolean = running

    /** Lazy-build the panel; called each frame from [util.DrawUtil] (paused during a run, which drives its own UI). */
    fun update() {
        ensure()
    }

    private fun ensure(): Boolean {
        if (built) return true
        if (document.body == null) return false
        if (js("typeof uPlot === 'undefined'").unsafeCast<Boolean>()) return false // CDN not ready → retry next frame
        val panel = el("div", "trainPanel")
        panel.appendChild(buildConfigRow())
        panel.appendChild(buildMain())
        Footer.tab("train").appendChild(panel)
        built = true
        refreshActions()
        setStatus("Idle — set the knobs and press Train to evolve a net on the live map.")
        return true
    }

    private fun buildConfigRow(): HTMLElement {
        val row = el("div", "trainConfig")
        popInput = numberField(row, "Population", "10", "1", "2", "40")
        genInput = numberField(row, "Generations", "15", "1", "1", "200")
        mutInput = numberField(row, "Mutation", "0.15", "0.05", "0", "1")
        archSelect = selectField(row, "Architecture", ARCHES.map { it.first to it.first })
        actSelect = selectField(row, "Activation", Activation.entries.map { it.name to it.name.lowercase() })
        val run = button("Train", "trainRun") { onRunClick() }
        runButton = run
        row.appendChild(run)
        statusEl = el("div", "trainStatus")
        row.appendChild(statusEl ?: el("div", "trainStatus"))
        return row
    }

    private fun buildMain(): HTMLElement {
        val main = el("div", "trainMain")
        main.appendChild(buildChartCol())
        main.appendChild(buildChampionCol())
        return main
    }

    private fun buildChartCol(): HTMLElement {
        val col = el("div", "trainCol")
        col.appendChild(el("div", "trainColHead").also { it.textContent = "Fitness — summed MU margin per generation" })
        val chart = el("div", "trainChart")
        col.appendChild(chart)
        plot = makePlot(chart)
        return col
    }

    private fun buildChampionCol(): HTMLElement {
        val col = el("div", "trainCol")
        col.appendChild(el("div", "trainColHead").also { it.textContent = "Champion genome (weights — sign × magnitude)" })
        val canvas = dprCanvas(PREVIEW_W, PREVIEW_H)
        previewCanvas = canvas
        col.appendChild(canvas)
        previewCaption = el("div", "trainCaption").also { it.textContent = "—" }
        col.appendChild(previewCaption ?: el("div", "trainCaption"))
        col.appendChild(buildActions())
        return col
    }

    private fun buildActions(): HTMLElement {
        val box = el("div", "trainActions")
        actionButtons.clear()
        actionButtons += button("Save champion", "trainAction") { saveChampion() }
        actionButtons += button("Install → ${Faction.ENL.abbr}", "trainAction") { install(Faction.ENL) }
        actionButtons += button("Install → ${Faction.RES.abbr}", "trainAction") { install(Faction.RES) }
        actionButtons.forEach { box.appendChild(it) }
        return box
    }

    private fun onRunClick() {
        if (running) stop() else start()
    }

    private fun start() {
        if (running) return
        if (World.grid.isEmpty()) {
            setStatus("Start a game first — training evolves a net on the live map.")
            return
        }
        HeadlessRun.begin() // park the live game + silence FX for the duration (shared with the leaderboard)
        running = true
        fitness.clear()
        plot?.setData(arrayOf(arrayOf<Double>(), arrayOf<Double>()))
        session = Evolution.Session(World.grid, SEED, readConfig()) // opponent = the uniform-slider baseline
        refreshActions()
        scheduleStep()
    }

    private fun scheduleStep() {
        timeoutId = window.setTimeout({ runStep() }, 0)
    }

    private fun runStep() {
        val current = session ?: return
        if (!running) return
        val champion = runCatching { current.step() }.getOrElse {
            abort(it.message)
            return
        }
        fitness.add(champion)
        renderFitness()
        renderChampion(current)
        setStatus("Generation ${current.generation} / ${current.config.generations} · best ${muLabel(current.bestFitness)}")
        if (current.done) finish() else scheduleStep()
    }

    private fun finish() {
        val done = session
        teardown()
        if (done != null) setStatus("Done · ${done.generation} generations · champion ${muLabel(done.bestFitness)}")
        refreshActions()
    }

    private fun stop() {
        if (!running) return
        val stopped = session
        teardown()
        if (stopped != null) setStatus("Stopped at generation ${stopped.generation} · best ${muLabel(stopped.bestFitness)}")
        refreshActions()
    }

    private fun abort(message: String?) {
        teardown()
        setStatus("Training error: ${message ?: "unknown"}")
        refreshActions()
    }

    // Restore the live game and end the run; the session is kept so the champion can still be saved/installed.
    private fun teardown() {
        running = false
        window.clearTimeout(timeoutId)
        HeadlessRun.end() // clear the throwaway match state + restore the live world & renderer
    }

    private fun saveChampion() {
        val json = championJson() ?: return
        NetStore.save(json)
        setStatus("Champion saved — it's now the “Neural net” driver's net (survives a reload).")
    }

    private fun install(faction: Faction) {
        val winner = session ?: return
        FactionPolicies.set(faction, NetPolicy(Net.fromGenome(winner.bestGenome, winner.config.arch), faction))
        DriverControls.reflect(faction, "net") // sync the toolbar picker so it doesn't read stale
        setStatus("Installed champion as the ${faction.abbr} driver (${muLabel(winner.bestFitness)}).")
    }

    private fun championJson(): String? {
        val winner = session ?: return null
        return GenomeIO.encode(winner.bestGenome, winner.config.arch, winner.bestFitness)
    }

    private fun readConfig(): EvolutionConfig {
        val pop = (popInput?.value?.toIntOrNull() ?: 10).coerceIn(2, 40)
        val gens = (genInput?.value?.toIntOrNull() ?: 15).coerceIn(1, 200)
        val mut = (mutInput?.value?.toDoubleOrNull() ?: 0.15).coerceIn(0.0, 1.0)
        val hiddens = ARCHES.firstOrNull { it.first == archSelect?.value }?.second ?: NetArch.DEFAULT_HIDDENS
        return EvolutionConfig(
            populationSize = pop,
            generations = gens,
            elite = (pop / 4).coerceIn(1, pop),
            mutationRate = mut,
            arch = NetArch(hiddens, bias = true, activation = Activation.from(actSelect?.value)),
            matchTicks = MATCH_TICKS,
            matchesPerEval = 1,
        )
    }

    private fun renderFitness() {
        val xs = fitness.indices.map { (it + 1).toDouble() }.toTypedArray()
        val ys = fitness.toTypedArray()
        plot?.setData(arrayOf(xs, ys))
    }

    private fun renderChampion(current: Evolution.Session) {
        val ctx = previewCanvas?.getContext("2d") as? CanvasRenderingContext2D ?: return
        val net = Net.fromGenome(current.bestGenome, current.config.arch)
        NetVizPanel.paintGenome(ctx, net, PREVIEW_W.toDouble(), PREVIEW_H.toDouble(), PREVIEW_COLOR)
        previewCaption?.textContent = "${current.config.arch.label()} · ${muLabel(current.bestFitness)}"
    }

    // Save/Install are usable once a generation has produced a champion and the run has finished (restored).
    private fun refreshActions() {
        runButton?.textContent = if (running) "Stop" else "Train"
        val ready = session != null && fitness.isNotEmpty() && !running
        actionButtons.forEach { it.disabled = !ready }
        listOfNotNull(popInput, genInput, mutInput).forEach { it.disabled = running }
        listOfNotNull(archSelect, actSelect).forEach { it.disabled = running }
    }

    private fun setStatus(text: String) {
        statusEl?.textContent = text
    }

    private fun muLabel(value: Double): String {
        if (value == Double.NEGATIVE_INFINITY) return "—"
        val rounded = value.roundToInt()
        return "${if (rounded >= 0) "+" else ""}$rounded MU"
    }

    private fun makePlot(target: HTMLElement): UPlot {
        val opts: dynamic = js("({})")
        opts.width = CHART_W
        opts.height = CHART_H
        opts.cursor = js("({ show: false })")
        opts.legend = js("({ show: false })")
        opts.scales = js("({ x: { time: false } })")
        val series: dynamic = js("({})")
        series.stroke = PREVIEW_COLOR
        series.width = 2
        series.fill = "rgba(120, 170, 255, 0.14)"
        series.points = js("({ show: false })")
        opts.series = arrayOf(js("({})"), series)
        val empty: dynamic = arrayOf(arrayOf<Double>(), arrayOf<Double>())
        return UPlot(opts, empty, target)
    }

    private fun numberField(parent: HTMLElement, label: String, value: String, step: String, min: String, max: String): HTMLInputElement {
        val wrap = el("div", "trainField")
        wrap.appendChild(el("span", "trainFieldLabel").also { it.textContent = label })
        val input = document.createElement("input") as HTMLInputElement
        input.type = "number"
        input.value = value
        input.step = step
        input.min = min
        input.max = max
        input.className = "trainInput"
        wrap.appendChild(input)
        parent.appendChild(wrap)
        return input
    }

    private fun selectField(parent: HTMLElement, label: String, options: List<Pair<String, String>>): HTMLSelectElement {
        val wrap = el("div", "trainField")
        wrap.appendChild(el("span", "trainFieldLabel").also { it.textContent = label })
        val select = document.createElement("select") as HTMLSelectElement
        select.className = "trainSelect"
        options.forEach { (value, text) ->
            val option = document.createElement("option") as HTMLOptionElement
            option.value = value
            option.textContent = text
            select.appendChild(option)
        }
        wrap.appendChild(select)
        parent.appendChild(wrap)
        return select
    }

    private fun button(label: String, cls: String, onClick: () -> Unit): HTMLButtonElement {
        val btn = document.createElement("button") as HTMLButtonElement
        btn.className = cls
        btn.textContent = label
        btn.onclick = {
            onClick()
            null
        }
        return btn
    }

    // A device-pixel-resolution canvas → crisp fills on HiDPI (CSS size w×h, backing store ×dpr).
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

    private fun el(tag: String, cls: String): HTMLElement {
        val e = document.createElement(tag) as HTMLElement
        e.className = cls
        return e
    }
}
