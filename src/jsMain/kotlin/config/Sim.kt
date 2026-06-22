package config

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
    const val NORMAL_SCALE = 1.5
    const val LARGE_SCALE = 2.0
    private const val MAX_SCALE = 3.0

    var width = (Dim.width * NORMAL_SCALE).toInt()
        private set
    var height = (Dim.height * NORMAL_SCALE).toInt()
        private set

    /** Round play field (inscribed circle) instead of the rectangle — chosen at onboarding. */
    var roundField = false

    /** Field radius in sim units (the inscribed circle = the smaller half-extent). */
    fun fieldRadius(): Double = minOf(width, height) / 2.0

    /** Whether a point is inside the play field — always true for a rectangle; the inscribed circle when round. */
    fun isInsideField(x: Double, y: Double): Boolean {
        if (!roundField) return true
        val dx = x - width / 2.0
        val dy = y - height / 2.0
        val r = fieldRadius()
        return dx * dx + dy * dy <= r * r
    }

    /** Effective scale vs the screen — drives the framed display zoom (MapUtil). */
    val scale: Double get() = maxOf(width.toDouble() / Dim.width, height.toDouble() / Dim.height)

    /** Set the play-area size (clamped to a sane range around the screen size). */
    fun setSize(w: Int, h: Int) {
        width = w.coerceIn(Dim.width, (Dim.width * MAX_SCALE).toInt())
        height = h.coerceIn(Dim.height, (Dim.height * MAX_SCALE).toInt())
    }

    fun presetWidth(scaleOf: Double) = (Dim.width * scaleOf).toInt()
    fun presetHeight(scaleOf: Double) = (Dim.height * scaleOf).toInt()

    // Spawn margins where no portals are placed (absolute, same as Dim's).
    val leftOffset = Dim.leftOffset
    val rightOffset = Dim.rightOffset
    val topOffset = Dim.topOffset
    val botOffset = Dim.botOffset
}
