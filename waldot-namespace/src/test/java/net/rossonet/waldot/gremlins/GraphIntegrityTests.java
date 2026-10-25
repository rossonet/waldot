package net.rossonet.waldot.gremlins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/** Verifies duplicate ids, self loops and property removal on a live graph. */
public class GraphIntegrityTests {
	private WaldotGraph g;

	@BeforeEach
	public void before() throws Exception {
		cleanup();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		g = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy());
	}

	@AfterEach
	public void after() throws Exception {
		cleanup();
	}

	private void cleanup() throws Exception {
		if (g != null) {
			g.getWaldotNamespace().close();
			g = null;
		}
		WaldotTestEnvironment.awaitOpcPortReleased();
		Files.deleteIfExists(Path.of("/tmp/index.txt"));
	}

	@Test
	public void duplicateVertexIdIsRejected() {
		g.addVertex("id", 9001, "name", "a");
		assertThrows(Graph.Exceptions.vertexWithIdAlreadyExists(9001).getClass(),
				() -> g.addVertex("id", 9001, "name", "b"));
	}

	@Test
	public void selfLoopBothReturnsTwice() {
		final Vertex v = g.addVertex("name", "loop");
		v.addEdge("self", v);
		final List<Edge> both = new java.util.ArrayList<>();
		v.edges(Direction.BOTH).forEachRemaining(both::add);
		assertEquals(2, both.size());
	}

	@Test
	public void dropPropertyRemovesIt() {
		final Vertex v = g.addVertex("name", "p");
		v.property("color", "red");
		v.property("color").remove();
		assertEquals(false, v.property("color").isPresent());
	}
}
