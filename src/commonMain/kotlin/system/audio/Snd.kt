package system.audio

/**
 * The live [Audio] sink — the install point for the audio imperative-shell boundary. Defaults to [NoOpAudio]
 * (headless: Node tests / the `SimRunner`); the JS shell [bind]s [BrowserAudio] once at boot (`Bootstrap.load`)
 * so the browser plays. A harness [install]s a transient fake override; [reset] drops it back to the bound
 * sink. The bound-vs-override split lets the accessor live in `commonMain` — it never names the jsMain
 * [BrowserAudio]. Mirrors [system.effect.Fx] / [system.grid.Nav] / [util.Names].
 */
object Snd {
    /**
     * Force the headless ([NoOpAudio]) sink even in the browser. An in-browser headless eval (the TRAIN tab
     * trainer / the leaderboard) runs many [ai.SimRunner] matches; setting this makes [sink] yield [NoOpAudio]
     * for the duration, so those matches fire no stray SFX/TTS. Clear it to restore the live engine.
     */
    var headless: Boolean = false

    private var bound: Audio = NoOpAudio // the boot-bound platform sink (NoOp until the shell binds one)
    private var overrideSink: Audio? = null // a transient test/harness override

    val sink: Audio get() = if (headless) NoOpAudio else (overrideSink ?: bound)

    /** Wire the real platform sink once, at boot (the JS shell binds [BrowserAudio]). */
    fun bind(audio: Audio) {
        bound = audio
    }

    /** Install a transient override (a test fake); [reset] drops it. */
    fun install(audio: Audio) {
        overrideSink = audio
    }

    fun reset() {
        overrideSink = null
    }
}
