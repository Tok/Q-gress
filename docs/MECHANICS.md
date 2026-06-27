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
and **link amps** (`items/deployable/Mod.kt`). Mods are deployed one-per-action by agents (`Deployer`),
fall out when the portal is neutralized/removed, and are dropped (cleared) by a virus flip.

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
Rare **70%**. Multiple: rarest applies full, each subsequent **halved** (`Portal.cooldownFactor`).
Visual: a **pentagonal radiator**.

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

## Viruses — `items/types/VirusType.kt`
**ADA Refactor** (→ ENL) and **JARVIS Virus** (→ RES) flip an **enemy** portal to the user's faction
(`Portal.refactor` via the `Refactorer` action): resonators are reassigned, mods dropped. The colour
change animates through `CaptureFx`; a `playVirusSound` plays.

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
a *portal's* resonators via a held key. Recycle is also **inventory management**: when ~full it dumps junk to
free space (`Inventory.recycleForSpace`) — surplus **duplicate keys** first (keeping a few per portal), then
the lowest resonators / surplus weapons / power cubes.

## Recruiting — `agent/action/cond/Recruiter.kt`, `agent/Balance.kt`
Growing a faction's roster by persuading a bystander **NPC** to join. It is the *team-size* lever, kept
deliberately gentle so neither faction snowballs.
- **Free:** recruiting costs **no XM** (it's persuasion, not an energy action). The old XM cost was removed.
- **How:** an agent walks to a random NPC; on meeting it rolls success
  `recruitmentBaseChance × (1 − rosterFill)` (`Config.recruitmentBaseChance = 0.05`), so an empty roster
  recruits at ~5% and a full one at ~0 (diminishing returns toward `Config.maxFor(faction)`).
- **The NPC turns faction in place:** a successful recruit converts that NPC into the new agent **at its own
  position** (`Agent.create(at = npc.pos)`) — it isn't deleted while a fresh agent spawns elsewhere.
- **Anti-snowball (`Balance.recruitFactor`):** how *often* an agent even tries to recruit scales by
  `(enemyRoster + 1) / (myRoster + 1)`, clamped to `[0.3, 3.0]` — the **smaller** team recruits more, the
  **larger** less, so sizes self-correct.
- **The crowd drains toward a floor:** recruiting is **not** topped up 1-for-1. The NPC population shrinks as
  agents recruit and is only refilled once it reaches `Config.MIN_NONFACTION` (30), so a long game thins the
  crowd but never runs out of recruits. There is **no `RECRUIT` behaviour slider** (retired — too snowbally).

## Portal discovery & removal — `system/Cycle.managePortalDensity`
Portals are **discovered and removed** by a neutral, density-driven *system* process (every checkpoint),
not an agent action — discovering a portal helps no faction, so it made a dull behaviour slider (there is no
`EXPLORE` action). The count converges
toward `Config.targetPortals()` (≈ 2.5 × the onboarding `startPortals`, capped at `maxPortals` 89):
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
neutral. Recharging (`Recharger`, via a held key) tops them back up at an XM cost.
- **Authentic Ingress (doc'd for reference, not implemented as-is):** a portal loses **15% of each
  resonator's energy per day**, so an un-recharged portal (e.g. a remote one you only hold via a key)
  fully **decays after ~a week**. We deliberately *don't* model real-time day/week decay — matches are
  meant to be fast — but it's the canonical mechanic to keep in mind.
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
  firepower so agents flip defended portals. It multiplies the XMP draw count
  (`DropRates.xmpDropMultiplier`, base 2) and the Ultra-Strike draw count.
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
