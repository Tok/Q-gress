package agent

import config.Config
import items.PowerCube
import items.QgressItem
import items.UltraStrike
import items.XmpBurster
import items.deployable.HeatSink
import items.deployable.Multihack
import items.deployable.Resonator
import items.deployable.Shield
import items.deployable.Virus
import portal.Portal
import portal.PortalKey

data class Inventory(val items: MutableList<QgressItem> = mutableListOf()) {
    fun size(): Int = items.size
    fun isFull(): Boolean = items.size >= Config.maxInventory
    fun freeSpace(): Int = (Config.maxInventory - items.size).coerceAtLeast(0)

    /** Surplus copies of a key — beyond [MAX_KEYS_PER_PORTAL] for the same portal you never need them, so
     *  they're the first thing to recycle (a few are kept for remote recharging/relinking). */
    private fun excessDuplicateKeys(): List<PortalKey> =
        findKeys().groupBy { it.portal }.flatMap { (_, keys) -> keys.drop(MAX_KEYS_PER_PORTAL) }

    /** Recycle up to [maxToRemove] of the least-useful items to free space — first the surplus duplicate keys,
     *  then the lowest resonators, surplus weapons and power cubes; a few keys per portal + shields/heat-sinks/
     *  multi-hacks are kept. Returns the XM recovered. */
    fun recycleForSpace(maxToRemove: Int): Int {
        if (maxToRemove <= 0) return 0
        val candidates = excessDuplicateKeys() +
            findResonators().sortedBy { it.level.level } + findXmps() + findUltraStrikes() + findPowerCubes()
        var xm = 0
        candidates.take(maxToRemove).forEach { item ->
            items.remove(item)
            xm += when (item) {
                is Resonator -> item.level.calculateRecycleXm()
                is PowerCube -> item.level.calculateRecycleXm()
                else -> 0
            }
        }
        return xm
    }
    fun findKeys(): List<PortalKey> = items.filter { it is PortalKey }.map { it as PortalKey }
    fun findXmps(): List<XmpBurster> = items.filter { it is XmpBurster }.map { it as XmpBurster }
    fun findUltraStrikes(): List<UltraStrike> = items.filter { it is UltraStrike }.map { it as UltraStrike }
    fun findResonators(): List<Resonator> = items.filter { it is Resonator }.map { it as Resonator }
    fun findPowerCubes(): List<PowerCube> = items.filter { it is PowerCube }.map { it as PowerCube }
    fun findShields(): List<Shield> = items.filterIsInstance<Shield>()
    fun findHeatSinks(): List<HeatSink> = items.filterIsInstance<HeatSink>()
    fun findMultihacks(): List<Multihack> = items.filterIsInstance<Multihack>()
    fun findViruses(): List<Virus> = items.filterIsInstance<Virus>()

    // "Unique" = one per PORTAL, not per key object: PortalKey has no equals/hashCode (identity), so plain
    // distinct() would keep every separately-hacked key to the same portal — inflating the count past the
    // portal total. distinctBy { portal } collapses all keys for a portal to one.
    fun findUniqueKeys(): List<PortalKey> = findKeys().distinctBy { it.portal }

    fun addItem(item: QgressItem) = items.add(item)
    fun addItems(newItems: List<QgressItem>) = items.addAll(newItems)

    fun consumeKeyToPortal(portal: Portal) {
        val key = findUniqueKeys().find { it.portal == portal } ?: error("Key should exist.")
        items.remove(key)
    }

    fun consumeXmps(xmps: List<XmpBurster>) = items.removeAll(xmps)
    fun consumeUltraStrikes(us: List<UltraStrike>) = items.removeAll(us)
    fun consumeResos(resos: List<Resonator>) = items.removeAll(resos)
    fun consumeCubes(cubes: List<PowerCube>) = items.removeAll(cubes)

    fun keyCount(): Int = items.filter { it is PortalKey }.count()

    override fun toString(): String {
        val filtered = items.filterNot { it is PortalKey || it is XmpBurster || it is Resonator }
        val items = filtered.groupingBy { it }.eachCount().map {
            val count = it.value
            if (count == 1) {
                it.key.toString()
            } else {
                count.toString() + "x" + it.key
            }
        }.toString()
        return keyCount().toString() + " keys " + items
    }

    companion object {
        const val MAX_KEYS_PER_PORTAL = 4 // keep a few keys per portal (remote recharge/relink); recycle the rest

        fun empty() = Inventory()

        /** Starting gear for the chosen [config.StartStage]: none at START, a light kit at MID, the full
         *  loadout at END. Scaled to the agent's level. */
        fun startingGear(agent: Agent, stage: config.StartStage): List<QgressItem> = when (stage) {
            config.StartStage.START -> emptyList()
            config.StartStage.MID -> lightGear(agent)
            config.StartStage.END -> quickStart(agent)
        }

        // A modest kit: enough to capture + field a few portals, not a full L8 arsenal.
        private fun lightGear(agent: Agent): List<QgressItem> {
            val level = agent.getLevel()
            return listOf(
                (1..10).map { XmpBurster.create(agent, level) },
                (1..8).map { Resonator.create(agent, level) },
                (1..6).map { Resonator.create(agent, level - 1) },
                (1..6).map { PowerCube.create(agent, level) },
            ).flatten()
        }

        fun quickStart(agent: Agent): List<QgressItem> {
            val level = agent.getLevel()
            return listOf(
                (1..40).map { XmpBurster.create(agent, level) },
                (1..40).map { XmpBurster.create(agent, level - 1) },
                (1..40).map { Resonator.create(agent, level) }, // plenty of top-level resos → full L8 portals right away
                (1..20).map { Resonator.create(agent, level - 1) },
                (1..20).map { Resonator.create(agent, level - 2) },
                (1..20).map { Resonator.create(agent, level - 3) },
                (1..30).map { PowerCube.create(agent, level) },
                (1..20).map { PowerCube.create(agent, level - 1) },
            ).flatten()
        }
    }
}
