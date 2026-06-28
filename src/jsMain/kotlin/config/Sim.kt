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
    const val TINY_KM2 = 0.20
    const val SMALL_KM2 = 0.5
    const val MID_KM2 = 1.0
    const val LARGE_KM2 = 2.0
    const val GIANT_KM2 = 3.0

    // Absolute sim-pixel clamp for custom (manual / URL) sizes — window-independent now that presets are absolute.
    private const val MIN_SIDE = 600
    private const val MAX_SIDE = 9000

    /** Square side (sim px) whose inscribed circle covers [km2] of (nominal) ground: area = π·(side·MPP/2)². */
    fun sideForArea(km2: Double): Int = (2.0 * sqrt(km2 * 1_000_000.0 / PI) / MPP_REF).roundToInt()

    /** Suggested start-portal count for a [km2] map — sub-linear (∛) so the per-portal flow-field cost stays
     *  bounded as the map grows (≈ 4 · 6 · 8 · 10 · 12 across tiny…giant). */
    fun suggestedPortals(km2: Double): Int = (8.0 * km2.pow(1.0 / 3.0)).roundToInt().coerceAtLeast(3)

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
