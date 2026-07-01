package system.effect

/**
 * The live [Effects] sink — the install point for the imperative-shell boundary. Defaults to [NoOpEffects]
 * (headless: Node tests / the `SimRunner`); the JS shell [bind]s [BrowserEffects] once at boot (`Bootstrap.load`)
 * so the browser renders. A harness [install]s a transient fake override; [reset] drops it back to the bound
 * sink. This split (bound platform sink vs transient override) lets the accessor live in `commonMain` — it
 * never names the jsMain [BrowserEffects]. Mirrors [system.audio.Snd] / [system.grid.Nav] / [util.Names].
 */
object Fx {
    /**
     * Force the headless ([NoOpEffects]) sink even in the browser. An in-browser headless eval (the TRAIN tab
     * trainer / the leaderboard) runs many [ai.SimRunner] matches; setting this makes [sink] yield [NoOpEffects]
     * for the duration, so those matches fire no stray 3D FX. Clear it to restore the live renderer.
     */
    var headless: Boolean = false

    private var bound: Effects = NoOpEffects // the boot-bound platform sink (NoOp until the shell binds one)
    private var overrideSink: Effects? = null // a transient test/harness override

    val sink: Effects get() = if (headless) NoOpEffects else (overrideSink ?: bound)

    /** Wire the real platform sink once, at boot (the JS shell binds [BrowserEffects]). */
    fun bind(effects: Effects) {
        bound = effects
    }

    /** Install a transient override (a test fake / an AI driver); [reset] drops it. */
    fun install(effects: Effects) {
        overrideSink = effects
    }

    fun reset() {
        overrideSink = null
    }
}
