# LLM.md — in-browser LLM faction driver (Phase 6, Track B)

> **Status: stub / prep.** Design notes for the LLM-driven faction. The work itself is scoped in
> [../PLAN.md](../PLAN.md) → *Phase 6 (Track B: in-browser LLM driver)*; flesh this out when 6.3
> starts. Shares the substrate (policy API, observation/slider schema) with the custom net —
> see [NN.md](NN.md).

## The idea (the original "Gemma-vs-Gemma")

A small **in-browser LLM** drives a faction: prompted each checkpoint with the world state, it
emits a **slider vector** (and a short rationale). Flashier and explainable in natural language;
it *reasons* rather than *learns*. Same `FactionPolicy` API as the net, so any faction can be
Human / Net / LLM independently (net-vs-Gemma, Gemma-vs-Gemma, human-vs-net, …).

## Mechanism (planned)

- `LlmPolicy` driven at **checkpoint cadence**, off the tick loop: world state → compact prompt
  → model → **JSON slider vector**, schema-validated with a defensive fallback to the last/default
  vector on any parse failure. The sim keeps running on the last vector while inference is in flight.
- Engine(s) behind one interface: **transformers.js** (WebGPU small instruct model) and/or
  **Gemma via MediaPipe LLM Inference** — `external` declarations like MapLibre/uPlot.
- **Explainability**: surface the model's reasoning text in a "faction AI commentary" panel.

## Open questions (resolve at 6.3)
- Which model/runtime (transformers.js choice vs Gemma/MediaPipe); WebGPU gating + size budget.
- Prompt format (how to compactly serialize the `Observation`); output JSON schema.
- Latency handling so checkpoint-cadence inference never stalls the sim.

Cross-cutting: desktop-only + WebGPU gating; reproducibility via `?seed=`. See
[ARCHITECTURE.md](ARCHITECTURE.md) for the slider seam and scoring.
