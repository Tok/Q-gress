package agent

import World
import config.Dim
import config.Sim
import extension.Grid
import extension.VectorField
import system.grid.GridConnectivity
import system.grid.Pathfinding
import util.Rng
import util.data.Cell
import util.data.Complex
import util.data.Pos
import util.data.isPassable
import util.data.toShadow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Why agents wedge on buildings, pinned down over a **realistic pipeline grid** (random buildings →
 * [GridConnectivity.connectIslands] → round-arena mask), driven by the **real** step math
 * ([Movement.move] → [Movement.clampToPlayable]) from every passable start cell.
 *
 * The headline: a **flow field routes around walls, a bare heading does not**. Field steering strands nobody;
 * steering a bare heading at a far target strands ~48% of start positions, in the concave corner it slides into
 * — sliding out of the pocket points it straight back in. That is why [Movement.hasClearPath] gates every
 * straight-line target, why the bee-line recovery is gone, and why [Agent.isTravelling] (not "is on MOVE") is
 * what [StuckTracker] watches.
 *
 * Underneath all of it sat [Movement.step]: it used to truncate the agent's position to whole pixels each tick,
 * silently dropping any velocity component below 1 px/tick. That alone pinned agents on flat walls, and is the
 * reason NPCs never wedged — they always stepped continuously.
 */
class StuckNavigationTest {
    private val w = 60
    private val h = 60
    private val ring = 14
    private val speed = 4.0 // a mid [Skills] walk speed; one tick's step
    private val ticks = 4000

    private lateinit var grid: Grid

    private fun inCircle(): (Pos) -> Boolean {
        val cx = w / 2.0
        val cy = h / 2.0
        val rSq = (minOf(w, h) / 2.0).let { it * it }
        return { p -> (p.x - cx) * (p.x - cx) + (p.y - cy) * (p.y - cy) <= rSq }
    }

    private fun onScreen(p: Pos) = p.x >= 0 && p.y >= 0 && p.x < w && p.y < h

    /** Random buildings, then the real grid pipeline: seal pockets + join play regions, then mask the arena. */
    private fun pipelineGrid(seed: Int): Grid {
        Rng.seed(seed)
        val blocks = (0 until 30).map {
            intArrayOf(Rng.randomInt(2, w - 9), Rng.randomInt(2, h - 9), Rng.randomInt(3, 9), Rng.randomInt(3, 9))
        }
        fun blocked(x: Int, y: Int) = blocks.any { x >= it[0] && x < it[0] + it[2] && y >= it[1] && y < it[1] + it[3] }
        val raw = HashMap<Pos, Cell>()
        for (x in -ring until w + ring) {
            for (y in -ring until h + ring) {
                val p = Pos(x, y)
                raw[p] = if (!onScreen(p)) Cell(p, true, 80) else Cell(p, !blocked(x, y), Pathfinding.MIN_HEAT)
            }
        }
        val connected = GridConnectivity.connectIslands(raw, w, h, inCircle())
        val inC = inCircle()
        return connected.mapValues { (p, c) -> if (onScreen(p) && !inC(p) && c.isPassable) Cell(p, false, c.movementPenalty) else c }
    }

    private fun playCells(g: Grid) = g.filter { (p, c) -> c.isPassable && inCircle()(p) && onScreen(p) }.keys
        .sortedWith(compareBy({ it.x }, { it.y }))

    private fun centreOf(cell: Pos) = Pos(cell.x * Pos.res + 5, cell.y * Pos.res + 5)

    /** The agent travel loop, over the REAL [Movement.step] the game uses; [field] null = bare-heading steering. */
    private fun travel(start: Pos, goal: Pos, field: VectorField?, arrive: Double): Boolean {
        var pos = start
        var vel = Complex.ZERO
        repeat(ticks) {
            if (pos.distanceTo(goal) < arrive) return true
            val force = field?.get(pos.toShadow()) ?: Movement.headingTo(pos, goal)
            val (next, deflected) = Movement.step(pos, Movement.move(vel, force, speed))
            pos = next
            vel = deflected
        }
        return false
    }

    @BeforeTest
    fun setup() {
        Sim.roundField = true
        Sim.setSize(w * Pos.res, h * Pos.res)
        grid = pipelineGrid(7)
        World.grid = grid
    }

    @AfterTest
    fun tidy() {
        World.grid = emptyMap()
        Sim.roundField = true
    }

    /** The grid pipeline leaves one connected play region, so every start CAN reach every goal. */
    @Test
    fun thePipelineGridIsFullyConnected() {
        val report = GridConnectivity.report(grid, w, h)
        assertEquals(1, report.onScreenIslands, "the masked play area must be one region — else a goal is unreachable")
    }

