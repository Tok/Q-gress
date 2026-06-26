# LLM.md — in-browser LLM faction driver (Track B)

> **Status: shipped, experimental.** A real **WebLLM (MLC) on WebGPU** driver runs entirely in the browser
> (`ai/llm/`). It's **off by default** (neural net is the default) and gated behind an onboarding opt-in,
> because it needs a capable WebGPU GPU. Shares the substrate (policy API, `Observation` schema) with the
> custom net — see [NN.md](NN.md).

## The idea ("Gemma-vs-Gemma")

A small **in-browser LLM** drives a faction: prompted each checkpoint with the world state, it emits a
**slider vector**. Flashier and explainable; it *reasons* rather than *learns*. Same `FactionPolicy` API as
the net, so any side can be Human / Heuristic / Net / LLM independently — and each LLM side can run a
**different model**, so net-vs-LLM and (e.g.) Qwen-vs-Gemma matches are possible.

## What's built (`ai/llm/`)

- **`WebLlmClient`** — wraps **WebLLM** (`@mlc-ai/web-llm`, WebGPU), loaded lazily from a CDN via a runtime
  `import()` (hidden from webpack so the URL isn't bundled). Exposes `status` / `modelId`. Robustness:
  - probes the adapter for the WGSL **`shader-f16`** feature and **auto-swaps `q4f16_1` → `q4f32_1`** when
    it's missing (many Linux / software setups reject `enable f16;`, which otherwise fails shader compile);
  - everything is wrapped — missing WebGPU / device-loss / parse failures yield `""` so `LlmPolicy` simply
    keeps using the heuristic (no crash), with an actionable `status`;
  - a curated model list (`MODELS`): SmolLM2 360M · Qwen 2.5 0.5B/1.5B · Llama 3.2 1B · SmolLM2 1.7B ·
    Gemma 2 2B; plus `webGpuAvailable()`, `isChromiumLike()`, and `gpuReport()` (adapter + max-buffer limits
    + f16 — the closest thing to a VRAM gauge the web exposes).
- **`LlmPolicy`** — a `FactionPolicy` driven at **checkpoint cadence**, off the tick loop: world state →
  `LlmPrompt` → model → JSON → `LlmParser` → `SliderVector`. Async + non-blocking (`weight()`/`currentVector()`
  return the **last** vector, falling back to an adaptive `HeuristicPolicy` until the first reply lands and
  whenever a reply is unparseable). At most one request per checkpoint.

## Where to see it / how to enable it

- **Onboarding** has a "Show experimental LLM driver" checkbox (off by default); it unlocks the LLM option
  and rides the start URL as `?exp`. The per-faction driver dropdown then offers **LLM** + a **model picker**.
- **BRAINS tab** (`util/ui/BrainsPanel`) — the LLM card shows model / backend / status, the parsed **chose**
  as the headline, a collapsed raw prompt/reply, the **WebGPU capability readout**, and (on Chromium only) a
  collapsible `chrome://` troubleshooting help.

## Requirements / gotchas
- Needs **WebGPU on a real GPU**. On Chromium, that often means enabling `chrome://flags/#enable-unsafe-webgpu`
  (+ `#enable-vulkan`, `#ignore-gpu-blocklist` on Linux/NVIDIA), and checking `chrome://gpu` shows hardware
  Vulkan (not SwiftShader). The model is a few-hundred-MB-to-~2GB download, cached by the browser.
- Each LLM faction loads its **own** model weights → two large models at once can exceed a modest GPU.

## Open questions / next
- Better prompt format + output schema (compact `Observation` serialization; robust JSON).
- Model/runtime choices + size budget; per-GPU pre-flight to disable cleanly on insufficient hardware.
- Latency / stability handling so checkpoint-cadence inference never stalls or crashes the renderer.

Cross-cutting: desktop-only + WebGPU gating; reproducibility via `?seed=`. See
[ARCHITECTURE.md](ARCHITECTURE.md) for the slider seam and scoring.
