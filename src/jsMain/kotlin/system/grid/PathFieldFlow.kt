package system.grid

import config.Config
import extension.Grid
import extension.VectorField
import system.ui.Bootstrap
import util.data.Pos

/**
 * The real [FieldFlow] sink: forwards to the coroutine-bound [Pathfinding]. Self-branches per call —
 * **async** in the browser (frame-yielding, so world-gen doesn't jank), **inline sync** in a headless match
 * that opted in ([Config.headlessFieldCompute], e.g. an `ai.SimRunner` eval that wants real flow fields), or
 * **skip** otherwise (the caller keeps [VectorField.EMPTY] → cheap straight-line movement). Preserves the
 * exact branching the call sites used to inline; merely referencing this object touches no coroutine (the
 * skip branch — the Node-test default — never calls into [Pathfinding]).
 */
object PathFieldFlow : FieldFlow {
    override fun compute(destination: Pos, grid: Grid, onReady: (VectorField) -> Unit) {
        when {
            Bootstrap.isRunningInBrowser() -> PathfindingAsync.computeFieldAsync(destination, grid, onReady)
            Config.headlessFieldCompute -> onReady(Pathfinding.computeFieldSync(destination, grid))
            else -> Unit // fields skipped: caller keeps its empty field + straight-line fallback
        }
    }
}
