package system

import ai.SimRunner
import system.audio.Snd
import system.effect.Fx

/**
 * Brackets an in-browser **headless evaluation** (the TRAIN-tab trainer, the leaderboard) so it never disturbs
 * the live game: [begin] parks the live world via [WorldSnapshot] and forces the no-op effect sink ([Fx.headless]);
 * [end] clears the throwaway match state and restores the world + renderer exactly as they were. The tick loop
 * pauses while [active] (see `Bootstrap.tick`), and the eval drives its own matches chunked over `setTimeout`.
 * Re-entrant-safe via a simple depth count, but evals are expected to be one-at-a-time.
 */
object HeadlessRun {
    private var snapshot: WorldSnapshot.Snapshot? = null

    /** True while a headless eval is in flight — the live tick loop pauses on this. */
    var active = false
        private set

    /** Park the live game + silence FX. Idempotent: a no-op if already active. */
    fun begin() {
        if (active) return
        snapshot = WorldSnapshot.capture()
        Fx.headless = true // survives SimRunner's per-match Fx.reset(), so eval matches fire no 3D FX
        Snd.headless = true // ditto for audio: eval matches stay silent through their per-match Snd.reset()
        active = true
    }

    /** Restore the live game exactly as it was (clears throwaway match state, re-enables the renderer). */
    fun end() {
        if (!active) return
        SimRunner.reset()
        snapshot?.let { WorldSnapshot.restore(it) }
        snapshot = null
        Fx.headless = false
        Fx.reset()
        Snd.headless = false
        Snd.reset()
        active = false
    }
}
