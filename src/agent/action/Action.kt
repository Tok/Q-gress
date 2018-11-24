package agent.action

data class Action(var item: ActionItem, var untilTick: Int) {
    fun start(item: ActionItem) {
        this.item = item
        this.untilTick = World.tick + item.durationSeconds
    }
    fun end() {
        this.item = ActionItem.WAIT
        this.untilTick = World.tick + 1
    }
    override fun toString() = item.text
    companion object {
        fun create() = Action(ActionItem.WAIT, World.tick)
    }
}
