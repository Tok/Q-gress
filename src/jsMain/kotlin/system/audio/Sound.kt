package system.audio

import World
import agent.Faction
import agent.NonFaction
import config.Config
import config.Dim
import config.OscillatorType
import config.Sim
import external.sound.AudioContext
import external.sound.AudioNode
import external.sound.GainNode
import external.sound.OscillatorNode
import external.sound.PannerNode
import external.sound.StereoPannerNode
import portal.Field
import portal.Link
import system.Checkpoint
import system.HeadlessRun
import system.display.Scene3D
import system.ui.Bootstrap
import util.Rng
import util.data.Pos
import kotlin.math.sqrt

// The audio hub: master bus + volume/listener control + the shared Web Audio node factory, plus the simpler
// per-event SFX. Heavier voice families live in sibling objects that call back into this factory: detonation
// /impact in [BlastSound], plus [KickDrum], [HackSound], [SteamSound], [AudioFx], [Mixer].
object Sound {
    const val DEFAULT_VOLUME = 0.3 // start quiet (30%) — less startling on first interaction
    internal const val EPS = 0.0001 // exponentialRamp can't target 0

    // The shared musical scale (level → note, major/minor on the lead) lives in [Scale].
    internal fun noteFor(level: Int, octaveUp: Int = 0) = Scale.noteFor(level, octaveUp)

    // 3D-audio tuning (sim-space is real metres). The play area is hundreds of metres across, so a
    // small reference distance + gentle rolloff makes near/far audibly differ at gameplay zoom while
    // distant events still carry. The listener (camera) sits well above, so Z mostly adds elevation.
    private const val SOUND_Z = 1.6 // head height in metres

    // The listener rides the camera, which sits hundreds–thousands of metres from the action at gameplay
    // zoom, so a small ref distance + steep rolloff crushed everything to near-silence. A large ref
    // distance + gentle rolloff keeps distant events audible while near/far still differ.
    private const val REF_DISTANCE = 300.0
    private const val MAX_DISTANCE = 12000.0
    private const val ROLLOFF = 0.5
    private const val PANNING_MODEL = "HRTF" // front/back + elevation cues (vs cheaper "equalpower")
    private const val MASTER_BOOST = 2.6 // lift the whole bus (3D attenuation made it quiet); limiter guards clipping
    private const val MUFFLE_CLOSED_HZ = 600.0 // muffled: distant/underwater (title behind onboarding)

    // The whole audio graph is LAZY so merely referencing Sound headless (Node tests / SimRunner)
    // doesn't construct an AudioContext (which doesn't exist outside a browser → would crash). Every play*
    // method gates on isMuted() (true headless), so these are only ever touched in a browser.
    internal val audioCtx: AudioContext by lazy { AudioContext() }
    private val listener by lazy { audioCtx.listener }

    // Master limiter on the way to the speakers: lets us boost the bus without harsh clipping when many
    // voices stack. (DynamicsCompressor configured as a brick-wall-ish limiter.)
    private val limiter: dynamic by lazy {
        val c = audioCtx.asDynamic().createDynamicsCompressor()
        c.threshold.value = -5.0
        c.knee.value = 6.0
        c.ratio.value = 10.0
        c.attack.value = 0.009 // slower than a brick-wall so kick/blast transients punch through before it clamps
        c.release.value = 0.12
        c.connect(audioCtx.destination)
        c
    }

    // Single master gain all sounds route through; controls overall volume (× MASTER_BOOST into the
    // limiter). The master FX bus (low/high-pass + reverb send — see AudioFx) sits between it + the limiter.
    internal val masterGain: GainNode by lazy {
        audioCtx.createGain().also {
            it.gain.value = 0.0
            AudioFx.build(audioCtx.asDynamic(), it.asDynamic(), limiter)
        }
    }

    /** Muffle (lowpass) the whole mix, or open it back up — used to push the title audio behind onboarding. */
    fun setMuffled(on: Boolean) {
        if (Bootstrap.isNotRunningInBrowser()) return
        // Opening back up restores the player's own AUDIO-tab cutoff, not a hardcoded "open" (else the muffle
        // would stomp a low-pass they'd dialled in).
        AudioFx.setLowpass(if (on) MUFFLE_CLOSED_HZ else AudioFx.lowpassHz)
    }

