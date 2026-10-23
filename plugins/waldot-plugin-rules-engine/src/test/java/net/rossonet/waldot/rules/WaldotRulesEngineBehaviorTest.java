package net.rossonet.waldot.rules;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.rules.vertices.ComputeVertex;
import net.rossonet.waldot.rules.vertices.RuleVertex;
import net.rossonet.waldot.utils.NetworkHelper;

/**
 * Tests of the rules engine logic: counters, errors, runtime changes of the
 * properties, defaults and invalid input.
 */
public class WaldotRulesEngineBehaviorTest {

	private static final long WAIT_MS = 10000;

	private WaldotGraph g;

	@AfterEach
	public void afterEach() throws Exception {
		clean();
	}

	@BeforeEach
	public void beforeEach() throws Exception {
		clean();
		g = OpcFactory.getOpcGraph("file:///tmp/boot.conf", new LoggerHistoryStrategy());
	}

	private Vertex newCompute(String id, Object... extra) {
		final Object[] base = {"id", id, "type", "rules:compute", "name", id};
		final Object[] all = new Object[base.length + extra.length];
		System.arraycopy(base, 0, all, 0, base.length);
		System.arraycopy(extra, 0, all, base.length, extra.length);
		return g.addVertex(all);
	}

	private Vertex newRule(String id, String condition, String action, Object... extra) {
		final Object[] base = {"id", id, "type", "rules:rule", "name", id, "condition", condition, "action", action};
		final Object[] all = new Object[base.length + extra.length];
		System.arraycopy(base, 0, all, 0, base.length);
		System.arraycopy(extra, 0, all, base.length, extra.length);
		return g.addVertex(all);
	}

	/** Compute -> rule (execute) and input -> rule (fire). */
	private Vertex wire(Vertex compute, Vertex rule, Vertex input) {
		compute.addEdge("runner", rule, "type", "rules:execute");
		input.addEdge("fire", rule, "type", "fire");
		return rule;
	}

	private static void waitFor(String what, BooleanSupplier condition) throws InterruptedException {
		final long end = System.currentTimeMillis() + WAIT_MS;
		while (System.currentTimeMillis() < end) {
			if (condition.getAsBoolean()) {
				return;
			}
			Thread.sleep(20);
		}
		throw new AssertionError("Timeout waiting for " + what);
	}

	private static long num(Vertex v, String label) {
		return ((Number) v.property(label).value()).longValue();
	}

	private void clean() throws Exception {
		if (g != null && g.getWaldotNamespace() != null) {
			g.getWaldotNamespace().close();
			g = null;
		}
		Files.deleteIfExists(Path.of("/tmp/boot.conf"));
		Files.deleteIfExists(Path.of("/tmp/index.txt"));
		while (!NetworkHelper.checkLocalPortAvailable(12686)) {
			Thread.sleep(10);
		}
		Files.deleteIfExists(Path.of("/tmp/waldot-client.ks"));
	}

	@Test
	public void pluginInstanceIsAvailable() {
		assertNotNull(WaldotRulesEnginePlugin.getInstance(g.getWaldotNamespace()));
		assertNotNull(WaldotRulesEnginePlugin.getInstance(g.getWaldotNamespace()).getJexlEngine());
	}

	@Test
	public void verticesHaveExpectedTypes() {
		final Vertex compute = newCompute("c1");
		final Vertex rule = newRule("r1", "true", "1");
		assertTrue(compute instanceof ComputeVertex);
		assertTrue(rule instanceof RuleVertex);
	}

