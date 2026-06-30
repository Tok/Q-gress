package config

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Simulation/grid extent — independent of the screen ([Dim], which drives the HUD). The Pos→metre bridge stays
 * anchored at zoom 18, so a sim pixel is a fixed real-world size ([MPP_REF]).
 *
 * **Sizes are chosen by real-world AREA (km²), not a screen multiple** — area is the meaningful measure (a "1 km²"
 * map covers ~1 km² of ground regardless of the window). Each preset [TINY_KM2]…[GIANT_KM2] maps to a SQUARE
 * whose inscribed circle (the round play field) covers that area; [sideForArea] does the conversion. Bigger =
 * more grid cells = a slower build and pricier per-portal flow fields (each portal builds a full-map field), so
 * the **per-portal pathfinding cost — not the raw area — is what bounds how big we go**: [suggestedPortals] grows
 * the portal count only sub-linearly (∛), and [GIANT_KM2] is warned at onboarding. NPC population is area-linear
 * but capped (see [ConfigMath.npcPopulation]), so it won't run away on the big presets.
 *
 * Area is NOMINAL (computed at the equator reference [MPP_REF]); the real on-map area shrinks by ~cos²(latitude),
 * but sizing the grid nominally keeps the cell count — and therefore perf — predictable everywhere.
 */
object Sim {
    /** Metres per sim pixel at the zoom-18 anchor, equator (512-px tiles: earthCircumference/512 ÷ 2¹⁸). The
     *  real value is ×cos(lat); we size grids by this nominal value so perf doesn't swing with latitude. */
    const val MPP_REF = 78271.516964 / 262144.0 // ≈ 0.2986 m/px

    // Map-size presets by play-area in km². "tiny" is the title-screen arena, "small" the onboarding default
    // (smaller = less walking = more action), "giant" the warned heavy ceiling. Portal count (the pathfinding
    // driver) follows area sub-linearly.
    const val TINY_KM2 = 0.10
    const val SMALL_KM2 = 0.20
    const val MID_KM2 = 0.50
    const val LARGE_KM2 = 1.0
    const val GIANT_KM2 = 2.0

    // Absolute sim-pixel clamp for custom (manual / URL) sizes — window-independent now that presets are absolute.
    private const val MIN_SIDE = 600
    private const val MAX_SIDE = 9000

    /** How many shadow-cell rows the grid extends *beyond* each edge of the play area (off-screen margin so NPCs
     *  can stream in/out + flow fields resolve past the border). The grid build ([system.map.ShadowGridBuilder])
     *  and the off-map NPC destinations ([agent.NonFaction]) both key off this. */
    const val OFFSCREEN_CELL_ROWS = 10

    /** Square side (sim px) whose inscribed circle covers [km2] of (nominal) ground: area = π·(side·MPP/2)². */
    fun sideForArea(km2: Double): Int = (2.0 * sqrt(km2 * 1_000_000.0 / PI) / MPP_REF).roundToInt()

    /** The current play field's nominal area (km²) — the inverse of [sideForArea] over the inscribed circle of
     *  the (smaller) side. Used to scale the roster ([config.Config.rosterForStart] / [config.Config.rosterCap]). */
    fun areaKm2(): Double = PI * (minOf(width, height).toDouble() * MPP_REF / 2.0).pow(2.0) / 1_000_000.0

    /** Suggested start-portal count for a [km2] map — sub-linear (∛) so the per-portal flow-field cost stays
     *  bounded as the map grows (≈ 4 · 5 · 6 · 8 · 10 across tiny…giant). */
    fun suggestedPortals(km2: Double): Int = (8.0 * km2.pow(1.0 / 3.0)).roundToInt().coerceAtLeast(3)

    /** Suggested per-faction MID-GAME roster for a [km2] map — bucketed by the size presets (midpoint thresholds,
     *  robust to [sideForArea]'s rounding): Tiny 3 · Small 5 · Mid 8 · Large 12 · Giant 16. */
    fun suggestedAgents(km2: Double): Int = when {
        km2 < (TINY_KM2 + SMALL_KM2) / 2.0 -> 3
        km2 < (SMALL_KM2 + MID_KM2) / 2.0 -> 5
        km2 < (MID_KM2 + LARGE_KM2) / 2.0 -> 8
        km2 < (LARGE_KM2 + GIANT_KM2) / 2.0 -> 12
        else -> 16
    }

    /** Per-faction MAX roster (the recruiting cap / end-game seed) for a [km2] map — bucketed by size so small
     *  maps can't fill with the giant ceiling: Tiny 8 · Small 16 · Mid 24 · Large 28 · Giant 32 (the hard limit). */
    fun maxAgents(km2: Double): Int = when {
        km2 < (TINY_KM2 + SMALL_KM2) / 2.0 -> 8
        km2 < (SMALL_KM2 + MID_KM2) / 2.0 -> 16
        km2 < (MID_KM2 + LARGE_KM2) / 2.0 -> 24
        km2 < (LARGE_KM2 + GIANT_KM2) / 2.0 -> 28
        else -> 32
    }

    var width = sideForArea(SMALL_KM2)
        private set
    var height = sideForArea(SMALL_KM2)
        private set

    /** Round play field (inscribed circle) instead of the rectangle — chosen at onboarding (default on). */
    var roundField = true

    /** Field radius in sim units (the inscribed circle = the smaller half-extent). */
    fun fieldRadius(): Double = SimMath.fieldRadius(width, height)

    /** Whether a point is inside the play field — always true for a rectangle; the inscribed circle when round. */
    fun isInsideField(x: Double, y: Double): Boolean = SimMath.isInsideField(roundField, width, height, x, y)

    /** Inside the *displayable* play area: on-screen bounds AND inside the field (circle when round). */
    fun isInPlayArea(x: Double, y: Double) = SimMath.isInPlayArea(roundField, width, height, x, y)

    /** Effective scale vs the screen — drives the framed display zoom (MapController). */
    val scale: Double get() = maxOf(width.toDouble() / Dim.width, height.toDouble() / Dim.height)

    /** Set the play-area size (clamped to a sane absolute pixel range — see [MIN_SIDE]/[MAX_SIDE]). */
    fun setSize(w: Int, h: Int) {
        width = w.coerceIn(MIN_SIDE, MAX_SIDE)
        height = h.coerceIn(MIN_SIDE, MAX_SIDE)
    }

    /**
     * Force an exact play-area extent, bypassing the screen-size clamp. For **headless matches**, which size
     * their arena to their own (small) grid so a match is self-contained and deterministic — independent of the
     * live onboarding preset. The live game always goes through [setSize]; a mid-game eval restores the real
     * size afterwards via [system.WorldSnapshot].
     */
    fun setExactSize(w: Int, h: Int) {
        width = w
        height = h
    }

    // Spawn margins where no portals are placed (absolute, same as Dim's).
    val leftOffset = Dim.leftOffset
    val rightOffset = Dim.rightOffset
    val topOffset = Dim.topOffset
    val botOffset = Dim.botOffset
}
