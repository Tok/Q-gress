package util

import World
import config.Config
import config.Dim
import util.data.Cell
import util.data.Complex
import util.data.Coords
import kotlin.math.max

object PathUtil {
    const val MIN_HEAT = 35
    const val MAX_HEAT = 100
    const val RESOLUTION = 12
    fun w() = Dim.width / RESOLUTION
    fun h() = Dim.height / RESOLUTION
    fun posToShadowPos(pos: Coords) = Coords(pos.x / RESOLUTION, pos.y / RESOLUTION)
    fun shadowPosToPos(pos: Coords) = Coords(pos.x * RESOLUTION, pos.y * RESOLUTION)

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

    private fun createWaveFront(currentHeatMap: Map<Coords, Int>, passable: Map<Coords, Cell>
                                , heat: Int, max: Int): Pair<Map<Coords, Int>, Boolean> {
        val sameHeat: Map<Coords, Int> = currentHeatMap.filter { it.value == heat }
        val (layer, hasMaybeMore) = calcFront(currentHeatMap, passable, sameHeat.keys, heat)
        return layer to hasMaybeMore
    }

    fun generateHeatMap(goal: Coords): Map<Coords, Int> {
        val passable = World.passableCells()
        var heat = 0
        var maxHeat = 0
        val map = mutableMapOf<Coords, Int>()
        map[posToShadowPos(goal)] = heat
        while (true) {
            val (layer, hasMaybeMore) = createWaveFront(map, passable, heat++, maxHeat)
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

    fun calculateVectorField(heatMap: Map<Coords, Int>): Map<Coords, Complex> {
        val maxHeat = heatMap.values.max()!!
        val fields = World.grid.map {
            val leftPos = Coords(it.key.x - 1, it.key.y)
            val rightPos = Coords(it.key.x + 1, it.key.y)
            val upPos = Coords(it.key.x, it.key.y - 1)
            val downPos = Coords(it.key.x, it.key.y + 1)
            val left = heatMap[leftPos] ?: maxHeat
            val right = heatMap[rightPos] ?: maxHeat
            val up = heatMap[upPos] ?: maxHeat
            val down = heatMap[downPos] ?: maxHeat
            val rawer = Complex(left - right, up - down)
            val raw = Complex.fromMagnitudeAndPhase(1F, rawer.phase) //FIXME use terrain penalty
            it.key to raw
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
        return map.map {
            val pos = it.key
            val up = map[Coords(pos.x, pos.y - 1)] ?: Complex.ZERO
            val down = map[Coords(pos.x, pos.y + 1)] ?: Complex.ZERO
            val left = map[Coords(pos.x - 1, pos.y)] ?: Complex.ZERO
            val right = map[Coords(pos.x + 1, pos.y)] ?: Complex.ZERO
            val sum = up + down + left + right
            it.key to Complex.fromMagnitudeAndPhase(1F, sum.phase) //FIXME use terrain penalty
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
