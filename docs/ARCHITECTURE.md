# ARCHITECTURE.md — how Q-Gress is put together

How the running system fits together. For *what's shipped* see [FEATURES.md](FEATURES.md);
for *what's next* see [../PLAN.md](../PLAN.md).

## Entry & top-level shape

`Main.kt` → `window.onload` → `HtmlUtil.load()`. The simulation lives in a fixed pixel
space (`config/Dim.kt`, `config/Sim.kt` — Sim is a scaled-up multiple of the screen). The
world is **rendered in 3D** by a three.js scene mounted as a **MapLibre custom layer**; the
HUD is **DOM**. There is no on-screen 2D game canvas anymore (see *Rendering* below).

`World` (`World.kt`, a singleton `object`) holds all mutable game state: agents, portals,
the passability grid, the tick counter, and the selected user faction. Game logic mutates
`World` directly today (the functional-core split that would isolate this is future work —
see PLAN.md).

## Source layout (`src/jsMain/kotlin/`, tests mirror under `src/jsTest/kotlin/`)

- **`World.kt`** — global mutable game state.
- **`agent/`** — the AI. `Agent` (faction members), `NonFaction` (recruitable NPCs),
  movement, skills, inventory, and:
  - **`agent/action/`** — `ActionSelector` is the brain: each tick every agent picks an
    action by **weighted-random selection** (`Util.select`) over candidate actions.
  - **`agent/action/cond/`** — one object per action (`Recruiter`, `Hacker`, `Linker`,
    `Deployer`, `Attacker`, `Recharger`, `Glypher`, `Explorer`, `Recycler`).
  - **`agent/qvalue/`** — `QActions` (10) and `QDestinations` (7) define the tunable
    behaviours. Each `QValue` has a base `weight`; the **slider value (0..1) × weight** is the
    selection probability. Sliders exist per faction (`…SliderFrog` / `…SliderSmurf`).
  - **`ai/`** — the AI substrate (PLAN Phase 6): `FactionPolicy` (the per-faction source of slider
    weightings; default `DomSliderPolicy` reads the DOM, and `currentVector()` exposes the vector an AI is
    driving — null = no AI in control), `SliderVector` (the 17 sliders as one ordered encode/decode vector),
    `HeuristicPolicy` (the first live AI driver — an adaptive `Observation → SliderVector` mapping),
    `ai/net/NetPolicy` (a trained net as a driver), `Observation` (a normalized world feature vector — the
    NN/LLM input), `SimRunner` (the **headless match harness** — see *Rendering* below), and `Tournament`
    (ranks drivers over seeded `SimRunner` matches → a `Standing` leaderboard; the in-game benchmark wraps it
    in `system/WorldSnapshot` so it can run without disturbing the live game).
- **`portal/`** — portals, resonators, links, fields, XM, cooldowns, level/quality.
- **`items/`** — bursters, power cubes, resonators, mods, levels.
- **`config/`** — `Config` (balance constants), `Dim`/`Sim` (geometry), `Location` (preset
  places), `Styles`, `Colors`, `Time`.
- **`system/`** — `Cycle`/`Checkpoint` (scoring/history over time), `Com` (message log),
  `WorldSnapshot` (capture/restore the live sim singletons so a headless eval can run + the game resume),
  **`system/display/`** (all 3D rendering: `Scene3D` + the shader/effect/material modules), and
  **`system/effect/`** (the `Effects` sink seam — see *Rendering* below).
- **`util/`** — `HtmlUtil` (DOM/UI construction + the main tick loop), `MapUtil` (MapLibre
  lifecycle + grid build + camera), `PathUtil` (vector-field pathfinding), `Navigation`,
  `SoundUtil`, `GridConnectivity`, `DrawUtil` (canvas-icon prerender helpers), geometry under
  `util/data/`, DOM panels under `util/ui/`.
- **`external/`** — thin `external` declarations: `MapLibre`, `Three`, `GLTFLoader`, `UPlot`,
  the Web Audio API, cannon-es.

## The selection brain (sliders → behaviour)

`ActionSelector.q(faction, qValue)` returns `FactionPolicies.of(faction).weight(qValue) × qValue.weight`
and feeds that into `Util.select` (cumulative-probability weighted random). The default policy
(`DomSliderPolicy`) reads the slider straight from the DOM
(`getElementById("${id}Slider${nick}").valueAsNumber`, or `0.1` headless), so behaviour is unchanged —
but **this policy seam is where the AI drivers plug in** (PLAN.md Phase 6: a driver installs a
`SliderVectorPolicy` / `HeuristicPolicy` / `NetPolicy` via `FactionPolicies.set`, re-tuning the 17 sliders
at checkpoint cadence). When a faction is AI-driven, `FactionPolicy.currentVector()` is non-null, so the
`TuningPanel` mirrors that vector onto the live sliders each frame (they auto-move) and the **AI** footer
tab (`SliderHistoryPanel`, merged with the observation readout) graphs every slot over the checkpoint window.

