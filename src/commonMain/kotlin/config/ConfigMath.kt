package config

/**
 * Pure tuning formulas behind [Config] — the dynamism-driven combat curves, the portal-density equilibrium and
 * the NPC-population model. Lives in the shared functional core (`commonMain`): all inputs (the mutable
 * sliders, the browser-derived [Dim] sizes) are passed in, so there is no `World`/DOM/`js()` coupling and the
 * curves are JVM-unit-tested + Kover-covered. [Config] holds the live state and delegates here.
 */
object ConfigMath {
    const val PORTALS_PER_WALKABLE_KM2 = 200.0 // discovery equilibrium: portals per km² of FULLY-walkable ground

    const val MIN_NONFACTION = 30 // floor: always enough to recruit, even on a tiny/dense map (or a low multiplier)
    const val MAX_NONFACTION_CAP = 1000 // ceiling for tiny…mid maps: keep them from spawning a perf-killing crowd
    const val MAX_NONFACTION_CAP_LARGE = 2000 // higher ceiling for LARGE/GIANT maps (more room for a big crowd)
    private const val NPC_DENSITY = 300.0 // NPCs per one screenful (Dim.width × Dim.height) of walkable area
    private const val CITY_GAIN = 1.2 // built-up (low-walkable) areas pack in more people
    private const val TOURIST_MUL = 1.6 // famous tourist spots draw extra crowds

    private const val MITIGATION_DYNAMISM_SPAN = 75.0 // how far max dynamism lowers the mitigation cap
    private const val MITIGATION_FLOOR = 15
    private const val DROP_DYNAMISM_SPAN = 19.0 // weapon drops scale 1× … 20× across the dynamism range
    private const val XMP_THRESHOLD_BASE = 30 // XMPs hoarded before an assault at zero dynamism…
    private const val XMP_THRESHOLD_SPAN = 22 // …falling by this much at full dynamism
    private const val XMP_THRESHOLD_MIN = 8

    /**
     * Equilibrium portal count the discovery churn converges toward (agent.action.cond.Discoverer), set by the map's
     * WALKABLE ground — its [areaKm2] × [walkability] — at a constant [PORTALS_PER_WALKABLE_KM2] density. So open maps
     * support proportionally more portals and built-up / cramped ones fewer, independent of window size. Clamped to
     * keep at least the onboarding [startPortals] and never exceed [maxPortals] (the per-portal flow-field perf ceiling).
     */
    fun targetPortals(areaKm2: Double, walkability: Double, startPortals: Int, maxPortals: Int): Int {
        val walkableKm2 = areaKm2 * walkability.coerceIn(0.0, 1.0)
        return (PORTALS_PER_WALKABLE_KM2 * walkableKm2).toInt().coerceIn(startPortals, maxPortals)
    }

    /** Shield/link mitigation cap for the current [combatDynamism]: higher dynamism → lower cap → more flips. */
    fun maxMitigation(combatDynamism: Double): Int {
        val cap = IngressFacts.MITIGATION_CAP_PCT - combatDynamism * MITIGATION_DYNAMISM_SPAN
        return cap.toInt().coerceIn(MITIGATION_FLOOR, IngressFacts.MITIGATION_CAP_PCT)
    }

    /** Weapon-drop multiplier (XMP + Ultra-Strike yield per hack): `1×` … `20×` as [combatDynamism] rises. */
    fun weaponDropMultiplier(combatDynamism: Double): Double = 1.0 + combatDynamism * DROP_DYNAMISM_SPAN

    /** XMPs an agent hoards before an assault: `30` (cautious) … `8` (trigger-happy) as [combatDynamism] rises. */
    fun attackXmpThreshold(combatDynamism: Double): Int =
        (XMP_THRESHOLD_BASE - combatDynamism * XMP_THRESHOLD_SPAN).toInt().coerceAtLeast(XMP_THRESHOLD_MIN)

    /** Comeback bonus fraction — equals [combatDynamism] (the faction behind on portals hits harder). */
    fun comebackAttackBonus(combatDynamism: Double): Double = combatDynamism

    /**
     * Appropriate NPC population for a map whose area is [areaRatio]× the reference screen, at a location of the
     * given [walkability] (fraction of passable cells). Driven by the **walkable area**, boosted by **city
     * density** (built-up = low-walkable → more people) and, for a [tourist] hotspot, a crowd bonus. Scaled by
     * [npcMultiplier], then clamped to [[MIN_NONFACTION], [maxCap]]. The caller computes [areaRatio] from the
     * (browser-derived) map vs reference dimensions and picks [maxCap] by map tier (larger on big maps).
     */
    fun npcPopulation(
        areaRatio: Double,
        walkability: Double,
        tourist: Boolean,
        npcMultiplier: Double,
        maxCap: Int = MAX_NONFACTION_CAP,
    ): Int {
        val walk = walkability.coerceIn(0.0, 1.0)
        val walkableArea = areaRatio * walk // NPCs roam open ground → the primary driver
        val cityDensity = 1.0 - walk // proxy: the less walkable a (playable) area is, the more built-up
        val touristMul = if (tourist) TOURIST_MUL else 1.0
        val pop = NPC_DENSITY * walkableArea * (1.0 + CITY_GAIN * cityDensity) * touristMul * npcMultiplier
        return pop.toInt().coerceIn(MIN_NONFACTION, maxCap)
    }
}
