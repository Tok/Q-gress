package external.sound

/*
 * https://developer.mozilla.org/en-US/docs/Web/API/OscillatorNode
 */
external class OscillatorNode : AudioNode {
    val frequency: AudioParam
    var type: String
    fun start()
    fun stop(time: Double)
}
