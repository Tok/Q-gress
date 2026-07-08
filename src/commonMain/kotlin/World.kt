import agent.Agent
import agent.Faction
import agent.NonFaction
import config.Config
import config.Dim
import config.Sim
import extension.Grid
import portal.Portal
import util.Rng
import util.data.*
import kotlin.math.sqrt

object World {
    var tick: Int = 0
    var isReady = false

    // Whether idle agents may churn the board's portal count (agent.action.cond.Discoverer). On in the live game;
    // the title sim turns it OFF so its curated arena stays put (the title has no Cycle and shouldn't drift).
    var portalDiscoveryEnabled = true
    var userFaction: Faction? = null

    fun userFactionOrThrow(): Faction = requireNotNull(userFaction) { "user faction has not been chosen yet" }

    // The offscreen passability-readback canvas + street→ImageData conversion live in the imperative edge
    // ([system.grid.StreetImage]); the NPC drop-in spawner in [system.NonFactionSpawner] — both split out so
    // World stays pure game state.

    // var center: JSON = MapController.INITIAL_MAP_CENTER
    var mousePos: Pos? = null

    // Screen extent — the fixed play area (Dim); used for on/off-screen tests and sound ratios.
    fun w() = Dim.width
    fun h() = Dim.height

    // Simulation/grid extent (Sim, larger than the screen) — world bounds & grid cells.
    fun simW() = Sim.width
    fun simH() = Sim.height
    fun shadowW() = Sim.width / Pos.res
    fun shadowH() = Sim.height / Pos.res
    fun diagonalLength() = sqrt((Dim.width.toDouble() * Dim.width) + (Dim.height.toDouble() * Dim.height)).toInt()
    fun totalArea() = Dim.width * Dim.height

    lateinit var grid: Grid
    var walkability = 0.0 // fraction of on-screen cells that are passable (set at grid build)

    // The grid AMBIENT NPCs navigate on. Same as [grid] but WITHOUT the round-arena mask, so NPCs walk clear
    // through the whole map (edge to edge, across the moat) instead of hitting the play-field border like a wall
    // — the border confines only agents (via the masked [grid] + clampToPlayable). Set at build; falls back to
    // the masked grid when unset (headless / bare tests) so NPCs behave as before there.
    private var npcGridOrNull: Grid? = null
    var npcGrid: Grid
        // Fall back to the masked grid, or an empty grid when no world is built (bare tests still evaluate this
        // arg into a no-op [system.grid.NoOpFieldFlow.compute], so it must not throw on uninitialized [grid]).
        get() = npcGridOrNull ?: if (::grid.isInitialized) grid else emptyMap()
        set(value) {
            npcGridOrNull = value
        }

    /** Test hook: drop the NPC-grid override so [npcGrid] falls back to [grid] again (keeps test isolation). */
    fun resetNpcGrid() {
        npcGridOrNull = null
    }

    /** Whether the passability grid is set yet — false in bare unit tests that build an entity without a world. */
    fun hasGrid() = ::grid.isInitialized

    /** The masked grid, or an empty grid when no world is built — the safe default for the flow-field seam so a
     *  bare-test portal (NoOp sink) never trips the `lateinit` [grid] just by evaluating the default argument. */
    val gridOrEmpty: Grid get() = if (::grid.isInitialized) grid else emptyMap()

    fun passableCells(): Grid = grid.filter { it.value.isPassable }
    private fun wellPassableCells(): Grid = grid.filter { it.value.isPassableInAllDirections() }
    private fun passableOnScreen(): Grid = wellPassableCells().filterNot { it.key.isOffGrid() }

    // Portals may spawn anywhere passable in the (larger) play area; the old screen-region
    // block was a 2D-era restriction.
    fun passableInActionArea(): Grid = passableOnScreen()

    val allAgents: MutableSet<Agent> = mutableSetOf()

    // Agents recruited mid-tick are buffered here and flushed after the agent
    // loop, so we never mutate allAgents while iterating it (avoids CME).
    val pendingAgents: MutableList<Agent> = mutableListOf()

    fun flushPendingAgents() {
        if (pendingAgents.isNotEmpty()) {
            allAgents.addAll(pendingAgents)
            pendingAgents.clear()
        }
    }

    val frogs = allAgents.filter { it.faction == Faction.ENL }.toSet()
    val smurfs = allAgents.filter { it.faction == Faction.RES }.toSet()
    fun countAgents() = allAgents.count()
    fun countAgents(fact: Faction) = allAgents.count { it.faction == fact }
    fun canRecruitMore(fact: Faction) = countAgents(fact) < Config.maxFor(fact) // size-scaled cap; rosters grow to it

    val allNonFaction: MutableSet<NonFaction> = mutableSetOf()
    fun countNonFaction() = allNonFaction.count()

    val allPortals: MutableList<Portal> = mutableListOf()
    fun randomPortal() = allPortals[(Rng.random() * (World.allPortals.size - 1)).toInt()]
    fun enlPortals() = allPortals.filter { it.owner?.faction == Faction.ENL }
    fun resPortals() = allPortals.filter { it.owner?.faction == Faction.RES }
    fun unclaimedPortals() = allPortals.filter { it.owner == null }
    fun factionPortals(fact: Faction) = allPortals.filter { it.owner?.faction == fact }
    fun countPortals() = allPortals.count()
    fun countPortals(fact: Faction) = factionPortals(fact).count()

    fun allLinks() = allPortals.flatMap { it.links }
    fun countLinks() = allLinks().count()
    fun countLinks(fact: Faction) = allLinks().filter { it.creator.faction == fact }.count()

    fun allFields() = allPortals.flatMap { it.fields }
    fun countFields() = allFields().count()
    fun countFields(fact: Faction) = allFields().filter { it.owner.faction == fact }.count()

    fun allLines() = allLinks().map { it.getLine() }

    /**
     * Safety net for link/field integrity: drop any link or field that has become invalid — an endpoint
     * portal vanished from the board, went neutral, or no longer shares the others' faction (e.g. it was
     * virus-flipped the same moment a link was being made). The normal teardown
     * ([portal.Portal.destroy]/[portal.Portal.refactor]) already does this; this guards against any path
     * that slips past it, so a link can never dangle to a portal that's gone or now enemy. Run each tick.
     */
    fun pruneInvalidLinksAndFields() {
        val present = allPortals.toHashSet()
        allPortals.forEach { portal ->
            portal.links.retainAll { sameFactionPresent(it.origin, it.destination, present) }
            portal.fields.retainAll {
                sameFactionPresent(it.origin, it.primaryAnchor, present) && sameFactionPresent(it.origin, it.secondaryAnchor, present)
            }
        }
    }

    // Both portals are still on the board AND owned by the same (non-null) faction — the link/field invariant.
    private fun sameFactionPresent(a: Portal, b: Portal, present: Set<Portal>): Boolean {
        val factionA = a.owner?.faction
        return a in present && b in present && factionA != null && factionA == b.owner?.faction
    }

    fun calcTotalMu(fact: Faction) = allFields().filter { it.owner.faction == fact }.map { it.calculateMu() }.sum()
}
