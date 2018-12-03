package util

import World
import config.Config
import util.data.Cell
import util.data.Complex
import util.data.Pos
import kotlin.math.max

object PathUtil {
    const val MIN_HEAT = 35
    const val MAX_HEAT = 100

    private fun calcPosCost(pos: Pos, heat: Int) =
            heat + (World.grid[pos]?.movementPenalty ?: MAX_HEAT)

    private fun posToCost(positions: Set<Pos>, heat: Int) =
            positions.map { pos -> pos to calcPosCost(pos, heat) }.toMap()

    private fun mergeMaps(maps: Set<Map<Pos, Int>>) =
            maps.flatMap { m -> m.map { it.key to it.value } }.toMap()

    private fun findSuccessors(currentMap: Map<Pos, Int>,
                               passable: Map<Pos, Cell>,
                               sameHeat: Set<Pos>, heat: Int) =
            sameHeat.map { pos ->
                val successors = findUnmarkedSurrounding(pos, passable, currentMap)
                posToCost(successors, heat) to successors.isNotEmpty()
            }.toMap()

    private fun calcFront(currentMap: Map<Pos, Int>, passable: Map<Pos, Cell>,
                          sameHeat: Set<Pos>, heat: Int): Pair<Map<Pos, Int>, Boolean> {
        val result = findSuccessors(currentMap, passable, sameHeat, heat)
        val front = mergeMaps(result.keys)
        val hasMore = result.values.contains(true)
        return front to hasMore
    }

    private fun createWaveFront(currentHeatMap: Map<Pos, Int>, passable: Map<Pos, Cell>,
                                heat: Int): Pair<Map<Pos, Int>, Boolean> {
        val sameHeat: Map<Pos, Int> = currentHeatMap.filter { it.value == heat }
        val (layer, hasMaybeMore) = calcFront(currentHeatMap, passable, sameHeat.keys, heat)
        return layer to hasMaybeMore
    }

    fun generateHeatMap(goal: Pos): Map<Pos, Int> {
        val passable = World.passableCells()
        var heat = 0
        var maxHeat = 0
        val map = mutableMapOf<Pos, Int>()
        map[goal.toShadow()] = heat
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

    private fun createVec(heatMap: Map<Pos, Int>, maxHeat: Int, destination: Pos, pos: Pos): Complex {
        val left = heatMap[Pos(pos.x - 1, pos.y)] ?: maxHeat
        val right = heatMap[Pos(pos.x + 1, pos.y)] ?: maxHeat
        val up = heatMap[Pos(pos.x, pos.y - 1)] ?: maxHeat
        val down = heatMap[Pos(pos.x, pos.y + 1)] ?: maxHeat
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

    fun calculateVectorField(heatMap: Map<Pos, Int>, destination: Pos): Map<Pos, Complex> {
        val maxHeat = heatMap.values.max()!!
        val fields = World.grid.map {
            val raw = createVec(heatMap, maxHeat, destination, it.key)
            val vec = raw.copyWithNewMagnitude(1.0) //FIXME use terrain penalty
            it.key to vec
        }.toMap()
        return smooth(fields, Config.vectorSmoothCount).toMap()
    }

    private fun smooth(map: Map<Pos, Complex>, count: Int): Map<Pos, Complex> =
            if (count > 0) {
                smooth(smoothVectorMap(map), count - 1)
            } else {
                map
            }

    private fun smoothVectorMap(map: Map<Pos, Complex>): Map<Pos, Complex> {
        val n = 1
        val xRange = -n..n
        val yRange = -n..n
        return map.map {
            val pos = it.key
            val sum: Complex = yRange.flatMap { dy ->
                xRange.map { dx ->
                    map[Pos(pos.x + dx, pos.y + dy)] ?: Complex.ZERO
                }
            }.fold(Complex.ZERO) { acc, complex -> acc.plus(complex) }
            val magnitude = sum.magnitude / (xRange.count() * yRange.count())
            val phase = sum.phase
            it.key to Complex.fromMagnitudeAndPhase(magnitude, phase) //FIXME use terrain penalty
        }.toMap()
    }

    private fun findUnmarkedSurrounding(node: Pos, passable: Map<Pos, Cell>,
                                        heatMap: Map<Pos, Int>): Set<Pos> =
            findAllSurrounding(node)
                    .filterNot { heatMap.containsKey(it) }
                    .filter { passable.containsKey(it) }
                    .toSet()

    private fun findAllSurrounding(node: Pos): List<Pos> {
        return listOfNotNull(
                Pos(node.x - 1, node.y - 1),
                Pos(node.x - 1, node.y),
                Pos(node.x - 1, node.y + 1),
                Pos(node.x, node.y - 1),
                Pos(node.x, node.y + 1),
                Pos(node.x + 1, node.y - 1),
                Pos(node.x + 1, node.y),
                Pos(node.x + 1, node.y + 1))
    }
}
