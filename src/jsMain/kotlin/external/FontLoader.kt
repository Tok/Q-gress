// three.js FontLoader (examples/jsm) — loads a typeface.json into a Font usable by TextGeometry.
// Bundled via webpack from the npm "three" package; @JsNonModule because the browser target emits UMD.
@file:JsModule("three/examples/jsm/loaders/FontLoader.js")
@file:JsNonModule
@file:Suppress("UnusedParameter")

package external

external class FontLoader {
    /** onLoad receives a `Font` (pass it as TextGeometry's `font` parameter). */
    fun load(
        url: String,
        onLoad: (dynamic) -> Unit,
        onProgress: (dynamic) -> Unit = definedExternally,
        onError: (dynamic) -> Unit = definedExternally,
    )
}
