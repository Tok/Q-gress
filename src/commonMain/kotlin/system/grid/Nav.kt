package system.grid

/**
 * The live [FieldFlow] sink — the install point for the flow-field-compute boundary. Defaults to
 * [NoOpFieldFlow] (no fields → callers keep their empty field + straight-line fallback); the JS shell
 * [bind]s the real [PathFieldFlow] at boot (`Bootstrap.load`) and per headless match (`ai.SimRunner`, so
 * `Config.headlessFieldCompute` matches still compute inline). A harness [install]s a transient override;
 * [reset] drops it. The bound-vs-override split lets the accessor live in `commonMain` — it never names the
 * jsMain [PathFieldFlow]. Mirrors [system.effect.Fx] / [system.audio.Snd] / [util.Names].
 */
object Nav {
    private var bound: FieldFlow = NoOpFieldFlow // the boot-bound platform sink (NoOp until the shell binds one)
    private var overrideSink: FieldFlow? = null // a transient test/harness override

    val sink: FieldFlow get() = overrideSink ?: bound

    /** Wire the real platform sink (the JS shell binds [PathFieldFlow]). */
    fun bind(fieldFlow: FieldFlow) {
        bound = fieldFlow
    }

    /** Install a transient override (a test fake); [reset] drops it. */
    fun install(fieldFlow: FieldFlow) {
        overrideSink = fieldFlow
    }

    fun reset() {
        overrideSink = null
    }
}