Scoring: `World.calcTotalMu(faction)` (Mind Units = summed field area control) is the headline
metric. `system/Cycle` snapshots a `Checkpoint` every `Config.ticksPerCheckpoint` ticks into a
rolling window (~35 points) — per-faction MU **and** Portals/Links/Fields/Agents counts — which
feeds the HUD history dashboard.

## How the map becomes a playfield

`MapUtil` instantiates **three MapLibre maps** over divs:
- **`initMap`** (satellite, Esri + openmaptiles for 3D buildings) — the **visible** map; it
  hosts the three.js custom layer and receives all navigation gestures.
- **`map`** (street, OpenFreeMap positron) — the alternate base layer (View dropdown toggle).
- **`shadowMap`** (a black-bg / white-streets / graded-landcover mask) — rendered once at
  startup; its WebGL canvas is read with **`gl.readPixels()`** to build the passability
  **`Grid`** (bright = walkable, dark = impassable; landcover class → movement penalty). Hidden
  after the grid + POI/street names are read.

`MapUtil.addGrid` reads the shadow pixels into an `ImageData` (allocated via a **detached
offscreen `World.bgCan`** — the only surviving use of a 2D canvas), `createGrid` turns it into
the cell grid, and `GridConnectivity.connectIslands` carves corridors so no area is sealed off.
`PathUtil` computes per-portal vector fields over that grid (flow magnitude scaled by terrain
penalty). `util/PortalNames` queries the shadow map's vector source for real POI/street names.

**Zoom is calibrated to 18**: the grid, the pixel-to-metre factor, portal sizes, and ranges
are all implicitly tied to zoom 18. The display zooms out to *frame* the whole Sim area, but
the grid anchor stays at 18. Dynamic-zoom would require rebuilding the grid + rescaling — see
the "rework movement model" / "going 3D" icebox notes in PLAN.md.

**Buildings are our own meshes, sourced from OSM.** MapLibre's openmaptiles building layer is
heavily simplified (a city tile may hold ~19 of 1000+ real buildings), so `util/BuildingTiles`
queries **OSM via Overpass** for the full footprints in the play-area bbox; `system/display/
OwnBuildings` extrudes them into three.js prisms (sun shadows, grow-in, per-mesh blast shake,
debris colliders) and hides MapLibre's fill-extrusion. `util/BuildingStream` keeps streaming
new regions from Overpass as the camera flies elsewhere. Elevation comes from the live DEM
(`Scene3D.groundZAtLngLat`) so buildings sit right even outside the play-area height grid.

## Rendering pipeline (3D + DOM, no 2D game canvas)

- **World → 3D.** `system/display/Scene3D` builds/refreshes three.js meshes each tick
  (`sync()`): portals as metal pole + rubber gasket + glass orb (+ resonator rods), links as
  glass pipes, fields as plasma sheets, agents/NPCs as faction spheres, stray XM as glowing
  motes. The whole "glass apparatus" look is shader-driven (`GlassShader`, `PlasmaShader`,
  `XmpShaders`) — grayscale vessel, faction colour as the only tint. Effects (shatter via
  cannon-es, XMP fireball, hack centrifuge, spawn/teardown, **portal-defense lightning** in
  `BoltFx`) live in `ShatterFx`/`XmpBurst`/`HackFx`/`FieldFx`/`BoltFx`/`Spawns`. `Scene3D.render`
  shares the map depth buffer so buildings occlude the sim.
- **HUD → DOM.** `util/ui/`: `StatsPanel` (MU bars + time/tick + action LOG), `HistoryPanel`
  (per-metric uPlot sparklines + live values), `TuningPanel` (behaviour sliders; auto-moves under an AI
  driver), `DriverControls` (per-faction brain picker + LLM model picker), `AiPanel` (observation readout),
  `BrainsPanel` (the **BRAINS** tab — per-faction driver card: NN live activation + genome via `NetVizPanel`,
  or LLM model/status/prompt/reply), `TrainerPanel` (the **TRAIN** tab — neuroevolution trainer + leaderboard),
  `SliderHistoryPanel` (per-slider sparklines over time, in the AI tab), `TopAgentsPanel`, `Inspector`, `LayerView`,
  `Onboarding`, `LoadingOverlay`, `MiniMap` (globe inset), `Controls`. Styled by
  `resources/stylesheet/QGress.css` (faction colours via `--enl-color`/`--res-color`;
  **Chakra Petch** title face, **Coda** for text/numbers).
