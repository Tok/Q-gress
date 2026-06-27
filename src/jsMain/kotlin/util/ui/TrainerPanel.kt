package util.ui

import World
import agent.Faction
import ai.FactionPolicies
import ai.MatchSetup
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
    private const val PREVIEW_COLOR = "#e8e8e8" // faction-neutral accent for the champion preview / curve

    // Per-hidden-layer width choices (two hidden layers, each picked independently → 4×4 … 32×32).
    private val HIDDEN_WIDTHS = listOf(4, 8, 16, 24, 32)
    private const val DEFAULT_HIDDEN = 16

    private var built = false
    private var running = false
    private var session: Evolution.Session? = null
    private var timeoutId = 0
    private val fitness = mutableListOf<Double>()

    private var popInput: HTMLInputElement? = null
    private var genInput: HTMLInputElement? = null
    private var mutInput: HTMLInputElement? = null
    private var hidden1Select: HTMLSelectElement? = null
    private var hidden2Select: HTMLSelectElement? = null
    private var actSelect: HTMLSelectElement? = null
    private var cleanEvalBox: HTMLInputElement? = null
    private var loadInput: HTMLInputElement? = null
    private var runButton: HTMLButtonElement? = null
    private var statusEl: HTMLElement? = null
    private var previewCanvas: HTMLCanvasElement? = null
    private var previewCaption: HTMLElement? = null
    private var plot: UPlot? = null
    private var progressFill: HTMLElement? = null
    private var progressLabel: HTMLElement? = null
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
        val widthOpts = HIDDEN_WIDTHS.map { it.toString() to it.toString() }
        hidden1Select = selectField(row, "Hidden 1", widthOpts).also { it.value = DEFAULT_HIDDEN.toString() }
        hidden2Select = selectField(row, "Hidden 2", widthOpts).also { it.value = DEFAULT_HIDDEN.toString() }
        actSelect = selectField(row, "Activation", Activation.entries.map { it.name to it.name.lowercase() })
        cleanEvalBox = checkboxField(row, "Clean eval", "anti-runaway mechanics off — a cleaner training gradient")
        val run = button("Train", "trainRun") { onRunClick() }
        runButton = run
        row.appendChild(run)
        statusEl = el("div", "trainStatus")
        row.appendChild(statusEl ?: el("div", "trainStatus"))
        row.appendChild(
            el("div", "trainWarn").also {
                it.textContent = "⚠ Training pauses the live game and runs the CPU flat-out — a big run can take many minutes."
            },
        )
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
        col.appendChild(buildProgress())
        val chart = el("div", "trainChart")
        col.appendChild(chart)
        plot = makePlot(chart)
        return col
    }

    private fun buildProgress(): HTMLElement {
        val wrap = el("div", "trainProgressWrap")
        progressLabel = el("div", "trainProgressLabel").also { it.textContent = "—" }
        wrap.appendChild(progressLabel ?: el("div", "trainProgressLabel"))
        val track = el("div", "trainProgress")
        progressFill = el("div", "trainProgressFill")
        track.appendChild(progressFill ?: el("div", "trainProgressFill"))
        wrap.appendChild(track)
        return wrap
    }

    private fun setProgress(fraction: Double, label: String) {
        progressFill?.style?.width = "${(fraction.coerceIn(0.0, 1.0) * 100.0)}%"
        progressLabel?.textContent = label
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
        actionButtons += button("Download JSON", "trainAction") { downloadChampion() }
        actionButtons.forEach { box.appendChild(it) }
        // "Load JSON" is always usable (no champion needed) — it imports a shared net + makes it the net driver.
        box.appendChild(button("Load JSON", "trainAction") { triggerLoad() })
        box.appendChild(buildLoadInput())
        return box
    }

    private fun buildLoadInput(): HTMLElement {
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = "application/json,.json"
        input.classList.add("invisible")
        input.onchange = {
            (input.files?.item(0))?.let { loadFile(it) }
            input.value = "" // allow re-loading the same file
            null
        }
        loadInput = input
        return input
    }

    private fun triggerLoad() = loadInput?.click()

    // Read a shared genome JSON, validate it (GenomeIO.decode throws on a bad shape), persist it as the net
    // driver's net (survives reload, like Save champion), and install it for both factions on the spot.
    private fun loadFile(file: org.w3c.files.File) {
        val reader = org.w3c.files.FileReader()
        reader.onload = {
            val text = reader.result as? String ?: ""
            runCatching { GenomeIO.decode(text) }
                .onSuccess {
                    NetStore.save(text)
                    Faction.all().forEach { f ->
                        FactionPolicies.set(f, NetPolicy(GenomeIO.decode(text), f))
                        DriverControls.reflect(f, "net")
                    }
                    setStatus("Loaded net from ${file.name} — saved + installed for both factions.")
                }
                .onFailure { setStatus("Couldn't load that file: ${it.message ?: "not a valid net JSON"}") }
            null
        }
        reader.readAsText(file)
    }

    // Offer the current champion as a downloadable .json (Blob → object URL → synthetic click) for sharing.
    private fun downloadChampion() {
        val json = championJson() ?: return
        val url = objectUrlFor(json)
        val a = document.createElement("a") as org.w3c.dom.HTMLAnchorElement
        a.href = url
        a.asDynamic().download = "qgress-net.json"
        a.click()
        revokeObjectUrl(url)
        setStatus("Champion downloaded as qgress-net.json — share it; Load JSON imports it back.")
    }

    @Suppress("UnusedParameter") // referenced inside the js() intrinsic, invisible to detekt
    private fun objectUrlFor(json: String): String =
        js("URL.createObjectURL(new Blob([json], { type: 'application/json' }))") as String

    @Suppress("UnusedParameter") // referenced inside the js() intrinsic, invisible to detekt
    private fun revokeObjectUrl(url: String) {
        js("URL.revokeObjectURL(url)")
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
        setProgress(0.0, "Starting…")
        setStatus("Live game paused — training on the live map.")
        refreshActions()
        scheduleStep()
    }

    private fun scheduleStep() {
        timeoutId = window.setTimeout({ runStep() }, 0)
    }

    // One genome per tick (not a whole generation) so the progress bar + status advance smoothly and the run
    // never looks frozen. A generation completes when stepGenome() returns true → fold its champion into the curve.
    private fun runStep() {
        val current = session ?: return
        if (!running) return
        val generationDone = runCatching { current.stepGenome() }.getOrElse {
            abort(it.message)
            return
        }
        if (generationDone) {
            fitness.add(current.history().last())
            renderFitness()
            renderChampion(current)
        }
        updateProgress(current)
        if (current.done) finish() else scheduleStep()
    }

    private fun updateProgress(current: Evolution.Session) {
        val total = current.config.generations * current.populationSize
        val done = current.generation * current.populationSize + current.evaluatedThisGeneration
        setProgress(
            done.toDouble() / total,
            "Generation ${current.generation + (if (current.done) 0 else 1)} / ${current.config.generations} · genome ${current.evaluatedThisGeneration}/${current.populationSize}",
        )
        setStatus("Training · best ${muLabel(current.bestFitness)} ($done/$total matches)")
    }

    private fun finish() {
        val done = session
        teardown()
        if (done != null) {
            setProgress(1.0, "Done — ${done.generation} generations")
            setStatus("Done · ${done.generation} generations · champion ${muLabel(done.bestFitness)} · live game resumed.")
        }
        refreshActions()
    }

    private fun stop() {
        if (!running) return
        val stopped = session
        teardown()
        if (stopped != null) {
            setProgress(0.0, "Stopped")
            setStatus("Stopped at generation ${stopped.generation} · best ${muLabel(stopped.bestFitness)} · live game resumed.")
        }
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
        val h1 = hidden1Select?.value?.toIntOrNull() ?: DEFAULT_HIDDEN
        val h2 = hidden2Select?.value?.toIntOrNull() ?: DEFAULT_HIDDEN
        val hiddens = listOf(h1, h2)
        return EvolutionConfig(
            populationSize = pop,
            generations = gens,
            elite = (pop / 4).coerceIn(1, pop),
            mutationRate = mut,
            arch = NetArch(hiddens, bias = true, activation = Activation.from(actSelect?.value)),
            matchTicks = MATCH_TICKS,
            matchesPerEval = 1,
            setup = MatchSetup(cleanEval = cleanEvalBox?.checked ?: false),
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
        listOfNotNull(popInput, genInput, mutInput, cleanEvalBox).forEach { it.disabled = running }
        listOfNotNull(hidden1Select, hidden2Select, actSelect).forEach { it.disabled = running }
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
        opts.axes = arrayOf(whiteAxis(), whiteAxis()) // tick labels white so they read on the dark UI
        val series: dynamic = js("({})")
        series.stroke = PREVIEW_COLOR
        series.width = 2
        series.fill = "rgba(120, 170, 255, 0.14)"
        series.points = js("({ show: false })")
        opts.series = arrayOf(js("({})"), series)
        val empty: dynamic = arrayOf(arrayOf<Double>(), arrayOf<Double>())
        return UPlot(opts, empty, target)
    }

    // A uPlot axis with white tick labels + faint grid/ticks — legible on the dark trainer panel.
    private fun whiteAxis(): dynamic {
        val a: dynamic = js("({})")
        a.stroke = "#ffffff" // tick-label + axis colour
        a.grid = js("({ stroke: 'rgba(255,255,255,0.10)', width: 1 })")
        a.ticks = js("({ stroke: 'rgba(255,255,255,0.25)', width: 1 })")
        return a
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

    private fun checkboxField(parent: HTMLElement, label: String, title: String): HTMLInputElement {
        val wrap = el("label", "ladderEntrant").also { it.title = title } // reuse the leaderboard checkbox-row style
        val box = document.createElement("input") as HTMLInputElement
        box.type = "checkbox"
        wrap.appendChild(box)
        wrap.appendChild(el("span", "trainFieldLabel").also { it.textContent = label })
        parent.appendChild(wrap)
        return box
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
}
