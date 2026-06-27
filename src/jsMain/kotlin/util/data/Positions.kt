package util.data

import World
import config.Dim
import config.Sim
import extension.Grid
import system.ui.Bootstrap
import util.Rng

/**
 * Spawn / candidate-position factories — the World/grid-coupled half of the old `Pos` companion, relocated to
 * the JS shell so the pure [Pos] can live in `commonMain`. Call sites use `Positions.createRandomForPortal()`
 * etc. (was `Pos.createRandomForPortal()`).
 */
object Positions {
    private fun createRandomNoOffset() = Pos(Rng.randomInt(Sim.width), Rng.randomInt(Sim.height))

    /**
     * Valid portal-spawn cells (passable, inside the deploy margin, not too close to an existing portal) as
     * offset world positions — ONE grid scan; callers sample from the result. (Calling this per candidate is
     * what made best-candidate placement freeze world-gen.)
     */
    fun portalCandidates(): List<Pos> {
        val offset = Pos.res / 2
        // The grid IS the play area: passable cells already exclude everything outside the (circular) field —
        // ShadowGridBuilder.maskToCircle forces out-of-circle cells impassable at build, so sampling passable
        // cells already stays inside it. The old separate Sim.isInsideField pass was therefore redundant on a
        // real (masked) grid, and it broke headless matches whose Sim default didn't match their grid. Driving
        // placement off the live grid also future-proofs a play area that grows/moves at runtime: it's
        // re-queried each call, so candidates always track the current field.
        return World.passableInActionArea()
            .filterNot { it.key.fromShadow().x < Dim.maxDeploymentRange }
            .filterNot { it.key.fromShadow().x > World.simW() - Dim.maxDeploymentRange }
            .filterNot { it.key.fromShadow().hasClosePortal() }
            .map {
                val p = it.key.fromShadow()
                Pos(p.x + offset, p.y + offset)
            }
    }

    /** Whether a new portal can still be placed without clipping an existing one (a free, well-spaced
     *  candidate exists). Always true headless (no rendering → spacing irrelevant). Lets the density
     *  system skip discovery when the map is packed instead of failing to place. */
    fun hasPortalSpace(): Boolean = Bootstrap.isNotRunningInBrowser() || portalCandidates().isNotEmpty()

    fun createRandomForPortal(): Pos {
        if (!World.hasGrid()) return createRandomNoOffset() // bare unit test, no grid → any position will do
        val candidates = portalCandidates()
        if (candidates.isNotEmpty()) return candidates[(Rng.random() * candidates.size).toInt()]
        // No well-spaced candidate (a tiny or packed grid): fall back to any passable cell — grid-driven and in
        // sim coords, NEVER the old rectangular Rng(Sim.width) box, which ignored passability and the field shape
        // (and, headless, scattered portals off the match grid → coupled matches to the live Sim default size).
        return createRandomPassable(World.grid)
    }

    // Cache the passable-cell key list per grid (rebuilt only when the grid reference changes — once per
    // world / headless match). Picking a random passable cell was an O(cells) full shuffle + allocation on
    // every call (every recruit), which dominated headless-match cost; this makes it O(1) after the first call
    // (and fixes spawning on impassable cells — the old shuffle keyed off all cells).
    private var cachedGrid: Grid? = null
    private var cachedPassableKeys: List<Pos> = emptyList()

    private fun passableKeys(grid: Grid): List<Pos> {
        if (cachedGrid !== grid) {
            cachedGrid = grid
            cachedPassableKeys = grid.filterValues { it.isPassable }.keys.toList()
        }
        return cachedPassableKeys
    }

    fun createRandomPassable(grid: Grid) = createRandomPassable(grid, retries = 10)
    private fun createRandomPassable(grid: Grid, retries: Int): Pos {
        if (Bootstrap.isNotRunningInBrowser()) {
            val keys = passableKeys(grid)
            if (keys.isEmpty()) return Pos(0, 0)
            // Grid keys are SHADOW cells; agents/portals live in SIM space. Return the cell centre in sim
            // coords (× res), else headless spawns cluster in a shadow-sized corner of the map and never reach
            // the (sim-space) portals — no gameplay, no MU (broke AI training).
            val cell = keys[(Rng.random() * keys.size).toInt()]
            return Pos(cell.x * Pos.res + Pos.res / 2, cell.y * Pos.res + Pos.res / 2)
        }
        check(grid.isNotEmpty())
        val random = createRandomNoOffset()
        return if (grid[random.toShadow()]?.isPassable ?: false) {
            random
        } else {
            if (retries > 0) {
                createRandomPassable(grid, retries - 1)
            } else {
                console.warn("Blocked Position: $random")
                random // FIXME workaround..
            }
        }
    }
}
