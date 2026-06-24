# MECHANICS.md — items, mods & drop rates

Reference for Q-Gress's item / mod mechanics, modelled on **~2018 Ingress**. Drop rates are not hard
hidden constants: they live in one tunable place — **`config/DropRates.kt`** — and are surfaced
in-app via **Menu → Drop rates**. A future "game setup" can override `DropRates` per match.

> Q-Gress is a simulation, not the real game. Numbers below are our current values (close to Ingress
> where known); tune them in `DropRates` / the type enums.

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

## Viruses — `items/types/VirusType.kt`
**ADA Refactor** (→ ENL) and **JARVIS Virus** (→ RES) flip an **enemy** portal to the user's faction
(`Portal.refactor` via the `Refactorer` action): resonators are reassigned, mods dropped. The colour
change animates through `CaptureFx`; a `playVirusSound` plays.

## Keys — `portal/PortalKey.kt`
Per-agent inventory; used to link to a portal. Surfaced as counts (leaderboard + inspector); no 3D
model yet.

## Portal discovery & removal — `system/Cycle.managePortalDensity`
Portals are **discovered and removed** by a neutral, density-driven *system* process (every checkpoint),
not an agent action — discovering a portal helps no faction, so it made a dull behaviour slider (the old
`EXPLORE` action + slider are **retired**; the AI's `SliderVector` is now 18, not 19). The count converges
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
