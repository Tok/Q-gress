package util.data

import util.PathUtil

data class Cell(val position: Pos, val isPassable: Boolean, val movementPenalty: Int) {
    /**
     * Colour for the 3D passability overlay: blocked cells read as dark blocks, walkable cells
     * as a white(fast road)→grey(high-penalty ground) ramp. Semi-transparent so the map shows
     * through. (A green→red penalty heatmap mode can be added later.)
     */
    fun overlayColor(): String {
        if (!isPassable) return "rgba(0, 0, 0, 0.75)"
        val span = (PathUtil.MAX_HEAT - PathUtil.MIN_HEAT).toDouble()
        val t = ((movementPenalty - PathUtil.MIN_HEAT) / span).coerceIn(0.0, 1.0)
        val v = (255 - t * 175).toInt() // 255 = road, ~80 = high-penalty ground
        return "rgba($v, $v, $v, 0.45)"
    }

    fun getColor() = if (isPassableInAllDirections()) {
        "#ffffff33"
    } else if (isPassable) {
        "#00000011"
    } else {
        "#00000033"
    }
    fun isPassableInAllDirections(): Boolean {
        fun isLeftPassable() = World.grid[Pos(position.x - 1, position.y)]?.isPassable ?: false
        fun isRightPassable() = World.grid[Pos(position.x + 1, position.y)]?.isPassable ?: false
        fun isUpPassable() = World.grid[Pos(position.x, position.y - 1)]?.isPassable ?: false
        fun isDownPassable() = World.grid[Pos(position.x, position.y + 1)]?.isPassable ?: false
        fun isUpLeftPassable() = World.grid[Pos(position.x - 1, position.y - 1)]?.isPassable ?: false
        fun isUpRightPassable() = World.grid[Pos(position.x + 1, position.y - 1)]?.isPassable ?: false
        fun isDownLeftPassable() = World.grid[Pos(position.x - 1, position.y + 1)]?.isPassable ?: false
        fun isDownRightPassable() = World.grid[Pos(position.x + 1, position.y + 1)]?.isPassable ?: false
        return isPassable &&
            isLeftPassable() &&
            isRightPassable() &&
            isUpPassable() &&
            isDownPassable() &&
            isUpLeftPassable() &&
            isUpRightPassable() &&
            isDownLeftPassable() &&
            isDownRightPassable()
    }

    override fun toString() = position.x.toString() + ":" + position.y.toString()
}
