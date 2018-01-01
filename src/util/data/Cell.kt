package util.data

data class Cell(val position: Coords, val isPassable: Boolean, val movementPenalty: Int) {
    fun getColor() = if (isPassableInAllDirections()) "#ffffff33" else if (isPassable) "#00000011" else "#00000033"
    fun isPassableInAllDirections(): Boolean {
        fun isLeftPassable() = World.grid.get(Coords(position.x - 1, position.y))?.isPassable ?: false
        fun isRightPassable() = World.grid.get(Coords(position.x + 1, position.y))?.isPassable ?: false
        fun isUpPassable() = World.grid.get(Coords(position.x, position.y - 1))?.isPassable ?: false
        fun isDownPassable() = World.grid.get(Coords(position.x, position.y + 1))?.isPassable ?: false
        fun isUpLeftPassable() = World.grid.get(Coords(position.x - 1, position.y - 1))?.isPassable ?: false
        fun isUpRightPassable() = World.grid.get(Coords(position.x + 1, position.y - 1))?.isPassable ?: false
        fun isDownLeftPassable() = World.grid.get(Coords(position.x - 1, position.y + 1))?.isPassable ?: false
        fun isDownRightPassable() = World.grid.get(Coords(position.x + 1, position.y + 1))?.isPassable ?: false
        return isPassable && isLeftPassable() && isRightPassable() && isUpPassable() && isDownPassable() &&
                isUpLeftPassable() && isUpRightPassable() && isDownLeftPassable() && isDownRightPassable()
    }

    override fun toString() = position.x.toString() + ":" + position.y.toString()
}
