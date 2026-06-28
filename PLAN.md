# PLAN.md — Q-Gress roadmap (what's next)

Branch: `develop` · Owner: @zirteq

**Future work only.** *Shipped* → [docs/FEATURES.md](docs/FEATURES.md); *how it fits together* →
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md); AI-driver design → [docs/NN.md](docs/NN.md) +
[docs/LLM.md](docs/LLM.md). Completed work lives in the **git log**, not here — keep this file to the point.

## ★ Next session — start here
**Phase D's cheap perf wins + baseline tooling are banked** (flat-array flow fields, squared-distance targeting,
panel throttling, FX physics, `Profiler`/`FpsMeter`/`profiler.sh` — see Phase D); map presets are now sized by
real **km²** (`Sim.sideForArea`, Small default, Giant 3 km² warned) with sub-linear portal counts, and the
default pace was doubled. The next big perf lever (**three.js mesh instancing**) is deferred — reprofile when
things change, don't pre-optimize.

**Phase B (refactor) is the open structural work — pick from:** more **god-object seams** (this session cut
`Portal`→`HackLoot`, `Bootstrap`→`CanvasFactory`+`GameLoop`, `MapController`→`ShadowGridBuilder`; `Sound` (768)
and the rest of `Bootstrap`/`MapController` remain), the **140 → 120 line-length** pass, and the **magic-numbers**
naming pass. The **commonMain pure-logic migration** had a deep pass (cleanly-liftable core moved + tested);
**what's left is blocked by deeper coupling** — `Config` gates `Tournament`/`Observation`/`Cycle`, `Portal`/
`World`/`Agent` gate `Field`/`Inventory`/`knockMods` — so it rides the SoC work, not a separate pass.
Field-layering is verified (`MultilayerFieldTest`); the only remaining piece — nudging the AI to actually
*pursue* layers — is deferred to the next **champion rebake** (see *Gameplay mechanics* / *AI vs AI*).

Optional small wins: a **CSS design-token dedup** (hoist the repeated glass/button magic values — `blur(7px)`,
the `rgba(255,255,255,.06/.12/.18)` button fills, `rgba(0,0,0,.45)`, `border-radius:6px` — into `:root` custom
properties; zero-toolchain, a visual no-op), plus the leftover code dupes (the 3 mod-type `getColorForLevel()`
companions; the two history panels' uPlot series config).

## Non-functional track — quality → coverage → perf (CURRENT FOCUS)
Get the code to a state we're comfortable with, *then* make it fast. Mostly phased, but we took an early,
data-driven **phase D** dip (the baseline tooling + cheap perf wins were too good to defer — see Phase D). **Phase
B** (refactor under the net) remains the open structural focus; phase D's big lever (mesh instancing) is deferred.

### Phase B — Refactor under the net — 🔄 in progress
Functional core / imperative shell, properly. Module-by-module, behind the phase-A safety net.
- [ ] **SoC / split god-objects** — cut along seams in `MapController` (835) / `Bootstrap` (788) / `Sound`
  (768) / `Portal` (685) as they're touched. (`Scene3D` keeps an intentional `LargeClass` suppress — a
  scene-graph hub is legitimately large; the cleanly-separable concerns are already out.)
- [ ] **Reduce magic numbers** — name them / fold into `Config` where it aids clarity (detekt `MagicNumber` is
  off, so this is a by-hand judgement pass, not a gate-chase).
- [ ] **Functional patterns** where they fit; **tighten line length 140 → 120** *alongside* the class
  extractions (it inflates `LargeClass` otherwise).
- **Exit criterion:** pure logic is testable in isolation; gate (ktlint/detekt/tests) stays green throughout.

### Phase D — Profiling & optimization — 🔄 baseline + cheap wins landed (out of strict order, data-driven)
We dipped into phase D ahead of finishing phase B because a profiling pass was cheap and high-value. **Baseline
tooling + the cheap, high-confidence optimizations are done; the big remaining lever is deferred.**
- [x] **Baseline tooling** — `util/Profiler` (world-gen per-stage `[perf]` timing + the per-portal flow-field
  aggregate), `system/ui/FpsMeter` (`?debug` FPS/frame-time overlay), and **`./profiler.sh`** (a zero-dep
  headless-Chrome CDP CPU profiler → `build/profiles/*.cpuprofile`; `tools/profiling/`). Re-run any time.
