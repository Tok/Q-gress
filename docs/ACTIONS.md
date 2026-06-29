# ACTIONS.md — the agent action machine

How a single agent decides and performs what it does each tick: the per-tick loop, how the **17
behaviour sliders** (Q-values) are applied to pick a *productive* action, and what happens when there's
nothing productive to do — the **idle fallbacks** (recruiting / discovery), including their timing and
success odds.

Scope: this is the *per-agent* decision layer. The objective the AI is ultimately scored on (links /
fields / MU) is in [MECHANICS.md](MECHANICS.md); the faction-level AI driver that *re-tunes* the sliders
is in [NN.md](NN.md) / [LLM.md](LLM.md). Code lives in `agent/action/` (`ActionSelector`, `Action`,
`ActionItem`, `cond/*`), `agent/Agent.kt`, `agent/qvalue/`, and `ai/FactionPolicy.kt`.

## Time base — `config/Time.kt`
**1 tick = 1 sim-second** (`secondsPerTick = 1`, so `secondsToTicks(s) = s`). At max speed a tick is
`minTickInterval = 20 ms` of wall-clock, but all durations below are in **sim-seconds / ticks**. Every
agent runs `Agent.act()` once per tick (on a shuffled, seeded snapshot — see `system/Simulation.kt`).

## The per-tick loop — `Agent.act()`
Each tick an agent is either **mid-action** (still busy / still walking) or **free to re-decide**.
`act()` routes purely on the *current* `action.item`:

```
ATTACK / DEPLOY / MOVE / EXPLORE / RECRUIT  → their own self-managed step (handles approach + completion)
WAIT                                        → re-select immediately (ActionSelector.doSomethingElse)
<any other item> &&  isBusy()               → keep waiting (the action's cooldown is still running)
<any other item> && !isBusy()               → re-select (ActionSelector.doSomethingElse)
```

So actions come in two shapes:

- **Self-managed** (`ATTACK`, `DEPLOY`/`CAPTURE`, `MOVE`, `EXPLORE`, `RECRUIT`): the step function walks
  the agent into range / to the target, then resolves. `MOVE` and `EXPLORE` end on **arrival**; `ATTACK`
  and `DEPLOY` use `isBusy()` as a *move-into-range budget*; `RECRUIT` uses `isBusy()` as the **meeting**
  timer.
- **Instant-effect + cooldown** (`HACK`, `GLYPH`, `LINK`, `RECHARGE`, `RECYCLE`, `VIRUS`): when selected,
  the `cond/*` handler applies the effect **once** and calls `action.start(item)`. Subsequent ticks fall to
  `isBusy() → this` (the agent waits out the cooldown), then re-selects when it expires.

When any action ends it calls `Action.end()`, which parks the agent in `WAIT` for one tick; the next
`act()` sees `WAIT` and re-selects. (`RECRUIT` is special-cased to re-select **in the same tick** so its
transient `WAIT` never renders — see *Recruiting* below.)

### `ActionItem.durationSeconds` — which durations are live
`durationSeconds` is **only** read by `Action.start` (→ `untilTick = tick + durationSeconds`) and only
*matters* where the handler consults `isBusy()`:

| Action | dur (s) | Role of the duration |
|---|---|---|
| `HACK` | 10 | post-hack cooldown (the core farm loop) |
| `VIRUS` | 15 | cooldown |
| `ATTACK` | 15 | budget to close into firing range |
| `DEPLOY` | 15 | budget to close into deploy range |
| `LINK` / `RECHARGE` / `RECYCLE` | 30 | cooldown |
| `GLYPH` | 60 | cooldown (slow skill-hack) |
| `RECRUIT` | 100 | the **meeting** length (see below) |
| `MOVE` | 60 | *nominal* — `MOVE` ends on **arrival**, never the timer |
| `EXPLORE` | 300 | *nominal* — `EXPLORE` ends on **arrival**, never the timer |
| `WAIT` | 10 | *nominal* — `WAIT` is the 1-tick transitional state |
| `CAPTURE` | 15 | *nominal* — `capturePortal` runs the `DEPLOY` machine, so the item is `DEPLOY` |