- **No 2D game canvas.** The old `mainCanvas`/`uiCanvas` layers are gone; `DrawUtil` keeps only
  the offscreen prerender of agent action icons (→ 3D textures). `World.bgCan` survives solely
  as a detached `ImageData` factory for the grid readback.

The tick loop (`HtmlUtil.tick`) calls the shared functional-core step `system/Simulation.stepEntities`
(advance every agent on a snapshot — recruits buffer in `World.pendingAgents`, flushed after — then every
NPC, then feed the stuck tracker), then a `requestAnimationFrame` drives `DrawUtil.redraw`
(→ `Scene3D.sync`) + the DOM HUD update + `Cycle` scoring. The headless harness (`ai/SimRunner`) calls the
*same* `Simulation.stepEntities` with synchronous `Cycle` scoring instead — no rendering loop.

**The effect-sink seam (`system/effect/`).** The crash-prone *visual* effects that game logic fires
inline (XMP bursts, hack/deploy animations, reward motes, retaliation bolts, portal shatter, falling
resonators, the flow-field flash) go through `Fx.sink` — an installable `Effects` interface, mirroring
`FactionPolicies`. `BrowserEffects` forwards 1:1 to the `system/display/` renderer; `NoOpEffects` (the
headless default) does nothing, so the whole tick loop runs in Node without touching three.js — the
imperative-shell boundary that unblocks the headless `SimRunner` (PLAN Phase 6.1). Audio (`SoundUtil`)
and the message log (`Com`) already self-guard / are pure, so they stay outside this seam.

**Headless matches (`ai/SimRunner`).** With the effect sink (no renderer crashes), `PathUtil.computeFieldSync`
(deterministic inline flow fields, opt-in via `Config.headlessFieldCompute`) and the shared
`Simulation.stepEntities`, a whole match runs in Node: `SimRunner.runMatch(grid, seed, maxTicks, …)` seeds
the RNG + a `GridFixture` grid, seeds portals/agents/NPCs, ticks, and returns a `MatchResult` of
per-checkpoint MU (the AI fitness signal). `SimRunner.reset()` clears all match state between runs. This is
the training/eval engine for Phase 6.2+. A full-resolution match runs in ~tens of ms (the AI consumes only
`Observation` stats, never cell data, so flow-field navigation isn't on its critical path); `MatchSetup.flowFields`
toggles obstacle-routed vs straight-line movement for fidelity, not speed.

**Title screen reuses this whole pipeline.** `util/ui/TitleSim` runs a small *real* `Scene3D` sim
(real grid, ~8 portals, a 3-v-3 levelled roster + ~30 NPCs, the real tick loop) behind the faction
menu — no parallel rendering code. The wordmark is real 3D extruded text (`system/display/
TitleWordmark`, camera-locked each frame from the recovered eye/forward/up, reacting to XMP blasts);
`external/FontLoader` + `external/TextGeometry` (three addons, bundled like `GLTFLoader`) load the
brand `typeface.json`. Picking a faction **reloads** into the game (URL handoff — there is no in-place
map/Scene3D teardown).

Flow fields (the per-portal navigation heat maps) are generated **off the synchronous path**:
`PathUtil.computeFieldAsync` runs a bucketed-Dijkstra heat map + vector field as a `suspend` coroutine
on `MainScope`, yielding every ~2000 cells, and writes the result back into `Portal.vectors` when ready.

## Locations

`config/Location.kt` is a fixed `enum` of preset places (lng/lat). Selecting one (onboarding
or in-game) routes through `?lng=&lat=&name=` URL params. Free-form geocoding (Nominatim) is
available in onboarding/search history; arbitrary coordinates work.

## Build & toolchain

Kotlin 2.4 → JS (IR), Gradle 9.5, build JVM **JDK 21** (detekt can't run on 25). Browser
bundle via the Kotlin/JS webpack; **unit tests run in Node** (Mocha). Quality gates: **ktlint**
+ **detekt** (complexity limits) + tests, enforced by an in-repo **pre-commit hook**
(`.githooks/`, `core.hooksPath`). three.js / cannon-es / MapLibre / uPlot come via npm or CDN
`external` declarations. See [../CLAUDE.md](../CLAUDE.md) for commands.
