package net.rossonet.waldot.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.commands.DeleteDirectory;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerBaseStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerOptimizedStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Regression: {@code delete directory} removed only the OPC UA folder (the
 * vertices and edges stayed in the graph) and {@code cmd.reset()} ran the
 * bootstrap again without removing the existing vertices.
 */
class DeleteDirectoryAndResetRegressionTest {

	private WaldotGraph graph;

	private void cleanup() throws Exception {
		if (graph != null) {
			graph.getWaldotNamespace().close();
			graph = null;
		}
		WaldotTestEnvironment.awaitOpcPortReleased();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
	}

	private void createPlant() {
		final Vertex pump = graph.addVertex("id", "pump-1", "label", "pump-1", MiloStrategy.DIRECTORY_PARAMETER,
				"plant/area-1", "v", 1);
		final Vertex tank = graph.addVertex("id", "tank-1", "label", "tank-1", MiloStrategy.DIRECTORY_PARAMETER,
				"plant/area-2", "v", 0);
		final Vertex other = graph.addVertex("id", "other-1", "label", "other-1", MiloStrategy.DIRECTORY_PARAMETER,
				"office");
		pump.addEdge("fills", tank, "type", MiloStrategy.LINK_TO_EDGE_TYPE, "properties-to-monitor", "v");
		other.addEdge("watches", pump);
	}

	private boolean hasNode(final String identifier) {
		return graph.getWaldotNamespace().hasNodeId(new NodeId(2, identifier));
	}

	private Set<String> vertexIds() {
		return graph.traversal().V().toList().stream().map(v -> ((NodeId) v.id()).getIdentifier().toString())
				.collect(Collectors.toSet());
	}

	@Test
	void deleteDirectoryCommandRemovesTheVerticesOfTheFolder() throws Exception {
		start(new MiloSingleServerOptimizedStrategy());
		createPlant();
		assertTrue(hasNode("v:plant/area-1"));
		final String[] result = new DeleteDirectory(graph.getWaldotNamespace()).runCommand(null,
				new String[] { "v:plant/area-1" });
		assertEquals("", result[1], "error output");
		assertEquals(Set.of("tank-1", "other-1"), vertexIds());
		// the incident edges are removed with the vertex
		assertEquals(0L, graph.traversal().E().count().next().longValue());
		assertFalse(hasNode("pump-1"));
		assertFalse(hasNode("v:plant/area-1"));
		assertTrue(hasNode("v:plant/area-2"));
	}

	@Test
	void deleteParentDirectoryRemovesTheWholeTree() throws Exception {
		start(new MiloSingleServerOptimizedStrategy());
		createPlant();
		final String deleted = graph.getWaldotNamespace().deleteDirectory("ns=2;s=v:plant");
		assertEquals("ns=2;s=v:plant", deleted);
		assertEquals(Set.of("other-1"), vertexIds());
		assertEquals(0L, graph.traversal().E().count().next().longValue());
		assertFalse(hasNode("v:plant"));
		assertFalse(hasNode("v:plant/area-1"));
		assertFalse(hasNode("v:plant/area-2"));
		assertTrue(hasNode("other-1"));
		// the directory can be used again
		graph.addVertex("id", "pump-2", "label", "pump-2", MiloStrategy.DIRECTORY_PARAMETER, "plant/area-1");
		assertTrue(hasNode("v:plant/area-1"));
	}

	@Test
	void deleteUnknownNodeIsReportedAsError() throws Exception {
		start(new MiloSingleServerOptimizedStrategy());
		final String[] result = new DeleteDirectory(graph.getWaldotNamespace()).runCommand(null,
				new String[] { "v:does-not-exist" });
		assertEquals("", result[0]);
		assertTrue(result[1].contains("not found"), result[1]);
	}

	@ParameterizedTest
	@ValueSource(strings = { "optimized", "base" })
	void resetRemovesTheGraphBeforeRunningTheBootstrapAgain(final String strategy) throws Exception {
		Files.writeString(WaldotTestEnvironment.bootFile(),
				"graph.addVertex('id', 'boot-v', 'label', 'boot-v', 'directory', 'boot')\n");
		start("base".equals(strategy) ? new MiloSingleServerBaseStrategy() : new MiloSingleServerOptimizedStrategy());
		assertEquals(Set.of("boot-v"), vertexIds());
		createPlant();
		assertEquals(4, vertexIds().size());
		graph.getWaldotNamespace().resetNameSpace();
		// only the vertex created by the bootstrap survives (created again)
		assertEquals(Set.of("boot-v"), vertexIds());
		assertEquals(0L, graph.traversal().E().count().next().longValue());
		assertFalse(hasNode("pump-1"));
		// a second reset gives the same result (no duplicates, no errors)
		graph.getWaldotNamespace().resetNameSpace();
		assertEquals(Set.of("boot-v"), vertexIds());
	}

	@BeforeEach
	void setUp() throws Exception {
		cleanup();
	}

	private void start(final MiloStrategy strategy) throws Exception {
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy(), strategy);
	}

	@AfterEach
	void tearDown() throws Exception {
		cleanup();
	}
}
