package net.rossonet.waldot.rules.commands;

import java.util.Random;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;

import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * Show case "Evolution Lab": an evolutionary algorithm that maximizes a
 * 3-gene Rastrigin function, modelled on the graph.
 * <p>
 * Every individual of the population is a vertex (genes x, y, z and
 * fitness). A generator per individual produces the "mutation noise" and a
 * <code>link-to</code> edge copies it into the individual; a
 * <code>fire</code> edge triggers the rule of that individual, which mutates
 * the genes when the noise is below the mutation rate and keeps the mutation
 * only if the fitness improves (hill climbing, evaluated in parallel by the
 * <code>eval-pool</code> compute). A second compute with one thread
 * (<code>breeding</code>) runs the rules that must not overlap: hall of fame,
 * crossover, stagnation (raises the mutation rate), convergence (stops the
 * generators), statistics (debug enabled) and a broken rule that shows the
 * <code>errors</code> counter. The mutation rate oscillates (sinusoidal
 * generator) between exploration and exploitation.
 * </p>
 * Input arguments: <code>population-size</code> (default 40, minimum 30,
 * so that the graph has more than 100 vertices), <code>delay-ms</code>
 * (period of the noise generators, default 200), <code>target-fitness</code>
 * (convergence threshold, default 99; the maximum possible is 100) and
 * <code>seed</code> (seed of the initial population, default 42).
 */
public class CreateShowCaseEvolutionCommand extends AbstractShowCaseCommand {

	public static final String BREEDING_COMPUTE_ID = "evo-breeding";
	public static final String CLOCK_GENERATOR_ID = "evo-gen-clock";
	public static final long DEFAULT_DELAY_MS = 200;
	public static final int DEFAULT_POPULATION = 40;
	public static final long DEFAULT_SEED = 42;
	public static final long DEFAULT_TARGET_FITNESS = 99;
	public static final String EVAL_COMPUTE_ID = "evo-eval-pool";
	public static final String GENERATION_ID = "evo-generation";
	/** The existence of this vertex marks the scenario as already created. */
	public static final String HALL_OF_FAME_ID = "evo-hall-of-fame";
	public static final String INDIVIDUAL_PREFIX = "evo-ind-";
	public static final int MIN_POPULATION = 30;
	public static final String MUTATION_RATE_ID = "evo-mutation-rate";
	public static final String NOISE_PREFIX = "evo-gen-";
	public static final String RATE_GENERATOR_ID = "evo-gen-rate";
	public static final String STATS_ID = "evo-stats";
	private static final String DIR = BASE_DIRECTORY + "/evolution";
	private static final double GENE_LIMIT = 5.12;
	private static final String TWO_PI = "6.283185307179586";

	/**
	 * Fitness of the genes: 100 minus the Rastrigin function (maximum 100 in
	 * the origin).
	 */
	public static double fitness(double x, double y, double z) {
		return 100.0 - (30.0 + gene(x) + gene(y) + gene(z));
	}

	private static double gene(double v) {
		return v * v - 10.0 * Math.cos(2.0 * Math.PI * v);
	}

	/** JEXL expression of {@link #fitness(double, double, double)}. */
	private static String fitnessExpr(String x, String y, String z) {
		return "(100.0 - (30.0 + (" + x + " * " + x + " - 10.0 * math.cos("
				+ TWO_PI + " * " + x + ")) + (" + y + " * " + y
				+ " - 10.0 * math.cos(" + TWO_PI + " * " + y + ")) + (" + z
				+ " * " + z + " - 10.0 * math.cos(" + TWO_PI + " * " + z
				+ ")))" + ")";
	}

	/** JEXL expression that clamps a gene in the search domain. */
	private static String clamp(String expression) {
		return "math.max(-" + GENE_LIMIT + ", math.min(" + GENE_LIMIT + ", "
				+ expression + "))";
	}

	public CreateShowCaseEvolutionCommand(
			final WaldotNamespace waldotNamespace) {
		super(waldotNamespace, "create show case evolution",
				"creates the Evolution Lab example: an evolutionary algorithm with more than 100 vertices");
		addStringArgument("population-size",
				"number of individuals (default " + DEFAULT_POPULATION
						+ ", minimum " + MIN_POPULATION + ")");
		addStringArgument("delay-ms",
				"period in milliseconds of the noise generators (default "
						+ DEFAULT_DELAY_MS + ", minimum 10)");
		addStringArgument("target-fitness",
				"fitness that stops the evolution (default "
						+ DEFAULT_TARGET_FITNESS + ", maximum possible 100)");
		addStringArgument("seed", "seed of the initial population (default "
				+ DEFAULT_SEED + ")");
	}

