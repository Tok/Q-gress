package util.ui

import agent.Faction
import agent.qvalue.QActions
import agent.qvalue.QDestinations
import agent.qvalue.QValue
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

    private class Row(val input: HTMLInputElement, val valueLabel: HTMLElement, val bar: HTMLElement, val fill: HTMLElement)

    private var built = false
    private var mode = Mode.INTERACTIVE
    private var userFaction = Faction.ENL
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

    /** Re-sync the read-only bars from the (possibly externally-written) input values. */
    fun refresh() {
        if (mode != Mode.READONLY) return
        rows.forEach { it.fill.style.width = pct(it.input) }
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
        rst.gridTemplateColumns = "16px 1fr 92px 34px"
        rst.alignItems = "center"
        rst.columnGap = "6px"
        val userInput = slider(qValue, userFaction)
        val enemyInput = slider(qValue, userFaction.enemy()).also { it.classList.add("invisible") }
        val valueLabel = el("span", "qSliderLabel").also { it.textContent = display(userInput.value) }
        userInput.oninput = {
            valueLabel.textContent = display(userInput.value)
            null
        }
        val bar = el("div", "qBar")
        val fill = el("div", "qBarFill").also { bar.appendChild(it) }
        // Fixed 3-column grid (icon+label | slider/bar | value) so icons + values line up in columns.
        val control = el("div", "tuneControl")
        control.appendChild(userInput)
        control.appendChild(enemyInput) // hidden value store
        control.appendChild(bar)
        row.appendChild(iconCell(qValue))
        row.appendChild(textLabel(qValue))
        row.appendChild(control)
        row.appendChild(valueLabel)
        rows.add(Row(userInput, valueLabel, bar, fill))
        return row
    }

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
        val readOnly = mode == Mode.READONLY
        rows.forEach { r ->
            r.input.disabled = readOnly
            setVisible(r.input, !readOnly)
            setVisible(r.valueLabel, !readOnly)
            setVisible(r.bar, readOnly)
            if (readOnly) r.fill.style.width = pct(r.input)
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

    private fun el(tag: String, cls: String): HTMLElement {
        val e = document.createElement(tag) as HTMLElement
        if (cls.isNotEmpty()) e.className = cls
        return e
    }
}
