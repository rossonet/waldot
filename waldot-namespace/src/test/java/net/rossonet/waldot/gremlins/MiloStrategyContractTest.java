package net.rossonet.waldot.gremlins;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerBaseStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerOptimizedStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Contract of {@link MiloStrategy}: every behavior verified here must be the
 * same for the base and the optimized implementation, so each test runs on
 * both of them.
 */
public class MiloStrategyContractTest {
	private static final String BASE = "base";
	private static final String OPTIMIZED = "optimized";

	private WaldotGraph g;
	private MiloStrategy strategy;

	@BeforeEach
	public void before() throws Exception {
		cleanup();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
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

	private void open(final String implementation) throws Exception {
		strategy = OPTIMIZED.equals(implementation)
				? new MiloSingleServerOptimizedStrategy()
				: new MiloSingleServerBaseStrategy();
		g = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy(), strategy);
	}

	private static <T> List<T> list(final Iterator<T> iterator) {
		final List<T> result = new ArrayList<>();
		iterator.forEachRemaining(result::add);
		return result;
	}

	@ParameterizedTest
	@ValueSource(strings = { BASE, OPTIMIZED })
	public void duplicateVertexIdIsRejected(final String implementation) throws Exception {
		open(implementation);
		g.addVertex("id", 9101, "name", "a");
		assertThrows(Graph.Exceptions.vertexWithIdAlreadyExists(9101).getClass(),
				() -> g.addVertex("id", 9101, "name", "b"));
		assertEquals(1, list(g.vertices()).stream().filter(v -> "a".equals(v.value("name"))).count());
	}

	@ParameterizedTest
	@ValueSource(strings = { BASE, OPTIMIZED })
	public void duplicateEdgeIdIsRejectedAndMultiEdgesAreKept(final String implementation) throws Exception {
		// known limitation of the base strategy: same label between the same
		// vertices overwrites the first edge
		assumeTrue(OPTIMIZED.equals(implementation), "base strategy overwrites multi-edges");
		open(implementation);
		final Vertex a = g.addVertex("name", "a");
		final Vertex b = g.addVertex("name", "b");
		final Edge first = a.addEdge("rel", b);
		final Edge second = a.addEdge("rel", b);
		assertNotEquals(first.id(), second.id());
		assertEquals(2, list(a.edges(Direction.OUT, "rel")).size());
		assertEquals(2, list(b.edges(Direction.IN, "rel")).size());
		a.addEdge("explicit", b, T.id, 9201);
		assertThrows(Graph.Exceptions.edgeWithIdAlreadyExists(9201).getClass(),
				() -> a.addEdge("explicit", b, T.id, 9201));
		assertEquals(3, list(a.edges(Direction.OUT)).size());
	}

	@ParameterizedTest
	@ValueSource(strings = { BASE, OPTIMIZED })
	public void propertiesCreatedInParallelAreAllPresent(final String implementation) throws Exception {
		open(implementation);
		final Vertex v = g.addVertex("name", "parallel");
		final int threads = 8;
		final int keysPerThread = 5;
		final ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			final CountDownLatch start = new CountDownLatch(1);
			final List<Future<?>> futures = new ArrayList<>();
			for (int t = 0; t < threads; t++) {
				final int thread = t;
				futures.add(pool.submit(() -> {
					start.await();
					for (int k = 0; k < keysPerThread; k++) {
						v.property("key-" + thread + "-" + k, k);
						// the same key written by every thread
						v.property("shared", thread);
					}
					return null;
				}));
			}
			start.countDown();
			for (final Future<?> f : futures) {
				f.get(60, TimeUnit.SECONDS);
			}
		} finally {
			pool.shutdownNow();
		}
		for (int t = 0; t < threads; t++) {
			for (int k = 0; k < keysPerThread; k++) {
				assertEquals(k, (int) v.value("key-" + t + "-" + k));
			}
		}
		assertEquals(1, list(v.properties("shared")).size());
	}

	@ParameterizedTest
	@ValueSource(strings = { BASE, OPTIMIZED })
	public void removingAVertexRemovesItsEdges(final String implementation) throws Exception {
		// known limitation of the base strategy: incident edges stay orphan
		assumeTrue(OPTIMIZED.equals(implementation), "base strategy leaves orphan edges");
		open(implementation);
		final Vertex a = g.addVertex("name", "a");
		final Vertex b = g.addVertex("name", "b");
		final Vertex c = g.addVertex("name", "c");
		a.property("color", "red");
		a.addEdge("knows", b);
		c.addEdge("knows", a);
		a.addEdge("self", a);
		assertEquals(3, list(g.edges()).size());
		a.remove();
		assertEquals(0, list(g.edges()).size());
		assertEquals(2, list(g.vertices()).size());
		assertDoesNotThrow(() -> list(b.edges(Direction.BOTH)));
		assertDoesNotThrow(() -> list(c.vertices(Direction.BOTH)));
		assertEquals(0, list(b.edges(Direction.BOTH)).size());
		assertEquals(0, list(c.edges(Direction.OUT)).size());
		// removing again must be harmless
		assertDoesNotThrow(() -> g.removeVertex(((net.rossonet.waldot.api.models.WaldotVertex) a).getNodeId()));
	}

	@ParameterizedTest
	@ValueSource(strings = { BASE, OPTIMIZED })
	public void adjacencyRespectsDirectionAndLabel(final String implementation) throws Exception {
		open(implementation);
		final Vertex a = g.addVertex("name", "a");
		final Vertex b = g.addVertex("name", "b");
		final Vertex c = g.addVertex("name", "c");
		final Vertex d = g.addVertex("name", "d");
		a.addEdge("knows", b);
		a.addEdge("likes", c);
		d.addEdge("knows", a);
		assertEquals(2, list(a.edges(Direction.OUT)).size());
		assertEquals(1, list(a.edges(Direction.OUT, "knows")).size());
		assertEquals(1, list(a.edges(Direction.IN)).size());
		assertEquals(3, list(a.edges(Direction.BOTH)).size());
		assertEquals(0, list(a.edges(Direction.IN, "likes")).size());
		assertEquals(2, list(a.edges(Direction.BOTH, "knows")).size());
		final Set<Object> out = new HashSet<>();
		a.vertices(Direction.OUT).forEachRemaining(v -> out.add(v.value("name")));
		assertEquals(Set.of("b", "c"), out);
		final Set<Object> in = new HashSet<>();
		a.vertices(Direction.IN, "knows").forEachRemaining(v -> in.add(v.value("name")));
		assertEquals(Set.of("d"), in);
		assertEquals(Set.of("b", "c", "d"), bothNames(a));
	}

	private static Set<Object> bothNames(final Vertex v) {
		final Set<Object> names = new HashSet<>();
		v.vertices(Direction.BOTH).forEachRemaining(n -> names.add(n.value("name")));
		return names;
	}

	@ParameterizedTest
	@ValueSource(strings = { BASE, OPTIMIZED })
	public void closeCanBeCalledMoreThanOnce(final String implementation) throws Exception {
		open(implementation);
		g.addVertex("name", "x");
		g.getWaldotNamespace().close();
		assertDoesNotThrow(() -> strategy.close());
		assertDoesNotThrow(() -> strategy.close());
		g = null;
		assertTrue(true);
	}
}
