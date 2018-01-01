package agent.action

data class Action(val item: ActionItem, val untilTick: Int) {
    override fun toString() = item.text
    companion object {
        fun start(item: ActionItem, tick: Int) = Action(item, tick + item.durationSeconds)
    }
}