- [x] **Cheap wins (banked)** — lazy `Complex` (no eager `sqrt`/`atan2` per construction); **flat-array flow
  fields** (`Pathfinding` over `IntArray`/`DoubleArray` + a flat `VectorField`, no `Pos`-keyed HashMaps of boxed
  `Complex`: flow-field build **459 → 29 ms avg**, world-gen **33.5 → 17 s** on a Large+endgame board);
  squared-distance targeting (`Pos.distanceTo2` in the hot per-tick loops); hidden-footer-tab panel throttle
  (`Footer.isActive`); cannon-es FX physics (SAP broadphase + `allowSleep` + a digit collision filter).
- [ ] **The big remaining lever — three.js mesh instancing/merging.** World-gen + steady-state are now dominated
  by mesh construction (`setValueM4` / `Object3D` / link tubes / `generateUUID`). Cutting it is a real refactor
  (instance/merge static meshes), worth it only to push map sizes much further. Plus a ~5% runtime `update`
  still to identify. **Reprofile when things change** — don't pre-optimize.
- [ ] Still open in their sections below: **Building perf + lifecycle**, the **Map-size preset** warning popup +
  per-preset tuning. (The **Pathfinding-scalability** flow-field cost is largely handled by the flat arrays.)

## ⚑ Verify in-browser first (`./start.sh`)
Built headless recently, not yet confirmed on screen — eyeball these, then move on:
- **3D portal names** (`PortalNameTicker`) — **select** a portal in-game → its name rings the orb as spinning
  extruded 3D letters (white, φ-smaller damage-number style, facing sideways). Arabic/Hebrew spin **CCW**, the
  rest **CW**; **no names on the title**. Tune `SPIN_SPEED` / `RADIUS_MARGIN` / `NAME_RING_GAP`; if a name reads
  mirrored as it spins, flip the placement sign in `buildRing` (the spin-direction requirement is separate).
- **Buildings (parallel mode)** — at e.g. **Red Square**: gaps gone; our meshes **and** MapLibre's gap-fillers
  both bob on an XMP. The z-fight + too-dark fixes landed (footprint inset + roof drop + emissive) — confirm
  **no flicker** where the two overlap and the tone matches MapLibre; tune `EMISSIVE_INTENSITY` /
  `INSET_FRAC` / `ROOF_DROP_M` in `OwnBuildings` if needed. `OwnBuildings.PARALLEL_MODE=false` to compare.
- **Hack/glyph centrifuge** — top o-rings tilt out *with* the rods and fall with them on shatter / reso-kill.
- **NET tab** — maximize/collapse compacts it; activation diagram + genome heatmap legible; 16×16 → 4 columns.
- **"Who plays?" onboarding** — grid aligned, selection clearly highlighted, picks take effect in-game.
- **LLM driver** (`WebLlmClient`) — the WebGPU model actually loads + drives a faction (only un-headless bit).
- **TRAIN tab** (`TrainerPanel`) — pick pop/gens/mutation/arch/activation → **Train** evolves a net live (one
  generation per `setTimeout`): the fitness curve climbs, the champion genome preview fills in, and the live
  game **pauses + resumes untouched** afterwards. Confirm **Save champion** + **Install → ENL/RES** take
  effect (the NET tab then shows the installed net thinking). Tune the in-browser defaults
  (`SEED`/`MATCH_TICKS`, pop/gens) in `TrainerPanel` if a generation feels slow.

## North star
Q-Gress becomes an **AI-vs-AI sandbox**: each faction (ENL/RES) is driven by an agent whose **output _is_ the
17 behaviour sliders** — a custom net and/or an in-browser LLM. A human can play any side; any two brains can
be matched. **Desktop-only**; mobile is blocked.

