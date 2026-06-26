package util.data

import World
import config.Dim
import config.Sim
import extension.Grid
import util.HtmlUtil
import util.Util

/**
 * Spawn / candidate-position factories — the World/grid-coupled half of the old `Pos` companion, relocated to
 * the JS shell so the pure [Pos] can live in `commonMain`. Call sites use `Positions.createRandomForPortal()`
 * etc. (was `Pos.createRandomForPortal()`).
 */
object Positions {
    private fun createRandomNoOffset() = Pos(Util.randomInt(Sim.width), Util.randomInt(Sim.height))

    /**
     * Valid portal-spawn cells (passable, inside the deploy margin, not too close to an existing portal) as
     * offset world positions — ONE grid scan; callers sample from the result. (Calling this per candidate is
     * what made best-candidate placement freeze world-gen.)
     */
    fun portalCandidates(): List<Pos> {
        val offset = Pos.res / 2
        return World.passableInActionArea()
            .filterNot { it.key.fromShadow().x < Dim.maxDeploymentRange }
            .filterNot { it.key.fromShadow().x > World.simW() - Dim.maxDeploymentRange }
            .filterNot { it.key.fromShadow().hasClosePortal() }
            .map {
                val p = it.key.fromShadow()
                Pos(p.x + offset, p.y + offset)
            }
            .filter { Sim.isInsideField(it.x, it.y) } // round field → only spawn within the inscribed ellipse
    }

    /** Whether a new portal can still be placed without clipping an existing one (a free, well-spaced
     *  candidate exists). Always true headless (no rendering → spacing irrelevant). Lets the density
     *  system skip discovery when the map is packed instead of failing to place. */
    fun hasPortalSpace(): Boolean = HtmlUtil.isNotRunningInBrowser() || portalCandidates().isNotEmpty()

    fun createRandomForPortal(): Pos {
        if (HtmlUtil.isNotRunningInBrowser()) {
            return Pos(Util.randomInt(Sim.width), Util.randomInt(Sim.height))
        }
        val candidates = portalCandidates()
        check(candidates.isNotEmpty()) // map is blocked or there is no more space left.
        return candidates[(Util.random() * candidates.size).toInt()]
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
        if (HtmlUtil.isNotRunningInBrowser()) {
            val keys = passableKeys(grid)
            if (keys.isEmpty()) return Pos(0, 0)
            // Grid keys are SHADOW cells; agents/portals live in SIM space. Return the cell centre in sim
            // coords (× res), else headless spawns cluster in a shadow-sized corner of the map and never reach
            // the (sim-space) portals — no gameplay, no MU (broke AI training).
            val cell = keys[(Util.random() * keys.size).toInt()]
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
