package system.grid

import World
import extension.Grid
import extension.VectorField
import util.data.Pos

/**
 * The imperative-shell boundary for **flow-field computation** — the one piece of the portal/agent path that
 * can't be pure: building a [VectorField] runs off the coroutine-bound [Pathfinding] (async frame-yielding in
 * the browser, deterministic inline headless, or skipped entirely for cheap straight-line movement). Logic
 * calls `Nav.sink.compute(destination) { field -> … }`; the field is delivered to the callback **async**
 * in-browser ([PathFieldFlow] via `Pathfinding.computeFieldAsync`), **inline** in a headless match that opts
 * in (`Config.headlessFieldCompute` → `computeFieldSync`), or **never** otherwise — the caller keeps its
 * [VectorField.EMPTY] and falls back to a straight-line heading.
 *
 * Mirrors the [system.effect.Effects] / [system.audio.Audio] seams: a thin interface so the entities no
 * longer name the coroutine-bound `Pathfinding` object directly. The PLAN Phase-B prerequisite for lifting
 * `Portal`/`Agent`/`NonFaction` into `commonMain`; the seam migrates with that batch.
 */
interface FieldFlow {
    /**
     * Request the flow field to [destination] over [grid], delivering it to [onReady] when ready. [onReady]
     * runs inline (same call) for the synchronous/headless sink, on a later frame for the async/browser sink,
     * or not at all when fields are skipped — so a caller that needs the value synchronously must read its own
     * cache after this returns (it'll be populated only in the inline case), else use [VectorField.EMPTY].
     *
     * [grid] defaults to the masked [World.gridOrEmpty] (agents/portals stay in the round arena); ambient NPCs
     * pass [World.npcGrid] (unmasked) so they route clear across the whole map.
     */
    fun compute(destination: Pos, grid: Grid = World.gridOrEmpty, onReady: (VectorField) -> Unit)
}
