package system.audio

import system.ui.Bootstrap

/**
 * The live [Audio] sink — the install point for the audio imperative-shell boundary. Self-selects
 * [BrowserAudio] in the browser / [NoOpAudio] headless at first reference (assigning the sink *object*
 * touches no Web-Audio node, so no `AudioContext` is forced). A harness installs a fake via [install];
 * [reset] restores the default (e.g. between headless matches / tests). Mirrors [system.effect.Fx].
 */
object Snd {
    /**
     * Force the headless ([NoOpAudio]) sink even in the browser. An in-browser headless eval (the TRAIN tab
     * trainer / the leaderboard) runs many [ai.SimRunner] matches, each of which calls [reset] — so a one-off
     * [install] wouldn't stick. Setting this makes [default]/[reset] yield [NoOpAudio] for the duration, so
     * those matches fire no stray SFX/TTS; clear it (then [reset]) to restore the live engine.
     */
    var headless: Boolean = false

    private fun default(): Audio = if (headless || Bootstrap.isNotRunningInBrowser()) NoOpAudio else BrowserAudio

    var sink: Audio = default()
        private set

    fun install(audio: Audio) {
        sink = audio
    }

    fun reset() {
        sink = default()
    }
}
