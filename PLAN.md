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

## Decisions

1. **Stack direction — DECIDED: modernize Kotlin/JS in place.** Migrate to the current
   `kotlin("js")` plugin (Kotlin 2.x, Gradle 8, IR backend) and keep all game logic +
   tests. A full TS rewrite was rejected (throws away 7,200 lines of working, tested code
   for no gameplay gain). Plain `kotlin/js`, not KMP — no non-browser targets planned.
   Gemma (WebGPU/MediaPipe) interops fine from Kotlin/JS via `external` declarations.
2. **Map provider — DECIDED: switch to MapLibre GL JS.** Open-source, no token/billing.
   Covers the three needs (street raster for the pixel grid, satellite, 3D buildings). We
   replace the Mapbox GL include + custom Mapbox style URLs with MapLibre + an open style
   (e.g. a free street style for the shadow/grid map). `external/MapBox.kt` becomes a
   MapLibre declaration.

3. **Build JVM — DECIDED (settled): run Gradle on JDK 21 (LTS), not JDK 25.** detekt's
   latest release (1.23.8) crashes inside a JDK 25 process, and detekt is our complexity
   gate. Since the project targets Kotlin/JS (no JVM bytecode shipped), the JDK running the
   build is a pure tooling detail and JDK 21 costs the product nothing. Kotlin stays 2.4,
   Gradle 9.5. Not revisiting unless a concrete need arises.

### Still open
- **Bundler.** Lean on the Kotlin/JS Gradle plugin's built-in webpack/dev-server first;
  only reach for Vite if that proves limiting. Either way, stop committing `published/*.js`.
- **Coverage tooling.** Kover does **not** support Kotlin/JS (JVM/Android only). Real
  line-coverage therefore arrives with the functional-core split: extract pure logic into a
  `commonMain` source set + a `jvm()` test target, and run Kover there. Until then the
  enforced gates are ktlint (format) + detekt (lint/complexity) + tests.

## Roadmap

### Phase 0 — Repo hygiene & docs ✅ (this commit)
- Clone, assess, write `CLAUDE.md` + `PLAN.md`, create `develop` branch.

### Phase 1 — Get it building & running on a modern toolchain
- [x] Migrate Gradle build to `kotlin("multiplatform")` js() IR, **Kotlin 2.4**,
      **Gradle 9.5** (build JVM **JDK 21**; sources moved to `src/jsMain` / `src/jsTest`).
- [x] Replace removed/relocated APIs: `kotlin.browser.*`/`kotlin.dom.*` →
      `kotlinx-browser 0.5.0`; `toUpperCase`/`toLowerCase` → `uppercase`/`lowercase`;
      `Double.toByte()`; literal `js()`; null-safety.
- [x] Get the existing tests compiling and **green in Node** (62/62) via the Kotlin/JS
      Mocha runner. Browser/Karma+Chrome wiring kept (disabled) for future browser tests.
- [x] **Strict dev setup:** ktlint (format) + detekt (lint + complexity limits, baselined)
      wired into Gradle. Kover deferred — no Kotlin/JS support (see Still open).
- [x] **Enforced git hooks** (`core.hooksPath` → `.githooks/`): pre-commit runs
      ktlint + detekt and blocks on failure (verified). Mirror in CI later.
- [ ] Stand up the dev server / bundling wiring into `index.html`; stop committing
      `published/*.js`. _(Browser run/dist tasks exist; index.html rewire lands with Phase 2.)_
- **Exit criterion:** ✅ sim builds and tests green on a modern toolchain; a commit that
  breaks format/lint/complexity is rejected by the hook. _(App-in-browser rewire → Phase 2.)_

### Phase 2 — Fix maps & the zoom bug  ✅ (verified in headless Chrome)
- [x] Removed the dead rawgit OpenLayers include (it was unused in Kotlin).
- [x] Switched Mapbox GL → **MapLibre GL 5.24** (`external/MapLibre.kt`); dropped the
      access token. Open, keyless tiles: OpenFreeMap (street + vector) + Esri imagery.
- [x] Authored open styles: positron street backdrop, satellite (Esri + openmaptiles for
      3D buildings), and the **black-bg/white-streets shadow mask** for the passability grid.
- [x] Fixed the grid/zoom reliability bug: `preserveDrawingBuffer:true`, wait for `idle`
      (not `load`) before `readPixels`, and select the shadow canvas by container query
      (not a global index). Tuned road width so streets survive the 10× grid downscale as
      cells passable-in-all-directions.
- [x] Fixed latent migration crashes: the `[0,0]` default-center sentinel (→ pin to a
      covered default location) and `max()/min()/maxBy/minBy` throwing on empty.
- [x] Moved web assets into `src/jsMain/resources`; the webpack bundle now drives a served
      `index.html` (`start.sh` builds + serves + opens it). Retired `published/*.js`.
