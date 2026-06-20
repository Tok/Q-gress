# PLAN.md — Q-Gress Modernization

Status: **draft** · Branch: `develop` · Owner: @zirteq

This is the working roadmap for bringing Q-Gress back to life and toward the AI-vs-AI
vision. Sequenced so each phase produces something runnable before we move on.

## Vision recap

Q-Gress becomes an **AI-vs-AI sandbox**: ENL and RES are each driven by a **client-side
Gemma** model (in-browser, WebGPU) that tunes the faction behaviour sliders (0.0–1.0
values). Gemma-vs-Gemma, each side possibly tuned differently. **Desktop-only**; mobile is
blocked. Until the LLM layer lands, the existing slider sim is the substrate we harden.

## Current-state assessment (2026-06-20)

The codebase is solid in design (clean Kotlin, ~7,200 LOC, real tests) but the **toolchain
and map deps are dead**:

| Area | State | Problem |
|---|---|---|
| Kotlin | 1.3.10 (2018) | 2 majors behind; `kotlin2js` Gradle plugin is removed in modern Kotlin |
| Gradle plugins | `kotlin2js`, `com.moowork.node 1.2.0` | both deprecated/abandoned |
| OpenLayers | v5.3.0 via `cdn.rawgit.com` | **CDN shut down in 2019 → broken load** |
| Mapbox GL JS | v0.51.0 (2018) | ~3 majors behind; v2+ changed license (token + billing) |
| Zoom | hard-coded to **18** everywhere | grid + geometry calibrated to z18; "inconsistent zoom" bug; blocks dynamic zoom |
| Locations | fixed `enum` of 11 | no "play your hometown" / arbitrary coordinates |
| Balance | recruit-rush dominates | set recruit=1.0, rest=0 → cap out agents → snowball wins |
| Build artifacts | `published/*.js` committed | want a real build/bundle step instead |

### Why recruit-rush wins (balance root cause)

`ActionSelector` picks actions by weighted-random over `slider × weight`. Recruiting adds
new agents to your faction up to a cap (`Config.maxFrogs/maxSmurfs = 21`). More agents =
more actions per tick = a compounding snowball. Rushing recruitment to the cap first, then
switching, beats any balanced strategy because every other action benefits from having more
actors. Fixing this means giving recruitment a **cost / diminishing return / upkeep**, or
making field-building yield score fast enough to compete with raw headcount.

## Engineering standards (cross-cutting, non-negotiable)

Quality and testability come first, from day one — applied to all new/changed code and
retrofitted as we touch legacy code.

- **Functional core, imperative shell.** Game logic lives in **pure functions** (given
  state → return new state/decision, no I/O, no mutation of globals). Side effects —
  canvas/DOM rendering, Mapbox/WebGL, audio, RNG, `World` mutation, timers — are pushed to
  a thin outer shell. Today logic and effects are entangled (e.g. `ActionSelector` reads
  DOM sliders directly, agents mutate `World`); we refactor toward
  `pureDecision(state) → effect` so the core is testable without a browser.
- **Determinism for tests.** RNG must be injectable (seedable) rather than calling
  `Math.random()` inline, so behaviour can be unit-tested deterministically.
- **High coverage.** Unit tests for every pure function; aim high (target ≥80% lines on the
  functional core, with balance/selection logic near 100%). Coverage is measured and
  reported, not vibes.
- **Enforced on commit.** A pre-commit hook (and matching CI) blocks commits that fail:
  - **formatting** (ktlint / `kotlinFormat`),
  - **linting / static analysis** (detekt),
  - **cyclomatic complexity limits** (detekt `ComplexMethod`/`LongMethod` thresholds),
  - **tests + coverage threshold** (kotlinx-kover or equivalent; commit fails below floor).
  Hooks live in-repo (e.g. a `.githooks/` dir wired via `core.hooksPath`, or a Gradle
  `installGitHooks` task) so the rules travel with the repo, not just one machine.
- **Small, reviewable commits** on `develop`, each green.

## Open decisions (resolve before Phase 1 work)

1. **Stack direction.** Recommendation: **modernize Kotlin/JS in place** — migrate to the
   current `kotlin("js")` plugin (Kotlin 2.x, Gradle 8, IR backend) and keep all game
   logic. A full TypeScript rewrite throws away 7,200 lines of working, tested code for no
   gameplay gain. (See question posed to user.)
