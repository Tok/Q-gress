package system.ui

import agent.Faction
import agent.qvalue.QActions
import agent.qvalue.QDestinations
import agent.qvalue.QValue
import ai.FactionPolicies
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement

/**
 * The merged behaviour-tuning panel (TUNE tab) — one flat list of all q-value sliders: the Actions
 * first, a divider, then the Destinations. Replaces the two separate slider panes.
 *
 * The DOM `<input>` per (q-value × faction) is still the value store — [agent.action.ActionSelector.q]
 * reads it by id (`<id>Slider<NickName>`), so we keep those exact ids and create both factions' inputs
 * (the user's interactive/visible, the enemy's hidden but present as its value source).
 *
 * Two display modes (the read-only one is for future agent-vs-agent matches where the player can't
 * tune): [Mode.INTERACTIVE] shows the draggable sliders; [Mode.READONLY] hides them (still the value
 * source) and shows 0–1 progress bars that mirror the values. [refresh] re-syncs the bars — the entry
 * point for an AI driver that writes the inputs each tick.
 */
object TuningPanel {
    enum class Mode { INTERACTIVE, READONLY }

    private class Row(
        val qValue: QValue,
        val input: HTMLInputElement,
        val valueLabel: HTMLElement,
        val bar: HTMLElement,
        val fill: HTMLElement,
        val lock: HTMLElement,
        var locked: Boolean = false,
    )

    private var built = false
    private var mode = Mode.INTERACTIVE
    private var userFaction = Faction.ENL
    private var aiDriven = false // is the displayed faction currently driven by an AI policy?
    private val rows = mutableListOf<Row>()

    /** Build the panel into the dock's TUNE tab (idempotent). [readOnly] picks the initial mode. */
    fun build(fact: Faction, readOnly: Boolean) {
        if (built) return
        if (document.body == null) return
        built = true
        userFaction = fact
        mode = if (readOnly) Mode.READONLY else Mode.INTERACTIVE
        val list = el("div", "tuneList")
        QActions.values().forEach { list.appendChild(buildRow(it)) }
        list.appendChild(el("div", "tuneDivider"))
        QDestinations.values().forEach { list.appendChild(buildRow(it)) }
        Hud.left().appendChild(list)
        applyMode()
    }

    fun setMode(readOnly: Boolean) {
        mode = if (readOnly) Mode.READONLY else Mode.INTERACTIVE
        applyMode()
    }

    /**
     * Per-frame sync (called from [util.DrawUtil.redrawUserInterface]). If an **AI policy** drives the
     * displayed faction, mirror its current vector onto the inputs and auto-move the read-only bars — the
     * player watches the sliders move under AI control. A **locked** slider (the padlock toggle, AI-driven
     * only) stays interactive and the AI doesn't overwrite it. Under manual control this is just the read-only-bar
     * resync (a no-op in interactive mode).
     */
    fun refresh() {
        if (!built) return
        val vector = FactionPolicies.of(userFaction).currentVector()
        val nowDriven = vector != null
        if (nowDriven != aiDriven) {
            aiDriven = nowDriven
            rows.forEach { it.locked = FactionPolicies.lockedValue(userFaction, it.qValue) != null } // resync on driver change
            applyMode()
        }
        if (vector != null) {
            rows.forEach { r ->
                if (r.locked) return@forEach // player owns this slider — leave it
                r.input.valueAsNumber = vector[r.qValue]
                r.valueLabel.textContent = display(r.input.value)
                r.fill.style.width = pct(r.input)
            }
        } else if (mode == Mode.READONLY) {
            rows.forEach { it.fill.style.width = pct(it.input) }
        }
    }

    private fun toggleLock(row: Row) {
        row.locked = !row.locked
        if (row.locked) {
            FactionPolicies.lock(userFaction, row.qValue, row.input.valueAsNumber)
        } else {
            FactionPolicies.unlock(userFaction, row.qValue)
        }
        applyMode()
    }

    // Stable order both factions' values are serialized in (Actions then Destinations) — the share link.
    private fun orderedQValues(): List<QValue> = QActions.values().toList() + QDestinations.values().toList()

    private fun sliderInput(qValue: QValue, faction: Faction): HTMLInputElement? = document.getElementById(
        qValue.sliderId + faction.nickName,
    ) as? HTMLInputElement

    /** Serialize both factions' tuning as "enl,…|res,…" for the share link (empty until the panel is built). */
    fun exportTuning(): String {
        if (!built) return ""
        val qs = orderedQValues()
        fun csv(f: Faction) = qs.joinToString(",") { sliderInput(it, f)?.value ?: "0.10" }
        return "${csv(Faction.ENL)}|${csv(Faction.RES)}"
    }

    /** Restore tuning from [exportTuning]'s format (no-op if not built or malformed). */
    fun importTuning(encoded: String) {
        if (!built || encoded.isBlank()) return
        val parts = encoded.split("|")
        if (parts.size != 2) return
        val qs = orderedQValues()
        listOf(Faction.ENL to parts[0], Faction.RES to parts[1]).forEach { (faction, csv) ->
            val vals = csv.split(",")
            if (vals.size == qs.size) qs.forEachIndexed { i, q -> sliderInput(q, faction)?.value = vals[i] }
        }
        rows.forEach { it.valueLabel.textContent = display(it.input.value) } // resync interactive labels
        refresh() // and the read-only bars
    }

