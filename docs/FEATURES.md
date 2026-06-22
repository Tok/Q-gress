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
    + **level-up tween**. **Selection** keeps the faction hue and just lights the orb brighter
    (`GlassShader.SELECT_BRIGHT`) — no more neutral-looking white tint. A faction-neutral **name + level
    billboard** floats above each portal's orb (cached `CanvasTexture` sprite, Chakra Petch).
  - **Resonators** — 8 colour-coded rods in rubber slot-rings, real-time from `resoMap()`, grow
    with the pole, **fall on shatter** (cannon-es physics rods), **hack spin + top-jointed
    centrifuge** (`HackFx`); shatter physics in `ShatterFx` (pole sinks, shards fall).
  - **Links → glass pipes** (`linkGeo`/`orientTube`, brighter variant + additive plasma core).
  - **Fields → plasma** sheets (`PlasmaShader`, animated; fill-in + dissolve + collapse sound).
  - **XMP** — volumetric raymarched **mini-nuke** (`XmpShaders`/`XmpBurst`), detonates at the agent.
    The field morphs from an initial fireball into a **rising mushroom** — a torus cap (the rising
    donut / vortex ring) that climbs + spreads, plus a stem — carved into pyroclastic billows by
    rotation-decorrelated fbm displacement (no smooth-sphere look, no grid streaks). The hot core stays
    emissive while cool smoke is gradient-lit + translucent (warm, not a black blob); it starts tiny and
    fast-expands, then **dissolves gradually** (cooling colour + thinning alpha — a smoke tail). Plus a
    flat neon ground shockwave ring.
  - **Stray XM** rendered as glowing additive motes (`Materials.xmGlow`).
  - **Lifecycle registry** `Spawns` (per-entity first-seen, survives the sync rebuild) drives
    spawn/teardown animations; `FieldFx` dissolves.
- **Real portal names** from map data (`util/PortalNames`: POI/street query on the shadow source).
- **GLB compaction** (`95dda03`, Blender): `shattered_flask.glb` 2.43 MB → **657 KB** (strip unused
  UVs/materials, weld verts, smooth normals; all 12 shatter variants preserved). Editable source at
  `assets/blender/shattered_flask.blend`.
- **Demo sandbox** (`/#demo`): place/select/remove portals, Upgrade/Downgrade/Link/Hack/Glyph,
  grow-in animations, gray backdrop toggle. **XMP** fires at the selected portal via the X1–8 buttons,
  or — with the **"Fire XMP on click"** toggle on — detonates at the map click point (chosen X-level).

## UI / HUD (canvas → DOM)
- The **entire HUD is DOM** and themeable. `StatsPanel` (MU "covered area" bars + time/tick +
  action **LOG**), **history dashboard** `HistoryPanel` (per-metric uPlot sparklines — MU + Portals
  + Links + Fields + Agents over time, with live ENL/RES values; `Checkpoint` snapshots all of them),
  `TopAgentsPanel` leaderboard, `Inspector` (selected entity), `Controls` legend + desktop-only gate.
- **Unified tabbed dock** (`util/ui/Dock`): the scattered corner panes are consolidated into one
  right-docked panel with **NOW / HISTORY / TUNE** tabs (one view at a time) — NOW holds the
  scoreboard + leaderboard + LOG, HISTORY the sparklines, TUNE the sliders. It's the only
  pointer-interactive HUD surface (rest stays click-through for map gestures). The `Inspector` stays a
  separate contextual panel (top-left, clear of the dock), unified to the same glass frame.
- **Merged tuning panel** (`util/ui/TuningPanel`): the two slider panes (Actions + Destinations) are
  now one flat list (Actions, a divider, then Destinations). The DOM `<input>` per (q-value × faction)
  is still the value store `ActionSelector.q` reads. A **read-only mode** (`?readonly=true` or the
  Menu "Lock tuning" toggle) swaps the sliders for 0–1 progress bars that mirror the values
  (`refresh()` re-syncs them) — the hook for future agent-vs-agent matches where the player can't tune.
  Action icons render from the **hi-res** (supersampled) canvas + shown small via CSS, so they're crisp.
- **Portals are discovered, not placed**: manual portal **placement** and **deletion** are removed
  from the real game (the map click only selects/deselects; the Inspector has no Remove button). The
  `/#demo` sandbox keeps LMB-place / RMB-shatter for showcasing.