2. **Map provider.** Recommendation: evaluate **MapLibre GL JS** (open-source fork of
   Mapbox GL, no token/billing) vs staying on **Mapbox GL JS v3** (needs account + usage
   billing). The game only needs: a styled street raster to read pixels from, a satellite
   layer, and 3D buildings. MapLibre can do all three. Decision affects the pixel-grid
   styling.
3. **Bundler.** Likely Vite (fast, simple) to replace committed `published/*.js`, or lean
   on the Kotlin/JS Gradle plugin's webpack integration. Lean toward whatever the Kotlin
   plugin gives us for free first.

## Roadmap

### Phase 0 — Repo hygiene & docs ✅ (this commit)
- Clone, assess, write `CLAUDE.md` + `PLAN.md`, create `develop` branch.

### Phase 1 — Get it building & running on a modern toolchain
- [ ] Migrate Gradle build to `org.jetbrains.kotlin.js` (IR), Kotlin 2.x, Gradle 8.
- [ ] Replace deprecated `kotlin.browser.*` / `kotlin.dom.*` APIs with `kotlinx-browser`
      / current equivalents (these moved out of stdlib after 1.3).
- [ ] Stand up a dev server + bundling; stop committing `published/*.js` (gitignore them).
- [ ] Get the existing tests compiling and green again on the Kotlin/JS test runner.
- [ ] **Stand up the strict dev setup** (see Engineering standards): ktlint + detekt
      (with complexity limits) + kover coverage, all wired into Gradle.
- [ ] **Install enforced git hooks** (`core.hooksPath` → in-repo `.githooks/`): pre-commit
      runs format-check + detekt + tests/coverage and blocks on failure. Mirror in CI.
- **Exit criterion:** sim runs as it did in 2018, and a commit that breaks
  format/lint/complexity/coverage is rejected by the hook.

### Phase 2 — Fix maps & the zoom bug
- [ ] Remove the dead rawgit OpenLayers include; decide if OpenLayers is still needed.
- [ ] Upgrade Mapbox GL → v3 **or** switch to MapLibre GL (per decision #2). Update
      `MapUtil` map init, style URLs, and the `external/MapBox.kt` declarations.
- [ ] Fix inconsistent zoom: make the grid build deterministic w.r.t. the *actual* rendered
      zoom (wait for `idle`/`load` reliably before `readPixels`; today it indexes
      `mapboxgl-canvas`[2] by position, which is fragile).
- **Exit criterion:** map renders, grid builds reliably every load at a known zoom.

### Phase 3 — Universal locations ("play your hometown")
- [ ] Add a free-form location input (search box) with **geocoding** (Nominatim/Mapbox
      Geocoding) → lng/lat, alongside the existing presets.
- [ ] Persist arbitrary coordinates through the existing `?lng=&lat=&name=` URL flow.
- **Exit criterion:** any city/address playable, not just the dropdown.

### Phase 4 — Scale & dynamic zoom
- [ ] Decouple game geometry from the fixed 1200×800 @ z18 assumption: derive
      `pixelToMFactor`, portal/range sizes, and grid resolution from the current zoom.
- [ ] Support a configurable zoom and then **dynamic zoom** (rebuild/rescale grid + vector
      fields on zoom change without breaking in-flight agents).
- **Exit criterion:** zoom level is a parameter; changing it keeps the sim consistent.

### Phase 5 — Game balance / make it interesting
- [ ] Neutralize the recruit-rush: add recruitment cost/upkeep or diminishing returns;
      retune `QActions`/`QDestinations` weights and `Config` caps.
- [ ] Add metrics so we can compare strategies (already have MU checkpoints/cycles).
- **Exit criterion:** no single slider-maxing strategy dominates; field-building competes.

### Phase 6 — AI-vs-AI with client-side Gemma
- [ ] Block mobile (UA + capability check) with a "desktop only" notice.
- [ ] Expose the per-faction sliders as a programmatic API (read current game state →
      proposed slider vector) instead of only DOM inputs.
- [ ] Integrate in-browser Gemma (MediaPipe LLM Inference / WebGPU). Two instances (ENL,
      RES), each given the world state + its faction and asked to set its sliders each
      cycle. Support different tuning/prompts per side.
- **Exit criterion:** a Gemma-vs-Gemma match runs end-to-end in a desktop browser.

## Constraints / agreements
- Commit to `develop`; **no pushing** until something works end-to-end.
- Keep `CLAUDE.md` and this file current as decisions land.
- Desktop-only; do not invest in mobile support.
