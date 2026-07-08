package system.ui.panel

import World
import agent.Faction
import ai.FactionPolicies
import ai.MatchSetup
import ai.SimRunner
import ai.net.Activation
import ai.net.ChampionLibrary
import ai.net.Evolution
import ai.net.EvolutionConfig
import ai.net.GenomeIO
import ai.net.Net
import ai.net.NetArch
import ai.net.NetPolicy
import ai.net.NetStore
import config.Config
import external.UPlot
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import system.HeadlessRun
import system.ui.DriverControls
import system.ui.GameLoop
import system.ui.el
import kotlin.math.roundToInt

/**
 * The **Train NN** screen (PLAN Phase 6.5) — an in-browser neuro-evolution trainer, opened from the MENU's
 * "Train NN" entry / the BRAINS-tab link (not a footer tab) as its own full-screen overlay with the live game
 * paused behind it. Pick ONE architecture, hit **Train**, and an [Evolution.Session] evolves a challenger
 * **against that arch's current champion** (NN-vs-NN, like `train-champs.sh` — the fitness *is* the margin over
 * the champion, so a positive result means the challenger is better). One generation runs per `setTimeout(…, 0)`
 * (so the UI never blocks), drawing a live fitness curve + a genome preview. The run is bracketed by
 * [HeadlessRun] (parks the live world + silences FX; the game is paused while the screen is up), so the headless
 * matches never disturb the live game — which resumes untouched on close. A better challenger can be **saved**
 * to [NetStore], **installed** as either faction's driver, or **downloaded** to share.
 *
 * This is for training + comparing ONE candidate interactively (~10 min at the defaults). Each match is a full
 * scoring cycle ([Config.ticksPerScoringCycle], ~35 checkpoints) — the fitness goal is to win the cycle, so it
 * can't be assessed in less. The full multi-arch sweep belongs headless (`scripts/bake-champs.sh` / `train-champs.sh`).
 */
object TrainerPanel {
    private const val SEED = 12345 // fixed → a run is reproducible (re-train gives the same champion)

    // A full SCORING cycle per match (~35 checkpoints). The fitness goal is "win the cycle" — lead the most
    // checkpoints — which can't be assessed in less than a whole cycle. The old 601 ticks (3 checkpoints) never
    // gave fields time to form on the live map, so MU stayed 0 and every match tied.
    private val MATCH_TICKS = Config.ticksPerScoringCycle
    private const val PREVIEW_W = 540
    private const val PREVIEW_H = 96
    private const val CHART_W = 560
    private const val CHART_H = 220
    private const val PREVIEW_COLOR = "#e8e8e8" // faction-neutral accent for the champion preview / curve

    // Per-hidden-layer width choices (two hidden layers, each picked independently → 4×4 … 32×32).
    private val HIDDEN_WIDTHS = listOf(4, 8, 16, 24, 32)
    private const val DEFAULT_HIDDEN = 16

    private const val DECIDER_SEEDS = 3 // unseen-seed held-out matches (× both colours) to decide vs the opponent
    private const val DECIDER_SEED_BASE = 90_000 // far from the training SEED so the decider is genuinely held-out

    private var built = false
    private var running = false
    private var session: Evolution.Session? = null
    private var timeoutId = 0
    private val fitness = mutableListOf<Double>()

    // Opponent the challenger trains + is decided against: a loaded net if set, else the candidate arch's champion.
    private var opponentNet: Net? = null
    private var opponentLabel: HTMLElement? = null
    private var oppLoadInput: HTMLInputElement? = null
    private var runOpponent: Net? = null // the opponent actually used for the in-flight run (for the decider)
    private var runSetup: MatchSetup = MatchSetup()

    // Held-out decider phase (runs after training): best challenger vs the opponent over unseen seeds, both colours.
    private var deciding = false
    private var deciderMatch = 0
    private var deciderMarginSum = 0.0
    private var deciderCand: Net? = null
    private var deciderOpp: Net? = null

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
    private var activationCanvas: HTMLCanvasElement? = null
    private var previewCaption: HTMLElement? = null
    private var plot: UPlot? = null
    private var progressFill: HTMLElement? = null
    private var progressLabel: HTMLElement? = null
    private val actionButtons = mutableListOf<HTMLButtonElement>()
    private var overlay: HTMLElement? = null

    /** True while a training run is in flight — the tick loop pauses on this so matches don't fight the game. */
    fun isTraining(): Boolean = running

