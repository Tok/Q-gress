package system.audio

import agent.Faction
import util.Rng

/**
 * The TTS announcer's lines — a clinical "scanner" voice (science vs luddite, not military). Each call returns
 * one randomly-chosen variant so repeated events don't read identically. Pure string building; [Tts] speaks them.
 */
object TtsPhrases {
    private fun one(vararg variants: String) = variants[Rng.randomInt(variants.size - 1)]
    private fun side(f: Faction) = if (f == Faction.ENL) "Enlightened" else "Resistance"

    fun title() = one("Q-Gress.", "Q-Gress online.", "Scanner active. Q-Gress.")

    fun faction(f: Faction) = one(
        "${side(f)} selected.",
        "You stand with the ${side(f)}.",
        "Allegiance set. ${side(f)}.",
    )

    fun location(name: String) = one(
        "Scanning $name.",
        "Theatre of operations. $name.",
        "Initialising the portal network over $name.",
    )

    fun checkpointLead(leader: Faction, leadMu: Int) = one(
        "${side(leader)} lead the cycle by $leadMu mind units.",
        "Checkpoint. ${side(leader)} ahead by $leadMu M-U.",
    )

    fun hugeField(owner: Faction, mu: Int) = one(
        "Massive ${side(owner)} field. $mu mind units.",
        "${side(owner)} control field detected. $mu M-U.",
    )

    fun portalDiscovery(name: String) = one("New portal. $name.", "Portal discovered. $name.")

    fun recruitment(f: Faction) = one("${side(f)} recruit acquired.", "A new agent joins the ${side(f)}.")

    /** The "glyph" verbosity tier: read the hacked sequence back, like the Scanner's glyph channel. */
    fun glyphHack(f: Faction, glyphNames: List<String>) = "${side(f)} glyph sequence. ${glyphNames.joinToString(". ")}."
}
