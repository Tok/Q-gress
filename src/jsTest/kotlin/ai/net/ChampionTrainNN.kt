package ai.net

import agent.Faction
import ai.Driver
import ai.MatchSetup
import ai.SimRunner
import ai.Tournament
import config.Config
import extension.Grid
import system.grid.GridFixture
import kotlin.test.Test

/**
 * Headless champion **train** harness — the 2nd-generation rebake (driven by `scripts/train-champs.sh`).
 * Where [ChampionBake] trains gen-1 champions against the adaptive [ai.HeuristicPolicy] baseline, this trains
 * each architecture's fresh **challenger against that SAME arch's current repo champion** (NN-vs-NN), then runs
 * a round-robin **decider** (challenger elites + the incumbent) and only crowns a new champion if a challenger
 * actually beats the incumbent — so a repo genome is replaced only on a real improvement.
 *
 * Env-gated exactly like ChampionBake (so a long sweep can't leak memory into one Node process):
 *  - `TRAIN_CHAMPS=1` + optional `TRAIN_ARCH=16-16` — train + decide one arch (or the whole 25 sweep).
 *  - `TRAIN_OVERALL=1` — the cross-arch ladder report (run once, after the per-arch updates).
 *  - `TRAIN_POP` / `TRAIN_GENS` / `TRAIN_SEEDS` — override the budget (for a fast smoke run).
 * It reuses the same [Evolution] / [Tournament] / [SimRunner] / [GenomeIO] the in-game [system.ui.panel.TrainerPanel]
 * and [system.ui.panel.LeaderboardPanel] run on, so script and UI training are mechanism-equivalent.
 */
class ChampionTrainNN {

    private fun grid() = GridFixture("TRAIN", ARENA_W, ARENA_H, 2, GridFixture.rleEncode(List(ARENA_W * ARENA_H) { true })).toGrid()

    private fun nowMs(): Double = js("Date.now()").unsafeCast<Double>()

    private fun envValue(name: String): String? {
        val env: dynamic = js("(typeof process !== 'undefined' && process.env) ? process.env : null")
        val raw = if (env == null) null else env[name]
        return (raw as? String)?.takeIf { it.isNotEmpty() }
    }

    private fun envFlag(name: String): Boolean = envValue(name) == "1"
    private fun envInt(name: String, fallback: Int): Int = envValue(name)?.trim()?.toIntOrNull() ?: fallback

    private fun archs(): List<NetArch> {
        val only = envValue("TRAIN_ARCH")
        if (only != null) return listOf(NetArch(only.split("-").mapNotNull { it.trim().toIntOrNull() }))
        return WIDTHS.flatMap { h1 -> WIDTHS.map { h2 -> NetArch(listOf(h1, h2)) } }
    }

    @Test
    fun trainSweep() {
        if (!envFlag("TRAIN_CHAMPS")) return
        val grid = grid()
        val archs = archs()
        val budget = Budget(
            pop = envInt("TRAIN_POP", POP),
            gens = envInt("TRAIN_GENS", GENS),
            seeds = (1..envInt("TRAIN_SEEDS", TOURNEY_SEEDS)).toList(),
        )
        val start = nowMs()
        println("TRAIN start: ${archs.size} archs vs their own champions, pop ${budget.pop} x gen ${budget.gens} x $MATCHES_PER_EVAL seeds")
        archs.forEachIndexed { i, arch ->
            trainArch(grid, arch, "[${i + 1}/${archs.size} ${arch.label()}]", budget, start)
        }
        println("TRAIN done: ${archs.size} archs in ${fmt((nowMs() - start) / 1000.0)}s")
    }

    private class Budget(val pop: Int, val gens: Int, val seeds: List<Int>)