    /** Flow-field steering: the routing we rely on. Nobody is stranded, from anywhere, on any building layout. */
    @Test
    fun flowFieldSteeringStrandsNobody() {
        val cells = playCells(grid)
        val goalCell =
            cells.minByOrNull { (it.x - (w - 3.0)) * (it.x - (w - 3.0)) + (it.y - h / 2.0) * (it.y - h / 2.0) } ?: error("no cell")
        val goal = centreOf(goalCell)
        Rng.seed(1)
        val field = Pathfinding.computeFieldSync(goal, grid)
        val stranded = cells.map { centreOf(it) }
            .filter { it.distanceTo(goal) >= Dim.maxDeploymentRange }
            .count { !travel(it, goal, field, Dim.maxDeploymentRange) }
        assertEquals(0, stranded, "the flow field routes around every building, from every start cell")
    }

    /** …and the same walk on a bare heading strands a large fraction — the reason [Movement.hasClearPath] exists.
     *  If this ever drops to 0, straight-line steering became safe and the clear-path gate could be revisited. */
    @Test
    fun bareHeadingSteeringStrandsAgentsBehindBuildings() {
        val cells = playCells(grid)
        val goalCell =
            cells.minByOrNull { (it.x - (w - 3.0)) * (it.x - (w - 3.0)) + (it.y - h / 2.0) * (it.y - h / 2.0) } ?: error("no cell")
        val goal = centreOf(goalCell)
        val starts = cells.map { centreOf(it) }.filter { it.distanceTo(goal) >= Dim.maxDeploymentRange }
        val stranded = starts.count { !travel(it, goal, null, Dim.maxDeploymentRange) }
        assertTrue(
            stranded > starts.size / 10,
            "a bare heading wedges in concave corners (~48% of starts); got $stranded/${starts.size}",
        )
    }

    // --- the clear-path gate --------------------------------------------------

    @Test
    fun hasClearPathRejectsASegmentThroughAWall() {
        val wall = grid.entries.first { !it.value.isPassable && onScreen(it.key) }.key
        // Straddle the wall cell: the segment from one side to the other must cross it.
        val from = centreOf(Pos(wall.x - 2.0, wall.y))
        val to = centreOf(Pos(wall.x + 2.0, wall.y))
        if (from.isPassable() && to.isPassable()) {
            assertFalse(Movement.hasClearPath(from, to), "a segment crossing a wall cell is not a clear path")
        }
    }

    @Test
    fun hasClearPathAcceptsAnOpenSegmentAndRejectsAWallEndpoint() {
        val open = playCells(grid).first { c -> (0..3).all { grid[Pos(c.x + it, c.y)]?.isPassable == true } }
        assertTrue(Movement.hasClearPath(centreOf(open), centreOf(Pos(open.x + 3.0, open.y))), "open ground is a clear path")
        val wall = grid.entries.first { !it.value.isPassable && onScreen(it.key) }.key
        assertFalse(Movement.hasClearPath(centreOf(open), centreOf(wall)), "a target standing in a wall is never reachable")
    }

    /** Every wander destination is reachable in a straight line — [Agent.wanderStep] has no flow field to save it. */
    @Test
    fun openGroundNearOnlyPicksClearPathDestinations() {
        val cells = playCells(grid)
        Rng.seed(5)
        var checked = 0
        repeat(400) {
            val from = centreOf(cells[Rng.randomInt(0, cells.size - 1)])
            val dest = Movement.openGroundNear(from)
            if (dest == from) return@repeat // hemmed in on every probe → holds and re-selects; never a blind walk
            checked++
            assertTrue(Movement.hasClearPath(from, dest), "wander destination $dest is not walkable in a straight line from $from")
            assertTrue(Sim.isInPlayArea(dest.x, dest.y), "wander destination $dest left the play area")
        }
        assertTrue(checked > 300, "openGroundNear should nearly always find a target; only found $checked/400")
    }

    /** Clear-path targets make bare-heading steering safe: a wanderer always reaches the ground it picked. */
    @Test
    fun wanderingToClearPathTargetsNeverStrands() {
        val cells = playCells(grid)
        Rng.seed(3)
        var stranded = 0
        var total = 0
        repeat(800) {
            val from = centreOf(cells[Rng.randomInt(0, cells.size - 1)])
            val dest = Movement.openGroundNear(from)
            if (dest == from) return@repeat
            total++
            if (!travel(from, dest, null, speed)) stranded++
        }
        assertTrue(total > 700, "sanity: openGroundNear nearly always finds a target; got $total/800")
        assertEquals(0, stranded, "a clear-path wander target is always reachable on a bare heading")
    }

