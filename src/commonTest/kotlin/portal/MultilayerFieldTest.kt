package portal

import World
import agent.Agent
import agent.Faction
import config.Dim
import items.deployable.Resonator
import util.data.Pos
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Multilayer fielding — the *directional* rules that let control fields stack into layers (the MU multiplier
 * the AI optimises for). Pins the intended behaviour so it can't silently regress:
 *  - A portal **covered** by a field can **never link out** (hard rule — [Portal.canLinkOut] via `isCoveredByField`).
 *  - An **anchor** (a field's vertex, which is *not* covered) **can link INTO** the covered portals: the line from
 *    a vertex to an interior portal stays inside the triangle, so it crosses no edge and closes a **nested** field.
 *  - A fan of such links sharing two anchors **stacks** those nested fields, each adding its area to the MU.
 *  - Linking the other way — covered → outward — is blocked (can't link out; and geometrically it would cross an edge).
 *
 * Geometry: a big triangle A(0,0) B(400,0) C(200,400) with p1/p2 strictly inside it.
 */
class MultilayerFieldTest {

    private lateinit var enl: Agent

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        enl = Factory.frog().also { it.addXm(1_000_000) } // plenty of XM for the link costs
    }

    @Test
    fun coveredPortalCanNeverLinkOut() {
        val board = coveredBoard()
        val a = board.a
        val b = board.b
        val c = board.c
        val p1 = board.p1
        val p2 = board.p2
        assertTrue(World.allFields().any { it.isCoveringPortal(p1) }, "p1 sits inside the big field")
        assertFalse(p1.canLinkOut(enl), "a portal covered by a field can NEVER link out (hard rule)")
        assertFalse(p2.canLinkOut(enl), "…the same for every covered portal")
        listOf(a, b, c).forEach { assertTrue(it.canLinkOut(enl), "an anchor (a field vertex) is uncovered → still links out") }
    }

    @Test
    fun coveredToOutsideBlockedByCrossing_anchorToInsideIsNot() {
        val board = coveredBoard()
        val a = board.a
        val p1 = board.p1
        val outside = own(900, 0) // well outside the triangle
        enl.inventory.items.add(PortalKey(outside, enl))
        assertFalse(p1.findLinkableForKeys(enl).contains(outside), "covered → outside would cross an edge → not linkable")
        enl.inventory.items.add(PortalKey(p1, enl))
        assertTrue(a.findLinkableForKeys(enl).contains(p1), "anchor → inside stays within the triangle → linkable")
    }

    @Test
    fun anchorsNestFieldsUnderTheCover_andMuStacks() {
        val board = coveredBoard()
        val a = board.a
        val b = board.b
        val p1 = board.p1
        val p2 = board.p2
        val coverMu = World.calcTotalMu(Faction.ENL)
        // Two anchors (a, b) each link the inside portals: every second link closes a nested triangle that
        // shares edge a-b — the fan that stacks the layers.
        keyedLink(a, p1)
        keyedLink(b, p1) // → nested field a-b-p1
        keyedLink(a, p2)
        keyedLink(b, p2) // → nested field a-b-p2
        assertEquals(3, World.allFields().size, "big cover + two nested layers")
        assertTrue(World.allFields().any { f -> f.isConnectedTo(a) && f.isConnectedTo(b) && f.isConnectedTo(p1) }, "layer a-b-p1 formed")
        assertTrue(World.allFields().any { f -> f.isConnectedTo(a) && f.isConnectedTo(b) && f.isConnectedTo(p2) }, "layer a-b-p2 formed")
        // Layering ADDS area — overlaps count, so total MU is the cover plus both nested fields.
        val total = World.calcTotalMu(Faction.ENL)
        assertEquals(World.allFields().sumOf { it.calculateMu() }, total, "MU sums every field, overlaps included")
        assertTrue(total > coverMu, "each layer adds its area on top of the cover")
    }

    @Test
    fun aPointUnderEveryLayerIsMultiplyCovered() {
        val board = coveredBoard()
        val a = board.a
        val b = board.b
        val p1 = board.p1
        val p2 = board.p2
        keyedLink(a, p1)
        keyedLink(b, p1)
        keyedLink(a, p2)
        keyedLink(b, p2)
        val probe = own(200, 30) // near edge a-b: inside the cover AND both nested triangles
        val layers = World.allFields().count { it.isCoveringPortal(probe) }
        assertEquals(3, layers, "the central area is covered by all three layers (true layering)")
    }

    @Test
    fun neutralisingAnAnchorCollapsesAllItsLayers() {
        val board = coveredBoard()
        val a = board.a
        val b = board.b
        val p1 = board.p1
        val p2 = board.p2
        keyedLink(a, p1)
        keyedLink(b, p1)
        keyedLink(a, p2)
        keyedLink(b, p2)
        assertEquals(3, World.allFields().size, "cover + two nested layers")
        // b is a shared anchor of the cover AND both nested layers — neutralising it must collapse every field on it,
        // exactly as for any portal's links/fields when it's torn down. (This is why anchors must be defended.)
        b.destroy()
        assertTrue(World.allFields().none { it.isConnectedTo(b) }, "no field referencing the neutralised anchor may survive")
        assertTrue(World.allLinks().none { it.isConnectedTo(b) }, "no link referencing the neutralised anchor survives either")
        assertEquals(0, World.allFields().size, "all three fields shared anchor b → every layer collapses with it")
    }

    @Test
    fun teamplay_theClosingAgentOwnsTheNestedFieldAndScoresIt() {
        val board = coveredBoard()
        val a = board.a
        val b = board.b
        val p1 = board.p1
        val scout = Factory.frog().also { it.addXm(1_000_000) } // opens the link from anchor a
        val closer = Factory.frog().also { it.addXm(1_000_000) } // makes the CLOSING link from anchor b
        scout.inventory.items.add(PortalKey(p1, scout))
        a.createLink(scout, p1) // a→p1, no field yet
        val closerApBefore = closer.ap
        closer.inventory.items.add(PortalKey(p1, closer))
        b.createLink(closer, p1) // b→p1 closes a-b-p1
        val nested = World.allFields().first { it.isConnectedTo(a) && it.isConnectedTo(b) && it.isConnectedTo(p1) }
        assertSame(closer, nested.owner, "the agent who makes the CLOSING link owns the nested field")
        assertTrue(closer.ap > closerApBefore, "…and scores the field-creation AP (the honour of closing goes to the closer)")
    }

    // --- scenario builders ------------------------------------------------------------------------------------

    /** Big covering triangle A-B-C (linked + fielded) with two portals strictly inside it. */
    private fun coveredBoard(): Board {
        val a = own(0, 0)
        val b = own(400, 0)
        val c = own(200, 400)
        val p1 = own(180, 120)
        val p2 = own(220, 120)
        linkLow(a, b)
        linkLow(b, c)
        linkLow(c, a)
        coverField(c, a, b) // the field that covers p1 / p2
        return Board(a, b, c, p1, p2)
    }

    private fun own(x: Int, y: Int): Portal = Portal.create(Pos(x.toDouble(), y.toDouble())).also { p ->
        World.allPortals.add(p)
        p.owner = enl
        Octant.values().forEach { o -> p.deploy(enl, mapOf(o to Resonator.create(enl, 1)), Dim.maxDeploymentRange.toInt()) }
    }

    private fun linkLow(a: Portal, b: Portal) {
        a.links.add(requireNotNull(Link.create(a, b, enl)))
    }
    private fun coverField(o: Portal, p1: Portal, p2: Portal) {
        o.fields.add(requireNotNull(Field.create(o, p1, p2, enl)))
    }

    /** The real link path: hand the linker a key, then drive `createLink` (which closes nested fields). */
    private fun keyedLink(from: Portal, to: Portal) {
        enl.inventory.items.add(PortalKey(to, enl))
        from.createLink(enl, to)
    }

    private data class Board(val a: Portal, val b: Portal, val c: Portal, val p1: Portal, val p2: Portal)
}
