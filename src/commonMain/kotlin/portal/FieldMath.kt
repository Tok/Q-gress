package portal

import util.data.Line
import util.data.Pos
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Pure field geometry, extracted from [Field] (which holds the Portal references) into the shared functional
 * core (`commonMain`): the **Mind-Unit area** of a control field (Heron's formula over the three portal
 * positions) and the **point-in-triangle** test for whether a field covers a portal. No `World`/`Portal`
 * coupling — JVM-unit-tested + Kover-covered.
 */
object FieldMath {
    private const val MU_DIVISOR = 100 // sim-pixel area → MU scale (a tuning choice, not authentic Ingress)

    /** Mind Units of the triangle [a]-[b]-[c] (Heron's formula, scaled, floored at 1). */
    fun triangleAreaMu(a: Pos, b: Pos, c: Pos): Int {
        val ab = Line(a, b).length()
        val ac = Line(a, c).length()
        val bc = Line(b, c).length()
        val s = (ab + ac + bc) / 2 // semiperimeter
        val area = sqrt(s * (s - ab) * (s - ac) * (s - bc)).toInt()
        return max(1, area / MU_DIVISOR)
    }

    /** Whether point [p] lies inside the triangle [a]-[b]-[c] (barycentric sign test). */
    fun isInsideTriangle(p: Pos, a: Pos, b: Pos, c: Pos): Boolean {
        val dXtoC = p.x - c.x
        val dYtoC = p.y - c.y
        val dXcToB = c.x - b.x
        val dYbToC = b.y - c.y
        val d = (dYbToC * (a.x - c.x)) + (dXcToB * (a.y - c.y))
        val s = (dYbToC * dXtoC) + (dXcToB * dYtoC)
        val t = ((c.y - a.y) * dXtoC) + ((a.x - c.x) * dYtoC)
        return if (d < 0) s < 0 && t < 0 && s + t > d else s > 0 && t > 0 && s + t < d
    }
}
