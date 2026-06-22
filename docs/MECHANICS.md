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

## Drop rates (per hack roll) — `config/DropRates.kt`
- **Resonators / XMP / Power Cubes**: roll by **tier** via `portal/Quality.kt` (BEST 0.1 / TOP 0.3 /
  GOOD 0.5 / MORE 0.7, with level offsets). Community estimate: ~1.5 resonators per item roll.
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
