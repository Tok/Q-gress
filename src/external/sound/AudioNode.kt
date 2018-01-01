package external.sound

/*
 * https://developer.mozilla.org/en-US/docs/Web/API/AudioNode
 */
external open class AudioNode {
    val destination: AudioNode
    fun connect(destination: AudioNode)
}