- **Exit criterion:** ✅ map renders, grid builds reliably, sim runs end-to-end (22 sliders,
  agents/portals spawn, 0 console errors) — confirmed via headless-Chrome verification.

### Phase 3 — Universal locations ("play your hometown")  ✅ (verified in headless Chrome)
- [x] Free-form search box that geocodes any place/address via keyless **Nominatim/OSM**
      → lng/lat, alongside the preset dropdown.
- [x] Recenters through the `?lng=&lat=&name=` URL flow; the dropdown shows the searched
      location's name (not "Unknown Location").
- [x] Fixed `createNewUrl` to build off the current origin/path (was a hard-coded
      `localhost:63342`), so recentering works on any host — this also unbroke the preset
      dropdown's navigation.
- **Exit criterion:** ✅ any city/address playable — verified by searching "Brandenburg
      Gate, Berlin" and confirming the sim recenters there and builds its grid.

### Phase 4 — Free navigation & decoupling the sim from the 2D screen grid
The headline goal: **free map controls** — mouse-scroll zoom, drag / right-button pan, and
WASD — i.e. drop the fixed top-down zoom-18 restriction entirely. This is *not* a toggle:
today portals/agents live in screen pixels and the passability grid is baked once from the
rendered view, so any pan/zoom desyncs everything. The fix is to **decouple the simulation
from the rendered view**:
- [ ] **Geo-anchor entities.** Store portals/agents/links in geographic coordinates
      (lng/lat), and project to screen space each frame via the map. Pan/zoom then just
      reprojects; entities stay glued to the world.
- [ ] **Replace the pixel-readback grid.** Derive walkability/penalties from the **vector
      tile road geometry** (query rendered features / GeoJSON) or a street graph/navmesh,
      instead of reading rasterized shadow-map pixels. Removes the screen-space coupling and
      the fixed-zoom assumption (also unblocks "going 3D"). Folds in the icebox pathfinding
      rework.
- [ ] **Free controls.** Let map gestures through (the overlay canvas currently swallows
      them): scroll-zoom, drag / RMB pan, optional WASD. Rebuild/refresh the nav data on
      `moveend`/`zoomend`; derive scale (`pixelToMFactor`, ranges) from the live zoom.
- **Exit criterion:** the user can freely zoom/pan (and WASD) and the simulation stays
  consistent and glued to the real world.

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

## Under consideration (icebox)

- **Null-safety hardening.** The Kotlin 1.3→2.x migration silently changed
  `max()`/`min()`/`maxBy`/`minBy` from returning `null` on empty to *throwing* — these
  compiled with only warnings but crashed at runtime (hit during Phase 2: empty wavefront
  layer → `NoSuchElementException`). Fixed the known sites by switching to the `…OrNull`
  variants. Follow-up: **audit every `!!` in the project** and replace with safer constructs
  (`?.`, `?:`, `requireNotNull(x) { "msg" }`, `let`, early return). `!!` is the same class
  of latent NPE/`NoSuchElement` hazard; the codebase leans on it heavily.

- **Rework the movement / pathfinding model.** Today: a "shadow" map is rendered as a
  black-background / white-streets mask, read back via WebGL `readPixels`, and turned into a
  per-cell penalty grid; agents are circles that flow along vector fields, preferring
  streets because off-street cells carry a higher penalty (the "ants" look). Phase 2
  preserves this exactly, just sourcing the mask from MapLibre + OpenFreeMap vector tiles
  instead of a custom Mapbox style. **But** the screen-pixel → grid coupling is fragile and
  ties the simulation to a fixed top-down 2D view (also the blocker for dynamic zoom and
  "going 3D"). Worth replacing with something better: derive walkability/penalties directly
  from the **vector tile road geometry** (query rendered features / GeoJSON) rather than
  reading rasterized pixels, and/or a proper graph/navmesh over the street network. This
  decouples the sim from the render and is the natural partner of the functional-core split.

- **Going 3D.** MapLibre supports map pitch, 3D terrain, and prominent extruded
  buildings. Worth exploring for visual impact. **Caveat:** the gameplay grid is built by
  reading the *top-down* rendered map pixels and mapping screen → flat game cells; a pitched
  / 3D camera breaks that linear mapping. So "going 3D" is not a toggle — it needs either a
  decoupled simulation grid (game logic stops depending on screen-space pixels) or a 3D
  pathfinding model. Revisit after the functional-core split, which is the natural place to
  separate the simulation space from the render space. (3D *buildings* in the top-down
  satellite view already work and stay.)

## Constraints / agreements
- Commit to `develop`; **no pushing** until something works end-to-end.
- Keep `CLAUDE.md` and this file current as decisions land.
- Desktop-only; do not invest in mobile support.
