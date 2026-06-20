package external.sound

/*
 * https://developer.mozilla.org/en-US/docs/Web/API/AudioContext
 */
external class AudioContext {
    val destination: AudioNode
    val currentTime: Int
    val sampleRate: Int

    // "suspended" until a user gesture resumes it (browser autoplay policy).
    val state: String
    fun resume()
    fun suspend()

    fun createOscillator(): OscillatorNode
    fun createStereoPanner(): StereoPannerNode
    fun createGain(): GainNode
    fun createBiquadFilter(): BiquadFilterNode
    fun createBufferSource(): AudioBufferSourceNode
    fun createBuffer(numChannels: Int, length: Int, sampleRate: Int): AudioBuffer
}
