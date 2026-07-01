package system.grid

import extension.VectorField
import util.data.Pos

/**
 * A [FieldFlow] sink that never delivers a field — [compute]'s callback is dropped, so callers keep their
 * [VectorField.EMPTY] and fall back to straight-line movement. Touches no [Pathfinding] / coroutine; the
 * clean install target for tests (and the pure default once this seam migrates to `commonMain`).
 */
object NoOpFieldFlow : FieldFlow {
    override fun compute(destination: Pos, onReady: (VectorField) -> Unit) = Unit
}
