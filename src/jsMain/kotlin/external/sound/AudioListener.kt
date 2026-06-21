package external.sound

/*
 * https://developer.mozilla.org/en-US/docs/Web/API/AudioListener
 * The single listener; its position + orientation place the ears in the scene (we drive it from
 * the camera each frame). Uses the modern AudioParam interface (positionX/forwardX/upX, …).
 */
external class AudioListener {
    val positionX: AudioParam
    val positionY: AudioParam
    val positionZ: AudioParam
    val forwardX: AudioParam
    val forwardY: AudioParam
    val forwardZ: AudioParam
    val upX: AudioParam
    val upY: AudioParam
    val upZ: AudioParam
}
