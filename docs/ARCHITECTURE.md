# ARCHITECTURE.md — how Q-Gress is put together

How the running system fits together. For the *feature list* see [FEATURES.md](FEATURES.md);
for *what's next* see [../PLAN.md](../PLAN.md).

## Entry & top-level shape

`Main.kt` → `window.onload` → `Bootstrap.load()`. The simulation lives in a fixed pixel
space (`config/Dim.kt`, `config/Sim.kt` — `Sim` sizes the play area by **real-world km²**, not a
screen multiple: `sideForArea(km²)` picks the square whose inscribed round field hits the target area,
window-independent). The world is **rendered in 3D** by a three.js scene mounted as a **MapLibre custom
layer**; the HUD is **DOM**. There is no on-screen 2D game canvas anymore (see *Rendering* below).

`World` (`World.kt`, a singleton `object`) holds all mutable game state: agents, portals,
the passability grid, the tick counter, and the selected user faction. Game logic mutates
`World` directly today (the functional-core split that would isolate this is future work —
see PLAN.md).

## Source layout (`src/jsMain/kotlin/`, tests mirror under `src/jsTest/kotlin/`)

The packages split along one line: **`agent`/`portal`/`items`/`ai`/`config` are the Ingress domain**
(abstract game objects), while **everything that runs and presents the sim is engine, grouped under
`system/`**, kept away from the domain. `util/` holds only genuine cross-cutting helpers.

- **`World.kt`** — global mutable game state.
- **`agent/`** — the AI. `Agent` (faction members), `NonFaction` (recruitable NPCs),
  movement, skills, inventory, and:
  - **`agent/action/`** — `ActionSelector` is the brain: each tick every agent picks an
    action by **weighted-random selection** (`Rng.select`) over candidate actions.
  - **`agent/action/cond/`** — one object per action (`Recruiter`, `Hacker`, `Linker`,
    `Deployer`, `Attacker`, `Recharger`, `Glypher`, `Explorer`, `Recycler`).
  - **`agent/qvalue/`** — `QActions` (10) and `QDestinations` (7) define the tunable
    behaviours. Each `QValue` has a base `weight`; the **slider value (0..1) × weight** is the
    selection probability. Sliders exist per faction (`…SliderFrog` / `…SliderSmurf`).
  - **`ai/`** — the AI substrate: `FactionPolicy` (the per-faction source of slider
    weightings; default `DomSliderPolicy` reads the DOM, and `currentVector()` exposes the vector an AI is
    driving — null = no AI in control), `SliderVector` (the 17 sliders as one ordered encode/decode vector),
    `HeuristicPolicy` (the first live AI driver — an adaptive `Observation → SliderVector` mapping),
    `ai/net/NetPolicy` (a trained net as a driver), `Observation` (a normalized world feature vector — the
    NN/LLM input), `SimRunner` (the **headless match harness** — see *Rendering* below), and `Tournament`
    (ranks drivers over seeded `SimRunner` matches → a `Standing` leaderboard; the in-game benchmark wraps it
    in `system/WorldSnapshot` so it can run without disturbing the live game).
- **`portal/`** — portals, resonators, links, fields, XM, cooldowns, level/quality; hack-reward rolls split
  into `HackLoot`, portal-placement geometry in `Octant` (`commonMain`).
- **`items/`** — bursters, power cubes, resonators, mods, levels.
- **`config/`** — `Config` (balance constants), `ConfigMath`/`SimMath` (the pure tuning + geometry formulas,
  in `commonMain`), `Dim`/`Sim` (extent), `Location`/`Locations` (the JSON-backed place catalogue), `Styles`,
  `Colors`, `Time`.
