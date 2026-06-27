package config

/**
 * Pure play-field geometry — the rectangle/inscribed-circle membership math, extracted from [Sim] into the
 * shared functional core (`commonMain`). [Sim] owns the live size/shape *state* (which keys off the browser
 * screen via [Dim]); these are the side-effect-free predicates over it, so they're JVM-unit-tested +
 * Kover-covered. The field is the rectangle `[0,width)×[0,height)`, narrowed to its inscribed circle when
 * `roundField` is on.
 */
object SimMath {
    /** Field radius in sim units — the inscribed circle, i.e. the smaller half-extent. */
    fun fieldRadius(width: Int, height: Int): Double = minOf(width, height) / 2.0

    /** Inside the play field: always true for a rectangle; inside the inscribed circle when [roundField]. */
    fun isInsideField(roundField: Boolean, width: Int, height: Int, x: Double, y: Double): Boolean {
        if (!roundField) return true
        val dx = x - width / 2.0
        val dy = y - height / 2.0
        val r = fieldRadius(width, height)
        return dx * dx + dy * dy <= r * r
    }

    /** Inside the *displayable* play area: within the on-screen bounds AND inside the field (circle when round). */
    fun isInPlayArea(roundField: Boolean, width: Int, height: Int, x: Double, y: Double): Boolean =
        x >= 0 && y >= 0 && x < width && y < height && isInsideField(roundField, width, height, x, y)
}
