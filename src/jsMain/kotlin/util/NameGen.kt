package util

import agent.Faction

/**
 * Plausible Ingress-style agent handles — combined from themed word banks (techno/sci-fi + nature,
 * leaning into the "science vs luddite" aesthetic; deliberately not military) plus the usual handle
 * decorations: CamelCase/snake/dot styling, optional leetspeak, numeric suffixes, and the odd
 * `xX_..._Xx` wrap. Optional faction flavour (frogs lean green/bio, smurfs lean cold/blue) and a
 * location token (e.g. "BerlinRaven") when a real place is loaded.
 *
 * Handles are deduped within a world ([reset] on each new agent roster) since the name doubles as the
 * agent's lookup key.
 */
object NameGen {
    private val used = HashSet<String>()

    /** Drop the dedupe history — call when a fresh agent roster is created. */
    fun reset() = used.clear()

    /** A unique handle for [faction], optionally flavoured by the loaded [location] name. */
    fun handle(faction: Faction? = null, location: String? = null): String {
        repeat(RETRIES) {
            val name = build(faction, locationToken(location))
            if (used.add(name)) return name
        }
        val base = build(faction, locationToken(location))
        var i = 2
        while (!used.add(base + i)) i++
        return base + i
    }

    private fun build(faction: Faction?, loc: String?): String {
        val core = pickCore(faction, loc)
        val styled = style(core)
        val numbered = if (core.size == 1 || Util.random() < NUMBER_CHANCE) styled + number() else styled
        return if (Util.random() < WRAP_CHANCE) "xX_${numbered}_Xx" else numbered
    }

    // --- core word selection ---------------------------------------------------------------------

    private fun pickCore(faction: Faction?, loc: String?): List<String> {
        // Available patterns, picked uniformly. adj+noun appears twice (the most "handle-like" shape).
        val patterns = mutableListOf<() -> List<String>>(
            { listOf(adj(), noun()) },
            { listOf(adj(), noun()) },
            { listOf(prefix(), noun()) },
            { listOf(prefix(), adj(), noun()) },
            { listOf(noun(), pick(SUFFIX_WORDS)) },
            { listOf(pick(TITLES), noun()) },
            { listOf(noun()) }, // a lone noun — build() always numbers it
        )
        if (loc != null) {
            patterns.add { if (Util.randomBool()) listOf(loc, noun()) else listOf(adj(), loc) }
        }
        if (faction != null) {
            val fw = pick(factionWords(faction))
            patterns.add { listOf(fw, noun()) }
            patterns.add { listOf(adj(), fw) }
        }
        return patterns[Util.randomInt(0, patterns.size - 1)]()
    }

    private fun pick(list: List<String>) = Util.shuffle(list).first()
    private fun adj() = pick(ADJECTIVES)
    private fun noun() = pick(NOUNS)
    private fun prefix() = pick(PREFIXES)

    // --- styling ---------------------------------------------------------------------------------

    private fun style(tokens: List<String>): String {
        val sep = Util.select(SEPARATORS, "")
        val joined = tokens.joinToString(sep)
        val cased = when (Util.select(CASE_STYLES, CaseStyle.CAMEL)) {
            CaseStyle.LOWER -> joined.lowercase()
            CaseStyle.UPPER -> joined.uppercase()
            CaseStyle.CAMEL -> joined
        }
        // Leet only short handles — long compounds leet into unreadable gibberish.
        return if (cased.length <= LEET_MAX_LEN && Util.random() < LEET_CHANCE) leet(cased) else cased
    }

    private val LEET = mapOf('a' to '4', 'e' to '3', 'i' to '1', 'o' to '0', 's' to '5', 't' to '7')

    private fun leet(s: String): String = s.map { LEET[it.lowercaseChar()] ?: it }.joinToString("")

    private fun number(): String {
        val sep = if (Util.random() < NUMBER_SEP_CHANCE) Util.select(SEPARATORS, "") else ""
        val n = when (Util.select(NUMBER_KINDS, 0)) {
            0 -> "${Util.randomInt(2, 99)}" // two digits
            1 -> "${Util.randomInt(100, 999)}" // three digits
            else -> pick(ICONIC_NUMBERS) // "1337", "42", "007", …
        }
        return sep + n
    }

