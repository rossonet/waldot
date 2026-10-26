package net.rossonet.waldot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.test.WaldotTestEnvironment;

class AddLinkMonitoredEdgeCommandTest {

	private WaldotGraph graph;

	@BeforeEach
	void setUp() throws Exception {
		cleanup();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(),
				new LoggerHistoryStrategy());
	}

	@AfterEach
	void tearDown() throws Exception {
		cleanup();
	}

	private void cleanup() throws Exception {
		if (graph != null) {
			graph.getWaldotNamespace().close();
			graph = null;
		}
		WaldotTestEnvironment.awaitOpcPortReleased();
	}

	@Test
	void createsLinkFromWhenEdgeInvertedIsTrue() {
		graph.addVertex("id", "source", "label", "source");
		graph.addVertex("id", "target", "label", "target");

		final AddLinkMonitoredEdgeCommand command = new AddLinkMonitoredEdgeCommand(
				graph.getWaldotNamespace());
		command.runCommand(null,
				new String[] { "l1", "source", "target", "", "", "",
						"true", "", "", "" });

		assertEquals(1L,
				graph.traversal().E().has("type", MiloStrategy.LINK_FROM_EDGE_TYPE)
						.count().next().longValue());
		assertEquals(0L,
				graph.traversal().E().has("type", MiloStrategy.LINK_TO_EDGE_TYPE)
						.count().next().longValue());
	}

	@Test
	void createsLinkToWhenEdgeInvertedIsFalse() {
		graph.addVertex("id", "source", "label", "source");
		graph.addVertex("id", "target", "label", "target");

		final AddLinkMonitoredEdgeCommand command = new AddLinkMonitoredEdgeCommand(
				graph.getWaldotNamespace());
		command.runCommand(null,
				new String[] { "l2", "source", "target", "", "", "",
						"false", "", "", "" });

		assertEquals(1L,
				graph.traversal().E().has("type", MiloStrategy.LINK_TO_EDGE_TYPE)
						.count().next().longValue());
		assertEquals(0L,
				graph.traversal().E().has("type", MiloStrategy.LINK_FROM_EDGE_TYPE)
						.count().next().longValue());
	}
}