    // Master volume in 0..1. Starts silent; the first user gesture brings it to default (browser autoplay
    // policy needs one). The volume slider drives it too. [userMuted] is the EXPLICIT mute intent, tracked
    // separately from the level so "muted (0)" and "not enabled yet (0)" don't get conflated — without it, any
    // later gesture / world-gen would un-mute a user who muted before audio was first enabled.
    private var masterVolume = 0.0 // actual live level — 0 until the first gesture (autoplay), or when muted
    private var savedVolume = DEFAULT_VOLUME // the user's chosen non-mute level; applied on the first gesture
    private var audioEnabled = false // the first gesture has raised the volume once
    private var userMuted = false // the user explicitly chose mute (vs just not-enabled-yet)

    /** Restore the saved level + mute intent ([VolumePrefs]) — call once at startup, BEFORE the volume widget
     *  builds. Sets vars only (silent until the first gesture, per autoplay). This is what makes a mute survive
     *  the title→onboarding→world-gen reloads (each re-inits this object, losing in-memory state). */
    fun restoreVolume() {
        val (vol, muted) = VolumePrefs.load()
        savedVolume = vol ?: DEFAULT_VOLUME
        userMuted = muted
    }

    /** What the volume widget should show before the first gesture: 0 when muted, else the saved level. */
    fun displayVolume(): Double = if (userMuted) 0.0 else savedVolume

    /** Resume the audio context and turn sound on. Idempotent; call on a user gesture. */
    fun enableAudio() {
        if (Bootstrap.isNotRunningInBrowser()) return
        if (audioCtx.state != "running") audioCtx.resume()
        // The first gesture brings the volume up — to the SAVED level — UNLESS the user has muted. Afterwards the
        // level is left alone, so a gesture / world-gen never un-mutes them.
        if (!audioEnabled) {
            audioEnabled = true
            if (!userMuted) setMasterVolume(savedVolume)
        }
    }

    fun setMasterVolume(volume: Double) {
        if (Bootstrap.isNotRunningInBrowser()) return
        if (audioCtx.state != "running") audioCtx.resume()
        masterVolume = volume
        userMuted = volume <= 0.0 // dragging the slider to 0 IS a mute; any positive level un-mutes
        if (volume > 0.0) savedVolume = volume // remember the chosen non-mute level (for unmute / reload restore)
        masterGain.gain.setTargetAtTime(volume * MASTER_BOOST, now(), 0.01)
        VolumePrefs.save(savedVolume, userMuted) // survive the title→onboarding→game reloads
    }

    // True when muted OR there's simply no audio (headless: Node tests / SimRunner) OR a headless eval is in
    // flight (in-browser training/leaderboard — the parked matches must be silent). Every play* method checks
    // this first, so it's the single gate that keeps ALL sound — and the lazy audio graph — off; no per-call
    // guards needed.
    internal fun isMuted() = masterVolume <= 0.0 || Bootstrap.isNotRunningInBrowser() || HeadlessRun.active || pausedMute

    // Implicit mute while the sim is paused — silences sound without touching the user's volume/mute setting.
    private var pausedMute = false
    fun setPausedMute(on: Boolean) {
        pausedMute = on
    }

    /** Toggle mute. Returns the new volume (0 when muted). Keys off the explicit [userMuted] intent (NOT the
     *  live volume), so it works the same before and after audio is first enabled — clicking the speaker on the
     *  title/onboarding mutes for real and (via [persist]) stays muted across the reloads into the game. */
    fun toggleMute(): Double {
        setMasterVolume(if (userMuted) savedVolume else 0.0)
        return masterVolume
    }

    /**
     * Place the Web Audio listener at the camera (sim-space metres) — called every frame from
     * [Scene3D]. Forward/up arrive un-normalised and are normalised here; a degenerate/pre-first
     * frame keeps the last orientation.
     */
    fun updateListener(eye: DoubleArray, forward: DoubleArray, up: DoubleArray) {
        if (Bootstrap.isNotRunningInBrowser()) return
        val fl = sqrt(forward[0] * forward[0] + forward[1] * forward[1] + forward[2] * forward[2])
        val ul = sqrt(up[0] * up[0] + up[1] * up[1] + up[2] * up[2])
        if (fl < EPS || ul < EPS) return
        listener.positionX.value = eye[0]
        listener.positionY.value = eye[1]
        listener.positionZ.value = eye[2]
        listener.forwardX.value = forward[0] / fl
        listener.forwardY.value = forward[1] / fl
        listener.forwardZ.value = forward[2] / fl
        listener.upX.value = up[0] / ul
        listener.upY.value = up[1] / ul
        listener.upZ.value = up[2] / ul
    }

