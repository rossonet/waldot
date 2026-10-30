package net.rossonet.waldot.regression;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerOptimizedStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.waldot.opc.MiloSingleServerBaseReferenceNodeBuilder;
import net.rossonet.waldot.opc.MiloSingleServerBaseReferenceNodeBuilder.ReferenceNodes;
import net.rossonet.waldot.plugin.AbstractPluginListener;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * {@code namespace.close()} must release every resource reachable from the
 * namespace: the {@link AutoCloseable} vertices are closed, the plugins are
 * stopped and then closed, a failure does not stop the shutdown, a second
 * close does nothing and a failed startup does not leave the namespace
 * registered.
 */
class NamespaceCloseRegressionTest {

	/** Vertex that records its closes; fails on close when asked. */
	public static final class ClosableTestVertex extends AbstractOpcVertex
			implements
				AutoCloseable {

		public static final String FAIL_FIELD = "fail-on-close";

		public static void generateParameters(final WaldotNamespace namespace,
				final UaObjectTypeNode typeNode) {
			// no parameter
		}

		private final AtomicInteger closes = new AtomicInteger();

		private final boolean failOnClose;

		private List<String> events;

		public ClosableTestVertex(final WaldotGraph graph,
				final UaNodeContext context, final NodeId nodeId,
				final QualifiedName browseName, final LocalizedText displayName,
				final LocalizedText description, final UInteger writeMask,
				final UInteger userWriteMask, final UByte eventNotifier,
				final long version, final Object[] propertyKeyValues) {
			super(graph, context, nodeId, browseName, displayName, description,
					writeMask, userWriteMask, eventNotifier, version);
			failOnClose = "true".equals(MiloStrategy
					.getKeyValuesProperty(propertyKeyValues, FAIL_FIELD));
		}

		@Override
		public Object clone() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() {
			closes.incrementAndGet();
			if (events != null) {
				events.add("vertex-close:" + label());
			}
			if (failOnClose) {
				throw new IllegalStateException("close failure of " + label());
			}
		}

		int closes() {
			return closes.get();
		}
	}

	/** Plugin that manages {@link ClosableTestVertex} and records stop/close. */
	private static final class ClosingPlugin extends AbstractPluginListener {

		static final String TYPE = "test:closable";

		private final List<String> events;

		private final boolean failing;

		ClosingPlugin(final List<String> events, final boolean failing) {
			this.events = events;
			this.failing = failing;
		}

		@Override
		public void close() throws Exception {
			events.add(name() + "-close");
			if (failing) {
				throw new IllegalStateException("plugin close failure");
			}
			super.close();
		}

		@Override
		public void initialize(final WaldotNamespace waldotNamespace) {
			super.initialize(waldotNamespace);
			if (!failing) {
				registerTypeNode(ClosableTestVertex.class,
						"TestClosableObjectType", "Test closable", TYPE);
			}
		}

		private String name() {
			return failing ? "failing" : "plugin";
		}

		@Override
		public void stop() {
			events.add(name() + "-stop");
			if (failing) {
				throw new IllegalStateException("plugin stop failure");
			}
		}
	}

	private final List<String> events = new CopyOnWriteArrayList<>();

	private WaldotGraph graph;

	private ClosableTestVertex addClosable(final String label,
			final boolean failOnClose) {
		final ClosableTestVertex vertex = assertInstanceOf(
				ClosableTestVertex.class,
				graph.addVertex(MiloStrategy.TYPE_FIELD.toLowerCase(),
						ClosingPlugin.TYPE, "label",
						label, ClosableTestVertex.FAIL_FIELD,
						String.valueOf(failOnClose)));
		vertex.events = events;
		return vertex;
	}

	private void cleanup() throws Exception {
		if (graph != null) {
			graph.getWaldotNamespace().close();
			graph = null;
		}
		WaldotTestEnvironment.awaitOpcPortReleased();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
	}

	@Test
	void closeClosesVerticesThenStopsAndClosesPluginsEvenWithFailures()
			throws Exception {
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(),
				new LoggerHistoryStrategy(),
				new MiloSingleServerOptimizedStrategy());
		final WaldotNamespace namespace = graph.getWaldotNamespace();
		final ClosingPlugin failing = new ClosingPlugin(events, true);
		namespace.registerPlugin(failing);
		final ClosingPlugin plugin = new ClosingPlugin(events, false);
		namespace.registerPlugin(plugin);
		final ClosableTestVertex first = addClosable("first", false);
		final ClosableTestVertex broken = addClosable("broken", true);
		final ClosableTestVertex last = addClosable("last", false);
		final ReferenceNodes nodes = MiloSingleServerBaseReferenceNodeBuilder
				.registeredFor(namespace);
		assertTrue(nodes != null, "reference nodes registered");
		events.clear();

