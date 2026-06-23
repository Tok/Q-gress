package config

/**
 * AUTHENTIC INGRESS REFERENCE DATA — researched from the public Ingress wikis/guides (sources below).
 *
 * ⚠️ DO NOT change these numbers to tune our sim. They are a read-only record of how the *original game*
 * works, kept here (even for mechanics we don't use yet) so the design has a single source of truth.
 * Our sim deliberately diverges for a more dynamic AI-vs-AI feel — tune that in [items.Combat] and the
 * gameplay level enums, NOT here. (Note: animation-only "energy" for building-shake / shatter physics is
 * a sim invention with no Ingress equivalent — it lives in its own FX modules and is freely tunable.)
 *
 * Sources:
 *  - https://ingress.wiki.gg/wiki/XMP_Burster   (XMP table: damage / range / cost / recycle)
 *  - https://ingress.fandom.com/wiki/Weapons , /wiki/Ultra_Strike  (XMP vs US roles, crit, falloff)
 *  - https://ingress.fandom.com/wiki/Resonator , /wiki/Portal_Shield (energy, mitigation, stickiness)
 *  - https://fevgames.net/ingress/ingress-guide/actions/attack/      (attack mechanics)
 */
object IngressFacts {

    /** XMP Burster per level: damage (XM), blast range (m), usage cost (XM), recycle value (XM). */
    enum class Xmp(val level: Int, val damageXm: Int, val rangeM: Int, val costXm: Int, val recycleXm: Int) {
        L1(1, 150, 42, 50, 20),
        L2(2, 300, 48, 100, 40),
        L3(3, 500, 58, 150, 60),
        L4(4, 900, 72, 200, 80),
        L5(5, 1200, 90, 250, 100),
        L6(6, 1500, 112, 300, 120),
        L7(7, 1800, 138, 350, 140),
        L8(8, 2700, 168, 400, 160),
    }

    /** Resonator per level: max energy (XM) and how many ONE player may deploy on a single portal. */
    enum class Reso(val level: Int, val energyXm: Int, val perPlayer: Int) {
        L1(1, 1000, 8),
        L2(2, 1500, 4),
        L3(3, 2000, 4),
        L4(4, 2500, 4),
        L5(5, 3000, 2),
        L6(6, 4000, 2),
        L7(7, 5000, 1),
        L8(8, 6000, 1),
    }

    /**
     * Portal Shield mods: mitigation (% incoming-damage reduction). Stickiness (resistance to being
     * knocked out by an XMP/US) is a real mechanic but its exact values aren't published — the numbers
     * here are our best-guess ordering (higher tier = stickier), not authoritative.
     */
    enum class Shield(val abbr: String, val mitigationPct: Int, val approxStickiness: Int) {
        COMMON("CS", 30, 0),
        RARE("RS", 40, 15),
        VERY_RARE("VRS", 60, 45),
        AXA("AXA / AEGIS", 70, 80),
    }

    // --- Combat rules of the original game ------------------------------------------------------------
    /** Stepped range falloff: a reso at 0-20 / 20-40 / 40-60 / 60-80 / 80-100% of range takes this % of
     *  raw damage. Our sim uses this same rule (see [items.Combat.rangeFalloff]). */
    val RANGE_FALLOFF_PCT = intArrayOf(100, 50, 25, 13, 6) // 12.5 / 6.25 rounded
    const val MITIGATION_CAP_PCT = 95 // shields/links cap damage reduction here (we soften to 80 for dynamism)
    const val MITIGATION_MIN_DAMAGE = 1 // every in-range reso still takes ≥1 XM, mitigation rounded down
    const val CRIT_MULTIPLIER = 2 // a chance hit deals DOUBLE damage (we use 3× for extra punch)
    const val RESO_SLOTS = 8 // a portal has 8 resonator slots (one per octant)…
    const val MOD_SLOTS = 4 // …and 4 mod slots
    // Roles: XMP Bursters destroy RESONATORS — AoE centred on the agent, radius ∝ level. Ultra-Strikes are
    // specialised XMPs with a much smaller radius (≈⅓–½), best at destroying MODS (esp. Portal Shields).
    // Both can crit; an Ultra-Strike drops less often than an XMP Burster.

    // --- Distances & timings of the original (real-world metres / seconds) ---------------------------
    const val DEPLOY_RANGE_M = 40 // an agent must be within ~40 m of a portal to hack / deploy / attack / link
    const val HACK_COOLDOWN_S = 300 // 5 minutes between hacks of the same portal…
    const val HACKS_BEFORE_BURNOUT = 4 // …and 4 hacks within 4 h trips a longer burnout (we cap hacks at 4)
    // Link length is gated by the SOURCE portal's level (higher level → longer links); the exact per-level
    // metres aren't cleanly published, so we don't tabulate them. Links may never cross existing links/fields.

    // --- AP (action points) the original awards ------------------------------------------------------
    const val AP_DESTROY_RESONATOR = 75
    const val AP_DESTROY_MOD = 75
    const val AP_DEPLOY_RESONATOR = 125
    const val AP_CAPTURE_PORTAL = 500 // full capture (deploy on a neutral portal)
    const val AP_CREATE_LINK = 313
    const val AP_CREATE_FIELD = 1250
}
