package system.ui

import config.Time
import kotlinx.browser.document
import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import system.audio.Sound
import system.display.Scene3D
import system.map.MapCamera
import kotlin.math.roundToInt

/**
 * The sim playback controller — the game clock split out of [Bootstrap] (the SoC / god-object split, PLAN
 * phase B). Owns the `setInterval` tick loop, the speed multiplier, and pause/resume (including the pause
 * side-effects: muting, parking the auto-cam, and freezing the 3D animation clock). [Bootstrap] builds the
 * toolbar buttons and delegates their clicks here; the per-tick work itself stays in Bootstrap and is handed
 * in via [start] (the live game's `tick`, or the demo's). Paused state is `intervalID == -1`.
 */
object GameLoop {
    const val PAUSE_BUTTON_ID = "pauseButton"
    private const val MIN_SPEED = 0.25
    private const val MAX_SPEED = 4.0

    // Sim-speed presets behind the toolbar buttons (mult, label, button id). "Max" = MAX_SPEED.
    val SPEED_PRESETS = listOf(
        Triple(1.0, "×1", "speedBtnX1"),
        Triple(3.0, "×3", "speedBtnX3"),
        Triple(MAX_SPEED, "Max", "speedBtnMax"),
    )

    private var intervalID = 0
    private var speedMult = 1.0
    private var autoCamBeforePause = false
    private var tickFn: () -> Unit = {}
    private var evalPauseDepth = 0 // nested eval-pause holders (trainer screen open + a training run inside it)
    private var playingBeforeEval = false

    /** (Re)start the loop, running [tick] each speed-scaled interval — the live game's `tick` or the demo's. */
    fun start(tick: () -> Unit) {
        tickFn = tick
        intervalID = schedule()
    }

    fun isPaused() = intervalID == -1

    /** Pick a sim-speed preset; resumes first if currently paused (so a speed button always plays). */
    fun selectSpeed(mult: Double) {
        if (isPaused()) togglePause()
        setSpeed(mult)
    }

    /** Nudge the speed by [delta] (the -/+ keys). */
    fun nudgeSpeed(delta: Double) = setSpeed(speedMult + delta)

    fun togglePause() {
        intervalID = pauseHandler(intervalID)
        applyPauseSideEffects(paused = isPaused())
        refreshSpeedButtons()
    }

    /**
     * Force a full pause for the duration of an in-browser eval (the TRAIN screen being open / a training run /
     * the leaderboard), so the live game is truly still behind it — not just sim-frozen but with the 3D
     * animation clock, auto-cam and audio parked too. **Reference-counted**, so the trainer screen and a
     * training run inside it nest without fighting; remembers whether the game was playing so the last
     * [resumeAfterEval] restores the prior state. A no-op before the live game loop exists (onboarding / title).
     */
    fun pauseForEval() {
        if (evalPauseDepth == 0) {
            playingBeforeEval = !isPaused()
            if (playingBeforeEval && document.getElementById(PAUSE_BUTTON_ID) != null) togglePause()
        }
        evalPauseDepth++
    }

    /** Release one eval pause; the LAST release resumes — only if an eval was what paused it (respects a manual pause). */
    fun resumeAfterEval() {
        if (evalPauseDepth == 0) return
        evalPauseDepth--
        if (evalPauseDepth == 0 && playingBeforeEval && isPaused()) togglePause()
    }

    // Highlight the preset matching the live speed (none while paused, i.e. intervalID == -1).
    fun refreshSpeedButtons() {
        SPEED_PRESETS.forEach { (mult, _, id) ->
            val active = !isPaused() && mult == speedMult
            (document.getElementById(id) as? HTMLElement)?.let { if (active) it.addClass("active") else it.removeClass("active") }
        }
    }

    // Schedule the loop. Above 1× the timer stays at minTickInterval and we run [stepsPerFire] sim steps per
    // fire; below 1× we slow the timer instead (one step per fire). Dividing the period for fast speeds didn't
    // work: the browser/CPU floors setInterval at ~5 ms, so ×3 (20/3≈6 ms) and Max (20/4=5 ms) collapsed to the
    // same real rate. Stepping N times decouples sim speed from that floor, so ×3 and Max are genuinely 3× / 4×.
    private fun schedule(): Int = document.defaultView?.setInterval({ repeat(stepsPerFire()) { tickFn() } }, currentTickMs()) ?: 0

    private fun currentTickMs() = (Time.minTickInterval / minOf(speedMult, 1.0)).toInt().coerceAtLeast(1)
    private fun stepsPerFire() = maxOf(1, speedMult.roundToInt())

    /** Set the sim speed multiplier; restarts the tick interval (paused stays paused) and scales animations.
     *  Walking/actions follow automatically — they run more sim steps per fire (or a slower timer below 1×). */
    private fun setSpeed(mult: Double) {
        speedMult = mult.coerceIn(MIN_SPEED, MAX_SPEED)
        applyAnimationSpeed() // visual FX (hack spin, deploy, shatter, build-in, sun) track the speed
        if (!isPaused()) {
            document.defaultView?.clearInterval(intervalID)
            intervalID = schedule()
        }
        refreshSpeedButtons()
    }

    // The 3D render loop runs independently of the sim tick, so drive its animation clock from here: the live
    // speed multiplier normally, 0 while paused (freezes hack spins, the sun's arc, etc. — a true pause).
    private fun applyAnimationSpeed() {
        Scene3D.animationSpeed = if (isPaused()) 0.0 else speedMult
    }

    // Pausing the tick interval freezes the sim logic, but the 3D render + auto-cam run on their own loop —
    // so also park the auto-cam (restoring it on resume) and implicitly mute, so a paused game is truly still.
    private fun applyPauseSideEffects(paused: Boolean) {
        Sound.setPausedMute(paused)
        applyAnimationSpeed() // freeze (or restore) the 3D animation clock — sun arc, hack spins, etc.
        if (paused) {
            autoCamBeforePause = MapCamera.isAutoCamOn()
            if (autoCamBeforePause) MapCamera.setAutoCam(false)
            MapCamera.stopCamera() // halt the in-flight camera ease at once (else a 27 s auto-cam leg glides on)
        } else if (autoCamBeforePause) {
            MapCamera.setAutoCam(true)
        }
    }

    private fun pauseHandler(intervalID: Int): Int {
        val pauseButton = document.getElementById(PAUSE_BUTTON_ID) as HTMLButtonElement
        return if (intervalID != -1) {
            pauseButton.innerText = "Resume"
            document.defaultView?.clearInterval(intervalID)
            -1
        } else {
            pauseButton.innerText = "Pause"
            schedule()
        }
    }
}
