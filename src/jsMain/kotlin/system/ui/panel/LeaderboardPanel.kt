package system.ui.panel

import World
import agent.Faction
import ai.Driver
import ai.FactionPolicy
import ai.HeuristicPolicy
import ai.Tournament
import ai.net.NetPolicy
import ai.net.NetStore
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import system.HeadlessRun
import system.ui.Footer
import system.ui.el
import kotlin.math.roundToInt

/**
 * The **leaderboard** (PLAN Phase 6) — a second section of the TRAIN tab that ranks AI drivers head-to-head on
 * the live map. Pick which drivers enter, hit **Run ladder**, and a [Tournament.Session] plays a round-robin
 * one match per `setTimeout` (so the UI never blocks), showing the live `Standing` table (W-L-T + avg MU
 * margin). It reuses the trainer's [HeadlessRun] harness: the live game is parked + FX silenced for the run and
 * restored untouched afterwards (the tick loop pauses meanwhile).
 */
object LeaderboardPanel {
    private const val MATCH_TICKS = 601 // short, in-browser-friendly matches (3 checkpoints: 0/300/600)
    private val SEEDS = listOf(1, 2, 3)

    // A possible entrant — a named driver factory + an on/off toggle.
    private class Entrant(val name: String, val make: (Faction) -> FactionPolicy?) {
        var enabled = true
    }

    private val ENTRANTS = listOf(
        Entrant("Baseline") { null }, // the uniform-slider default
        Entrant("Heuristic") { HeuristicPolicy(it) },
        Entrant("Neural net") { NetPolicy(NetStore.loadNet(), it) },
    )

    private var built = false
    private var running = false
    private var session: Tournament.Session? = null
    private var timeoutId = 0
    private var runButton: HTMLButtonElement? = null
    private var statusEl: HTMLElement? = null
    private var tbody: HTMLElement? = null

    fun update() {
        ensure()
    }

    private fun ensure(): Boolean {
        if (built) return true
        if (document.body == null) return false
        val section = el("div", "trainPanel")
        section.appendChild(el("div", "trainColHead").also { it.textContent = "Leaderboard — rank the drivers head-to-head" })
        section.appendChild(buildConfigRow())
        section.appendChild(buildTable())
        Footer.tab("train").appendChild(section)
        built = true
        setStatus("Pick the drivers and press Run ladder (a round-robin on the live map).")
        return true
    }

    private fun buildConfigRow(): HTMLElement {
        val row = el("div", "trainConfig")
        ENTRANTS.forEach { entrant ->
            val wrap = el("label", "ladderEntrant")
            val box = document.createElement("input") as HTMLInputElement
            box.type = "checkbox"
            box.checked = entrant.enabled
            box.onchange = {
                entrant.enabled = box.checked
                null
            }
            wrap.appendChild(box)
            wrap.appendChild(el("span", "").also { it.textContent = entrant.name })
            row.appendChild(wrap)
        }
        val run = button("Run ladder", "trainRun") { onRunClick() }
        runButton = run
        row.appendChild(run)
        statusEl = el("div", "trainStatus")
        row.appendChild(statusEl ?: el("div", "trainStatus"))
        return row
    }

    private fun buildTable(): HTMLElement {
        val table = el("table", "ladderTable")
        val head = el("tr", "")
        listOf("Driver", "Matches", "W", "L", "T", "Avg MU").forEach { h ->
            head.appendChild(el("th", "ladderHeadCell").also { it.textContent = h })
        }
        val thead = el("thead", "")
        thead.appendChild(head)
        table.appendChild(thead)
        val body = el("tbody", "")
        table.appendChild(body)
        tbody = body
        return table
    }

    private fun onRunClick() {
        if (running) stop() else start()
    }

    private fun start() {
        if (running) return
        if (World.grid.isEmpty()) {
            setStatus("Start a game first — the ladder runs on the live map.")
            return
        }
        val drivers = ENTRANTS.filter { it.enabled }.map { Driver(it.name, it.make) }
        if (drivers.size < 2) {
            setStatus("Pick at least two drivers to rank them.")
            return
        }
        HeadlessRun.begin()
        running = true
        session = Tournament.Session(World.grid, drivers, SEEDS, MATCH_TICKS)
        refreshButton()
        scheduleStep()
    }

    private fun scheduleStep() {
        timeoutId = window.setTimeout({ runStep() }, 0)
    }

    private fun runStep() {
        val current = session ?: return
        if (!running) return
        val outcome = runCatching { current.step() }
        if (outcome.isFailure) {
            teardown()
            setStatus("Ladder error: ${outcome.exceptionOrNull()?.message ?: "unknown"}")
            refreshButton()
            return
        }
        renderTable(current)
        setStatus("Match ${current.played} / ${current.total}…")
        if (current.done) finish() else scheduleStep()
    }

    private fun finish() {
        val done = session
        teardown()
        renderTable(done)
        setStatus("Done — ${done?.total ?: 0} matches over ${SEEDS.size} seeds.")
        refreshButton()
    }

    private fun stop() {
        if (!running) return
        teardown()
        setStatus("Stopped.")
        refreshButton()
    }

    private fun teardown() {
        running = false
        window.clearTimeout(timeoutId)
        HeadlessRun.end()
    }

    private fun renderTable(current: Tournament.Session?) {
        val body = tbody ?: return
        body.textContent = ""
        current?.standings()?.forEach { s ->
            val row = el("tr", "ladderRow")
            row.appendChild(cell(s.name, "ladderName"))
            row.appendChild(cell(s.matches.toString(), "ladderNum"))
            row.appendChild(cell(s.wins.toString(), "ladderNum"))
            row.appendChild(cell(s.losses.toString(), "ladderNum"))
            row.appendChild(cell(s.ties.toString(), "ladderNum"))
            row.appendChild(cell(muLabel(s.avgMargin()), "ladderNum"))
            body.appendChild(row)
        }
    }

    private fun refreshButton() {
        runButton?.textContent = if (running) "Stop" else "Run ladder"
    }

    private fun setStatus(text: String) {
        statusEl?.textContent = text
    }

    private fun muLabel(value: Double): String {
        val rounded = value.roundToInt()
        return "${if (rounded >= 0) "+" else ""}$rounded"
    }

    private fun cell(text: String, cls: String): HTMLElement = el("td", cls).also { it.textContent = text }

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
}
