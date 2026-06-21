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
- [x] **Stage 1.5 — Standard map navigation** (verified in headless Chrome): retired the
      hand-rolled overlay-canvas nav (which only *panned* on RMB and blocked the map). The
      visible map (`initialMap`) now gets `pointer-events: auto` and the HUD canvas
      `pointer-events: none`, so **MapLibre's own handlers** drive everything — left-drag pan,
      **right-drag rotate + tilt**, wheel zoom, all unrestricted (`maxPitch` 85; top-down at
      pitch 0). Added a **NavigationControl** block (zoom + compass + pitch visualiser,
      bottom-right). Portal select/build + build-marker hover moved to the map's own
      `click`/`mousemove` events (`MapUtil.bindInteractions`/`eventToSimPos`); `Navigation`
      now only layers optional WASD/Q-E/R-F keys. Removed dead `screenToSimPos`/`zoomBy`.
      _Follow-up DONE (`90e82f1`):_ **mini-globe inset** (`util/ui/MiniMap`) — a circular second
      MapLibre map (globe projection) on the mid-left edge, synced to the main camera so the played
      location stays centred under a static pin; **FLAT/GLOBE** button toggles the projection
      (`MapLibre.setProjection`). `interactive:false` (passive overview); game-only. Mounts the map
      in an inner div so MapLibre's own `maplibregl-map{position:relative}` doesn't override the
      wrapper's fixed positioning.
- [ ] **Stage 2 — Ingress fidelity**: resonators ringed at the portal base; pole height ←
      level (L8 ≈ 100 m); raised fields; health/level visuals. **Shield visualization** once
      `Portal.deployMods` actually stores mods (today it's a stub — no deployed-shield state to
      draw). Effects: glass **shatter** on portal destruction + 3D **XMP explosion** animations.
      (Portals already use a translucent glass material.)
- [ ] **Stage 3 — Pathfinding scalability**: drop the per-portal full-map flow field;
      multi-mode nav (flow fields near, cheap nav far); NPCs ambient; continuous toggleable
      field viz.
- [ ] **Stage 4 — later**: humanoid glTF models (+ the colony-management attributes above).

#### Portal / link / field visual overhaul — detailed plan (2026-06-21, from user direction)

The throughline is **abstract glass**: portals, the tubes between them, and the shards are all
one "glass apparatus" look (à la qlippostasis), tinted by faction (the only allowed colour;
the vessel itself stays grayscale).

> **Status — end of 2026-06-21 session (resume here).** This overhaul is **essentially complete**
> and committed on `develop` (gate-green; last commit `1de3946`). Done: glass shader · metal-pole +
> rubber-gasket + glass-orb portals (φ-scaled, **grow-on-spawn** + **level-up tween**) · gray demo
> with satellite/buildings toggle · physics shatter (randomized, scaled, low-energy) · **volumetric
> raymarched XMP fireball** + click-to-detonate (`/#demo/xmp`) + explosion sound · **glass-pipe
> links** · **plasma fields** (fill-in + dissolve + collapse sound) · **real portal names** from map
> POI/street data. New files this push: `GlassShader`, `PlasmaShader`, `XmpShaders`, `XmpBurst`,
> `FieldFx`, `Spawns`, `Materials`, `ShardAssets`, `PortalShape`(unused/removed), `util/PortalNames`.
> **What's left** = the small follow-ups below + separate features (mini-map/globe, `?debug`,
> Phase 7 onboarding) + the Blender GLB step. Nothing is mid-flight; pick any item to start.
> Verify visually with `./start.sh` (game + `/#demo/portal` + `/#demo/xmp`).

- [x] **Glass shader (foundation)** — `system/display/GlassShader.kt` ShaderMaterial ported from
  qlippostasis (Fresnel rim + emission + vnoise smudges, DoubleSide, no depth write, per-instance
  faction `tint`). View direction approximated from the world normal's verticality (no opaque-pass
  texture in the custom layer); alpha/emission pushed well above Godot's so it reads over the map.
  Double-shell + true SSR still a follow-up.
