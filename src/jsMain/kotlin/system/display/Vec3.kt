package system.display

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Shared 3-component vector helpers (each vector is a `DoubleArray` of size 3) for the 3D effect
 * modules — [BoltFx], [TitleWordmark], … Top-level `internal` in `system.display`, so call sites use
 * them unqualified (matching [glsl]). Each used to be a private copy per module.
 */

internal fun sub(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[0] - b[0], a[1] - b[1], a[2] - b[2])
internal fun add(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[0] + b[0], a[1] + b[1], a[2] + b[2])
internal fun scale(a: DoubleArray, s: Double) = doubleArrayOf(a[0] * s, a[1] * s, a[2] * s)
internal fun dot(a: DoubleArray, b: DoubleArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
internal fun lenSq(a: DoubleArray) = dot(a, a)
internal fun cross(a: DoubleArray, b: DoubleArray) = doubleArrayOf(
    a[1] * b[2] - a[2] * b[1],
    a[2] * b[0] - a[0] * b[2],
    a[0] * b[1] - a[1] * b[0],
)

/** Unit vector along [a]; returns [fallback] for a (near-)zero-length input. */
internal fun norm(a: DoubleArray, fallback: DoubleArray = doubleArrayOf(0.0, 1.0, 0.0)): DoubleArray {
    val l = sqrt(lenSq(a))
    return if (l < 1e-9) fallback else scale(a, 1.0 / l)
}

internal fun lerp(a: DoubleArray, b: DoubleArray, t: Double) = doubleArrayOf(
    a[0] + (b[0] - a[0]) * t,
    a[1] + (b[1] - a[1]) * t,
    a[2] + (b[2] - a[2]) * t,
)

internal fun lerp1(a: Double, b: Double, t: Double) = a + (b - a) * t
internal fun dist(a: DoubleArray, b: DoubleArray) = sqrt(lenSq(sub(a, b)))

internal fun rotate(v: DoubleArray, axis: DoubleArray, angle: Double): DoubleArray {
    val c = cos(angle)
    val s = sin(angle)
    return add(add(scale(v, c), scale(cross(axis, v), s)), scale(axis, dot(axis, v) * (1.0 - c)))
}
