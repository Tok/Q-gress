package portal

import Factory
import World
import agent.Agent
import agent.Faction
import config.Dim
import items.deployable.Resonator
import util.data.Line
import util.data.Pos
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Core link/field integrity invariants (the rules that "must work like before" after the 3D move):
 * the no-crossing-links geometry, and — critically — that removing or neutralising a portal never
 * leaves a dangling link or field referencing it from any other portal.
 *
 * The cleanup lives in `Portal.destroy()` → `destroyAllLinksAndFields` (the functional core), which
 * is what these exercise. `Portal.remove()` is the imperative shell: `destroy()` + the `World.
 * allPortals` bookkeeping + the (browser-only) shatter FX/sound — not unit-testable in Node.
 */
class LinkFieldIntegrityTest {

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
    }

    private fun portalAt(x: Int, y: Int): Portal = Portal.create(Pos(x.toDouble(), y.toDouble())).also { World.allPortals.add(it) }

    private fun link(a: Portal, b: Portal): Link = requireNotNull(Link.create(a, b, Factory.linker())).also { a.links.add(it) }

    private fun field(o: Portal, p1: Portal, p2: Portal): Field = requireNotNull(Field.create(o, p1, p2, Factory.owner())).also {
        o.fields.add(it)
    }

    private fun deployFull(p: Portal, agent: Agent) = Octant.values().forEach {
        p.deploy(agent, mapOf(it to Resonator.create(agent, 1)), Dim.maxDeploymentRange.toInt())
    }

    // --- the no-crossing rule at the API level (findLinkableForKeys), not just the geometry ---

    @Test
    fun crossingDestinationIsNotLinkable() {
        val linker = Factory.frog()
        val origin = portalAt(0, 0)
        val dest = portalAt(100, 100)
        deployFull(dest, linker) // owned by the linker's faction + fully deployed → otherwise linkable
        linker.inventory.items.add(PortalKey(dest, linker)) // a key to the destination
        // An existing link that crosses the origin→dest diagonal:
        link(portalAt(0, 100), portalAt(100, 0))
        assertTrue(origin.findLinkableForKeys(linker).isEmpty(), "a destination whose link would cross is not linkable")
    }

    @Test
    fun nonCrossingDestinationIsLinkable() {
        val linker = Factory.frog()
        val origin = portalAt(0, 0)
        val dest = portalAt(100, 100)
        deployFull(dest, linker)
        linker.inventory.items.add(PortalKey(dest, linker))
        assertEquals(listOf(dest), origin.findLinkableForKeys(linker), "with no crossing, the keyed destination is linkable")
    }

    // --- no-crossing geometry (the rule backing findLinkableForKeys / Linker) ---

    @Test
    fun crossingSegmentsIntersect() {
        assertTrue(Line(Pos(0.0, 0.0), Pos(10.0, 10.0)).doesIntersect(Line(Pos(0.0, 10.0), Pos(10.0, 0.0))))
    }

    @Test
    fun sharedEndpointIsNotACrossing() {
        // Two links out of the same portal share an endpoint — that must NOT count as crossing.
        assertFalse(Line(Pos(0.0, 0.0), Pos(10.0, 0.0)).doesIntersect(Line(Pos(0.0, 0.0), Pos(0.0, 10.0))))
    }

    @Test
    fun disjointSegmentsDoNotIntersect() {
        assertFalse(Line(Pos(0.0, 0.0), Pos(1.0, 1.0)).doesIntersect(Line(Pos(5.0, 5.0), Pos(6.0, 6.0))))
    }

    @Test
    fun parallelSegmentsDoNotIntersect() {
        assertFalse(Line(Pos(0.0, 0.0), Pos(10.0, 0.0)).doesIntersect(Line(Pos(0.0, 5.0), Pos(10.0, 5.0))))
    }

    @Test
    fun findLinkableForKeysIsEmptyWithoutKeys() {
        val origin = portalAt(0, 0)
        assertTrue(origin.findLinkableForKeys(Factory.linker()).isEmpty())
    }

    // --- no dangling links on removal ---

    @Test
    fun destroyingDestinationClearsIncomingLinkFromOrigin() {
        val a = portalAt(0, 0)
        val b = portalAt(100, 0)
        link(a, b) // a → b, stored on a
        assertEquals(1, a.links.size)
        b.destroy()
        assertTrue(a.links.isEmpty(), "the link into the destroyed portal must be gone from its origin")
        assertTrue(World.allLinks().none { it.isConnectedTo(b) }, "no link may still reference the destroyed portal")
    }

    @Test
    fun destroyingOriginClearsItsOutgoingLinks() {
        val a = portalAt(0, 0)
        val b = portalAt(100, 0)
        link(a, b)
        a.destroy()
        assertTrue(a.links.isEmpty())
        assertTrue(World.allLinks().none { it.isConnectedTo(a) }, "no link may still reference the destroyed portal")
    }

    // --- no dangling fields on destruction ---

    @Test
    fun destroyingAnAnchorClearsTheFieldFromItsOrigin() {
        val o = portalAt(0, 0)
        val p1 = portalAt(100, 0)
        val p2 = portalAt(0, 100)
        field(o, p1, p2)
        assertEquals(1, World.allFields().size)
        p1.destroy() // p1 is the primaryAnchor, but the field lives on o
        assertTrue(o.fields.isEmpty(), "field must be removed from its origin when an anchor is destroyed")
        assertTrue(World.allFields().none { it.isConnectedTo(p1) })
    }

    @Test
    fun destroyingTheOriginClearsItsField() {
        val o = portalAt(0, 0)
        val p1 = portalAt(100, 0)
        val p2 = portalAt(0, 100)
        field(o, p1, p2)
        o.destroy()
        assertTrue(World.allFields().isEmpty(), "destroying the origin removes its field")
    }

    // --- a virus flip must not leave the old faction's links/fields on the flipped portal ---

    @Test
    fun virusFlipDestroysCrossFactionLinksAndFields() {
        val a = portalAt(0, 0)
        val b = portalAt(100, 0)
        val c = portalAt(0, 100)
        a.owner = Factory.frog()
        b.owner = Factory.frog()
        link(a, b) // a → b (green), an INCOMING link to b stored on a — the one that used to survive the flip
        link(b, c) // b → c (green), an OUTGOING link stored on b
        field(b, a, c) // a field anchored on b

        b.refactor(Factory.smurf()) // JARVIS / ADA virus: flip b to RES

        assertEquals(Faction.RES, b.owner?.faction, "the virus flips the portal's faction")
        assertTrue(
            World.allLinks().none {
                it.isConnectedTo(b)
            },
            "no link may survive on a virus-flipped portal (the green-links-on-a-blue-portal bug)",
        )
        assertTrue(b.links.isEmpty(), "the flipped portal keeps no outgoing links")
        assertTrue(a.links.isEmpty(), "the incoming green link is cleared from its origin too")
        assertTrue(World.allFields().none { it.isConnectedTo(b) }, "no field may survive on a virus-flipped portal")
    }

    // --- neutralising via destroy() clears everything ---

    @Test
    fun destroyNeutralisesAndClearsLinksAndFields() {
        val a = portalAt(0, 0)
        val b = portalAt(100, 0)
        val c = portalAt(0, 100)
        a.owner = Factory.frog()
        link(a, b)
        field(a, b, c)
        a.destroy()
        assertEquals(null, a.owner, "a destroyed/neutralised portal has no owner")
        assertTrue(a.links.isEmpty(), "neutralised portal keeps no links")
        assertTrue(a.fields.isEmpty(), "neutralised portal keeps no fields")
    }
}
