package system.display

import external.Three
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * The abstract glass "mushroom" portal silhouette, revolved with `LatheGeometry`, morphing across
 * the 8 portal levels. Deliberately abstract — *inspired by* a growing mushroom, not a literal one:
 *
 *  - L1     : a small round flask on a very thin stem.
 *  - L2..L4 : stem + flask grow.
 *  - L5     : the top opens from a round flask into a flat cylinder / shallow umbrella.
 *  - L6..L8 : the umbrella widens and its cap goes convex (L6) → flat (L7) → concave (L8).
 *
 * The stem thickens with level, and the **golden ratio φ** drives height-per-level. Geometry is
 * cached per level (only 8 shapes ever exist) and baked Z-up (lathe revolves around Y).
 */
object PortalShape {
    const val PHI = 1.618
    private const val SEGMENTS = 28 // radial segments of the revolve
    private const val CAP_STEPS = 14 // profile samples up the cap

    private const val BASE_HEIGHT = 9.0 // metres at L1
    private const val STEM_RADIUS0 = 0.5

    private val cache = mutableMapOf<Int, dynamic>()

    /** A cached, Z-up lathe geometry for [level] (1..8). */
    fun geometry(level: Int): dynamic = cache.getOrPut(level.coerceIn(1, 8)) {
        val pts = profile(level.coerceIn(1, 8)).map { Three.Vector2(it[0], it[1]) }.toTypedArray()
        val geo = Three.LatheGeometry(pts, SEGMENTS).asDynamic()
        geo.rotateX(PI / 2) // revolve axis Y → world up (+Z)
        geo
    }

    /** Total silhouette height (metres) at [level] — φ-scaled. Used for sizing pipes/anchors. */
    fun height(level: Int): Double = BASE_HEIGHT * PHI.pow((level.coerceIn(1, 8) - 1) / 4.0)

    /** Half-silhouette as [radius, height] control points, base at height 0, rising up the axis. */
    private fun profile(level: Int): List<DoubleArray> {
        val totalH = height(level)
        val stemR = STEM_RADIUS0 * (1.0 + (level - 1) * 0.22) // thickens with level
        val stemH = totalH * (0.6 - (level - 1) * 0.035)
        val capH = totalH - stemH
        val open = ((level - 4.0) / 4.0).coerceIn(0.0, 1.0) // 0 at L≤4 → 1 at L8 (flask → umbrella)
        val curve = (level - 7.0).coerceIn(-1.0, 1.0) // L6 convex(-1) → L7 flat(0) → L8 concave(+1)
        val neckR = stemR * (0.8 - 0.2 * open)
        val bulbR = totalH * 0.18
        val umbR = totalH * 0.48
        val capR = bulbR + (umbR - bulbR) * open

        val pts = mutableListOf<DoubleArray>()
        pts.add(doubleArrayOf(0.02, 0.0)) // base centre (tiny, avoids a pinch artifact)
        pts.add(doubleArrayOf(stemR, 0.0)) // base rim
        pts.add(doubleArrayOf(stemR, stemH * 0.55)) // up the stem
        pts.add(doubleArrayOf(neckR, stemH)) // neck at the cap base
        for (i in 1..CAP_STEPS) {
            val t = i.toDouble() / CAP_STEPS
            val rBulb = neckR + (capR - neckR) * sin(t * PI) * (0.6 + 0.4 * (1.0 - t)) // teardrop
            val rUmb = if (t < 0.35) {
                neckR * (1.0 - t * 0.5) // thin stalk
            } else {
                val u = ((t - 0.35) / 0.65).coerceIn(0.0, 1.0)
                neckR * 0.7 + (capR - neckR * 0.7) * smooth(u) // flare to the rim
            }
            val r = rBulb + (rUmb - rBulb) * open
            var h = stemH + capH * t
            if (open > 0.5) h += curve * capH * 0.35 * (r / capR.coerceAtLeast(0.01)) // cap curvature
            pts.add(doubleArrayOf(r.coerceAtLeast(0.01), h))
        }
        return pts
    }

    private fun smooth(x: Double): Double = x * x * (3.0 - 2.0 * x)
}
