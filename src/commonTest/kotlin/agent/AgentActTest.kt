package agent

import Factory
import World
import agent.action.ActionItem
import config.Dim
import config.Sim
import extension.VectorField
import items.XmpBurster
import items.deployable.Resonator
import items.level.XmpLevel
import portal.Octant
import portal.Portal
import portal.XmMap
import system.grid.GridFixture
import util.Rng
import util.data.Pos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Deep coverage of the stateful [Agent] act loop: every [Agent.act] committed-action branch (MOVE / EXPLORE /
 * RECRUIT / WAIT / isBusy), the attack + deploy handlers and their gates, [Agent.collectXm], the stuck-recovery
 * re-target escalation, the player level-up sound path, and the companion level / XM-capacity tables.
 */
class AgentActTest {

    @BeforeTest
    fun reset() {
        Sim.roundField = false
        World.grid = GridFixture("AGENTACT", 180, 120, 2, GridFixture.rleEncode(List(180 * 120) { true })).toGrid()
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.pendingAgents.clear()
        World.tick = 0
        World.userFaction = null
        World.isReady = true
        StuckTracker.reset()
        XmMap.clear()
        Rng.seed(31)
    }

    @AfterTest
    fun tidy() {
        Sim.roundField = true
        World.grid = emptyMap()
        World.allPortals.clear()
        World.allAgents.clear()
        World.allNonFaction.clear()
        World.pendingAgents.clear()
        World.tick = 0
        World.userFaction = null
        World.isReady = false
        StuckTracker.reset()
        XmMap.clear()
    }

    // A fully-deployed enemy (RES) portal at [pos] — a valid target for a frog attacker.
    private fun enemyPortalAt(pos: Pos): Portal {
        val foe = Factory.smurf()
        val portal = Portal.create(pos)
        portal.deploy(foe, Octant.values().associateWith { Resonator.create(foe, 1) }, Dim.minDeploymentRange.toInt())
        World.allPortals.add(portal)
        return portal
    }

    private fun frogAt(pos: Pos) = Agent.createFrog(Factory.grid(), pos)

    // --- act() dispatch ------------------------------------------------------

    @Test
    fun actMoveClosesOnTheActionPortal() {
        val portal = Portal.create(Pos(1400, 400))
        World.allPortals.add(portal)
        val agent = frogAt(Pos(200, 400))
        agent.actionPortal = portal
        agent.destination = portal.location
        agent.action.start(ActionItem.MOVE)
        val next = agent.act()
        assertTrue(next.pos.distanceTo(portal.location) < agent.pos.distanceTo(portal.location), "a MOVE agent closes on its portal")
        assertTrue(next.stepPx > 0.0, "act() records the ground covered this tick")
    }

    @Test
    fun actMoveEndsTheActionOnArrival() {
        val portal = Portal.create(Pos(400, 400))
        World.allPortals.add(portal)
        val agent = frogAt(Pos(400, 400)) // already on it
        agent.actionPortal = portal
        agent.action.start(ActionItem.MOVE)
        val next = agent.act()
        assertEquals(ActionItem.WAIT, next.action.item, "arriving at the action portal ends the MOVE (→ WAIT)")
    }

    @Test
    fun actMoveIsANoOpWhenTheWorldIsNotReady() {
        World.isReady = false
        val portal = Portal.create(Pos(1400, 400))
        World.allPortals.add(portal)
        val agent = frogAt(Pos(200, 400))
        agent.actionPortal = portal
        agent.action.start(ActionItem.MOVE)
        val next = agent.act()
        assertEquals(ActionItem.MOVE, next.action.item, "a not-ready world keeps the MOVE action (no re-select mid-teardown)")
        assertEquals(agent.pos, next.pos, "no movement while the world isn't ready")
    }

    @Test
    fun actExploreStrollsTowardOpenGround() {
        val agent = frogAt(Pos(600, 400))
        agent.destination = Pos(1200, 400)
        agent.action.start(ActionItem.EXPLORE)
        val next = agent.act()
        assertTrue(next.pos.distanceTo(agent.destination) < agent.pos.distanceTo(agent.destination), "an explorer moves toward its point")
    }

    @Test
    fun actExploreOnArrivalDiscoversAndReSelects() {
        World.allPortals.add(Portal.create(Pos(500, 500)))
        val agent = frogAt(Pos(600, 400))
        agent.destination = agent.pos // already arrived
        agent.action.start(ActionItem.EXPLORE)
        val before = agent.pos
        val next = agent.act()
        assertEquals(before, next.pos, "an arrived explorer resolves discovery in place (no step)")
        assertTrue(next.action.item != ActionItem.WAIT, "it re-selects a real action rather than parking in WAIT")
    }

