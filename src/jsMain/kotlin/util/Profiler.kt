package util

import system.ui.Bootstrap

/**
 * Lightweight world-gen timing (PLAN phase D — establish a baseline). `performance.now()` stage marks logged
 * to the console as `[perf] …`, so both the browser DevTools console and the headless CDP profiler can read a
 * per-stage breakdown of a world build, plus an aggregate of the per-portal flow-field cost (the suspected
 * large-map hotspot). One-shot per build, so it's always on (negligible); the per-frame FPS readout lives in
 * [system.ui.FpsMeter] (`?debug`). No-op outside the browser so the functional core stays test-clean.
 */
object Profiler {
    /** `performance.now()` in the browser, 0 headless. Public so suspend hot-paths (Pathfinding) can time inline. */
    fun nowMs(): Double = if (Bootstrap.isRunningInBrowser()) js("performance.now()").unsafeCast<Double>() else 0.0
    private fun now(): Double = nowMs()

    private var genStart = 0.0
    private var lastMark = 0.0
    private var fieldCount = 0
    private var fieldTotalMs = 0.0
    private var fieldMaxMs = 0.0

    /** Start a world-gen timing run (call at the very start of generation). */
    fun beginWorldGen() {
        genStart = now()
        lastMark = genStart
        fieldCount = 0
        fieldTotalMs = 0.0
        fieldMaxMs = 0.0
    }

    /** Log a stage boundary: time since the previous mark + cumulative since [beginWorldGen]. */
    fun mark(stage: String) {
        if (Bootstrap.isNotRunningInBrowser()) return
        val t = now()
        console.log("[perf] $stage: +${(t - lastMark).toInt()}ms (total ${(t - genStart).toInt()}ms)")
        lastMark = t
    }

    /** Time a synchronous [block], logging its wall-clock; returns the block's result. */
    fun <T> time(label: String, block: () -> T): T {
        if (Bootstrap.isNotRunningInBrowser()) return block()
        val s = now()
        val r = block()
        console.log("[perf] $label: ${(now() - s).toInt()}ms")
        return r
    }

    /** Record one flow-field computation's wall-clock (called per portal field — see Pathfinding). */
    fun addFieldMs(ms: Double) {
        fieldCount++
        fieldTotalMs += ms
        if (ms > fieldMaxMs) fieldMaxMs = ms
    }

    /** Log the aggregate flow-field cost for the build (count, total, avg, worst). */
    fun flushFields() {
        if (Bootstrap.isNotRunningInBrowser() || fieldCount == 0) return
        val avg = (fieldTotalMs / fieldCount).toInt()
        console.log(
            "[perf] flow-fields: $fieldCount fields · ${fieldTotalMs.toInt()}ms total · ${avg}ms avg · ${fieldMaxMs.toInt()}ms worst",
        )
    }
}
