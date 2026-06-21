// External binding for the global `uPlot` charting library (loaded via <script> in index.html).
// Options/data are passed as dynamic JS so series/scales can be built at runtime.
@file:Suppress("UnusedParameter", "UnusedPrivateProperty")

package external

@JsName("uPlot")
external class UPlot(opts: dynamic, data: dynamic, target: dynamic) {
    fun setData(data: dynamic)
    fun setSize(size: dynamic)
    fun destroy()
}
