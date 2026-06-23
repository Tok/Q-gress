// External bindings for the three.js npm module (bundled by webpack). Lean by
// design: constructors + the methods we call are typed; mutable instance
// properties (position/rotation/scale/material/...) are reached via asDynamic()
// at the call sites in Scene3D. Parameters describe the JS contract and have no
// Kotlin body, so detekt's "unused" rules don't apply.
@file:Suppress("UnusedParameter", "UnusedPrivateProperty")

package external

@JsModule("three")
@JsNonModule
external object Three {
    class Scene {
        fun add(obj: dynamic)
        fun remove(obj: dynamic)
    }

    class Camera {
        var projectionMatrix: Matrix4
    }

    class PerspectiveCamera(
        fov: dynamic = definedExternally,
        aspect: dynamic = definedExternally,
        near: dynamic = definedExternally,
        far: dynamic = definedExternally,
    )

    class WebGLRenderer(params: dynamic) {
        var autoClear: Boolean
        fun resetState()
        fun clearDepth() // wipe the depth buffer so the 3D scene draws over the map's own layers
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

    class Quaternion {
        fun setFromUnitVectors(from: Vector3, to: Vector3): Quaternion
    }

    class Raycaster {
        fun set(origin: Vector3, direction: Vector3)
        fun intersectObjects(objects: dynamic, recursive: Boolean = definedExternally): dynamic
    }

    class DirectionalLight(color: dynamic, intensity: dynamic = definedExternally)
    class AmbientLight(color: dynamic, intensity: dynamic = definedExternally)
    class PointLight(color: dynamic, intensity: dynamic = definedExternally, distance: dynamic = definedExternally)

    class Group
    class Mesh(geometry: dynamic, material: dynamic)
    class Sprite(material: dynamic)
    class Line(geometry: dynamic, material: dynamic)
    class Float32BufferAttribute(array: dynamic, itemSize: dynamic)

    val NearestFilter: Int
    val AdditiveBlending: Int
    val DoubleSide: Int
    val NormalBlending: Int
    val EquirectangularReflectionMapping: Int

    class BoxGeometry(width: dynamic, height: dynamic, depth: dynamic)
    class PlaneGeometry(width: dynamic, height: dynamic)
    class TorusGeometry(
        radius: dynamic,
        tube: dynamic,
        radialSegments: dynamic = definedExternally,
        tubularSegments: dynamic = definedExternally,
    )
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
        heightSegments: dynamic = definedExternally,
        openEnded: dynamic = definedExternally,
    )
    class Shape {
        fun moveTo(x: dynamic, y: dynamic)
        fun lineTo(x: dynamic, y: dynamic)
        val holes: dynamic
    }
    class Path {
        fun absellipse(
            aX: dynamic,
            aY: dynamic,
            xRadius: dynamic,
            yRadius: dynamic,
            startAngle: dynamic,
            endAngle: dynamic,
        )
    }
    class ShapeGeometry(shapes: dynamic)
    class ExtrudeGeometry(shapes: dynamic, options: dynamic = definedExternally)
    class ConeGeometry(
        radius: dynamic,
        height: dynamic,
        radialSegments: dynamic = definedExternally,
    )
    class DodecahedronGeometry(radius: dynamic = definedExternally)
    class BufferGeometry {
        fun setFromPoints(points: dynamic): BufferGeometry
    }

    class EdgesGeometry(geometry: dynamic, thresholdAngle: dynamic = definedExternally) // polygon edges only (merges coplanar tris)
    class LineSegments(geometry: dynamic, material: dynamic)

    class MeshBasicMaterial(params: dynamic)
    class MeshStandardMaterial(params: dynamic)
    class MeshPhysicalMaterial(params: dynamic) // adds transmission/clearcoat → real glass
    class ShaderMaterial(params: dynamic)
    class LineBasicMaterial(params: dynamic)
    class SpriteMaterial(params: dynamic)
    class CanvasTexture(canvas: dynamic)
}