    @Test
    fun actRecruitWalksTowardTheTargetNpc() {
        val npc = NonFaction(Pos(1200, 400), 5.0, AgentSize(0), Pos(1200, 400), VectorField.EMPTY, busyUntil = -1)
        World.allNonFaction.add(npc)
        val agent = frogAt(Pos(200, 400))
        agent.recruitTargetId = npc.id
        agent.action.start(ActionItem.RECRUIT)
        val next = agent.act()
        assertTrue(next.pos.distanceTo(npc.pos) < agent.pos.distanceTo(npc.pos), "the recruiter walks up to its NPC")
    }

    @Test
    fun actRecruitAbortsWhenTheTargetIsGone() {
        // A real NPC on the board so the post-abort re-selection can't throw on an empty crowd.
        World.allNonFaction.add(NonFaction(Pos(300, 300), 5.0, AgentSize(0), Pos(300, 300), VectorField.EMPTY, busyUntil = -1))
        val agent = frogAt(Pos(200, 400))
        agent.recruitTargetId = 987654 // no such NPC on the board
        agent.action.start(ActionItem.RECRUIT)
        val next = agent.act()
        assertTrue(next.recruitTargetId != 987654, "a vanished recruit target is abandoned (never re-committed to the phantom id)")
    }

    @Test
    fun actRecruitMeetsAndResolvesWhenTogether() {
        val npc = NonFaction(Pos(205, 400), 5.0, AgentSize(0), Pos(205, 400), VectorField.EMPTY, busyUntil = -1)
        World.allNonFaction.add(npc)
        val agent = frogAt(Pos(200, 400)) // within a deploy range of the NPC
        agent.recruitTargetId = npc.id
        agent.action.start(ActionItem.RECRUIT)
        agent.action.untilTick = World.tick - 1 // meeting already over → resolve rolls the result
        val next = agent.act()
        assertNull(next.recruitTargetId, "the meeting resolves and clears the recruit state")
    }

    @Test
    fun actWhileBusyOnACooldownActionStaysPut() {
        val agent = frogAt(Pos(300, 300))
        agent.action.start(ActionItem.HACK) // a cooldown action, not one of the movement handlers
        agent.action.untilTick = World.tick + 50 // still busy
        val before = agent.pos
        val next = agent.act()
        assertEquals(before, next.pos, "a busy cooldown action just waits out its timer")
        assertEquals(ActionItem.HACK, next.action.item, "the cooldown action is preserved while busy")
    }

    @Test
    fun actWaitImmediatelyReSelects() {
        World.allPortals.add(Portal.create(Pos(700, 400)))
        val agent = frogAt(Pos(200, 400))
        agent.action.item = ActionItem.WAIT
        val next = agent.act()
        assertTrue(next.action.item != ActionItem.WAIT, "WAIT is never a resting state — act() picks a real action at once")
    }

    @Test
    fun actReSelectsWhenACooldownActionHasExpired() {
        World.allPortals.add(Portal.create(Pos(700, 400)))
        val agent = frogAt(Pos(200, 400))
        agent.action.item = ActionItem.RECYCLE // a cooldown action, not a movement handler
        agent.action.untilTick = World.tick - 1 // no longer busy → act() must re-select via doSomethingElse
        val next = agent.act()
        assertTrue(next.action.item != ActionItem.RECYCLE, "an expired cooldown action re-selects a fresh action")
    }

    @Test
    fun exploreEndsWhenTheWorldIsNotReady() {
        World.isReady = false
        val agent = frogAt(Pos(600, 400))
        agent.destination = Pos(1200, 400)
        agent.action.start(ActionItem.EXPLORE)
        val next = agent.act()
        assertEquals(ActionItem.WAIT, next.action.item, "a not-ready world ends the EXPLORE stroll")
    }

    // --- collectXm -----------------------------------------------------------

    @Test
    fun collectingXmToppedUpFromANearbyHeap() {
        val agent = frogAt(Pos(300, 300))
        agent.removeXm(agent.xm) // empty the bar so there is headroom to collect
        XmMap.createStrayXm(agent.pos, isPortalDrop = false)
        agent.action.item = ActionItem.WAIT
        val next = agent.act()
        assertTrue(next.xm > 0, "act() sweeps up stray XM within collection range")
    }

    // --- attack handler ------------------------------------------------------

    @Test
    fun attackFirstOnACrippledPortalAimsAtAResonator() {
        val portal = enemyPortalAt(Pos(600, 400))
        portal.filledSlots().forEach { it.resonator?.energy = 1 } // crippled → health rounds to 0
        val agent = frogAt(Pos(600, 400))
        agent.attackPortal(true)
        assertEquals(ActionItem.ATTACK, agent.action.item, "attackPortal(true) commits to ATTACK")
        assertTrue(agent.destination != Pos(0, 0), "a destination in-range spot is chosen")
    }