    /** Open the trainer as its own full-screen screen (from the MENU's TRAIN entry / the BRAINS tab link),
     *  pausing the live game behind it. Builds lazily on first open. */
    fun open() {
        if (!ensure()) return // uPlot CDN not ready yet — try again on the next click
        overlay?.classList?.remove("invisible")
        GameLoop.pauseForEval() // freeze the live game while the trainer screen is up
        renderArchChampion() // show the selected arch's current champion (what a challenger will face) up front
    }

    /** Close the trainer screen and resume the game (stopping any run in progress first). */
    fun close() {
        if (running) stop() // ends the run + restores the live world via HeadlessRun.end()
        overlay?.classList?.add("invisible")
        GameLoop.resumeAfterEval()
    }

    private fun ensure(): Boolean {
        if (built) return true
        if (document.body == null) return false
        if (js("typeof uPlot === 'undefined'").unsafeCast<Boolean>()) return false // CDN not ready → retry next open
        val panel = el("div", "trainPanel")
        panel.appendChild(buildConfigRow())
        panel.appendChild(buildMain())
        val screen = el("div", "trainScreen invisible")
        val glass = el("div", "trainScreenPanel")
        glass.appendChild(buildScreenHeader())
        glass.appendChild(panel)
        LeaderboardPanel.buildInto(glass) // the driver ladder shares the training screen (was the same TRAIN tab)
        glass.appendChild(buildScreenFooter())
        screen.appendChild(glass)
        document.body?.appendChild(screen)
        overlay = screen
        built = true
        refreshActions()
        setStatus("Idle — pick an architecture and press Train to evolve a challenger against that arch's current champion.")
        return true
    }

    // The screen's header: the TRAIN title + a close (×) that resumes the game.
    private fun buildScreenHeader(): HTMLElement {
        val head = el("div", "trainScreenHead")
        head.appendChild(el("span", "trainScreenTitle").also { it.textContent = "Train NN" })
        val closeBtn = el("button", "trainScreenClose") as HTMLButtonElement
        closeBtn.type = "button"
        closeBtn.textContent = "×"
        closeBtn.title = "Close (resumes the game)"
        closeBtn.onclick = {
            close()
            null
        }
        head.appendChild(closeBtn)
        return head
    }

    // Footer: point at the headless BATCH scripts (this screen trains one arch; the full 25-arch sweep is a
    // multi-hour CLI run) + a link to the committed champion library on GitHub.
    private fun buildScreenFooter(): HTMLElement {
        val foot = el("div", "trainScreenFoot")
        foot.appendChild(
            el("span", "").also {
                it.textContent = "Full 25-architecture training runs headless — scripts/bake-champs.sh (vs heuristics) " +
                    "then scripts/train-champs.sh (NN-vs-NN). Bundled champions: "
            },
        )
        val link = el("a", "trainScreenLink") as org.w3c.dom.HTMLAnchorElement
        link.href = "https://github.com/Tok/Q-gress/tree/main/src/jsMain/resources/champions"
        link.target = "_blank"
        link.rel = "noopener"
        link.textContent = "resources/champions ↗"
        foot.appendChild(link)
        return foot
    }