## 3D / rendering
- [ ] **Graphics-settings menu + anti-aliased links.** Add a **Graphics** options group (Menu or onboarding)
  for quality toggles, and use it to **anti-alias the link tubes** — they read jaggy now. Options: bump the
  renderer's `antialias`/`samples` (MSAA) or add a post-process (FXAA/SMAA pass); for the links specifically,
  fatten + feather the tube edges or draw them as anti-aliased lines. Make AA / shadow quality / building cap
  / DEM exaggeration live toggles so low-end machines can dial down and high-end can crank it.
- [ ] **Buildings — per-building replacement** *(the parallel-mode follow-up).* Today both sets render (ours
  on top, MapLibre filling gaps). Cleaner end-state: hide **only** the MapLibre footprints we have our own
  mesh for, so the gap-fillers and our look match and there's no overlap/z-fight. Needs matching our synthetic
  centroid keys to MapLibre feature ids (now that the `openmaptiles` source carries `generateId`) — or a
  custom building layer we fully own.
- [ ] **Terrain-aware shatter ground.** The cannon-es plane is flat z=0, so shards/pole sink to sea level on
  high ground; sample the DEM under the blast (maybe a Menu exaggeration slider); resample if the play area
  moves (ties into the grand-game movable field).
- [ ] **Explosion shader tuning (optional).** GLSL consts in `XmpShaders.VOLUME_FRAG` (`NOISE_FREQ`,
  `DISPLACE`, `DENSITY_GAIN`, `STEPS`) + the rise/grow curve in `XmpBurst.update`; promote to uniforms if the
  fireball needs frequent live tuning.
- [ ] **Pathfinding scalability.** The per-cell churn is solved (phase D: the Dijkstra + vector field + flat
  `VectorField` are now `IntArray`/`DoubleArray`, no `Pos`-keyed HashMaps — flow-field build is ~16× cheaper).
  Still **per-portal full-map**, though: the remaining want is multi-mode nav (flow fields near, cheap nav far)
  + a coarser `pathResolution` lever for very large maps, plus a field viz.
- [ ] **Humanoid glTF models** — people are head-sized spheres at head height today; swap in real models
  (pairs with the colony-management attributes, icebox).

## UI
- [ ] **Vector-field viz toggle** — a Menu **checkbox to turn the flow-field arrow visualization off**
  in-game. Right now the arrows auto-flash on new portals with no off switch; some players will want them gone.
- [ ] **Portal-discovery-rate slider** — discovery (`Cycle.managePortalDensity` / `Config.portalChurnRate`)
  feels too fast now; expose it as a tuning **slider next to the combat-dynamics slider** so the neutral
  portal churn pace is adjustable (it's a system process, not an agent action — see MECHANICS).
- [ ] **Schematic base view** (reuse `SHADOW_STYLE`) + more toggleable info overlays (e.g. a
  movement-penalty heatmap) alongside the existing Terrain toggle.
- [ ] **Per-faction tuning presets** — save/recall named slider sets (the slider↔AI auto-move link is done).
- [ ] **Stage 5 — the polished end-state UI.** A cohesive visual-theme + layout pass over the whole
  HUD/onboarding/menus building on the dock: consistent typography, spacing, panels and states; responsive to
  window size. The "real UI" we want to ship behind.

## Onboarding
- [ ] **Map-size pass.** ✅ mostly landed: presets are now defined by real **play-area in km²** (`Sim.sideForArea`)
  — Tiny 0.1 · Small 0.5 (default) · Mid 1 · Large 2 · Giant 3 — with portal count sub-linear (`suggestedPortals`)
  so the per-portal pathfinding doesn't blow up, and a reserved-space **pacing caution** for Large+ (walking, not
  just FPS). Remaining: (a) per-preset **runtime-FPS tuning** on a real GPU (entity count, not build time, is the
  constraint — find where Large/Giant bite); (b) the **dynamic grow/move the play area mid-game** idea (kept in
  mind during the SimRunner area-decoupling); (c) the deferred **three.js mesh instancing** is what would let the
  big presets run smoothly (see phase D).
- [ ] **Location selection polish** — Home / nearest city via Geolocation; a curated preset list; Random;
  surface the free-form search on the onboarding screen (it only exists in-game now).