    @Test
    fun attackEndsWhenTheTargetIsNoLongerValid() {
        val portal = Portal.create(Pos(400, 400)) // neutral: no resonators → invalid attack target
        World.allPortals.add(portal)
        val agent = frogAt(Pos(400, 400))
        agent.actionPortal = portal
        agent.destination = agent.pos // arrived
        agent.action.start(ActionItem.ATTACK)
        val next = agent.attackPortal(false)
        assertEquals(ActionItem.WAIT, next.action.item, "attacking a dead/neutral portal ends the action")
    }

    @Test
    fun attackFiresWhenArrivedAndArmed() {
        val portal = enemyPortalAt(Pos(600, 400))
        val agent = frogAt(Pos(600, 400))
        agent.actionPortal = portal
        agent.destination = agent.pos // arrived
        repeat(30) { agent.inventory.items.add(XmpBurster.create(agent, 1)) }
        agent.addXm(agent.xmCapacity())
        agent.action.start(ActionItem.ATTACK)
        val energyBefore = portal.filledSlots().sumOf { it.resonator?.energy ?: 0 }
        agent.attackPortal(false)
        assertTrue(
            portal.filledSlots().sumOf {
                it.resonator?.energy ?: 0
            } < energyBefore,
            "an armed, in-range attacker damages resonators",
        )
    }

    @Test
    fun attackWithoutWeaponsEndsRatherThanIdles() {
        val portal = enemyPortalAt(Pos(600, 400))
        val agent = frogAt(Pos(600, 400))
        agent.actionPortal = portal
        agent.destination = agent.pos // arrived
        agent.inventory.items.clear() // no XMPs → can't fire
        agent.action.start(ActionItem.ATTACK)
        val next = agent.attackPortal(false)
        assertEquals(ActionItem.WAIT, next.action.item, "a worthy target but no XMPs → end + re-select, not idle")
    }

    @Test
    fun findsResonatorsInAttackRange() {
        val portal = enemyPortalAt(Pos(600, 400))
        val agent = frogAt(Pos(600, 400))
        agent.actionPortal = portal
        val resos = agent.findResosInAttackRange(XmpLevel.ONE)
        assertTrue(resos.isNotEmpty(), "the enemy portal's resonators register inside XMP range")
    }

    // --- deploy / capture handler -------------------------------------------

    @Test
    fun deployPlacesAResonatorOnArrival() {
        val portal = Portal.create(Pos(400, 400)) // neutral → capturable
        World.allPortals.add(portal)
        val agent = frogAt(Pos(400, 400))
        agent.actionPortal = portal
        agent.destination = agent.pos // arrived
        agent.action.start(ActionItem.DEPLOY)
        agent.deployPortal(false)
        assertTrue(portal.numberOfResosLeft() > 0, "an arrived deployer places a resonator")
    }

    @Test
    fun deployEndsWhenNothingCanBeDeployed() {
        val portal = Portal.create(Pos(400, 400))
        World.allPortals.add(portal)
        val agent = frogAt(Pos(400, 400))
        agent.actionPortal = portal
        agent.destination = agent.pos // arrived
        agent.inventory.items.clear() // no resonators / mods
        agent.action.start(ActionItem.DEPLOY)
        val next = agent.deployPortal(false)
        assertEquals(ActionItem.WAIT, next.action.item, "nothing to deploy → end + re-select")
    }

    @Test
    fun captureIsDeployUnderAnotherName() {
        val agent = frogAt(Pos(400, 400))
        agent.actionPortal = Portal.create(Pos(400, 400))
        agent.capturePortal(true)
        assertEquals(ActionItem.DEPLOY, agent.action.item, "capturePortal delegates to the DEPLOY handler")
    }

    // --- moveElsewhere / moveOptions ----------------------------------------

    @Test
    fun moveElsewhereWeighsEveryDestinationKind() {
        World.allPortals.add(Portal.create(Pos(400, 400))) // neutral → uncaptured option live
        val friendly = Portal.create(Pos(600, 400)) // ENL → friendly option live
        val fFrog = Factory.frog()
        friendly.deploy(fFrog, mapOf(Octant.N to Resonator.create(fFrog, 1)), Dim.maxDeploymentRange.toInt())
        World.allPortals.add(friendly)
        World.allPortals.add(enemyPortalAt(Pos(500, 300))) // RES → enemy options live (agent carries XMPs)
        val agent = frogAt(Pos(100, 100))
        repeat(5) { agent.inventory.items.add(XmpBurster.create(agent, 1)) } // hasXmps → enemy movement options weighed
        val next = agent.moveElsewhere()
        assertTrue(
            next.action.item == ActionItem.MOVE || next.action.item == ActionItem.EXPLORE,
            "with every destination kind present the agent still relocates deterministically",
        )
    }

