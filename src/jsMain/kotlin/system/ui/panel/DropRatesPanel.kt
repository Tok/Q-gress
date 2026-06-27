package system.ui.panel

import config.DropRates
import kotlinx.browser.document
import kotlinx.dom.addClass
import org.w3c.dom.HTMLDivElement

/** The "Drop rates" reference panel (Menu → Drop rates): per-hack-roll chances, toggled on/off. */
object DropRatesPanel {
    private const val PANEL_ID = "dropRatesPanel"

    fun toggle() {
        (document.getElementById(PANEL_ID) as? HTMLDivElement)?.let {
            it.remove()
            return
        }
        val panel = document.createElement("div") as HTMLDivElement
        panel.id = PANEL_ID
        panel.addClass("dropRatesPanel")
        panel.innerHTML = html()
        document.body?.appendChild(panel)
    }

    fun close() {
        (document.getElementById(PANEL_ID) as? HTMLDivElement)?.remove()
    }

    private fun html(): String {
        val sb = StringBuilder("<div class=\"dropRatesTitle\">Drop rates · per hack roll</div>")
        sb.append(rateRow("Portal key", DropRates.keyChance))
        DropRates.shieldChance.forEach { (t, c) -> sb.append(rateRow("Shield ${t.abbr}", c)) }
        DropRates.heatSinkChance.forEach { (t, c) -> sb.append(rateRow("Heat sink ${t.abbr}", c)) }
        DropRates.virusChance.forEach { (t, c) -> sb.append(rateRow("Virus ${t.abbr}", c)) }
        sb.append(
            "<div class=\"dropRatesNote\">Resonators / XMP / Power Cubes roll by tier — see " +
                "docs/MECHANICS.md. Link amps are inactive (never drop).</div>",
        )
        return sb.toString()
    }

    private fun rateRow(label: String, chance: Double): String {
        val pct = (chance * 1000).toInt() / 10.0
        return "<div class=\"dropRatesRow\">$label<span>$pct%</span></div>"
    }
}
