package net.rossonet.waldot.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.commands.DeleteDirectory;
import net.rossonet.waldot.commands.QueryCommand;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerOptimizedStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.AbstractOpcGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcHelper;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Regression (step 1.3): console commands failed with
 * ArrayIndexOutOfBounds/NullPointerException on missing arguments and returned
 * a null error message; OpcHelper.search compared the requested type by simple
 * name and failed on properties without value.
 */
class ConsoleCommandsRobustnessRegressionTest {

	private WaldotGraph graph;

	private void cleanup() throws Exception {
		if (graph != null) {
			graph.getWaldotNamespace().close();
			graph = null;
		}
		WaldotTestEnvironment.awaitOpcPortReleased();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
	}

	@Test
	void deleteDirectoryWithoutArgumentsReportsAnError() {
		final DeleteDirectory command = new DeleteDirectory(graph.getWaldotNamespace());
		for (final String[] input : new String[][] { null, {}, { null }, { " " } }) {
			final String[] result = command.runCommand(null, input);
			assertEquals("", result[0]);
			assertNotNull(result[1]);
			assertTrue(result[1].contains("directory"), result[1]);
		}
	}

	@Test
	void queryCommandErrorMessageIsNeverNull() {
		final QueryCommand command = new QueryCommand(graph.getWaldotNamespace());
		final String[] result = command.runCommand(null, new String[] { "this is not ( a valid expression" });
		assertEquals("", result[0]);
		assertNotNull(result[1]);
		assertTrue(!result[1].isBlank());
	}

	@Test
	void queryCommandWithoutArgumentsReportsAnError() {
		final QueryCommand command = new QueryCommand(graph.getWaldotNamespace());
		for (final String[] input : new String[][] { null, {}, { null }, { "" } }) {
			final String[] result = command.runCommand(null, input);
			assertEquals("", result[0]);
			assertNotNull(result[1]);
			assertTrue(result[1].contains("command"), result[1]);
		}
	}

	@Test
	void searchFiltersByElementClass() {
		final Vertex a = graph.addVertex("id", "search-a", "label", "search-a", "tag", "needle-vertex");
		final Vertex b = graph.addVertex("id", "search-b", "label", "search-b");
		a.addEdge("to", b, "tag", "needle-edge");

		final List<Object> vertexValues = values(
				OpcHelper.search((AbstractOpcGraph) graph, "needle-.*", Optional.of(Vertex.class)));
		assertTrue(vertexValues.contains("needle-vertex"), vertexValues.toString());
		assertTrue(!vertexValues.contains("needle-edge"), vertexValues.toString());

		final List<Object> edgeValues = values(
				OpcHelper.search((AbstractOpcGraph) graph, "needle-.*", Optional.of(Edge.class)));
		assertTrue(edgeValues.contains("needle-edge"), edgeValues.toString());
		assertTrue(!edgeValues.contains("needle-vertex"), edgeValues.toString());
	}

	@Test
	void searchWithInvalidRegexFailsFast() {
		assertThrows(IllegalArgumentException.class,
				() -> OpcHelper.search((AbstractOpcGraph) graph, "(", Optional.empty()));
	}

	@BeforeEach
	void setUp() throws Exception {
		cleanup();
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy(),
				new MiloSingleServerOptimizedStrategy());
	}

	@AfterEach
	void tearDown() throws Exception {
		cleanup();
	}

	private List<Object> values(final Iterator<Property> it) {
		final List<Object> out = new ArrayList<>();
		it.forEachRemaining(p -> out.add(p.value()));
		return out;
	}
}
