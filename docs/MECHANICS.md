# MECHANICS.md — items, mods & drop rates

Reference for Q-Gress's item / mod mechanics, modelled on **~2018 Ingress**. Drop rates are not hard
hidden constants: they live in one tunable place — **`config/DropRates.kt`** — and are surfaced
in-app via **Menu → Drop rates**. A future "game setup" can override `DropRates` per match.

> Q-Gress is a simulation, not the real game. Numbers below are our current values (close to Ingress
> where known); tune them in `DropRates` / the type enums.

## The goal: links, fields & Mind Units (MU) — `portal/Field.kt`, `World.calcTotalMu`
Everything else (hacking for gear, capturing, recruiting) is in service of one objective: **cover area with
control fields**. A captured portal can **link** to another the agent holds a **key** for; three portals
mutually linked into a triangle form a **control field** (`Field`), whose area in **Mind Units (MU)** is the
triangle's area (Heron's formula). A faction's score is its **total MU** (`World.calcTotalMu` = sum of its
fields' areas), and fields can be **layered** (nested/overlapping triangles) to stack MU over the same
ground. So the optimal play is to **field aggressively and maximise MU each turn**: capture → fully deploy →
link → field, then layer more fields on top. The AI drivers are scored on exactly this — the summed
per-checkpoint MU margin over the opponent (see `docs/NN.md`, `ai/Evolution`) — so an agent "consolidates
into links/fields when ahead and presses the attack to tear down the leader's fields when behind". A field's
plasma also **fades toward a low-health portal corner** (per-vertex alpha from each anchor's health), so a
field hanging off a weak/dying portal reads as near-transparent (`PlasmaShader`).

**Linking & layering rules** (`Portal.canLinkOut`, matching Ingress). A link needs an owned source with at
least `Config.linkMinResos` resonators, a held key to the target, ≤ 8 outgoing links, and **no crossing**
existing link (`findLinkableForKeys`). Critically, you **cannot link out from a portal that sits under an
existing control field** (`!isCoveredByField()`) — only from *outside* it (the field's own anchors are exempt,
since `isCoveringPortal` ignores connected portals). This makes fielding **unidirectional** and means layered
fields must be built **innermost-first**: you stack a new field by linking from an outside/anchor portal *over*
existing fields, never from a portal trapped inside one. The logic is sound; agents layering *less* than they
used to is a behaviour/tuning question (board churn may tear stable fields down before they can be layered),
not a rule bug — see `PLAN.md`. (Modern Ingress exempts **< 500 m** links from the under-field block, but
that looks to be a **post-2018** addition, so the classic-era sim deliberately omits it.) A per-tick
invariant sweep (`World.pruneInvalidLinksAndFields`) guarantees a link/field can never **dangle**: any whose
endpoints aren't all still on the board and the same faction is dropped — a safety net for a flip/removal that
races a link being made.

## Mod slots
Each portal has **4 mod slots** (`portal/ModSlot.kt`), holding any mix of **shields**, **heat sinks**,
and **link amps** (`items/deployable/Mod.kt`). Mods are deployed one-per-action by agents (`Deployer`)
and fall out when the portal is neutralized/removed. A **virus flip keeps the mods** (and resonators) in
place — only the links/fields are torn down (see *Flip items* below).

### Shields — `items/types/ShieldType.kt`
Reduce **incoming XMP damage** (mitigation), capped at 95% total (links + shields). ~2018 values:

| Shield | Rarity | Mitigation |
|--------|--------|-----------|
| Common (CS) | mint | 30% |
| Rare (RS) | purple | 40% |
| Very Rare (VRS) | pink | 60% |
| Aegis (AEGIS, ex-AXA) | pink | 70% |

Multiple shields each add their full mitigation (then the 95% cap applies). Visual: a **dodecahedron**
inside the orb + a φ× **shield bubble** (`ShieldShader`).

### Heat sinks — `items/types/HeatSinkType.kt`
Reduce the portal **hack cooldown** for everyone. ~2018 values: Common **20%**, Rare **50%**, Very
Rare **70%**. Multiple: rarest applies full, each subsequent **halved** (`Portal.cooldownFactor`). Deploying one
also **instantly resets** the portal's cooldown (wipes the per-agent hack history `Portal.lastHacks`), so it's
immediately hackable again. Visual: a **pentagonal radiator**.

### Link amps — `items/types/LinkAmpType.kt`  *(inactive)*
Boost link range / outbound-link count. **Inactive in Q-Gress** (linking range isn't a balance lever
yet): defined + drawable (**diagonal cube**) but **never dropped** and with no gameplay effect. ~2018:
standard Rare + Very Rare (VR never drops — passcode only), plus the SoftBank Ultra Link (SBUL).

### Multihacks — `items/types/MultihackType.kt`
Raise a portal's **hacks-before-burnout** limit for everyone: Common **+4**, Rare **+8**, Very Rare **+12**
(authentic Ingress). With several deployed the **rarest counts full, each other at half**
(`Multihack.additionalHacks`), so `Portal.maxHacks()` = base **6** + that bonus. They **drop** from hacks
(`DropRates.multihackChance`, heat-sink-scaled) and **deploy** like any mod (after shields + heat sinks).
Colour from [rarity]; rendered as a **hollow square ring** in the orb.

## Flip items (viruses) — `items/types/VirusType.kt`
Two flip items: the **JARVIS Virus** (→ **ENL**/green) and the **ADA Refactor** (→ **RES**/blue). The
**item type** decides the result faction (`VirusType.flipsTo`), *not* the user's — and **either faction
may carry and use either item** (they drop independently, `1 / VirusType.roll` ≈ 1-in-2500 per hack).
A flip (`Portal.refactor` via the `Refactorer` action) re-owns the portal **and all of its slot content
in place** (every resonator + mod stays, just re-owned to the new faction); only the links/fields are
torn down (they'd be cross-faction now). The orb re-skins through `CaptureFx` **without** the capture
shatter and a `playVirusSound` (pitched to the new colour) plays.

A flip is allowed only when:
- the target is a **faction-owned** portal — a **neutral portal can't be flipped** (it's captured by
  deploying, not flipped);
- the item's `flipsTo` **differs** from the portal's current faction — you can't flip a portal to the
  colour it already is; and
- the portal is **off its flip-immunity cooldown** — after any flip a portal is immune to being flipped
  again (in **either** direction) for **1 h** of sim time (`Portal.isFlippable`, `FLIP_IMMUNITY_S`).

This makes both directions possible: **attack-flip** an enemy portal to your colour with the matching
item, or **friendly-flip** your *own* portal to the enemy colour with the off-colour item — e.g. to shed
a friendly link that blocks a bigger field, or to turn a friendly portal hostile so it can be
neutralized and re-captured with different mods. A friendly-flip hands the portal to the nearest agent
of the target faction. These tactics are **emergent**: the AI attack-flips enemy portals on the `VIRUS`
slider, and is offered the friendly-flip at a heavily damped weight (`ActionSelector.FRIENDLY_FLIP_FACTOR`)
so it stays rare rather than hand-coded.

## Keys — `portal/PortalKey.kt`
Per-agent inventory; used to link to a portal. Surfaced as counts (leaderboard + inspector); no 3D
model yet.

## Hacking, cooldown & glyphing — `portal/Portal.kt`, `agent/action/cond/{Hacker,Glypher}.kt`
A **hack** is the supply action: an agent in range of a portal pulls items + XM from it.
- **Yield (`Portal.hack`):** resonators, XMP bursters, Ultra-Strikes, shields, heat sinks, power cubes,
  viruses and portal keys, rolled per `config/DropRates.kt` (see *Drop rates* below). Hacking an **enemy**
  portal also awards **AP** and costs more XM than a friendly hack.
- **Cooldown (`portal/Cooldown.kt`, `portal/PortalMath.cooldownAfter`):** after a hack the same portal is on
  cooldown for that agent, bucketed `FIVE` (5 min, the base) → … → `NONE`, shortened by deployed **heat
  sinks** (`Portal.cooldownFactor`).
- **Burnout (`PortalMath.isBurnedOut`):** an agent may hack a portal **`Portal.maxHacks()`** times within
  the burnout window before it locks them out with `BURNOUT` (a long cooldown) — a base **6** (above the
  authentic 4 so agents restock fast enough to assault) plus any deployed **multi-hacks** (see Mod slots).
  On the hack that tips a portal into burnout it vents a one-shot **white-steam puff + hiss** from the flask
  top (`SmokeFx`/`SteamSound`) — a visual cue, since burnout itself is tracked per agent.
- **Inventory cap (`Config.maxInventory`, 2000):** a **full** inventory **can't hack/glyph** for more items
  (`Hacker.isActionPossible`) — the agent must spend (deploy/attack/link) or **recycle** to free space first.
- **Glyphing (`Portal.tryGlyph`, `Glypher`):** a glyph hack yields **2×** a normal hack always, and **3×**
  when the agent's `glyphSkill` (default **0.8**) passes — pure item-volume bonus (no extra AP/XM). It's how
  agents refuel quickly; the heuristic/AI drivers "hack/glyph to refuel when XM runs low".

## Power cubes & recycling — `items/PowerCube.kt`, `agent/action/cond/Recycler.kt`
Power cubes are XM batteries that **drop from hacks** (`PowerCubeLevel` L1–L8 = **1000–8000 XM**, linear, as
in Ingress). An agent low on XM (< 10% of capacity) **recycles** one (`Recycler`, the `RECYCLE` action),
consuming it to restore its level × 1000 XM — the agent's main self-refill alongside picking up stray XM.
This is distinct from **recharge** (`Recharger`, the `RECHARGE` action), which spends the agent's XM to refill
a *portal's* resonators — and the two run **back and forth**, as in Ingress: recharging drains the bar toward
the same < 10% mark below which the cube-recycle kicks in, so an agent holding portals alternates
recharge → recycle → recharge until the portal is topped up or the cubes run out. Recycle is also
**inventory management**: when ~full it dumps junk to free space (`Inventory.recycleForSpace`) — surplus
**duplicate keys** first (keeping a few per portal), then the lowest resonators / surplus weapons / power cubes.

## Recruiting — `agent/action/cond/Recruiter.kt`, `agent/Balance.kt`
Growing a faction's roster by persuading a bystander **NPC** to join. It is the *team-size* lever, kept
deliberately gentle so neither faction snowballs.
- **An idle fallback, not a slider.** Recruiting is **not** a behaviour slider and never competes in the action
  roulette — agent action is purely the 17 sliders. It's what an agent does when it has **no gameplay action
  left** (portals burnt out, nothing to hack/deploy/attack), exactly like EXPLORE (`Recruiter.canRecruit` from
  `ActionSelector`). Only `Config.maxConcurrentRecruiters` (2) per faction recruit **at once** — the rest explore
  — so a quiet board never shows every agent recruiting. Free (no XM).
- **How:** an idle agent walks to the **nearest** NPC; on meeting it rolls success
  `recruitmentChance = base × Progress-speed × Balance.recruitFactor × (1 − rosterFill) × Skills.recruitingFactor`
  (`Config.recruitmentBaseChance = 0.05`, rolled once per **100 s** meeting). To recruit faster you raise the **success chance** (Progress speed),
  not the number of recruiters. A successful recruit converts that NPC into the new agent **at its own position**
  (`Agent.create(at = npc.pos)`) — it isn't deleted while a fresh agent spawns elsewhere.
- **Anti-snowball (`Balance.recruitFactor`):** the per-meeting success chance scales by
  `(enemyRoster + 1) / (myRoster + 1)`, clamped to `[0.3, 3.0]` — the **smaller** team recruits more, the
  **larger** less — and by the roster's remaining headroom (→ 0 at the cap), so sizes self-correct.
- **Rosters only grow.** No quitting / faction-change (`frogQuitRate` / `smurfQuitRate` / `factionChangeRate` = 0);
  recruiting fills the roster up to the **size-scaled cap** (`Config.maxFor` → `Sim.maxAgents`: Tiny 8 · Small 16 ·
  Mid 24 · Large 28 · Giant 32). The start roster scales too (`Sim.suggestedAgents` 3·5·8·12·16 at mid-game; 1 at
  a normal start; the full cap at end-game).
- **The crowd drains toward a floor:** recruiting is **not** topped up 1-for-1. The NPC population shrinks as
  agents recruit and is only refilled once it reaches `Config.MIN_NONFACTION` (30), so a long game thins the
  crowd but never runs out of recruits.

## Portal discovery & removal — `agent/action/cond/Discoverer.kt`
Portals are **discovered and removed** by the agent **discovery** idle action — a neutral, density-driven
process driven by idle agents (it replaced the old per-checkpoint `Cycle.managePortalDensity`). Discovering
a portal helps no faction, so it's not a behaviour slider; it's what an idle agent does on a wander's
**arrival** (the sibling of recruiting — see [ACTIONS.md](ACTIONS.md) for the idle-decision machine). It
self-throttles: a busy board churns little, a sparse one floods discovery and fills fast. The count converges
toward `Config.targetPortals(walkability)` — set by the map's **walkable ground** (`200 × Sim.areaKm2 ×
walkability`, a constant portals-per-walkable-km² density), clamped to `[startPortals, maxPortals 89]`, so an
open map supports more portals than a built-up one of the same size:
- `d = count / target` (1.0 at target). `createChance ∝ (1 − d/2)`, `removeChance ∝ (d/2)` (× `Config.
  portalChurnRate`) — so well below target discovery dominates (~4:1 near empty), at target it's ~1:1, and
  above it removal wins. Equilibrium settles a bit *below* target because combat also destroys portals
  (discovery replaces them).
- **No-space handling:** if the map is too packed to place a non-clipping portal (`Pos.hasPortalSpace`,
  enforced by `Dim.minDistanceBetweenPortals`), discovery is skipped and that budget rolls into removal, so
  a full board thins out instead of stalling. (Min-distance also caps portal density per area.)
- A **removed** portal `remove()`s like any destroyed portal: it shatters (FX + sound), drops its mods, and
  all its links + fields (incoming and outgoing) are destroyed.

## Resonator decay & recharge — `items/deployable/Resonator.kt`
Resonators lose energy over time; a portal with no energy left loses resonators and eventually goes
neutral. Recharging (`Recharger`, the `RECHARGE` action) tops them back up from the agent's own XM.
- **Targets (`Portal.findChargeable`):** the friendly portal the agent is **standing at and working**
  (its action portal, no key needed) once below **90%** health — the casual top-up — plus **keyed remote**
  portals once **badly hurt** (≤ 50% health) — the emergency hold, as in Ingress. The remote bar is
  deliberately low: cycle decay leaves *every* friendly portal a bit below full, and a looser gate had
  key-holding agents tarpit at each damaged portal they passed instead of travelling.
- **One action = one "recharge all" press:** the agent's XM is split **evenly** across the portal's
  resonators, at most **1000 XM per resonator** (`Recharger.RECHARGE_XM_PER_RESO`, authentic), each capped
  by the resonator's open capacity; only the XM actually applied is deducted (+10 AP per resonator).
- **The recharge↔recycle loop:** recharge fires while the agent isn't drained (`Agent.isXmLow`, ≥ 10% of
  capacity) and can spend the bar right down; below the same mark the `Recycler` taps a power cube to
  refill — so holding a portal is the authentic alternation: recharge → recycle a cube → recharge.
- **Authentic Ingress (doc'd for reference, not implemented as-is):** a portal loses **15% of each
  resonator's energy per day**, so an un-recharged portal (e.g. a remote one you only hold via a key)
  fully **decays after ~a week**. We deliberately *don't* model real-time day/week decay — matches are
  meant to be fast — but it's the canonical mechanic to keep in mind. Likewise **remote-recharge
  efficiency** (falling with key distance, down to 35%) is not modelled: remote recharge is full-strength.
- **Sim model:** `Resonator.DECAY_RATIO = 0.15` (the 15% figure) applied at **cycle end**
  (`Portal.decay`), i.e. accelerated to sim time. Recharging counters it.
- **Dominance decay (sim anti-runaway, `Config.dominanceDecay`):** *every checkpoint*, a portal owned by
  the **leading** faction erodes extra resonator energy proportional to that faction's MU lead
  (`Portal.erodeByDominance`, scale = `leadShare × dominanceDecay`). An over-extended empire crumbles, the
  board reopens, and the MU lead can change — tuned via the headless sweep (`ai/BalanceSweep`); it lifted
  lead-sharing balance from ~0.37 (runaway) to ~0.75 between equally-tuned factions. Not in real Ingress.

## Drop rates (per hack roll) — `config/DropRates.kt`
- **Resonators / XMP / Power Cubes**: roll by **tier** via `portal/Quality.kt` (BEST 0.1 / TOP 0.3 /
  GOOD 0.5 / MORE 0.7, with level offsets). Community estimate: ~1.5 resonators per item roll.
- **XMP + Ultra-Strike yield** is scaled by **`Config.weaponDropMultiplier()`**, now derived from the
  single **Combat dynamics** slider (`Config.combatDynamism`, `1×`…`20×`) — higher dynamism hands out more
  firepower so agents flip defended portals. It multiplies the weapon-draw count
  (`DropRates.xmpDropMultiplier`, base 1), which drives both the XMP and the Ultra-Strike yields.
- **Ultra Strikes** *(sim-tuning, not authentic)*: drop from hacks at `DropRates.usDropChance`
  (default 0.25) per weapon draw — a single Bernoulli per draw, so **rarer** than XMP's quality cascade.
  In an assault (`Attacker`) the agent spends Ultra-Strikes **first**, to strip the portal's shields/mods
  *before* the burst volleys — so the bursts aren't soaked up by shield mitigation (the dynamism payoff).
- **Portal key**: `DropRates.keyChance` (default 0.8).
- **Shields**: per type (`DropRates.shieldChance`, defaults from `ShieldType.chance`).
- **Heat sinks**: per type (`DropRates.heatSinkChance`).
- **Viruses**: per type (`DropRates.virusChance`, default `1 / VirusType.roll`).
- **Link amps**: never drop (inactive).

### Sources (Ingress references, ~2014–2018)
- Heat Sink (20/50/70% cooldown, rarest-full-then-halved): https://ingress.fandom.com/wiki/Heat_Sink ·
  https://fevgames.net/ingress/ingress-guide/items/mod/heat-sink/
- Link Amp (Rare ~1/500, VR passcode-only) + SoftBank Ultra Link: https://ingress.fandom.com/wiki/Link_Amp ·
  https://fevgames.net/softbank-ultra-link-amp/
- Portal Shield (30/40/60/70%, Aegis = renamed AXA): https://ingress.fandom.com/wiki/Portal_Shield ·
  https://fevgames.net/ingress/ingress-guide/items/mod/portal-shield/
- Resonator drop rate (~1.5/roll): https://ingress.fandom.com/wiki/Resonator

### Gaps / TODO
- Exact 2018 per-portal-level drop tables aren't published; our tier chances are approximations.
- Link amps + SBUL effects are stubbed (inactive).
- A per-game-setup override UI for `DropRates` is future work (the data is already centralized).
