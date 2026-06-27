package system.audio

import org.khronos.webgl.set
import util.Util
import util.data.Pos
import kotlin.math.exp

/**
 * The steam-release hiss for a burned-out portal — a subtle airy "ffffff…": band-passed white noise with a
 * soft attack and a long trail, settling slightly in pitch (distinct from the fiery XMP blasts). Split out of
 * [SoundUtil] (a known oversized class) but reuses its audio graph internals (same module/package).
 */
object SteamSound {
    fun play(pos: Pos) {
        Mixer.current = Mixer.Group.PORTAL // burnout steam → the Portal mixer channel
        if (SoundUtil.isMuted()) return
        val ctx = SoundUtil.audioCtx
        val sr = ctx.sampleRate
        val dur = 0.9
        val len = (dur * sr).toInt().coerceAtLeast(1)
        val buffer = ctx.createBuffer(1, len, sr)
        val data = buffer.getChannelData(0)
        var i = 0
        while (i < len) {
            val t = i.toDouble() / sr
            val env = (1.0 - exp(-t / 0.08)) * exp(-t / 0.35) // ~80 ms swell, then a soft trail
            data[i] = ((Util.random() * 2.0 - 1.0) * env).toFloat()
            i++
        }
        val source = ctx.createBufferSource()
        source.buffer = buffer
        val bandpass = ctx.createBiquadFilter()
        bandpass.type = "bandpass"
        val n = SoundUtil.now()
        bandpass.frequency.setValueAtTime(2600.0, n)
        bandpass.frequency.exponentialRampToValueAtTime(1400.0, n + dur) // hiss settles a touch
        bandpass.asDynamic().Q.setValueAtTime(0.7, n) // wide → airy, not whistly
        val gainNode = SoundUtil.createStaticGain(0.3)
        val panNode = SoundUtil.createPanner(pos)
        source.connect(bandpass)
        bandpass.connect(gainNode)
        gainNode.connect(panNode)
        panNode.connect(Mixer.currentBus())
        source.start()
        source.stop(n + dur)
    }
}
