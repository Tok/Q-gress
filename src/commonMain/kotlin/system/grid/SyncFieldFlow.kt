package system.grid

import config.Config
import extension.VectorField
import util.data.Pos

/**
 * A pure, **synchronous** [FieldFlow] sink built on the commonMain [Pathfinding] core: when a headless match
 * opts in ([Config.headlessFieldCompute]) it computes the flow field inline; otherwise it skips (the caller
 * keeps its empty field + straight-line fallback). This is what [ai.SimRunner] binds so headless matches
 * (Node/JVM tests + the in-Node trainer) compute real fields with no coroutine — the browser live game binds
 * the frame-yielding jsMain [PathFieldFlow] instead.
 */
object SyncFieldFlow : FieldFlow {
    override fun compute(destination: Pos, onReady: (VectorField) -> Unit) {
        if (Config.headlessFieldCompute) onReady(Pathfinding.computeFieldSync(destination))
    }
}
