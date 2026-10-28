package net.rossonet.waldot.regression;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.B_O_Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.util.TraverserSet;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONVersion;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadResult;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadValueId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import net.rossonet.waldot.api.NamespaceListener;
import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotProperty;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.configuration.DefaultHomunculusConfiguration;
import net.rossonet.waldot.gremlin.opcgraph.services.GremlinCommandSettings;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcCommandListServiceFactory;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcCommandServiceFactory;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcDegreeCentralityFactory;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcTextSearchFactory;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.AbstractOpcGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcHelper;
import net.rossonet.waldot.namespaces.HomunculusNamespace;
import net.rossonet.waldot.opc.AbstractOpcCommand;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Delegations of HomunculusNamespace and Gremlin features (services, search,
 * GraphSON, step strategy, per-vertex commands) on a real OPC UA server
 * holding the TinkerPop "classic" toy graph. One server for each concrete
 * subclass, which selects the Milo strategy.
 */
@TestInstance(Lifecycle.PER_CLASS)
abstract class NamespaceDelegationRegressionTest {

	/** The Milo strategy under test. */
	protected abstract MiloStrategy createMiloStrategy();

	private static final class RecordingListener implements NamespaceListener {
		private final List<String> events = new CopyOnWriteArrayList<>();

		@Override
		public void onCommandRegistered(final WaldotCommand command) {
			events.add("registered:" + command.getConsoleCommand());
		}

		@Override
		public void onCommandRemoved(final WaldotCommand command) {
			events.add("removed:" + command.getConsoleCommand());
		}

		@Override
		public void onDataItemsCreated(final java.util.List<org.eclipse.milo.opcua.sdk.server.items.DataItem> items) {
			events.add("created");
		}

		@Override
		public void onDataItemsDeleted(final java.util.List<org.eclipse.milo.opcua.sdk.server.items.DataItem> items) {
			events.add("deleted");
		}

		@Override
		public void onDataItemsModified(final java.util.List<org.eclipse.milo.opcua.sdk.server.items.DataItem> items) {
			events.add("modified");
		}

		@Override
		public void onMonitoringModeChanged(
				final java.util.List<org.eclipse.milo.opcua.sdk.server.items.MonitoredItem> items) {
			events.add("monitoring");
		}

		@Override
		public void onUpdateNode(final UaNode sourceNode) {
			events.add("update:" + sourceNode.getNodeId().getIdentifier());
		}
	}

	private static final class TestCommand extends AbstractOpcCommand {
		private final Object[] outputs;

		TestCommand(final WaldotNamespace namespace, final String name, final String directory) {
			this(namespace, name, directory, true, new Object[0]);
		}

		TestCommand(final WaldotNamespace namespace, final String name, final String directory,
				final boolean executable, final Object[] outputs) {
			super(namespace.getGremlinGraph(), namespace, name, name, "test command " + name, directory,
					DefaultHomunculusConfiguration.DEFAULT_WRITE_MASK, DefaultHomunculusConfiguration.DEFAULT_WRITE_MASK,
					executable, executable);
			this.outputs = outputs;
		}

		@Override
		public Object[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
			return outputs;
		}
	}

	private WaldotGraph graph;
	private HomunculusNamespace namespace;