The four *nominal* rows are never consulted; the value is documentation, not a live knob.

## Selecting a productive action — the Q-values
When an agent is free to decide, `ActionSelector.doSomethingElse(agent)` builds a **candidate list** for
the agent's current situation and picks one by weighted random.

### 1. Context → candidate list — `ActionSelector`
The candidates depend on where the agent is and what it's standing on:

- not at its target portal → `doAnywhereAction` → `{ RECYCLE, RECHARGE }`
- at a **friendly** portal → `+ { HACK, GLYPH, DEPLOY, LINK }`
- at a **neutral** portal → `+ { HACK, GLYPH, CAPTURE }`
- at an **enemy** portal → `+ { HACK, GLYPH, ATTACK, VIRUS }`

Each candidate is gated by its `cond/*` `isActionPossible(agent)` — an action only enters the list with a
**positive** weight when it's actually doable right now (a deployable resonator in hand, a key + an
uncrossed link target, XMPs to fire, …); otherwise it's added with weight `-1.0` (ineligible).

### 2. Weight = slider × base — `ActionSelector.q`
```
q(faction, value) = FactionPolicies.of(faction).weight(value)   // the 0..1 slider reading
                  * value.weight                                // the QValue's fixed base weight
```
The **slider reading** is the per-faction behaviour input — the action substrate. By default it's
`DomSliderPolicy` (reads the live tuning slider, or `SliderVector.DEFAULT_WEIGHT` when headless). An AI
driver (net / LLM) installs a `SliderVectorPolicy` via `FactionPolicies.set` and rewrites the vector at
checkpoint cadence — **without changing how an agent picks actions**. Player **overrides** wrap the policy
(`OverridePolicy`): a locked slider returns the player's value, the rest pass through to the AI.

The **base weights** (`agent/qvalue/QActions.kt`) bias the mix toward fielding even at equal sliders:

| QValue | base | QValue | base |
|---|---|---|---|
| `LINK` | 3.0 | `HACK` | 1.0 |
| `DEPLOY` | 2.0 | `GLYPH` | 1.0 |
| `CAPTURE` | 1.5 | `ATTACK` | 1.0 |
| `RECYCLE` | 1.0 | `VIRUS` | 1.0 |
| `RECHARGE` | 1.0 | `MOVE_ELSEWHERE` | 0.01 |

`LINK > DEPLOY > CAPTURE` so a held portal gets consolidated into fields rather than endlessly
hacked/captured. (`RECRUIT` and `EXPLORE` are **not** sliders — they were retired as the idle fallback;
see below.)

### 3. The pick — `Rng.select`
If **any** candidate has a positive weight, `MOVE_ELSEWHERE` (AI-weighted relocation, base `0.01`) is
appended and `Rng.select` chooses one with probability proportional to its weight (seeded → deterministic).
`MOVE_ELSEWHERE` itself picks a destination from the **7 movement Q-values** (`QDestinations`:
`toUncaptured 1.0`, `toMostFriendly 0.8`, `toNear 0.2`, `toRandom 0.1`, `toNearEnemy/Weak/Strong` …) and
heads there as a `MOVE`.

If **no** candidate is positive — nothing productive to do — the agent is **idle**.

> A small anti-snowball tap precedes all of this: with probability `leadShare × Config.leaderDistraction`
> (default `0`) a leading-faction agent just wanders instead of acting, costing the leader tempo.

## Idle — recruiting vs discovery — `ActionSelector.idle`
Reached when there's no productive action *at the agent's current portal*. But **"nothing here" ≠ "nothing
to do"** — the fallbacks (recruit / discover) are *not* Q-values and only fire when the agent has no
productive work to **seek** anywhere either. The choice:

