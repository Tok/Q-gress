# PLAN.md — Q-Gress roadmap (what's next)

Branch: `develop` · Owner: @zirteq

**Future work only.** *Shipped* → [docs/FEATURES.md](docs/FEATURES.md); *how it fits together* →
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md); AI-driver design → [docs/NN.md](docs/NN.md) +
[docs/LLM.md](docs/LLM.md). Completed work lives in the **git log**, not here — keep this file to the point.

## ★ Next session — start here
**Line-coverage target met (phase C) — backfill the rest of `commonMain`, then resume phase B.** The Kover
report over the shared core now reads **~97.6% line coverage** (was ~78%): a `commonTest` backfill pinned the
previously-uncovered pure data/enums — `config.IngressFacts` (0→100%, a "DO-NOT-EDIT reference table" pin),
`agent.Faction` (3→100%, migrated `FactionTest` jsTest→commonTest so Kover counts it), `config.Time` (54→100%),
plus the last `Rng` overloads. Codecov should flip from the red ~71% toward green on the next CI run. The
remaining ~2% is auto-generated data-class members (`Pos`/`Line`) and the platform `freshSeed` actual — low
value, leave it. **From here:** the lever for *more* coverage is **migrating more pure logic into `commonMain`**
(phase B's functional-core split), which then gets `commonTest` + Kover for free. The **`Scene3D` → `Showcases`
split** (phase B) is the next big structural piece, then the 140 → 120 line-length pass. Work in phase order:
**A** safety-net tests (done) → **B** refactor (+ functional-core split, ← resume here) → **C** coverage to
target (met) → **D** profiling & optimization. The perf items elsewhere (Pathfinding scalability, Building perf,
Map-size profiling) are **phase D** inputs — don't pull them forward. One gameplay item also queued:
**field-layering tests + tuning investigation** (under *Gameplay mechanics* — the rules are sound, but agents
layer too rarely).

A **code-duplication pass** just landed (shared `util.ColorUtil` hex helpers, `system.display.Vec3` +
`ShaderUtil`, `util.ui.Dom` `el()` factory, `util.Prefs` localStorage plumbing). Minor dupes still open if
wanted: the 3 mod-type `getColorForLevel()` companions and the two history panels' uPlot series config.

## Non-functional track — quality → coverage → perf (CURRENT FOCUS)
The push to get the code to a state we're comfortable with, *then* make it fast. Strictly phased: each phase
de-risks the next. "Coverage" means two things and they split across phases — a behavioural **safety net** comes
first (phase A, in the existing Node harness); real **line-coverage measurement** (Kover) needs the
functional-core split, so it rides along with phase B — the `commonMain` + `jvm()` + Kover scaffolding is now
up (phase C) and grows as each pure module migrates.

### Phase A — Safety net (behavioural tests on today's behaviour) — ✅ done
Lock current behaviour before moving any code, so the refactor can't silently change it. Pure Node/Mocha tests
(no tooling change). Characterization tests over the functional core as-it-is:
- [x] `Balance` — `recruitFactor` / `attackBoost` (deficit² steepness) / `leadShare` zero-guard.
- [x] `Config` formulas — `npcPopulation`, `targetPortals`, `maxMitigation`, `weaponDropMultiplier`,
  `attackXmpThreshold`, `comebackAttackBonus` (`ConfigTest` pins the input→output table at the current consts).
- [x] `ActionSelector.q` weighting (composition + field-maker ordering); `StuckTracker` already covered.
- [x] `Util` — the seeded mulberry32 RNG + the weighted `select` contract (`UtilTest`).
- [x] `Field` MU/area (Heron ÷100 floored) + `isCoveringPortal` (the fitness-objective math).
- [x] `Recruiter` self-balancing `selectionWeight` + diminishing `recruitSuccessProbability`.
- [x] `SimRunner` determinism — already covered (`sameSeedIsDeterministic`, `reproducibleAfterAnotherMatch`).
- [x] `MovementUtil` — `headingTo` (pure) + the **wander never leaves the map** invariant (passable + in-play-area
  destination, from centre and edge), via a rectangle field + a passable `GridFixture` (restored per test).
- **Exit criterion (met):** a green net that fails if behaviour changes. (Suite now **260** tests, all green.)

### Phase B — Refactor under the net — 🔄 in progress
Functional core / imperative shell, properly. Do it module-by-module behind phase-A tests:
- [~] **Isolate pure logic** — extract decision functions (state in → value/decision out) and push effects to the
  edges. Done so far (each guarded by a new unit test): `Cycle.churnChances`, `Portal.linkMitigationFor`,
  `Portal.retaliationDamage`, `Recruiter.recruitSuccessProbability`, `NonFaction.opposingHalf` (off-map bearing),
  `Portal.cooldownAfter` + `Portal.isBurnedOut` (hack cooldown/burnout), `Agent.enemyPortalsInRange` (attack
  targeting, `World` read inverted to a param). **Remaining candidates:** `Linker.fieldClosingTarget` (a trivial
  set-intersection pick — low value); `Deployer.deployTargetFor` was assessed and **skipped** (it mixes agent-level
  + portal reads — not cleanly pure, and `DeployerTest` already covers it). The cheap, high-signal extractions are
  largely done; the **`commonMain` move** is complete (below) and the **Scene3D split** remains.
- [x] **Incremental functional-core split** — the pure logic now lives in `commonMain` (compiled for both JS
  and the test-only `jvm()` target, Kover-covered ~78%; the World/`Config`/`Portal`-coupled holders delegate).
  **Migrated:** `util.MathUtil`, `util.Rng` (seedable PRNG + `select`, `freshSeed` via expect/actual),
  `config.Time`, `config.IngressFacts`, `config.ConfigMath`, `portal.Cooldown`, `portal.PortalMath`,
  `portal.FieldMath`, `system.ChurnMath`, `agent.BalanceMath`, `agent.Faction`, `agent.NonFactionMath`
  (`opposingHalf`), and the geometry core (`util.data.Pos` + `util.data.Line`, with grid/geo/RNG members
  split to `PosExt`/`Positions` in the shell). **Intentionally left in `jsMain`:** `Agent.enemyPortalsInRange`
  (references `Portal`, a JS-shell type — already a tested pure function there).
- [ ] **Reduce magic numbers** — name them / fold into `Config` where it aids clarity (detekt `MagicNumber` is
  off, so this is a by-hand judgement pass, not a gate-chase). *(Started opportunistically — e.g. named the
  `LINK_MITIGATION_SCALE`.)*
- [ ] **SoC / split god-objects** — `Scene3D` (1579 → extract the demo/showcase code to `Showcases`, drop the
  `LargeClass` suppress), and cut along seams in `MapUtil` (835) / `HtmlUtil` (788) / `SoundUtil` (768) /
  `Portal` (685) as they're touched. *(The big one — not started.)*
- [x] **Null-safety hardening** — `!!` audit complete: **zero** `!!` in `src/jsMain` (the convention has held).
- [x] **De-duplication pass** — hoisted copy-pasted helpers into shared homes: `util.ColorUtil.hexToRgb` /
  `blendHex` (3 shaders + 2 panels), `system.display.Vec3` + `ShaderUtil.glsl` (BoltFx/TitleWordmark + shaders),
  `util.ui.Dom.el()` (14 footer panels), `util.Prefs` localStorage plumbing (5 prefs stores). *Minor leftovers:*
  the 3 mod-type `getColorForLevel()` companions; the two history panels' uPlot series config.
- [ ] **Functional patterns** where they fit; **tighten line length 140 → 120** *alongside* the class
  extractions (it inflates `LargeClass` otherwise).
- **Exit criterion:** pure logic is testable in isolation; gate (ktlint/detekt/tests) stays green throughout.

### Phase C — Real coverage measurement (enabled by B)
- [x] Stand up **Kover on a `jvm()` test target** over the `commonMain` functional core (`build.gradle.kts`:
  `jvm()` + `kotlinx-kover` 0.9.8 + JUnit5; `koverHtmlReport` / `koverLog`). The shared-core tests run on
  **both** jsNodeTest and jvmTest.
- [x] **Hook up Codecov via the GitHub pipeline** — CI emits `koverXmlReport` and uploads via
  `codecov/codecov-action@v5` (`CODECOV_TOKEN` set; `codecov.yml` keeps status informational); README carries
  the badge. **Live and reporting ~71%.**
- [x] **Drive coverage past 90%** — Kover over `commonMain` now reads **~97.6%** (was ~78%). `commonTest`
  backfill pinned the last uncovered pure data: `IngressFacts` (0→100%), `Faction` (3→100%, `FactionTest` moved
  jsTest→commonTest so Kover counts it), `Time` (54→100%), + the remaining `Rng` overloads. The leftover ~2% is
  auto-generated data-class members + the `freshSeed` actual (left intentionally). Codecov flips red→green next
  CI run. Pushing *higher* now means migrating more pure logic into `commonMain` (phase B) — it then gets
  `commonTest` + Kover for free. Effects/shell stay Node-side and aren't counted.

### Phase D — Profiling & optimization (last)
Only once we're comfortable with structure + coverage. **Baseline first, then optimize, guarded by the net:**
- [ ] **Establish a baseline** — an FPS / frame-time readout (ties to `?debug`, also a legacy TODO) + world-gen
  per-stage timing logs, so regressions are visible and wins are measurable.
- [ ] **In-game hotspots** — `Scene3D` per-frame draw, pathfinding heat maps, the NPC swarm, shadows/effects.
- [ ] **World-creation** — grid build, shadow readback, async flow-field compute, building stream.
- [ ] Fold in the existing perf items as data dictates: **Pathfinding scalability**, **Building perf + lifecycle**,
  and the **Map-size preset** profiling/tuning (all listed in their sections below).

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
- [ ] **Pathfinding scalability.** Heat map is a bucketed Dijkstra (O(cells), async via
  `PathUtil.computeFieldAsync`) but still **per-portal full-map**. Want: multi-mode nav (flow fields near,
  cheap nav far), a coarser `pathResolution` lever, ambient NPCs, a field viz.
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
- [ ] **Map-size pass.** (a) A **warning popup for large maps** — "reduced FPS to be expected" before the
  build, so the player opts in knowingly. (b) **Optimize the three size presets** (Small / Normal / Large) —
  profile + tune each (portal count, NPC density, build time, runtime FPS) so each hits a sane quality/perf
  target. (c) **Drop square maps** if any remain (the round-field option already squares; rectangular presets
  are the keepers) — confirm and remove.
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
- [ ] **Field layering — tests + tuning investigation.** The *rules* are sound and match Ingress
  (`Portal.canLinkOut`: no linking out from a portal **under** a field, so fielding is unidirectional and
  layers stack from **outside/anchor** portals, innermost-first — confirmed vs the Ingress wiki; the modern
  `<500 m` exemption is post-2018 and intentionally omitted). But agents seem to **layer far less** than in the
  2D era — likely a **behaviour/tuning** cause, not logic (suspects: the `Linker` greedily closes *any*
  triangle with no preference for stacking; faster board churn / `dominanceDecay` tears stable fields down
  before they can be layered; link Q-weights). **To do:** (a) add **field-layering tests** — assert a link
  from an outside/anchor portal creates an overlapping field and that `calcTotalMu` sums the layers, and that a
  link from a covered interior portal is rejected; (b) instrument a headless run for layered-field counts and
  tune the `Linker`/weights so layering actually emerges.
- [ ] **Glyph hacking** — a skill-based hack: **~3× rewards**, but **longer**, **needs skill**, **can fail**.
  Glyph skill (+ portal level) sets odds + duration. The collar animation + glassy sound already land
  (`HackFx`/`SoundUtil`); this is the reward/skill/timing model in `Glypher`/`Portal.tryGlyph` + a glyph skill
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
- [ ] **Grid fixtures** — infra is built (`GridFixture` RLE + `GridCapture` ?debug=capture + the
  `PresetConnectivityTest` audit). Only the committed `PresetFixtures.kt` is missing: run `?debug=capture` once
  in-browser, drop the download into `src/jsTest/kotlin/util/`, commit. (The trainer/leaderboard already run on
  the live `World.grid`, so this only feeds the offline connectivity audit.)
- [x] ~~Clean-eval flag~~ — shipped (`MatchSetup.cleanEval`, restored via try/finally; "Clean eval" toggle in TRAIN).
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
*These are the concrete items the **Non-functional track** (above) executes — phase B for the refactors, phase C
for coverage tooling. Kept here for the rationale; the track sequences them.*
- **Coverage tooling.** ✅ resolved: Kover has no Kotlin/JS support, so the pure core moves to `commonMain` and
  is line-covered via a test-only `jvm()` target (`kotlinx-kover` 0.9.8). **Codecov is now wired into CI and
  reporting (~71%)**; coverage grows as the split proceeds. → *phase C (Codecov live); push to >90% ongoing with
  phase B.*
- **Tighten max line length 140 → 120.** Deferred: ktlint auto-wrapping inflates detekt's `LargeClass` count,
  so it must land alongside the class extractions (`Scene3D`, and any near the 600-line cap like
  `HtmlUtil`/`MapUtil`). A dedicated refactor pass. → *phase B.*
- **Extract the demo/showcase subsystem from `Scene3D`** — most of Scene3D's `LargeClass` bulk is the
  self-contained sandbox code; move it to a `Showcases` object (build helpers go `internal`), then drop the
  `LargeClass` suppress. → *phase B.*
- ~~**Null-safety hardening**~~ — ✅ done (phase B): **zero** `!!` in `src/jsMain`; the convention has held.

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
