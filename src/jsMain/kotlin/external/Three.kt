// External JS bindings for the global THREE namespace (three.js UMD, loaded in
// index.html). Lean by design: constructors + the methods we call are typed;
// mutable instance properties (position/rotation/scale/material/...) are reached
// via asDynamic() at the call sites in Scene3D. Parameters describe the JS
// contract and have no Kotlin body, so detekt's "unused" rules don't apply.
@file:Suppress("UnusedParameter", "UnusedPrivateProperty")

package external

@JsName("THREE")
external object Three {
    class Scene {
        fun add(obj: dynamic)
        fun remove(obj: dynamic)
    }

    class Camera {
        var projectionMatrix: Matrix4
    }

    class WebGLRenderer(params: dynamic) {
        var autoClear: Boolean
        fun resetState()
        fun render(scene: Scene, camera: Camera)
        fun dispose()
    }

    class Matrix4 {
        fun fromArray(array: dynamic): Matrix4
        fun makeTranslation(x: Double, y: Double, z: Double): Matrix4
        fun scale(v: Vector3): Matrix4
        fun multiply(m: Matrix4): Matrix4
        fun copy(m: Matrix4): Matrix4
        fun invert(): Matrix4
    }

    class Vector3(x: Double = definedExternally, y: Double = definedExternally, z: Double = definedExternally) {
        fun applyMatrix4(m: Matrix4): Vector3
        fun subVectors(a: Vector3, b: Vector3): Vector3
        fun normalize(): Vector3
    }

    class Raycaster {
        fun set(origin: Vector3, direction: Vector3)
        fun intersectObjects(objects: dynamic, recursive: Boolean = definedExternally): dynamic
    }

    class DirectionalLight(color: dynamic, intensity: dynamic = definedExternally)
    class AmbientLight(color: dynamic, intensity: dynamic = definedExternally)

    class Group
    class Mesh(geometry: dynamic, material: dynamic)
    class Sprite(material: dynamic)
    class Line(geometry: dynamic, material: dynamic)

    val NearestFilter: Int

    class BoxGeometry(width: dynamic, height: dynamic, depth: dynamic)
    class PlaneGeometry(width: dynamic, height: dynamic)
    class RingGeometry(
        innerRadius: dynamic,
        outerRadius: dynamic,
        thetaSegments: dynamic = definedExternally,
    )
    class SphereGeometry(
        radius: dynamic,
        widthSegments: dynamic = definedExternally,
        heightSegments: dynamic = definedExternally,
    )
    class CylinderGeometry(
        radiusTop: dynamic,
        radiusBottom: dynamic,
        height: dynamic,
        radialSegments: dynamic = definedExternally,
    )
    class ConeGeometry(
        radius: dynamic,
        height: dynamic,
        radialSegments: dynamic = definedExternally,
    )
    class BufferGeometry {
        fun setFromPoints(points: dynamic): BufferGeometry
    }

    class MeshBasicMaterial(params: dynamic)
    class MeshStandardMaterial(params: dynamic)
    class LineBasicMaterial(params: dynamic)
    class SpriteMaterial(params: dynamic)
    class CanvasTexture(canvas: dynamic)
}
