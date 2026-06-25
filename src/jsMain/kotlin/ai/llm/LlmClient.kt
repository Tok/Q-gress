package ai.llm

/**
 * A pluggable LLM backend (PLAN Phase 6.3): turn a [prompt] into completion text, asynchronously. A real
 * implementation wraps an in-browser model (transformers.js / WebLLM / MediaPipe, WebGPU) behind this one
 * method; [MockLlmClient] is the deterministic stand-in used by tests + the headless path.
 */
interface LlmClient {
    suspend fun complete(prompt: String): String
}

/** Returns a fixed [canned] completion regardless of the prompt — a deterministic LLM stand-in. */
class MockLlmClient(private val canned: String) : LlmClient {
    override suspend fun complete(prompt: String): String = canned
}
