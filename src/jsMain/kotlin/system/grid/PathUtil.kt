package system.grid
import World
import config.Config
import config.Sim
import extension.Grid
import extension.GridMap
import extension.VectorField
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import util.data.*

object PathUtil {
    const val MIN_HEAT = 35
    const val MAX_HEAT = 100
    private const val MIN_SPEED_FACTOR = 0.45 // slowest terrain still moves at 45% (so agents never stall)
    private const val YIELD_EVERY_CELLS = 2000 // cooperative yield cadence while filling the vector field

    // Cost field by bucketed Dijkstra: each cell's cost = the wave it's reached on + its terrain
    // movement penalty, so high-penalty ground reads as "farther". A frontier bucket per cost level
    // means each cell is touched once (O(cells)) instead of re-scanning the whole visited map every
    // wavefront. suspend: yields (delay(0) → macrotask, so the browser can render) every
    // ~YIELD_EVERY_CELLS cells, so a full-grid fill stays a background task and never freezes the frame.
    suspend fun generateHeatMap(goal: Pos): GridMap {
        val passable = World.passableCells()
        val map = mutableMapOf<Pos, Int>()
        val buckets = mutableMapOf<Int, MutableList<Pos>>() // cost level → cells awaiting expansion
        val start = goal.toShadow()
        map[start] = 0
        buckets[0] = mutableListOf(start)
        var heat = 0
        var sinceYield = 0
        while (buckets.isNotEmpty()) {
            for (pos in buckets.remove(heat) ?: emptyList()) {
                sinceYield += expand(pos, heat, map, buckets, passable)
                if (sinceYield >= YIELD_EVERY_CELLS) {
                    sinceYield = 0
                    delay(0)
                }
            }
            heat++
        }
        return map
    }

    // Mark + bucket pos's unvisited passable neighbours at their cost (wave + terrain penalty); returns
    // how many were added (so the caller can pace its cooperative yields).
    private fun expand(pos: Pos, heat: Int, map: MutableMap<Pos, Int>, buckets: MutableMap<Int, MutableList<Pos>>, passable: Grid): Int {
        var added = 0
        for (next in findAllSurrounding(pos)) {
            if (map.containsKey(next) || !passable.containsKey(next)) continue
            val cost = heat + (World.grid[next]?.movementPenalty ?: MAX_HEAT)
            map[next] = cost
            if (cost > heat) buckets.getOrPut(cost) { mutableListOf() }.add(next)
            added++
        }
        return added
    }

    // Launch the full heat-map + vector-field computation off the synchronous path. It runs on the
    // JS event loop yielding between chunks, then hands the finished field back via [onReady].
    fun computeFieldAsync(destination: Pos, onReady: (VectorField) -> Unit) {
        MainScope().launch {
            val heatMap = generateHeatMap(destination)
            onReady(calculateVectorField(heatMap, destination))
        }
    }

    // The synchronous twin of [computeFieldAsync]: same bucketed-Dijkstra heat map + vector field +
    // smoothing, computed inline and returned (no coroutine, no frame-yielding `delay(0)`). For headless
    // matches (the SimRunner / Node) where there's no frame to keep responsive and a synchronous tick
    // loop can't drive `MainScope`. Same result as the async path (shares [expand]/[createVec]/
    // [smoothVectorMap]) → deterministic. Browser keeps the async path so the frame never stalls.
    fun computeFieldSync(destination: Pos): VectorField = calculateVectorFieldSync(generateHeatMapSync(destination), destination)

    private fun generateHeatMapSync(goal: Pos): GridMap {
        val passable = World.passableCells()
        val map = mutableMapOf<Pos, Int>()
        val buckets = mutableMapOf<Int, MutableList<Pos>>()
        val start = goal.toShadow()
        map[start] = 0
        buckets[0] = mutableListOf(start)
        var heat = 0
        while (buckets.isNotEmpty()) {
            for (pos in buckets.remove(heat) ?: emptyList()) {
                expand(pos, heat, map, buckets, passable)
            }
            heat++
        }
        return map
    }

