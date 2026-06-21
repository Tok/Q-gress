package external.sound

/*
 * https://developer.mozilla.org/en-US/docs/Web/API/PannerNode
 * A spatialized source: its position is in the same coordinate space as the AudioListener.
 */
external class PannerNode : AudioNode {
    var panningModel: String
    var distanceModel: String
    var refDistance: Double
    var maxDistance: Double
    var rolloffFactor: Double
    val positionX: AudioParam
    val positionY: AudioParam
    val positionZ: AudioParam
    val orientationX: AudioParam
    val orientationY: AudioParam
    val orientationZ: AudioParam
}
