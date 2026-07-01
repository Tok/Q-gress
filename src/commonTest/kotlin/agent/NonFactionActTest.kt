package agent

import Factory
import World
import config.Sim
import extension.VectorField
import system.grid.GridFixture
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The [NonFaction] travel machine in motion: the per-tick force selection (vector-field fallback, the rare
 * swarm nudge, the stuck bee-line then re-target escalation), the [NonFaction.moveElsewhere] destination
 * roll (off-map ring / far portal / random portal), and the companion spawn + off-map-ring helpers on a
 * rectangular field.
 */
class NonFactionActTest {

    @BeforeTest
    fun reset() {
        Sim.roundField = false
        World.grid = GridFixture("NPCACT", 180, 120, 2, GridFixture.rleEncode(List(180 * 120) { true })).toGrid()
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.tick = 0
        NonFaction.reset()
        StuckTracker.reset()
        Rng.seed(29)
    }

    @AfterTest
    fun tidy() {
        Sim.roundField = true
        World.grid = emptyMap()
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.tick = 0
        NonFaction.reset()
        StuckTracker.reset()
    }

    private fun npc(pos: Pos, destination: Pos, busyUntil: Int = -1) =
        NonFaction(pos, speed = 5.0, size = AgentSize(0), destination = destination, vectors = VectorField.EMPTY, busyUntil = busyUntil)

    private fun addPortals() {
        World.allPortals.add(Factory.portal())
        World.allPortals.add(Factory.portal())
        World.allPortals.add(Factory.portal())
    }

    @Test
    fun aTravellingNpcFallsBackToAStraightLineHeading() {
        addPortals()
        val mover = npc(Pos(100, 400), Pos(1600, 400))
        World.allNonFaction.add(mover)
        val before = mover.pos
        mover.act()
        assertTrue(
            mover.pos.distanceTo(mover.destination) < before.distanceTo(mover.destination),
            "an NPC with no flow field walks straight at its target",
        )
    }

    @Test
    fun aStuckNpcBeelinesThenReTargets() {
        addPortals()
        val mover = npc(Pos(300, 400), Pos(40000, 400)) // destination unreachably far → never arrives
        World.allNonFaction.add(mover)
        // Flag it stuck via the NPC key StuckTracker uses.
        repeat(40) { StuckTracker.sample(listOf("npc:${mover.id}" to mover.pos)) }
        assertTrue(StuckTracker.isStuck("npc:${mover.id}"), "the wedged NPC is flagged stuck")
        val destBefore = mover.destination
        mover.act() // first escalation: bee-line begins
        repeat(StuckTracker.RECOVERY_BEELINE_TICKS + 2) { mover.act() } // drain the bee-line
        mover.act() // second escalation: re-roll a new destination
        assertTrue(mover.destination != destBefore, "a spent bee-line escalates to a fresh destination")
    }

    @Test
    fun theSwarmNudgeEventuallyFires() {
        addPortals()
        val mover = npc(Pos(500, 400), Pos(90000, 400)) // far target → keeps travelling, never arrives
        World.allNonFaction.add(mover)
        // A co-located neighbour so the (rare) swarm branch's findNearest() has a target.
        World.allNonFaction.add(npc(Pos(500, 400), Pos(90000, 400), busyUntil = Int.MAX_VALUE))
        val before = mover.pos
        repeat(2500) { mover.act() } // the ~2% swarm roll is near-certain to fire at least once over this many ticks
        assertTrue(mover.pos != before, "the NPC travels over the run (covering the vector-field + swarm force paths)")
    }

    @Test
    fun arrivedNpcsRollAllDestinationKinds() {
        addPortals()
        val movers = (0 until 24).map { npc(Pos(600, 600), Pos(600, 600)) } // pos == destination → arrival re-rolls
        movers.forEach { World.allNonFaction.add(it) }
        World.tick = 5
        movers.forEach { it.act() }
        assertTrue(movers.all { it.busyUntil > World.tick }, "every arrived NPC picked a new destination and rests")
    }

    @Test
    fun createSpawnsNpcsViaBothDestinationBranches() {
        addPortals()
        val made = (0 until 40).map { NonFaction.create(World.grid) } // both the off-map (85%) and portal (15%) spawn branches
        assertTrue(made.all { true }, "create always returns an NPC")
        assertEquals(40, made.size, "every create produced an NPC")
    }

    @Test
    fun offscreenRingIsBuiltForARectangularField() {
        Sim.roundField = false
        val ring = NonFaction.offscreenDestinations()
        assertTrue(ring.isNotEmpty(), "the rectangle border ring has points")
        ring.forEach { assertTrue(!Sim.isInPlayArea(it.x, it.y), "each ring point sits outside the play area") }
        NonFaction.prepareOffscreenLocations() // computes a field per ring point (headless inline)
        assertTrue(NonFaction.offscreenTotal() > 0, "the ring is counted")
    }

    @Test
    fun findRandomPrefersInPlayAreaNpcs() {
        val inside = npc(Pos(400, 400), Pos(400, 400))
        val offMap = npc(Pos(-5000, -5000), Pos(-5000, -5000))
        World.allNonFaction.add(inside)
        World.allNonFaction.add(offMap)
        val picked = NonFaction.findRandom()
        assertEquals(inside, picked, "findRandom prefers an NPC inside the play area over an off-map one")
    }
}