- [ ] **Location list import / export** — let the player export the current location catalogue (the
  `Locations` registry / `resources/locations.json`) to a file and import a custom one, so curated place
  sets can be shared without a rebuild. Builds on the already-externalized JSON catalogue + pure parser
  (`Locations.parse`); pairs with the shareable-scenario seam.
- [ ] **Real per-stage load %** (especially flow-field computation).
- [ ] **Initial roster "roll"** — light flavour, not a gacha loop; ties to the icebox rarity tiers.
- [ ] **`?debug` dev tooling** — has the grid self-check + stuck-agent detection + `?debug=capture`. Remaining:
  load-timing/profiling logs; and the handoff — run `?debug=capture` once in-browser, drop the downloaded
  `PresetFixtures.kt` into `src/jsTest/kotlin/util/` and commit, flipping `PresetConnectivityTest` from a
  synthetic harness into a real per-preset audit gate.

## Gameplay mechanics
- [ ] **Field layering — AI nudge (defer to the next champion rebake).** ✅ The *mechanic* is verified:
  `MultilayerFieldTest` pins the directional rules (a portal **under** a field can never link out; an **anchor**
  vertex links *into* the covered portals to close **nested** triangles; a fan sharing two anchors stacks the
  layers and `calcTotalMu` sums them; a covered→outward link is rejected; neutralising a shared anchor collapses
  every layer on it; the **closing** agent owns the field + scores its AP). So the rules are sound — the reason
  agents layer rarely is purely **behavioural**: `Linker.fieldClosingTarget` (Linker.kt:32-35) closes *any*
  triangle with **no preference for nesting**. **To do, when we rebake champions** (it's a fitness-shaping
  change, so it belongs with retraining — see *AI vs AI* below): bias the `Linker` toward links that nest under
  an existing field (anchor-fanning), instrument a headless run for layered-field counts, and re-evaluate. *(Open
  later: a rose-method scenario test; a **link-amp mod** raising the hard 8-link cap for deeper multi-layers.)*
- [ ] **Glyph hacking** — a skill-based hack: **~3× rewards**, but **longer**, **needs skill**, **can fail**.
  Glyph skill (+ portal level) sets odds + duration. The collar animation + glassy sound already land
  (`HackFx`/`Sound`); this is the reward/skill/timing model in `Glypher`/`Portal.tryGlyph` + a glyph skill
  on `agent/Skills`, exposed as a high-risk/high-reward QAction the AI learns to weigh.
- [ ] **Recruiting as an agent skill + items.** Rate is self-balancing now (`Recruiter.selectionWeight` ×
  `Balance.recruitFactor`). Next: a per-agent **skill** (`agent/Skills`) some are better at, plus **items**
  (e.g. *beer* — a temporary recruit boost) — a characterful lever instead of a flat faction rate.
- [ ] **Aim skill (XMP / Ultra-Strike accuracy)** — a per-agent skill: high-aim detonates **closer to portal
  centre** (max damage), low-aim lands **off-centre** (damage falls off with miss distance). Makes the
  small-radius Ultra-Strike reward good aim; feeds the damage calc + blast VFX origin; another AI lever.
- [ ] **Portal-mod follow-ups** (shields/heat-sinks/viruses ship; link amps inactive): heat-sink **instant
  cooldown reset** on attach; a **multi-hack** mod; **activate link amps** (range/outbound/SBUL); the
  **Ultra-Strike** weapon + targeted mod-stripping honouring shield `stickiness`; a **3D key** model; a
  per-game **drop-rate tuning UI** (`DropRates` is centralized — Menu → Drop rates; `docs/MECHANICS.md`).

## Grand game — multiple locations & a living field *(big, exploratory)*
- [ ] **Movable / expandable play field** — the playable area can **grow** or **shift** over a game (captured
  territory / objectives push the boundary). Grid + flow-field + border + overlays already key off
  `Sim.fieldRadius()` / `isInPlayArea`, so the field is the seam to make dynamic (re-mask + re-sample on change).
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
  field compute, so the title sim is instant. Pairs with the Phase-6 grid fixtures.