```
idle(agent) = when {
    hasWorkToSeek(agent)                                    -> agent.moveElsewhere()           // GO do real work
    agent.isAtActionPortal() && Recruiter.canRecruit(agent) -> Recruiter.performAction(agent)  // grow the roster
    Discoverer.canDiscover(agent)                           -> Discoverer.performAction(agent) // churn the board
    else                                                    -> agent.moveElsewhere()           // keep roaming
}
```

- **`hasWorkToSeek`** is the key gate — there's almost always a portal worth travelling to, so the agent
  goes (`moveElsewhere` → `MOVE`) instead of filler-recruiting/discovering. It's true when **any** of:
  a portal this agent can still **hack** (off its per-agent cooldown, not burnt out, inventory not full),
  an **enemy** portal to attack (with XMPs in hand), or a **neutral** portal to **capture**. This is what
  stops the game-start idling — every portal is neutral, so everyone heads off to capture.
- Only when **none** of that exists — the board's burnt out / nothing to take / no XMPs (exactly the "truly
  idle" case) — do the coin-less fallbacks fire: **recruit** (parked at a worked-out portal, under the cap →
  grow the roster) or **discover** (under the cap → stroll to open ground and run a density-driven portal
  **create/remove** on arrival, below).
- A **portal-less board** has no work to seek *and* no portal to `moveElsewhere` toward, so every idle agent
  routes into `EXPLORE` (via `Movement.wander`) and resolves discovery on arrival — **bootstrapping** the
  board until portals appear.

Both idle states render **pill-less** in the 3D scene (no action coin — `ActionItem.isFallback`), each
with its own tell so they're not mistaken for a stuck agent: **recruiting jumps** in place (a hard
vertical head-bob) and **discovery runs a small circle** (a horizontal orbit). In the **AGENTS** table they
show the **empty coin** and read as `idle/recruiting` / `idle/discovery`.

### Recruiting — `agent/action/cond/Recruiter.kt`, `Agent.recruitStep`
The flow once `idle()` starts a recruit:

1. **Target** the *nearest* NPC (`NonFaction.findNearestTo`), set it as the destination, `start(RECRUIT)`.
2. **Walk up**, refreshing the timer each tick until within `Dim.maxDeploymentRange`; the NPC is held in
   place (`holdInPlace`).
3. **Meet**: stand together while `isBusy()` — the `RECRUIT` duration, **100 sim-seconds**.
4. **Resolve** (`Recruiter.resolve`): roll **once** against the success chance below. On success the NPC
   converts in place into a new teammate (`pendingAgents`, flushed after the loop), with a COM message + a
   success chirp + verbose TTS. On failure: nothing (silent — declines resolve many times over, so no blip
   / no feed spam). Either way the agent clears its target and **re-selects in the same tick**, so the
   1-tick `WAIT` from `end()` never renders as a flashing empty pill.

**Concurrency cap.** Only `Config.maxConcurrentRecruiters` (**2**) per faction recruit at once
(`canRecruit` counts current recruiters); the rest of the idle agents seek work. Recruiting also requires
roster headroom (`World.canRecruitMore`).

**Per-meeting success chance** — `Recruiter.recruitmentChance` → `BalanceMath.recruitChance`:
```
chance = clamp01( baseChance × progressSpeed × recruitFactor × (1 − fillRatio) ) × recruitingFactor
```
| factor | source | default / range |
|---|---|---|
| `baseChance` | `Config.recruitmentBaseChance` | **0.05** |
| `progressSpeed` | `Config.progressSpeed` (the "make it faster" lever) | `1.0` |
| `recruitFactor` | `Balance.recruitFactor` — anti-snowball: smaller roster recruits more | clamped `[0.3, 3.0]` |
| `fillRatio` | `countAgents / maxFor(faction)` → headroom `(1 − fillRatio)` | `→ 0` at the roster cap |
| `recruitingFactor` | per-agent aptitude `Skills.recruitingFactor` = `0.5 + recruiting` | `0.5 … 1.5`, avg `~1.0` |

