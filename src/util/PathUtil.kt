package util

import World
import config.Config
import config.Dimensions
import util.data.Cell
import util.data.Complex
import util.data.Coords

object PathUtil {
    val MIN_HEAT = 35
    val MAX_HEAT = 100
    val RESOLUTION = 12
    fun w() = Dimensions.width / RESOLUTION
    fun h() = Dimensions.height / RESOLUTION
    fun posToShadowPos(pos: Coords) = Coords(pos.x / RESOLUTION, pos.y / RESOLUTION)
    fun shadowPosToPos(pos: Coords) = Coords(pos.x * RESOLUTION, pos.y * RESOLUTION)

    fun generateHeatMap(goal: Coords): Map<Coords, Int> {
        val passable = World.passableCells()
        val heatMap = mutableMapOf<Coords, Int>()
        fun createWaveFront(heat: Int): Boolean {
            val sameHeat = heatMap.filter { it.value == heat }
            var hasMaybeMore = false
            sameHeat.forEach { entry ->
                val succs = findUnmarkedSurrounding(entry.key, passable, heatMap)
                succs.forEach { succ ->
                    val cell = World.grid.get(succ)
                    val cost = cell?.movementPenalty ?: MAX_HEAT
                    heatMap.put(succ, heat + cost)
                    hasMaybeMore = true
                }
            }
            val overcount = heat - (heatMap.map { it.value }.max() ?: 0)
            return hasMaybeMore || overcount < MAX_HEAT
        }

        var heat = 0
        heatMap.put(posToShadowPos(goal), heat)
        while (createWaveFront(heat++)) {
        }

        fun nextLayer(map: Map<Coords, Int>): Map<Coords, Int> {
            val layer = mutableMapOf<Coords, Int>()
            map.forEach { original ->
                original.key.getSurrounding(w(), h()).forEach { surrounding ->
                    if (!map.containsKey(surrounding)) {
                        layer.put(surrounding, original.value + MAX_HEAT)
                    }
                }
            }
            return layer.toMap()
        }

        heatMap.putAll(nextLayer(heatMap))
        //heatMap.putAll(nextLayer(heatMap))
        //heatMap.putAll(nextLayer(heatMap))

        return heatMap
    }

    fun calculateVectorField(heatMap: Map<Coords, Int>): Map<Coords, Complex> {
        val maxHeat = heatMap.values.max()!!
        val fields = World.grid.map {
            val leftPos = Coords(it.key.x - 1, it.key.y)
            val rightPos = Coords(it.key.x + 1, it.key.y)
            val upPos = Coords(it.key.x, it.key.y - 1)
            val downPos = Coords(it.key.x, it.key.y + 1)
            val left = heatMap.get(leftPos) ?: maxHeat
            val right = heatMap.get(rightPos) ?: maxHeat
            val up = heatMap.get(upPos) ?: maxHeat
            val down = heatMap.get(downPos) ?: maxHeat
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
            val up = map.get(Coords(pos.x, pos.y - 1)) ?: Complex.ZERO
            val down = map.get(Coords(pos.x, pos.y + 1)) ?: Complex.ZERO
            val left = map.get(Coords(pos.x - 1, pos.y)) ?: Complex.ZERO
            val right = map.get(Coords(pos.x + 1, pos.y)) ?: Complex.ZERO
            val sum = up + down + left + right
            it.key to Complex.fromMagnitudeAndPhase(1F, sum.phase) //FIXME use terrain penalty
        }.toMap()
    }

    private fun findUnmarkedSurrounding(node: Coords, passable: Map<Coords, Cell>, heatMap: Map<Coords, Int>): List<Coords> {
        return findAllSurrounding(node)
                .filterNot { heatMap.containsKey(it) }
                .filter { passable.containsKey(it) }
    }

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
