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

### Phase 4 — Free navigation (camera-follow) + UI polish  ✅ (verified in headless Chrome)
Approach: keep the **grid/sim fixed** (built once at an anchor view) and make the *camera*
free — the canvas layer is CSS-transformed to track the MapLibre camera. This sidesteps the
vector-field-recompute and off-screen-portal problems (panning needs zero recompute; the
existing off-screen border handles invisible portals). A *growing* world is deferred (see
icebox — needs on-demand pathfinding).
- [x] **Free controls** (`util/Navigation.kt`): wheel = zoom, right-button drag = pan, WASD
      = pan, driven on `uiCan`; left-click stays portal placement. Relaxed the map min/max
      zoom (was locked at 18) to a free range.
- [x] **Camera-follow transform**: `MapUtil` captures the anchor center/zoom and exposes
      `cameraTransform()` = `translate(screenC0 - center0·s) scale(s)`,
      `s = 2^(zoom − anchorZoom)`; `#canvasLayer` (the canvas container) gets this CSS
      transform on every map `move`. Verified exact (scale = 2^Δzoom, zoom-to-center, pan =
      drag delta).
- [x] **Part B** — base-map **layer dropdown** (Satellite/Street) replacing the satellite
      checkbox.
- [x] **Part C** — **volume slider** + master gain replacing the sound checkbox; audio
      resumes on first user gesture.
- **Exit criterion:** ✅ user can wheel-zoom / RMB-pan / WASD and the sim stays glued to the
  map; verified in headless Chrome with 0 console errors. _(Sim remains anchored to its
  build area — a growing world is the deferred follow-up.)_

### Phase 5 — Game balance / make it interesting  ✅ (first pass; tune via playtest)
Root cause: recruiting was **free** (no resource cost), making roster size a strictly
positive throughput multiplier — recruit-rush = free head start. Fix:
- [x] Recruiting now **costs XM** (`Config.recruitmentXmCost`, = link cost) and is gated on
      having it, so growing the roster competes with linking/deploying for the same energy.
- [x] **Diminishing returns**: success chance (`Config.recruitmentBaseChance`) scales →0 as
      the faction fills toward its cap, so rushing the cap pays less and less.
- [x] Fixed a latent **ConcurrentModificationException** (recruiting mutated `allAgents`
      mid-tick): recruits buffer in `World.pendingAgents`, flushed after the agent loop; the
      tick iterates a snapshot.
- Metrics: the per-faction **MU cycle graph** (Cycle) + agent-count displays already show
  who's winning — use them to judge balance.
- **Exit criterion (tunable):** recruit-rush is no longer free; the balance constants live
  in `Config` for playtest tuning. _Deeper "no strategy dominates" validation is iterative
  (playtest via ./start.sh, or a future headless strategy-comparison harness)._

### 3D rework + pathfinding scalability (pre-AI) — see `.claude/plans/` for the full plan
Pivot rendering to a **three.js scene as a MapLibre custom layer** (real 3D, pitch/rotate)
while the **simulation stays 2D**. Staged.
- [x] **Stage 1 — 3D foundation** (verified in headless Chrome): three.js (r160 UMD) custom
      layer camera-synced via the map matrix (`system/display/Scene3D.kt`, `external/Three.kt`);
      entities render in 3D — portals as pole+sphere, agents as faction spheres with a
      camera-facing action-indicator billboard, NPCs as head-sized spheres at head height,
      links as lines, fields as translucent triangles. `DrawUtil.redraw` now clears the
      (transparent) world canvas + `Scene3D.sync()`; HUD stays 2D & screen-fixed. Free 3D nav
      (wheel zoom, RMB pan, WASD, Q/E rotate, R/F pitch) via `Navigation`; the Phase-4 CSS
      camera-follow is retired (the 3D follows the map natively).
- [ ] **Stage 2 — Ingress fidelity**: resonators ringed at the portal base; pole height ←
      level (L8 ≈ 100 m); raised fields; health/level visuals. **Shield visualization** once
      `Portal.deployMods` actually stores mods (today it's a stub — no deployed-shield state to
      draw). Effects: glass **shatter** on portal destruction + 3D **XMP explosion** animations.
      (Portals already use a translucent glass material.)
- [ ] **Stage 3 — Pathfinding scalability**: drop the per-portal full-map flow field;
      multi-mode nav (flow fields near, cheap nav far); NPCs ambient; continuous toggleable
      field viz.
- [ ] **Stage 4 — later**: humanoid glTF models (+ the colony-management attributes above).

### UI rework (a better UI) — see `.claude/plans/` for the full master plan
- [x] **UI Stage 1** (verified): **portal/agent selection** (click → `map.unproject` → sim Pos
      → nearest portal; pitch-safe) + a DOM **inspector panel** (`util/ui/Inspector.kt`); an
      on-screen **controls legend** + **desktop-only gate** (`util/ui/Controls.kt`).
