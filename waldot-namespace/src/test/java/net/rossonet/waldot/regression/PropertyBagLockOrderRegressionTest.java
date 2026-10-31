package net.rossonet.waldot.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerOptimizedStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Creating a property must not wait for the monitor of the element while
 * holding the property bag: a thread that holds the element monitor (Milo
 * synchronizes the attribute access on it, plugins did the same) and reads
 * the properties would deadlock with it (seen in waldot-plugin-snmp).
 */
class PropertyBagLockOrderRegressionTest {

	private static final int PROPERTIES = 300;

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
	void creatingEdgePropertiesWhileTheEdgeMonitorIsHeld() throws Exception {
		final Vertex a = graph.addVertex("id", "lock-a", "label", "lock-a");
		final Vertex b = graph.addVertex("id", "lock-b", "label", "lock-b");
		final Element edge = a.addEdge("plain", b);
		createWhileReadingUnderMonitor(edge);
	}

	@Test
	void creatingVertexPropertiesWhileTheVertexMonitorIsHeld() throws Exception {
		final Vertex vertex = graph.addVertex("id", "lock-v", "label", "lock-v");
		createWhileReadingUnderMonitor(vertex);
	}

	private void createWhileReadingUnderMonitor(final Element element) throws Exception {
		final ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			final CountDownLatch start = new CountDownLatch(1);
			final Future<?> writer = pool.submit(() -> {
				start.await();
				for (int i = 0; i < PROPERTIES; i++) {
					element.property("k" + i, i);
				}
				return null;
			});
			final Future<?> reader = pool.submit(() -> {
				start.await();
				while (!writer.isDone()) {
					synchronized (element) {
						element.property("k0");
						element.keys();
					}
					// release the monitor: without a lock cycle the writer
					// must progress
					Thread.sleep(1);
				}
				return null;
			});
			start.countDown();
			try {
				writer.get(60, TimeUnit.SECONDS);
				reader.get(10, TimeUnit.SECONDS);
			} catch (final TimeoutException e) {
				fail("lock cycle between the property bag and the element monitor");
			}
		} finally {
			pool.shutdownNow();
			pool.awaitTermination(10, TimeUnit.SECONDS);
		}
		assertEquals(PROPERTIES - 1, element.<Integer>property("k" + (PROPERTIES - 1)).value());
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
}
