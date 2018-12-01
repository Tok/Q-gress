package util

import World
import config.Config
import util.data.Cell
import util.data.Complex
import util.data.Coords
import kotlin.math.max

object PathUtil {
    const val MIN_HEAT = 35
    const val MAX_HEAT = 100

    private fun calcPosCost(pos: Coords, heat: Int) =
            heat + (World.grid[pos]?.movementPenalty ?: MAX_HEAT)

    private fun posToCost(positions: Set<Coords>, heat: Int) =
            positions.map { pos -> pos to calcPosCost(pos, heat) }.toMap()

    private fun mergeMaps(maps: Set<Map<Coords, Int>>) =
            maps.flatMap { m -> m.map { it.key to it.value } }.toMap()

    private fun findSuccessors(currentMap: Map<Coords, Int>,
                               passable: Map<Coords, Cell>,
                               sameHeat: Set<Coords>, heat: Int) =
            sameHeat.map { pos ->
                val successors = findUnmarkedSurrounding(pos, passable, currentMap)
                posToCost(successors, heat) to successors.isNotEmpty()
            }.toMap()

    private fun calcFront(currentMap: Map<Coords, Int>, passable: Map<Coords, Cell>,
                          sameHeat: Set<Coords>, heat: Int): Pair<Map<Coords, Int>, Boolean> {
        val result = findSuccessors(currentMap, passable, sameHeat, heat)
        val front = mergeMaps(result.keys)
        val hasMore = result.values.contains(true)
        return front to hasMore
    }

    private fun createWaveFront(currentHeatMap: Map<Coords, Int>, passable: Map<Coords, Cell>,
                                heat: Int): Pair<Map<Coords, Int>, Boolean> {
        val sameHeat: Map<Coords, Int> = currentHeatMap.filter { it.value == heat }
        val (layer, hasMaybeMore) = calcFront(currentHeatMap, passable, sameHeat.keys, heat)
        return layer to hasMaybeMore
    }

    fun generateHeatMap(goal: Coords): Map<Coords, Int> {
        val passable = World.passableCells()
        var heat = 0
        var maxHeat = 0
        val map = mutableMapOf<Coords, Int>()
        map[goal.toShadowPos()] = heat
        while (true) {
            val (layer, hasMaybeMore) = createWaveFront(map, passable, heat++)
            map.putAll(layer)
            val layerMax = (layer.map { it.value }.max() ?: 0)
            maxHeat = max(maxHeat, layerMax)
            val overCount = heat - maxHeat
            val hasMore = hasMaybeMore || overCount < MAX_HEAT
            if (!hasMore) {
                break
            }
        }
        return map
    }

    private fun createVec(heatMap: Map<Coords, Int>, maxHeat: Int, destination: Coords, pos: Coords): Complex {
        val left = heatMap[Coords(pos.x - 1, pos.y)] ?: maxHeat
        val right = heatMap[Coords(pos.x + 1, pos.y)] ?: maxHeat
        val up = heatMap[Coords(pos.x, pos.y - 1)] ?: maxHeat
        val down = heatMap[Coords(pos.x, pos.y + 1)] ?: maxHeat
        val lr = left - right
        val ud = up - down
        val isBlocked = lr == 0 && ud == 0
        return if (!isBlocked) {
            Complex(lr, ud)
        } else {
            val xDiff = destination.x - pos.x
            val yDiff = destination.y - pos.y
            Complex(xDiff, yDiff)
        }
    }

    fun calculateVectorField(heatMap: Map<Coords, Int>, destination: Coords): Map<Coords, Complex> {
        val maxHeat = heatMap.values.max()!!
        val fields = World.grid.map {
            val raw = createVec(heatMap, maxHeat, destination, it.key)
            val vec = raw.copyWithNewMagnitude(1.0) //FIXME use terrain penalty
            it.key to vec
        }.toMap()
        return smooth(fields, Config.vectorSmoothCount).toMap()
    }

    private fun smooth(map: Map<Coords, Complex>, count: Int): Map<Coords, Complex> =
            if (count > 0) {
                smooth(smoothVectorMap(map), count - 1)
            } else {
                map
            }

    private fun smoothVectorMap(map: Map<Coords, Complex>): Map<Coords, Complex> {
        val n = 1
        val xRange = -n..n
        val yRange = -n..n
        return map.map {
            val pos = it.key
            val sum: Complex = yRange.flatMap { dy ->
                xRange.map { dx ->
                    map[Coords(pos.x + dx, pos.y + dy)] ?: Complex.ZERO
                }
            }.fold(Complex.ZERO) { acc, complex -> acc.plus(complex) }
            val magnitude = sum.magnitude / (xRange.count() * yRange.count())
            val phase = sum.phase
            it.key to Complex.fromMagnitudeAndPhase(magnitude, phase) //FIXME use terrain penalty
        }.toMap()
    }

    private fun findUnmarkedSurrounding(node: Coords, passable: Map<Coords, Cell>,
                                        heatMap: Map<Coords, Int>): Set<Coords> =
            findAllSurrounding(node)
                    .filterNot { heatMap.containsKey(it) }
                    .filter { passable.containsKey(it) }
                    .toSet()

    private fun findAllSurrounding(node: Coords): List<Coords> {
        return listOfNotNull(
                Coords(node.x - 1, node.y - 1),
                Coords(node.x - 1, node.y),
                Coords(node.x - 1, node.y + 1),
                Coords(node.x, node.y - 1),
                Coords(node.x, node.y + 1),
                Coords(node.x + 1, node.y - 1),
                Coords(node.x + 1, node.y),
                Coords(node.x + 1, node.y + 1))
    }
}
