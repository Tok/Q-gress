package config

/**
 * How far along the game starts — chosen at onboarding (the old "quick start" checkbox, now a 3-way pick,
 * default [MID]). Drives each agent's level (via [initialAp]) and gear (`Inventory.startingGear`); the starting
 * ROSTER SIZE is stage × map size in [Config.rosterForStart] (normal = 1 agent, mid = the size's
 * [Sim.suggestedAgents], end = the full [Config.rosterCap]). Leaner start, bigger end.
 */
enum class StartStage(val initialAp: Int) {
    START(0), // a single L1 agent per side, no gear — a true cold open
    MID(100_000), // ~L4 squad with light gear — the default, a game already in motion
    END(2_000_000), // a full L8 roster with full L8 gear — the late game

    ;

    companion object {
        fun fromString(s: String?): StartStage? = values().firstOrNull { it.name.equals(s, ignoreCase = true) }
    }
}