- [ ] **FreeCamera flight path (optional)** — fly the camera *position* over the terrain while
  `lookAtPoint(centre)` keeps the action framed (today it orbits a fixed centre — MapLibre's default camera
  can't decouple position from look-at without FreeCamera).
- [ ] **Drifting particles + a generative ambient bed** (the defense thunderbolts + wordmark XMP-reaction
  already land — the title inherits the game FX).

## Phase 6 — AI-vs-AI (the payoff)
**Substrate is shipped** (see FEATURES + git log): the policy seam (`FactionPolicy`/`FactionPolicies`), the
`Observation` → `SliderVector` contract, the deterministic headless `SimRunner` match harness + `WorldSnapshot`,
the **custom-net track** (`Net`/`NetArch`/`Evolution`/`NetPolicy`, a baked **16×16** champion that beats the
baseline, JSON genome save/load via `GenomeIO`/`NetStore`, the NET activation + genome viz), the adaptive
`HeuristicPolicy`, the **in-browser LLM track** (`LlmPolicy`/`WebLlmClient` + reasoning panel, browser-only),
per-faction driver selection (top toolbar + the onboarding step), the **visual NN trainer** (resumable
`Evolution.Session` + the **TRAIN** tab `TrainerPanel`), and the **in-game leaderboard** (resumable
`Tournament.Session` + `LeaderboardPanel`, a round-robin ranking). Both eval tools share the `HeadlessRun`
harness (snapshot + tick-pause + `Fx` no-op) so they never disturb the live game.

**Fitness objective:** maximize **summed per-checkpoint MU** (sustained field area), not just final MU — a
team effort to layer fields across the cycle. The net/LLM only re-tunes the 17 sliders at checkpoint cadence;
it does **not** replace per-agent `ActionSelector`.

Remaining:
- [ ] **Lock training to the standard gameplay balance.** Pin the training/eval harness (`MatchSetup` /
  `SimRunner`) to the **shipped default** balance — drop rates, combat dynamics, progress speed — rather than
  the live `GameplayPrefs`/`Config` tunables a player may have moved. One canonical training target → champions
  are **"one fits all"** instead of a champion-per-balance matrix to train, version and pick between. The menu
  sliders stay a *play-time* knob only, never a training input.
- [ ] **Rebake the champions (release prep).** Gameplay/balance has shifted a fair bit since the baked
  **16×16** champion was trained, so a champion trained on the old rules is stale. Retrain against the
  now-locked standard balance and re-commit the genome (`GenomeIO`/`NetStore`). Do this **after** the balance
  lock above (so the bake targets the canonical config) and once gameplay tuning has settled — a champion is
  only as good as the balance it learned.
- [ ] **Grid fixtures** — infra is built (`GridFixture` RLE + `GridCapture` ?debug=capture + the
  `PresetConnectivityTest` audit). Only the committed `PresetFixtures.kt` is missing: run `?debug=capture` once
  in-browser, drop the download into `src/jsTest/kotlin/util/`, commit. (The trainer/leaderboard already run on
  the live `World.grid`, so this only feeds the offline connectivity audit.)
- [ ] **Per-side net-architecture / variant pick in onboarding** — *blocked*: only meaningful once there's a
  library of trained nets per `NetArch` (a net driver of an untrained arch just plays randomly). Fold into the
  icebox "download/upload + saved-net library" item below.
- [ ] **icebox — download / upload trained nets.** `GenomeIO` JSON-ables a genome+arch (~16 KB at 16×16): a
  "download champion" (Blob → file) + "load from file/paste", and a small saved-net library — share/version
  nets outside `localStorage`.
- [ ] **icebox — genome/action-set versioning.** Once training is serious AND the action set grows (more
  `QActions`/observation slots), stamp a **schema version** in the genome JSON and refuse/migrate incompatible
  nets (the `inputs`/`outputs` dim check is the seed). Not needed pre-release while the layout churns.

**Cross-cutting AI risk:** pure win-maximizing self-play may rediscover the recruit-rush degenerate (below) —
keep tuning `Config` and consider shaping fitness for *interesting* play (a follow-up lever, not v1).

## Open engineering decisions
*Rationale for the pending **Non-functional track** items (above); the track sequences them.*
- **Tighten max line length 140 → 120.** Deferred: ktlint auto-wrapping inflates detekt's `LargeClass` count,
  so it must land alongside the class extractions (`Scene3D`, and any near the 600-line cap like
  `Bootstrap`/`MapController`). A dedicated refactor pass. → *phase B.*
- **Extract the demo/showcase subsystem from `Scene3D`** — most of Scene3D's `LargeClass` bulk is the
  self-contained sandbox code; move it to a `Showcases` object (build helpers go `internal`), then drop the
  `LargeClass` suppress. → *phase B.*

## Balance risk (recruit-rush)
`ActionSelector` picks by weighted-random over `slider × weight`; recruiting adds agents (→ more actions/tick →
a compounding snowball), so a recruit-rush can beat balanced play. Mitigations are in (XM cost + diminishing
returns; `Balance.recruitFactor` self-corrects roster sizes; `Balance.attackBoost` lets the side behind flip
the board; fair shuffled agent order). Deeper "no single strategy dominates" validation stays iterative
(playtest + the headless harness); **self-play fitness shaping** is the AI-era lever.

## Under consideration (icebox)
- **Mini-map (top-down, north-up, fields always visible).** A small fixed overlay that renders the play area
  from an **exact top-down** view that's **always facing north** — independent of the main 3D camera's pan/
  tilt/rotation — so the **control fields** (and portals/links) stay legible at a glance even while the main
  view is pitched and turned. A constant strategic read of who owns what, where the fields are layered, and
  where the action is. (Could reuse the field/link geometry against a simple north-up projection of
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
- **Sound-design pass → 303 / 808 / 909.** The detonations now lead with a deep, hard 909 kick + reverb send
  (`KickDrum`/`AudioFx`); take the rest of the palette the same way — drum-machine-flavoured hits (808 toms/
  claps, 909 hats) for deploys/links/checkpoints and acid-303 sweeps for viruses/power-ups, a shared reverb/
  delay bus, and maybe a subtle musical bed. Tune the kick (`XMP_KICK_HZ`/`US_KICK_HZ`, `KickDrum` consts) +
  global `AudioFx.setReverbMix` to taste; the `#audio` demo is the dial-in surface.
- **TTS announcements (low pri)** — speak key events (captures, fields, cycle changes) via `speechSynthesis`,
  throttled, per-faction voices, off by default behind a toggle + master volume.
- **Modern Ingress (post-2018), optional** — most-aligned first: **Machina** (the red AI "third faction" — the
  closest analogue to our AI-vs-AI north star: a rules-driven faction that spawns, auto-links, decays, is easy
  to clear); the **hacking-economy triad** (heat-sink / multi-hack / Fracker + ITO) shaping farm rate; **drones**
  (remote hack). Skip the real-world/social bits.
- **Evaluate NVIDIA Komodo** (per user) — investigate fit (rendering / AI / acceleration?) before committing.
- **Legacy 2D TODOs still open** — smarter agent behaviour (more destinations, **swarming**); an
  inventory/capacity limit; an FPS/perf readout (pairs with `?debug`); unit tests for fielding + deploying.

## Known glitches (low priority)
- **Passability overlay not visible on terrain.** The Menu → **Passability** overlay
  (`PassabilityOverlay`, a textured ground quad at a fixed low z `OVERLAY_Z`) doesn't follow the DEM, so on
  elevated terrain it sits *below* the raised ground and is hidden. Fix: drape it on the sampled terrain
  heights (`Scene3D.groundZ`) like the objects do, or lift/conform the quad to the DEM. Same root cause as the
  *Terrain-aware shatter ground* item (3D / rendering).

## Constraints / agreements
- Commit to `develop`; **no pushing** until something works end-to-end.
- Keep `CLAUDE.md` + this file + `docs/` current as work lands; no overlapping info between them (future →
  here, shipped → FEATURES, how → ARCHITECTURE).
- Desktop-only; do not invest in mobile support.