- **3D terrain (DEM heights)**: the map is no longer flat — a keyless **terrarium DEM** source (AWS
  open Terrain Tiles) drives MapLibre `setTerrain`, so the satellite drapes over real elevation. Game
  objects **sit on the terrain**: `Scene3D` samples a coarse elevation grid (`groundZ`, bilinear, via
  `simPosToLngLat` + `queryTerrainElevation`) and offsets every ground-anchored z (portals, agents,
  NPCs, stray XM, links/fields, labels, the deploy/loot/pickup FX). Menu **"3D terrain"** toggle
  (default on); degrades to flat if the DEM is unavailable. (The cannon-es shatter ground stays flat —
  a known approximation.)
- **Top toolbar** reorganized: Menu far-left (with Terrain/Vectors overlay toggles + Lock-tuning
  inside it), Pause/Resume, Home; View dropdown + Volume far-right.
- Map visuals: grayscale-terrain default + Colored/Street views (`LayerView`), white play-area
  border + upright **semi-transparent white boundary walls** + dimmed out-of-bounds (`PlayAreaMask`),
  so the arena reads as a physical box. Rule: faction colours for faction things only.
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
- **Shared 8-note scale** (`SoundUtil.noteFor`, C2 natural-minor octave): the 8 portal/XMP levels map
  to 8 notes with **level 8 = the lowest**, so a portal's level is audible across every level-keyed
  sound — XMP boom + volley blips, hack/glyph whirs (+ glyph chime), and upgrade/downgrade notes — and
  the sim plays in one key.
- **Layered XMP explosion** (`SoundUtil.playXmpExplosion`): on top of the kept synthetic boom + noise
  blast, a detonation snap + chest-punch sub at the note + a long lowpassed rumble tail whose
  brightness and amplitude decay over the fireball's life — so the sound rises and dissolves with the
  mini-nuke mushroom animation.

## Mods, viruses, items & drop rates
- **4 mod slots per portal** (`portal/ModSlot`) holding a generic `Mod` (`items/deployable/Mod`):
  - **Shields** (`ShieldType`) reduce incoming **XMP damage** — `Portal.totalMitigation` (links +
    shields, capped 95%) is applied in `XmpBurster.dealDamage`.
  - **Heat sinks** (`HeatSinkType`, 20/50/70%) cut the portal **hack cooldown** — `Portal.cooldownFactor`
    (rarest full, each subsequent halved) feeds `handleCooldown`.
  - **Link amps** (`LinkAmpType`) are defined + drawable but **inactive** (never drop, no effect).
  - Rarity colours are shared (`items/types/Rarity`: mint / purple / pink).
- **Deploy** is one item per action (`Deployer`): a resonator if one fits, else a mod into a free slot
  — each with a sound (resonator **ding** / mod **clunk**) + animation.
- **Viruses** (`VirusType` ADA / JARVIS): the `Refactorer` action (new `ActionItem.VIRUS` + tuning
  slider) flips an **enemy** portal to the agent's faction (`Portal.refactor` reassigns resonators,
  drops mods) — colour change animates via `CaptureFx`, with a glitchy `playVirusSound`.
- **Items fall on loss**: resonators tumble out as each is destroyed (`removeReso` → `dropResonator`);
  mods tumble out when the portal is neutralized (`destroy` → `dropMods`) or fully shattered
  (`shatterPortal`) — cannon-es physics in `ShatterFx`.
- **Drop rates** are centralized + tunable in `config/DropRates` (single source; future per-game
  override), surfaced in-app via **Menu → Drop rates** and documented (with ~2018 Ingress sources) in
  `docs/MECHANICS.md`. Fixed a long-standing bug where viruses never dropped (integer `1/roll` = 0).
- **3D**: mods render inside the orb at tetrahedron vertices, shaped by type — **dodecahedron**
  (shield) / **pentagonal radiator** (heat sink) / **diagonal cube** (link amp) — rarity-coloured, plus
  a sci-fi **shield bubble** (`ShieldShader`: camera-tracking Fresnel + animated hex lattice + bloom
  tonemap) at φ× the orb when shielded, intensity scaling with mitigation.
- **Keys**: surfaced as counts (leaderboard + inspector); no 3D model yet.

## Settled decisions
- **Modernize Kotlin/JS in place** (not a TS rewrite; not KMP) — keep the ~7k lines of tested logic.
- **MapLibre GL JS** as the map provider (open, keyless).
- **Build on JDK 21**, Kotlin 2.4, Gradle 9.5 (pure tooling choice; ships JS only).
