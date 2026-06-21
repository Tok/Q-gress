package system

import config.Config

object Com {
    private val messages: MutableList<String> = mutableListOf()
    fun messageCount() = messages.count()

    fun clear() = messages.clear()

    fun addMessage(message: String) {
        messages.add(message)
        if (messages.size > Config.comMessageLimit) {
            messages.removeAt(0)
        }
    }

    /** Snapshot of the current messages (oldest→newest), for the DOM Com panel. */
    fun currentMessages(): List<String> = messages.toList()
}
