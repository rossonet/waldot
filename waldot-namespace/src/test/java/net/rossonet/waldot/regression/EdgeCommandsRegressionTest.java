package net.rossonet.waldot.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.client.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.base.CommandInvocationHandler;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.commands.AddFireMonitoredEdgeCommand;
import net.rossonet.waldot.commands.AddLinkMonitoredEdgeCommand;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Regression: the edge commands ({@code create link monitored edge},
 * {@code create fire monitored edge}) failed with {@code BadInternalError}
 * when called from an OPC UA client: the NodeId arguments were converted with
 * {@code NodeId.toString()} (not parseable) and the edge was created from the
 * target to the source vertex.
 */
class EdgeCommandsRegressionTest {

	// command folders are addressed without prefix, as the base strategy and
	// the client library do (e.g. ns=2;s=information, ns=2;s=manage/edges)
	private static final NodeId EDGES_FOLDER = new NodeId(2, "manage/edges");

	private static void awaitValue(final Vertex vertex, final String key, final Object expected)
			throws InterruptedException {
		final long deadline = System.currentTimeMillis() + 5_000;
		while (System.currentTimeMillis() < deadline) {
			final Object actual = vertex.property(key).isPresent() ? vertex.property(key).value() : null;
			if (actual != null && String.valueOf(expected).equals(String.valueOf(actual))) {
				return;
			}
			Thread.sleep(20);
		}
		throw new AssertionError(key + " of " + vertex.id() + " is "
				+ (vertex.property(key).isPresent() ? vertex.property(key).value() : null) + ", expected " + expected);
	}

	private WaldotGraph graph;

	private void assertSingleEdge(final String type, final String out, final String in) {
		final List<Edge> edges = graph.traversal().E().has("type", type).toList();
		assertEquals(1, edges.size(), "edges of type " + type);
		assertEquals(out, ((NodeId) edges.get(0).outVertex().id()).getIdentifier());
		assertEquals(in, ((NodeId) edges.get(0).inVertex().id()).getIdentifier());
	}

	private void cleanup() throws Exception {
		if (graph != null) {
			graph.getWaldotNamespace().close();
			graph = null;
		}
		WaldotTestEnvironment.awaitOpcPortReleased();
	}

	@Test
	void commandInputsKeepNodeIdsParseable() {
		assertEquals("ns=2;s=pump-1", CommandInvocationHandler.inputToString(new NodeId(2, "pump-1")));
		assertEquals("ns=2;i=120001",
				CommandInvocationHandler.inputToString(new NodeId(2, UInteger.valueOf(120001))));
		assertEquals("ns=2;s=pump-1",
				CommandInvocationHandler.inputToString(new NodeId(2, "pump-1").expanded()));
		assertEquals("true", CommandInvocationHandler.inputToString(Boolean.TRUE));
		assertEquals(null, CommandInvocationHandler.inputToString(null));
	}

	@Test
	void fireCommandCreatesEdgeFromSourceToTarget() {
		graph.addVertex("id", "src", "label", "src");
		graph.addVertex("id", "dst", "label", "dst");
		final String[] result = new AddFireMonitoredEdgeCommand(graph.getWaldotNamespace()).runCommand(null,
				new String[] { "f1", "ns=2;s=src", "ns=2;s=dst", "", "v", "", "", "", "" });
		assertTrue(result[0].startsWith("ns=2;"), result[0]);
		assertSingleEdge(MiloStrategy.FIRE_EDGE_TYPE, "src", "dst");
	}

	@Test
	void fireCommandOverOpcUaWithNodeIdArguments() throws Exception {
		graph.addVertex("id", "src", "label", "src");
		graph.addVertex("id", "dst", "label", "dst");
		try (RawOpcUaClient client = RawOpcUaClient.connect(new AnonymousProvider())) {
			final CallMethodResult result = client.call(EDGES_FOLDER, new NodeId(2, "create fire monitored edge"),
					new Variant("f-opc"), new Variant(new NodeId(2, "src")), new Variant(new NodeId(2, "dst")),
					new Variant(""), new Variant("v"), new Variant(true), new Variant(""), new Variant(0.0),
					new Variant(true));
			assertTrue(result.getStatusCode().isGood(), result.getStatusCode().toString());
			final String output = (String) result.getOutputArguments()[0].getValue();
			assertTrue(output.startsWith("ns=2;"), output);
		}
		assertSingleEdge(MiloStrategy.FIRE_EDGE_TYPE, "src", "dst");
	}

	@Test
	void linkCommandAcceptsParseableAndPlainIds() {
		graph.addVertex("id", "src", "label", "src");
		graph.addVertex("id", "dst", "label", "dst");
		final AddLinkMonitoredEdgeCommand command = new AddLinkMonitoredEdgeCommand(graph.getWaldotNamespace());
		final String[] parseable = command.runCommand(null,
				new String[] { "l1", "ns=2;s=src", "ns=2;s=dst", "", "", "", "false", "", "", "" });
		assertTrue(parseable[0].startsWith("ns=2;"), parseable[0]);
		assertSingleEdge(MiloStrategy.LINK_TO_EDGE_TYPE, "src", "dst");
		final String[] plain = command.runCommand(null,
				new String[] { "l2", "src", "dst", "", "", "", "true", "", "", "" });
		assertTrue(plain[0].startsWith("ns=2;"), plain[0]);
		assertSingleEdge(MiloStrategy.LINK_FROM_EDGE_TYPE, "src", "dst");
	}

	@Test
	void linkCommandOverOpcUaWithNodeIdArgumentsPropagatesValues() throws Exception {
		final Vertex src = graph.addVertex("id", "src", "label", "src", "v", 1);
		final Vertex dst = graph.addVertex("id", "dst", "label", "dst", "v", 0);
		try (RawOpcUaClient client = RawOpcUaClient.connect(new AnonymousProvider())) {
			final CallMethodResult result = client.call(EDGES_FOLDER, new NodeId(2, "create link monitored edge"),
					new Variant("l-opc"), new Variant(new NodeId(2, "src")), new Variant(new NodeId(2, "dst")),
					new Variant(""), new Variant("v"), new Variant(true), new Variant(false), new Variant(""),
					new Variant(0.0), new Variant(true));
			assertTrue(result.getStatusCode().isGood(), result.getStatusCode().toString());
			final String output = (String) result.getOutputArguments()[0].getValue();
			assertTrue(output.startsWith("ns=2;"), output);
		}
		assertSingleEdge(MiloStrategy.LINK_TO_EDGE_TYPE, "src", "dst");
		// link-to: the source writes into the target
		src.property("v", 5);
		awaitValue(dst, "v", 5);
	}

	@Test
	void missingVertexIsReportedAsError() {
		graph.addVertex("id", "src", "label", "src");
		final String[] link = new AddLinkMonitoredEdgeCommand(graph.getWaldotNamespace()).runCommand(null,
				new String[] { "l1", "src", "ns=2;s=missing", "", "", "", "", "", "", "" });
		assertTrue(link[0].startsWith("error:") && link[0].contains("missing"), link[0]);
		final String[] fire = new AddFireMonitoredEdgeCommand(graph.getWaldotNamespace()).runCommand(null,
				new String[] { "f1", "", "src", "", "", "", "", "", "" });
		assertTrue(fire[0].startsWith("error:"), fire[0]);
		assertEquals(0L, graph.traversal().E().count().next().longValue());
	}

	@BeforeEach
	void setUp() throws Exception {
		cleanup();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy());
	}

	@AfterEach
	void tearDown() throws Exception {
		cleanup();
	}
}
