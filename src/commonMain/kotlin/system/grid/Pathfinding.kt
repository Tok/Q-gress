package system.grid
import World
import config.Config
import config.Sim
import extension.Grid
import extension.VectorField
import util.data.*
import kotlin.math.sqrt

/**
 * Flow-field pathfinding — the **pure, synchronous** core. The heavy world-gen + per-tick math runs over FLAT
 * arrays indexed by a packed cell index (origin at the grid's bounding-box corner, row stride [Cells.w]) — not
 * `Pos`-keyed HashMaps of boxed `Complex`. The output is a [VectorField] so consumers are unchanged; only the
 * internals are flat. This lives in `commonMain` so headless matches ([ai.SimRunner]) + the JVM tests compute
 * fields with no coroutine. The frame-yielding **async** wrapper for the live browser world-gen is the jsMain
 * [PathfindingAsync], which reuses the `internal` helpers here.
 */
object Pathfinding {
    const val MIN_HEAT = 35
    const val MAX_HEAT = 100

    // How much worse a wall/unreached cell reads than the passable cell beside it, so the heat gradient always
    // points AWAY from walls — at any distance from the goal. Must be RELATIVE (here + this), not a fixed
    // ceiling: accumulated heat grows ≥ MIN_HEAT per cell, so cells far from the goal sit in the thousands and a
    // fixed-100 wall would read as the LOWEST (most attractive) heat around → the gradient would steer agents
    // straight INTO walls (where the passability clamp then traps them). One max-penalty-cell of repulsion.
    private const val WALL_REPULSION = MAX_HEAT
    private const val MIN_SPEED_FACTOR = 0.45 // slowest terrain still moves at 45% (so agents never stall)

    // Curl guard: the lowest a smoothed vector's alignment with the raw (curl-free) downhill gradient may fall.
    // ≥ 0 means heat strictly decreases every step, so the field can't form a closed loop — no whirls. 0.3 keeps
    // the smoothing's gentle arcs wherever it's benign, only correcting cells the blur rotated toward "uphill".
    private const val MIN_DOWNHILL = 0.3
    private const val EPS = 1e-9
    internal const val YIELD_EVERY_CELLS = 2000 // cooperative yield cadence while filling the field (async path)
    internal const val UNREACHED = -1

