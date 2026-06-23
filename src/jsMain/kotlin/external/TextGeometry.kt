// three.js TextGeometry (examples/jsm) — extrudes a string into 3D geometry using a loaded Font.
// Bundled via webpack from the npm "three" package; @JsNonModule because the browser target emits UMD.
@file:JsModule("three/examples/jsm/geometries/TextGeometry.js")
@file:JsNonModule
@file:Suppress("UnusedParameter", "UnusedPrivateProperty")

package external

/** parameters: { font, size, depth/height, curveSegments, bevelEnabled, bevelThickness, bevelSize, bevelSegments }. */
external class TextGeometry(text: String, parameters: dynamic) {
    fun computeBoundingBox()
    val boundingBox: dynamic
}