	@Test
	public void computeDefaultsAndInvalidValues() {
		final ComputeVertex defaults = (ComputeVertex) newCompute("c1");
		assertEquals(WaldotRulesEnginePlugin.DEFAULT_THREAD_POOL_SIZE_IN_COMPUTE, defaults.getThreads());
		assertEquals(WaldotRulesEnginePlugin.DEFAULT_EXECUTION_TIMEOUT_MS_IN_COMPUTE,
				defaults.getExecutionTimeoutMs());
		assertEquals(WaldotRulesEnginePlugin.DEFAULT_PRIORITY_FACTOR_IN_COMPUTE, defaults.getPriorityFactor());
		final ComputeVertex custom = (ComputeVertex) newCompute("c2", "threads", "4", "execution-timeout-ms", "5000",
				"factor", "2.5");
		assertEquals(4, custom.getThreads());
		assertEquals(5000, custom.getExecutionTimeoutMs());
		assertEquals(2.5, custom.getPriorityFactor());
		final ComputeVertex invalid = (ComputeVertex) assertDoesNotThrow(
				() -> newCompute("c3", "threads", "-3", "execution-timeout-ms", "0"));
		assertEquals(WaldotRulesEnginePlugin.DEFAULT_THREAD_POOL_SIZE_IN_COMPUTE, invalid.getThreads());
		assertEquals(WaldotRulesEnginePlugin.DEFAULT_EXECUTION_TIMEOUT_MS_IN_COMPUTE,
				invalid.getExecutionTimeoutMs());
	}

	@Test
	public void computeWeightGrowsWithPriority() {
		final ComputeVertex compute = (ComputeVertex) newCompute("c1", "factor", "10");
		assertTrue(compute.calcolateWeight(5, 0) > compute.calcolateWeight(1, 0));
		assertTrue(compute.calcolateWeight(1, 7) > compute.calcolateWeight(1, 0));
		assertEquals(Integer.MAX_VALUE, compute.calcolateWeight(Integer.MAX_VALUE, Integer.MAX_VALUE));
	}

	@Test
	public void ruleDefaultsAndInvalidValues() {
		final RuleVertex rule = (RuleVertex) assertDoesNotThrow(
				() -> g.addVertex("id", "r1", "type", "rules:rule", "debug", "abc"));
		assertEquals(WaldotRulesEnginePlugin.DEFAULT_CONDITION_VALUE, rule.getCondition());
		assertEquals(WaldotRulesEnginePlugin.DEFAULT_ACTION_VALUE, rule.getAction());
		assertEquals(0, rule.getDebug());
		assertEquals(0, rule.getTotal());
		assertEquals(0, rule.getExecuted());
		assertEquals(0, rule.getErrors());
	}

	@Test
	public void trueConditionExecutesAction() throws Exception {
		final Vertex input = g.addVertex("id", "in", "value", "0");
		final Vertex compute = newCompute("c1");
		final Vertex rule = wire(compute, newRule("r1", "true", "self.property('counter', 7)", "counter", "0"), input);
		input.property("value", "1");
		waitFor("rule executed", () -> num(rule, "executed") == 1);
		assertEquals(1, num(rule, "total"));
		assertEquals(0, num(rule, "errors"));
		assertEquals(7, num(rule, "counter"));
		waitFor("empty queue", () -> num(rule, "queue") == 0);
	}

	@Test
	public void falseConditionCountsTotalOnly() throws Exception {
		final Vertex input = g.addVertex("id", "in", "value", "0");
		final Vertex compute = newCompute("c1");
		final Vertex rule = wire(compute, newRule("r1", "false", "1"), input);
		input.property("value", "1");
		waitFor("rule evaluated", () -> num(rule, "total") == 1);
		Thread.sleep(200);
		assertEquals(0, num(rule, "executed"));
		assertEquals(0, num(rule, "errors"));
	}

