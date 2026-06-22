# FEATURES.md — what's shipped

A record of completed work, so [PLAN.md](../PLAN.md) can stay future-only. Grouped by area,
newest themes roughly last. Commit hashes are illustrative pointers, not exhaustive.

## Toolchain & build (Phase 1)
- Migrated the dead 2018 build to **Kotlin 2.4 → JS (IR)**, **Gradle 9.5**, build JVM **JDK 21**
  (detekt can't run on JDK 25). Sources under `src/jsMain` / `src/jsTest`.
- Replaced removed/relocated APIs (`kotlin.browser.*`/`kotlin.dom.*` → `kotlinx-browser`,
  `toUpperCase`→`uppercase`, `Double.toByte()`, literal `js()`, null-safety).
- Existing tests compile + green in **Node** (Mocha). Karma/Chrome wiring kept (disabled).
- **ktlint + detekt** (complexity limits, baselined) wired into Gradle; **pre-commit hook**
  (`.githooks/`, `core.hooksPath`) enforces the **full gate** — ktlintFormat (auto-fix + restage)
  then compile + ktlintCheck + detekt + jsNodeTest. Gradle config cache on.

## Maps & the zoom bug (Phase 2)
- Dropped the dead rawgit OpenLayers include; **Mapbox GL → MapLibre GL 5.24**
  (`external/MapLibre.kt`), no token. Keyless tiles: OpenFreeMap (street/vector) + Esri imagery.
- Authored open styles: positron street, satellite (+ openmaptiles 3D buildings), and the
  **black-bg / white-streets shadow mask** for the passability grid.
- Fixed grid/zoom reliability: `preserveDrawingBuffer`, wait for `idle` before `readPixels`,
  select the shadow canvas by container query. Fixed `[0,0]` center sentinel + empty-collection
  `max/min` crashes. Web assets → `src/jsMain/resources`; webpack bundle serves `index.html`
  (`start.sh`); retired `published/*.js`.

## Locations & navigation (Phases 3–4, 3D nav)
- **"Play your hometown"**: free-form geocoding via keyless **Nominatim/OSM** → `?lng=&lat=&name=`
  recenter, works on any host.
- Navigation handed to **MapLibre's own handlers** (left-drag pan, right-drag rotate+tilt, wheel
  zoom, unrestricted; NavigationControl block). Optional WASD / Q-E / R-F keys (`Navigation`).
- **Mini-globe inset** (`util/ui/MiniMap`) — circular synced overview with a FLAT/GLOBE toggle.
- **Home button** — recenter top-down over the play area (pitch 0) to find the action again.
- **Shareable links** — URLs carry lng/lat/name + size (w/h) + **seed**; a Menu "Copy link" copies a
  link reproducing the exact world. Backed by a **seedable mulberry32 RNG** (`Util.random`, the sole
  randomness source) so the same seed → the same world.

## 3D rendering & the glass visual overhaul
- **Foundation**: three.js custom layer camera-synced to the MapLibre matrix (`system/display/
  Scene3D`, `external/Three`). The simulation stays 2D; only rendering went 3D.
- **Abstract-glass apparatus** look (à la qlippostasis), grayscale vessel + faction tint only:
  - `GlassShader` (Fresnel rim + emission + vnoise smudges, double-shell thick walls,
    camera-tracking rim via `updateEye`).
  - Portals = **metal pole + rubber gasket + glass orb** (φ-scaled by level L1→L8), **grow-on-spawn**
    + **level-up tween**.
  - **Resonators** — 8 colour-coded rods in rubber slot-rings, real-time from `resoMap()`, grow
    with the pole, **fall on shatter** (cannon-es physics rods), **hack spin + top-jointed
    centrifuge** (`HackFx`); shatter physics in `ShatterFx` (pole sinks, shards fall).
  - **Links → glass pipes** (`linkGeo`/`orientTube`, brighter variant + additive plasma core).
  - **Fields → plasma** sheets (`PlasmaShader`, animated; fill-in + dissolve + collapse sound).
  - **XMP** — volumetric raymarched fireball (`XmpShaders`/`XmpBurst`), detonates at the agent.
  - **Stray XM** rendered as glowing additive motes (`Materials.xmGlow`).
  - **Lifecycle registry** `Spawns` (per-entity first-seen, survives the sync rebuild) drives
    spawn/teardown animations; `FieldFx` dissolves.
- **Real portal names** from map data (`util/PortalNames`: POI/street query on the shadow source).
- **GLB compaction** (`95dda03`, Blender): `shattered_flask.glb` 2.43 MB → **657 KB** (strip unused
  UVs/materials, weld verts, smooth normals; all 12 shatter variants preserved). Editable source at
  `assets/blender/shattered_flask.blend`.
- **Demo sandbox** (`/#demo`): Build/Effects mode toggle (place/remove · XMP/hack), Upgrade/
  Downgrade/Link, grow-in animations, gray backdrop toggle.

