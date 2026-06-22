package util

import extension.Grid
import util.data.Cell
import util.data.Pos

/**
 * A compact, committed snapshot of one preset's passability, for offline (Node) connectivity tests
 * that can't reach live map tiles. Only `isPassable` is stored (all the connectivity helpers need);
 * the off-screen ring is reconstructed as passable, matching MapUtil's grid builder.
 *
 * Captured **pre-[GridConnectivity.connectIslands]** (the raw screen-derived passability) so tests
 * run the real carve themselves and stay valid if the carve logic changes. Storage is run-length
 * encoding over the on-screen w×h cells (row-major), runs alternating starting from *impassable*.
 */
data class GridFixture(val preset: String, val w: Int, val h: Int, val off: Int, val rle: String) {
    /** Rebuild the full grid (on-screen cells from the RLE, off-screen ring forced passable). */
    fun toGrid(): Grid {
        val onScreen = rleDecode(rle, w * h)
        val map = HashMap<Pos, Cell>((w + 2 * off) * (h + 2 * off))
        for (y in -off until h + off) {
            for (x in -off until w + off) {
                val inBounds = x in 0 until w && y in 0 until h
                val passable = if (inBounds) onScreen[y * w + x] else true
                val pos = Pos(x.toDouble(), y.toDouble())
                map[pos] = Cell(pos, passable, if (passable) PASSABLE_PENALTY else IMPASSABLE_PENALTY)
            }
        }
        return map
    }

    /** Serialize to a Kotlin literal line for the committed fixtures file (see GridCapture). */
    fun toKotlin() = "GridFixture(\"$preset\", $w, $h, $off, \"$rle\")"

    companion object {
        private const val PASSABLE_PENALTY = 50
        private const val IMPASSABLE_PENALTY = 100

        /** Capture the on-screen passability of [grid] (use the raw, pre-carve grid). */
        fun fromGrid(preset: String, grid: Grid, w: Int, h: Int, off: Int): GridFixture {
            val bits = ArrayList<Boolean>(w * h)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    bits.add(grid[Pos(x.toDouble(), y.toDouble())]?.isPassable ?: false)
                }
            }
            return GridFixture(preset, w, h, off, rleEncode(bits))
        }

        /** Run-length encode [bits] as CSV run lengths, runs alternating from impassable (false). */
        fun rleEncode(bits: List<Boolean>): String {
            if (bits.isEmpty()) return ""
            val runs = mutableListOf<Int>()
            var current = false // the first run counts impassable cells (0 if the grid starts passable)
            var len = 0
            for (b in bits) {
                if (b == current) {
                    len++
                } else {
                    runs.add(len)
                    current = b
                    len = 1
                }
            }
            runs.add(len)
            return runs.joinToString(",")
        }

        fun rleDecode(rle: String, count: Int): List<Boolean> {
            val out = ArrayList<Boolean>(count)
            var current = false
            if (rle.isNotEmpty()) {
                rle.split(",").forEach { part ->
                    repeat(part.toInt()) { out.add(current) }
                    current = !current
                }
            }
            return out
        }
    }
}