    // The 8 neighbour offsets (reused, not re-allocated per cell).
    private val NEIGHBOURS = arrayOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)

    /**
     * The grid as flat per-cell arrays over its bounding box. Replaces the `Pos`-keyed HashMaps: `idx(x,y)`
     * maps a shadow cell to a packed index (or -1 outside the box), and [passable]/[penalty] are read in O(1).
     */
    internal class Cells(grid: Grid) {
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

    // --- public entry point ------------------------------------------------------------------------------

    /** Compute the flow field to [destination] inline (no coroutine, no frame-yielding). The jsMain
     *  [PathfindingAsync.computeFieldAsync] is the frame-yielding twin for the live browser world-gen. */
    fun computeFieldSync(destination: Pos): VectorField {
        val cells = Cells(World.grid)
        val heat = generateHeatSync(cells, destination)
        val (re, im) = buildVectors(cells, heat, destination)
        val rawRe = re.copyOf()
        val rawIm = im.copyOf()
        smoothSync(cells, re, im, Config.vectorSmoothCount)
        deWhirl(re, im, rawRe, rawIm)
        return emit(cells, re, im)
    }

    // --- heat (bucketed Dijkstra over flat indices) -------------------------------------------------------
    // Each cell's cost = the wave it's reached on + its terrain movement penalty (high penalty reads as
    // "farther"). A frontier bucket per cost level → each cell is set exactly once (O(cells)).

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
    internal fun expand(cells: Cells, heat: IntArray, buckets: HashMap<Int, MutableList<Int>>, i: Int, wave: Int): Int {
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

    internal fun buildVectors(cells: Cells, heat: IntArray, destination: Pos): Pair<DoubleArray, DoubleArray> {
        val n = cells.w * cells.h
        val re = DoubleArray(n)
        val im = DoubleArray(n)
        for (i in 0 until n) {
            val x = cells.minX + i % cells.w
            val y = cells.minY + i / cells.w
            // Walls/unreached neighbours read as THIS cell's heat + a repulsion margin (always "worse than
            // here"), so the gradient points away from them whether the cell is near the goal or deep in the map.
            val wallHeat = (if (heat[i] != UNREACHED) heat[i] else 0) + WALL_REPULSION
            val lr = heatAt(cells, heat, x - 1, y, wallHeat) - heatAt(cells, heat, x + 1, y, wallHeat)
            val ud = heatAt(cells, heat, x, y - 1, wallHeat) - heatAt(cells, heat, x, y + 1, wallHeat)
            val rawX: Double
            val rawY: Double
            if (lr != 0 || ud != 0) {
                rawX = lr.toDouble()
                rawY = ud.toDouble()
            } else {
                val fb = fallbackVector(cells, heat, i, x, y, destination)
                rawX = fb.first
                rawY = fb.second
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

    // The direction to use when the heat gradient is flat (all four axis-neighbours read equal). Outside the play
    // area → point at the field centre (a tidy flash default). Inside: a REACHED cell has a route, so fall back
    // toward the destination (matches the old createVec); an UNREACHED cell — a passable pocket the flood never
    // reached (e.g. a region the round-mask sealed off from the goal) — must NOT aim at the destination through the
    // wall, or the agent pins against it and reads as stuck. Repel it from adjacent walls so it drifts into open
    // space instead (fix #2, connectivity, shrinks how often a cell is unreached in the first place).
    private fun fallbackVector(cells: Cells, heat: IntArray, i: Int, x: Int, y: Int, destination: Pos): Pair<Double, Double> {
        val sim = Pos(x.toDouble(), y.toDouble()).fromShadow()
        return when {
            !Sim.isInPlayArea(sim.x, sim.y) -> (Sim.width / 2.0 - sim.x) to (Sim.height / 2.0 - sim.y)
            heat[i] != UNREACHED -> (destination.x - x) to (destination.y - y)
            else -> wallRepulsion(cells, x, y)
        }
    }

    // Sum of unit pushes AWAY from each adjacent wall / off-grid cell (8-neighbourhood) — the degenerate-gradient
    // safety net for an unreached pocket, so an agent there drifts into open space instead of into a wall. A cell
    // with no wall neighbour nets ~zero (it simply idles rather than driving into anything).
    private fun wallRepulsion(cells: Cells, x: Int, y: Int): Pair<Double, Double> {
        var wx = 0.0
        var wy = 0.0
        for ((dx, dy) in NEIGHBOURS) {
            val ni = cells.idx(x + dx, y + dy)
            if (ni < 0 || !cells.passable[ni]) {
                wx -= dx.toDouble()
                wy -= dy.toDouble()
            }
        }
        return wx to wy
    }

    // Heat at a cell, or [wallHeat] (the caller's own heat + a repulsion margin) when off-grid or unreached, so
    // a wall always reads worse than the cell beside it → the gradient steers away from it everywhere.
    private fun heatAt(cells: Cells, heat: IntArray, x: Int, y: Int, wallHeat: Int): Int {
        val i = cells.idx(x, y)
        return if (i >= 0 && heat[i] != UNREACHED) heat[i] else wallHeat
    }

    // --- smoothing (3×3 box blur — averaging the 9 neighbour vectors, missing ones count as zero) ----------

    private fun smoothSync(cells: Cells, re: DoubleArray, im: DoubleArray, passes: Int) {
        repeat(passes) { blur(cells, re, im) }
    }

    internal fun blur(cells: Cells, re: DoubleArray, im: DoubleArray) {
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

    // --- curl guard (de-whirl) ----------------------------------------------------------------------------
    // The raw gradient is curl-free (gradient of the scalar heat) → following it always reaches the goal. The
    // box-blur smoothing, however, can rotate a cell's vector away from downhill, creating closed "whirls" that
    // trap orbiting agents. Restore a minimum downhill component (vs the saved RAW direction) in every cell, so
    // heat strictly decreases each step and no closed loop can exist — keeping the smoothed direction wherever
    // it's already downhill enough.
    internal fun deWhirl(re: DoubleArray, im: DoubleArray, rawRe: DoubleArray, rawIm: DoubleArray) {
        for (i in re.indices) {
            // buildVectors always sets a non-zero raw magnitude (>= MIN_SPEED_FACTOR), so rMag is safe to divide.
            val rMag = sqrt(rawRe[i] * rawRe[i] + rawIm[i] * rawIm[i])
            val rx = rawRe[i] / rMag
            val ry = rawIm[i] / rMag
            val sMag = sqrt(re[i] * re[i] + im[i] * im[i])
            if (sMag < EPS) { // smoothing cancelled the vector out → fall back to the raw downhill gradient
                re[i] = rawRe[i]
                im[i] = rawIm[i]
            } else {
                val downhill = re[i] * rx + im[i] * ry // the smoothed vector's component along the raw gradient
                val floor = MIN_DOWNHILL * sMag
                if (downhill < floor) { // rotated toward/past sideways → nudge back toward raw, keep the magnitude
                    val nx = re[i] + (floor - downhill) * rx
                    val ny = im[i] + (floor - downhill) * ry
                    val nMag = sqrt(nx * nx + ny * ny)
                    re[i] = nx / nMag * sMag
                    im[i] = ny / nMag * sMag
                }
            }
        }
    }

    // --- emit the field: wrap the flat arrays directly (no per-cell Map/Complex) --------------------------

    internal fun emit(cells: Cells, re: DoubleArray, im: DoubleArray): VectorField =
        VectorField(cells.minX, cells.minY, cells.w, cells.h, re, im)
}
