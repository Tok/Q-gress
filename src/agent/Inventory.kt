package agent

import items.PowerCube
import items.QgressItem
import items.UltraStrike
import items.XmpBurster
import items.deployable.Resonator
import items.deployable.Shield
import portal.Portal
import portal.PortalKey

data class Inventory(val items: MutableList<QgressItem> = mutableListOf()) {
    fun findKeys(): List<PortalKey> = items.filter { it is PortalKey }.map { it as PortalKey }
    fun findXmps(): List<XmpBurster> = items.filter { it is XmpBurster }.map { it as XmpBurster }
    fun findResonators(): List<Resonator> = items.filter { it is Resonator }.map { it as Resonator }
    fun findPowerCubes(): List<PowerCube> = items.filter { it is PowerCube }.map { it as PowerCube }
    fun findShields(): List<Shield> = items.filter { it is Shield }.map { it as Shield }

    fun findUniqueKeys(): List<PortalKey>? = findKeys().distinct()

    fun consumeKeyToPortal(portal: Portal) {
        val key = findUniqueKeys()!!.find { it.portal == portal } ?: throw IllegalStateException("Key should exist.")
        items.remove(key)
    }

    fun consumeXmps(xmps: List<XmpBurster>) = items.removeAll(xmps)
    fun consumeResos(resos: List<Resonator>) = items.removeAll(resos)
    fun consumeCubes(cubes: List<PowerCube>) = items.removeAll(cubes)

    fun keyCount(): Int = items.filter { it is PortalKey }.count()
    private fun xmpCount(): Int = items.filter { it is XmpBurster }.count()
    private fun usCount(): Int = items.filter { it is UltraStrike }.count()
    private fun weaponCount(): Int = xmpCount() + usCount()
    private fun resoCount(): Int = items.filter { it is Resonator }.count()
    private fun shieldCount(): Int = items.filter { it is Shield }.count()
    private fun powerCubeCount(): Int = items.filter { it is PowerCube }.count()

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
        fun empty() = Inventory()
        fun quickStart(agent: Agent): List<QgressItem> {
            val level = agent.getLevel()
            return listOf(
                    (1..20).map { XmpBurster.create(agent, level) },
                    (1..30).map { XmpBurster.create(agent, level - 1) },
                    (1..10).map { Resonator.create(agent, level) },
                    (1..10).map { Resonator.create(agent, level - 1) },
                    (1..20).map { Resonator.create(agent, level - 2) },
                    (1..30).map { Resonator.create(agent, level - 3) },
                    (1..20).map { PowerCube.create(agent, level) },
                    (1..10).map { PowerCube.create(agent, level - 1) }
            ).flatten()
        }
    }
}