	@Test
	public void conditionErrorsAreCounted() throws Exception {
		final Vertex input = g.addVertex("id", "in", "value", "0");
		final Vertex compute = newCompute("c1");
		final Vertex broken = wire(compute, newRule("r1", "1 +* )", "1"), input);
		final Vertex notBoolean = wire(compute, newRule("r2", "'text'", "1"), input);
		final Vertex throwing = wire(compute, newRule("r3", "1 / 0", "1"), input);
		input.property("value", "1");
		waitFor("errors counted", () -> num(broken, "errors") == 1 && num(notBoolean, "errors") == 1
				&& num(throwing, "errors") == 1);
		assertEquals(0, num(broken, "executed"));
		assertEquals(0, num(notBoolean, "executed"));
		assertEquals(0, num(throwing, "executed"));
	}

	@Test
	public void actionErrorsAreNotCountedAsExecuted() throws Exception {
		final Vertex input = g.addVertex("id", "in", "value", "0");
		final Vertex compute = newCompute("c1");
		final Vertex badCompile = wire(compute, newRule("r1", "true", "1 +* )"), input);
		final Vertex badRun = wire(compute, newRule("r2", "true", "1 / 0"), input);
		input.property("value", "1");
		waitFor("errors counted", () -> num(badCompile, "errors") == 1 && num(badRun, "errors") == 1);
		assertEquals(0, num(badCompile, "executed"));
		assertEquals(0, num(badRun, "executed"));
	}

	@Test
	public void conditionAndActionChangeAtRuntime() throws Exception {
		final Vertex input = g.addVertex("id", "in", "value", "0");
		final Vertex compute = newCompute("c1");
		final Vertex rule = wire(compute, newRule("r1", "false", "self.property('counter', 1)", "counter", "0"),
				input);
		input.property("value", "1");
		waitFor("first evaluation", () -> num(rule, "total") == 1);
		assertEquals(0, num(rule, "executed"));
		rule.property("condition", "true");
		rule.property("action", "self.property('counter', 2)");
		assertEquals("true", ((RuleVertex) rule).getCondition());
		assertEquals("self.property('counter', 2)", ((RuleVertex) rule).getAction());
		input.property("value", "2");
		waitFor("changed rule executed", () -> num(rule, "executed") == 1);
		assertEquals(2, num(rule, "counter"));
	}

	@Test
	public void invalidRuntimeValuesDoNotBreakTheRule() throws Exception {
		final RuleVertex rule = (RuleVertex) newRule("r1", "true", "1");
		assertDoesNotThrow(() -> rule.property("debug", "not-a-number"));
		assertDoesNotThrow(() -> rule.property("hysteresis", "not-a-number"));
		assertDoesNotThrow(() -> rule.property("hysteresis", "-5"));
		assertTrue(rule.getDebug() >= 0);
	}

	@Test
	public void multipleThreadsExecuteAllEvents() throws Exception {
		final Vertex input = g.addVertex("id", "in", "value", "0");
		final Vertex compute = newCompute("c1", "threads", "3");
		final Vertex rule = wire(compute, newRule("r1", "true", "1"), input);
		for (int i = 1; i <= 5; i++) {
			input.property("value", String.valueOf(i));
			final long expected = i;
			waitFor("event " + i, () -> num(rule, "executed") == expected);
		}
		assertEquals(0, num(rule, "errors"));
	}

	@Test
	public void removedRuleIsForgottenByCompute() throws Exception {
		final Vertex input = g.addVertex("id", "in", "value", "0");
		final ComputeVertex compute = (ComputeVertex) newCompute("c1");
		final Vertex rule = newRule("r1", "true", "1");
		final org.apache.tinkerpop.gremlin.structure.Edge edge = compute.addEdge("runner", rule, "type",
				"rules:execute");
		input.addEdge("fire", rule, "type", "fire");
		edge.remove();
		input.property("value", "1");
		Thread.sleep(300);
		assertEquals(0, compute.getPendingRulesCount());
		assertEquals(0, num(rule, "executed"));
	}

	@Test
	public void closeStopsTheCompute() throws Exception {
		final ComputeVertex compute = (ComputeVertex) newCompute("c1");
		assertTrue(compute.isActive());
		compute.close();
		assertTrue(!compute.isActive());
	}
}