- **`system/`** — the **engine/runtime**: `Cycle`/`Checkpoint` (scoring/history over time), `Com`
  (message log), `Simulation` (the shared tick step), `WorldSnapshot` (capture/restore the live sim
  singletons so a headless eval can run + the game resume), plus the presentation/IO subsystems:
  - **`system/display/`** — all 3D rendering: `Scene3D` + materials/overlays, with **`display/shader/`**
    (GLSL: `GlassShader`/`PlasmaShader`/`ShieldShader`/`XmpShaders`/`Glsl`) and **`display/fx/`** (the
    one-shot effects: `ShatterFx`/`XmpBurst`/`HackFx`/`BoltFx`/`FieldFx`/…).
  - **`system/effect/`** — the `Effects` sink seam (headless vs browser; see *Rendering* below).
  - **`system/audio/`** — the synth/mixer sound engine (`Sound`, `Mixer`, `AudioFx`, `KickDrum`, …).
  - **`system/map/`** — MapLibre integration (`MapController`, `MapStyles`, `GeoLocator`, `Navigation`).
  - **`system/building/`** — own-mesh buildings (`BuildingTiles`/`BuildingStream`/`BuildingShake`).
  - **`system/grid/`** — the spatial substrate: `Pathfinding` (flat-array bucketed Dijkstra → a flat
    `VectorField` in `extension/`, no `Pos`-keyed maps); the pure `Grid` (`extension/`), `GridConnectivity` and
    `GridFixture` live in **`commonMain`**.
  - **`system/ui/`** — the DOM HUD: `Bootstrap` (entry/DOM construction) — the **main tick + RAF loop is split
    out into `GameLoop`** (pause/speed/scheduling), the offscreen canvases into `extension/CanvasFactory` —
    plus `HudRenderer`, `Footer`/`Hud`/`Dom`, `FpsMeter`, `Onboarding`/`TitleSim`, with the panels under
    **`system/ui/panel/`**.
- **`util/`** (jsMain) — genuine cross-cutting helpers only (no longer a home for subsystems): `ColorUtil`,
  `ImprovedNoise`, `PortalNames`, `Prefs`/`GameplayPrefs`, `Debug`, `GameUrl`, `VersionCheck`.
- **`commonMain` — the pure, testable core.** Mirrors the same package names and holds the side-effect-free
  logic the shell delegates to (Kover-covered): `util/` (`Rng`, `MathUtil`, `Time`, `NameGen`, and `util/data/`
  `Vec3`/`Complex`/`Pos`/`GeoCoords`), `config/` (`SimMath`, `ConfigMath`), `agent/` (`MovementMath`, the
  `qvalue/` model), `ai/` (`SliderVector`, `HeuristicTune`) + `ai/net/` (`Activation`, `NetArch`),
  `portal/Octant`, `extension/Grid`, and `system/grid/` (`GridConnectivity`, `GridFixture`). Grows as more logic
  migrates out of the shell (see PLAN phase B; *Build & toolchain* for the jvm()-test/Kover setup).
- **`external/`** — thin `external` declarations: `MapLibre`, `Three`, `GLTFLoader`, `UPlot`,
  the Web Audio API, cannon-es.

## The selection brain (sliders → behaviour)

`ActionSelector.q(faction, qValue)` returns `FactionPolicies.of(faction).weight(qValue) × qValue.weight`
and feeds that into `Rng.select` (cumulative-probability weighted random). The default policy
(`DomSliderPolicy`) reads the slider straight from the DOM
(`getElementById("${id}Slider${nick}").valueAsNumber`, or `0.1` headless), so behaviour is unchanged —
but **this policy seam is where the AI drivers plug in** (a driver installs a
`SliderVectorPolicy` / `HeuristicPolicy` / `NetPolicy` via `FactionPolicies.set`, re-tuning the 17 sliders
at checkpoint cadence). When a faction is AI-driven, `FactionPolicy.currentVector()` is non-null, so the
`TuningPanel` mirrors that vector onto the live sliders each frame (they auto-move) and the **AI** footer
tab (`SliderHistoryPanel`, merged with the observation readout) graphs every slot over the checkpoint window.

Scoring: `World.calcTotalMu(faction)` (Mind Units = summed field area control) is the headline
metric. `system/Cycle` snapshots a `Checkpoint` every `Config.ticksPerCheckpoint` ticks into a
rolling window (~35 points) — per-faction MU **and** Portals/Links/Fields/Agents counts — which
feeds the HUD history dashboard.

## How the map becomes a playfield

`MapController` instantiates **three MapLibre maps** over divs:
- **`initMap`** (satellite, Esri + openmaptiles for 3D buildings) — the **visible** map; it
  hosts the three.js custom layer and receives all navigation gestures.