    private fun buildRow(qValue: QValue): HTMLElement {
        val row = el("div", "tuneRow")
        // 4-column grid: a fixed icon column (so every row's icon lines up in a column, regardless of
        // how the name text aligns) | name | slider/bar | value. Inline so nothing can collapse it.
        val rst = row.asDynamic().style
        rst.display = "grid"
        rst.gridTemplateColumns = "16px 1fr 92px 34px 16px" // + a lock-toggle column (AI-driven only)
        rst.alignItems = "center"
        rst.columnGap = "6px"
        val userInput = slider(qValue, userFaction)
        val enemyInput = slider(qValue, userFaction.enemy()).also { it.classList.add("invisible") }
        val valueLabel = el("span", "qSliderLabel").also { it.textContent = display(userInput.value) }
        val bar = el("div", "qBar")
        // The read-only bar mirrors the AI-driven value for the displayed faction → tint it that faction's colour.
        val fill = el("div", "qBarFill").also {
            bar.appendChild(it)
            it.asDynamic().style.background = userFaction.color
        }
        val lock = lockToggle()
        val builtRow = Row(qValue, userInput, valueLabel, bar, fill, lock)
        userInput.oninput = {
            valueLabel.textContent = display(userInput.value)
            // While the AI drives, an edit only sticks if this slider is locked — push it to the override.
            if (builtRow.locked) FactionPolicies.lock(userFaction, qValue, userInput.valueAsNumber)
            null
        }
        lock.onclick = {
            toggleLock(builtRow)
            null
        }
        // Fixed 3-column grid (icon+label | slider/bar | value) so icons + values line up in columns.
        val control = el("div", "tuneControl")
        control.appendChild(userInput)
        control.appendChild(enemyInput) // hidden value store
        control.appendChild(bar)
        row.appendChild(iconCell(qValue))
        row.appendChild(textLabel(qValue))
        row.appendChild(control)
        row.appendChild(valueLabel)
        row.appendChild(lock)
        rows.add(builtRow)
        return row
    }

    // The per-row lock toggle: shown only when an AI drives the faction; click to grab/release the slider.
    // Player-facing semantics: LOCKED (🔒) = the AI holds it (read-only bar); UNLOCKED (🔓) = you've taken it
    // over (editable slider). [applyMode] sets the live icon + title per row.
    private fun lockToggle(): HTMLElement = el("span", "qLockToggle").also {
        it.innerHTML = LOCK_CLOSED
        it.asDynamic().style.cursor = "pointer"
    }

    // Simple monochrome padlock icons (inherit the row's text colour via currentColor) — not the OS emoji.
    private const val LOCK_BODY =
        "<rect x='4' y='11' width='16' height='10' rx='2'/>"
    private val LOCK_CLOSED =
        svgIcon("$LOCK_BODY<path d='M8 11V7a4 4 0 0 1 8 0v4'/>")
    private val LOCK_OPEN =
        svgIcon("$LOCK_BODY<path d='M8 11V7a4 4 0 0 1 7.9-1'/>")

    private fun svgIcon(inner: String): String = "<svg viewBox='0 0 24 24' width='11' height='11' fill='none' stroke='currentColor' " +
        "stroke-width='2' stroke-linecap='round' stroke-linejoin='round'>$inner</svg>"

    // The action icon in its own fixed 16px column → icons line up in a column across all rows.
    private fun iconCell(qValue: QValue): HTMLElement {
        val cell = el("span", "qSliderIcon")
        qValue.icon?.let {
            val img = document.createElement("img") as HTMLImageElement
            img.src = it.toDataURL()
            cell.appendChild(img)
        }
        return cell
    }

    private fun textLabel(qValue: QValue): HTMLElement = el("span", "qSliderTextLabel").also { it.textContent = qValue.description }

    private fun slider(qValue: QValue, faction: Faction): HTMLInputElement {
        val s = document.createElement("input") as HTMLInputElement
        s.id = qValue.sliderId + faction.nickName
        s.type = "range"
        s.min = "0.00"
        s.max = "1.00"
        s.step = "0.01"
        s.value = "0.10"
        s.className = "slider qSlider " + faction.abbr.lowercase() + "Slider"
        return s
    }

    private fun applyMode() {
        val globallyLocked = mode == Mode.READONLY
        rows.forEach { r ->
            // A row is interactive when the panel isn't globally locked AND either no AI drives it or the
            // player has grabbed it (locked). Otherwise it's an auto-moving read-only bar.
            val interactive = !globallyLocked && (!aiDriven || r.locked)
            r.input.disabled = !interactive
            setVisible(r.input, interactive)
            setVisible(r.bar, !interactive)
            // The numeric value is shown in BOTH modes (slider + bar) and always sits in its own fixed grid
            // column, so the lock never shifts into the value's cell when the bar is showing.
            if (!interactive) r.fill.style.width = pct(r.input)
            // The lock toggle only makes sense while an AI drives and the panel isn't globally locked.
            setVisible(r.lock, aiDriven && !globallyLocked)
            // Player-facing: unlocked (open) = you're editing it; locked (closed) = the AI holds it.
            r.lock.innerHTML = if (r.locked) LOCK_OPEN else LOCK_CLOSED
            r.lock.title =
                if (r.locked) "You're editing this slider — click to hand it back to the AI" else "AI-controlled — click to unlock and edit"
        }
    }

    private fun setVisible(e: HTMLElement, visible: Boolean) {
        if (visible) e.classList.remove("tuneHidden") else e.classList.add("tuneHidden")
    }

    private fun pct(input: HTMLInputElement) = "${input.valueAsNumber * 100.0}%"

    private fun display(value: String): String {
        val fixed = value.padEnd(4, '0')
        return when (fixed) {
            "0000" -> "0.00"
            "1000" -> "1.00"
            else -> fixed
        }
    }
}