    private fun trainArch(grid: Grid, arch: NetArch, tag: String, budget: Budget, start: Double) {
        val incumbent = GenomeIO.decode(ChampionLibrary.jsonFor(arch)) // this arch's current repo champion
        val config = EvolutionConfig(
            populationSize = budget.pop,
            generations = budget.gens,
            elite = ELITE.coerceAtMost(budget.pop), // elite must fit the population (a small smoke-run pop can be < ELITE)
            matchesPerEval = MATCHES_PER_EVAL,
            arch = arch,
            matchTicks = TICKS,
            setup = MatchSetup(flowFields = false), // train cheap (straight-line); the decider re-checks with flow fields
        )
        println("TRAIN $tag training challenger vs incumbent… (@${fmt((nowMs() - start) / 1000.0)}s)")
        val session = Evolution.Session(grid, TRAIN_SEED, config) { NetPolicy(incumbent, Faction.RES) }
        while (!session.done) {
            val fit = session.step()
            println("TRAIN $tag   gen ${session.generation}/${budget.gens} trainBest=${fmt(fit)} (@${fmt((nowMs() - start) / 1000.0)}s)")
        }

        // Decider bracket: the incumbent vs the top challenger elites, round-robin (both colours) under the live
        // flow-field movement model — the accurate serve model, so an overfit straight-line winner can't sneak in.
        val challengers = (listOf(session.bestGenome) + session.elites()).distinct().take(TOURNEY_ELITES)
        val drivers = buildList {
            add(Driver("champion") { f -> NetPolicy(incumbent, f) })
            challengers.forEachIndexed { ci, g ->
                val net = Net.fromGenome(g, arch)
                add(Driver("challenger#${ci + 1}") { f -> NetPolicy(net, f) })
            }
        }
        println("TRAIN $tag deciding: ${drivers.size}-way round-robin over ${budget.seeds.size} seeds…")
        val standings = Tournament.roundRobin(grid, drivers, budget.seeds, TICKS, MatchSetup(flowFields = true))
        standings.forEachIndexed { r, s ->
            println("TRAIN $tag   #${r + 1} ${s.name}  W${s.wins} L${s.losses} T${s.ties}  avg=${fmt(s.avgMargin())}")
        }

        val winner = standings.first()
        if (winner.name == "champion") {
            println("TRAIN $tag incumbent held — no new champion (repo file unchanged)")
            return
        }
        val genome = challengers[winner.name.removePrefix("challenger#").toInt() - 1]
        val fitness = heldOutVsHeuristic(genome, arch) // tag with margin vs the baseline, for a display-consistent fitness
        println("TRAIN $tag NEW CHAMPION: ${winner.name} beat the incumbent (held-out vs baseline ${fmt(fitness)})")
        println("TRAINGENOME|${arch.label()}|${GenomeIO.encode(genome, arch, fitness)}")
    }

    @Test
    fun overallLadder() {
        if (!envFlag("TRAIN_OVERALL")) return
        val grid = grid()
        val seeds = (1..envInt("TRAIN_SEEDS", OVERALL_SEEDS)).toList()
        val drivers = ChampionLibrary.bakedArchs().take(envInt("TRAIN_MAX_ARCHS", Int.MAX_VALUE)).map { arch ->
            val net = GenomeIO.decode(ChampionLibrary.jsonFor(arch))
            Driver(arch.label()) { f -> NetPolicy(net, f) }
        }
        println("TRAIN overall: ${drivers.size}-champion round-robin over ${seeds.size} seeds…")
        val session = Tournament.Session(grid, drivers, seeds, TICKS, MatchSetup(flowFields = false))
        while (!session.done) {
            session.step()
            if (session.played % PROGRESS_EVERY == 0) println("TRAIN overall   ${session.played}/${session.total} matches…")
        }
        val standings = session.standings()
        println("TRAIN overall ladder (${drivers.size} champions, round-robin):")
        standings.forEachIndexed { r, s ->
            val mark = if (r == 0) "   <- overall champion" else ""
            println("TRAIN overall   #${r + 1} ${s.name}  W${s.wins} L${s.losses} T${s.ties}  avg=${fmt(s.avgMargin())}$mark")
        }
        println("TRAIN overall champion: ${standings.first().name}")
    }

    // Mean checkpoint-win fitness over VALIDATE_SEEDS unseen seeds vs the HeuristicPolicy baseline, under the live
    // flow-field movement — the same signal ChampionBake tags with, so a trained champion's fitness stays comparable.
    private fun heldOutVsHeuristic(genome: DoubleArray, arch: NetArch): Double {
        val net = Net.fromGenome(genome, arch)
        val grid = grid()
        var total = 0.0
        repeat(VALIDATE_SEEDS) { s ->
            val r = SimRunner.runMatch(
                grid,
                TRAIN_SEED + HELD_OUT_SEED_BASE + s,
                TICKS,
                MatchSetup(flowFields = true),
                policyEnl = NetPolicy(net, Faction.ENL),
                policyRes = ai.HeuristicPolicy(Faction.RES),
            )
            total += r.checkpointFitness(Faction.ENL)
            SimRunner.reset()
        }
        return total / VALIDATE_SEEDS
    }

    private fun fmt(d: Double) = ((d * 100).toInt() / 100.0).toString()

    companion object {
        private const val ARENA_W = 60 // same inscribed round arena as ChampionBake (comparable training conditions)
        private const val ARENA_H = 40
        private const val TRAIN_SEED = 42
        private const val HELD_OUT_SEED_BASE = 10_000
        private val TICKS = Config.ticksPerScoringCycle // a full scoring cycle (~35 checkpoints) — assess "winning the cycle"

        private const val POP = 16
        private const val GENS = 12
        private const val ELITE = 6
        private const val MATCHES_PER_EVAL = 3
        private const val VALIDATE_SEEDS = 6
        private const val TOURNEY_ELITES = 3 // challenger elites that enter the decider bracket alongside the incumbent
        private const val TOURNEY_SEEDS = 3 // per-pair seeds in the decider round-robin (each seed plays both colours)
        private const val OVERALL_SEEDS = 3
        private const val PROGRESS_EVERY = 50

        private val WIDTHS = listOf(4, 8, 16, 24, 32)
    }
}
