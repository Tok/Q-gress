package extension

import util.data.Complex
import util.data.Pos

/**
 * A flow field over a rectangular cell region, stored as flat re/im [DoubleArray]s indexed by a packed cell
 * index (origin [minX], [minY]; row stride [w]) — replacing a `Pos`-keyed HashMap of boxed [Complex]. [get] is
 * an O(1) array read (no hashing, no key boxing), and [system.grid.Pathfinding] builds it directly from its
 * flat compute (no per-cell Map/Complex). Agents read it every tick, so the flat layout trims runtime cost
 * too, not just world-gen.
 */
class VectorField(val minX: Int, val minY: Int, val w: Int, val h: Int, private val re: DoubleArray, private val im: DoubleArray) {
    /** The flow vector at shadow cell [pos], or null outside the field. Allocates one Complex (callers want one). */
    operator fun get(pos: Pos): Complex? {
        val x = pos.x.toInt()
        val y = pos.y.toInt()
        if (x !in minX until minX + w || y !in minY until minY + h) return null
        val i = (y - minY) * w + (x - minX)
        return Complex(re[i], im[i])
    }

    fun isEmpty() = re.isEmpty()
    fun isNotEmpty() = re.isNotEmpty()

    /** True iff [predicate] holds for every cell's vector. */
    fun all(predicate: (Complex) -> Boolean): Boolean = re.indices.all { predicate(Complex(re[it], im[it])) }

    /** Visit every cell's (position, vector) — for the flow-field overlay render. */
    fun forEach(action: (Pos, Complex) -> Unit) {
        for (i in re.indices) {
            val x = minX + i % w
            val y = minY + i / w
            action(Pos(x.toDouble(), y.toDouble()), Complex(re[i], im[i]))
        }
    }

    override fun equals(other: Any?): Boolean = other is VectorField &&
        minX == other.minX &&
        minY == other.minY &&
        w == other.w &&
        h == other.h &&
        re.contentEquals(other.re) &&
        im.contentEquals(other.im)

    override fun hashCode(): Int = ((((minX * 31 + minY) * 31 + w) * 31 + h) * 31 + re.contentHashCode())

    companion object {
        /** An empty field (a freshly-created portal before its field computes). [get] returns null for any cell. */
        val EMPTY = VectorField(0, 0, 0, 0, DoubleArray(0), DoubleArray(0))
    }
}