    private fun calculateVectorFieldSync(heatMap: GridMap, destination: Pos): VectorField {
        val maxHeat = heatMap.values.max()
        val fields = mutableMapOf<Pos, Complex>()
        World.grid.forEach { (pos, cell) ->
            val raw = createVec(heatMap, maxHeat, destination, pos)
            val speedFactor = (MIN_HEAT.toDouble() / cell.movementPenalty).coerceIn(MIN_SPEED_FACTOR, 1.0)
            fields[pos] = raw.copyWithNewMagnitude(speedFactor)
        }
        return smoothSync(fields, Config.vectorSmoothCount)
    }

    private fun smoothSync(vectors: VectorField, count: Int): VectorField =
        if (count > 0) smoothSync(smoothVectorMap(vectors), count - 1) else vectors

    private fun createVec(heatMap: GridMap, maxHeat: Int, destination: Pos, pos: Pos): Complex {
        val left = heatMap[Pos(pos.x - 1, pos.y)] ?: maxHeat
        val right = heatMap[Pos(pos.x + 1, pos.y)] ?: maxHeat
        val up = heatMap[Pos(pos.x, pos.y - 1)] ?: maxHeat
        val down = heatMap[Pos(pos.x, pos.y + 1)] ?: maxHeat
        val lr = left - right
        val ud = up - down
        val isBlocked = lr == 0 && ud == 0
        if (!isBlocked) return Complex(lr, ud)
        // Blocked/unreached cell: outside the play area, point at the playfield centre (a tidy default
        // for the brief flow-field flash); inside, fall back toward the destination.
        val sim = pos.fromShadow()
        return if (!Sim.isInPlayArea(sim.x, sim.y)) {
            Complex(Sim.width / 2.0 - sim.x, Sim.height / 2.0 - sim.y)
        } else {
            Complex(destination.x - pos.x, destination.y - pos.y)
        }
    }

    suspend fun calculateVectorField(heatMap: GridMap, destination: Pos): VectorField {
        val maxHeat = heatMap.values.max()
        val fields = mutableMapOf<Pos, Complex>()
        var processed = 0
        World.grid.forEach { (pos, cell) ->
            val raw = createVec(heatMap, maxHeat, destination, pos)
            // Flow magnitude scales with terrain: agents move slower over rough ground (forest >
            // grass > concrete), not just route around it. MIN_HEAT cell → full speed, high penalty → slow.
            val speedFactor = (MIN_HEAT.toDouble() / cell.movementPenalty).coerceIn(MIN_SPEED_FACTOR, 1.0)
            fields[pos] = raw.copyWithNewMagnitude(speedFactor)
            if (++processed % YIELD_EVERY_CELLS == 0) delay(0) // yield every ~2000 cells
        }
        return smooth(fields, Config.vectorSmoothCount)
    }

    private suspend fun smooth(vectors: VectorField, count: Int): VectorField = if (count > 0) {
        delay(0) // yield between smooth passes
        smooth(smoothVectorMap(vectors), count - 1)
    } else {
        vectors
    }

    private fun smoothVectorMap(vectors: VectorField): VectorField {
        val n = 1
        val xRange = -n..n
        val yRange = -n..n
        return vectors.map {
            val pos = it.key
            val sum: Complex = yRange.flatMap { dy ->
                xRange.map { dx ->
                    vectors[Pos(pos.x + dx, pos.y + dy)] ?: Complex.ZERO
                }
            }.fold(Complex.ZERO) { acc, complex -> acc.plus(complex) }
            val magnitude = sum.magnitude / (xRange.count() * yRange.count())
            val phase = sum.phase
            it.key to Complex.fromMagnitudeAndPhase(magnitude, phase) // FIXME use terrain penalty
        }.toMap()
    }

    private fun findAllSurrounding(node: Pos): List<Pos> = listOfNotNull(
        Pos(node.x - 1, node.y - 1),
        Pos(node.x - 1, node.y),
        Pos(node.x - 1, node.y + 1),
        Pos(node.x, node.y - 1),
        Pos(node.x, node.y + 1),
        Pos(node.x + 1, node.y - 1),
        Pos(node.x + 1, node.y),
        Pos(node.x + 1, node.y + 1),
    )
}