- [x] **Portal = metal pole + rubber gasket + glass orb** (the mushroom was tried and **reverted**
  per user — too much). A faction-tinted **metallic** pole, a small matte-black **rubber gasket**
  torus at the junction so metal never touches glass, and a round **glass orb** on top whose size
  scales with portal **level** (L1 small → L8 large). The **shatter reuses the flask shards**,
  scaled to the orb's level (the pole is metal, so only the orb shatters). Verified L1/L8 in demo.
- [x] **`#demo/portal` scene** (L1–L8 orb size + faction tint) — plus demos now render on a **gray
  backdrop** (satellite hidden, checkbox to toggle) and sit **zoomed in** (`DEMO_ZOOM`).
- [x] **Glass shader follow-ups — done** (true SSR refraction is the only deferred piece, and it's
  **out of scope**: the MapLibre custom layer doesn't expose the opaque pass as a texture, which SSR
  needs). Shipped: **double-shell thick-wall** orbs (`6bcc9b6`) — a concentric inner glass shell
  (`INNER_SHELL_FRAC`, child of the orb so it inherits the level scale + grow-in) gives an outer +
  inner rim, reading as a thick blown-glass vessel; **camera-tracking rim** + **brighter link variant
  + plasma-core tube** (`c92f176`): `GlassShader.updateEye` recovers the camera eye in sim-space (null
  vector of the sim→clip matrix's x/y/w rows — MapLibre 5.24 has no `getFreeCameraOptions`) feeding a
  shared `uEye` uniform so the fresnel uses the true per-fragment view dir; links use a brighter glass
  variant (`uBright`/`Materials.linkGlass`) wrapping an additive `Materials.linkCore` filament
  (`coreGeo`). Shatter shards now also share the orb glass shader (`eb2169f`, with a `uFade` fade-out).
  (Grow-on-spawn + level-up animation also **done** — see the lifecycle bullet.)
- [ ] **GLB compaction + shard reuse (needs Blender).** Decimate `shattered_flask.glb` /
  `glass_shards.glb` (fewer pieces, lower poly) for our scale; reuse shard **panels** to build the
  open umbrella cap at high levels (cap reads as fitted glass shards). Glass look stays
  shader-driven (no bake). *This is the one piece that genuinely needs Blender.* **Commit the
  raw `.blend` source files under `/assets/blender/`** (not just the exported `.glb` in `models/`).
- [x] **Links → 3D glass pipes.** The 2D `Line` links are now thin **glass cylinders**
  (`linkGeo` + `orientTube`: a unit Y-cylinder placed at the midpoint, Y-scaled to length, Y
  rotated to the direction via `Quaternion.setFromUnitVectors`) spanning the two portals' **orb
  centres** (`orbCenterZ`), on the shared `GlassShader` (faction-tinted). _Done (`c92f176`):_ a
  **brighter link variant** (`Materials.linkGlass`) + an **additive plasma-core** inner tube
  (`Materials.linkCore`/`coreGeo`), so pipes read strongly despite the near-transparent orb glass.
- [x] **Fields → plasma (visual).** Control fields are now an animated **plasma `ShaderMaterial`**
  (`PlasmaShader`): a faction-tinted energy sheet rippling via summed sine waves, with one shared
  `uTime` advanced each frame from `Scene3D.render`. Triangles now sit at the three portals' orb
  centres (`orbCenterZ`), not a flat height. Replaced the flat `MeshBasicMaterial`.
- [x] **Lifecycle animation layer + field/portal spawn/teardown.** Built the persistent per-entity
  animation registry the sync rebuild lacked: `Spawns` (first-seen time per stable id, survives the
  rebuild, reports vanished ids). On it: **portal orb grows in** on spawn (easeOutBack), **fields
  fill in** from their centroid, and **fields dissolve** on destruction (`FieldFx` transient shrink)
  with a **collapse sound** (`SoundUtil.playFieldDownSound`). Field-up + portal-create sounds
  already existed. Portal **level-up** now tweens too: the rendered level eases toward the real one
  each sync (`tweenedLevel`/`displayedLevel`), so the pole height + orb size grow smoothly when a
  portal gains a level. _Remaining:_ a richer field-up "whoosh".
- [x] **Portal names from map data (replace the random gibberish).** `util/PortalNames`: at grid
  time it `querySourceFeatures`-queries the shadow map's `openmaptiles` source for the `poi` and
  `transportation_name` source-layers (works even though the shadow style doesn't render them).
  `Portal.create` takes the nearest named POI (≤90 px), else nearest street (≤140 px), else the
  generator. Spike confirmed the data on OpenFreeMap: **~1100 POIs + ~300 streets** at zoom 18 for
  the default location. Lng/lat → sim `Pos` via `Scene3D.lngLatToSimPos`, projected lazily (Scene3D
  must be anchored first); all defensive (any failure → generator). _Follow-ups:_ a portal sitting
  right on a POI could adopt its `class` (fountain/monument…); gate the diagnostic log behind
  `?debug`.

**Blender needed?** Only for GLB compaction + authoring shard panels. The glass shader, the
portals, the pipes, the plasma fields, and the naming are all procedural/data — no Blender. (When
the Blender step happens, commit the raw `.blend` under `/assets/blender/`, `.glb` in `models/`.)

### UI rework (a better UI) — see `.claude/plans/` for the full master plan
- [x] **UI Stage 1** (verified): **portal/agent selection** (click → `map.unproject` → sim Pos
      → nearest portal; pitch-safe) + a DOM **inspector panel** (`util/ui/Inspector.kt`); an
      on-screen **controls legend** + **desktop-only gate** (`util/ui/Controls.kt`).
- [ ] **UI Stage 2** — map views & info layers: Satellite (default) + **Schematic** base
      (reuse `SHADOW_STYLE`), and independent toggleable overlays (movement-penalty heatmap,
      vector field, …).
- [x] **UI Stage 3 — stats: canvas → DOM + dynamic graphs — DONE.** The whole canvas HUD is now
      themeable **DOM**: `StatsPanel` (MindUnits + entity counts + tick + Com, `8b35296`), the MU
      **time-series graph** via **uPlot** (`CycleChart`, `b51fd02`; uPlot added by CDN like MapLibre,
      `external/UPlot.kt`), and the **TopAgents leaderboard table** (`TopAgentsPanel`, `fb7f626`,
      per-level inventory bars in grayscale). Retired the canvas `MindUnits`/`StatsDisplay`/
      `TickDisplay`/`Com.draw`/`CycleDisplay`/`TopAgentsDisplay`/`UiTable`/`ActionLimitsDisplay`, and
      the loading HUD (`5bd5452`). _Follow-up (stays a stats-improvement phase):_ richer time-series /
      more metrics; the `uiCanvas` is now unused (could drop `clearUserInterface`).
- Map visuals (done, this batch): **grayscale terrain default** + Colored/Street views
      (`util/ui/LayerView`; raster-saturation on the satellite layer only); **white** play-area
      border + **dimmed out-of-bounds** (`system/display/PlayAreaMask`); rule: no new colours,
      faction colours for faction things only.
- [ ] **UI Stage 4** — tuning-slider panel redesign (both factions, presets) for the AI phase.
- [ ] **UI Stage 5** — visual theme + responsiveness.

### Phase 6 — AI-vs-AI (the Q-gress payoff)

**The north star.** Q-Gress is named for the original idea: each faction is driven by an
agent whose **output layer _is_ the behaviour sliders**. The 18 sliders per faction
(11 `QActions` + 7 `QDestinations`, each 0..1) were always meant to be the action vector of
a network, not just human knobs. The endgame: a human can play against an AI, and two AIs
can be matched against each other — **ENL-brain vs RES-brain**, each possibly a different
kind of brain.

**Decision — DECIDED (2026-06-21): build _both_ AI drivers on _one_ shared substrate.**
- **Track A — custom tiny net + neuroevolution** (the "real Q-gress", optimizing + visualizable).
- **Track B — in-browser LLM** (transformers.js / WebGPU, and/or Gemma via MediaPipe — the
  original "Gemma-vs-Gemma" idea; reasons about state → sliders, explains itself in words).
Both speak the same **programmatic slider API** and run on the same **deterministic headless
match harness**, so any faction can be Human / Net / LLM independently and they can fight in
any combination. Those two shared pieces (6.0, 6.1) are the bulk of the work and are built
first; the drivers (6.2, 6.3) then fork cheaply off them.

**Why these mechanisms (recorded so we don't relitigate):**
- **Trainer = neuroevolution / ES, not gradient RL or tabular Q.** The "Q" is heritage
  branding. The env is non-differentiable (no gradients through the sim), the reward is
  **sparse and episodic** (MU resolves over 5-min checkpoints / 30-min cycles), and the
  action space is **18 continuous** values — that combination is poison for tabular
  Q-learning and awkward for online policy-gradient, but ideal for **evolution strategies +
  self-play** (just need a fast headless match and a fitness number; embarrassingly
  parallel). So we keep the name and ship an honest mechanism.
- **The slider vector stays the action substrate.** The net/LLM re-tunes the 18 sliders at a
  **slow cadence** (≈ once per checkpoint, reacting to aggregate world state) — it does **not**
  replace per-agent `ActionSelector` and does **not** run per-tick. The existing engine is
  left intact; we only swap _where the slider values come from_.

#### 6.0 — Substrate I: programmatic policy API + determinism _(prereq, no AI yet)_
The one seam everything plugs into. Today `ActionSelector.q()` reads each slider straight
from the DOM (`getElementById("${id}Slider${Frog|Smurf}").valueAsNumber * weight`). Replace
that with a per-faction **`FactionPolicy`** source:
- [ ] **`FactionPolicy` interface** — `sliderValue(faction, qValue): Double` (or a whole
      `SliderVector`). Default impl `DomSliderPolicy` reads the DOM exactly as today → **zero
      gameplay change**, human play untouched. `ActionSelector.q()` calls the policy, not the DOM.
- [ ] **`Observation` (pure)** — `observe(world, faction): DoubleArray`, a fixed, documented,
      normalized feature vector (MU + Δ-MU, portal/agent/link/field counts per faction, tick
      fraction, avg agent XM/level, unclaimed-portal share, …). The NN/LLM input.
- [ ] **`SliderVector` (pure)** — 18 named slots in a fixed order ↔ `QActions`+`QDestinations`;
      encode/decode + clamp. The NN/LLM output.
- [ ] **Injectable seedable RNG** — replace the global `Math.random()` in `Util.random()` with
      a threaded, seedable `Rng` (this is the determinism standard PLAN already mandates).
      Thread it through `Util.select`, movement, recruiting, cycle events.
- **Exit:** human play is byte-identical to today; a faction can be driven by a swapped policy;
      **same seed → tick-for-tick identical match** (unit test).

#### 6.1 — Substrate II: headless match harness _(the training engine)_
Training needs thousands of fast, reproducible matches with **no DOM / canvas / MapLibre /
WebGL**. This is where the **functional-core / imperative-shell split** finally gets paid off.
- [ ] **`SimRunner` (headless)** — `runMatch(gridFixture, policyEnl, policyRes, seed,
      maxTicks): MatchResult { enlMu, resMu, checkpoints, winner }`. Runs the tick loop with
      rendering/audio/DOM stubbed out at the shell boundary.
- [ ] **Grid fixtures** — serialize a built `Grid` (+ portal seeds) for a handful of locations
      to committed JSON, so matches reproduce without live map tiles or `readPixels`. (Decouples
      the sim from the screen-pixel grid — the icebox "rework movement model" item, partially.)
- [ ] **Speed** — run accelerated in **Node** (tests/CI) and a **Web Worker** (in-tab training)
      so the UI never blocks. Target: hundreds of full matches/min.
- **Exit:** a full match runs headless & deterministic in Node, emitting an MU time-series +
      winner; fast enough to drive a training loop.

#### 6.2 — Track A: custom net + neuroevolution
- [ ] **Tiny MLP** (`ai/net/`) — input = `Observation` (~16), 1–2 hidden layers, output = 18
      sigmoid sliders. Hand-rolled forward pass (no framework; it's small), weights = flat array.
- [ ] **Evolution trainer** — `(μ,λ)`-ES / CMA-ES / GA (mutation + crossover + elitism) over the
      headless harness; **self-play league** (+ a Hall-of-Fame to curb cycling). Fitness = MU
      margin / win-rate vs opponents and vs the default-slider baseline. Runs in a Worker / Node.
- [ ] **Persistence** — serialize the best genome to JSON; `NetPolicy` loads it to drive a live
      faction.
- [ ] **Visualization (the Q-gress payoff)** — render the live net's **layers/activations** and
      the driven faction's **chosen agent paths** while it plays (reuses the 3D/overlay infra).
- **Exit:** a trained `NetPolicy` beats the default-slider baseline over K seeded matches by a
      clear margin, loads into the live game, and its activations are visualized.

#### 6.3 — Track B: in-browser LLM driver
- [ ] **`LlmPolicy`** (same API) driven at **checkpoint cadence**, off the tick loop: build a
      compact world-state prompt → model → **JSON slider vector** (schema-validated, defensive
      fallback to the last/default vector on any parse failure). Sim keeps running on the last
      vector while inference is in flight.
- [ ] **Engine(s)** behind one interface: **transformers.js** (WebGPU small instruct model)
      and/or **Gemma via MediaPipe LLM Inference** (the original idea) — `external` decls like
      MapLibre/uPlot.
- [ ] **Explainability** — surface the model's reasoning text in a "faction AI commentary" panel.
- **Exit:** an LLM drives a faction end-to-end in-browser, re-tuning sliders each checkpoint with
      visible reasoning, sim stays smooth.

#### 6.4 — Mix, match & human-vs-AI
- [ ] **Per-faction driver selection** (onboarding + UI Stage 4 slider panel): **Human / Net /
      LLM** chosen independently for ENL and RES → human-vs-net, net-vs-net, net-vs-LLM,
      LLM-vs-LLM. When a side is AI-driven its sliders animate read-only so you can watch the
      brain tune itself.
- [ ] **Tournament / eval view** — round-robin between saved policies → a leaderboard (reuses the
      headless harness + uPlot).
- **Exit:** pick a driver per faction at start and watch/participate; saved nets compete in a
      tournament view.

**Cross-cutting (all of Phase 6):** desktop-only + **WebGPU** gating (block mobile / unsupported);
seeds surfaced via `?seed=` for reproducible matches; **balance risk** — pure win-maximizing
self-play may rediscover the recruit-rush degenerate (see Phase 5), so keep tuning `Config` and
optionally **shape fitness for _interesting_ play** (lead changes / strategy diversity) as a
follow-up, not v1.

**Still open:** which LLM model (transformers.js choice vs Gemma/MediaPipe), the exact
grid-fixture serialization format, the precise `Observation` schema, and the fitness-shaping
function. Resolve each at the start of its sub-phase, not now.

### Phase 7 — Init / onboarding selections (started)
A richer start-up flow before the sim runs.
- [x] **Ordered onboarding** (`e0caf08`): **faction → location → load** (`startOnboardingOrWorld`
  + `util/ui/Onboarding`). Faction screen (ENL/RES), then location screen (preset dropdown + globe
  preview via `MiniMap`), then the world loads. URL-param driven; `?local=true` auto-starts.
- [x] **Initial progress bar reworked** (`361364c`/`37ae0bd`): `util/ui/LoadingOverlay` shows
  staged progress from the first frame (map tiles → street tiles → passability map → street grid
  → building world), faction-tinted, translucent at the build stage to reveal the spawning world.
- **Location selection** follow-ups: (1) **Home / nearest city** via Geolocation API; (2) a
  **curated list** of preselected places (extend the preset enum); (3) **Random**. Free-form search
  exists in-game but isn't on the onboarding screen yet.
- [x] **Map size + portal density** (`a1f150b`): onboarding step after location — Small/Normal/Large
  presets + editable Width/Height (`Sim.width/height` now runtime `var`, default Normal=1.5×) and an
  editable initial portal count (`Config.startPortals` now `var`), with a slow-build warning. Loads
  in-memory (no reload). Remaining: real per-stage load % (esp. flow fields); Geolocation/random.
- [x] **Loading unified into the DOM** (`5bd5452`): overall + sub-process bars + live "Creating X
  (n/total)" detail; retired the canvas `Loading`/`VectorBar`/`NpcBar`/`LoadingText`.
- **Faction selection**: already exists (ENL/RES).
- **Initial roster** (later): optionally "roll" a few starting individuals — ties into the
  rarity-tiered agents in the icebox; **light flavour, not a gacha/gambling loop**.
- **Dev tooling**: a `?debug` URL param enabling **timing measurements + console logging**
  (instrument load stages + tick cost) to profile and optimise the long loads.

### Landed since the 2026-06-21 overhaul note (this session)
Resonators end-to-end (8 colour-coded rods in rubber slot-rings, real-time from `resoMap()`, grow
with the pole, **fall on shatter** as physics rods, **hack spin + top-jointed centrifuge**); the
shatter physics extracted to `ShatterFx` (pole now **sinks**, donut/shards fall free); a **unified
demo sandbox** (`#demo`: Build/Effects mode toggle — LMB place/RMB remove · LMB XMP/RMB hack — plus
Upgrade/Downgrade/Link); **NPC marble drop-in** + removal of the dead 2D NPC render; a full
**dead-2D-draw sweep** (Portal/Agent/Field/Link/XmHeap/XmMap draws + DrawUtil); **camera-aware
stereo panning** (`Scene3D.audioPan`, projects through the live camera) + a **marble "tok"** sound;
**quick-start default-on**, a **location label + Menu (New Game/Reset)** replacing the old dropdown,
the **Gradle config cache**, and thinner orb glass + L8 demo default.

### Map playability, terrain & link/field integrity ✅ DONE (2026-06-21)
Made every playable map actually playable, gave terrain real meaning, and locked down the core
link/field rules with tests. **23 new unit tests.**

- [x] **No closed-off areas (grid connectivity)** (`9ca5144`). New pure, tested `util/GridConnectivity`
  (`components`/`walkability`/`connectIslands`): BFS from the largest component (the always-passable
  off-screen ring = the outside) carves the shortest corridor (`CORRIDOR_PENALTY`) to every other
  passable island. Run in `MapUtil.createGrid` after readback. +6 tests.
- [x] **Walkability gate** (`30bf33f`). `World.walkability` computed/logged at grid build;
  `HtmlUtil.onMapload` blocks `< MIN_WALKABILITY` (12%) maps with a "choose another location" overlay
  before the expensive world build (auto-start exempt).
- [x] **Location preset conformance** (`36de3de`). `LocationTest` (+7) guards all 62 presets (coords
  in range, no null-island, unique names+coords, ASCII enum names). Walkability of marina/bridge
  spots stays a runtime concern (gate) + future jet-skis make water traversable — none removed.
- [x] **Per-terrain penalties + terrain display** (`df2d242`). `SHADOW_STYLE` grades landcover by
  class (wood darkest/slowest → wetland → grass/farmland → sand/rock lightest); flow-field magnitude
  now scales with the cell penalty (`PathUtil`, `MIN_SPEED_FACTOR` floor) so agents physically slow on
  rough ground; the overlay is relabelled **"Terrain"** (`PassabilityOverlay`, extracted from Scene3D).
- [x] **Link/field integrity — fix + TEST** (`fed7f00`). Fixed `Portal.findLinkableForKeys` (its
  no-crossing filter was a no-op via `Link.isNotExisting`; now checks `World.allLines()` directly).
  New `LinkFieldIntegrityTest` (+10): no-crossing geometry, and **no dangling links/fields** when a
  portal is destroyed/neutralised (cleanup core = `destroy()`/`destroyAllLinksAndFields`).

**Demo/game polish landed alongside (2026-06-21):** explicit demo **action buttons** for every
animation (`afada15`); **game XMP burst + hack collar-spin** now fire in-game via new `HackFx`
(`9ccce9f`); **XMP detonates at the agent**, not aimed (`10fad73`); demo **click-to-select-or-place**
+ mouse cursor ring + min gap (`10fad73`); no vector-field preview during world build; quick-start
default-on + location label + New Game/Reset menu (`bb6ba87`); marble NPC drop + dead-2D-draw sweep.

### Smaller deferred follow-ups (this session)
- [x] **Orphaned `Queues.endTick` — removed** (`b73766d`). The attack/damage queues only fed the
  dead `Attacks.draw` telegraph and leaked; damage applies synchronously in `XmpBurster.dealDamage`.
  Deleted the whole telegraph/queue system (`Attacks`/`Display`/`Damage`/`Queues`); `Attacker` now
  applies damage + sound + the 3D XMP burst inline. Attacks ARE visualised in 3D now (the burst).
- [ ] **XM heaps have no 3D representation** (their 2D draw was dead). Bring stray XM
  (`portal/XmMap`/`XmHeap`) into Scene3D so the collectible XM is visible. (Attack telegraphs are now
  covered by the 3D XMP burst; floating damage numbers stay dropped.)
- [ ] **Full Web Audio 3D (optional).** Current panning is screen-projected (`Scene3D.audioPan`); a
  true `PannerNode` + listener driven by the camera would add distance attenuation + front/back +
  elevation. Only if we want richer spatialisation.
- [x] **Stray XM in 3D** (`b06274a`): glowing additive-white motes per heap (`Materials.xmGlow`),
  scaled by amount. Extracted `VectorFieldOverlay` to fit under the size limit.
- [x] **Demo build-grow + rod spacing** (`2f54642`): demo portals grow in on place/upgrade;
  `RESO_RADIUS_FRAC` 1.15→1.7 so resonator slots read distinct from top-down.
- [ ] **Font swap:** Amarillo USAF (Shareware) → an OFL alternative (user confirmed non-commercial).
- [ ] **Full Web Audio 3D** (next, after compact): `PannerNode` + camera-driven listener for distance
  attenuation + front/back + elevation (current `Scene3D.audioPan` is screen-projected stereo only).

**Tooling:** the pre-commit hook now runs the FULL gate (`2f54642` era) — ktlintFormat (auto-fix +
re-stage) then compile + ktlintCheck + detekt + jsNodeTest — so `git add -A && git commit` enforces
everything; no manual `./gradlew` gate runs needed.

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
  - **Items**: skateboards, **jet-skis** (water traversal — would make the marina/bridge/canal
    location presets fully playable, pairs with per-terrain water cost), power-banks, second phones,
    … affecting movement/energy/capacity (extends the existing `items/` + `agent/Inventory.kt`).
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