## UI / HUD (canvas → DOM)
- The **entire HUD is DOM** and themeable. `StatsPanel` (MU "covered area" bars + time/tick +
  action **LOG**), **history dashboard** `HistoryPanel` (per-metric uPlot sparklines — MU + Portals
  + Links + Fields + Agents over time, with live ENL/RES values; `Checkpoint` snapshots all of them),
  `TopAgentsPanel` leaderboard, `Inspector` (selected entity), `Controls` legend + desktop-only gate.
- **Top toolbar** reorganized: Menu far-left (with Terrain/Vectors overlay toggles inside it),
  Pause/Resume, Home; View dropdown + Volume far-right.
- Map visuals: grayscale-terrain default + Colored/Street views (`LayerView`), white play-area
  border + dimmed out-of-bounds (`PlayAreaMask`). Rule: faction colours for faction things only.
- **Dead 2D canvas layer removed** — `mainCanvas`/`uiCanvas` + the `#canvasLayer` div gone (world is
  the 3D layer, HUD is DOM); `bgCan` kept only as a detached `ImageData` factory for the grid.
- **Font**: shareware Amarillo USAF → **Chakra Petch** (SIL OFL 1.1, self-hosted), techno/sci-fi-HUD
  to fit the science-vs-luddite theme. Coda for text.

## Gameplay, balance & map playability
- **Balance (Phase 5)**: recruiting now **costs XM** + has **diminishing returns** near the cap, so
  recruit-rush is no longer a free snowball. Fixed a recruiting CME (`World.pendingAgents` buffer).
- **Map playability**: `GridConnectivity` carves corridors so no area is sealed off — and the gameplay
  carver (`connectIslands(grid, w, h)`) now also joins **on-screen** regions to each other directly, so
  two areas that both touch the off-screen ring no longer connect only via a long map-edge detour (the
  cause of agents wandering/looking stuck). `World.walkability` gate blocks mostly-water maps;
  `LocationTest` guards all presets; per-terrain movement penalties (landcover-graded shadow style →
  `PathUtil` flow magnitude) + Terrain overlay.
- **`?debug` diagnostics** (off by default, `util.Debug`, disabled in Node): grid-build connectivity
  self-check log (islands / on-screen islands / walkability, warns when unhealthy); **stuck/loop
  detection** (`StuckTracker`) that flags non-progressing agents/NPCs with a 3D marker + a HUD count;
  **`?debug=capture`** sweeps every preset and downloads a `GridFixture` snapshot file, which
  `PresetConnectivityTest` audits offline in Node (single component + on-screen-connected + walkable).
- **Non-blocking flow fields** (`PathUtil.computeFieldAsync`): the per-portal heat-map BFS + vector
  field are now `suspend` and yield (`delay(0)`) per wavefront layer / every ~2000 cells / between
  smooth passes, computed on a `MainScope` and written back into `Portal.vectors` (and the offscreen
  field cache) when ready — so creating a portal/field no longer freezes the JS thread ~1s. Agents
  fall back to a straight-line heading while a field is still empty (so they keep moving + re-sample
  the real field once it lands) instead of stalling on `Complex.ZERO`.
- **Link/field integrity** (`LinkFieldIntegrityTest`): fixed `Portal.findLinkableForKeys` no-crossing
  filter; no dangling links/fields on portal destroy/neutralise.
- Removed the dead attack/damage telegraph/queue system (`Attacks`/`Display`/`Damage`/`Queues`);
  damage applies inline, visualised by the 3D XMP burst.

## Onboarding (Phase 7, partial)
- **Ordered onboarding** faction → map-size → location → load (`util/ui/Onboarding`), in-memory
  (no reloads); `?local=true` auto-starts; deep links load directly.
- **Map size + portal density** presets (Small/Normal/Large, editable W/H + portal count).
- **Staged loading overlay** (`LoadingOverlay`) — map tiles → street tiles → passability → grid →
  building world, faction-tinted, translucent at build to reveal the spawning world.

## Audio
- **True 3D positional audio** (`ba82492`): every positional sound routes through a `PannerNode` at
  its sim-space metre position, with an `AudioListener` driven by the live camera each frame
  (`Scene3D.render` → `SoundUtil.updateListener`; eye from `GlassShader.eye()`, forward/up by
  unprojecting the inverse projection). Distance attenuation + front/back + elevation that track
  rotate/pitch/zoom (HRTF, inverse distance model). Replaced the screen-projected stereo pan.
- Per-event sounds (portal create/remove, field up/down, shatter, XMP, hack/glyph/deploy/link,
  checkpoint/cycle, marble "tok" NPC drop); non-positional events (checkpoint/cycle/fail) stay
  center-panned. **Volume slider** + master gain; audio resumes on first gesture.

## Settled decisions
- **Modernize Kotlin/JS in place** (not a TS rewrite; not KMP) — keep the ~7k lines of tested logic.
- **MapLibre GL JS** as the map provider (open, keyless).
- **Build on JDK 21**, Kotlin 2.4, Gradle 9.5 (pure tooling choice; ships JS only).
