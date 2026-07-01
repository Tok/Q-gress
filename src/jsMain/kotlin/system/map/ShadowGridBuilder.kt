package system.map

import World
import config.Config
import config.Sim
import extension.*
import kotlinx.browser.document
import org.khronos.webgl.Uint8ClampedArray
import org.khronos.webgl.get
import org.w3c.dom.ImageData
import system.grid.GridCapture
import system.grid.GridConnectivity
import system.grid.Pathfinding
import util.Debug
import util.data.Cell
import util.data.Pos

/**
 * Builds the passability [Grid] from the shadow map's WebGL readback — the map→grid pipeline split out of
 * [MapController] (the SoC / god-object split, PLAN phase B). Downsamples the street/terrain shadow into a
 * per-cell grid (a crisp pass for the hard passable test, a blurred pass for the movement cost), seals
 * closed-off pockets + joins on-screen regions ([GridConnectivity]), then masks the round arena.
 * [MapController.addGrid] does the raw pixel readback and hands the [ImageData] here.
 */
object ShadowGridBuilder {
    private val OFFSCREEN_CELL_ROWS = Sim.OFFSCREEN_CELL_ROWS

    fun build(imageData: ImageData, width: Int, height: Int): Grid {
        // Grid resolution follows the game canvas (CSS pixels), not the raw WebGL readback (which is
        // window × devicePixelRatio). The full readback is downscaled into this grid below, so the grid
        // stays aligned with the visible map regardless of the display's pixel ratio.
        val w = Sim.width / Pos.res
        val h = Sim.height / Pos.res

        val unscaledCan = document.createElement("canvas") as Canvas
        val unscaledCtx = unscaledCan.getContext("2d") as Ctx
        unscaledCan.width = width
        unscaledCan.height = height
        unscaledCtx.putImageData(imageData, 0.0, 0.0)

        // Crisp downsample → the hard passable/impassable test (walls + water stay sharp).
        val passCan = document.createElement("canvas") as Canvas
        val passCtx = CanvasFactory.readbackCtx(passCan)
        passCan.width = w
        passCan.height = h
        passCtx.drawImage(unscaledCan, 0, 0, w, h)

        // Blurred downsample → the movement COST only, so flow fields curve smoothly instead of
        // zig-zagging around blocky building edges. (The old `tempCan.blur()` was the DOM focus method —
        // a no-op; this uses a real canvas blur filter.)
        val costCan = document.createElement("canvas") as Canvas
        val costCtx = CanvasFactory.readbackCtx(costCan)
        costCan.width = w
        costCan.height = h
        costCtx.asDynamic().filter = "blur(${Config.shadowBlurCount}px)"
        costCtx.drawImage(unscaledCan, 0, 0, w, h)
        costCtx.asDynamic().filter = "none"

        val passData = passCtx.getImageData(0, 0, w, h).data
        val costData = costCtx.getImageData(0, 0, w, h).data
        val rawGrid: Grid = (-OFFSCREEN_CELL_ROWS until (w + OFFSCREEN_CELL_ROWS)).flatMap { x ->
            nextRow(passData, costData, w, h, x)
        }.toMap()
        // No closed-off areas + on-screen routes: seal pockets to the outside AND join on-screen regions
        // directly (else agents detour around the map edge between them and look stuck). The connect pass is
        // circle-aware ([inCircle]) so it never carves a corridor through an on-screen cell the round mask
        // then blocks — which used to re-sever corridors and re-fragment the grid. Mask AFTER (now a no-op on
        // connectivity, since no corridor rides an out-of-circle cell) so walkability/flow truly stay in the circle.
        val grid = maskToCircle(GridConnectivity.connectIslands(rawGrid, w, h, inCircle(w, h)), w, h)
        World.walkability = GridConnectivity.walkability(grid, w, h)
        console.log(
            "grid built: walkability ${(World.walkability * 100).toInt()}% (${GridConnectivity.components(
                rawGrid,
            ).size} islands connected)",
        )
        if (Debug.enabled) logConnectivity(rawGrid, grid, w, h)
        if (Debug.enabled) logFootpathSignal(passData, w, h)
        if (Debug.mode == "capture") GridCapture.onGridBuilt(rawGrid, w, h) // raw passability snapshot for fixtures
        return grid
    }

    // Read each downsampled canvas back ONCE (full RGBA buffer) and index per cell — a single-pixel
    // getImageData per cell would be w×h readbacks (slow, and trips the willReadFrequently warning).
    private fun nextRow(passData: Uint8ClampedArray, costData: Uint8ClampedArray, w: Int, h: Int, x: Int): List<Pair<Pos, Cell>> {
        fun isOffScreen(pos: Pos) = pos.x < 0 || pos.y < 0 || pos.x >= w || pos.y >= h
        return (-OFFSCREEN_CELL_ROWS until (h + OFFSCREEN_CELL_ROWS)).map { y ->
            val pos = Pos(x, y)
            if (isOffScreen(pos)) {
                pos to Cell(pos, true, 80)
            } else {
                // Crisp pixel → the hard passable test (a blur must NOT bleed walls/water open);
                // blurred pixel → the movement COST, so flow fields read smooth (not jagged grid routes).
                val passabilityOffset = 32
                val idx = (y * w + x) * 4 // red channel of cell (x, y) in the RGBA buffer
                val passPixel = passData[idx]
                val costPixel = costData[idx]
                val isPassable = passPixel > passabilityOffset
                val penalty =
                    Pathfinding.MIN_HEAT + ((255 - costPixel) * (Pathfinding.MAX_HEAT - Pathfinding.MIN_HEAT) / 255)
                pos to Cell(pos, isPassable, penalty)
            }
        }
    }

    // ?debug: since roads now render mid-grey and ONLY footpaths render white, this reports whether footpath
    // data actually lands in the downsampled grid at this location — the brightest cell + how many are near-
    // white. brightest ≈ 255 ⇒ footpaths present; brightest well below 255 (e.g. ~154 grass, ~110 wood) ⇒ no
    // footpath features rendered here (nothing to prefer), so it's OSM coverage / zoom, not our colouring.
    private fun logFootpathSignal(passData: Uint8ClampedArray, w: Int, h: Int) {
        var white = 0
        var brightest = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val v = passData[(y * w + x) * 4].toInt() and 0xFF // red channel, as unsigned 0..255
                if (v >= 240) white++
                if (v > brightest) brightest = v
            }
        }
        console.log("[debug] footpaths: brightest shadow cell $brightest (≈255 ⇒ present), near-white cells $white / ${w * h}")
    }

    // The play area as a predicate for [GridConnectivity.connectIslands] — the SAME inscribed circle
    // [maskToCircle] enforces, so the connector never carves a corridor through a cell the mask will block.
    // Non-round fields play the whole rectangle, so every cell is "in play".
    private fun inCircle(w: Int, h: Int): (Pos) -> Boolean {
        if (!Sim.roundField) return { true }
        val cx = w / 2.0
        val cy = h / 2.0
        val rSq = (minOf(w, h) / 2.0).let { it * it }
        return { pos -> (pos.x - cx) * (pos.x - cx) + (pos.y - cy) * (pos.y - cy) <= rSq }
    }

    /** Round field: force on-screen cells outside the inscribed circle impassable (a true circular arena). */
    internal fun maskToCircle(grid: Grid, w: Int, h: Int): Grid {
        if (!Sim.roundField) return grid
        val cx = w / 2.0
        val cy = h / 2.0
        val rSq = (minOf(w, h) / 2.0).let { it * it }
        // Mask on-screen cells outside the circle (keeps agents/portals/flow in the round arena). The
        // off-screen ring stays passable so ambient NPCs can roam in and out of the map; the overlays
        // clip the display to the play area, so off-area flow never shows anyway.
        return grid.mapValues { (pos, cell) ->
            val onScreen = pos.x >= 0 && pos.y >= 0 && pos.x < w && pos.y < h
            val outside = (pos.x - cx) * (pos.x - cx) + (pos.y - cy) * (pos.y - cy) > rSq
            if (onScreen && outside && cell.isPassable) Cell(pos, false, cell.movementPenalty) else cell
        }
    }

    // ?debug connectivity self-check: how walkable + how connected the built grid is. on-screen islands
    // > 1 means playable regions only reach each other via the off-screen ring → detour/wander hazard.
    private fun logConnectivity(rawGrid: Grid, grid: Grid, w: Int, h: Int) {
        val before = GridConnectivity.report(rawGrid, w, h)
        val after = GridConnectivity.report(grid, w, h)
        console.log(
            "[debug] connectivity — islands ${before.islands}→${after.islands}, " +
                "on-screen islands ${before.onScreenIslands}→${after.onScreenIslands}, " +
                "walkability ${(after.walkability * 100).toInt()}%",
        )
        if (!after.isHealthy) {
            console.warn(
                "[debug] UNHEALTHY grid: ${after.onScreenIslands} on-screen regions only reach each other " +
                    "via the off-screen ring — agents/NPCs may path the long way around and look stuck.",
            )
        }
    }
}
