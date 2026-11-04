package net.rossonet.waldot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.rossonet.waldot.api.exchange.DataExchangeProtocol;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * directory content (dry-run of delete directory) and the count returned by
 * delete directory.
 */
@TestInstance(Lifecycle.PER_CLASS)
class DirectoryContentCommandTest {

	private static final ObjectMapper JSON = new ObjectMapper();

	private WaldotGraph graph;

	private Object[] call(final String browseName, final String... inputs) {
		return command(browseName).runCommand(null, inputs);
	}

	private WaldotCommand command(final String browseName) {
		return graph.getWaldotNamespace().getConsoleStrategy().getCommands().stream()
				.filter(c -> c.getBrowseName() != null && browseName.equals(c.getBrowseName().getName())).findFirst()
				.orElseThrow(() -> new AssertionError("command " + browseName + " not registered"));
	}

	private JsonNode content(final String directory) throws Exception {
		final Object[] out = call(DataExchangeProtocol.DIRECTORY_CONTENT_COMMAND, directory);
		assertEquals("", out[1], String.valueOf(out[1]));
		return JSON.readTree((String) out[0]);
	}

	@Test
	void dryRunListsWhatTheDeletionRemoves() throws Exception {
		final long before = graph.traversal().V().count().next();
		final JsonNode content = content("v:plant/area-1");
		assertEquals(before, graph.traversal().V().count().next(), "nothing removed by the dry-run");
		final Set<String> vertices = new HashSet<>();
		content.get("vertices").forEach(v -> vertices.add(v.get("id").asText()));
		assertEquals(Set.of("pump-1", "valve-1"), vertices);
		assertEquals(2, content.get("edges").size(), content.toString());
		content.get("edges").forEach(e -> assertTrue(e.get("with_vertex").asBoolean(), e.toString()));
		assertEquals(4, content.get("count").asInt());
		final Object[] deleted = call(graph.getWaldotNamespace().getConfiguration().getDeleteDirectoryLabel(),
				"v:plant/area-1");
		assertEquals("", deleted[1], String.valueOf(deleted[1]));
		assertEquals("4", deleted[2], "removed count equals the dry-run count");
		assertEquals(before - 2, graph.traversal().V().count().next());
	}

	@Test
	void parentFolderIncludesTheSubFolders() throws Exception {
		final JsonNode content = content("ns=2;s=v:plant");
		assertEquals(3, content.get("vertices").size(), content.toString());
	}

	@Test
	void singleVertex() throws Exception {
		final JsonNode content = content("other-1");
		assertEquals(1, content.get("vertices").size(), content.toString());
		assertEquals("other-1", content.get("vertices").get(0).get("id").asText());
	}

	@Test
	void unknownFolderIsEmpty() throws Exception {
		final JsonNode content = content("v:does-not-exist");
		assertEquals(0, content.get("count").asInt());
	}

	@BeforeEach
	void plant() {
		graph.getWaldotNamespace().resetNameSpace();
		final Vertex pump = graph.addVertex(T.id, "pump-1", "directory", "plant/area-1");
		final Vertex valve = graph.addVertex(T.id, "valve-1", "directory", "plant/area-1");
		final Vertex tank = graph.addVertex(T.id, "tank-1", "directory", "plant/area-2");
		graph.addVertex(T.id, "other-1");
		pump.addEdge("feeds", valve);
		valve.addEdge("feeds", tank);
	}

	@BeforeAll
	void startServer() throws Exception {
		WaldotTestEnvironment.awaitOpcPortReleased();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy());
	}

	@AfterAll
	void stopServer() {
		WaldotTestEnvironment.cleanup(graph, null);
	}
}