    fun playNoiseGenSound() {
        if (!Config.isPlayInitialSound || isMuted()) return
        val freq = 330
        val osc = createNoiseOscillator(freq)
        playSound(osc, createNoisePan(), 0.15, 13.0)
    }

    fun playOffScreenLocationCreationSound() {
        val center = Pos(Sim.width / 2, Sim.height / 2)
        return playPortalCreationSound(center, 0.5)
    }

    fun playPortalCreationSound(pos: Pos, gain: Double = 1.0) {
        Mixer.current = Mixer.Group.PORTAL
        if (isMuted()) return
        val duration = 0.5
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, 120.0, 0.0, duration)
        playSound(oscNode, createPanner(pos), gain, duration)
    }

    fun playPortalRemovalSound(pos: Pos) {
        Mixer.current = Mixer.Group.PORTAL
        if (isMuted()) return
        val duration = 0.5
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, 60.0, 120.0, duration)
        playSound(oscNode, createPanner(pos), 1.0, duration)
    }

    /** A short rising 3-note jingle when one of the player's agents levels up (level 5→3→1 on the scale). */
    fun playLevelUp(pos: Pos) {
        if (isMuted()) return
        val n = now()
        val osc = createStaticOscillator(OscillatorType.TRIANGLE, noteFor(5, octaveUp = 2))
        osc.frequency.setValueAtTime(noteFor(5, octaveUp = 2), n)
        osc.frequency.setValueAtTime(noteFor(3, octaveUp = 2), n + 0.09)
        osc.frequency.setValueAtTime(noteFor(1, octaveUp = 2), n + 0.18)
        val g = audioCtx.createGain()
        g.gain.setValueAtTime(0.32, n)
        g.gain.setValueAtTime(0.32, n + 0.26)
        g.gain.exponentialRampToValueAtTime(EPS, n + 0.4)
        connectVoice(osc, createPanner(pos), g, n + 0.4)
    }

    internal fun connectVoice(osc: OscillatorNode, panNode: AudioNode, gainNode: GainNode, stopTime: Double) {
        osc.connect(panNode)
        panNode.connect(gainNode)
        gainNode.connect(Mixer.currentBus())
        osc.start()
        osc.stop(stopTime)
    }

    fun playCheckpointSound(@Suppress("UNUSED_PARAMETER") checkpoint: Checkpoint) {
        Mixer.current = Mixer.Group.FIELD
        if (isMuted()) return
        val duration = 0.05
        val pan = 0.0 // centre
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, 440.0, 440.0, duration)
        playSound(oscNode, createStaticPan(pan), 0.5, duration)
    }

    fun playFailSound() {
        if (isMuted()) return
        val freq = 220.0
        val osc = createStaticOscillator(OscillatorType.SINE, freq)
        playSound(osc, createNoisePan(), 0.1, 0.5)
    }

    fun playCycleSound() {
        if (isMuted()) return
        val duration = 0.01
        val pan = 0.0 // centre
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, 220.0, 220.0, duration)
        playSound(oscNode, createStaticPan(pan), 0.5, duration)
    }

    /** Recruiting succeeded: a cheerful rising two-step chirp at the agent — a new teammate signs on. */
    fun playRecruitSuccess(pos: Pos) {
        if (isMuted()) return
        val dur = 0.26
        val osc = createExponentialRampOscillator(OscillatorType.TRIANGLE, 520.0, 880.0, dur) // upbeat rise
        playSound(osc, createPanner(pos), 0.12, dur)
    }

    /** A soft marble "tup": a short sine with a quick downward pitch drop + a gentle attack/decay envelope
     *  (no hard click) — the NPC dropping in. Quiet + rounded so a fast world-gen crowd of these doesn't
     *  machine-gun sharp pings. */
    fun playNpcCreationSound(npc: NonFaction) {
        if (isMuted()) return
        val dur = 0.07
        val sizePitch = 1.0 - npc.size.offset * 0.18 // smaller NPC → higher-pitched marble
        val start = 860.0 * sizePitch // softer/rounder than the old bright 1150 ping
        val end = 360.0 * sizePitch // fast downward chirp = a marble tap
        val osc = createExponentialRampOscillator(OscillatorType.SINE, start, end, dur) // sine = no buzzy harmonics
        val gainNode = audioCtx.createGain()
        val n = now()
        gainNode.gain.setValueAtTime(EPS, n)
        gainNode.gain.exponentialRampToValueAtTime(0.09, n + 0.008) // soft attack (vs the old instant-on click)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + dur) // gentle tail
        connectVoice(osc, createPanner(npc.pos), gainNode, n + dur)
    }

    /** Portal gained a level: a quick note rising up to the NEW [level]'s note on the shared scale. */
    fun playUpgradeSound(pos: Pos, level: Int) {
        Mixer.current = Mixer.Group.PORTAL
        if (isMuted()) return
        val dur = 0.18
        val target = noteFor(level, octaveUp = 3)
        val osc = createExponentialRampOscillator(OscillatorType.SINE, target * 0.67, target, dur)
        playSound(osc, createPanner(pos), 0.08, dur)
    }

    /** Portal lost a level: a quick note falling down to the NEW [level]'s note on the shared scale. */
    fun playDowngradeSound(pos: Pos, level: Int) {
        Mixer.current = Mixer.Group.PORTAL
        if (isMuted()) return
        val dur = 0.2
        val target = noteFor(level, octaveUp = 3)
        val osc = createExponentialRampOscillator(OscillatorType.SINE, target * 1.5, target, dur)
        playSound(osc, createPanner(pos), 0.08, dur)
    }

    /** Portal neutralized (lost its owner): a short descending "power-down" sweep. */
    fun playNeutralizeSound(pos: Pos) {
        Mixer.current = Mixer.Group.PORTAL
        if (isMuted()) return
        val dur = 0.5
        val osc = createExponentialRampOscillator(OscillatorType.SAW, 440.0, 90.0, dur)
        val n = now()
        val gainNode = audioCtx.createGain()
        gainNode.gain.setValueAtTime(0.12, n)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + dur)
        connectVoice(osc, createPanner(pos), gainNode, n + dur)
    }

    /** A short bell-like "ding" as a single resonator drops into its slot (on the shared scale). */
    fun playResoDeploySound(pos: Pos, level: Int) {
        Mixer.current = Mixer.Group.PORTAL
        if (isMuted()) return
        val freq = noteFor(level, octaveUp = 3) // bright + on-key (level 8 = lowest)
        val osc = createStaticOscillator(OscillatorType.SINE, freq)
        val gainNode = audioCtx.createGain()
        val n = now()
        val dur = 0.22
        gainNode.gain.setValueAtTime(EPS, n)
        gainNode.gain.exponentialRampToValueAtTime(0.12, n + 0.005) // fast attack → the "ding"
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + dur) // quick bell decay
        connectVoice(osc, createPanner(pos), gainNode, n + dur)
    }

    /** A metallic "clunk" when a mod (shield / heat sink) is slotted into a portal. */
    fun playModDeploySound(pos: Pos, level: Int) {
        Mixer.current = Mixer.Group.PORTAL
        if (isMuted()) return
        val osc = createStaticOscillator(OscillatorType.SQUARE, noteFor(level, octaveUp = 2))
        val gainNode = audioCtx.createGain()
        val n = now()
        val dur = 0.16
        gainNode.gain.setValueAtTime(0.08, n)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + dur)
        connectVoice(osc, createPanner(pos), gainNode, n + dur)
    }

    /** A bright shimmering "shing" as a shield powers up on the portal. */
    fun playShieldDeploySound(pos: Pos, level: Int) {
        Mixer.current = Mixer.Group.PORTAL
        if (isMuted()) return
        val base = noteFor(level, octaveUp = 3)
        val osc = createLinearRampOscillator(OscillatorType.TRIANGLE, base, base * 1.5, 0.35) // rises into place
        val gainNode = audioCtx.createGain()
        val n = now()
        gainNode.gain.setValueAtTime(EPS, n)
        gainNode.gain.exponentialRampToValueAtTime(0.10, n + 0.02)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + 0.4) // gentle ring-out
        connectVoice(osc, createPanner(pos), gainNode, n + 0.4)
    }

    /** A descending "power-down" as a shield collapses / is stripped off. */
    fun playShieldRemoveSound(pos: Pos, level: Int) {
        Mixer.current = Mixer.Group.PORTAL
        if (isMuted()) return
        val base = noteFor(level, octaveUp = 3)
        val osc = createLinearRampOscillator(OscillatorType.TRIANGLE, base * 1.5, base * 0.5, 0.35) // falls away
        val gainNode = audioCtx.createGain()
        val n = now()
        gainNode.gain.setValueAtTime(0.12, n)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + 0.35)
        connectVoice(osc, createPanner(pos), gainNode, n + 0.35)
    }

    /** A glitchy faction-pitched sweep when a virus (ADA / JARVIS) flips a portal. */
    fun playVirusSound(pos: Pos, faction: Faction) {
        Mixer.current = Mixer.Group.PORTAL
        if (isMuted()) return
        val base = if (faction == Faction.ENL) 180.0 else 140.0
        val osc = createLinearRampOscillator(OscillatorType.SQUARE, base, base * 4.0, VIRUS_DUR)
        val gainNode = audioCtx.createGain()
        val n = now()
        gainNode.gain.setValueAtTime(0.14, n)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + VIRUS_DUR)
        connectVoice(osc, createPanner(pos), gainNode, n + VIRUS_DUR)
    }

    private const val VIRUS_DUR = 0.5

    fun playDeploySound(pos: Pos, distanceToPortal: Int) {
        Mixer.current = Mixer.Group.PORTAL
        if (isMuted()) return
        val ratio = distanceToPortal / Dim.maxDeploymentRange
        val gain = 0.10
        val duration = 0.2
        val minFreq = 250.0
        val baseFreq = -250.0
        val startFreq = minFreq + (baseFreq * ratio)
        val endFreq = minFreq + (baseFreq * ratio * 2)
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, startFreq, endFreq, duration)
        playSound(oscNode, createPanner(pos), gain, duration)
    }

    /** Linking: a quick glissando between the two portals' notes (their levels) travelling along the
     *  link — so the sound expresses *what* is being joined. Longer links sweep longer + a register
     *  deeper (a long span reads heavier). */
    fun playLinkingSound(link: Link) {
        Mixer.current = Mixer.Group.FIELD
        if (isMuted()) return
        val ratio = (link.getLine().length() / World.diagonalLength()).coerceIn(0.0, 1.0)
        val dur = 0.1 + 0.3 * ratio
        val oct = if (ratio > 0.5) 1 else 2 // longer link → deeper octave
        val from = noteFor(link.origin.getLevel().toInt(), octaveUp = oct)
        val to = noteFor(link.destination.getLevel().toInt(), octaveUp = oct)
        val oscNode = createLinearRampOscillator(OscillatorType.SINE, from, to, dur)
        val panNode = createPannerRamp(link.getLine().from, link.getLine().to, dur)
        playSound(oscNode, panNode, 0.22, dur)
    }

    /** Field collapse (teardown): a short downward sweep that decays away. */
    fun playFieldDownSound() {
        Mixer.current = Mixer.Group.FIELD
        if (isMuted()) return
        val dur = 0.5
        val osc = createExponentialRampOscillator(OscillatorType.TRIANGLE, 110.0, 28.0, dur)
        val gainNode = audioCtx.createGain()
        val n = now()
        gainNode.gain.setValueAtTime(0.35, n)
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + dur)
        connectVoice(osc, createStaticPan(0.0), gainNode, n + dur)
    }

    /** Fielding: a swelling triad whose three notes come from the field's three side lengths (longer side
     *  → lower note → the triangle's SHAPE as a chord), in a register set by the field's AREA (bigger =
     *  deeper, longer, fuller) — a control field powering up. */
    fun playFieldingSound(field: Field) {
        Mixer.current = Mixer.Group.FIELD
        if (isMuted()) return
        val areaRatio = (field.calculateArea().toDouble() / World.totalArea()).coerceIn(0.0, 1.0)
        val dur = 0.7 + 1.0 * areaRatio // bigger field rings longer
        val oct = if (areaRatio > 0.4) 1 else 2 // bigger field → deeper register
        val diag = World.diagonalLength().toDouble()
        val o = field.origin
        val p = field.primaryAnchor
        val s = field.secondaryAnchor
        val center = Pos((o.x() + p.x() + s.x()) / 3, (o.y() + p.y() + s.y()) / 3)
        listOf(
            o.location.distanceTo(p.location),
            o.location.distanceTo(s.location),
            p.location.distanceTo(s.location),
        ).forEach { len ->
            val lvl = (1 + (len / diag) * 7.0).toInt().coerceIn(1, 8) // longer side → higher level → lower note
            fieldVoice(center, noteFor(lvl, octaveUp = oct), dur)
        }
    }

    /** One swelling voice of the fielding triad: a slight upward bend (power-up whoosh) that swells then rings out. */
    private fun fieldVoice(pos: Pos, base: Double, dur: Double) {
        val n = now()
        val osc = createLinearRampOscillator(OscillatorType.TRIANGLE, base, base * 1.04, dur)
        val gainNode = audioCtx.createGain()
        gainNode.gain.setValueAtTime(EPS, n)
        gainNode.gain.linearRampToValueAtTime(0.11, n + dur * 0.45) // swell in
        gainNode.gain.exponentialRampToValueAtTime(EPS, n + dur) // ring out
        connectVoice(osc, createPanner(pos), gainNode, n + dur)
    }

    internal fun playSound(oscNode: OscillatorNode, panNode: AudioNode, gain: Double, duration: Double) {
        val gainNode = createStaticGain(gain)
        oscNode.connect(panNode)
        panNode.connect(gainNode)
        gainNode.connect(Mixer.currentBus())
        oscNode.start()
        oscNode.stop(now() + duration)
    }

    internal fun now() = audioCtx.currentTime.toDouble()

    internal fun createStaticOscillator(type: String, freq: Double): OscillatorNode {
        val node = audioCtx.createOscillator()
        node.type = type
        node.frequency.setTargetAtTime(freq, now(), 0.0)
        return node
    }

    private fun createNoiseOscillator(maxFreq: Int): OscillatorNode {
        val node = audioCtx.createOscillator()
        node.type = OscillatorType.SQUARE
        val n = now()
        val timeConstant = 0.01
        val max = 1000
        for (i in 0..max) {
            val freq = Rng.random() * (maxFreq - (maxFreq * i / max))
            val tc = timeConstant * i
            node.frequency.setTargetAtTime(freq, n + tc, timeConstant)
        }
        return node
    }

    private fun createLinearRampOscillator(type: String, startFreq: Double, endFreq: Double, duration: Double): OscillatorNode {
        val node = createStaticOscillator(type, startFreq)
        node.frequency.linearRampToValueAtTime(endFreq, now() + duration)
        return node
    }

    internal fun createExponentialRampOscillator(type: String, startFreq: Double, endFreq: Double, duration: Double): OscillatorNode {
        val node = createStaticOscillator(type, startFreq)
        node.frequency.exponentialRampToValueAtTime(endFreq, now() + duration)
        return node
    }

    internal fun createStaticPan(pan: Double): StereoPannerNode {
        val node = audioCtx.createStereoPanner()
        node.pan.setTargetAtTime(pan, now(), 0.0)
        return node
    }

    private fun createNoisePan(): StereoPannerNode {
        val node = audioCtx.createStereoPanner()
        val timeConstant = 0.01
        val max = 1000
        val n = now()
        for (i in 0..max) {
            val pan = Rng.random() * 2.0 - 1.0 // full −1…+1 stereo field
            val tc = timeConstant * i
            node.pan.setTargetAtTime(pan, n + tc, timeConstant)
        }

        return node
    }

    /** A positional source at sim [pos] (metres), spatialized relative to the camera listener. */
    internal fun createPanner(pos: Pos): PannerNode {
        val node = audioCtx.createPanner()
        node.panningModel = PANNING_MODEL
        node.distanceModel = "inverse"
        node.refDistance = REF_DISTANCE
        node.maxDistance = MAX_DISTANCE
        node.rolloffFactor = ROLLOFF
        node.positionX.value = Scene3D.sceneX(pos)
        node.positionY.value = Scene3D.sceneY(pos)
        node.positionZ.value = SOUND_Z
        return node
    }

    /** A panner that travels from [from] to [to] over [duration] — e.g. a link / field sweep. */
    private fun createPannerRamp(from: Pos, to: Pos, duration: Double): PannerNode {
        val node = createPanner(from)
        val end = now() + duration
        node.positionX.setValueAtTime(Scene3D.sceneX(from), now())
        node.positionY.setValueAtTime(Scene3D.sceneY(from), now())
        node.positionX.linearRampToValueAtTime(Scene3D.sceneX(to), end)
        node.positionY.linearRampToValueAtTime(Scene3D.sceneY(to), end)
        return node
    }

    internal fun createStaticGain(gain: Double): GainNode {
        val node = audioCtx.createGain()
        node.gain.setTargetAtTime(gain, now(), 0.0)
        return node
    }
}
