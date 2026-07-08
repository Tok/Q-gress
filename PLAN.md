# PLAN.md — Q-Gress roadmap (what's next)

Branch: `main` · Owner: @zirteq

**Concrete, standalone work only.** *Shipped* → [docs/FEATURES.md](docs/FEATURES.md); *how it fits together* →
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md); AI-driver design → [docs/NN.md](docs/NN.md) +
[docs/LLM.md](docs/LLM.md). The big interconnected far-horizon vision (the "grand game" across locations + the
roster/colony layer under it) lives in [docs/FUTURE.md](docs/FUTURE.md) — not here. Completed work lives in the
**git log**, not here. Keep this file to open, self-contained tasks you could pick up and finish now.

## North star — reached (pretty much)
Q-Gress is an **AI-vs-AI sandbox**: each faction (ENL/RES) is driven by an agent whose **output _is_ the
17 behaviour sliders** — a custom net and/or an in-browser LLM. A human can play any side; any two brains can be
matched. **Desktop-only**; mobile is blocked. The substrate ships; what's left below is tuning, polish, and
technical improvements — the ambitious expansion is deliberately parked in `docs/FUTURE.md`.

## AI-vs-AI (tuning the payoff)
**Fitness objective:** win the cycle by **leading the most checkpoints** — fitness is the net checkpoint-win
margin over a full cycle (summed MU margin only as a sub-integer tiebreak; `MatchResult.checkpointFitness`,
matching `CheckpointStats`). The net/LLM re-tunes the 17 sliders at checkpoint cadence; it does **not** replace
the per-agent `ActionSelector`. Training/eval is pinned to the shipped default balance (`MatchSetup.useDefaultBalance`)
so champions are "one fits all". Champions live as one JSON per arch under `resources/champions/` (`ChampionLibrary`).

- [ ] **Field-layering Linker nudge (belongs with a rebake).** The *mechanic* is verified (`MultilayerFieldTest`);
  agents layer rarely for a purely **behavioural** reason: `Linker.fieldClosingTarget` (Linker.kt:32-35) closes
  *any* triangle with no preference for nesting. Bias the `Linker` toward links that nest under an existing field
  (anchor-fanning), instrument a headless run for layered-field counts, re-evaluate. *(Open: a rose-method
  scenario test; a link-amp mod raising the hard 8-link cap for deeper layers.)*
