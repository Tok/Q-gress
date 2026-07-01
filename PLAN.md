# PLAN.md — Q-Gress roadmap (what's next)

Branch: `main` · Owner: @zirteq

**Future work only.** *Shipped* → [docs/FEATURES.md](docs/FEATURES.md); *how it fits together* →
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md); AI-driver design → [docs/NN.md](docs/NN.md) +
[docs/LLM.md](docs/LLM.md). Completed work lives in the **git log**, not here — keep this file to what's left
to do, not what's been done.

## North star
Q-Gress is an **AI-vs-AI sandbox**: each faction (ENL/RES) is driven by an agent whose **output _is_ the
17 behaviour sliders** — a custom net and/or an in-browser LLM. A human can play any side; any two brains can
be matched. **Desktop-only**; mobile is blocked. (The substrate ships; what's left is tuning + the rebake.)

## Phase B — functional core / imperative shell — ✅ DONE
The structural focus: functional core / imperative shell, module-by-module, gate green throughout. Exit
criterion — *pure logic testable in isolation* — **met**: the whole game core (entities, action machine,
side-effect seams, **and the `SimRunner` match/eval cluster**) lives in `commonMain`, JVM-measured at
**~96%** (`./scripts/coverage.sh`). The only game logic still in jsMain is the genuine imperative shell
(renderer/audio/DOM engines + the platform seam impls) and the **net-driver storage/serialization** — `ai.net`
(`GenomeIO`'s `dynamic` JSON, `NetStore`'s `localStorage`) — which would want a serialization + storage seam
to lift (optional; only the net `Champion`/`Evolution` tests still run JS-only). History below for reference:
- **The commonMain pure-logic lift — DONE (the whole functional core is now in `commonMain`).** The entity
  SCC moved: `World`, `agent.{Agent,NonFaction,Movement,Inventory,Balance}`, `portal.{Portal,Field,Link,
  ResonatorSlot,PortalKey,PortalHacks,HackLoot}`, the item entity classes (`items.{XmpBurster,PowerCube,
  UltraStrike,RewardVisual}` + `items.deployable.*`), the action machine (`agent.action.{Action,ActionItem,
  ActionSelector}` + `agent.action.cond.*`), plus `util.data.{Positions,PosExt,CellExt}` and `extension.Slots`.
  This came after the earlier leaf lifts (the math core; the `config/` package behind the `config.Platform`
  seam; the `items/` data layer; `Checkpoint`/`HackResult`/`ModSlot`/`XmMap`/`XmHeap`/`Skills`/`AgentSize`/
  `StuckTracker`/`Circle`/`Dim`/`VectorField`/`GridMap`; `system.Com`; `util.PortalNameGen`;
  `agent.action.HackTiming`; `ai.FactionPolicy`+registry).
  - **Side-effect seams (the imperative shell).** Four mirror the same shape — a commonMain interface +
    `<accessor>.sink` + a `NoOp*` default, with the jsMain platform impl `bind`-installed at boot
    (`Bootstrap.load`): **visual** `system.effect.{Effects,Fx}`→`BrowserEffects`; **audio**
    `system.audio.{Audio,Snd}`→`BrowserAudio`; **flow-field compute** `system.grid.{FieldFlow,Nav}`→
    `PathFieldFlow` (also bound per `SimRunner` match); **map naming** `util.{PortalNamer,Names}`→
    `MapPortalNamer`. Host facts go through `config.Platform` (`isBrowser`/`locationName`/…); diagnostics
    through `util.Log`; the AI slider read through `ai.FactionPolicies.defaultPolicy`→`DomSliderPolicy`.
    Accessors default to NoOp headless, so the Node/JVM tests + `SimRunner` run the full tick loop with no
    renderer/audio/DOM.
  - **The jsMain shell keeps only** the platform impls above, the JS-canvas `agent.action.ActionIcons` /
    `agent.qvalue.QIcons` / `util.data.CellOverlay`, `util.data.GeoCoordsExt`, and the renderer/audio/DOM/
    MapLibre engines. (`Scene3D` keeps an *intentional* `LargeClass` suppress — its entity-sync +
    effect-dispatch bulk is irreducibly bound to the three.js groups.)
  - **Coverage: ~96%** (JVM Kover, `./scripts/coverage.sh`) — over the codecov green threshold. Got there with
    commonTest unit-test batches over the whole lifted core (post-lift baseline was ~60%): the entity/action
    machine (`Agent`/`Portal`/`World`/`NonFaction`/`Movement`/`Inventory`/`cond.*`), item damage/deploy, the
    data/enum/util leaves, and the `SimRunner`-cluster integration path. Remaining ~4% is genuinely
    hard/unreachable — browser-only `Platform.isBrowser()` branches, the net-driver storage (jsMain), and a few
    low-value edge paths (huge-field TTS, uniqueName numeral fallback).
  - **`SimRunner` cluster lift — DONE.** `ai.{SimRunner,Tournament,Observation,HeuristicPolicy}` +
    `system.{Cycle,WorldSnapshot,Simulation}` are in commonMain; `SimRunnerTest`/`TournamentTest`/
    `WorldSnapshotTest` run on the JVM. Enabled by the **`Pathfinding` sync-split** (pure core → commonMain,
    coroutine `PathfindingAsync` → jsMain) + `SyncFieldFlow` injected via `SimRunner.fieldFlow` (Bootstrap
    swaps in the async `PathFieldFlow` for in-browser evals), and hoisting the default-balance constants into
    `Config`. Still jsMain: the **`ai.net` driver** (`GenomeIO` JSON, `NetStore` localStorage) — a
    serialization + storage seam would let `EvolutionTest`/net tests run on the JVM too (optional).
  - **Eyes-on still owed:** one `./start.sh` pass to confirm the seam boot-binds (FX / audio / flow-field
    flash / real portal names) + the in-browser trainer/eval still compute fields, after the accessor rewire.
- **Reduce magic numbers** — name them / fold into `Config` where it aids clarity (detekt `MagicNumber` is
  off → a by-hand judgement pass, not a gate-chase).
- **Tighten line length 140 → 120** — land *alongside* the class extractions (auto-wrapping inflates
  `LargeClass` otherwise).

## Perf — the big deferred lever
- [ ] **three.js mesh instancing / merging / persistent sync.** `sync()` clear+recreates the portal / field /
  agent groups **every tick** — that per-tick mesh construction (`setValueM4` / `Object3D` / `generateUUID`) is
  the steady-state cost. The diff-sync pattern (reuse + reposition, add new, remove gone — already used for the
  link meshes + NPC spheres) is the lever; extend it to **portals** (poles/orbs/resos/mods — the heaviest group,
  but entangled with grow-in/hack/tumble animation → persist the static skeleton, keep the animated bits
  updating) and **fields**. Also **world-gen** mesh construction (profiled: portals + agents/NPCs dominate a
  ~17 s gen). True GPU `InstancedMesh` is blocked for links (custom `GlassShader` needs `instanceMatrix`) + resos
  (per-frame animation); viable only for standard-material static parts if draw calls turn out to dominate.
  **Reprofile when things change.** Tooling: `util/Profiler`, `?debug` `FpsMeter`, `./scripts/profiler.sh`
  (→ `build/profiles/*.cpuprofile`).

## 3D / rendering
- [ ] **Graphics-settings menu — more levers.** The group exists (a live, persisted **High-detail shadows**
  toggle via `GraphicsPrefs`). Add: a **building cap**, **DEM exaggeration**; surface the group in onboarding
  too. (Anti-aliasing is iceboxed — see below.)
- [ ] **Buildings — per-building replacement.** Both sets render (ours inset on top, MapLibre fills gaps); the
  want is to hide **only** the MapLibre footprints we've meshed, so there's no overlap/z-fight. Needs matching
  our synthetic centroid keys to MapLibre feature ids (the `openmaptiles` source carries `generateId`) — or a
  custom building layer we fully own.
- [ ] **Explosion shader tuning (optional).** GLSL consts in `XmpShaders.VOLUME_FRAG` (`NOISE_FREQ`,
  `DISPLACE`, `DENSITY_GAIN`, `STEPS`) + the rise/grow curve in `XmpBurst.update`; promote to uniforms if the
  fireball needs frequent live tuning.
- [ ] **Pathfinding scalability.** Flow fields are still **per-portal full-map**: the want is multi-mode nav
  (flow fields near, cheap nav far) + a coarser `pathResolution` lever for very large maps, plus a field viz.
- [ ] **Humanoid glTF models** — people are head-sized spheres at head height today; swap in real models
  (pairs with the colony-management attributes, icebox).

## UI
- [ ] **Schematic base view** (reuse `SHADOW_STYLE`) + more toggleable info overlays (e.g. a
  movement-penalty heatmap) alongside the existing Terrain toggle.
- [ ] **Stage 5 — the polished end-state UI.** A cohesive visual-theme + layout pass over the whole
  HUD/onboarding/menus building on the dock: consistent typography, spacing, panels and states; responsive to
  window size. The "real UI" we want to ship behind.

## Onboarding
- [ ] **Map-size per-preset tuning + dynamic field.** Presets are km²-based (Tiny 0.1 · Small 0.2 default ·
  Mid 0.5 · Large 1 · Giant 2; Large + Giant warned at ≥ 1 km²) with sub-linear portal counts + size-scaled
  rosters. Remaining: (a) per-preset **runtime-FPS tuning** on a real GPU (entity count, not build time, is the
  constraint — find where Large/Giant bite); (b) the **dynamic grow/move the play area mid-game** idea (kept in
  mind during the SimRunner area-decoupling); (c) **mesh instancing** is what would let the big presets run
  smoothly (see Perf).
- [ ] **Location selection polish** — Home / nearest city via Geolocation; a curated preset list; Random.
  *(Done: the onboarding LOCATION step now has a keyless OSM/Nominatim free-form search + a live name/place/
  country readout + editable lng/lat.)* Remaining: a nearest-city Home refinement + a curated shortlist.
- [ ] **Location list import / export** — let the player export the current location catalogue (the
  `Locations` registry / `resources/locations.json`) to a file and import a custom one, so curated place
  sets can be shared without a rebuild. Builds on the externalized JSON catalogue + pure parser
  (`Locations.parse`); pairs with the shareable-scenario seam.
- [ ] **Real per-stage load %.** The single-async-wait stages (map/street/shadow/grid) still **creep** rather
  than report true progress. Wire real signal where the APIs allow it — esp. **flow-field computation**
  (per-portal field-build counts) and MapLibre tile-load events — so those stages fill for real.
- [ ] **Initial roster "roll"** — light flavour, not a gacha loop; ties to the icebox rarity tiers.
- [ ] **`?debug` dev tooling — remaining.** Load-timing/profiling logs; and the handoff: run `?debug=capture`
  once in-browser, drop the downloaded `PresetFixtures.kt` into `src/jsTest/kotlin/util/` and commit, flipping
  `PresetConnectivityTest` from a synthetic harness into a real per-preset audit gate.

## Gameplay mechanics
- [ ] **Field layering — AI nudge (defer to the next champion rebake).** The *mechanic* is verified
  (`MultilayerFieldTest`); agents layer rarely for a purely **behavioural** reason: `Linker.fieldClosingTarget`
  (Linker.kt:32-35) closes *any* triangle with **no preference for nesting**. When we rebake champions (a
  fitness-shaping change → belongs with retraining, see *AI-vs-AI*): bias the `Linker` toward links that nest
  under an existing field (anchor-fanning), instrument a headless run for layered-field counts, re-evaluate.
  *(Open later: a rose-method scenario test; a **link-amp mod** raising the hard 8-link cap for deeper layers.)*
- [ ] **Glyph hacking** — a skill-based hack: **~3× rewards**, but **longer**, **needs skill**, **can fail**.
  Glyph skill (+ portal level) sets odds + duration. The collar animation + glassy sound already land
  (`HackFx`/`Sound`); this is the reward/skill/timing model in `Glypher`/`Portal.tryGlyph` + a glyph skill
  on `agent/Skills`, exposed as a high-risk/high-reward QAction the AI learns to weigh. Drive the glyph count +
  hack duration off portal level (Ingress wiki: L1 = 1 glyph / 20 s … L8 = 5 glyphs / 15 s; perfect-hack +
  speed bonuses) via the `glyph/Glyph` enum; the TTS "glyph" tier already reads the sequence back.
- [ ] **Recruiting items** — a temporary recruit-success boost item (e.g. *beer*) on top of the existing
  idle-fallback recruiting (an idle agent recruits a nearby NPC, capped per faction; success scales with
  progress speed + the per-agent `Skills.recruitingFactor`).
- [ ] **Aim skill (XMP / Ultra-Strike accuracy)** — a per-agent skill: high-aim detonates **closer to portal
  centre** (max damage), low-aim lands **off-centre** (damage falls off with miss distance). Makes the
  small-radius Ultra-Strike reward good aim; feeds the damage calc + blast VFX origin; another AI lever.
  *(Wants an eyes-on session: it rewrites the live combat-damage model + blast VFX origin — verify visually.)*
- [ ] **Portal-mod follow-ups** — **activate link amps** (range/outbound/SBUL); the **Ultra-Strike** weapon +
  targeted mod-stripping honouring shield `stickiness`; a **3D key** model; a per-game **drop-rate tuning UI**
  (`DropRates` is centralized — Menu → Drop rates; `docs/MECHANICS.md`). When drop rates become player-tunable,
  also fold them into the training balance lock (`MatchSetup.useDefaultBalance`).

## Grand game — multiple locations & a living field *(big, exploratory)*
- [ ] **Movable / expandable play field** — the playable area can **grow** or **shift** over a game (captured
  territory / objectives push the boundary). Grid + flow-field + border + overlays + the cannon-es shatter
  ground already key off `Sim.fieldRadius()` / `isInPlayArea` / `groundZ`, so the field is the seam to make
  dynamic (re-mask + re-sample on change).
- [ ] **Multiple linked locations (a "grand game")** — run several real-world locations at once: **one focused
  sim** at full fidelity + **off-site locations in a simplified/abstract form** (aggregate MU/portal counts,
  cheap tick, no 3D) to bound cost. Locations connect (shared roster, cross-site links/objectives).
- [ ] **Roster management across sites** — a player **roster of ~16–32** spread over the locations,
  allocated/moved between the focused sim and the off-sites — a meta layer over the sliders (the AI should
  reason at both the local-tactical and roster/strategic level). *Open:* the off-site model (pure stats vs
  coarse grid), travel/relocation cost, and how cross-site links/fields score.

## Title / faction screen
- [ ] **Precompute the title world** — serialize the fixed title location's grid + portal positions + flow
  fields (extend the `GridCapture` fixture pattern) and load them instead of the live shadow-readback + async
  field compute, so the title sim is instant. Pairs with the grid fixtures below.
- [ ] **FreeCamera flight path (optional)** — fly the camera *position* over the terrain while
  `lookAtPoint(centre)` keeps the action framed (today it orbits a fixed centre — MapLibre's default camera
  can't decouple position from look-at without FreeCamera).
- [ ] **Drifting particles + a generative ambient bed** (the defense thunderbolts + wordmark XMP-reaction
  already land — the title inherits the game FX).

## AI-vs-AI (the payoff)
**Fitness objective:** maximize **summed per-checkpoint MU** (sustained field area), not just final MU — a
team effort to layer fields across the cycle. The net/LLM re-tunes the 17 sliders at checkpoint cadence; it
does **not** replace the per-agent `ActionSelector`. (Training/eval is pinned to the shipped default balance via
`MatchSetup.useDefaultBalance`, so champions are "one fits all".)

- [ ] **Rebake the champions (release prep).** Gameplay/balance has shifted since the baked **16×16** champion
  was trained, so it's stale. Retrain against the locked standard balance and re-commit the genome
  (`GenomeIO`/`NetStore`). Do this once gameplay tuning has settled — a champion is only as good as the balance
  it learned. Fold in the **field-layering Linker nudge** above.
- [ ] **Grid fixtures** — infra is built (`GridFixture` RLE + `GridCapture` ?debug=capture +
  `PresetConnectivityTest`). Only the committed `PresetFixtures.kt` is missing: run `?debug=capture` once
  in-browser, drop the download into `src/jsTest/kotlin/util/`, commit. (Feeds the offline connectivity audit;
  the trainer/leaderboard already run on the live `World.grid`.)
- [ ] **Per-side net-architecture / variant pick in onboarding** — *blocked*: only meaningful once there's a
  library of trained nets per `NetArch` (a net of an untrained arch plays randomly). Fold into the
  download/upload + saved-net library item below.
- [ ] **icebox — download / upload trained nets.** `GenomeIO` JSON-ables a genome+arch (~16 KB at 16×16): a
  "download champion" (Blob → file) + "load from file/paste", and a small saved-net library — share/version
  nets outside `localStorage`.
- [ ] **icebox — genome/action-set versioning.** Once training is serious AND the action set grows (more
  `QActions`/observation slots), stamp a **schema version** in the genome JSON and refuse/migrate incompatible
  nets (the `inputs`/`outputs` dim check is the seed). Not needed pre-release while the layout churns.

## Under consideration (icebox)
- **Anti-aliasing (the terrain custom-layer problem).** Deferred (not critical). Under 3D terrain MapLibre
  renders the three.js custom layer to an offscreen texture, so the GL-context `antialias` (MSAA) never reaches
  it; live `setPixelRatio` SSAA desyncs the layer's screen-space (objects shift on toggle). When revisited, do it
  **hands-on in-browser**: likely **construction-time SSAA** (`pixelRatio` at map creation, applied on reload —
  avoids the live-resize desync), or an **FXAA** post-process threaded into the terrain compositing. The Graphics
  menu group + `GraphicsPrefs` are in place to host a working toggle.
- **Mini-map (top-down, north-up, fields always visible).** A small fixed overlay that renders the play area
  from an **exact top-down** view that's **always facing north** — independent of the main 3D camera's pan/
  tilt/rotation — so the **control fields** (and portals/links) stay legible at a glance even while the main
  view is pitched and turned. (Could reuse the field/link geometry against a simple north-up projection of
  `Sim.fieldRadius()` / the grid; pairs with the movable/expandable play field.)
- **Weather (gameplay + atmosphere).** Rain/fog/snow/day-night as a visual layer **and** a modifier (rain
  drains agent battery faster, fog cuts hack/attack range, snow slows movement) — random/seasonal or from a
  real weather API for the location. Pairs with the colony-management battery idea.
  - **A directional sun** (with time-of-day): a real key light so chrome poles cast highlights + terrain gets
    shading; plus a render-to-cubemap/PMREM of sky+terrain so chrome/glass reflect the *actual* scene (today a
    static gradient env in `Materials`). Sun direction drives shadow mood.
- **Colony-management / roster.** Per-entity attributes (endurance/speed/agility/radius on `agent/Skills` +
  `AgentSize`); **rarity-tiered agents** (randomised attributes, **no gambling UX** — manage composition, not a
  gacha); **items** (skateboards, **jet-skis** → makes marina/bridge presets playable, power-banks, second
  phones); **battery %** (depleted phone → the player leaves). Pairs with the 3D humanoid work.
- **Movement/pathfinding rework.** Derive walkability/penalties from **vector-tile road geometry** (features /
  GeoJSON) and/or a navmesh instead of reading rasterized shadow pixels — decouples the sim from the screen and
  unblocks dynamic zoom + a pitched/3D camera. Natural partner of the functional-core split. (The
  pbf/`@mapbox/vector-tile` decoder in `external/VectorTile.kt` can pull road/water/landcover layers.)
- **Portal-name ticker — non-latin / RTL font.** `PortalNameTicker` reuses Coda (latin + digits), so
  Arabic/Hebrew/CJK names can't be drawn — RTL names are currently **suppressed** (`isRtl`), latin renders.
  To support them: load an RTL/CJK-capable typeface.json (or per-script fonts chosen by script detection)
  alongside Coda, add Arabic shaping/joining, and re-enable the CCW spin for RTL.
- **Walkable roofs / per-building destruction** — now that we mesh every building (`OwnBuildings`), agents
  could path over roofs, buildings could be destructible, etc.
- **Building perf + lifecycle** — (a) a dense streamed city can reach 1000s of shadow-casting meshes; if FPS
  suffers, **merge** static buildings into a few `BufferGeometry` batches (keep play-area ones individually
  shakeable); (b) wire `OwnBuildings.clear()` + `BuildingStream.reset()` into world-regen (they exist, unused);
  (c) a per-bbox **Overpass response cache** so a long fly-around doesn't hammer the public instance; (d) widen
  / view-follow the sun's ortho shadow camera so **streamed** buildings beyond the play area cast/receive.
- **Sound-design pass → 303 / 808 / 909.** Detonations already lead with a deep, hard 909 kick + reverb send
  (`KickDrum`/`AudioFx`/`BlastSound`); take the rest of the palette the same way — 808 toms/claps + 909 hats
  for deploys/links/checkpoints, acid-303 sweeps for viruses/power-ups, a shared reverb/delay bus, maybe a
  subtle musical bed. Tune the kick (`XMP_KICK_HZ`/`US_KICK_HZ`, `KickDrum` consts) + global
  `AudioFx.setReverbMix` to taste; the `#audio` demo is the dial-in surface.
- **Modern Ingress (post-2018), optional** — most-aligned first: **Machina** (the red AI "third faction" — the
  closest analogue to our AI-vs-AI north star: a rules-driven faction that spawns, auto-links, decays, is easy
  to clear); the **hacking-economy triad** (heat-sink / multi-hack / Fracker + ITO) shaping farm rate; **drones**
  (remote hack). Skip the real-world/social bits.
- **Evaluate NVIDIA Komodo** (per user) — investigate fit (rendering / AI / acceleration?) before committing.
- **Swarming agent behaviour** — agents coordinate/cluster toward shared objectives (a flocking / group-movement
  layer + more destination variety) instead of each pathing solo. The last open lever from the old behaviour list.

## Constraints / agreements
- Working directly on `main` for now; **no pushing** until something works end-to-end.
- Keep `CLAUDE.md` + this file + `docs/` current as work lands; no overlapping info between them (future →
  here, shipped → FEATURES, how → ARCHITECTURE).
- Desktop-only; do not invest in mobile support.
</content>
</invoke>
