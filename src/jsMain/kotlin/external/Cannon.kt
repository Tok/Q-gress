// External bindings for the cannon-es npm module (rigid-body physics, bundled by webpack).
// Lean by design: constructors + the few methods we call are typed; mutable props
// (gravity/position/quaternion/velocity/angularVelocity/*Damping) are reached via asDynamic()
// at the call sites in Scene3D. @JsNonModule is required because the browser target emits UMD.
@file:Suppress("UnusedParameter", "UnusedPrivateProperty")

package external

@JsModule("cannon-es")
@JsNonModule
external object Cannon {
    class Vec3(x: Double = definedExternally, y: Double = definedExternally, z: Double = definedExternally) {
        fun set(x: Double, y: Double, z: Double)
    }

    class Quaternion

    class World(options: dynamic = definedExternally) {
        fun addBody(body: Body)
        fun removeBody(body: Body)
        fun step(timeStep: Double, timeSinceLastCalled: dynamic = definedExternally, maxSubSteps: dynamic = definedExternally)
    }

    class Body(options: dynamic = definedExternally)

    class Box(halfExtents: Vec3)
    class Plane
    class Material(name: dynamic = definedExternally)
}
