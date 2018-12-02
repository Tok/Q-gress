package agent

import Factory
import config.Dim
import kotlin.test.*

class ResonatorSlotTest {

    @Test
    fun emptyDefault() = assertTrue(Factory.slot().isEmpty())

    @Test
    fun checkOwner() = with(Factory) {
        val owner = owner()
        assertTrue(slot().copy(owner = owner).isOwnedBy(owner))
    }

    @Test
    fun checkDeployer() = with(Factory) {
        val deployer = deployer()
        val slot = slot()
        slot.deployReso(deployer, resonator(), Dim.minDeploymentRange.toInt())
        assertTrue(slot.isOwnedBy(deployer))
    }

    @Test
    fun blockNonFactionDeployment() = with(Factory) {
        assertFailsWith(IllegalStateException::class) {
            slot().deployReso(nonFactionAgent(), resonator(), Dim.minDeploymentRange.toInt())
        }
    }

    @Test
    fun checkResoClear() = with(Factory) {
        val deployer = deployer()
        val slot = slot()
        slot.deployReso(deployer, resonator(), Dim.minDeploymentRange.toInt())
        slot.clear()
        assertNull(slot.owner)
        assertNull(slot.resonator)
        assertFalse(slot.isOwnedBy(deployer))
        assertEquals(0, slot.distance)
        assertTrue(slot.isEmpty())
    }

    @Test
    fun deployDistance() = with(Factory) {
        val dist = Dim.minDeploymentRange.toInt()
        val slot = slot()
        slot.deployReso(deployer(), resonator(), dist)
        assertEquals(dist, slot.distance)
    }

    @Test
    fun rangeConfig() = assertTrue(Dim.minDeploymentRange < Dim.maxDeploymentRange)

    @Test
    fun minDeploymentRange() = with(Factory) {
        val dist = Dim.minDeploymentRange.toInt() - 11
        assertFailsWith(IllegalStateException::class) {
            slot().deployReso(deployer(), resonator(), dist)
        }
    }

    @Test
    fun maxDeploymentRange() = with(Factory) {
        val dist = Dim.maxDeploymentRange.toInt() + 11
        assertFailsWith(IllegalStateException::class) {
            slot().deployReso(deployer(), resonator(), dist)
        }
    }
}
