# PLAN.md — Q-Gress roadmap (what's next)

Branch: `develop` · Owner: @zirteq

Future TODOs only. For *what's already shipped* see [docs/FEATURES.md](docs/FEATURES.md);
for *how the system fits together* see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md); for the
AI-driver design notes see [docs/NN.md](docs/NN.md) + [docs/LLM.md](docs/LLM.md).

## North star

Q-Gress becomes an **AI-vs-AI sandbox**: each faction (ENL/RES) is driven by an agent whose
**output layer _is_ the 18 behaviour sliders** — a custom net and/or an in-browser LLM. Human
can play any side; any two brains can be matched. **Desktop-only**; mobile is blocked. Until
the AI layer lands, the slider sim is the substrate we keep hardening.

## Near-term queue
- [ ] **Full Web Audio 3D** — replace the screen-projected `Scene3D.audioPan` with a `PannerNode`
  + camera-driven listener (distance attenuation + front/back + elevation).
- [ ] **Tear down `shadowMap` after grid build** — it's only needed at startup (grid + POI names);
  null it out after `addGrid` to free the held WebGL context.

## 3D / rendering
- [ ] **Stage 2 leftovers** — **shield visualization** (needs `Portal.deployMods` to actually store
  mods; today a stub); a richer field-up "whoosh".
- [ ] **Stage 3 — pathfinding scalability** — drop the per-portal full-map flow field; multi-mode
  nav (flow fields near, cheap nav far); ambient NPCs; continuous toggleable field viz.
- [ ] **Stage 4** — humanoid glTF models (ready: people are head-sized spheres at head height),
  pairs with the colony-management attributes (icebox).

## UI
- [ ] **Stage 2** — **Schematic** base view (reuse `SHADOW_STYLE`) + more toggleable info overlays
  (e.g. movement-penalty heatmap) alongside the existing Terrain/Vectors toggles.
- [ ] **Stage 4** — tuning-slider panel redesign (both factions, presets); ties into Phase 6.4
  (per-faction driver selection + AI-driven sliders animating read-only).
- [ ] **Stage 5** — visual theme pass + responsiveness.

## Onboarding (Phase 7 leftovers)
- [ ] **Location selection**: Home / nearest city via Geolocation; a curated preset list; Random;
  surface the free-form search on the onboarding screen (it only exists in-game now).
- [ ] Real **per-stage load %** (especially flow-field computation).
- [ ] **Initial roster "roll"** — light flavour, not a gacha loop; ties to the icebox rarity tiers.
- [ ] **`?debug` dev tooling** — timing measurements + console logging to profile the long loads.

## Phase 6 — AI-vs-AI (the Q-gress payoff)

**Decided:** build **both** AI drivers on **one** shared substrate, so any faction can be Human /
Net / LLM independently and fight in any combination. Track design lives in [docs/NN.md](docs/NN.md)
(custom net + neuroevolution) and [docs/LLM.md](docs/LLM.md) (in-browser LLM). The slider vector
stays the action substrate (the net/LLM only re-tunes the 18 sliders at checkpoint cadence — it does
**not** replace per-agent `ActionSelector`).

**6.0 — Substrate I: programmatic policy API + determinism** _(prereq, no AI yet)_
- [ ] **`FactionPolicy`** — replace the DOM slider read in `ActionSelector.q()` with a per-faction
  policy source; default `DomSliderPolicy` = today's behaviour (zero gameplay change).
- [ ] **`Observation` (pure)** — `observe(world, faction): DoubleArray`, fixed normalized feature
  vector (MU + Δ, per-faction counts, tick fraction, avg agent XM/level, …). The NN/LLM input.
- [ ] **`SliderVector` (pure)** — 18 named slots ↔ `QActions`+`QDestinations`; encode/decode + clamp.
- [ ] **Injectable seedable RNG** — replace inline `Math.random()` in `Util.random()` with a
  threaded seedable `Rng`. Exit: human play byte-identical; **same seed → identical match** (test).

**6.1 — Substrate II: headless match harness** _(the training engine; pays off the functional-core split)_
- [ ] **`SimRunner`** — `runMatch(gridFixture, policyEnl, policyRes, seed, maxTicks): MatchResult`,
  tick loop with rendering/audio/DOM stubbed at the shell boundary.