- [ ] **Re-run the rebake after balance/behaviour changes.** Two scripts drive it: `scripts/bake-champs.sh`
  (gen-1, each arch vs the `HeuristicPolicy` baseline) and `scripts/train-champs.sh` (gen-2+, NN-vs-NN — each
  arch's challenger vs its own current champion, tournament decider, overwrite-on-win + an overall ladder). Re-run
  after the field-layering nudge (and any balance change — a champion is only as good as the balance it learned).
  Deeper budget / self-play would strengthen them further.
- [ ] **Grid fixtures** — infra is built (`GridFixture` RLE + `GridCapture` ?debug=capture + `PresetConnectivityTest`).
  Only the committed `PresetFixtures.kt` is missing — currently an **empty placeholder** (`PRESET_FIXTURES =
  emptyList()`), so the connectivity test audits nothing. Run `?debug=capture` once in-browser, drop the download
  into `src/jsTest/kotlin/util/`, commit.

## Rendering / perf
- [ ] **Pathfinding scalability.** Flow fields are still **per-portal full-map**: the want is multi-mode nav (flow
  fields near, cheap nav far) + a coarser `pathResolution` lever for very large maps, plus a field viz.
- [ ] **Precompute the title world.** Serialize the fixed title location's grid + portal positions + flow fields
  (extend the `GridCapture` fixture pattern) and load them instead of the live shadow-readback + async field
  compute, so the title sim is instant.
- [ ] **Building perf + lifecycle.** (a) A dense streamed city can reach 1000s of shadow-casting meshes; if FPS
  suffers, **merge** static buildings into a few `BufferGeometry` batches (keep play-area ones individually
  shakeable); (b) wire `OwnBuildings.clear()` + `BuildingStream.reset()` into world-regen (they exist, unused);
  (c) a per-bbox **Overpass response cache** so a long fly-around doesn't hammer the public instance; (d) widen /
  view-follow the sun's ortho shadow camera so **streamed** buildings beyond the play area cast/receive.
- [ ] **Anti-aliasing (the terrain custom-layer problem).** Deferred (not critical). Under 3D terrain MapLibre
  renders the three.js custom layer to an offscreen texture, so the GL-context `antialias` (MSAA) never reaches
  it; live `setPixelRatio` SSAA desyncs the layer's screen-space (objects shift on toggle). Do it **hands-on
  in-browser**: likely **construction-time SSAA** (`pixelRatio` at map creation, applied on reload), or an
  **FXAA** post-process threaded into the terrain compositing. A graphics settings group + `GraphicsPrefs` host a
  working toggle.

## UI / onboarding
- [ ] **Schematic base view** (reuse `SHADOW_STYLE`) + more toggleable info overlays (e.g. a movement-penalty
  heatmap) alongside the existing Terrain toggle.
- [ ] **The polished end-state UI.** A cohesive visual-theme + layout pass over the whole HUD/onboarding/menus
  building on the dock: consistent typography, spacing, panels and states; responsive to window size. The "real
  UI" we want to ship behind.
- [ ] **Location list import / export.** Let the player export the current location catalogue (`Locations` /
  `resources/locations.json`) to a file and import a custom one, so curated place sets can be shared without a
  rebuild. Builds on the externalized JSON catalogue + pure parser (`Locations.parse`).
- [ ] **Real per-stage load %.** The single-async-wait stages (map/street/shadow/grid) **creep** an animated
  percentage rather than report true progress (only the portal/people spawn phase is a real `done/total`). Wire
  real signal where the APIs allow — esp. **flow-field computation** (per-portal field-build counts) and MapLibre
  tile-load events.

## Gameplay mechanics (standalone)
- [ ] **Portal-mod follow-ups** — **activate link amps** (range/outbound/SBUL); the **Ultra-Strike** weapon +
  targeted mod-stripping honouring shield `stickiness`; a **3D key** model; a per-game **drop-rate tuning UI**
  (`DropRates` is centralized — Menu → Drop rates; `docs/MECHANICS.md`). When drop rates become player-tunable,
  fold them into the training balance lock (`MatchSetup.useDefaultBalance`).

> Skill-based mechanics that need the per-agent/colony layer first — **glyph hacking**, the **aim skill**,
> **recruiting items** — live in [docs/FUTURE.md](docs/FUTURE.md) under *Roster management*.

## Toolchain
- [ ] **JDK 25 · detekt 2.x · Gradle 10 (blocked — revisit when detekt 2.0 ships).** The build runs on JDK 21
  because detekt 1.23.8 crashes on JDK 25 (its bundled Kotlin compiler can't parse the "25.0.x" runtime) — see the
  header note in `build.gradle.kts`. detekt 2.x is what unlocks JDK 25, but only **1.23.8** is published today (no
  2.0-alpha on Maven Central / the Gradle Plugin Portal), and **Gradle 10 isn't released** (latest 9.6.1, which
  we're on). Once detekt 2.0 publishes a real artifact, do JDK 25 + detekt 2.x (+ Gradle 10 if out) as one
  coherent upgrade and drop the JDK-21 pin.

## Constraints / agreements
- Working directly on `main` for now; **no pushing** until something works end-to-end.
- Keep `CLAUDE.md` + this file + `docs/` current as work lands; no overlapping info (future far-horizon →
  `docs/FUTURE.md`, concrete next → here, shipped → FEATURES, how → ARCHITECTURE).
- Desktop-only; do not invest in mobile support.
