package net.rossonet.waldot.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.tinkerpop.gremlin.process.computer.GraphFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerOptimizedStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Closing the namespace must also close the Gremlin graph, and a reset must
 * stop a plugin before starting it again.
 */
class NamespaceLifecycleRegressionTest {

	private WaldotGraph graph;

	private final List<String> events = new ArrayList<>();

	@BeforeEach
	void setUp() throws Exception {
		cleanup();
	}

	@AfterEach
	void tearDown() throws Exception {
		cleanup();
	}

	@Test
	void closingTheNamespaceClosesTheGremlinGraph() throws Exception {
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy(),
				new MiloSingleServerOptimizedStrategy());
		final OpcGraph opcGraph = (OpcGraph) graph;
		assertFalse(opcGraph.isClosed());
		graph.getWaldotNamespace().close();
		assertTrue(opcGraph.isClosed());
		// a second close, from either side, must not fail
		graph.getWaldotNamespace().close();
		opcGraph.close();
		graph = null;
	}

	@Test
	void closingTheGraphClosesTheNamespaceAndStopsPlugins() throws Exception {
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy(),
				new MiloSingleServerOptimizedStrategy());
		final RecordingPlugin plugin = new RecordingPlugin(events);
		graph.getWaldotNamespace().registerPlugin(plugin);
		events.clear();
		((OpcGraph) graph).close();
		assertTrue(events.contains("stop"));
		graph = null;
	}

	@Test
	void resetStopsThePluginBeforeStartingItAgain() throws Exception {
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy(),
				new MiloSingleServerOptimizedStrategy());
		final RecordingPlugin plugin = new RecordingPlugin(events);
		final WaldotNamespace namespace = graph.getWaldotNamespace();
		namespace.registerPlugin(plugin);
		assertTrue(namespace.getPlugins().contains(plugin));
		assertThrows(UnsupportedOperationException.class, () -> namespace.getPlugins().clear());
		events.clear();
		namespace.resetNameSpace();
		assertEquals(List.of("stop", "reset", "start"), events);
		namespace.unregisterPlugin(plugin);
		assertFalse(namespace.getPlugins().contains(plugin));
		assertEquals(List.of("stop", "reset", "start", "stop"), events);
	}

	@Test
	void registerCommandRejectsANonOpcCommandBeforeRegistering() throws Exception {
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy(),
				new MiloSingleServerOptimizedStrategy());
		final WaldotNamespace namespace = graph.getWaldotNamespace();
		final int commandsBefore = namespace.listConfiguredCommands().size();
		final WaldotCommand foreign = (WaldotCommand) Proxy.newProxyInstance(WaldotCommand.class.getClassLoader(),
				new Class<?>[] { WaldotCommand.class }, (proxy, method, args) -> switch (method.getName()) {
				case "hashCode" -> System.identityHashCode(proxy);
				case "equals" -> proxy == args[0];
				case "toString" -> "foreign-command";
				default -> null;
				});
		assertThrows(IllegalArgumentException.class, () -> namespace.registerCommand(foreign));
		assertThrows(IllegalArgumentException.class, () -> namespace.registerCommand(null));
		assertEquals(commandsBefore, namespace.listConfiguredCommands().size());
	}

	@Test
	void graphComputerIsNotSupported() throws Exception {
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy(),
				new MiloSingleServerOptimizedStrategy());
		final WaldotNamespace namespace = graph.getWaldotNamespace();
		assertThrows(UnsupportedOperationException.class,
				() -> namespace.createGraphComputerView(graph, new GraphFilter(), Set.of()));
		assertFalse(namespace.inComputerMode());
		assertNull(namespace.getGraphComputerView());
	}

	private void cleanup() throws Exception {
		if (graph != null) {
			graph.getWaldotNamespace().close();
			graph = null;
		}
		WaldotTestEnvironment.awaitOpcPortReleased();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
	}

	private static final class RecordingPlugin implements PluginListener {

		private final List<String> events;

		private RecordingPlugin(final List<String> events) {
			this.events = events;
		}

		@Override
		public void initialize(final WaldotNamespace waldotNamespace) {
			events.add("initialize");
		}

		@Override
		public void start() {
			events.add("start");
		}

		@Override
		public void stop() {
			events.add("stop");
		}

		@Override
		public void reset() {
			events.add("reset");
		}
	}
}
