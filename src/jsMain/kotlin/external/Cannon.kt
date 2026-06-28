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
        fun addContactMaterial(cm: ContactMaterial)
        fun step(timeStep: Double, timeSinceLastCalled: dynamic = definedExternally, maxSubSteps: dynamic = definedExternally)
    }

    // Sweep-and-prune broadphase: ~O(n log n) instead of NaiveBroadphase's O(n²) candidate pairs — the FX
    // debris/digit worlds spawn many short-lived bodies, so this cuts the per-step collision cost in combat.
    class SAPBroadphase(world: World)

    class Body(options: dynamic = definedExternally)

    class Box(halfExtents: Vec3)
    class Plane
    class Material(name: dynamic = definedExternally)

    // Per-pair friction/restitution (options = { friction, restitution }). Registered via World.addContactMaterial;
    // a body carries a Material, and the pair's ContactMaterial sets how bouncy that contact is.
    class ContactMaterial(m1: Material, m2: Material, options: dynamic = definedExternally)
}
