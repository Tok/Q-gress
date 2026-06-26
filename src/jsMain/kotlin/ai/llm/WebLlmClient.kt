package ai.llm

import kotlinx.coroutines.await
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

    /** The MLC model id being run (for the reasoning / brains panels). */
    val modelId: String get() = model

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
    // webpack's static analysis so it stays a true runtime import (the CDN URL isn't bundled).
    private suspend fun engine(): dynamic {
        if (engine != null) return engine
        status = "loading model…"
        val import = js("(new Function('u', 'return import(u)'))")
        val module = import(CDN).unsafeCast<Promise<dynamic>>().await()
        engine = module.CreateMLCEngine(model).unsafeCast<Promise<dynamic>>().await()
        return engine
    }

    companion object {
        private const val CDN = "https://esm.run/@mlc-ai/web-llm"
        const val DEFAULT_MODEL = "Qwen2.5-0.5B-Instruct-q4f16_1-MLC" // small + fast; swap for a stronger one

        /** The Chromium flag that enables WebGPU where it's gated (esp. Linux / Brave). Shown in the brains UI. */
        const val WEBGPU_FLAG = "chrome://flags/#enable-unsafe-webgpu"

        /** True if the browser exposes the WebGPU API (`navigator.gpu`). False → the LLM can't run. */
        fun webGpuAvailable(): Boolean = js("typeof navigator !== 'undefined' && !!navigator.gpu") as Boolean
    }
}