- **`map`** (street, OpenFreeMap positron) — the alternate base layer (View dropdown toggle).
- **`shadowMap`** (a black-bg / white-streets / graded-landcover mask) — rendered once at
  startup; its WebGL canvas is read with **`gl.readPixels()`** to build the passability
  **`Grid`** (bright = walkable, dark = impassable; landcover class → movement penalty). Hidden
  after the grid + POI/street names are read.

`MapController.addGrid` reads the shadow pixels and `World.createStreetImage` packs them into an
`ImageData` (offscreen 2D canvas via `extension/CanvasFactory` — the only surviving 2D-canvas use); then
**`system/map/ShadowGridBuilder.build`** turns that into the cell `Grid`, and `GridConnectivity.connectIslands`
carves corridors so no area is sealed off. `Pathfinding` computes per-portal vector fields over that grid
(flat-array bucketed Dijkstra, flow magnitude scaled by terrain penalty). `util/PortalNames` queries the
shadow map's vector source for real POI/street names.

**Zoom is calibrated to 18**: the grid, the pixel-to-metre factor, portal sizes, and ranges
are all implicitly tied to zoom 18. The display zooms out to *frame* the whole Sim area, but
the grid anchor stays at 18. Dynamic-zoom would require rebuilding the grid + rescaling — see
the movement/pathfinding-rework note in [FUTURE.md](FUTURE.md) (*Grand game*).

**Buildings are our own meshes, sourced from OSM.** MapLibre's openmaptiles building layer is
heavily simplified (a city tile may hold ~19 of 1000+ real buildings), so `system/building/BuildingTiles`
queries **OSM via Overpass** for the full footprints in the play-area bbox; `system/display/
OwnBuildings` extrudes them into three.js prisms (sun shadows, grow-in, per-mesh blast shake,
debris colliders) and hides MapLibre's fill-extrusion. `system/building/BuildingStream` keeps streaming
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
- **HUD → DOM.** `system/ui/`: `StatsPanel` (MU bars + time/tick + action LOG), `HistoryPanel`
  (per-metric uPlot sparklines + live values), `TuningPanel` (behaviour sliders; auto-moves under an AI
  driver), `DriverControls` (per-faction brain picker + LLM model picker), `AiPanel` (observation readout),
  `BrainsPanel` (the **BRAINS** tab — per-faction driver card: NN live activation + genome via `NetVizPanel`,
  or LLM model/status/prompt/reply), `TrainerPanel` (the **TRAIN** tab — neuroevolution trainer + leaderboard),
  `SliderHistoryPanel` (per-slider sparklines over time, in the AI tab), `TopAgentsPanel`, `Inspector`, `LayerView`,
  `Onboarding`, `LoadingOverlay`, `MiniMap` (globe inset), `Controls`. Styled by
  `resources/stylesheet/QGress.css` (faction colours via `--enl-color`/`--res-color`;
  **Chakra Petch** title face, **Coda** for text/numbers).
- **No 2D game canvas.** The old `mainCanvas`/`uiCanvas` layers are gone; `HudRenderer` keeps only
  the offscreen prerender of agent action icons (→ 3D textures). `World.bgCan` survives solely
  as a detached `ImageData` factory for the grid readback.

The tick loop (`system/ui/GameLoop`, split out of `Bootstrap`) calls the shared functional-core step
`system/Simulation.stepEntities` (advance every agent on a snapshot — recruits buffer in `World.pendingAgents`,
flushed after — then every NPC, then feed the stuck tracker); `GameLoop` owns pause + the ×1/×3/Max speed (it
runs N steps per fire), and a `requestAnimationFrame` drives `HudRenderer.redraw` (→ `Scene3D.sync`) + the DOM
HUD update + `Cycle` scoring. The headless harness (`ai/SimRunner`) calls the *same* `Simulation.stepEntities`
with synchronous `Cycle` scoring instead — no rendering loop.

**The effect-sink seam (`system/effect/`).** The crash-prone *visual* effects that game logic fires
inline (XMP bursts, hack/deploy animations, reward motes, retaliation bolts, portal shatter, falling
resonators, the flow-field flash) go through `Fx.sink` — an installable `Effects` interface, mirroring
`FactionPolicies`. `BrowserEffects` forwards 1:1 to the `system/display/` renderer; `NoOpEffects` (the
headless default) does nothing, so the whole tick loop runs in Node without touching three.js — the
imperative-shell boundary that unblocks the headless `SimRunner`. Audio (`Sound`)
and the message log (`Com`) already self-guard / are pure, so they stay outside this seam.

