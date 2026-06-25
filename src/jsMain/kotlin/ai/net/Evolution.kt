package ai.net

import agent.Faction
import ai.FactionPolicy
import ai.MatchSetup
import ai.SimRunner
import config.Config
import extension.Grid
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt

/** Knobs for a [Evolution.train] run (defaults are a sensible full-size run; tests pass tiny values). */
data class EvolutionConfig(
    val populationSize: Int = 24,
    val generations: Int = 20,
    val elite: Int = 4, // top genomes carried over unchanged (elitism → best fitness is monotonic)
    val mutationRate: Double = 0.15, // fraction of weights perturbed per offspring
    val mutationScale: Double = 0.3, // stddev of the gaussian perturbation
    val initScale: Double = 0.6, // stddev of initial random weights
    val arch: NetArch = NetArch.DEFAULT, // the net shape to evolve (default 13 → 16 → 16 → 17)
    val matchTicks: Int = Config.ticksPerCycle, // evaluate over a full scoring cycle
    val matchesPerEval: Int = 3, // seeds averaged per genome (variance reduction)
    val setup: MatchSetup = MatchSetup(),
)

/** The outcome of training: the winning genome, its fitness, and the best-per-generation curve. */
class EvolutionResult(val bestGenome: DoubleArray, val bestFitness: Double, val bestPerGeneration: List<Double>, val arch: NetArch) {
    /** A ready-to-install [NetPolicy] for [faction] from the winning genome. */
    fun bestPolicy(faction: Faction): NetPolicy = NetPolicy(Net.fromGenome(bestGenome, arch), faction)
}

/**
 * Neuroevolution trainer (PLAN Phase 6.2 / docs/NN.md) — evolves [Net] genomes that drive a faction, scored
 * by the headless [SimRunner]. The env is non-differentiable and the reward (MU margin) is episodic, so this
 * is a `(μ+λ)` GA with elitism + gaussian mutation rather than gradient RL. A [NetPolicy] drives ENL; the
 * [opponent] drives RES (null → the default uniform-slider baseline). Fully deterministic given (grid, seed,
 * config): the trainer's RNG is independent of [util.Util] (which `SimRunner` reseeds per match).
 */
object Evolution {

    fun train(
        grid: Grid,
        seed: Int,
        config: EvolutionConfig = EvolutionConfig(),
        opponent: () -> FactionPolicy? = { null },
    ): EvolutionResult {
        require(config.populationSize >= 1) { "need at least one genome" }
        require(config.elite in 1..config.populationSize) { "elite must be in 1..populationSize" }
        val rng = Rng(seed)
        var population = List(config.populationSize) { randomGenome(config, rng) }
        var bestGenome = population.first()
        var bestFitness = Double.NEGATIVE_INFINITY
        val history = mutableListOf<Double>()

        repeat(config.generations) {
            val ranked = population
                .map { it to evaluate(it, grid, seed, config, opponent) }
                .sortedByDescending { it.second }
            val championFitness = ranked.first().second
            if (championFitness > bestFitness) {
                bestFitness = championFitness
                bestGenome = ranked.first().first
            }
            history.add(championFitness)
            population = nextGeneration(ranked, config, rng)
        }
        return EvolutionResult(bestGenome, bestFitness, history, config.arch)
    }

    // Mean over [matchesPerEval] seeded matches of (our summed checkpoint MU − the foe's) — the fitness
    // objective: sustain the larger fields across the cycle. Match seeds are fixed across generations so
    // selection compares genomes on the same scenarios.
    private fun evaluate(genome: DoubleArray, grid: Grid, seed: Int, config: EvolutionConfig, opponent: () -> FactionPolicy?): Double {
        val net = Net.fromGenome(genome, config.arch)
        var total = 0.0
        repeat(config.matchesPerEval) { m ->
            val result = SimRunner.runMatch(
                grid,
                seed + m * MATCH_SEED_STRIDE,
                config.matchTicks,
                config.setup,
                policyEnl = NetPolicy(net, Faction.ENL),
                policyRes = opponent(),
            )
            total += result.checkpointMuSum(Faction.ENL) - result.checkpointMuSum(Faction.RES)
        }
        return total / config.matchesPerEval
    }

    private fun nextGeneration(ranked: List<Pair<DoubleArray, Double>>, config: EvolutionConfig, rng: Rng): List<DoubleArray> {
        val elites = ranked.take(config.elite).map { it.first }
        val offspring = List(config.populationSize - config.elite) {
            mutate(elites[rng.nextInt(elites.size)], config, rng)
        }
        return elites + offspring
    }

    private fun randomGenome(config: EvolutionConfig, rng: Rng): DoubleArray =
        DoubleArray(config.arch.genomeSize()) { rng.nextGaussian() * config.initScale }

    private fun mutate(genome: DoubleArray, config: EvolutionConfig, rng: Rng): DoubleArray = DoubleArray(genome.size) { i ->
        if (rng.next() < config.mutationRate) genome[i] + rng.nextGaussian() * config.mutationScale else genome[i]
    }

    private const val MATCH_SEED_STRIDE = 7919 // spreads the per-eval match seeds
}

/**
 * A small independent mulberry32 PRNG. The trainer's randomness must NOT use [util.Util], which `SimRunner`
 * reseeds on every match — same algorithm as `Util.random` so it's known-good, but with its own state.
 */
private class Rng(seed: Int) {
    private var state = seed

    fun next(): Double {
        val a = state + 0x6D2B79F5
        state = a
        var t = (a xor (a ushr 15)) * (1 or a)
        t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
        return (t xor (t ushr 14)).toUInt().toDouble() / 4294967296.0
    }

    fun nextInt(bound: Int): Int = (next() * bound).toInt()

    // Box-Muller — one normal sample per call (the cached second sample isn't worth the state).
    fun nextGaussian(): Double {
        val u1 = next().coerceAtLeast(1e-12)
        val u2 = next()
        return sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
    }
}
