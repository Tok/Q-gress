package items.types

import agent.Faction

/**
 * The two flip items. **Either faction may carry and use either one** — the item's [flipsTo] (not the
 * holder's faction) decides the result: a JARVIS Virus always flips a portal to ENL (green), an ADA
 * Refactor always to RES (blue). So the matching item attack-flips an enemy portal to your colour, and
 * the off-colour item friendly-flips your own portal to the enemy colour (to shed a blocking link, etc.).
 */
enum class VirusType(val abbr: String, val color: String, val roll: Int, val flipsTo: Faction) {
    JARVIS_VIRUS("JARVIS", Faction.ENL.color, 2500, Faction.ENL),
    ADA_REFACTOR("ADA", Faction.RES.color, 2500, Faction.RES),
}