    /** On open ground a wanderer always arrives — nothing deflects its approach, so it walks straight in. */
    @Test
    fun aWanderAlwaysArrivesOnOpenGround() {
        val inC = inCircle()
        World.grid = grid.mapValues { (p, c) -> Cell(p, inC(p) || !onScreen(p), c.movementPenalty) } // no buildings
        val cells = playCells(World.grid)
        Rng.seed(3)
        var stranded = 0
        repeat(600) {
            val from = centreOf(cells[Rng.randomInt(0, cells.size - 1)])
            val angle = Rng.random() * 2.0 * PI
            val dist = Dim.maxDeploymentRange + Rng.random() * 200.0
            val dest = Pos((from.x + dist * cos(angle)).toInt(), (from.y + dist * sin(angle)).toInt())
            if (!dest.isPassable() || !Sim.isInPlayArea(dest.x, dest.y)) return@repeat
            if (!travel(from, dest, null, speed)) stranded++
        }
        assertEquals(0, stranded, "a wanderer on open ground always reaches its target")
    }

    // --- the sub-pixel truncation that wedged agents on buildings ------------

    /** [Movement.step] must carry a velocity component under 1 px/tick. Truncating the step to whole pixels
     *  discarded it every tick, so an agent on a shallow diagonal walked dead-straight into a wall it should
     *  have rounded — the single biggest cause of agents pinned against buildings. */
    @Test
    fun aStepKeepsItsSubPixelComponent() {
        val open = playCells(grid).first { c -> (-2..2).all { d -> grid[Pos(c.x + d, c.y + 1)]?.isPassable == true } }
        val from = centreOf(open)
        val (next, _) = Movement.step(from, Complex(0.4, 3.9)) // 0.4 px/tick sideways: under one whole pixel
        assertTrue(next.x > from.x, "a sub-pixel sideways component must move the agent, not be silently dropped")
        assertTrue(next.y > from.y, "and the dominant component still advances")
    }

    /** Pressed against a flat wall on a heading that is mostly into it, an agent must still SLIDE along it.
     *  With a truncated step the sideways component rounded to zero, the clamp reset velocity to the blocked
     *  delta, and the agent re-derived the same doomed unit force every tick — a permanent wedge. */
    @Test
    fun anAgentPressedIntoAWallSlidesAlongItRatherThanFreezing() {
        // A passable cell with a wall directly to its right (+x) and open ground above/below it.
        val wallSide = playCells(grid).first { c ->
            grid[Pos(c.x + 1.0, c.y)]?.isPassable == false && grid[Pos(c.x, c.y + 1.0)]?.isPassable == true
        }
        var pos = centreOf(wallSide)
        var vel = Complex.ZERO
        val start = pos
        repeat(40) {
            val into = Complex(0.97, 0.24) // hard into the wall, slightly down: the sideways part is sub-pixel
            val (next, deflected) = Movement.step(pos, Movement.move(vel, into, speed))
            pos = next
            vel = deflected
        }
        assertTrue(pos.y - start.y > Pos.res, "an agent hugging a wall slides along it (moved ${pos.y - start.y} px)")
    }

    /** A recruiter chases its NPC on a bare heading, and NPCs stand inside buildings — so the target it picks
     *  must be one it can walk to. [NonFaction.findNearestReachableTo] is what makes that walk safe. */
    @Test
    fun recruitersOnlyTargetNpcsTheyCanWalkTo() {
        val cells = playCells(grid)
        Rng.seed(9)
        var reachableChases = 0
        var stranded = 0
        repeat(300) {
            val from = centreOf(cells[Rng.randomInt(0, cells.size - 1)])
            // An NPC anywhere on the map — wall or open, exactly as NPCs roam (they never clamp to passable ground).
            val npc = centreOf(Pos(Rng.randomInt(0, w - 1), Rng.randomInt(0, h - 1)))
            if (from.distanceTo(npc) < Dim.maxDeploymentRange) return@repeat
            if (!npc.isPassable() || !Movement.hasClearPath(from, npc)) return@repeat // the recruiter would skip this NPC
            reachableChases++
            if (!travel(from, npc, null, Dim.maxDeploymentRange)) stranded++
        }
        assertTrue(reachableChases > 20, "sanity: some NPCs are reachable; got $reachableChases")
        assertEquals(0, stranded, "a clear-path NPC is always reachable on a bare heading")
    }
}
