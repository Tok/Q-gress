package external.sound

/*
 * https://developer.mozilla.org/en-US/docs/Web/API/AudioContext
 */
external class AudioContext {
    val destination: AudioNode
    val currentTime: Int
    fun createOscillator(): OscillatorNode
    fun createStereoPanner(): StereoPannerNode
    fun createGain(): GainNode
}
