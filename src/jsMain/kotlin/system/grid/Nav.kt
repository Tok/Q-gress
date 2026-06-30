package system.grid

/**
 * The live [FieldFlow] sink — the install point for the flow-field-compute boundary. Defaults to the real
 * [PathFieldFlow] (which itself self-branches browser-async / headless-sync / skip per call, so the default
 * is correct everywhere and stays inert in Node tests — its skip branch never touches [Pathfinding]'s
 * coroutines). A harness installs a fake via [install]; [reset] restores the default (e.g. between headless
 * matches / tests). Mirrors [system.effect.Fx] / [system.audio.Snd].
 */
object Nav {
    var sink: FieldFlow = PathFieldFlow
        private set

    fun install(fieldFlow: FieldFlow) {
        sink = fieldFlow
    }

    fun reset() {
        sink = PathFieldFlow
    }
}
