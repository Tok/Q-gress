package agent

import Factory
import World
import config.Sim
import extension.VectorField
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The wandering NPC crowd ([NonFaction]): the per-tick [NonFaction.act] travel loop (walk toward a destination,
 * rest on arrival, then strike out again), the busy/rest gating, and the companion helpers ([findRandom],
 * [findNearestTo], the off-map destination ring). Headless the flow fields are empty (Nav is NoOp), so an NPC
 * falls back to a straight-line heading and still moves — the assertions below track that ground being covered.
 */
class NonFactionTest {

    @BeforeTest
    fun reset() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.tick = 0
        NonFaction.reset()
        Rng.seed(7)
    }

    @AfterTest
    fun tidy() {
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.tick = 0
        NonFaction.reset()
    }

    private fun npc(pos: Pos, destination: Pos, busyUntil: Int = -1) =
        NonFaction(pos, speed = 5.0, size = AgentSize(0), destination = destination, vectors = VectorField.EMPTY, busyUntil = busyUntil)

    // A far-away second NPC so the (rare) swarm branch's findNearest() never throws on an empty crowd.
    private fun addBystander() = World.allNonFaction.add(npc(Pos(9000, 9000), Pos(9000, 9000), busyUntil = Int.MAX_VALUE))

    @Test
    fun anActiveNpcWalksTowardItsDestination() {
        addBystander()
        val mover = npc(Pos(0, 0), Pos(2000, 0))
        World.allNonFaction.add(mover)
        val before = mover.pos
        mover.act()
        assertTrue(mover.pos != before, "an un-busy NPC en route moves")
        assertTrue(mover.pos.distanceTo(mover.destination) < before.distanceTo(mover.destination), "it moved toward the destination")
    }

    @Test
    fun aBusyNpcStaysPut() {
        val resting = npc(Pos(100, 100), Pos(2000, 0), busyUntil = 5)
        World.allNonFaction.add(resting)
        World.tick = 3 // tick <= busyUntil → still resting
        val before = resting.pos
        resting.act()
        assertEquals(before, resting.pos, "a resting NPC doesn't move until its timer expires")
    }

    @Test
    fun arrivingNpcPicksANewDestinationAndRests() {
        World.allPortals.add(Factory.portal())
        World.allPortals.add(Factory.portal())
        val arrived = npc(Pos(500, 500), Pos(500, 500)) // pos == destination → isAtDestination
        World.allNonFaction.add(arrived)
        World.tick = 10
        arrived.act()
        assertTrue(arrived.busyUntil > World.tick, "on arrival the NPC rests (busyUntil pushed into the future)")
    }

    @Test
    fun holdInPlaceOnlyEverExtendsTheRest() {
        val npc = npc(Pos(0, 0), Pos(10, 0), busyUntil = 20)
        npc.holdInPlace(50)
        assertEquals(50, npc.busyUntil, "holdInPlace extends the rest")
        npc.holdInPlace(30) // earlier tick → must not shorten it
        assertEquals(50, npc.busyUntil, "holdInPlace never pulls the rest earlier")
    }

    @Test
    fun stuckCandidateOnlyWhileTravellingAndNotBusy() {
        val travelling = npc(Pos(0, 0), Pos(2000, 0), busyUntil = -1)
        assertTrue(travelling.isStuckCandidate(0), "an un-busy NPC still far from its destination is a stuck candidate")
        val resting = npc(Pos(0, 0), Pos(2000, 0), busyUntil = 100)
        assertTrue(!resting.isStuckCandidate(0), "a resting NPC isn't a stuck candidate")
        val there = npc(Pos(0, 0), Pos(0, 0), busyUntil = -1)
        assertTrue(!there.isStuckCandidate(0), "an NPC already at its destination isn't a stuck candidate")
    }

    @Test
    fun findRandomIsNullOnAnEmptyCrowdAndAnNpcOtherwise() {
        assertNull(NonFaction.findRandom(), "no NPCs → nobody to recruit")
        val only = npc(Pos(10, 10), Pos(20, 20))
        World.allNonFaction.add(only)
        assertNotNull(NonFaction.findRandom(), "a populated crowd yields a recruitable NPC")
    }

    @Test
    fun findNearestToReturnsTheClosestNpc() {
        val near = npc(Pos(10, 10), Pos(0, 0))
        val far = npc(Pos(5000, 5000), Pos(0, 0))
        World.allNonFaction.add(near)
        World.allNonFaction.add(far)
        assertEquals(near, NonFaction.findNearestTo(Pos(0, 0)), "the nearest NPC by distance is returned")
    }

    @Test
    fun offscreenDestinationsSitOutsideThePlayArea() {
        val destinations = NonFaction.offscreenDestinations()
        assertTrue(destinations.isNotEmpty(), "the off-map ring has points")
        assertEquals(destinations.size, NonFaction.offscreenTotal(), "offscreenTotal counts the ring")
        destinations.forEach {
            assertTrue(!Sim.isInPlayArea(it.x, it.y), "every off-map destination is placed OUTSIDE the play area")
        }
    }

    @Test
    fun opposingOffscreenDestinationIsAnOffMapPoint() {
        val dest = NonFaction.opposingOffscreenDestination(Pos(0, 0))
        assertTrue(!Sim.isInPlayArea(dest.x, dest.y), "the opposing destination is off-map")
    }

    @Test
    fun resetClearsTheCachedFields() {
        NonFaction.getOrCreateVectorField(Pos(-500, -500)) // touch the cache
        NonFaction.reset()
        assertEquals(0, NonFaction.offscreenCount(), "reset drops all cached flow fields")
    }

    @Test
    fun createProducesAnNpcOnAPassableGrid() {
        World.allPortals.add(Factory.portal())
        val created = NonFaction.create(Factory.grid())
        assertNotNull(created, "create returns a fresh NPC")
    }
}
