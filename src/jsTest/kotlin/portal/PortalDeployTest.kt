package portal

import Factory
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Regression for [Portal.deploy]'s distance clamp: deploying from beyond [config.Dim.maxDeploymentRange] (the
 * agent's per-tick step granularity can land it a hair out of range) must not trip [ResonatorSlot.deployReso]'s
 * range checks — the distance is clamped into the legal band instead of crashing the tick.
 */
class PortalDeployTest {

    @Test
    fun deployingFromBeyondMaxRangeClampsTheDistanceInsteadOfCrashing() = with(Factory) {
        val portal = portal() // unowned → this deploy captures it
        val agent = frog()
        val reso = resonator(agent, 1)
        portal.deploy(agent, mapOf(Octant.N to reso), distance = 999) // way past max range
        assertNotNull(portal.slots.getValue(Octant.N).resonator, "the reso deployed (distance clamped into range)")
    }
}
