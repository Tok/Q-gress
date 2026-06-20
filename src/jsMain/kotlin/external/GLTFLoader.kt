// three.js GLTFLoader (examples/jsm), bundled via webpack from the npm "three"
// package. Used to load the glass-shard fracture models. @JsNonModule is required
// because the browser target emits UMD (same as Three).
@file:JsModule("three/examples/jsm/loaders/GLTFLoader.js")
@file:JsNonModule
@file:Suppress("UnusedParameter")

package external

external class GLTFLoader {
    /** onLoad receives a `gltf` object whose `.scene` is a THREE.Group of meshes. */
    fun load(
        url: String,
        onLoad: (dynamic) -> Unit,
        onProgress: (dynamic) -> Unit = definedExternally,
        onError: (dynamic) -> Unit = definedExternally,
    )
}
