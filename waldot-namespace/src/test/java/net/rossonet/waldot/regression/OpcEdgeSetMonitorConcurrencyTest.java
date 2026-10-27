package net.rossonet.waldot.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.MonitoredEdge;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerOptimizedStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Step 1.4: concurrent {@code setMonitor} calls on the same edge must leave
 * exactly one active monitor (every replaced monitor is removed).
 */
class OpcEdgeSetMonitorConcurrencyTest {

	private static final class CountingMonitor extends MonitoredEdge {
		private final AtomicInteger active;

		CountingMonitor(final WaldotGraph graph, final WaldotEdge edge, final AtomicInteger active) {
			super(graph.getWaldotNamespace(), edge, (WaldotVertex) edge.outVertex(), (WaldotVertex) edge.inVertex());
			this.active = active;
		}

		@Override
		protected void createObserverNeeded() {
			final int now = active.incrementAndGet();
			if (now > 1) {
				MAX_OVERLAP.accumulateAndGet(now, Math::max);
			}
		}

		@Override
		public void fireEvent(final UaNode node, final BaseEventType event) {
			// not used
		}

		@Override
		protected Object getLastValue(final String propertyLabel) {
			return null;
		}

		@Override
		public void propertyChanged(final UaNode sourceNode, final String propertyLabel, final DataValue value) {
			// not used
		}

		@Override
		public void remove() {
			active.decrementAndGet();
		}
	}

	private static final AtomicInteger MAX_OVERLAP = new AtomicInteger();

	private WaldotGraph graph;

	private void cleanup() throws Exception {
		if (graph != null) {
			graph.getWaldotNamespace().close();
			graph = null;
		}
		WaldotTestEnvironment.awaitOpcPortReleased();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
	}

	@BeforeEach
	void setUp() throws Exception {
		cleanup();
		MAX_OVERLAP.set(0);
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy(),
				new MiloSingleServerOptimizedStrategy());
	}

	@Test
	void concurrentSetMonitorKeepsOnlyOneActiveMonitor() throws Exception {
		final Vertex a = graph.addVertex("id", "mon-a", "label", "mon-a");
		final Vertex b = graph.addVertex("id", "mon-b", "label", "mon-b");
		final WaldotEdge edge = (WaldotEdge) a.addEdge("plain", b);
		final AtomicInteger active = new AtomicInteger();
		final int threads = 8;
		final int iterations = 200;
		final ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			final CountDownLatch start = new CountDownLatch(1);
			final List<Future<?>> futures = new ArrayList<>();
			for (int t = 0; t < threads; t++) {
				futures.add(pool.submit(() -> {
					start.await();
					for (int i = 0; i < iterations; i++) {
						edge.setMonitor(new CountingMonitor(graph, edge, active));
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
			pool.awaitTermination(10, TimeUnit.SECONDS);
		}
		assertEquals(1, active.get(), "exactly one monitor must stay active");
		assertEquals(0, MAX_OVERLAP.get(), "two monitors were active at the same time");
	}

	@AfterEach
	void tearDown() throws Exception {
		cleanup();
	}
}