- [ ] **Grid fixtures** — serialize a built `Grid` (+ portal seeds) to committed JSON so matches
  reproduce without live tiles / `readPixels`.
- [ ] **Speed** — accelerated in Node (CI) and a Web Worker (in-tab training). Exit: a full match
  runs headless & deterministic, hundreds/min.

**6.2 — Track A: custom net + neuroevolution** → [docs/NN.md](docs/NN.md)
- [ ] Tiny MLP (`ai/net/`, output = 18 sliders); ES/self-play trainer; `NetPolicy` (JSON genome);
  live **activation + path visualization**. Exit: beats the default-slider baseline over K seeded
  matches, loads into the live game, activations visualized.

**6.3 — Track B: in-browser LLM driver** → [docs/LLM.md](docs/LLM.md)
- [ ] `LlmPolicy` at checkpoint cadence (state → prompt → JSON slider vector, schema-validated,
  defensive fallback); transformers.js and/or Gemma/MediaPipe behind one interface; reasoning panel.
  Exit: an LLM drives a faction end-to-end in-browser, sim stays smooth.

**6.4 — Mix, match & human-vs-AI**
- [ ] Per-faction driver selection (Human / Net / LLM) in onboarding + the Stage-4 slider panel →
  any combination; AI-driven sliders animate read-only. Tournament/eval view (round-robin → leaderboard).

**Cross-cutting:** desktop-only + **WebGPU** gating; seeds via `?seed=`; **balance risk** — pure
win-maximizing self-play may rediscover the recruit-rush degenerate (below), so keep tuning `Config`
and consider shaping fitness for *interesting* play (follow-up, not v1).

## Open decisions
- **Coverage tooling.** Kover has no Kotlin/JS support. Real line-coverage arrives with the
  **functional-core split** (extract pure logic into `commonMain` + a `jvm()` test target, run Kover
  there). Until then the gates are ktlint + detekt + tests. (6.0/6.1 are the natural place to start
  the split.)

## Balance note (recruit-rush, root cause)
`ActionSelector` picks by weighted-random over `slider × weight`. Recruiting adds agents (up to a
cap), and more agents = more actions/tick = a compounding snowball, so rushing recruitment beats
balanced play. Phase 5 gave recruiting an **XM cost + diminishing returns**; deeper "no strategy
dominates" validation is iterative (playtest, or a future headless strategy-comparison harness — see
6.1). Self-play fitness shaping is the AI-era lever.

## Under consideration (icebox)
- **Colony-management / roster (gameplay expansion).** Per-entity attributes (endurance/speed/agility/
  radius, building on `agent/Skills`+`AgentSize`); **rarity-tiered agents** (randomised attributes but
  **no gambling UX** — the player *manages* composition, not a gacha loop); **items** (skateboards,
  **jet-skis** for water traversal → makes marina/bridge presets playable, power-banks, second phones);
  **battery/accu %** (depleted phone → the player leaves the scene). Pairs with the 3D humanoid work.
- **Null-safety hardening.** Audit every `!!` and replace with `?.`/`?:`/`requireNotNull`/early return
  (same NPE/`NoSuchElement` hazard class as the empty-collection `max/min` crashes already fixed).
- **Rework the movement / pathfinding model.** Derive walkability/penalties from the **vector-tile road
  geometry** (query features / GeoJSON) and/or a graph/navmesh, instead of reading rasterized shadow
  pixels. Decouples the sim from the screen and unblocks dynamic zoom — natural partner of the
  functional-core split + the 6.1 grid fixtures.
- **Going 3D (gameplay).** A pitched/3D camera breaks the top-down screen→grid mapping; needs a
  decoupled simulation grid or a 3D pathfinding model. Revisit after the functional-core split. (3D
  *buildings* in the top-down satellite view already work.)

## Constraints / agreements
- Commit to `develop`; **no pushing** until something works end-to-end.
- Keep `CLAUDE.md` + this file + `docs/` current as work lands; no overlapping info between them.
- Desktop-only; do not invest in mobile support.
