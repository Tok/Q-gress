package external.sound

/*
 * https://developer.mozilla.org/en-US/docs/Web/API/AudioBufferSourceNode
 */
external class AudioBufferSourceNode : AudioNode {
    var buffer: AudioBuffer
    fun start()
    fun stop(time: Double)
}