    private fun buildConfigRow(): HTMLElement {
        val row = el("div", "trainConfig")
        popInput = TrainerWidgets.numberField(row, "Population", "10", "1", "2", "40")
        genInput = TrainerWidgets.numberField(row, "Generations", "15", "1", "1", "200")
        mutInput = TrainerWidgets.numberField(row, "Mutation", "0.15", "0.05", "0", "1")
        val widthOpts = HIDDEN_WIDTHS.map { it.toString() to it.toString() }
        hidden1Select = TrainerWidgets.selectField(row, "Hidden 1", widthOpts).also {
            it.value = DEFAULT_HIDDEN.toString()
            it.onchange = {
                renderArchChampion()
                null
            } // repaint the viz for the newly-selected arch's champion
        }
        hidden2Select = TrainerWidgets.selectField(row, "Hidden 2", widthOpts).also {
            it.value = DEFAULT_HIDDEN.toString()
            it.onchange = {
                renderArchChampion()
                null
            }
        }
        actSelect = TrainerWidgets.selectField(row, "Activation", Activation.entries.map { it.name to it.name.lowercase() })
        cleanEvalBox = TrainerWidgets.checkboxField(row, "Clean eval", "anti-runaway mechanics off — a cleaner training gradient")
        buildOpponentControl(row)
        val run = TrainerWidgets.button("Train", "trainRun") { onRunClick() }
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

    // The opponent picker: the challenger trains + is decided against the candidate arch's current champion by
    // default, or any net loaded from JSON (NN-vs-NN with an arbitrary opponent). Reset (↺) reverts to the champion.
    private fun buildOpponentControl(row: HTMLElement) {
        val wrap = el("div", "trainField")
        wrap.appendChild(el("span", "trainFieldLabel").also { it.textContent = "Opponent" })
        val line = el("span", "trainOpponentLine")
        opponentLabel = el("span", "trainOpponentLabel").also { it.textContent = "current champion" }
        val load = el("button", "trainMiniBtn") as HTMLButtonElement
        load.type = "button"
        load.textContent = "Load…"
        load.title = "Train against a net loaded from JSON instead of the champion"
        load.onclick = {
            oppLoadInput?.click()
            null
        }
        val reset = el("button", "trainMiniBtn") as HTMLButtonElement
        reset.type = "button"
        reset.textContent = "↺"
        reset.title = "Reset the opponent to the selected arch's current champion"
        reset.onclick = {
            opponentNet = null
            opponentLabel?.textContent = "current champion"
            null
        }
        line.appendChild(opponentLabel ?: el("span", ""))
        line.appendChild(load)
        line.appendChild(reset)
        line.appendChild(buildOpponentInput())
        wrap.appendChild(line)
        row.appendChild(wrap)
    }

    private fun buildOpponentInput(): HTMLElement {
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = "application/json,.json"
        input.className = "invisible"
        input.onchange = {
            (input.files?.item(0))?.let { readOpponent(it) }
            input.value = ""
            null
        }
        oppLoadInput = input
        return input
    }

    private fun readOpponent(file: org.w3c.files.File) {
        val reader = org.w3c.files.FileReader()
        reader.onload = {
            val text = reader.result as? String ?: ""
            runCatching { GenomeIO.decode(text) }
                .onSuccess { net ->
                    opponentNet = net
                    opponentLabel?.textContent = "loaded ${archName(net.arch)}"
                    setStatus("Opponent set to the loaded ${archName(net.arch)} net — Train to evolve a challenger against it.")
                }
                .onFailure { setStatus("Couldn't load that opponent: ${it.message ?: "not a valid net JSON"}") }
            null
        }
        reader.readAsText(file)
    }

    private fun buildMain(): HTMLElement {
        val main = el("div", "trainMain")
        main.appendChild(buildChartCol())
        main.appendChild(buildChampionCol())
        return main
    }

    private fun buildChartCol(): HTMLElement {
        val col = el("div", "trainCol")
        col.appendChild(el("div", "trainColHead").also { it.textContent = "Fitness — checkpoints led vs the opponent, per generation" })
        col.appendChild(buildProgress())
        val chart = el("div", "trainChart")
        col.appendChild(chart)
        plot = TrainerWidgets.makePlot(chart, CHART_W, CHART_H, PREVIEW_COLOR)
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
        // Live NN activation diagram (same viz as the BRAINS tab): input → hidden → output with weighted edges,
        // the lit decision path, node activations, and driving-input / top-output labels — the net "thinking" on
        // the (paused) live game. Painted on open (current champion) + after each run (the challenger).
        col.appendChild(el("div", "trainColHead").also { it.textContent = "Net activation on the live game (inputs → hidden → actions)" })
        val act = TrainerWidgets.dprCanvas(NetVizPanel.CW, NetVizPanel.CH)
        activationCanvas = act
        col.appendChild(act)
        col.appendChild(el("div", "trainColHead").also { it.textContent = "Genome (all weights — sign × magnitude)" })
        val canvas = TrainerWidgets.dprCanvas(PREVIEW_W, PREVIEW_H)
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
        actionButtons += TrainerWidgets.button("Save champion", "trainAction") { saveChampion() }
        actionButtons += TrainerWidgets.button("Install → ${Faction.ENL.abbr}", "trainAction") { install(Faction.ENL) }
        actionButtons += TrainerWidgets.button("Install → ${Faction.RES.abbr}", "trainAction") { install(Faction.RES) }
        actionButtons += TrainerWidgets.button("Download JSON", "trainAction") { downloadChampion() }
        actionButtons.forEach { box.appendChild(it) }
        // "Load JSON" is always usable (no champion needed) — it imports a shared champion, installs it as its
        // arch's champion (persisted), and makes it the net driver.
        box.appendChild(TrainerWidgets.button("Load JSON", "trainAction") { triggerLoad() })
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

    // Read a shared champion JSON, validate it (GenomeIO.decode throws on a bad shape / wrong net I/O), register
    // it as its architecture's champion in the library (persisted), save it as the net driver's net, and install
    // it for both factions on the spot.
    private fun loadFile(file: org.w3c.files.File) {
        val reader = org.w3c.files.FileReader()
        reader.onload = {
            val text = reader.result as? String ?: ""
            runCatching { GenomeIO.decode(text) }
                .onSuccess {
                    // register as its arch's champion (persisted), tagged with the filename + load time (provenance)
                    ChampionLibrary.installChampion(text, file.name, kotlin.js.Date().toLocaleString())
                    NetStore.save(text)
                    Faction.all().forEach { f ->
                        FactionPolicies.set(f, NetPolicy(GenomeIO.decode(text), f))
                        DriverControls.reflect(f, "net")
                    }
                    setStatus("Loaded champion from ${file.name} — installed as its arch's champion + the net driver.")
                }
                .onFailure { setStatus("Couldn't load that file: ${it.message ?: "not a valid champion JSON"}") }
            null
        }
        reader.readAsText(file)
    }

    // Offer the current trained champion as a downloadable .json (Blob → object URL → synthetic click) for
    // sharing. The filename carries the arch + a timestamp so a UI-baked champion is unique and self-describing
    // (vs the dev-default per-arch files, which are plain `<arch>.json`).
    private fun downloadChampion() {
        val winner = session ?: return
        val json = championJson() ?: return
        val name = "qgress-${winner.config.arch.hiddens.joinToString("x")}-${fileTimestamp()}.json"
        val url = objectUrlFor(json)
        val a = document.createElement("a") as org.w3c.dom.HTMLAnchorElement
        a.href = url
        a.asDynamic().download = name
        a.click()
        revokeObjectUrl(url)
        setStatus("Champion downloaded as $name — share it; Load JSON installs it as that arch's champion.")
    }

    // yyyymmdd-hhmmss (local) — a filename-safe stamp so each UI-baked download is unique.
    private fun fileTimestamp(): String {
        val d = kotlin.js.Date()
        fun p(n: Int) = n.toString().padStart(2, '0')
        return "${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}-${p(d.getHours())}${p(d.getMinutes())}${p(d.getSeconds())}"
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
        val config = readConfig()
        // Train the candidate AGAINST an opponent NET (NN-vs-NN, like train-champs.sh): the loaded opponent if
        // set, else that arch's current champion. The fitness is then the margin over the opponent (positive =
        // the challenger is better) — the in-game "is my candidate better?" comparison, confirmed by the decider.
        val opponent = opponentNet ?: runCatching { GenomeIO.decode(ChampionLibrary.jsonFor(config.arch)) }.getOrNull()
        runOpponent = opponent
        runSetup = config.setup
        HeadlessRun.begin() // park the live game + silence FX for the duration (shared with the leaderboard)
        running = true
        fitness.clear()
        plot?.setData(arrayOf(arrayOf<Double>(), arrayOf<Double>()))
        session = if (opponent != null) {
            Evolution.Session(World.grid, SEED, config) { NetPolicy(opponent, Faction.RES) }
        } else {
            Evolution.Session(World.grid, SEED, config) // no opponent (shouldn't happen) → the uniform-slider baseline
        }
        setProgress(0.0, "Starting…")
        val oppName = opponentNet?.let { "the loaded ${archName(it.arch)} net" }
            ?: ChampionLibrary.fitnessFor(config.arch)
                ?.let { "the current ${archName(config.arch)} champion (it leads the baseline by ${muLabel(it)})" }
            ?: "the current champion"
        setStatus("Training a ${archName(config.arch)} challenger vs $oppName — live game paused.")
        refreshActions()
        scheduleStep()
    }

    private fun archName(arch: NetArch): String = arch.hiddens.joinToString("×")

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
        setStatus("Training · challenger ${muLabel(current.bestFitness)} vs opponent ($done/$total matches)")
    }

    // Training finished → hand off to the held-out decider (best challenger vs the opponent over UNSEEN seeds),
    // keeping the live game parked. If there was no opponent, just report and restore.
    private fun finish() {
        val done = session
        val opp = runOpponent
        if (done == null || opp == null) {
            teardown()
            done?.let {
                setStatus("Done · ${it.generation} generations · challenger ${muLabel(it.bestFitness)} · live game resumed.")
                renderActivation(Net.fromGenome(it.bestGenome, it.config.arch))
            }
            setProgress(1.0, "Done")
            refreshActions()
            return
        }
        startDecider(Net.fromGenome(done.bestGenome, done.config.arch), opp)
    }

    private fun startDecider(cand: Net, opp: Net) {
        deciding = true
        deciderCand = cand
        deciderOpp = opp
        deciderMatch = 0
        deciderMarginSum = 0.0
        setProgress(0.0, "Deciding…")
        setStatus("Training done — held-out decider vs the opponent over $DECIDER_SEEDS unseen cycles (both colours)…")
        timeoutId = window.setTimeout({ deciderStep() }, 0)
    }

    // One held-out match per tick (~2.8 s each) so the UI never freezes for long: the challenger plays the
    // opponent on an unseen seed, alternating colours, and we sum the challenger's net checkpoint margin.
    private fun deciderStep() {
        if (!running) return
        val cand = deciderCand
        val opp = deciderOpp
        if (cand == null || opp == null) {
            finishDecider()
            return
        }
        val total = DECIDER_SEEDS * 2
        val seed = DECIDER_SEED_BASE + deciderMatch / 2
        val candIsEnl = deciderMatch % 2 == 0
        val result = runCatching {
            if (candIsEnl) {
                SimRunner.runMatch(World.grid, seed, MATCH_TICKS, runSetup, NetPolicy(cand, Faction.ENL), NetPolicy(opp, Faction.RES))
            } else {
                SimRunner.runMatch(World.grid, seed, MATCH_TICKS, runSetup, NetPolicy(opp, Faction.ENL), NetPolicy(cand, Faction.RES))
            }
        }.getOrElse {
            abort(it.message)
            return
        }
        deciderMarginSum += result.checkpointWinMargin(if (candIsEnl) Faction.ENL else Faction.RES)
        SimRunner.reset()
        deciderMatch++
        setProgress(deciderMatch.toDouble() / total, "Deciding…")
        setStatus(
            "Held-out decider · match $deciderMatch/$total · challenger leads by ${signedInt(
                deciderMarginSum,
            )} checkpoints (unseen seeds)…",
        )
        if (deciderMatch >= total) finishDecider() else timeoutId = window.setTimeout({ deciderStep() }, 0)
    }

    private fun finishDecider() {
        val done = session
        val margin = deciderMarginSum
        teardown() // HeadlessRun.end() → restores the live world, clears the decider flag
        setProgress(1.0, "Done")
        val verdict = when {
            margin > 0 -> "BEATS"
            margin < 0 -> "does NOT beat"
            else -> "ties"
        }
        val gens = done?.generation ?: 0
        setStatus(
            "Done · $gens generations · held-out decider: the challenger $verdict the opponent by " +
                "${signedInt(margin)} checkpoints over ${DECIDER_SEEDS * 2} unseen matches — save / install / download if better.",
        )
        deciderCand?.let { renderActivation(it) } // show the trained challenger "thinking" on the restored game
        refreshActions()
    }

    private fun signedInt(value: Double): String {
        val n = value.roundToInt()
        return if (n >= 0) "+$n" else "$n"
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
        deciding = false
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
        previewCaption?.textContent = "${current.config.arch.label()} · ${muLabel(current.bestFitness)} vs opponent"
    }

    // Show the current CHAMPION for the arch selected in the Hidden-1/Hidden-2 dropdowns (activation + genome +
    // caption) — what a challenger will be trained against. Repainted when either dropdown changes; skipped mid-run
    // (the diagram then shows the live challenger). Answers "what's the viz showing": the selected arch's champion.
    private fun renderArchChampion() {
        if (running) return
        val h1 = hidden1Select?.value?.toIntOrNull() ?: DEFAULT_HIDDEN
        val h2 = hidden2Select?.value?.toIntOrNull() ?: DEFAULT_HIDDEN
        val arch = NetArch(listOf(h1, h2))
        runCatching {
            val net = GenomeIO.decode(ChampionLibrary.jsonFor(arch))
            renderActivation(net)
            (previewCanvas?.getContext("2d") as? CanvasRenderingContext2D)?.let {
                NetVizPanel.paintGenome(it, net, PREVIEW_W.toDouble(), PREVIEW_H.toDouble(), PREVIEW_COLOR)
            }
            val fit = ChampionLibrary.fitnessFor(arch)?.let { muLabel(it) } ?: "—"
            previewCaption?.textContent = "current ${arch.hiddens.joinToString("×")} champion · $fit"
        }
    }

    // Paint [net]'s activation diagram on the current (paused / restored) live game. No-op if no game is running.
    private fun renderActivation(net: Net) {
        if (World.grid.isEmpty()) return
        val ctx = activationCanvas?.getContext("2d") as? CanvasRenderingContext2D ?: return
        NetVizPanel.paintActivation(ctx, net, Faction.ENL, NetVizPanel.CW.toDouble(), NetVizPanel.CH.toDouble())
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
}
