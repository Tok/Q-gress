package system.grid
import World
import config.Config
import config.Sim
import extension.Grid
import extension.VectorField
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import util.Profiler
import util.data.*
import kotlin.math.sqrt

/**
 * Flow-field pathfinding. The heavy world-gen + per-tick math runs over FLAT arrays indexed by a packed cell
 * index (origin at the grid's bounding-box corner, row stride [Cells.w]) — not `Pos`-keyed HashMaps of boxed
 * `Complex`. The phase-D profile showed the old Map/Complex version dominating world-gen (per-cell `Pos`
 * allocation ×8 in the Dijkstra and ×9 per smoothing pass, plus `equals`/`hashCode`/`get` churn). The output
 * is still a [VectorField] (`Map<Pos, Complex>`) so consumers are unchanged; only the internals are flat.
 */
object Pathfinding {
    const val MIN_HEAT = 35
    const val MAX_HEAT = 100
    private const val MIN_SPEED_FACTOR = 0.45 // slowest terrain still moves at 45% (so agents never stall)
    private const val YIELD_EVERY_CELLS = 2000 // cooperative yield cadence while filling the field (async path)
    private const val UNREACHED = -1

    // The 8 neighbour offsets (reused, not re-allocated per cell).
    private val NEIGHBOURS = arrayOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)

    /**
     * The grid as flat per-cell arrays over its bounding box. Replaces the `Pos`-keyed HashMaps: `idx(x,y)`
     * maps a shadow cell to a packed index (or -1 outside the box), and [passable]/[penalty] are read in O(1).
     */
    private class Cells(grid: Grid) {
        val minX: Int
        val minY: Int
        val w: Int
        val h: Int
        val passable: BooleanArray
        val penalty: IntArray

        init {
            var mnx = Int.MAX_VALUE
            var mny = Int.MAX_VALUE
            var mxx = Int.MIN_VALUE
            var mxy = Int.MIN_VALUE
            grid.keys.forEach {
                val x = it.x.toInt()
                val y = it.y.toInt()
                if (x < mnx) mnx = x
                if (x > mxx) mxx = x
                if (y < mny) mny = y
                if (y > mxy) mxy = y
            }
            minX = mnx
            minY = mny
            w = mxx - mnx + 1
            h = mxy - mny + 1
            passable = BooleanArray(w * h)
            penalty = IntArray(w * h) { MAX_HEAT }
            grid.forEach { (pos, cell) ->
                val i = idx(pos.x.toInt(), pos.y.toInt())
                if (i >= 0) {
                    passable[i] = cell.isPassable
                    penalty[i] = cell.movementPenalty
                }
            }
        }

        fun idx(x: Int, y: Int): Int = if (x in minX until minX + w && y in minY until minY + h) (y - minY) * w + (x - minX) else -1
    }

    // --- public entry points -----------------------------------------------------------------------------

    /** Async (browser): yields to the frame between chunks so a full-grid fill never freezes rendering. */
    fun computeFieldAsync(destination: Pos, onReady: (VectorField) -> Unit) {
        MainScope().launch {
            val start = Profiler.nowMs()
            val cells = Cells(World.grid)
            val heat = generateHeat(cells, destination, yielding = true)
            val (re, im) = buildVectors(cells, heat, destination)
            smooth(cells, re, im, Config.vectorSmoothCount, yielding = true)
            val field = emit(cells, re, im)
            Profiler.addFieldMs(Profiler.nowMs() - start)
            onReady(field)
        }
    }

    /** Synchronous twin (headless matches / Node): identical result, no coroutine, no frame-yielding. */
    fun computeFieldSync(destination: Pos): VectorField {
        val cells = Cells(World.grid)
        val heat = generateHeatSync(cells, destination)
        val (re, im) = buildVectors(cells, heat, destination)
        smoothSync(cells, re, im, Config.vectorSmoothCount)
        return emit(cells, re, im)
    }

    // --- heat (bucketed Dijkstra over flat indices) -------------------------------------------------------
    // Each cell's cost = the wave it's reached on + its terrain movement penalty (high penalty reads as
    // "farther"). A frontier bucket per cost level → each cell is set exactly once (O(cells)).

    private suspend fun generateHeat(cells: Cells, goal: Pos, yielding: Boolean): IntArray {
        val heat = IntArray(cells.w * cells.h) { UNREACHED }
        val buckets = HashMap<Int, MutableList<Int>>()
        val start = goal.toShadow()
        val startIdx = cells.idx(start.x.toInt(), start.y.toInt())
        if (startIdx < 0) return heat // goal outside the grid → no field (all cells fall back below)
        heat[startIdx] = 0
        buckets[0] = mutableListOf(startIdx)
        var wave = 0
        var sinceYield = 0
        while (buckets.isNotEmpty()) {
            for (i in buckets.remove(wave) ?: emptyList()) {
                sinceYield += expand(cells, heat, buckets, i, wave)
                if (yielding && sinceYield >= YIELD_EVERY_CELLS) {
                    sinceYield = 0
                    delay(0)
                }
            }
            wave++
        }
        return heat
    }

    private fun generateHeatSync(cells: Cells, goal: Pos): IntArray {
        val heat = IntArray(cells.w * cells.h) { UNREACHED }
        val buckets = HashMap<Int, MutableList<Int>>()
        val start = goal.toShadow()
        val startIdx = cells.idx(start.x.toInt(), start.y.toInt())
        if (startIdx < 0) return heat
        heat[startIdx] = 0
        buckets[0] = mutableListOf(startIdx)
        var wave = 0
        while (buckets.isNotEmpty()) {
            for (i in buckets.remove(wave) ?: emptyList()) expand(cells, heat, buckets, i, wave)
            wave++
        }
        return heat
    }

    // Mark + bucket cell [i]'s unreached passable neighbours at their cost; returns how many were added.
    private fun expand(cells: Cells, heat: IntArray, buckets: HashMap<Int, MutableList<Int>>, i: Int, wave: Int): Int {
        val x = cells.minX + i % cells.w
        val y = cells.minY + i / cells.w
        var added = 0
        for ((dx, dy) in NEIGHBOURS) {
            val ni = cells.idx(x + dx, y + dy)
            if (ni >= 0 && heat[ni] == UNREACHED && cells.passable[ni]) {
                val cost = wave + cells.penalty[ni]
                heat[ni] = cost
                buckets.getOrPut(cost) { mutableListOf() }.add(ni)
                added++
            }
        }
        return added
    }

    // --- raw vectors (gradient of the heat field, scaled by terrain speed) --------------------------------

    private fun buildVectors(cells: Cells, heat: IntArray, destination: Pos): Pair<DoubleArray, DoubleArray> {
        val n = cells.w * cells.h
        val re = DoubleArray(n)
        val im = DoubleArray(n)
        for (i in 0 until n) {
            val x = cells.minX + i % cells.w
            val y = cells.minY + i / cells.w
            val lr = heatAt(cells, heat, x - 1, y) - heatAt(cells, heat, x + 1, y)
            val ud = heatAt(cells, heat, x, y - 1) - heatAt(cells, heat, x, y + 1)
            val rawX: Double
            val rawY: Double
            if (lr != 0 || ud != 0) {
                rawX = lr.toDouble()
                rawY = ud.toDouble()
            } else {
                // Blocked / unreached: outside the play area, point at the field centre (a tidy flash default);
                // inside, fall back toward the destination (matches the old createVec).
                val sim = Pos(x.toDouble(), y.toDouble()).fromShadow()
                if (!Sim.isInPlayArea(sim.x, sim.y)) {
                    rawX = Sim.width / 2.0 - sim.x
                    rawY = Sim.height / 2.0 - sim.y
                } else {
                    rawX = destination.x - x
                    rawY = destination.y - y
                }
            }
            // Scale to the terrain speed factor (= old raw.copyWithNewMagnitude(speed): magnitude speed, same
            // direction). MIN_HEAT cell → full speed, high penalty → slower; a zero raw → (speed, 0) like
            // Complex.ZERO.copyWithNewMagnitude(speed) did.
            val speed = (MIN_HEAT.toDouble() / cells.penalty[i]).coerceIn(MIN_SPEED_FACTOR, 1.0)
            val mag = sqrt(rawX * rawX + rawY * rawY)
            if (mag > 0.0) {
                re[i] = rawX * speed / mag
                im[i] = rawY * speed / mag
            } else {
                re[i] = speed
                im[i] = 0.0
            }
        }
        return re to im
    }

    // Heat at a cell, or [MAX_HEAT] when off-grid or unreached (matches the old `heatMap[pos] ?: maxHeat`,
    // with maxHeat ≡ MAX_HEAT — the cost ceiling unreached cells already carry).
    private fun heatAt(cells: Cells, heat: IntArray, x: Int, y: Int): Int {
        val i = cells.idx(x, y)
        return if (i >= 0 && heat[i] != UNREACHED) heat[i] else MAX_HEAT
    }

    // --- smoothing (3×3 box blur — averaging the 9 neighbour vectors, missing ones count as zero) ----------

    private suspend fun smooth(cells: Cells, re: DoubleArray, im: DoubleArray, passes: Int, yielding: Boolean) {
        repeat(passes) {
            blur(cells, re, im)
            if (yielding) delay(0) // yield between smooth passes
        }
    }

    private fun smoothSync(cells: Cells, re: DoubleArray, im: DoubleArray, passes: Int) {
        repeat(passes) { blur(cells, re, im) }
    }

    private fun blur(cells: Cells, re: DoubleArray, im: DoubleArray) {
        val n = cells.w * cells.h
        val outRe = DoubleArray(n)
        val outIm = DoubleArray(n)
        for (i in 0 until n) {
            val x = cells.minX + i % cells.w
            val y = cells.minY + i / cells.w
            var sr = 0.0
            var si = 0.0
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val ni = cells.idx(x + dx, y + dy)
                    if (ni >= 0) {
                        sr += re[ni]
                        si += im[ni]
                    }
                }
            }
            outRe[i] = sr / 9.0 // always /9 (a 3×3 window), so edge cells with missing neighbours dampen — as before
            outIm[i] = si / 9.0
        }
        outRe.copyInto(re)
        outIm.copyInto(im)
    }

    // --- emit the field: wrap the flat arrays directly (no per-cell Map/Complex) --------------------------

    private fun emit(cells: Cells, re: DoubleArray, im: DoubleArray): VectorField =
        VectorField(cells.minX, cells.minY, cells.w, cells.h, re, im)
}
