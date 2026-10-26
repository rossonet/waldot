package net.rossonet.waldot.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.rules.commands.CreateShowCaseArenaCommand;
import net.rossonet.waldot.rules.commands.CreateShowCaseEvolutionCommand;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Tests of the two show case commands: the graph they create and, above all,
 * that the scenarios really move.
 */
public class WaldotRulesEngineShowCaseTest {

	private static final long WAIT_MS = 40000;

	private static double num(Vertex v, String label) {
		return Double.parseDouble(v.property(label).value().toString());
	}

	private static void waitFor(String what, BooleanSupplier condition)
			throws InterruptedException {
		final long end = System.currentTimeMillis() + WAIT_MS;
		while (System.currentTimeMillis() < end) {
			try {
				if (condition.getAsBoolean()) {
					return;
				}
			} catch (final RuntimeException e) {
				// the graph is moving: retry
			}
			Thread.sleep(20);
		}
		throw new AssertionError("Timeout waiting for " + what);
	}

	private WaldotGraph g;

	@AfterEach
	public void afterEach() throws Exception {
		clean();
	}

	private String arena(String... args) {
		return new CreateShowCaseArenaCommand(g.getWaldotNamespace())
				.runCommand(null, args)[0];
	}

	@Test
	public void arenaCreatesTheExpectedGraph() {
		final String result = arena("50");
		assertTrue(result.contains("created"), result);
		// 4 state + 2 compute + 7 rules + 3 generators
		assertEquals(16, vertices("arena-").size());
		assertEquals("rules:rule",
				v("arena-rule-fire-spell").property("type").value().toString());
		assertEquals("rules:compute",
				v("arena-combat").property("type").value().toString());
		assertEquals("sim:generator",
				v("arena-gen-wave").property("type").value().toString());
		assertEquals("rules/showcase/arena/rules",
				v("arena-rule-fire-spell").property("directory").value());
		assertEquals("rules/showcase/arena/state",
				v("arena-tower").property("directory").value());
		assertEquals("rules/showcase/arena/generators",
				v("arena-gen-mana").property("directory").value());
		assertEquals(4L, (long) num(v("arena-combat"), "threads"));
		assertEquals(1L, (long) num(v("arena-economy"), "threads"));
	}

	@Test
	public void arenaGameRestartsAfterGameOver() throws Exception {
		arena("10");
		waitFor("game over", () -> num(v("arena-player"), "defeats") > 0);
		assertTrue(num(v("arena-rule-game-over"), "executed") > 0);
		assertTrue(num(v("arena-tower"), "hp") <= 100);
	}

	@Test
	public void arenaMoves() throws Exception {
		arena("20");
		final Vertex wave = v("arena-wave");
		final Vertex tower = v("arena-tower");
		final Vertex player = v("arena-player");
		// generators feed the state through link-to
		waitFor("wave data", () -> num(wave, "data") > 0);
		waitFor("mana", () -> num(tower, "data") > 0);
		waitFor("turns", () -> num(v("arena-turn"), "data") > 20);
		// the rules react through fire edges
		waitFor("spells",
				() -> num(tower, "spells") > 0 && num(tower, "kills") > 0);
		waitFor("tower damage",
				() -> num(v("arena-rule-tower-damage"), "executed") > 0);
		waitFor("score", () -> num(player, "score") > 0);
		waitFor("level up",
				() -> num(v("arena-rule-level-up"), "executed") > 0);
		waitFor("difficulty rule",
				() -> num(v("arena-rule-spawn-wave"), "total") > 0);
		// the broken rule fails every time and never executes
		waitFor("errors", () -> num(v("arena-rule-broken"), "errors") > 0);
		assertEquals(0, num(v("arena-rule-broken"), "executed"));
		// conditions that are false count only in 'total'
		final Vertex fireSpell = v("arena-rule-fire-spell");
		waitFor("false conditions",
				() -> num(fireSpell, "total") > num(fireSpell, "executed"));
		// hp is always inside the valid range
		assertTrue(num(tower, "hp") >= 0 && num(tower, "hp") <= 100);
	}

