package util

import World
import config.Config
import config.Sim
import extension.Grid
import extension.GridMap
import extension.VectorField
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import util.data.Complex
import util.data.Pos
import kotlin.math.max

object PathUtil {
    const val MIN_HEAT = 35
    const val MAX_HEAT = 100
    private const val MIN_SPEED_FACTOR = 0.45 // slowest terrain still moves at 45% (so agents never stall)
    private const val YIELD_EVERY_CELLS = 2000 // cooperative yield cadence while filling the vector field

    private fun calcPosCost(pos: Pos, heat: Int) = heat + (World.grid[pos]?.movementPenalty ?: MAX_HEAT)

    private fun posToCost(positions: Set<Pos>, heat: Int) = positions.map { pos -> pos to calcPosCost(pos, heat) }.toMap()

    private fun mergeMaps(maps: Set<GridMap>) = maps.flatMap { m -> m.map { it.key to it.value } }.toMap()

    private fun findSuccessors(currentMap: GridMap, passable: Grid, sameHeat: Set<Pos>, heat: Int) = sameHeat.map { pos ->
        val successors = findUnmarkedSurrounding(pos, passable, currentMap)
        posToCost(successors, heat) to successors.isNotEmpty()
    }.toMap()

    private fun calcFront(
        currentMap: GridMap,
        passable: Grid,
        sameHeat: Set<Pos>,
        heat: Int,
    ): Pair<GridMap, Boolean> {
        val result = findSuccessors(currentMap, passable, sameHeat, heat)
        val front = mergeMaps(result.keys)
        val hasMore = result.values.contains(true)
        return front to hasMore
    }

    private fun createWaveFront(currentHeatMap: GridMap, passable: Grid, heat: Int): Pair<GridMap, Boolean> {
        val sameHeat: GridMap = currentHeatMap.filter { it.value == heat }
        val (layer, hasMaybeMore) = calcFront(currentHeatMap, passable, sameHeat.keys, heat)
        return layer to hasMaybeMore
    }

    // suspend: yields (delay(0) → setTimeout → macrotask, so the browser can render) after every
    // wavefront layer, turning this full-grid BFS from a ~1s thread freeze into a background fill.
    suspend fun generateHeatMap(goal: Pos): GridMap {
        val passable = World.passableCells()
        var heat = 0
        var maxHeat = 0
        val map = mutableMapOf<Pos, Int>()
        map[goal.toShadow()] = heat
        while (true) {
            val (layer, hasMaybeMore) = createWaveFront(map, passable, heat++)
            map.putAll(layer)
            val layerMax = (layer.map { it.value }.maxOrNull() ?: 0)
            maxHeat = max(maxHeat, layerMax)
            val overCount = heat - maxHeat
            val hasMore = hasMaybeMore || overCount < MAX_HEAT
            delay(0) // yield each wavefront layer
            if (!hasMore) {
                break
            }
        }
        return map
    }

    // Launch the full heat-map + vector-field computation off the synchronous path. It runs on the
    // JS event loop yielding between chunks, then hands the finished field back via [onReady].
    fun computeFieldAsync(destination: Pos, onReady: (VectorField) -> Unit) {
        MainScope().launch {
            val heatMap = generateHeatMap(destination)
            onReady(calculateVectorField(heatMap, destination))
        }
    }

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

    private fun findUnmarkedSurrounding(node: Pos, passable: Grid, heatMap: GridMap): Set<Pos> = findAllSurrounding(node)
        .filterNot { heatMap.containsKey(it) }
        .filter { passable.containsKey(it) }
        .toSet()

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
