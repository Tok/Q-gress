package system.audio

import external.sound.GainNode

/**
 * The per-role audio mixer. Every voice routes through its role's gain bus (`bus → Sound.masterGain`), so
 * the AUDIO-tab Mixer can set a per-role level + mute. The role is picked by [current], set at the top of each
 * public play* function and read when the voice connects (connects are synchronous, so the value is current).
 * Sounds left un-annotated fall to [Group.WORLD]. Levels/mutes persist via [MixerPrefs]. Split out of
 * [Sound] (size); the gain buses are lazy so headless code never builds an AudioContext.
 */
object Mixer {
    enum class Group(val label: String) { WEAPONS("Weapons"), PORTAL("Portal"), FIELD("Field"), WORLD("World"), AMBIENT("Ambient") }

    var current: Group = Group.WORLD
    private val gains = mutableMapOf<Group, GainNode>()
    private val vol = Group.values().associateWith { 1.0 }.toMutableMap()
    private val mute = Group.values().associateWith { false }.toMutableMap()

    private fun busFor(g: Group): GainNode = gains.getOrPut(g) {
        Sound.audioCtx.createGain().also {
            it.gain.value = if (mute.getValue(g)) 0.0 else vol.getValue(g) // honour a pre-build (persisted) mute/level
            it.connect(Sound.masterGain)
        }
    }

    /** The gain bus for the [current] role — voices connect here instead of straight to `masterGain`. */
    fun currentBus(): GainNode = busFor(current)

    /** The gain bus for a specific role — for continuous sources (the ambient bed) that don't use [current]. */
    fun bus(g: Group): GainNode = busFor(g)

    fun setVolume(g: Group, v: Double) {
        vol[g] = v.coerceIn(0.0, 1.0)
        apply(g)
    }

    fun setMuted(g: Group, m: Boolean) {
        mute[g] = m
        apply(g)
    }

    fun volume(g: Group): Double = vol.getValue(g)
    fun isMuted(g: Group): Boolean = mute.getValue(g)

    private fun apply(g: Group) {
        val target = if (mute.getValue(g)) 0.0 else vol.getValue(g)
        gains[g]?.gain?.asDynamic()?.setTargetAtTime(target, Sound.audioCtx.asDynamic().currentTime, 0.02)
    }
}
