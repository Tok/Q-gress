package system.grid

import World
import config.Config
import extension.VectorField
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import util.Profiler
import util.data.Pos
import util.data.toShadow

/**
 * The frame-yielding **async** wrapper around the pure [Pathfinding] core — jsMain only, because the live
 * browser world-gen must cooperatively yield (`delay(0)`) so filling a full-grid flow field never freezes
 * rendering. Reuses [Pathfinding]'s `internal` flat-array helpers so the heat/vector/smooth math has one home;
 * only the yield cadence differs from [Pathfinding.computeFieldSync].
 */
object PathfindingAsync {
    /** Async (browser): yields to the frame between chunks so a full-grid fill never freezes rendering. */
    fun computeFieldAsync(destination: Pos, onReady: (VectorField) -> Unit) {
        MainScope().launch {
            val start = Profiler.nowMs()
            val cells = Pathfinding.Cells(World.grid)
            val heat = generateHeat(cells, destination)
            val (re, im) = Pathfinding.buildVectors(cells, heat, destination)
            val rawRe = re.copyOf()
            val rawIm = im.copyOf()
            smooth(cells, re, im, Config.vectorSmoothCount)
            Pathfinding.deWhirl(re, im, rawRe, rawIm)
            val field = Pathfinding.emit(cells, re, im)
            Profiler.addFieldMs(Profiler.nowMs() - start)
            onReady(field)
        }
    }

    // The yielding twin of Pathfinding.generateHeatSync: same bucketed Dijkstra, cooperatively yielding.
    private suspend fun generateHeat(cells: Pathfinding.Cells, goal: Pos): IntArray {
        val heat = IntArray(cells.w * cells.h) { Pathfinding.UNREACHED }
        val buckets = HashMap<Int, MutableList<Int>>()
        val start = goal.toShadow()
        val startIdx = cells.idx(start.x.toInt(), start.y.toInt())
        if (startIdx < 0) return heat // goal outside the grid → no field
        heat[startIdx] = 0
        buckets[0] = mutableListOf(startIdx)
        var wave = 0
        var sinceYield = 0
        while (buckets.isNotEmpty()) {
            for (i in buckets.remove(wave) ?: emptyList()) {
                sinceYield += Pathfinding.expand(cells, heat, buckets, i, wave)
                if (sinceYield >= Pathfinding.YIELD_EVERY_CELLS) {
                    sinceYield = 0
                    delay(0)
                }
            }
            wave++
        }
        return heat
    }

    // The yielding twin of Pathfinding.smoothSync: yield between smoothing passes.
    private suspend fun smooth(cells: Pathfinding.Cells, re: DoubleArray, im: DoubleArray, passes: Int) {
        repeat(passes) {
            Pathfinding.blur(cells, re, im)
            delay(0)
        }
    }
}
