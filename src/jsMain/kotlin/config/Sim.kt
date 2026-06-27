package config

import kotlin.math.sqrt

/**
 * Simulation/grid extent — independent of the screen ([Dim], which drives the HUD). The world
 * covers some multiple of the screen ([scale]) so the playable area spans the pitched view, not
 * just the top-down footprint. The Pos→metre bridge stays anchored at zoom 18.
 *
 * Size is chosen at onboarding (the map-size step): bigger = more grid cells = a slower build and
 * pricier per-portal flow fields (each portal builds a full-map field). "Large" is the original
 * SCALE=2.0; the default is "Normal" (a bit smaller); "Small" is way smaller.
 */
object Sim {
    const val SMALL_SCALE = 1.0
    const val LARGE_SCALE = 2.0

    /**
     * Normal sits at the **area midpoint** of Small and Large. Play area grows with the square of the scale, so
     * we average the squares and take the root (√((1²+2²)/2) ≈ 1.58, vs the old flat 1.5 which sat below the
     * true midpoint). We can't weight the presets by walkability — unknown until the grid is built — so we
     * balance the raw play-area instead: Normal's area is exactly halfway between Small's and Large's.
     */
    val NORMAL_SCALE = sqrt((SMALL_SCALE * SMALL_SCALE + LARGE_SCALE * LARGE_SCALE) / 2.0)

    private const val MAX_SCALE = 3.0

    var width = (Dim.width * NORMAL_SCALE).toInt()
        private set
    var height = (Dim.height * NORMAL_SCALE).toInt()
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

    /** Set the play-area size (clamped to a sane range around the screen size). */
    fun setSize(w: Int, h: Int) {
        width = w.coerceIn(Dim.width, (Dim.width * MAX_SCALE).toInt())
        height = h.coerceIn(Dim.height, (Dim.height * MAX_SCALE).toInt())
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

    fun presetWidth(scaleOf: Double) = (Dim.width * scaleOf).toInt()
    fun presetHeight(scaleOf: Double) = (Dim.height * scaleOf).toInt()

    // Spawn margins where no portals are placed (absolute, same as Dim's).
    val leftOffset = Dim.leftOffset
    val rightOffset = Dim.rightOffset
    val topOffset = Dim.topOffset
    val botOffset = Dim.botOffset
}