At defaults (empty roster, even teams, average aptitude) ≈ **5 % per 100 s meeting**. The chance scales
**inversely with the meeting length**: the meeting was made 10× longer (10 → 100 s) and `baseChance` 10×
higher (0.005 → 0.05) together, so each recruit is a longer visible commitment at a ~unchanged net fill
rate. `progressSpeed` makes recruiting *faster* (higher per-meeting odds) without adding recruiters.
Headroom drives the rate to `0` as the roster fills, so a faction never overflows its cap.

### Discovery (EXPLORE) — `agent/action/cond/Discoverer.kt`, `Agent.wanderStep`, `Movement.wander`
The sibling of recruiting: an idle agent strolls to a nearby open-ground point (sweeping up stray XM on the
way) and, **on arrival**, resolves a neutral, **density-driven portal change** — a new portal is
**discovered** or a random one is found **gone** — then re-selects in the same tick (no `WAIT` flash).
Pill-less; animated as a small running circle in the scene.

This **replaces** the old per-checkpoint `Cycle.managePortalDensity`: portal churn is now something agents
*do* when idle, not a disembodied tick. It self-throttles — a busy board churns little (few agents idle),
while a sparse / portal-less board **floods** discovery (every idle agent routes here via the
`moveElsewhere` wander fallback) and fills fast.

**Concurrency cap.** `Config.maxConcurrentDiscoverers` (**2**) per faction discover at once *by choice*; the
rest seek work. (The flood case is the exception — when there's no portal to seek, agents wander regardless,
which is what bootstraps an empty board.)

**Outcome on arrival** — `Discoverer.resolve` → `ChurnMath.churnChances(count, target, rate, hasSpace)`:
```
d = count / targetPortals
create = rate × clamp01(1 − d/2)          // discovery ≫ removal when sparse, fades as the board fills
remove = rate × clamp01(d/2) (+ create if no space)   // removal grows past the target; tops up when packed
// on arrival: if (hasSpace && rnd < create) discover a new portal;  else if (count > minPortals && rnd < remove) one is gone
```
| knob | source | default |
|---|---|---|
| `rate` | `Config.portalChurnRate` (per-discovery chance scale) | `0.17` |
| `target` | `Config.targetPortals()` = `startPortals × 2.5`, capped `[startPortals, maxPortals]` | `20` at default `startPortals 8` |
| `hasSpace` | `count < maxPortals (89)` **and** `Positions.hasPortalSpace()` (room for a non-clipping portal) | — |
| floor | `Config.minPortals` | `5` |

The count **converges to the target**: when sparse, `create ≫ remove` so the board fills; at the target
they're ~equal (gentle churn); above it, removal wins. It's also hard-capped at the board's **walkable
capacity** — when there's no room for a non-clipping portal, `hasSpace` is false and the create budget rolls
into removal, so a packed board can't overflow its open ground. **No success/fail beyond these rolls** — the
agent always completes the stroll; the portal change is the (probabilistic) payoff.

## Where to look
- `agent/Agent.kt` — `act()`, the self-managed steps (`attackPortal` / `deployPortal` /
  `moveCloserToDestinationPortal` / `wanderStep` / `recruitStep`).
- `agent/action/ActionSelector.kt` — context routing, `q()`, the candidate lists, `idle()`.
- `agent/action/ActionItem.kt` / `Action.kt` — the action records + the busy timer.
- `agent/action/cond/*` — one handler per action (`isActionPossible` + `performAction`), plus the two idle
  fallbacks `Recruiter` and `Discoverer`.
- `agent/qvalue/` — `QValue`, `QActions`, `QDestinations` (the sliders + base weights).
- `ai/FactionPolicy.kt` — how a slider weighting is sourced (DOM / AI / overrides).
- `agent/Balance.kt` + `agent/BalanceMath.kt` — `recruitFactor` and the anti-snowball math.
- `system/ChurnMath.kt` — the pure density-churn curve behind discovery (create/remove vs target).
