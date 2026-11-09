package net.rossonet.waldot.gremlins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

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

	private static int count(final java.util.Iterator<?> iterator) {
		int n = 0;
		while (iterator.hasNext()) {
			iterator.next();
			n++;
		}
		return n;
	}

	/**
	 * An edge from a vertex object already removed must be refused: it would
	 * stay only in the in-edges of the target, never notified as removed
	 * (trunk VLAN left applied after a configuration switch).
	 */
	@Test
	public void edgeFromRemovedVertexIsRefused() {
		final Vertex source = g.addVertex("id", 9101, "name", "source");
		final Vertex target = g.addVertex("id", 9102, "name", "target");
		source.remove();
		assertThrows(RuntimeException.class, () -> source.addEdge("e", target));
		assertEquals(0, count(target.edges(Direction.IN)));
	}

	/**
	 * Edges added by another thread while the source is removed: none may
	 * survive in the in-edges of the target.
	 */
	@Test
	public void edgesAddedDuringVertexRemovalDoNotSurvive() throws Exception {
		final Vertex target = g.addVertex("id", 9200, "name", "target");
		for (int i = 0; i < 30; i++) {
			final Vertex source = g.addVertex("id", 9300 + i, "name", "source" + i);
			final CountDownLatch start = new CountDownLatch(1);
			final AtomicInteger added = new AtomicInteger();
			final Thread adder = new Thread(() -> {
				try {
					start.await();
					for (int k = 0; k < 50; k++) {
						source.addEdge("e", target);
						added.incrementAndGet();
					}
				} catch (final RuntimeException | InterruptedException e) {
					// refused once the removal started: expected
				}
			});
			adder.start();
			start.countDown();
			Thread.sleep(i % 5);
			source.remove();
			adder.join(30_000);
			assertEquals(0, count(target.edges(Direction.IN)),
					"round " + i + ", edges added " + added.get());
		}
	}

	@Test
	public void dropPropertyRemovesIt() {
		final Vertex v = g.addVertex("name", "p");
		v.property("color", "red");
		v.property("color").remove();
		assertEquals(false, v.property("color").isPresent());
	}
}
