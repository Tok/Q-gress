package ai.llm

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlin.js.Promise
import kotlin.js.json

/**
 * A real in-browser LLM backend (PLAN Phase 6.3): wraps **WebLLM** (`@mlc-ai/web-llm`, WebGPU) loaded lazily
 * from a CDN the first time it's asked to complete. EXPERIMENTAL + browser-only (needs WebGPU + a few-hundred-MB
 * model download); everything is wrapped so a missing/failed model just yields `""` and [LlmPolicy] falls back
 * to the heuristic. Status is exposed for the reasoning panel. Unverifiable headless — verify in the browser.
 *
 * The dynamic `import()` is built via `new Function` so the webpack bundler doesn't try to resolve the CDN URL
 * at build time (it stays a genuine runtime import).
 */
class WebLlmClient(private val model: String = DEFAULT_MODEL) : LlmClient {
    private var engine: dynamic = null
    private var activeModel: String = model // the build actually loaded (may swap f16 → f32, see [engine])

    /** The model this client was asked to run (pre any f16→f32 swap) — for picker bookkeeping. */
    val requestedModel: String get() = model

    /** The MLC model id being run (for the reasoning / brains panels). */
    val modelId: String get() = activeModel

    var status: String = "idle"
        private set

    override suspend fun complete(prompt: String): String {
        if (!webGpuAvailable()) { // the #1 failure ("Unable to find a compatible GPU") — give the actionable fix
            status = "WebGPU unavailable — enable $WEBGPU_FLAG (Chrome/Brave), then reload"
            return "" // → LlmPolicy keeps using its heuristic fallback
        }
        return runCatching {
            val eng = engine()
            val request = json(
                "messages" to arrayOf(json("role" to "user", "content" to prompt)),
                "temperature" to 0.7,
                "max_tokens" to 200,
            )
            val response = eng.chat.completions.create(request).unsafeCast<Promise<dynamic>>().await()
            status = "ready"
            (response.choices[0].message.content as? String).orEmpty()
        }.getOrElse {
            // A GPU/driver failure often reads as a generic error — point at the flag that usually fixes it.
            status = "error: ${it.message} · try $WEBGPU_FLAG"
            "" // → LlmPolicy keeps using its heuristic fallback
        }
    }

    // Lazily load WebLLM from the CDN + create the engine, once. `new Function` hides the import() from
    // webpack's static analysis so it stays a true runtime import (the CDN URL isn't bundled). Picks the f16
    // build only when the GPU supports the WGSL `shader-f16` feature; otherwise swaps to the q4f32_1 build
    // (many setups — Linux / Brave / software adapters — reject `enable f16;` and the f16 shaders won't compile).
    private suspend fun engine(): dynamic {
        if (engine != null) return engine
        activeModel = if (supportsF16()) model else model.replace("q4f16_1", "q4f32_1")
        status = "loading $activeModel…"
        val import = js("(new Function('u', 'return import(u)'))")
        val module = import(CDN).unsafeCast<Promise<dynamic>>().await()
        engine = module.CreateMLCEngine(activeModel).unsafeCast<Promise<dynamic>>().await()
        return engine
    }

    // Whether the WebGPU adapter exposes the `shader-f16` feature (the `enable f16;` WGSL extension). Without
    // it, f16-quantized MLC models fail shader compilation, so we fall back to the f32 build.
    private suspend fun supportsF16(): Boolean = runCatching {
        val adapter = js("navigator.gpu.requestAdapter()").unsafeCast<Promise<dynamic>>().await()
        adapter != null && (adapter.features.has("shader-f16") as Boolean)
    }.getOrDefault(false)

    companion object {
        private const val CDN = "https://esm.run/@mlc-ai/web-llm"

        // Small + fast; the f16 build is preferred but [engine] auto-swaps to the q4f32_1 build when the GPU
        // lacks the `shader-f16` feature (else its shaders fail to compile). Swap for a stronger model freely.
        const val DEFAULT_MODEL = "Qwen2.5-0.5B-Instruct-q4f16_1-MLC"

        // Curated in-browser-friendly WebLLM (MLC) models offered in the driver picker (label → model id). All
        // are q4f16_1 ids; [engine] swaps each to its q4f32_1 build when shader-f16 is missing. Picking a
        // DIFFERENT model per faction makes for more interesting LLM-vs-LLM matches — but each loads its own
        // few-hundred-MB-to-~2GB weights into VRAM, so two large models at once can exceed a modest GPU.
        val MODELS: List<Pair<String, String>> = listOf(
            "Qwen2.5 0.5B (smallest)" to "Qwen2.5-0.5B-Instruct-q4f16_1-MLC",
            "Llama 3.2 1B" to "Llama-3.2-1B-Instruct-q4f16_1-MLC",
            "Qwen2.5 1.5B" to "Qwen2.5-1.5B-Instruct-q4f16_1-MLC",
            "SmolLM2 1.7B" to "SmolLM2-1.7B-Instruct-q4f16_1-MLC",
            "Gemma 2 2B" to "gemma-2-2b-it-q4f16_1-MLC",
        )

        /** The Chromium flag that enables WebGPU where it's gated (esp. Linux / Brave). Shown in the brains UI. */
        const val WEBGPU_FLAG = "chrome://flags/#enable-unsafe-webgpu"

        /** True if the browser exposes the WebGPU API (`navigator.gpu`). False → the LLM can't run. */
        fun webGpuAvailable(): Boolean = js("typeof navigator !== 'undefined' && !!navigator.gpu") as Boolean

        private var gpuReportCache: String? = null
        private var gpuQuerying = false

        /**
         * A best-effort WebGPU capability line: the adapter description + its **max buffer / storage-binding
         * limits** + whether `shader-f16` is supported. NOTE: browsers expose NO free-VRAM figure (privacy), so
         * these limits are the practical ceiling on what a model can allocate — the closest thing to a "VRAM
         * readout" the web allows. Cached; the first call kicks off the async query and returns a placeholder.
         */
        fun gpuReport(): String {
            gpuReportCache?.let { return it }
            queryGpu()
            return if (webGpuAvailable()) "querying GPU…" else "no WebGPU — enable $WEBGPU_FLAG"
        }

        private fun queryGpu() {
            if (gpuQuerying || gpuReportCache != null || !webGpuAvailable()) return
            gpuQuerying = true
            MainScope().launch {
                gpuReportCache = runCatching { buildGpuReport() }.getOrElse { "GPU query failed: ${it.message}" }
            }
        }

        private suspend fun buildGpuReport(): String {
            val adapter = js("navigator.gpu.requestAdapter()").unsafeCast<Promise<dynamic>>().await()
                ?: return "no WebGPU adapter (software-only?)"
            val lim = adapter.limits
            val info = adapter.info
            val desc = ((info?.description ?: info?.architecture ?: info?.vendor) as? String)?.takeIf { it.isNotBlank() }
                ?: "unknown adapter"
            val f16 = adapter.features.has("shader-f16") as Boolean
            return "$desc · max buffer ${mb(lim.maxBufferSize)} · max storage-binding ${mb(lim.maxStorageBufferBindingSize)} · " +
                "shader-f16 ${if (f16) "yes" else "no"}"
        }

        private fun mb(bytes: dynamic): String = "${(((bytes as? Double) ?: 0.0) / (1024.0 * 1024.0)).toInt()} MB"
    }
}