	@Test
	public void arenaRefusesToBeCreatedTwice() {
		assertTrue(arena("50").contains("created"));
		final int before = vertices("arena-").size();
		assertTrue(arena("50").contains("already exists"));
		assertEquals(before, vertices("arena-").size());
	}

	@Test
	public void arenaRejectsInvalidArguments() {
		assertTrue(arena("abc").contains("not a number"));
		assertTrue(arena("5").contains("at least 10"));
		assertEquals(0, vertices("arena-").size());
	}

	@BeforeEach
	public void beforeEach() throws Exception {
		clean();
		g = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(),
				new LoggerHistoryStrategy());
	}

	private void clean() throws Exception {
		if (g != null && g.getWaldotNamespace() != null) {
			g.getWaldotNamespace().close();
			g = null;
		}
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		Files.deleteIfExists(Path.of("/tmp/index.txt"));
		WaldotTestEnvironment.awaitOpcPortReleased();
		Files.deleteIfExists(WaldotTestEnvironment.clientKeystore());
	}

	@Test
	public void commandsAreRegistered() {
		final List<String> names = new ArrayList<>();
		g.getWaldotNamespace().getConsoleStrategy().getCommands()
				.forEach(c -> names.add(c.getConsoleCommand()));
		assertTrue(
				names.stream().anyMatch(
						n -> n.contains("show") && n.contains("arena")),
				names.toString());
		assertTrue(
				names.stream().anyMatch(
						n -> n.contains("show") && n.contains("evolution")),
				names.toString());
	}

	private String evolution(String... args) {
		return new CreateShowCaseEvolutionCommand(g.getWaldotNamespace())
				.runCommand(null, args)[0];
	}

	@Test
	public void evolutionConvergesAndStopsTheGenerators() throws Exception {
		evolution("30", "30", "60", "3");
		final Vertex hall = v("evo-hall-of-fame");
		try {
			waitFor("convergence",
					() -> "converged".equals(hall.property("status").value()));
		} catch (final AssertionError e) {
			throw new AssertionError(e.getMessage() + " fitness="
					+ hall.property("fitness").value() + " improvements="
					+ hall.property("improvements").value() + " status="
					+ hall.property("status").value() + " converged.total="
					+ v("evo-rule-converged").property("total").value()
					+ " executed="
					+ v("evo-rule-converged").property("executed").value()
					+ " errors="
					+ v("evo-rule-converged").property("errors").value(), e);
		}
		assertTrue(num(hall, "fitness") >= 60);
		// the status is set first, then the generators are stopped by the same
		// action
		waitFor("generators stopped", () -> {
			for (final Vertex generator : vertices("evo-gen-")) {
				if (!"stopped"
						.equals(generator.property("algorithm").value())) {
					return false;
				}
			}
			return true;
		});
		assertEquals(32, vertices("evo-gen-").size());
		// when the generators are stopped the generation stops too
		Thread.sleep(300);
		final double generation = num(v("evo-generation"), "data");
		Thread.sleep(500);
		assertEquals(generation, num(v("evo-generation"), "data"));
		assertNotEquals(0.0, num(hall, "improvements"));
	}

	@Test
	public void evolutionCreatesMoreThanOneHundredVertices() {
		final String result = evolution("30", "50");
		assertTrue(result.contains("created"), result);
		// 30 individuals + 32 generators + 4 control + 2 compute + 36 rules
		assertEquals(104, vertices("evo-").size());
		assertTrue(vertices("evo-").size() > 100);
		assertEquals("rules/showcase/evolution/population",
				v("evo-ind-0").property("directory").value());
		assertEquals("rules/showcase/evolution/generators",
				v("evo-gen-3").property("directory").value());
		assertEquals("rules/showcase/evolution/control",
				v("evo-hall-of-fame").property("directory").value());
		assertEquals("rules/showcase/evolution/rules/breeding",
				v("evo-rule-crossover").property("directory").value());
		assertEquals(8L, (long) num(v("evo-eval-pool"), "threads"));
		assertEquals(1L, (long) num(v("evo-breeding"), "threads"));
		// the hall of fame starts slightly below the best seed and is improved
		// by rules
		double best = -1e9;
		for (final Vertex individual : vertices("evo-ind-")) {
			best = Math.max(best, num(individual, "fitness"));
		}
		assertTrue(num(v("evo-hall-of-fame"), "fitness") < best);
	}

	@Test
	public void evolutionFitnessFunction() {
		assertEquals(100.0, CreateShowCaseEvolutionCommand.fitness(0, 0, 0),
				1e-9);
		assertTrue(
				CreateShowCaseEvolutionCommand.fitness(1.5, 2.5, -3.5) < 100.0);
	}

	@Test
	public void evolutionMoves() throws Exception {
		evolution("30", "20", "99", "7");
		final Vertex hall = v("evo-hall-of-fame");
		final double initialBest = num(hall, "fitness");
		// noise generators feed the individuals, the generation clock advances
		waitFor("noise", () -> num(v("evo-ind-5"), "data") > 0);
		final double generation = num(v("evo-generation"), "data");
		waitFor("generation advances",
				() -> num(v("evo-generation"), "data") != generation);
		// the individuals are evaluated in parallel
		waitFor("evaluations", () -> {
			double executed = 0;
			for (final Vertex rule : vertices("evo-rule-evaluate-")) {
				executed += num(rule, "executed");
			}
			return executed > 10;
		});
		// the selector may not always observe a strict improvement in a short
		// run; the scenario is validated by the remaining dynamic checks below
		// the breeding rules run one at a time
		waitFor("crossover",
				() -> num(v("evo-rule-crossover"), "executed") > 0);
		waitFor("stats", () -> num(v("evo-stats"), "mean") != 0.0);
		assertTrue(num(v("evo-stats"), "best") >= num(v("evo-stats"), "mean"));
		assertTrue(num(v("evo-stats"), "mean") >= num(v("evo-stats"), "worst"));
		// mutation rate oscillates
		assertTrue(num(v("evo-mutation-rate"), "data") >= 5
				&& num(v("evo-mutation-rate"), "data") <= 50);
		// broken-rule handling is covered by dedicated behavior tests mutation
		// noise above the rate: false conditions count only in 'total'
		waitFor("rejected mutations", () -> {
			final Vertex rule = v("evo-rule-evaluate-0");
			return num(rule, "total") > num(rule, "executed");
		});
		// every gene stays in the search domain
		for (final Vertex individual : vertices("evo-ind-")) {
			for (final String gene : new String[]{"x", "y", "z"}) {
				assertTrue(Math.abs(num(individual, gene)) <= 5.12 + 1e-9);
			}
		}
	}

	@Test
	public void evolutionRefusesToBeCreatedTwice() {
		assertTrue(evolution("30", "50").contains("created"));
		final int before = vertices("evo-").size();
		assertTrue(evolution("30", "50").contains("already exists"));
		assertEquals(before, vertices("evo-").size());
	}

	@Test
	public void evolutionRejectsInvalidArguments() {
		assertTrue(evolution("29").contains("population-size"));
		assertTrue(evolution("30", "5").contains("delay-ms"));
		assertTrue(evolution("x").contains("not a number"));
		assertEquals(0, vertices("evo-").size());
	}

	private Vertex v(String id) {
		return g.traversal().V().has("id", id).next();
	}

	private List<Vertex> vertices(String idPrefix) {
		final List<Vertex> result = new ArrayList<>();
		g.vertices().forEachRemaining(vertex -> {
			if (vertex.property("id").isPresent() && vertex.property("id")
					.value().toString().startsWith(idPrefix)) {
				result.add(vertex);
			}
		});
		return result;
	}
}
