package config

import agent.Faction
import system.ui.Bootstrap

object Config {
    const val minPortals = 5 // the board never churns/gens below this (always ≥5 portals on a map)
    const val maxPortals = 89
    const val minFrogs = 2
    const val maxFrogs = 32 // the hard per-faction ceiling (giant-map / end-game roster); the live cap scales by size — see [maxFor]
    const val minSmurfs = 2
    const val maxSmurfs = 32
    const val frogQuitRate = 0.0 // 0 = agents don't quit; rosters only GROW (via recruiting) toward the cap. Raise to re-enable churn.
    const val smurfQuitRate = 0.0
    const val factionChangeRate = 0.0 // 0 = no defections either (a defection shrinks a faction). Raise to re-enable.

    // Density-driven portal churn (system/Cycle.managePortalDensity, every checkpoint). Portals are
    // DISCOVERED and REMOVED as a neutral process that converges the count toward [targetPortals]: well below
    // target, discovery ≫ removal (~4:1) so the board fills; at target, ~1:1; above, removal wins. Replaces
    // the old agent EXPLORE action + cycle-end removePortals.
    var portalChurnRate = 0.17 // per-checkpoint activity (chance scale for a create/remove); ~⅓ the old 0.5 (churn was too fast)

    /** Equilibrium portal count the churn converges toward — grows from [startPortals], capped at [maxPortals]. */
    fun targetPortals(): Int = ConfigMath.targetPortals(startPortals, maxPortals)

    // --- Recruitment balance (Phase 5) ---------------------------------------
    // Recruiting is the IDLE FALLBACK (like EXPLORE) — an agent recruits a nearby NPC when it has no gameplay
    // action left ([agent.action.cond.Recruiter] via ActionSelector), NOT a Q-slider and NOT in the roulette.
    // Per-meeting SUCCESS chance at an empty roster: scaled live by [progressSpeed] (the "make it faster" lever),
    // the anti-snowball [agent.Balance.recruitFactor], roster headroom (→0 at the cap) + the recruiter's aptitude.
    var recruitmentBaseChance = 0.3

    // Cap on how many agents per faction recruit AT ONCE — the rest of the idle agents explore instead, so a quiet
    // board (everything burnt out) doesn't show EVERY agent recruiting. Recruiting completes + frees a slot.
    var maxConcurrentRecruiters = 2

    // Anti-snowball recruiting (Balance.recruitFactor): the LARGER faction recruits less, the SMALLER more, so
    // team sizes self-correct instead of the leader running away. Multiplies the recruit RATE (not selection — there
    // is no selection now) by (enemyRoster+1)/(myRoster+1), clamped to this band.
    var recruitFactorMin = 0.3 // a much-larger faction recruits at ≥30% of base
    var recruitFactorMax = 3.0 // a much-smaller faction recruits at ≤300% of base

    // Min resonators an owned portal needs before it can link out (Portal.isLinkable). Low (Ingress-like:
    // any owned portal links) so fields form even on a fast-flipping board; raise toward 8 for tankier,
    // harder-to-field play. The field-formation lever.
    var linkMinResos = 1

    var startPortals = 8 // initial portal count (chosen at onboarding — the "portal density"); scales by map size
    var startStage = StartStage.MID // onboarding pick: how far along the game starts (roster + level + gear)
    fun startFrogs() = rosterForStart()
    fun startSmurfs() = rosterForStart()
    fun initialAp() = startStage.initialAp

    /** Per-faction starting roster, by START STAGE × MAP SIZE: a normal start is always a single agent; a
     *  mid-game seeds [Sim.suggestedAgents] (Tiny 3 · Small 5 · Mid 8 · Large 12 · Giant 16); an end-game seeds
     *  the FULL size roster ([rosterCap] — Tiny 8 · Small 16 · Mid 24 · Large 28 · Giant 32). Recruiting then
     *  grows the roster up to [rosterCap]. */
    private fun rosterForStart(): Int = when (startStage) {
        StartStage.START -> 1
        StartStage.MID -> Sim.suggestedAgents(Sim.areaKm2())
        StartStage.END -> rosterCap()
    }

    /** Live per-faction agent cap, scaled by map size ([Sim.maxAgents]: Tiny 8 … Giant 32, the hard ceiling) so a
     *  tiny/small map can't fill with the giant roster. Rosters grow via recruiting toward this; nothing shrinks them. */
    fun rosterCap(): Int = Sim.maxAgents(Sim.areaKm2())

    // Per-agent inventory cap (authentic Ingress = 2000). When full, an agent can't hack/glyph for more items
    // (Hacker.isActionPossible) — it must spend (deploy/attack/link) or recycle to free space (Recycler).
    var maxInventory = 2000

    // NPC population is NOT a player setting — it's auto-derived from map area + location (see
    // [npcPopulation]) at world-gen. Recruiting draws it down over time; it's only refilled once it
    // reaches [MIN_NONFACTION] (Recruiter), so a game never runs out of people to recruit.
    var maxNonFaction = 500 // current target population (set by npcPopulation at world-gen)
    fun maxFor(faction: Faction? = null) = when (faction) {
        Faction.ENL, Faction.RES -> rosterCap() // the size-scaled per-faction agent cap (not the raw 32 ceiling)
        else -> maxNonFaction
    }

    // Floors/ceilings for the auto NPC population — the pure values live in [ConfigMath]; re-exported here so
    // existing Config.MIN_NONFACTION / MAX_NONFACTION_CAP call sites are unchanged.
    val MIN_NONFACTION = ConfigMath.MIN_NONFACTION
    val MAX_NONFACTION_CAP = ConfigMath.MAX_NONFACTION_CAP

    /** Player NPC-density multiplier (0.1–3.0), chosen at onboarding — scales the auto population. */
    var npcMultiplier = 1.0

