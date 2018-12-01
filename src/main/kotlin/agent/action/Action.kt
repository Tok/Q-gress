package agent.action

import World
import config.Time

data class Action(var item: ActionItem, var untilTick: Int) {
    fun start(item: ActionItem) {
        this.item = item
        this.untilTick = World.tick + Time.secondsToTicks(item.durationSeconds)
    }

    fun end() {
        this.item = ActionItem.WAIT
        this.untilTick = World.tick + 1
    }

    override fun toString() = item.text
    fun isBusy() = World.tick <= untilTick

    companion object {
        fun create() = Action(ActionItem.WAIT, World.tick)
    }
}