	@BeforeAll
	void startServer() throws Exception {
		WaldotTestEnvironment.awaitOpcPortReleased();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy(),
				createMiloStrategy());
		namespace = (HomunculusNamespace) graph.getWaldotNamespace();
		OpcFactory.generateClassic(graph);
	}

	@AfterAll
	void stopServer() throws Exception {
		WaldotTestEnvironment.cleanup(graph, null);
	}

	private Vertex vertexNamed(final String name) {
		return graph.traversal().V().has("name", name).next();
	}

	@Test
	void classicGraphIsLoaded() {
		final GraphTraversalSource g = graph.traversal();
		assertTrue(g.V().count().next() >= 6);
		assertEquals(2L, g.V().has("name", "marko").out("knows").count().next());
		assertEquals(3L, g.V().has("name", "lop").in("created").count().next());
		assertEquals(List.of("josh", "vadas"),
				g.V().has("name", "marko").out("knows").values("name").order().toList());
		assertTrue(namespace.getVerticesCount() >= 6);
		assertTrue(namespace.getEdgesCount() >= 6);
		assertTrue(namespace.toString().startsWith("Namespace [VerticesCount="), namespace.toString());
	}

	@Test
	void stepStrategyIsShownInExplain() {
		final String explain = graph.traversal().V().has("name", "marko").explain().prettyPrint();
		assertTrue(explain.contains("OpcGraphStep"), explain);
		final String edges = graph.traversal().E().hasLabel("knows").explain().prettyPrint();
		assertTrue(edges.contains("OpcGraphStep"), edges);
		assertEquals(2L, graph.traversal().E().hasLabel("knows").count().next());
	}

	@Test
	void textSearchFindsPropertyValues() {
		final List<Property> found = new ArrayList<>();
		OpcHelper.search((AbstractOpcGraph) graph, "mar.*", Optional.of(Vertex.class)).forEachRemaining(found::add);
		assertTrue(found.stream().anyMatch(p -> "marko".equals(p.value())), found.toString());
		final OpcTextSearchFactory<Object, Object> search = new OpcTextSearchFactory<>((AbstractOpcGraph) graph);
		final List<Object> results = new ArrayList<>();
		search.execute(null, Map.of(OpcTextSearchFactory.Params.SEARCH, "ripp")).forEachRemaining(results::add);
		assertFalse(results.isEmpty());
	}

	@Test
	void degreeCentralityOnARealVertex() {
		final OpcDegreeCentralityFactory degree = new OpcDegreeCentralityFactory((AbstractOpcGraph) graph);
		final Vertex marko = vertexNamed("marko");
		final List<Long> out = new ArrayList<>();
		degree.execute(null, new B_O_Traverser<>(marko, 1),
				Map.of(OpcDegreeCentralityFactory.Params.DIRECTION, Direction.OUT)).forEachRemaining(out::add);
		assertEquals(List.of(3L), out);
		final List<Long> in = new ArrayList<>();
		degree.execute(null, new B_O_Traverser<>(vertexNamed("lop"), 1), Map.of()).forEachRemaining(in::add);
		assertEquals(List.of(3L), in);
	}

	@Test
	void graphIsSerializedAsGraphSON() throws Exception {
		final Map<GraphSONVersion, org.apache.tinkerpop.gremlin.structure.io.IoRegistry> registries = Map.of(
				GraphSONVersion.V1_0, net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV1.instance(),
				GraphSONVersion.V2_0, net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV2.instance(),
				GraphSONVersion.V3_0, net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV3.instance());
		for (final Map.Entry<GraphSONVersion, org.apache.tinkerpop.gremlin.structure.io.IoRegistry> entry : registries
				.entrySet()) {
			final String json = GraphSONMapper.build().version(entry.getKey()).addRegistry(entry.getValue()).create()
					.createMapper().writeValueAsString(graph);
			assertTrue(json.contains("marko") && json.contains("knows"), entry.getKey() + ": " + json);
		}
	}

	@Test
	void perVertexCommandsUpdateAndDeleteTheVertex() {
		// generated (numeric) id: the commands must resolve it from the NodeId
		final Vertex target = graph.addVertex("name", "command-target");
		final String id = ((WaldotVertex) target).getNodeId().getIdentifier().toString();
		final Object[] updated = elementCommand(id, "property").runCommand(null, new String[] { "color", "red" });
		assertTrue(updated[0].toString().contains("updated"), java.util.Arrays.toString(updated));
		assertEquals("", updated[1]);
		assertEquals("red", graph.traversal().V(target.id()).values("color").next());
		final Object[] deleted = elementCommand(id, "delete").runCommand(null, new String[0]);
		assertTrue(deleted[0].toString().contains("deleted"), java.util.Arrays.toString(deleted));
		assertEquals("", deleted[1]);
		assertFalse(graph.traversal().V().has("name", "command-target").hasNext());
	}

	@Test
	void perEdgeCommandsUpdateAndDeleteTheEdge() {
		final Vertex a = graph.addVertex("name", "edge-command-a");
		final Vertex b = graph.addVertex("name", "edge-command-b");
		final Edge edge = a.addEdge("edgeCommand", b);
		final String id = ((WaldotEdge) edge).getNodeId().getIdentifier().toString();
		final Object[] updated = elementCommand(id, "property").runCommand(null, new String[] { "color", "blue" });
		assertEquals("", updated[1], java.util.Arrays.toString(updated));
		assertEquals("blue", graph.traversal().E(edge.id()).values("color").next());
		final Object[] deleted = elementCommand(id, "delete").runCommand(null, new String[0]);
		assertEquals("", deleted[1], java.util.Arrays.toString(deleted));
		assertFalse(graph.traversal().E().hasLabel("edgeCommand").hasNext());
		assertTrue(graph.traversal().V(a.id()).hasNext());
	}

	@Test
	void edgePropertiesPointBackToTheEdge() {
		final Vertex a = graph.addVertex("name", "property-owner-a");
		final Vertex b = graph.addVertex("name", "property-owner-b");
		final Edge edge = a.addEdge("weighted", b, "weight", 0.5d);
		@SuppressWarnings("unchecked")
		final WaldotProperty<Object> weight = (WaldotProperty<Object>) edge.property("weight");
		assertEquals(edge, weight.element());
		assertEquals(edge, namespace.getPropertyReference(weight));
	}

	@Test
	void graphWithDataIsWrittenAsGryo() throws Exception {
		final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		graph.io(org.apache.tinkerpop.gremlin.structure.io.gryo.GryoIo.build()).writer().create().writeGraph(out,
				graph);
		assertTrue(out.size() > 0);
	}

	private AbstractOpcCommand elementCommand(final String id, final String command) {
		return (AbstractOpcCommand) namespace.getStorageManager()
				.getNode(namespace.generateNodeId(id + "." + command)).orElseThrow();
	}

	@Test
	void graphVariablesAreNamespaceParameters() {
		final Graph.Variables variables = graph.variables();
		variables.set("owner", "plant-1");
		assertEquals("plant-1", namespace.namespaceParametersGet("owner"));
		assertTrue(namespace.namespaceParametersKeySet().contains("owner"));
		assertEquals(Optional.of("plant-1"), variables.get("owner"));
		assertNotNull(namespace.namespaceParametersToVariables());
		namespace.namespaceParametersRemove("owner");
		assertFalse(namespace.namespaceParametersKeySet().contains("owner"));
	}

	@Test
	void historyIsDelegatedToTheStrategy() {
		assertTrue(namespace.getHistoryStrategy() instanceof LoggerHistoryStrategy);
		final HistoryReadValueId id = new HistoryReadValueId(new NodeId(2, "x"), null, QualifiedName.NULL_VALUE,
				null);
		final List<HistoryReadResult> results = namespace.historyRead(null, null, TimestampsToReturn.Both,
				List.of(id));
		assertEquals(StatusCodes.Bad_HistoryOperationUnsupported, results.get(0).getStatusCode().getValue());
		assertTrue(namespace.historyUpdate(null, new ArrayList<>()).isEmpty());
	}

	@Test
	void notificationsReachTheListeners() {
		final RecordingListener listener = new RecordingListener();
		namespace.addListener(listener);
		try {
			assertTrue(namespace.getListeners().contains(listener));
			namespace.onDataItemsCreated(List.of());
			namespace.onDataItemsModified(List.of());
			namespace.onDataItemsDeleted(List.of());
			namespace.onMonitoringModeChanged(List.of());
			namespace.onEventItemsCreated(List.of());
			namespace.onEventItemsModified(List.of());
			namespace.onEventItemsDeleted(List.of());
			final WaldotVertex marko = (WaldotVertex) vertexNamed("marko");
			namespace.opcuaUpdateEvent((UaNode) marko);
			assertEquals(List.of("created", "modified", "deleted", "monitoring",
					"update:" + marko.getNodeId().getIdentifier()), listener.events);
		} finally {
			namespace.removeListener(listener);
		}
		assertFalse(namespace.getListeners().contains(listener));
	}

	@Test
	void pluginCommandsAreRegisteredAndRemoved() {
		final RecordingListener listener = new RecordingListener();
		namespace.addListener(listener);
		final TestCommand command = new TestCommand(namespace, "delegation-test", "tests/delegation");
		// no directory: the command is linked to the commands root
		final TestCommand rootCommand = new TestCommand(namespace, "delegation-root-test", null);
		final PluginListener plugin = new PluginListener() {
			@Override
			public Collection<WaldotCommand> getCommands() {
				return List.of(command, rootCommand);
			}

			@Override
			public void initialize(final WaldotNamespace waldotNamespace) {
				// nothing to prepare
			}
		};
		try {
			namespace.registerPlugin(plugin);
			assertTrue(namespace.hasNodeId(command.getNodeId()));
			assertTrue(namespace.hasNodeId(rootCommand.getNodeId()));
			assertTrue(namespace.listConfiguredCommands().stream().anyMatch(c -> c.equals(command.getConsoleCommand())),
					namespace.listConfiguredCommands().toString());
			namespace.unregisterPlugin(plugin);
			assertFalse(namespace.hasNodeId(command.getNodeId()));
			assertFalse(namespace.hasNodeId(rootCommand.getNodeId()));
			// a second unregister is a no-op
			assertDoesNotThrow(() -> namespace.unregisterPlugin(plugin));
			assertDoesNotThrow(() -> namespace.unregisterPlugin(null));
			assertEquals(List.of("registered:" + command.getConsoleCommand(), "registered:" + rootCommand.getConsoleCommand(),
					"removed:" + command.getConsoleCommand(), "removed:" + rootCommand.getConsoleCommand()), listener.events);
		} finally {
			namespace.removeListener(listener);
		}
	}

	@Test
	void duplicatedIdsAreRejected() {
		final Vertex marko = vertexNamed("marko");
		assertThrows(IllegalArgumentException.class, () -> graph.addVertex(T.id, marko.id(), "name", "copy"));
		final Edge knows = graph.traversal().E().hasLabel("knows").next();
		final Vertex peter = vertexNamed("peter");
		assertThrows(IllegalArgumentException.class, () -> marko.addEdge("knows", peter, T.id, knows.id()));
	}

	@Test
	void edgeIsRemovedThroughTheNamespace() {
		final Vertex a = graph.addVertex("name", "edge-removal-a");
		final Vertex b = graph.addVertex("name", "edge-removal-b");
		final Edge edge = a.addEdge("temporary", b);
		final NodeId edgeId = ((WaldotEdge) edge).getNodeId();
		assertNotNull(namespace.getEdgeNode(edgeId));
		namespace.removeEdge(edgeId);
		assertNull(namespace.getEdgeNode(edgeId));
		assertFalse(a.edges(Direction.OUT, "temporary").hasNext());
	}

	// --- OPC UA commands from Gremlin: call("waldot.command") ---

	/** Runs the body with {@value GremlinCommandSettings#ENABLED_KEY} set. */
	private static void withCommandsFlag(final String value, final Runnable body) {
		final String saved = System.getProperty(GremlinCommandSettings.ENABLED_KEY);
		if (value == null) {
			System.clearProperty(GremlinCommandSettings.ENABLED_KEY);
		} else {
			System.setProperty(GremlinCommandSettings.ENABLED_KEY, value);
		}
		try {
			body.run();
		} finally {
			if (saved == null) {
				System.clearProperty(GremlinCommandSettings.ENABLED_KEY);
			} else {
				System.setProperty(GremlinCommandSettings.ENABLED_KEY, saved);
			}
		}
	}

	private static Map<String, Object> commandParams(final String command, final String... args) {
		return Map.of(OpcCommandServiceFactory.Params.COMMAND, command, OpcCommandServiceFactory.Params.ARGS,
				List.of(args));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> outputs(final Map<String, Object> result) {
		return (Map<String, Object>) result.get(OpcCommandServiceFactory.RESULT_OUTPUTS);
	}

	@Test
	void gremlinCommandUpdatesAPropertyOfAGeneratedId() {
		withCommandsFlag("true", () -> {
			final Vertex target = graph.addVertex("name", "gremlin-command-target");
			final List<Map<String, Object>> results = graph.traversal().V(target.id())
					.<Map<String, Object>>call(OpcCommandServiceFactory.NAME, commandParams("property", "color", "red"))
					.toList();
			assertEquals(1, results.size(), results.toString());
			final Map<String, Object> result = results.get(0);
			assertNull(result.get(OpcCommandServiceFactory.RESULT_ERROR), result.toString());
			assertEquals(((WaldotVertex) target).getNodeId().toParseableString(),
					result.get(OpcCommandServiceFactory.RESULT_ELEMENT));
			assertEquals("property", result.get(OpcCommandServiceFactory.RESULT_COMMAND));
			assertTrue(outputs(result).get("output").toString().contains("updated"), result.toString());
			assertEquals("", outputs(result).get("error"));
			assertEquals("red", graph.traversal().V(target.id()).values("color").next());
		});
	}

	@Test
	void gremlinDeleteOnManyVerticesRunsAfterTheBarrier() {
		withCommandsFlag("true", () -> {
			graph.addVertex("name", "gremlin-delete-1");
			graph.addVertex("name", "gremlin-delete-2");
			graph.addVertex("name", "gremlin-delete-3");
			final List<Map<String, Object>> results = graph.traversal().V()
					.has("name", P.within("gremlin-delete-1", "gremlin-delete-2", "gremlin-delete-3"))
					.<Map<String, Object>>call(OpcCommandServiceFactory.NAME, commandParams("delete")).toList();
			assertEquals(3, results.size(), results.toString());
			for (final Map<String, Object> result : results) {
				assertNull(result.get(OpcCommandServiceFactory.RESULT_ERROR), result.toString());
				assertEquals("", outputs(result).get("error"), result.toString());
				assertTrue(outputs(result).get("output").toString().contains("deleted"), result.toString());
			}
			assertEquals(0L, graph.traversal().V()
					.has("name", P.within("gremlin-delete-1", "gremlin-delete-2", "gremlin-delete-3")).count().next());
		});
	}

	@Test
	void gremlinCommandsOfAnEdge() {
		withCommandsFlag("true", () -> {
			final Vertex a = graph.addVertex("name", "gremlin-edge-a");
			final Vertex b = graph.addVertex("name", "gremlin-edge-b");
			final Edge edge = a.addEdge("gremlinEdge", b);
			final Map<String, Object> updated = graph.traversal().E(edge.id())
					.<Map<String, Object>>call(OpcCommandServiceFactory.NAME, commandParams("property", "color", "blue"))
					.next();
			assertNull(updated.get(OpcCommandServiceFactory.RESULT_ERROR), updated.toString());
			assertEquals(((WaldotEdge) edge).getNodeId().toParseableString(),
					updated.get(OpcCommandServiceFactory.RESULT_ELEMENT));
			assertEquals("blue", graph.traversal().E(edge.id()).values("color").next());
			final Map<String, Object> deleted = graph.traversal().E(edge.id())
					.<Map<String, Object>>call(OpcCommandServiceFactory.NAME, commandParams("delete")).next();
			assertNull(deleted.get(OpcCommandServiceFactory.RESULT_ERROR), deleted.toString());
			assertFalse(graph.traversal().E().hasLabel("gremlinEdge").hasNext());
			assertTrue(graph.traversal().V(a.id()).hasNext());
		});
	}

	@Test
	void gremlinUnknownCommandIsAnError() {
		withCommandsFlag("true", () -> {
			final Vertex marko = vertexNamed("marko");
			final Map<String, Object> result = graph.traversal().V(marko.id())
					.<Map<String, Object>>call(OpcCommandServiceFactory.NAME, commandParams("not-a-command")).next();
			assertTrue(result.get(OpcCommandServiceFactory.RESULT_ERROR).toString().contains("not found"),
					result.toString());
			assertNull(result.get(OpcCommandServiceFactory.RESULT_OUTPUTS));
		});
	}

	@Test
	void gremlinMissingArgumentsAreAnError() {
		withCommandsFlag("true", () -> {
			final Vertex target = graph.addVertex("name", "gremlin-missing-args");
			final Map<String, Object> result = assertDoesNotThrow(() -> graph.traversal().V(target.id())
					.<Map<String, Object>>call(OpcCommandServiceFactory.NAME, commandParams("property", "only-label"))
					.next());
			assertTrue(result.get(OpcCommandServiceFactory.RESULT_ERROR).toString().contains("requires"),
					result.toString());
			assertFalse(graph.traversal().V(target.id()).has("only-label").hasNext());
		});
	}

	@Test
	void gremlinCommandOfARemovedElementIsAnError() {
		withCommandsFlag("true", () -> {
			final Vertex target = graph.addVertex("name", "gremlin-removed");
			target.remove();
			final OpcCommandServiceFactory factory = new OpcCommandServiceFactory((AbstractOpcGraph) graph);
			final Map<String, Object> params = commandParams("delete");
			final TraverserSet<Object> in = new TraverserSet<>();
			in.add(new B_O_Traverser<>(target, 1));
			final Map<String, Object> result = factory.createService(false, params).execute(null, in, params).next();
			assertTrue(result.get(OpcCommandServiceFactory.RESULT_ERROR).toString().contains("removed"),
					result.toString());
		});
	}

	@Test
	void gremlinGlobalCommandsRunInStartMode() {
		final TestCommand global = new TestCommand(namespace, "gremlin-global", null, true, new Object[] { "pong", 7 });
		global.addOutputArgument("reply", AbstractOpcCommand.VariableNodeTypes.String.getNodeId(), -1, null,
				LocalizedText.english("reply"));
		final TestCommand disabled = new TestCommand(namespace, "gremlin-not-executable", null, false,
				new Object[] { "never" });
		namespace.registerCommand(global);
		namespace.registerCommand(disabled);
		try {
			withCommandsFlag("true", () -> {
				final Map<String, Object> result = graph.traversal()
						.<Map<String, Object>>call(OpcCommandServiceFactory.NAME,
								Map.of(OpcCommandServiceFactory.Params.COMMAND, global.getConsoleCommand()))
						.next();
				assertNull(result.get(OpcCommandServiceFactory.RESULT_ERROR), result.toString());
				assertNull(result.get(OpcCommandServiceFactory.RESULT_ELEMENT));
				assertEquals(Map.of("reply", "pong", "out1", 7), outputs(result));
				final Map<String, Object> notExecutable = graph.traversal()
						.<Map<String, Object>>call(OpcCommandServiceFactory.NAME,
								Map.of(OpcCommandServiceFactory.Params.COMMAND, disabled.getConsoleCommand()))
						.next();
				assertTrue(notExecutable.get(OpcCommandServiceFactory.RESULT_ERROR).toString()
						.contains("not executable"), notExecutable.toString());
				assertNull(notExecutable.get(OpcCommandServiceFactory.RESULT_OUTPUTS));
				// the list service shows the global commands
				final List<Object> names = graph.traversal()
						.<Map<String, Object>>call(OpcCommandListServiceFactory.NAME).toList().stream()
						.map(c -> c.get(OpcCommandListServiceFactory.RESULT_COMMAND)).toList();
				assertTrue(names.contains(global.getConsoleCommand()), names.toString());
			});
		} finally {
			namespace.removeCommand(global);
			namespace.removeCommand(disabled);
		}
	}

	@Test
	void gremlinCommandsAreRefusedWhenDisabled() {
		withCommandsFlag(null, () -> {
			final Vertex marko = vertexNamed("marko");
			final IllegalStateException e = assertThrows(IllegalStateException.class, () -> graph.traversal()
					.V(marko.id()).call(OpcCommandServiceFactory.NAME, commandParams("delete")).toList());
			assertTrue(e.getMessage().contains(GremlinCommandSettings.ENABLED_KEY), e.getMessage());
			assertTrue(graph.traversal().V(marko.id()).hasNext());
		});
	}

	@Test
	void gremlinListsTheCommandsOfAnElement() {
		withCommandsFlag(null, () -> {
			final Vertex marko = vertexNamed("marko");
			final List<Map<String, Object>> commands = graph.traversal().V(marko.id())
					.<Map<String, Object>>call(OpcCommandListServiceFactory.NAME).toList();
			final List<Object> names = commands.stream()
					.map(c -> c.get(OpcCommandListServiceFactory.RESULT_COMMAND)).toList();
			assertTrue(names.containsAll(List.of("delete", "property")), names.toString());
			final Map<String, Object> property = commands.stream()
					.filter(c -> "property".equals(c.get(OpcCommandListServiceFactory.RESULT_COMMAND))).findFirst()
					.orElseThrow();
			assertEquals(2, ((List<?>) property.get(OpcCommandListServiceFactory.RESULT_INPUTS)).size(),
					property.toString());
			assertNotNull(property.get(OpcCommandListServiceFactory.RESULT_DESCRIPTION));
		});
	}

	@Test
	void gremlinCommandFromJexl() {
		withCommandsFlag("true", () -> {
			graph.addVertex(T.id, "gremlin-jexl-target", "name", "gremlin-jexl-target");
			final Object result = namespace.runExpression("g.V('gremlin-jexl-target').call('waldot.command', "
					+ "{'command':'property','args':['k','v']}).toList()");
			assertTrue(result instanceof List && ((List<?>) result).size() == 1, String.valueOf(result));
			final Map<?, ?> invocation = (Map<?, ?>) ((List<?>) result).get(0);
			assertNull(invocation.get(OpcCommandServiceFactory.RESULT_ERROR), invocation.toString());
			assertEquals("v", graph.traversal().V().has("name", "gremlin-jexl-target").values("k").next());
		});
	}

	@Test
	void accessorsOfTheNamespace() {
		assertNotNull(namespace.getEventFactory());
		assertNotNull(namespace.getNodeIdManager());
		assertNotNull(namespace.getEventBus());
		assertNotNull(namespace.getNamespaceTable());
		assertNotNull(namespace.getObjectTypeManager());
		assertNotNull(namespace.getReferenceTypes());
		assertNotNull(namespace.getTimer());
		assertNotNull(namespace.getCommandsAsFunction());
		assertNotNull(namespace.getConsoleLogger());
		assertNotNull(namespace.getBootLogger());
		assertEquals(WaldotTestEnvironment.bootUrl(), namespace.getBootstrapUrl());
		assertSame(graph, namespace.getGremlinGraph());
		assertNotNull(namespace.generateNodeId(42L));
		assertNotNull(namespace.generateNodeId(org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger
				.valueOf(43)));
		assertNotNull(namespace.generateNodeId(java.util.UUID.randomUUID()));
		assertEquals("label", namespace.generateQualifiedName("label").getName());
		assertNull(namespace.getGraphComputerView());
		assertFalse(namespace.inComputerMode());
		assertDoesNotThrow(namespace::dropGraphComputerView);
		assertNull(((OpcGraph) graph).getGraphComputerView());
		final var server = namespace.getOpcuaServer();
		namespace.setOpcuaServer(server);
		assertSame(server, namespace.getOpcuaServer());
	}
}