    /** Auto NPC population for a [width]×[height] map at the given [walkability] (and optional [tourist]
     *  hotspot bonus), scaled by [npcMultiplier]. Pure model in [ConfigMath.npcPopulation]. */
    fun npcPopulation(width: Int, height: Int, walkability: Double, tourist: Boolean = false): Int {
        val areaRatio = (width.toDouble() * height) / (Dim.width.toDouble() * Dim.height)
        return ConfigMath.npcPopulation(areaRatio, walkability, tourist, npcMultiplier)
    }

    /** Building-shake intensity multiplier (0–2), tunable live from the menu "Building shake" slider. */
    var buildingShakeMultiplier = 1.0

    // Peak comeback strength (Balance.attackBoost): a fully-shut-out faction deals up to
    // `comebackMax × dynamism ×` deficit² extra resonator damage, so it can tear down the leader's fielded
    // portals and turn the board. The anti-runaway lever (tuned via headless sweeps; ai/BalanceSweep).
    var comebackMax = 3.0 // headless sweep optimum (ai/BalanceSweep): >3 over-corrects → chaotic + unbalanced

    // Dominance decay (Portal.erodeByDominance, every checkpoint): the further AHEAD a faction is on MU, the
    // faster its OWN resonators erode (scale = leadShare × this), so an over-extended empire crumbles and the
    // board reopens → the lead changes. 0 = off. The anti-runaway *mechanic* (vs comebackMax, a combat boost);
    // headless sweep showed it lifts lead-sharing balance from ~0.37 (runaway) to ~0.75 between equal factions.
    var dominanceDecay = 3.0

    // Leader tempo handicap (ActionSelector): a leading faction's agents WANDER instead of acting with
    // probability leadShare × this, so the side that's ahead spends time on faction-neutral movement and the
    // trailing side keeps its focus. A behavioural anti-runaway lever (complements dominanceDecay). Default
    // 0.5: the headless sweep showed it ~doubles MU-lead changes (2.5→4.5/match) at high balance (0.83);
    // above ~0.6 it over-corrects (the leader can't hold anything → MU collapses).
    var leaderDistraction = 0.5

    // Combat dynamism (0 = realistic/tanky shields, slow board … 1 = portals flip very easily). The SINGLE
    // combat knob (menu "Combat dynamics" slider) — it drives shield mitigation, weapon-drop rate, how eagerly
    // agents attack, and the underdog comeback. Default 0.6: the headless balanced-dynamics sweep optimum
    // (ai/BalanceSweep) — the MU lead alternates and is shared ~evenly between equally-tuned factions, with
    // real fields forming, without descending into chaos. (The authentic 95% mitigation cap is in IngressFacts.)
    var combatDynamism = 0.6

    /** Gameplay shield/link mitigation cap for the current dynamism: higher dynamism → lower cap → flips. */
    fun maxMitigation(): Int = ConfigMath.maxMitigation(combatDynamism)

    /** Weapon-drop multiplier (XMP + Ultra-Strike yield per hack) vs the base [DropRates] rate: `1×` … `20×`
     *  as dynamism rises, so a dynamic sim hands out the firepower needed to flip defended portals. */
    fun weaponDropMultiplier(): Double = ConfigMath.weaponDropMultiplier(combatDynamism)

    /** XMPs an agent hoards before committing to an assault: `30` (cautious) … `8` (trigger-happy) as
     *  dynamism rises — lower means assaults start sooner and portals flip more often. */
    fun attackXmpThreshold(): Int = ConfigMath.attackXmpThreshold(combatDynamism)

    /** Comeback bonus (`Balance.attackBoost`): the faction behind on portals deals up to this fraction MORE
     *  resonator damage at a full deficit, so a losing side can turn the board. Scales with dynamism. */
    fun comebackAttackBonus(): Double = ConfigMath.comebackAttackBonus(combatDynamism)

    const val apMultiplier = 10

    // One "Progress speed" knob (like combatDynamism) for how fast the game ramps early→endgame: it scales
    // BOTH the recruiting success chance (Recruiter.recruitmentChance) AND AP gain (Agent.addAp → level faster).
    // 1.0 = baseline; <1 slower, >1 faster. Live-tunable + persisted (GameplayPrefs).
    var progressSpeed = 1.0

    const val isNpcSwarming = true
    const val npcXmSpawnRatio = 0.2

    // Stray-XM supply multiplier. XM (agent energy) used to spawn only at cycle end (every ticksPerCycle),
    // so agents starved mid-cycle and couldn't sustain assaults (firing XMPs costs real Ingress XM). XM now
    // spawns every checkpoint; this scales how much, so there's enough lying around to refuel from while
    // roaming (collected passively in range). Raise for an energy-rich, attack-heavy sim.
    var strayXmMultiplier = 2.0

    val isSoundOn = !Bootstrap.isLocal()
    const val isPlayInitialSound = false
    const val isSatOn = false

    const val isHighlighActionLimit = true
    const val vectorSmoothCount = 3
    const val shadowBlurCount = 3

    const val comMessageLimit = 8
    const val topAgentsMessageLimit = 8

    val ticksPerCheckpoint = Time.secondsToTicks(300)
    val ticksPerCycle = Time.secondsToTicks(1800)

    // Headless flow-field compute (PLAN Phase 6.1 / the SimRunner). Off by default: in the browser fields
    // are computed async (Pathfinding.computeFieldAsync) and in plain Node unit tests we skip them entirely
    // (agents bee-line). A headless match flips this on so Portal/NonFaction compute fields synchronously
    // (Pathfinding.computeFieldSync) — deterministic pathfinding without the coroutine event loop. Requires
    // World.grid to be initialised (the match harness sets it up first).
    var headlessFieldCompute = false
}
