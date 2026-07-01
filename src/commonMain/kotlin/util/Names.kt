package util

/**
 * The live [PortalNamer] sink — the install point for map-derived portal naming. Defaults to
 * [NoOpPortalNamer] (headless → the [PortalNameGen] fallback); the JS shell [bind]s [MapPortalNamer] at boot
 * (`Bootstrap.load`) so the browser reads real map names. A harness [install]s a transient override; [reset]
 * drops it. The bound-vs-override split lets the accessor live in `commonMain` — it never names the jsMain
 * [MapPortalNamer]. Mirrors [system.effect.Fx] / [system.audio.Snd] / [system.grid.Nav].
 */
object Names {
    private var bound: PortalNamer = NoOpPortalNamer // the boot-bound platform sink (NoOp until the shell binds one)
    private var overrideSink: PortalNamer? = null // a transient test/harness override

    val sink: PortalNamer get() = overrideSink ?: bound

    /** Wire the real platform sink at boot (the JS shell binds [MapPortalNamer]). */
    fun bind(namer: PortalNamer) {
        bound = namer
    }

    /** Install a transient override (a test fake); [reset] drops it. */
    fun install(namer: PortalNamer) {
        overrideSink = namer
    }

    fun reset() {
        overrideSink = null
    }
}