	@Override
	public Object clone() {
		return new CreateShowCaseEvolutionCommand(this.waldotNamespace);
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext,
			final String[] inputValues) {
		if (exists(HALL_OF_FAME_ID)) {
			return new String[]{
					"show case evolution already exists: remove its vertices before creating it again"};
		}
		final int population;
		final long delay;
		final long target;
		final long seed;
		try {
			population = (int) longArgument(inputValues, 0, DEFAULT_POPULATION,
					"population-size");
			delay = longArgument(inputValues, 1, DEFAULT_DELAY_MS, "delay-ms");
			target = longArgument(inputValues, 2, DEFAULT_TARGET_FITNESS,
					"target-fitness");
			seed = longArgument(inputValues, 3, DEFAULT_SEED, "seed");
		} catch (final IllegalArgumentException e) {
			return new String[]{e.getMessage()};
		}
		if (population < MIN_POPULATION || population > 1000) {
			return new String[]{"argument 'population-size' must be between "
					+ MIN_POPULATION + " and 1000"};
		}
		if (delay < 10) {
			return new String[]{"argument 'delay-ms' must be at least 10"};
		}

		// population
		final Random random = new Random(seed);
		final Vertex[] individuals = new Vertex[population];
		double bestFitness = -Double.MAX_VALUE;
		double bestX = 0;
		double bestY = 0;
		double bestZ = 0;
		for (int i = 0; i < population; i++) {
			final double x = (random.nextDouble() * 2 - 1) * GENE_LIMIT;
			final double y = (random.nextDouble() * 2 - 1) * GENE_LIMIT;
			final double z = (random.nextDouble() * 2 - 1) * GENE_LIMIT;
			final double f = fitness(x, y, z);
			if (f > bestFitness) {
				bestFitness = f;
				bestX = x;
				bestY = y;
				bestZ = z;
			}
			individuals[i] = vertex(INDIVIDUAL_PREFIX + i, null,
					"Individual " + i, DIR + "/population", "x", x, "y", y,
					"z", z, "fitness", f, "data", 0.0);
		}

		// control vertices
		final double initialHallFitness = bestFitness - 1.0;
		final Vertex hallOfFame = vertex(HALL_OF_FAME_ID, null,
				"Hall of fame", DIR + "/control", "fitness", initialHallFitness, "x",
				bestX, "y", bestY, "z", bestZ, "improvements", 0.0,
				"improved-at", 0.0, "stagnations", 0.0, "status", "running");
		final Vertex generation = vertex(GENERATION_ID, null, "Generation",
				DIR + "/control", "data", 0.0);
		final Vertex mutationRate = vertex(MUTATION_RATE_ID, null,
				"Mutation rate", DIR + "/control", "data", 25.0, "boost", 0.0);
		vertex(STATS_ID, null, "Statistics", DIR + "/control", "mean", 0.0,
				"best", bestFitness, "worst", 0.0);

		// compute vertices
		final Vertex evalPool = compute(EVAL_COMPUTE_ID, "Evaluation pool",
				DIR + "/compute", 8);
		final Vertex breeding = compute(BREEDING_COMPUTE_ID, "Breeding",
				DIR + "/compute", 1);

		// one rule per individual: mutation with the hill climbing acceptance
		final String rate = valOr(MUTATION_RATE_ID, "data", "25.0") + " + "
				+ valOr(MUTATION_RATE_ID, "boost", "0.0");
		final Vertex[] evaluateRules = new Vertex[population];
		for (int i = 0; i < population; i++) {
			final String id = INDIVIDUAL_PREFIX + i;
			final String v = refOrNull(id);
			final String action = "var v = " + v + "; if (v != null) { "
					+ "var step = (" + rate + ") / 50.0; "
					+ "var nx = " + clamp("v.property('x').value() + (rand.nextDouble() - 0.5) * step * 2.0")
					+ "; " + "var ny = "
					+ clamp("v.property('y').value() + (rand.nextDouble() - 0.5) * step * 2.0")
					+ "; " + "var nz = "
					+ clamp("v.property('z').value() + (rand.nextDouble() - 0.5) * step * 2.0")
					+ "; " + "var nf = " + fitnessExpr("nx", "ny", "nz")
					+ "; if (nf > v.property('fitness').value()) { "
					+ "v.property('x', nx); v.property('y', ny); v.property('z', nz); v.property('fitness', nf); } }";
			evaluateRules[i] = rule("evo-rule-evaluate-" + i,
					"Evaluate " + i, DIR + "/rules/evaluate",
					valOr(id, "data", "0.0") + " < " + rate, action, 0,
					0);
		}

		// rules that must not overlap (breeding compute)
		final String loopHead = "for (var i = 0; i < " + population
				+ "; i = i + 1) { var v = g.V().has('id', '"
				+ INDIVIDUAL_PREFIX + "' + i).tryNext().orElse(null); if (v != null) { ";
		final String generationValue = valOr(GENERATION_ID, "data", "0.0");
		final Vertex selectBest = rule("evo-rule-select-best", "Select best",
				DIR + "/rules/breeding", "true",
				"var bf = -1000000.0; var bi = 0; " + loopHead
						+ "var f = v.property('fitness').value(); if (f > bf) { bf = f; bi = i; } } } "
						+ "if (bf > " + valOr(HALL_OF_FAME_ID, "fitness", "-1000000.0")
						+ ") { var b = g.V().has('id', '" + INDIVIDUAL_PREFIX
						+ "' + bi).tryNext().orElse(null); if (b != null) { "
						+ set(HALL_OF_FAME_ID, "fitness", "bf") + "; "
						+ set(HALL_OF_FAME_ID, "x", "b.property('x').value()")
						+ "; "
						+ set(HALL_OF_FAME_ID, "y", "b.property('y').value()")
						+ "; "
						+ set(HALL_OF_FAME_ID, "z", "b.property('z').value()")
						+ "; "
						+ set(HALL_OF_FAME_ID, "improvements",
								valOr(HALL_OF_FAME_ID, "improvements", "0.0")
										+ " + 1.0")
						+ "; "
						+ set(HALL_OF_FAME_ID, "improved-at", generationValue)
						+ "; " + set(MUTATION_RATE_ID, "boost", "0.0")
						+ "; } }",
				150, 0);
		final Vertex crossover = rule("evo-rule-crossover", "Crossover",
				DIR + "/rules/breeding", generationValue + " % 3.0 == 0.0",
				"var bf = -1000000.0; var wf = 1000000.0; var bi = 0; var wi = 0; "
						+ loopHead
						+ "var f = v.property('fitness').value(); if (f > bf) { bf = f; bi = i; } if (f < wf) { wf = f; wi = i; } } } "
						+ "if (bi != wi) { var b = g.V().has('id', '"
						+ INDIVIDUAL_PREFIX
						+ "' + bi).tryNext().orElse(null); "
						+ "var w = g.V().has('id', '" + INDIVIDUAL_PREFIX
						+ "' + wi).tryNext().orElse(null); "
						+ "var r = g.V().has('id', '" + INDIVIDUAL_PREFIX
						+ "' + rand.nextInt(" + population
						+ ")).tryNext().orElse(null); "
						+ "if (b != null && w != null && r != null) { "
						+ "var nx = (b.property('x').value() + r.property('x').value()) / 2.0; "
						+ "var ny = (b.property('y').value() + r.property('y').value()) / 2.0; "
						+ "var nz = (b.property('z').value() + r.property('z').value()) / 2.0; "
						+ "w.property('x', nx); w.property('y', ny); w.property('z', nz); "
						+ "w.property('fitness', "
						+ fitnessExpr("nx", "ny", "nz") + "); } }",
				0, 0);
		final Vertex stagnation = rule("evo-rule-stagnation", "Stagnation",
				DIR + "/rules/breeding",
				generationValue + " - " + valOr(HALL_OF_FAME_ID, "improved-at", "0.0")
						+ " > 8.0",
				set(MUTATION_RATE_ID, "boost", "30.0") + "; "
						+ set(HALL_OF_FAME_ID, "stagnations",
								valOr(HALL_OF_FAME_ID, "stagnations", "0.0") + " + 1.0")
						+ "; "
						+ set(HALL_OF_FAME_ID, "improved-at", generationValue),
				0, 0);
		final StringBuilder stop = new StringBuilder();
		stop.append(set(HALL_OF_FAME_ID, "status", "'converged'")).append("; ");
		stop.append(set(CLOCK_GENERATOR_ID, "algorithm", "'stopped'"))
				.append("; ");
		stop.append(set(RATE_GENERATOR_ID, "algorithm", "'stopped'"))
				.append("; ");
		stop.append("for (var i = 0; i < ").append(population)
				.append("; i = i + 1) { g.V().has('id', '")
				.append(NOISE_PREFIX)
				.append("' + i).next().property('algorithm', 'stopped'); }");
		final Vertex converged = rule("evo-rule-converged", "Converged",
				DIR + "/rules/breeding",
				valOr(HALL_OF_FAME_ID, "status", "'running'") + " == 'running' && "
						+ valOr(HALL_OF_FAME_ID, "improvements", "0.0")
						+ " > 0.0 && "
						+ valOr(HALL_OF_FAME_ID, "fitness", "-1000000.0") + " >= " + target
						+ ".0",
				stop.toString(), 0, 0);
		final Vertex stats = rule("evo-rule-stats", "Statistics (debug)",
				DIR + "/rules/breeding", "true",
				"var sum = 0.0; var best = -1000000.0; var worst = 1000000.0; "
						+ loopHead
						+ "var f = v.property('fitness').value(); sum = sum + f; if (f > best) { best = f; } if (f < worst) { worst = f; } } "
						+ "} "
						+ set(STATS_ID, "mean", "sum / " + population + ".0")
						+ "; " + set(STATS_ID, "best", "best") + "; "
						+ set(STATS_ID, "worst", "worst"),
				0, 1);
		// broken on purpose: arithmetic exception, so 'errors' grows
		final Vertex broken = rule("evo-rule-bad-input", "Bad input (demo)",
				DIR + "/rules/breeding",
				"1 / 0 > 1.0",
				"log.info('never executed')", 0, 0);

		// execution. weight = priority * factor + queue size: select-best is the
		// busiest rule (one event per individual), so it gets a low priority to
		// avoid starving the rare ones on the single thread of the compute
		for (int i = 0; i < population; i++) {
			execute(evalPool, evaluateRules[i], "runs-evaluate-" + i, 10,
					DIR + "/compute");
		}
		execute(breeding, selectBest, "runs-select-best", 50,
				DIR + "/compute");
		execute(breeding, converged, "runs-converged", 190, DIR + "/compute");
		execute(breeding, crossover, "runs-crossover", 150, DIR + "/compute");
		execute(breeding, stagnation, "runs-stagnation", 100,
				DIR + "/compute");
		execute(breeding, stats, "runs-stats", 80, DIR + "/compute");
		execute(breeding, broken, "runs-bad-input", 60, DIR + "/compute");

		// triggers
		for (int i = 0; i < population; i++) {
			fire(individuals[i], evaluateRules[i], "noise-evaluates-" + i,
					"data", DIR + "/triggers");
			fire(individuals[i], selectBest, "fitness-selects-" + i, "fitness",
					DIR + "/triggers");
		}
		fire(hallOfFame, converged, "record-converges", "fitness",
				DIR + "/triggers");
		fire(generation, crossover, "generation-crosses", "data",
				DIR + "/triggers");
		fire(generation, stagnation, "generation-stagnates", "data",
				DIR + "/triggers");
		fire(generation, stats, "generation-counts", "data",
				DIR + "/triggers");
		fire(generation, broken, "generation-breaks", "data",
				DIR + "/triggers");

		// generators (created last: the scenario starts moving here)
		final Vertex clock = generator(CLOCK_GENERATOR_ID, "Generation clock",
				DIR + "/generators", "incremental", 0, 1000000, delay * 5);
		linkTo(clock, generation, "clock-to-generation", "data",
				DIR + "/generators");
		final Vertex rateGenerator = generator(RATE_GENERATOR_ID,
				"Mutation rate oscillator", DIR + "/generators", "sinusoidal",
				5, 50, delay * 10);
		linkTo(rateGenerator, mutationRate, "oscillator-to-rate", "data",
				DIR + "/generators");
		for (int i = 0; i < population; i++) {
			final Vertex noise = generator(NOISE_PREFIX + i, "Noise " + i,
					DIR + "/generators", "random", 0, 100, delay);
			linkTo(noise, individuals[i], "noise-to-" + i, "data",
					DIR + "/generators");
		}
		return new String[]{"show case evolution created: " + population
				+ " individuals, " + (population + 2)
				+ " generators, 2 compute, " + (population + 6)
				+ " rules, 4 control vertices"};
	}
}
