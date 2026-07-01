package agent.action.cond

import Factory
import config.Config
import items.PowerCube
import util.Rng
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [Recycler] is the agent's inventory management: tap a power cube for XM when drained, and dump junk to free
 * space when the inventory is nearly full (so it can hack again). These cover the [Recycler.isActionPossible]
 * gate and both [Recycler.performAction] effects (XM restored, slots freed).
 */
class RecyclerTest {

    @BeforeTest
    fun reset() = Rng.seed(9)

    @Test
    fun aToppedUpAgentWithSpaceHasNothingToRecycle() = with(Factory) {
        val agent = frog() // full XM, a modest starting kit (not near full)
        assertFalse(Recycler.isActionPossible(agent), "high XM + free slots → nothing to recycle")
    }

    @Test
    fun aDrainedAgentTapsAPowerCubeForXm() = with(Factory) {
        val agent = frog()
        agent.inventory.items.clear()
        agent.inventory.items.add(PowerCube.create(agent, 4))
        agent.xm = 0 // low XM + a cube → recycle taps it
        assertTrue(Recycler.isActionPossible(agent), "drained with a cube on hand → recycle is possible")
        Recycler.performAction(agent)
        assertTrue(agent.xm > 0, "recycling the cube restores XM")
        assertTrue(agent.inventory.findPowerCubes().isEmpty(), "the tapped cube is consumed")
    }

    @Test
    fun aNearFullInventoryRecyclesForSpace() = with(Factory) {
        val agent = frog()
        agent.xm = agent.xmCapacity() // NOT low → only the free-space branch fires
        agent.inventory.items.clear()
        repeat((Config.maxInventory * 0.98).toInt()) { agent.inventory.items.add(resonator(agent, 1)) }
        val before = agent.inventory.size()
        assertTrue(Recycler.isActionPossible(agent), "≥95% full → recycle to free space")
        Recycler.performAction(agent)
        assertTrue(agent.inventory.size() < before, "recycling frees inventory slots")
    }
}
