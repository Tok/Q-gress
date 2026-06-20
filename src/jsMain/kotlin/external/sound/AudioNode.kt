package external.sound

/*
 * https://developer.mozilla.org/en-US/docs/Web/API/AudioNode
 */
open external class AudioNode {
    val destination: AudioNode
    fun connect(destination: AudioNode)
}
