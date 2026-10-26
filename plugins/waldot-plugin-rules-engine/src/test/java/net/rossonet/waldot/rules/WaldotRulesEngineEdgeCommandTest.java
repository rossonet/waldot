package net.rossonet.waldot.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.base.CommandInvocationHandler;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.rules.commands.AddComputeMonitoredEdgeCommand;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Regression: {@code create compute monitored edge} could not resolve the
 * NodeId arguments sent by an OPC UA client (they were converted with
 * {@code NodeId.toString()}) and failed with {@code BadInternalError}.
 */
public class WaldotRulesEngineEdgeCommandTest {

	private WaldotGraph g;

	@AfterEach
	public void afterEach() throws Exception {
		clean();
	}

	@BeforeEach
	public void beforeEach() throws Exception {
		clean();
		g = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(),
				new LoggerHistoryStrategy());
		g.addVertex("id", "c1", "type", "rules:compute", "name", "c1");
		g.addVertex("id", "r1", "type", "rules:rule", "name", "r1",
				"condition", "true", "action", "1");
	}

	private void clean() throws Exception {
		if (g != null && g.getWaldotNamespace() != null) {
			g.getWaldotNamespace().close();
			g = null;
		}
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		WaldotTestEnvironment.awaitOpcPortReleased();
	}

	@Test
	public void createsExecuteEdgeFromOpcUaNodeIdArguments() {
		// exactly what the invocation handler passes for NodeId variants
		final String source = CommandInvocationHandler
				.inputToString(new NodeId(2, "c1"));
		final String target = CommandInvocationHandler
				.inputToString(new NodeId(2, "r1"));
		final String[] result = new AddComputeMonitoredEdgeCommand(
				g.getWaldotNamespace()).runCommand(null,
						new String[]{"runner", source, target, "", "100"});
		assertTrue(result[0].startsWith("ns=2;"), result[0]);
		final List<Edge> edges = g.traversal().E()
				.has("type", RulesEngineConstants.EXECUTE_EDGE_LABEL).toList();
		assertEquals(1, edges.size());
		assertEquals("c1",
				((NodeId) edges.get(0).outVertex().id()).getIdentifier());
		assertEquals("r1",
				((NodeId) edges.get(0).inVertex().id()).getIdentifier());
		final Object priority = edges.get(0).property("priority").value();
		assertEquals("100", String.valueOf(priority));
	}

	@Test
	public void missingVertexIsReportedAsError() {
		final String[] result = new AddComputeMonitoredEdgeCommand(
				g.getWaldotNamespace()).runCommand(null,
						new String[]{"runner", "c1", "ns=2;s=missing", "", ""});
		assertTrue(result[0].startsWith("error:"), result[0]);
		assertEquals(0L, g.traversal().E().count().next().longValue());
	}
}
