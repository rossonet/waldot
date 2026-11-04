package net.rossonet.waldot.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.waldot.rules.vertices.ComputeVertex;
import net.rossonet.waldot.rules.vertices.RuleVertex;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Validation of the managed properties of the vertices (core,
 * AbstractOpcVertex), seen on the vertices of the rules engine: a value that
 * cannot be converted, or that a callback refuses, ends up in run!last-error.
 */
public class ManagedPropertyValidationTest {

	private static String lastError(final Vertex v) {
		final VertexProperty<Object> p = v.property(AbstractOpcVertex.LAST_ERROR_PROPERTY);
		return p.isPresent() ? String.valueOf(p.value()) : "";
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

	@Test
	public void callbackRefusalIsReported() {
		final Vertex rule = g.addVertex("id", "r1", "type", "rules:rule", "hysteresis", "-5");
		final String error = lastError(rule);
		assertTrue(error.contains("'hysteresis'"), error);
		assertTrue(error.contains("non-negative"), error);
	}

	@Test
	public void decimalForAnIntegerIsReportedAndTheDefaultIsUsed() {
		final RuleVertex rule = (RuleVertex) g.addVertex("id", "r1", "type", "rules:rule", "debug", "1.5");
		final String error = lastError(rule);
		assertTrue(error.contains("'debug'"), error);
		assertTrue(error.contains("'1.5'"), error);
		assertTrue(error.contains("Integer"), error);
		assertEquals(0, rule.getDebug());
		assertTrue(Instant.parse(String.valueOf(rule.property(AbstractOpcVertex.LAST_ERROR_TIME_PROPERTY).value()))
				.isAfter(Instant.now().minusSeconds(60)));
	}

	@Test
	public void runtimeInvalidValueIsReported() {
		final ComputeVertex compute = (ComputeVertex) g.addVertex("id", "c1", "type", "rules:compute");
		assertEquals("", lastError(compute));
		compute.property("threads", "molti");
		final String error = lastError(compute);
		assertTrue(error.contains("'threads'"), error);
		assertTrue(error.contains("molti"), error);
	}

	@Test
	public void validValuesHaveNoError() {
		final RuleVertex rule = (RuleVertex) g.addVertex("id", "r1", "type", "rules:rule", "debug", "2",
				"hysteresis", "5000.0");
		assertEquals("", lastError(rule));
		assertEquals(2, rule.getDebug());
	}
}
