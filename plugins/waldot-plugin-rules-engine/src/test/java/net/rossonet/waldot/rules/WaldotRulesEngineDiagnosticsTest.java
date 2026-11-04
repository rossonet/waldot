package net.rossonet.waldot.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.jexl.JexlContextRegistry;
import net.rossonet.waldot.rules.vertices.RuleVertex;
import net.rossonet.waldot.rules.vertices.RuleVertex.Execution;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Diagnostics of the rules: last-error and last-error-time for every phase,
 * reset-counters, run-once, context of the scripts.
 */
public class WaldotRulesEngineDiagnosticsTest {

	private static final long WAIT_MS = 10000;

	private static long num(final Vertex v, final String label) {
		return ((Number) v.property(label).value()).longValue();
	}

	private static String text(final Vertex v, final String label) {
		return String.valueOf(v.property(label).value());
	}

	private static void waitFor(final String what, final BooleanSupplier condition) throws InterruptedException {
		final long end = System.currentTimeMillis() + WAIT_MS;
		while (System.currentTimeMillis() < end) {
			if (condition.getAsBoolean()) {
				return;
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

	@BeforeEach
	public void beforeEach() throws Exception {
		clean();
		g = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy());
	}

	private void clean() throws Exception {
		if (g != null && g.getWaldotNamespace() != null) {
			g.getWaldotNamespace().close();
			g = null;
		}
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		Files.deleteIfExists(Path.of("/tmp/index.txt"));
		WaldotTestEnvironment.awaitOpcPortReleased();
	}

	private WaldotCommand command(final String rule, final String name) {
		final UaNode node = g.getWaldotNamespace().getStorageManager()
				.getNode(g.getWaldotNamespace().generateNodeId(rule + "." + name)).orElse(null);
		assertNotNull(node, "command " + name + " of " + rule);
		return (WaldotCommand) node;
	}

	@Test
	public void conditionCompileError() {
		final RuleVertex rule = rule("r1", "1 +* )", "1");
		final Execution e = rule.runOnce();
		assertEquals("error", e.status());
		assertTrue(e.error().startsWith("compilazione della condizione: riga 1"), e.error());
		assertLastError(rule, "compilazione della condizione");
	}

	@Test
	public void conditionRuntimeError() {
		final RuleVertex rule = rule("r1", "missing > 1", "1");
		final Execution e = rule.runOnce();
		assertEquals("error", e.status());
		assertTrue(e.error().contains("esecuzione della condizione"), e.error());
		assertTrue(e.error().contains("variabile 'missing' non definita"), e.error());
		assertTrue(e.error().contains("  missing > 1"), "excerpt: " + e.error());
		assertLastError(rule, "esecuzione della condizione");
	}

	@Test
	public void conditionThatIsNotABoolean() {
		final RuleVertex rule = rule("r1", "42", "1");
		final Execution e = rule.runOnce();
		assertEquals("error", e.status());
		assertEquals(42, e.conditionResult());
		assertTrue(e.error().contains("deve restituire un booleano"), e.error());
		assertTrue(e.error().contains("Integer"), e.error());
		assertLastError(rule, "booleano");
	}

	@Test
	public void actionCompileError() {
		final RuleVertex rule = rule("r1", "true", "var mod = 3;");
		final Execution e = rule.runOnce();
		assertEquals("error", e.status());
		assertTrue(e.error().contains("compilazione dell'azione"), e.error());
		assertTrue(e.error().contains("parola riservata"), e.error());
		assertLastError(rule, "compilazione dell'azione");
	}

	@Test
	public void actionRuntimeError() {
		final RuleVertex rule = rule("r1", "true", "var a = 1;\nvar b = self.property('nope').value() + 1;");
		final Execution e = rule.runOnce();
		assertEquals("error", e.status());
		assertTrue(e.error().startsWith("esecuzione dell'azione: riga 2"), e.error());
		assertLastError(rule, "esecuzione dell'azione");
		assertEquals(1, rule.getErrors());
		assertEquals(0, rule.getExecuted());
	}

	private void assertLastError(final RuleVertex rule, final String expected) {
		final String lastError = text(rule, RulesEngineConstants.LAST_ERROR_LABEL);
		assertTrue(lastError.contains(expected), lastError);
		final Instant time = Instant.parse(text(rule, RulesEngineConstants.LAST_ERROR_TIME_LABEL));
		assertTrue(time.isAfter(Instant.now().minusSeconds(60)), time.toString());
		assertEquals(1, num(rule, RulesEngineConstants.ERRORS_SIZE_LABEL));
	}

	@Test
	public void contextHasMathRandCmdAndSelf() {
		final RuleVertex rule = rule("r1", "true",
				"math.max(1, 2) + rand.nextInt(1) + cmd.toNumber('3').intValue() + (self.id() == null ? 100 : 0)");
		final Execution e = rule.runOnce();
		assertEquals("executed", e.status(), String.valueOf(e.error()));
		assertEquals(5, ((Number) e.actionResult()).intValue());
	}

	@Test
	public void errorsFiredByAnEdgeAreRecordedToo() throws Exception {
		final Vertex input = g.addVertex("id", "in", "value", "0");
		final Vertex compute = g.addVertex("id", "c1", "type", "rules:compute", "name", "c1");
		final RuleVertex rule = rule("r1", "true", "1 / 0");
		compute.addEdge("runner", rule, "type", "rules:execute");
		input.addEdge("fire", rule, "type", "fire");
		input.property("value", "1");
		waitFor("error recorded", () -> num(rule, RulesEngineConstants.ERRORS_SIZE_LABEL) == 1);
		waitFor("last-error", () -> text(rule, RulesEngineConstants.LAST_ERROR_LABEL).contains("esecuzione dell'azione"));
	}

	@Test
	public void lastErrorIsEmptyAtStart() {
		final RuleVertex rule = rule("r1", "true", "1");
		assertEquals("", text(rule, RulesEngineConstants.LAST_ERROR_LABEL));
		assertEquals("", text(rule, RulesEngineConstants.LAST_ERROR_TIME_LABEL));
	}

	@Test
	public void resetCountersThenANewErrorCountsFromZero() {
		final RuleVertex rule = rule("r1", "true", "1 / 0");
		for (int i = 0; i < 3; i++) {
			rule.runOnce();
		}
		assertEquals(3, rule.getErrors());
		assertEquals(3, rule.getTotal());
		final Object[] out = command("r1", RulesEngineConstants.RESET_COUNTERS_COMMAND).runCommand(null, new String[0]);
		assertEquals("reset", out[0]);
		assertEquals(0, num(rule, RulesEngineConstants.ERRORS_SIZE_LABEL));
		assertEquals(0, num(rule, RulesEngineConstants.TOTAL_SIZE_LABEL));
		assertEquals(0, num(rule, RulesEngineConstants.ACTION_EXECUTED_SIZE_LABEL));
		assertEquals("", text(rule, RulesEngineConstants.LAST_ERROR_LABEL));
		assertEquals("", text(rule, RulesEngineConstants.LAST_ERROR_TIME_LABEL));
		rule.runOnce();
		assertEquals(1, num(rule, RulesEngineConstants.ERRORS_SIZE_LABEL), "not the old count + 1");
		assertEquals(1, num(rule, RulesEngineConstants.TOTAL_SIZE_LABEL));
	}

	private RuleVertex rule(final String id, final String condition, final String action) {
		return (RuleVertex) g.addVertex("id", id, "type", "rules:rule", "name", id, "condition", condition, "action",
				action);
	}

	@Test
	public void ruleContextIsRegistered() {
		final JexlContextRegistry.Context context = JexlContextRegistry.get(JexlContextRegistry.RULE).orElseThrow();
		assertTrue(context.consoleCommands());
		assertTrue(context.variableNames().containsAll(List.of("g", "graph", "cmd", "log", "math", "rand", "self")),
				context.variableNames().toString());
	}

	@Test
	public void runOnceCommandReturnsTheOutcome() {
		rule("r1", "true", "self.property('done', 'yes'); 7");
		final Object[] out = command("r1", RulesEngineConstants.RUN_ONCE_COMMAND).runCommand(null, new String[0]);
		assertEquals(4, out.length);
		assertEquals("executed", out[0]);
		assertEquals("true", out[1]);
		assertEquals("7", out[2]);
		assertEquals("", out[3]);
		assertEquals("yes", g.traversal().V("r1").values("done").next());
	}

	@Test
	public void runOnceWithAFalseConditionSkips() {
		final RuleVertex rule = rule("r1", "false", "self.property('done', 'yes')");
		final Execution e = rule.runOnce();
		assertEquals("skipped", e.status());
		assertEquals(Boolean.FALSE, e.conditionResult());
		assertFalse(g.traversal().V("r1").has("done").hasNext());
		assertEquals(1, rule.getTotal());
		assertEquals(0, rule.getExecuted());
		assertEquals(0, rule.getErrors());
	}

	@Test
	public void successfulRunAfterAnErrorKeepsTheLastError() {
		final RuleVertex rule = rule("r1", "true", "1 / 0");
		rule.runOnce();
		final String lastError = text(rule, RulesEngineConstants.LAST_ERROR_LABEL);
		rule.property(RulesEngineConstants.ACTION_FIELD, "1");
		assertEquals("executed", rule.runOnce().status());
		assertEquals(lastError, text(rule, RulesEngineConstants.LAST_ERROR_LABEL),
				"last-error is the last failure, kept until reset-counters");
		assertEquals(1, rule.getExecuted());
	}
}