		assertDoesNotThrow(namespace::close);

		// every AutoCloseable vertex closed once, also after a failing one
		assertEquals(1, first.closes());
		assertEquals(1, broken.closes());
		assertEquals(1, last.closes());
		// both plugins stopped and closed, also the one that throws
		assertTrue(events.containsAll(List.of("plugin-stop", "plugin-close",
				"failing-stop", "failing-close")), events.toString());
		assertTrue(events.indexOf("plugin-stop") < events
				.indexOf("plugin-close"), events.toString());
		// vertices are closed while the plugins are still alive
		for (final String vertexEvent : List.of("vertex-close:first",
				"vertex-close:broken", "vertex-close:last")) {
			assertTrue(events.indexOf(vertexEvent) >= 0, events.toString());
			assertTrue(events.indexOf(vertexEvent) < events.indexOf(
					"plugin-stop"), events.toString());
			assertTrue(events.indexOf(vertexEvent) < events.indexOf(
					"failing-stop"), events.toString());
		}
		// the rest of the shutdown ran: graph closed, port released, the
		// static registry no longer references the namespace
		assertTrue(((OpcGraph) graph).isClosed());
		assertTrue(WaldotTestEnvironment.awaitOpcPortReleased());
		assertNull(MiloSingleServerBaseReferenceNodeBuilder
				.registeredFor(namespace));
		assertNotSame(nodes.edgeTypeNode,
				MiloSingleServerBaseReferenceNodeBuilder.edgeTypeNode);
		assertNotSame(nodes, MiloSingleServerBaseReferenceNodeBuilder
				.forNamespace(null));

		// a second close does nothing and does not throw
		final int eventsAfterClose = events.size();
		assertDoesNotThrow(namespace::close);
		assertDoesNotThrow(() -> ((OpcGraph) graph).close());
		assertEquals(eventsAfterClose, events.size());
		assertEquals(1, first.closes());
		graph = null;
	}

	@Test
	void failedStartupClosesTheNamespace() throws Exception {
		final ReferenceNodes before = MiloSingleServerBaseReferenceNodeBuilder
				.forNamespace(null);
		try (ServerSocket busy = new ServerSocket(
				WaldotTestEnvironment.opcTcpPort())) {
			final ExecutionException failure = assertThrows(
					ExecutionException.class,
					() -> OpcFactory.getOpcGraph(
							WaldotTestEnvironment.bootUrl(),
							new LoggerHistoryStrategy(),
							new MiloSingleServerOptimizedStrategy()));
			assertTrue(failure.getSuppressed().length == 0,
					"closing after the failed start must not throw");
		}
		// the failed namespace has been closed: it is no longer the last
		// registered one and the legacy statics do not point to it
		assertSame(before,
				MiloSingleServerBaseReferenceNodeBuilder.forNamespace(null));
		assertTrue(WaldotTestEnvironment.awaitOpcPortReleased());
		// the port is free again: a new namespace starts normally
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(),
				new LoggerHistoryStrategy(),
				new MiloSingleServerOptimizedStrategy());
		assertTrue(!((OpcGraph) graph).isClosed());
	}

	@Test
	void removingAVertexClosesItOnce() throws Exception {
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(),
				new LoggerHistoryStrategy(),
				new MiloSingleServerOptimizedStrategy());
		final WaldotNamespace namespace = graph.getWaldotNamespace();
		namespace.registerPlugin(new ClosingPlugin(events, false));
		final ClosableTestVertex removed = addClosable("removed", false);
		final ClosableTestVertex kept = addClosable("kept", false);
		removed.remove();
		assertEquals(1, removed.closes());
		assertEquals(0, kept.closes());
		namespace.close();
		// the removed vertex is not in the graph any more: not closed twice
		assertEquals(1, removed.closes());
		assertEquals(1, kept.closes());
		graph = null;
	}

	@BeforeEach
	void setUp() throws Exception {
		cleanup();
	}

	@AfterEach
	void tearDown() throws Exception {
		cleanup();
	}

	@Test
	void pluginWithoutCloseIsOnlyStopped() throws Exception {
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(),
				new LoggerHistoryStrategy(),
				new MiloSingleServerOptimizedStrategy());
		final AtomicInteger stops = new AtomicInteger();
		graph.getWaldotNamespace().registerPlugin(new PluginListener() {
			@Override
			public void initialize(final WaldotNamespace waldotNamespace) {
				// nothing
			}

			@Override
			public void stop() {
				stops.incrementAndGet();
			}
		});
		graph.getWaldotNamespace().close();
		graph.getWaldotNamespace().close();
		assertEquals(1, stops.get());
		graph = null;
	}
}