- [ ] **UI Stage 2** — map views & info layers: Satellite (default) + **Schematic** base
      (reuse `SHADOW_STYLE`), and independent toggleable overlays (movement-penalty heatmap,
      vector field, …).
- [ ] **UI Stage 3 — stats: canvas → DOM + dynamic graphs.** Move *all* the info currently
      drawn to the 2D canvas info layer (MindUnits, StatsDisplay, TickDisplay, Com log,
      TopAgentsDisplay, the CycleDisplay graph) into themeable **DOM** panels, reusing the same
      faction colours, symbols/icons and overall design. Add a **charting library** for dynamic
      graphs — stats is an area we'll keep improving, so treat this as a **graph-migration +
      stats-improvement phase** (richer time-series, more metrics) rather than a one-shot port.
      Retire the canvas HUD + `ActionLimitsDisplay`.
- [ ] **UI Stage 4** — tuning-slider panel redesign (both factions, presets) for the AI phase.
- [ ] **UI Stage 5** — visual theme + responsiveness.

### Phase 6 — AI-vs-AI (approach TBD — needs its own plan)
**Polish the game/sim first** (Phases 4–5) before adding AI. The AI substrate is the
per-faction slider vector (0..1 each) as the output; the input is the game state. The
*how* is an open design question that deserves a dedicated mini-plan before coding:
- **Option A — custom small neural net.** A purpose-built network whose output layer *is*
  the sliders, trained/tuned for this game. Bonus: visualize the layers/activations and the
  agents' chosen paths. Smallest, most specific, most "Q-gress"; most control. Feasibility
  of in-browser training/inference + viz TBD.
- **Option B — transformers.js** (in-browser, WebGPU) running a small model.
- **Option C — gemma.js / MediaPipe LLM Inference** (the original idea): prompt a Gemma per
  faction with the world state → slider vector.
Cross-cutting regardless of option: block mobile (desktop-only notice); expose sliders as a
programmatic API (state → slider vector) instead of only DOM inputs; support a different
tuning per faction. **Decision deferred** — revisit with a focused plan once the sim is
polished. Leaning toward the custom NN (A) for specificity + visualization, but unproven.

### Phase 7 — Init / onboarding selections (later; own/compacted session)
A richer start-up flow before the sim runs. Mostly deferred to a fresh session.
- **Location selection** (extends Phase 3): (1) **Home / nearest city** — requires the user to
  share location (Geolocation API) → center there; (2) a **curated list of preselected places**
  (still to be authored); (3) **Random** — same pool as (2), chosen at random.
- **Map size**: **[Small] [Normal] [Big]** — sets the play-area `Sim.SCALE` (grid/area). Bigger =
  longer load (grid readback + per-portal flow fields). **Rework the initial progress bar**:
  today a Normal/Big load can take ~2 min but the countdown graph only starts ~30 s in, so it
  looks *stuck*. Show real "loading / preparing" progress from the first frame (staged: map tiles
  → shadow readback → grid build → flow fields), and a **warning when Big is selected** (slow).
- **Faction selection**: already exists (ENL/RES).
- **Initial roster** (later): optionally "roll" a few starting individuals — ties into the
  rarity-tiered agents in the icebox; **light flavour, not a gacha/gambling loop**.
- **Dev tooling**: a `?debug` URL param enabling **timing measurements + console logging**
  (instrument load stages + tick cost) to profile and optimise the long loads.

## Under consideration (icebox)

- **Colony-management / roster / gacha direction (gameplay expansion).** Lean the sim toward
  a management game:
  - **Per-entity attributes**: endurance, speed, agility, max radius, … so each agent/NPC has
    strengths/weaknesses (builds on `agent/Skills.kt` and `agent/AgentSize.kt`).
  - **Rarity-tiered agents (NOT a gambling minigame)**: agents/recruits have rarity tiers
    (common/rare/legendary…) with randomised attributes — but deliberately **no gambling UX**:
    no "choose 1 of 3", no pull-the-lever decisions (bad game design). Rarity + randomness drive
    sim/AI roster composition that the player *manages*, not a gacha loop the player gambles in.
    (The init "roster roll" — see the onboarding phase — stays light flavour, not a gacha loop.)
    Implement later.
  - **Items**: skateboards, power-banks, second phones, … affecting movement/energy/capacity
    (extends the existing `items/` + `agent/Inventory.kt`).
  - **Battery/accu %**: a key player state — when a player's phone energy is depleted they
    **leave the scene**. New resource distinct from in-game XM.
  - Cheating isn't a concern (client-only JS), so logic can live client-side.
  - Pairs with the 3D humanoid work (Scene3D Stage 4): people are already head-sized spheres
    at head height, ready for bodies + visible gear/tier.

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
