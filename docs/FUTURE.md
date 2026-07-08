# FUTURE.md — the far horizon

Speculative, **interconnected** directions that only make full sense together — mostly the "grand game"
(multiple locations + a managed roster) and the per-agent/colony layer beneath it. None of this is committed
near-term work; the shipped substrate (`docs/FEATURES.md`) is a complete AI-vs-AI sandbox on its own. Concrete,
standalone next steps live in [PLAN.md](../PLAN.md); this file is the "someday, as one big arc" doc.

The two sections below are the spine: a **grand game** across locations, and the **roster management** layer
that most of the deferred gameplay mechanics hang off. A short **other ideas** bucket holds the rest.

---

## 1. Grand game — multiple locations & a living field

The structural direction much of the rest depends on: more than one real-world location at once, and a play
area that can change shape over a game.

- **Movable / expandable play field** — the playable area can **grow** or **shift** over a game (captured
  territory / objectives push the boundary). Grid + flow-field + border + overlays + the cannon-es shatter
  ground already key off `Sim.fieldRadius()` / `isInPlayArea` / `groundZ`, so the field is the seam to make
  dynamic (re-mask + re-sample on change). Code already carries anticipating comments in `Positions`/`Config`,
  but there's no runtime resize yet. *(Absorbs the old onboarding "dynamic field mid-game" idea.)*
- **Multiple linked locations** — run several real-world locations at once: **one focused sim** at full
  fidelity + **off-site locations in a simplified/abstract form** (aggregate MU/portal counts, cheap tick, no
  3D) to bound cost. Locations connect (shared roster, cross-site links/objectives).
- **Movement / pathfinding rework** (enabler) — derive walkability/penalties from **road geometry** (features /
  GeoJSON) and/or a navmesh instead of reading rasterized shadow pixels — decouples the sim from the screen and
  unblocks dynamic zoom + a pitched/3D camera + the resizable field above. Natural partner of the functional-core
  split. (Source road/water/landcover from OSM Overpass — as `BuildingTiles` already does for footprints — or
  re-add an MVT decoder like pbf/`@mapbox/vector-tile`; both were removed when the building loader moved off
  OpenFreeMap vector tiles.)
- **Mini-map** (top-down, north-up, fields always visible) — a small fixed overlay rendering the play area from
  an **exact top-down**, **always-north** view, independent of the main 3D camera, so control fields / portals /
  links stay legible while the main view is pitched and turned. (Could reuse the field/link geometry against a
  north-up projection of `Sim.fieldRadius()` / the grid.) Pairs with the movable field.
- **Weather & a directional sun** (atmosphere + a modifier layer) — rain/fog/snow/day-night as a **visual layer**
  *and* a gameplay modifier (rain drains battery faster, fog cuts hack/attack range, snow slows movement),
  random/seasonal or from a real weather API for the location. A real **directional sun** with time-of-day (chrome
  poles cast highlights, terrain shading, a render-to-cubemap/PMREM of sky+terrain so chrome/glass reflect the
  *actual* scene — today a static gradient env in `Materials`). Pairs with the colony battery idea below.

## 2. Roster management & the per-agent / colony layer

A meta layer over the 17 sliders: a **roster of ~16–32** the AI allocates and improves, and the per-entity
attributes that make individual agents matter. Most of the deferred *skill* mechanics read or extend
`agent/Skills`, so this layer is the substrate to settle first.

- **Roster management across sites** — a player roster spread over the locations, allocated/moved between the
  focused sim and the off-sites — the AI reasons at both the local-tactical and roster/strategic level. *Open:*
  the off-site model (pure stats vs coarse grid), travel/relocation cost, and how cross-site links/fields score.
- **Colony-management / per-agent attributes** — per-entity attributes (endurance/speed/agility/radius on
  `agent/Skills` + `AgentSize`); **rarity-tiered agents** (randomised attributes, **no gambling UX** — manage
  composition, not a gacha); **items** (skateboards, **jet-skis** → makes marina/bridge presets playable,
  power-banks, second phones); **battery %** (depleted phone → the player leaves). Pairs with the humanoid work.
- **Humanoid glTF models** — people are head-sized spheres at head height today; swap in real models (pairs with
  the colony attributes above).
- **Initial roster "roll"** — light flavour on onboarding (not a gacha loop); ties to the rarity tiers.
- **Glyph hacking** — a skill-based hack: **~3× rewards**, but **longer**, **needs skill**, **can fail**. Glyph
  skill (+ portal level) sets odds + duration. The collar animation + glassy sound already land (`HackFx`/`Sound`);
  this is the reward/skill/timing model in `Glypher`/`Portal.tryGlyph` + a glyph skill on `agent/Skills`, exposed
  as a high-risk/high-reward QAction the AI learns to weigh. Drive the glyph count + hack duration off portal
  level (Ingress wiki: L1 = 1 glyph / 20 s … L8 = 5 glyphs / 15 s; perfect-hack + speed bonuses) via the
  `glyph/Glyph` enum; the TTS "glyph" tier already reads the sequence back.
- **Aim skill (XMP / Ultra-Strike accuracy)** — a per-agent skill: high-aim detonates **closer to portal centre**
  (max damage), low-aim lands **off-centre** (damage falls off with miss distance). Makes the small-radius
  Ultra-Strike reward good aim; feeds the damage calc + blast VFX origin; another AI lever. *(Wants an eyes-on
  session — it rewrites the live combat-damage model + blast VFX origin.)*
- **Recruiting items** — a temporary recruit-success boost item (e.g. *beer*) on top of the existing idle-fallback
  recruiting (success scales with progress speed + the per-agent `Skills.recruitingFactor`).

## 3. Other ideas (deep icebox)

Standalone-ish but far-off; parked here rather than cluttering PLAN.

- **Modern Ingress (post-2018), optional** — most-aligned first: **Machina** (the red AI "third faction" — the
  closest analogue to the AI-vs-AI north star: a rules-driven faction that spawns, auto-links, decays, is easy to
  clear); the **hacking-economy triad** (heat-sink / multi-hack / Fracker + ITO) shaping farm rate; **drones**
  (remote hack). Skip the real-world/social bits.
- **Swarming agent behaviour** — agents coordinate/cluster toward shared objectives (a flocking / group-movement
  layer + more destination variety) instead of each pathing solo.
- **Walkable roofs / per-building destruction** — now that we mesh every building (`OwnBuildings`), agents could
  path over roofs; buildings could be destructible.
- **Sound-design pass → 303 / 808 / 909** — detonations already lead with a deep 909 kick + reverb send
  (`KickDrum`/`AudioFx`/`BlastSound`); take the rest of the palette the same way (808 toms/claps + 909 hats for
  deploys/links/checkpoints, acid-303 sweeps for viruses/power-ups, a shared reverb/delay bus, maybe a musical
  bed). The `#audio` demo is the dial-in surface.
- **Portal-name ticker — non-latin / RTL font** — `PortalNameTicker` reuses Coda (latin + digits), so
  Arabic/Hebrew/CJK names can't be drawn (RTL currently suppressed via `isRtl`). To support them: load an
  RTL/CJK-capable typeface.json (or per-script fonts), add Arabic shaping/joining, re-enable the CCW spin for RTL.
- **Title-screen polish** — a FreeCamera flight path (fly the camera *position* over terrain while `lookAtPoint`
  keeps the action framed — MapLibre's default camera can't decouple position from look-at); more drifting
  particles / a generative ambient bed.
- **Evaluate NVIDIA Komodo** — investigate fit (rendering / AI / acceleration?) before committing.