**Headless matches (`ai/SimRunner`).** With the effect sink (no renderer crashes), `Pathfinding.computeFieldSync`
(deterministic inline flow fields, opt-in via `Config.headlessFieldCompute`) and the shared
`Simulation.stepEntities`, a whole match runs in Node: `SimRunner.runMatch(grid, seed, maxTicks, …)` seeds
the RNG + a `GridFixture` grid, seeds portals/agents/NPCs, ticks, and returns a `MatchResult` of
per-checkpoint MU (the AI fitness signal). `SimRunner.reset()` clears all match state between runs. This is
the training/eval engine. A full-resolution match runs in ~tens of ms (the AI consumes only
`Observation` stats, never cell data, so flow-field navigation isn't on its critical path); `MatchSetup.flowFields`
toggles obstacle-routed vs straight-line movement for fidelity, not speed.

**Title screen reuses this whole pipeline.** `system/ui/TitleSim` runs a small *real* `Scene3D` sim
(real grid, ~8 portals, a 3-v-3 levelled roster + ~30 NPCs, the real tick loop) behind the faction
menu — no parallel rendering code. The wordmark is real 3D extruded text (`system/display/
TitleWordmark`, camera-locked each frame from the recovered eye/forward/up, reacting to XMP blasts);
`external/FontLoader` + `external/TextGeometry` (three addons, bundled like `GLTFLoader`) load the
brand `typeface.json`. Picking a faction **reloads** into the game (URL handoff — there is no in-place
map/Scene3D teardown).

Flow fields (the per-portal navigation heat maps) are generated **off the synchronous path**:
`Pathfinding.computeFieldAsync` runs a bucketed-Dijkstra heat map + vector field as a `suspend` coroutine
on `MainScope`, yielding every ~2000 cells, and writes the result back into `Portal.vectors` when ready.

## Locations

The preset-place catalogue is **externalized to `resources/locations.json`** (edit it without a Kotlin
build) and loaded at startup via `Locations.load` into the `Locations` registry (`config/Location.kt`). A
`Location` is a `data class` (`name`, `displayName`, `lng`, `lat`, `title`); `title = true` flags a place as
a title-screen showpiece. Only a single hardcoded `Locations.DEFAULT` exists — the synchronous startup value
and the fallback if the JSON can't be fetched (which shouldn't happen). Selecting a place (onboarding or
in-game) routes through `?lng=&lat=&name=` URL params. Free-form geocoding (Nominatim) is available in
onboarding/search history; arbitrary coordinates work.

## Build & toolchain

Kotlin 2.4 → JS (IR), Gradle 9.5, build JVM **JDK 21** (detekt can't run on 25). Browser
bundle via the Kotlin/JS webpack; **unit tests run in Node** (Mocha). Quality gates: **ktlint**
+ **detekt** (complexity limits) + tests, enforced by an in-repo **pre-commit hook**
(`.githooks/`, `core.hooksPath`). three.js / cannon-es / MapLibre / uPlot come via npm or CDN
`external` declarations. See [../CLAUDE.md](../CLAUDE.md) for commands.

**Line-coverage:** the pure core in `commonMain` also compiles to a test-only `jvm()` target so
**Kover** can instrument it (it can't read Kotlin/JS); the same `commonTest` suite runs on both Node
and the JVM. CI emits `koverXmlReport` and uploads it to **Codecov** (badge in the README). The
browser/WebGL/three.js shell in `jsMain` isn't counted — coverage tracks the functional core, and
grows as more logic migrates into `commonMain` (see PLAN phase B/C).

Small **shared helpers** keep the shell DRY: `util.ColorUtil` (hex↔rgb / blend), `util.Prefs`
(localStorage load/save), `system.ui.Dom.el()` (DOM-div factory for the footer panels), `extension/CanvasFactory`
(offscreen 2D canvases / readback contexts), the `util.data.Vec3` vector kit (now in `commonMain`), and
`system.display.Glsl.glsl()`'s float-literal helper.