    // --- player level-up sound path -----------------------------------------

    @Test
    fun playerAgentDingsUpOnCrossingALevel() {
        World.userFaction = Faction.ENL
        val agent = Factory.frog()
        agent.ap = 9_000 // level 1
        agent.addAp(500) // ×10 → +5000 → 14000 AP → level 2 (crosses → playLevelUp on the player's faction)
        assertEquals(2, agent.getLevel(), "the player agent levelled up")
    }

    // --- stuck-recovery re-target escalation --------------------------------

    @Test
    fun recoverIfStuckReTargetsAStuckMoverWithoutABeeline() {
        Portal.create(Pos(900, 600)).also { World.allPortals.add(it) }
        Portal.create(Pos(1000, 500)).also { World.allPortals.add(it) }
        val far = Portal.create(Pos(1700, 600))
        World.allPortals.add(far)
        val agent = frogAt(Pos(100, 600))
        agent.actionPortal = far
        agent.destination = far.location
        agent.action.start(ActionItem.MOVE)
        World.allAgents.add(agent)
        repeat(40) { StuckTracker.sample(listOf(agent.key() to agent.pos)) }
        assertTrue(StuckTracker.isStuck(agent.key()), "the pinned agent is flagged stuck")
        val destBefore = agent.destination
        agent.recoverIfStuck()
        assertTrue(agent.destination != destBefore, "a stuck MOVE re-targets straight away — a fresh portal means a fresh field")
    }

    /** A wedged recruiter used to be invisible to [StuckTracker] (MOVE-only) AND immortal (it re-started its own
     *  RECRUIT timer every tick), pinning its held NPC forever. Recovery must drop the target and re-select. */
    @Test
    fun recoverIfStuckAbortsAWedgedRecruiter() {
        World.allPortals.add(Portal.create(Pos(900, 600)))
        val npc = NonFaction.create(Factory.grid())
        World.allNonFaction.add(npc)
        val agent = frogAt(Pos(100, 600))
        agent.recruitTargetId = npc.id
        agent.destination = Pos(1700, 600) // far away → still "approaching", so it counts as travelling
        agent.action.start(ActionItem.RECRUIT)
        World.allAgents.add(agent)
        assertTrue(agent.isTravelling(), "an approaching recruiter is watched for stuckness")
        repeat(40) { StuckTracker.sample(listOf(agent.key() to agent.pos)) }
        agent.recoverIfStuck()
        assertEquals(null, agent.recruitTargetId, "the wedged recruiter drops its unreachable target")
        assertTrue(agent.action.item != ActionItem.RECRUIT, "and stops recruiting, so act() re-selects next tick")
    }

    /** A recruiter standing in the meeting is meant to be still — it must not read as stuck. */
    @Test
    fun aMeetingRecruiterIsNotTravelling() {
        val agent = frogAt(Pos(100, 600))
        agent.destination = agent.pos
        agent.action.start(ActionItem.RECRUIT)
        assertFalse(agent.isTravelling(), "an arrived recruiter is standing in the meeting, not travelling")
    }

    // --- XM bar helpers ------------------------------------------------------

    @Test
    fun xmBarReflectsFillState() {
        val agent = Factory.frog()
        agent.ap = 5_000
        agent.removeXm(agent.xm)
        assertTrue(agent.isXmLow(), "an empty bar is low (below the drained mark)")
        agent.addXm(agent.xmCapacity())
        assertFalse(agent.isXmLow(), "a topped-up bar is not low")
        assertTrue(agent.keySet().isEmpty(), "a fresh agent holds no portal keys")
    }

    // --- companion level / capacity tables ----------------------------------

    @Test
    fun xmCapacityFollowsTheLevelTable() {
        val cases = listOf(
            5_000 to (1 to 3_000),
            20_000 to (2 to 4_000),
            50_000 to (3 to 5_000),
            100_000 to (4 to 6_000),
            200_000 to (5 to 7_000),
            400_000 to (6 to 8_000),
            800_000 to (7 to 9_000),
            1_500_000 to (8 to 10_000),
            3_000_000 to (9 to 10_900),
            5_000_000 to (10 to 11_700),
            7_000_000 to (11 to 12_400),
            10_000_000 to (12 to 13_000),
            14_000_000 to (13 to 13_500),
            20_000_000 to (14 to 13_900),
            30_000_000 to (15 to 14_200),
            50_000_000 to (16 to 14_400),
        )
        val agent = Factory.frog()
        cases.forEach { (ap, expected) ->
            agent.ap = ap
            assertEquals(expected.first, agent.getLevel(), "AP $ap maps to level ${expected.first}")
            assertEquals(expected.second, agent.xmCapacity(), "level ${expected.first} XM capacity")
        }
    }
}