    // --- location --------------------------------------------------------------------------------

    // First real word of the loaded place name (skips the generic fallbacks), Capitalised, letters only.
    private fun locationToken(location: String?): String? {
        val name = location?.trim().orEmpty()
        if (name.isEmpty() || name.equals("Your location", true) || name.startsWith("Custom", true)) return null
        val word = name.split(' ', ',', '-').map { it.filter(Char::isLetter) }.firstOrNull { it.length >= 3 }
            ?: return null
        return word.lowercase().replaceFirstChar { it.uppercase() }
    }

    private fun factionWords(faction: Faction) = when (faction) {
        Faction.ENL -> ENL_WORDS
        Faction.RES -> RES_WORDS
    }

    // --- banks -----------------------------------------------------------------------------------

    private enum class CaseStyle { CAMEL, LOWER, UPPER }

    private val CASE_STYLES = listOf(0.80 to CaseStyle.CAMEL, 0.14 to CaseStyle.LOWER, 0.06 to CaseStyle.UPPER)
    private val SEPARATORS = listOf(0.74 to "", 0.14 to "_", 0.07 to ".", 0.05 to "-")
    private val NUMBER_KINDS = listOf(0.45 to 0, 0.20 to 1, 0.35 to 2)
    private val ICONIC_NUMBERS = listOf("42", "23", "99", "88", "07", "13", "69", "777", "360", "1337", "007")

    private const val RETRIES = 8
    private const val NUMBER_CHANCE = 0.42
    private const val NUMBER_SEP_CHANCE = 0.25
    private const val LEET_CHANCE = 0.12
    private const val LEET_MAX_LEN = 9
    private const val WRAP_CHANCE = 0.03

    private val TITLES = listOf("Agent", "Dr", "Captain", "Prof", "Lord", "Lady", "Sir", "Major")
    private val SUFFIX_WORDS = listOf("Prime", "Zero", "Neo", "Max", "Pro", "X", "OG", "HD", "GG")

    private val PREFIXES = listOf(
        "Cyber", "Quantum", "Hyper", "Ultra", "Mega", "Neo", "Nano", "Astro", "Solar", "Lunar",
        "Neon", "Void", "Atom", "Echo", "Ghost", "Omega", "Alpha", "Prime", "Volt", "Flux",
        "Photon", "Plasma", "Iron", "Shadow", "Crimson", "Cosmic", "Turbo", "Pixel", "Vapor", "Glitch",
    )

    private val ADJECTIVES = listOf(
        "Silent", "Rogue", "Wild", "Frozen", "Toxic", "Electric", "Savage", "Lucid", "Phantom",
        "Radiant", "Static", "Feral", "Arcane", "Hollow", "Vivid", "Rapid", "Stealth", "Brave",
        "Grim", "Sly", "Mad", "Lone", "Hidden", "Restless", "Reckless", "Eternal", "Fractal", "Velvet",
    )

    private val NOUNS = listOf(
        // creatures
        "Fox", "Wolf", "Raven", "Falcon", "Viper", "Panda", "Otter", "Lynx", "Hawk", "Owl",
        "Heron", "Mantis", "Cobra", "Jackal", "Magpie",
        // mythic
        "Phoenix", "Dragon", "Wraith", "Specter", "Hydra", "Sphinx", "Golem", "Banshee", "Kraken", "Titan",
        // tech / sci
        "Cipher", "Vector", "Proton", "Reactor", "Circuit", "Matrix", "Synapse", "Pulse", "Nexus",
        "Beacon", "Relay", "Quark", "Daemon", "Probe", "Byte", "Glyph", "Drone",
        // cosmic / nature
        "Comet", "Nova", "Nebula", "Aurora", "Storm", "Ember", "Quartz", "Onyx", "Cobalt", "Cinder",
    )

    // ENL "frogs" — green/bio/light. RES "smurfs" — cold/blue/structure.
    private val ENL_WORDS = listOf("Frog", "Toad", "Newt", "Verdant", "Chloro", "Bio", "Spore", "Fern", "Moss", "Jade")
    private val RES_WORDS = listOf("Smurf", "Cobalt", "Azure", "Frost", "Indigo", "Glacier", "Cryo", "Sapphire", "Tide", "Ion")
}
