package net.rossonet.waldot.client.integration;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.DataChangeTrigger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import net.rossonet.waldot.api.exchange.DataExchangeProtocol;
import net.rossonet.waldot.api.exchange.GraphRenderOptions;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.client.api.WaldOTAgentClient;
import net.rossonet.waldot.client.api.WaldOTAgentClient.Status;
import net.rossonet.waldot.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.client.api.WaldotClient;
import net.rossonet.waldot.client.api.WaldotClientConfigurationBuilder;
import net.rossonet.waldot.client.api.WaldotClientException;
import net.rossonet.waldot.client.api.WaldotClientListener;
import net.rossonet.waldot.client.api.address.NodeInfo;
import net.rossonet.waldot.client.api.address.ReferenceInfo;
import net.rossonet.waldot.client.api.command.CommandDescriptor;
import net.rossonet.waldot.client.api.command.CommandResult;
import net.rossonet.waldot.client.api.graph.ElementDescription;
import net.rossonet.waldot.client.api.graph.GraphChangeResult;
import net.rossonet.waldot.client.api.graph.GraphSnapshot;
import net.rossonet.waldot.client.api.graph.GraphRendering;
import net.rossonet.waldot.client.api.graph.QueryResult;
import net.rossonet.waldot.client.api.subscription.DataItemOptions;
import net.rossonet.waldot.client.api.subscription.EventItemOptions;
import net.rossonet.waldot.client.api.subscription.MonitoredDataItem;
import net.rossonet.waldot.client.api.subscription.MonitoredEventItem;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroup;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroupListener;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroupOptions;
import net.rossonet.waldot.client.api.subscription.WaldotEvent;
import net.rossonet.waldot.client.utils.WaldotTestClientHandler;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Client v2 against an embedded WaldOT server: connection (A), commands (B),
 * data exchange commands (C), value and event subscriptions (D, E), graph
 * snapshots (F, G), address space (H) and compatibility of the first version.
 */
@TestInstance(Lifecycle.PER_CLASS)
class WaldotClientV2IntegrationTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(20);

	private static void await(final String what, final BooleanSupplier condition) throws InterruptedException {
		final long deadline = System.nanoTime() + TIMEOUT.toNanos();
		while (!condition.getAsBoolean()) {
			if (System.nanoTime() > deadline) {
				throw new AssertionError("timeout waiting for " + what);
			}
			Thread.sleep(20);
		}
	}

	private static Number number(final DataValue value) {
		return (Number) value.getValue().getValue();
	}

	private WaldotClient client;
	private WaldotGraph graph;
	private final List<Status> statuses = new CopyOnWriteArrayList<>();

	private NodeId id(final String identifier) {
		return client.addressSpace().waldotNodeId(identifier);
	}

	@BeforeEach
	void resetGraph() {
		graph.getWaldotNamespace().resetNameSpace();
		final Vertex marko = graph.addVertex(T.id, "x-marko", T.label, "person", "name", "marko", "age", 29);
		final Vertex vadas = graph.addVertex(T.id, "x-vadas", T.label, "person", "name", "vadas", "age", 27);
		final Vertex lop = graph.addVertex(T.id, "x-lop", T.label, "software", "name", "lop", "lang", "java");
		final Vertex josh = graph.addVertex(T.id, "x-josh", T.label, "person", "name", "josh", "age", 32);
		final Vertex ripple = graph.addVertex(T.id, "x-ripple", T.label, "software", "name", "ripple", "lang",
				"java");
		final Vertex peter = graph.addVertex(T.id, "x-peter", T.label, "person", "name", "peter", "age", 35);
		marko.addEdge("knows", vadas, T.id, "x-e7", "weight", 0.5d);
		marko.addEdge("knows", josh, T.id, "x-e8", "weight", 1.0d);
		marko.addEdge("created", lop, T.id, "x-e9", "weight", 0.4d);
		josh.addEdge("created", ripple, T.id, "x-e10", "weight", 1.0d);
		josh.addEdge("created", lop, T.id, "x-e11", "weight", 0.4d);
		peter.addEdge("created", lop, T.id, "x-e12", "weight", 0.2d);
	}

	@BeforeAll
	void startServerAndClient() throws Exception {
		WaldotTestEnvironment.awaitOpcPortReleased();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy());
		final WaldOTAgentClientConfiguration configuration = WaldotClientConfigurationBuilder.create()
				.agentName("v2-test").defaultPublishingInterval(50).defaultSamplingInterval(10)
				.reconnect(Duration.ofMillis(100), Duration.ofSeconds(1), 0).build();
		client = WaldotClient.create(configuration);
		client.addListener(new WaldotClientListener() {
			@Override
			public void onStatusChanged(final Status previous, final Status current) {
				statuses.add(current);
			}
		});
		client.connect(TIMEOUT);
	}

	@AfterAll
	void stopServerAndClient() {
		if (client != null) {
			client.close();
		}
		WaldotTestEnvironment.cleanup(graph, null);
	}

	// ---- A: connection -------------------------------------------------

	@Test
	void connectionIsRunning() {
		assertEquals(Status.RUNNING, client.getStatus());
		assertTrue(client.isConnected());
		assertTrue(statuses.contains(Status.RUNNING));
		assertNotNull(client.getOpcUaClient());
		assertEquals(2, client.namespaceIndex());
		assertEquals(2, client.addressSpace().namespaceIndex());
	}

	@Test
	void stopAndStartRecreatesTheGroups() throws Exception {
		final List<String> recreated = new CopyOnWriteArrayList<>();
		final AtomicInteger groupRecreated = new AtomicInteger();
		final WaldotClientListener listener = new WaldotClientListener() {
			@Override
			public void onSubscriptionGroupRecreated(final String groupName) {
				recreated.add(groupName);
			}
		};
		client.addListener(listener);
		final SubscriptionGroup group = client.subscriptions().createGroup("restart",
				SubscriptionGroupOptions.withPublishingInterval(50));
		group.addListener(new SubscriptionGroupListener() {
			@Override
			public void onRecreated(final SubscriptionGroup g) {
				groupRecreated.incrementAndGet();
			}
		});
		final List<DataValue> values = new CopyOnWriteArrayList<>();
		group.subscribeData(client.graph().propertyNodeId(id("x-marko"), "age"), DataItemOptions.sampling(10),
				(item, value) -> values.add(value));
		await("first value", () -> !values.isEmpty());
		try {
			client.stopConnectionProcedure().get();
			assertEquals(Status.STOPPED, client.getStatus());
			assertTrue(group.subscriptionId().isEmpty());
			assertThrows(WaldotClientException.class, () -> client.graph().queryJson("1"));
			client.connect(TIMEOUT);
			await("group recreated", () -> recreated.contains("restart"));
			assertEquals(1, groupRecreated.get());
			assertTrue(group.subscriptionId().isPresent());
			values.clear();
			client.addressSpace().writeValue(client.graph().propertyNodeId(id("x-marko"), "age"), 77);
			await("value after restart", () -> values.stream().anyMatch(v -> number(v).intValue() == 77));
			assertEquals(2, client.namespaceIndex());
		} finally {
			client.removeListener(listener);
			group.close();
		}
	}

	// ---- B: commands -----------------------------------------------------

	@Test
	void globalCommandsAreDiscovered() {
		final List<CommandDescriptor> commands = client.commands().globalCommands();
		assertFalse(commands.isEmpty());
		final CommandDescriptor queryJson = client.commands().globalCommand(DataExchangeProtocol.QUERY_JSON_COMMAND)
				.orElseThrow();
		assertEquals(DataExchangeProtocol.INFORMATION_DIRECTORY, queryJson.path());
		assertEquals(id(DataExchangeProtocol.INFORMATION_DIRECTORY), queryJson.objectId());
		assertEquals(List.of(DataExchangeProtocol.ARG_COMMAND),
				queryJson.inputs().stream().map(a -> a.name()).toList());
		assertEquals(NodeIds.String, queryJson.inputs().get(0).dataType());
		assertEquals(1, queryJson.outputIndex(DataExchangeProtocol.ARG_ERROR));
		assertNotNull(queryJson.description());
		assertEquals(DataExchangeProtocol.MANAGE_DIRECTORY,
				client.commands().globalCommand(DataExchangeProtocol.EXPORT_GRAPH_COMMAND).orElseThrow().path());
		assertTrue(client.commands().globalCommand("no such command").isEmpty());
		client.commands().refresh();
		assertEquals(commands.size(), client.commands().globalCommands().size());
		assertFalse(client.commands().about().isEmpty());
		assertThrows(WaldotClientException.class, () -> client.commands().invokeGlobal("no such command"));
	}

	@Test
	void elementCommands() throws Exception {
		final NodeId marko = id("x-marko");
		final List<CommandDescriptor> commands = client.commands().elementCommands(marko);
		final Set<String> names = Set.copyOf(commands.stream().map(CommandDescriptor::name).toList());
		assertTrue(names.containsAll(Set.of("delete", "property")), names.toString());
		final CommandResult update = client.commands().setProperty(marko, "color", "red");
		assertTrue(update.isSuccess(), update.toString());
		assertEquals("red", graph.traversal().V(new NodeId(2, "x-marko")).values("color").next());
		assertThrows(WaldotClientException.class, () -> client.commands().invokeOnElement(marko, "unknown"));
		final CommandResult deleted = client.commands().deleteElement(id("x-vadas"));
		assertTrue(deleted.isSuccess(), deleted.toString());
		assertEquals(5, graph.getWaldotNamespace().getVerticesCount());
	}

	@Test
	void baseOperations() throws Exception {
		assertFalse(client.getServerInfo().isEmpty());
		final List<String> expression = client.runExpression("1 + 2");
		assertEquals("3", expression.get(0));
		final String[] created = client.createVertex("x-new", "label-new", null, new String[] { "value", "12" });
		assertEquals("", created[1], java.util.Arrays.toString(created));
		await("vertex created", () -> graph.getWaldotNamespace().getVerticesCount() == 7);
		client.deleteVertex("x-new");
		await("vertex deleted", () -> graph.getWaldotNamespace().getVerticesCount() == 6);
	}

	// ---- C: graph through the data exchange commands -------------------

	@Test
	void structuredQueries() {
		final List<Vertex> persons = client.graph().vertices("g.V().hasLabel('person')");
		assertEquals(4, persons.size());
		assertTrue(persons.stream().allMatch(v -> v.id() instanceof NodeId));
		final List<Edge> knows = client.graph().edges("g.E().hasLabel('knows')");
		assertEquals(2, knows.size());
		assertEquals(6L, ((Number) client.graph().queryValue("g.V().count()")).longValue());
		assertEquals(List.of("josh", "vadas"), client.graph().queryObjects(
				"g.V().has('name','marko').out('knows').values('name').order()", String.class));
		final Object map = client.graph().queryValue("g.V().has('name','lop').project('n','l').by('name').by('lang')");
		assertEquals(Map.of("n", "lop", "l", "java"), map);
		assertTrue(client.graph().queryJson("g.V().count()").contains("g:Int64"));
		assertTrue(client.graph().queryObjects("null").isEmpty());
		final QueryResult text = client.graph().query("g.V().count().next()");
		assertTrue(text.isSuccess(), text.toString());
		assertEquals("6", text.output());
		assertFalse(client.graph().query("g.V(").isSuccess());
		assertThrows(WaldotClientException.class, () -> client.graph().queryJson("g.V("));
	}

	@Test
	void describeElements() {
		final ElementDescription marko = client.graph().describe(id("x-marko"));
		assertEquals("vertex", marko.kind());
		assertEquals("person", marko.label());
		assertEquals(3, marko.outEdges().size());
		assertTrue(marko.inEdges().isEmpty());
		final ElementDescription.PropertyDescription age = marko.property("age");
		assertNotNull(age);
		assertEquals(client.graph().propertyNodeId(id("x-marko"), "age"), age.nodeId());
		assertTrue(marko.commands().stream().anyMatch(c -> "property".equals(c.name())));
		final ElementDescription edge = client.graph().describe("x-e7");
		assertTrue(edge.isEdge());
		assertEquals(id("x-marko"), edge.source());
		assertEquals(id("x-vadas"), edge.target());
		assertThrows(WaldotClientException.class, () -> client.graph().describe("missing-element"));
	}

	// ---- D: value subscriptions -----------------------------------------

	@Test
	void propertySubscriptionWithRuntimeChanges() throws Exception {
		final NodeId marko = id("x-marko");
		final NodeId age = client.graph().propertyNodeId(marko, "age");
		final List<Integer> values = new CopyOnWriteArrayList<>();
		final SubscriptionGroup group = client.subscriptions().createGroup("values",
				SubscriptionGroupOptions.withPublishingInterval(50));
		final List<String> groupEvents = new CopyOnWriteArrayList<>();
		group.addListener(new SubscriptionGroupListener() {
			@Override
			public void onKeepAlive(final SubscriptionGroup g) {
				groupEvents.add("keepalive");
			}
		});
		try {
			final MonitoredDataItem item = group.subscribeData(age, DataItemOptions.sampling(10),
					(i, v) -> values.add(number(v).intValue()));
			await("initial value", () -> values.contains(29));
			assertTrue(item.status().orElseThrow().isGood());
			assertTrue(item.revisedSamplingInterval().isPresent());
			assertEquals(29, number(item.lastValue().orElseThrow()).intValue());
			assertTrue(group.subscriptionId().isPresent());
			assertTrue(group.revisedPublishingInterval().isPresent());

			assertTrue(client.addressSpace().writeValue(age, 30).isGood());
			await("written value", () -> values.contains(30));
			assertEquals(30, graph.traversal().V(new NodeId(2, "x-marko")).values("age").next());

			// queue, trigger and sampling interval at runtime
			final DataItemOptions changed = item.options().queue(5, true)
					.trigger(DataChangeTrigger.StatusValueTimestamp);
			item.modify(changed);
			assertEquals(changed, item.options());
			assertTrue(item.status().orElseThrow().isGood(), item.status().toString());
			assertEquals(5, item.revisedQueueSize().orElseThrow().intValue());
			client.addressSpace().writeValue(age, 41);
			await("value after modify", () -> values.contains(41));

			// the WaldOT properties are BaseDataType variables: the server refuses
			// a deadband and the previous parameters stay active
			final WaldotClientException refused = assertThrows(WaldotClientException.class,
					() -> item.modify(changed.absoluteDeadband(5)));
			assertEquals(StatusCodes.Bad_FilterNotAllowed, refused.getStatusCode().getValue());
			assertEquals(changed, item.options());
			client.addressSpace().writeValue(age, 42);
			await("value after the refused modify", () -> values.contains(42));

			// disabled item: no notifications
			item.setMonitoringMode(MonitoringMode.Disabled);
			assertEquals(MonitoringMode.Disabled, item.monitoringMode());
			client.addressSpace().writeValue(age, 60);
			Thread.sleep(300);
			assertFalse(values.contains(60));
			group.setMonitoringMode(MonitoringMode.Reporting, List.of(item));
			await("value after enabling", () -> values.contains(60));

			// publishing interval and publishing mode at runtime
			group.modify(group.options().publishingInterval(200).priority(10));
			assertEquals(200.0, group.revisedPublishingInterval().orElseThrow());
			group.setPublishingEnabled(false);
			assertFalse(group.isPublishingEnabled());
			client.addressSpace().writeValue(age, 70);
			Thread.sleep(500);
			assertFalse(values.contains(70));
			group.setPublishingEnabled(true);
			await("value after publishing enabled", () -> values.contains(70));
			await("keep alive", () -> !groupEvents.isEmpty());

			item.close();
			assertTrue(group.dataItems().isEmpty());
		} finally {
			group.close();
		}
	}

	@Test
	void shortcutSubscriptionsAndErrors() throws Exception {
		final List<Object> names = new CopyOnWriteArrayList<>();
		final MonitoredDataItem item = client.subscriptions().subscribeProperty(id("x-lop"), "name", null,
				(i, v) -> names.add(v.getValue().getValue()));
		try {
			await("name", () -> names.contains("lop"));
			assertEquals(net.rossonet.waldot.client.api.subscription.WaldotSubscriptionClient.DEFAULT_GROUP, item.group().name());
			final WaldotClientException error = assertThrows(WaldotClientException.class,
					() -> client.subscriptions().subscribeValue(id("missing/node"), null, (i, v) -> {
					}));
			assertTrue(error.getStatusCode().isBad());
			assertEquals(1, client.subscriptions().defaultGroup().dataItems().size());
		} finally {
			item.close();
		}
	}

	// ---- E: event subscriptions -------------------------------------------

	@Test
	void vertexEvents() throws Exception {
		final AbstractOpcVertex vertex = (AbstractOpcVertex) graph.addVertex("id", "x-events", "label", "emitter",
				"event-notifier", "true");
		final List<WaldotEvent> events = new CopyOnWriteArrayList<>();
		final List<WaldotEvent> filtered = new CopyOnWriteArrayList<>();
		final SubscriptionGroup group = client.subscriptions().createGroup("events",
				SubscriptionGroupOptions.withPublishingInterval(50));
		try {
			final MonitoredEventItem item = group.subscribeEvents(id("x-events"), EventItemOptions.defaults(),
					(i, e) -> events.add(e));
			group.subscribeEvents(id("x-events"),
					EventItemOptions.defaults().onlyTypes(NodeIds.BaseModelChangeEventType), (i, e) -> filtered.add(e));
			assertTrue(item.status().orElseThrow().isGood());
			for (int i = 0; i < 5; i++) {
				postEvent(vertex, String.valueOf(i));
			}
			await("events", () -> events.size() == 5);
			assertEquals("0", events.get(0).message());
			assertEquals(3, events.get(0).severity());
			assertEquals(id("x-events"), events.get(0).sourceNode());
			assertEquals(NodeIds.BaseEventType, events.get(0).eventType());
			assertTrue(filtered.isEmpty());

			// fields changed at runtime
			item.modify(EventItemOptions.defaults().withFields(WaldotEvent.MESSAGE));
			assertTrue(item.status().orElseThrow().isGood());
			postEvent(vertex, "only message");
			await("event with one field", () -> events.size() == 6);
			assertEquals(Set.of(WaldotEvent.MESSAGE), events.get(5).fields().keySet());

			item.setMonitoringMode(MonitoringMode.Disabled);
			assertEquals(MonitoringMode.Disabled, item.monitoringMode());
			assertTrue(item.status().orElseThrow().isGood());
			postEvent(vertex, "lost");
			item.setMonitoringMode(MonitoringMode.Reporting);
			assertEquals(MonitoringMode.Reporting, item.monitoringMode());
			postEvent(vertex, "again");
			// the Milo server keeps the events of a disabled event item in its
			// queue: only the delivery after the new reporting mode is checked
			await("event after enabling", () -> events.stream().anyMatch(e -> "again".equals(e.message())));
		} finally {
			group.close();
		}
	}

	private void postEvent(final AbstractOpcVertex vertex, final String message) throws Exception {
		final BaseEventTypeNode eventNode = graph.getWaldotNamespace().getOpcuaServer().getServer().getEventFactory()
				.createEvent(graph.getWaldotNamespace().generateNodeId(UUID.randomUUID()), NodeIds.BaseEventType);
		eventNode.setBrowseName(new QualifiedName(1, "message"));
		eventNode.setDisplayName(LocalizedText.english("message"));
		eventNode.setEventId(ByteString.of(new byte[] { 0, 1, 2, 3 }));
		eventNode.setEventType(NodeIds.BaseEventType);
		eventNode.setSourceNode(vertex.getNodeId());
		eventNode.setSourceName(vertex.getDisplayName().text());
		eventNode.setTime(DateTime.now());
		eventNode.setReceiveTime(DateTime.NULL_VALUE);
		eventNode.setMessage(LocalizedText.english(message));
		eventNode.setSeverity(ushort(3));
		vertex.postEvent(eventNode);
		eventNode.delete();
	}

	// ---- F, G: snapshots ------------------------------------------------

	@Test
	void exportClearAndRestore() {
		final GraphSnapshot snapshot = client.snapshots().exportGraph();
		assertTrue(snapshot.compressed());
		assertEquals(6, snapshot.vertices());
		assertEquals(6, snapshot.edges());
		assertEquals(1, snapshot.blocks());
		assertTrue(snapshot.text().contains("x-marko"));

		final GraphChangeResult cleared = client.snapshots().clearGraph();
		assertEquals(6, cleared.vertices());
		assertEquals(6, cleared.edges());
		assertEquals(0, graph.getWaldotNamespace().getVerticesCount());

		final GraphChangeResult restored = client.snapshots().restore(snapshot);
		assertFalse(restored.hasWarnings(), restored.warnings());
		assertEquals(6, restored.vertices());
		assertEquals(6, restored.edges());
		assertEquals(6, graph.getWaldotNamespace().getVerticesCount());
		assertEquals(6, graph.getWaldotNamespace().getEdgesCount());
		assertEquals(29, graph.traversal().V(new NodeId(2, "x-marko")).values("age").next());
		assertEquals(3, client.graph().describe("x-marko").outEdges().size());
	}

	@Test
	void chunkedExportAndImport() {
		for (int i = 0; i < 150; i++) {
			graph.addVertex("id", "bulk-" + i, "name", "bulk vertex " + i, "payload",
					"some text that makes the snapshot bigger " + i);
		}
		final int vertices = graph.getWaldotNamespace().getVerticesCount();
		final GraphSnapshot snapshot = client.snapshots().exportGraph(false, false, DataExchangeProtocol.MIN_BLOCK_SIZE);
		assertFalse(snapshot.compressed());
		assertTrue(snapshot.blocks() > 1, snapshot.toString());
		assertEquals(vertices, snapshot.vertices());
		assertEquals(snapshot.size(), snapshot.text().getBytes(java.nio.charset.StandardCharsets.UTF_8).length);

		client.snapshots().clearGraph();
		final GraphChangeResult imported = client.snapshots().importGraph(snapshot, false,
				DataExchangeProtocol.MIN_BLOCK_SIZE);
		assertEquals(vertices, imported.vertices());
		assertEquals(vertices, graph.getWaldotNamespace().getVerticesCount());
		assertEquals(6, graph.getWaldotNamespace().getEdgesCount());

		assertThrows(IllegalArgumentException.class, () -> client.snapshots().exportGraph(true, false, 10));
		assertThrows(IllegalArgumentException.class,
				() -> client.snapshots().importGraph(snapshot, true, DataExchangeProtocol.MAX_BLOCK_SIZE + 1));
		assertThrows(WaldotClientException.class, () -> client.snapshots()
				.importGraph(GraphSnapshot.of("not graphson".getBytes()), true));
	}

	@Test
	void renderGraph() {
		final GraphRendering svg = client.graph().render(new GraphRenderOptions());
		assertEquals("svg", svg.format());
		assertEquals(6, svg.vertices());
		assertEquals(6, svg.edges());
		assertTrue(svg.asString().startsWith("<?xml"), svg.asString());
		assertTrue(svg.asString().contains("data-node-id=\"ns=2;s=x-marko\""), svg.asString());

		final GraphRenderOptions neighbours = new GraphRenderOptions().format(GraphRenderOptions.Format.DOT);
		neighbours.vertices().roots(List.of("x-marko")).direction(GraphRenderOptions.Direction.OUT);
		neighbours.edges().labels(List.of("knows"));
		final GraphRendering dot = client.graph().render(neighbours);
		assertEquals("dot", dot.format());
		assertEquals(3, dot.vertices());
		assertEquals(2, dot.edges());
		assertFalse(dot.hasWarnings(), dot.warnings().toString());

		// un vertices.max oltre il limite del server viene ridotto
		neighbours.vertices().max(DataExchangeProtocol.DEFAULT_MAX_RENDER_VERTICES + 1);
		final GraphRendering reduced = client.graph().render(neighbours);
		assertEquals(3, reduced.vertices());
		assertEquals(List.of("vertices.max " + (DataExchangeProtocol.DEFAULT_MAX_RENDER_VERTICES + 1)
				+ " reduced to the server limit " + DataExchangeProtocol.DEFAULT_MAX_RENDER_VERTICES),
				reduced.warnings());

		for (int i = 0; i < 80; i++) {
			graph.addVertex("id", "render-" + i, "name", "render vertex " + i);
		}
		final GraphRendering chunked = client.graph().render(new GraphRenderOptions(),
				DataExchangeProtocol.MIN_BLOCK_SIZE);
		assertTrue(chunked.blocks() > 1, chunked.toString());
		assertTrue(chunked.asString().endsWith("</svg>\n"));

		final WaldotClientException error = assertThrows(WaldotClientException.class,
				() -> client.graph().render(GraphRenderOptions.fromJson("{\"vertices\":{\"roots\":[\"none\"]}}")));
		assertTrue(error.getMessage().contains("none"), error.getMessage());
	}

	// ---- H: address space -------------------------------------------------

	@Test
	void addressSpaceNavigation() {
		final NodeId commandsRoot = id(client.getConfiguration().getCommandsRootNodeId());
		final List<ReferenceInfo> children = client.addressSpace().browse(commandsRoot);
		assertTrue(children.stream().anyMatch(r -> DataExchangeProtocol.INFORMATION_DIRECTORY.equals(r.browseName())),
				children.toString());
		final NodeId queryMethod = client.addressSpace()
				.resolvePath(commandsRoot, DataExchangeProtocol.INFORMATION_DIRECTORY, "query").orElseThrow();
		assertTrue(client.addressSpace().resolvePath(commandsRoot, "missing").isEmpty());
		assertTrue(client.addressSpace().resolvePath(NodeIds.RootFolder, "0:Objects", "Server").isPresent());

		final NodeInfo info = client.addressSpace().node(queryMethod).orElseThrow();
		assertEquals(NodeClass.Method, info.nodeClass());
		assertEquals("query", info.browseName());
		assertTrue(client.addressSpace().node(id("does-not-exist")).isEmpty());

		final List<Object> outputs = client.addressSpace().call(id(DataExchangeProtocol.INFORMATION_DIRECTORY),
				queryMethod, "2 * 21");
		assertEquals("42", outputs.get(0));
		assertThrows(WaldotClientException.class,
				() -> client.addressSpace().call(id(DataExchangeProtocol.INFORMATION_DIRECTORY), id("nope")));

		final NodeId age = client.addressSpace().parseNodeId("x-marko/age");
		assertEquals(age, client.addressSpace().parseNodeId("ns=2;s=x-marko/age"));
		assertEquals(29, number(client.addressSpace().readValue(age)).intValue());
		final List<DataValue> values = client.addressSpace()
				.readValues(List.of(age, client.addressSpace().parseNodeId("x-josh/age")));
		assertEquals(32, number(values.get(1)).intValue());
		final Map<AttributeId, DataValue> attributes = client.addressSpace().readAttributes(id("x-marko"),
				AttributeId.BrowseName, AttributeId.NodeClass);
		assertEquals(2, attributes.size());
		final StatusCode written = client.addressSpace().writeValue(age, 33);
		assertTrue(written.isGood(), written.toString());
		assertEquals(33, number(client.addressSpace().readValue(age)).intValue());
		assertThrows(IllegalArgumentException.class, () -> client.addressSpace().parseNodeId(" "));

		final List<ReferenceInfo> markoReferences = client.addressSpace().browse(id("x-marko"),
				net.rossonet.waldot.client.api.address.BrowseFilter.all());
		assertTrue(markoReferences.size() > 3);
	}

	// ---- compatibility of the first version ------------------------------

	@Test
	void firstVersionClientStillWorks() throws Exception {
		try (WaldOTAgentClient v1 = WaldOTAgentClient
				.withConfiguration(WaldOTAgentClientConfiguration.getDefaultConfiguration())) {
			final List<Status> received = new ArrayList<>();
			v1.setStatusObserver(new net.rossonet.waldot.client.api.WaldotAgentClientObserver() {
				@Override
				public void onStatusChanged(final Status status) {
					received.add(status);
				}
			});
			v1.startConnectionProcedure();
			assertTrue(v1.awaitStatus(Status.RUNNING, TIMEOUT));
			assertTrue(received.contains(Status.RUNNING));
			assertEquals("6", v1.runExpression("g.V().count().next()").get(0));
			assertFalse(v1.getServerInfo().isEmpty());
			v1.stopConnectionProcedure();
			assertEquals(Status.STOPPED, v1.getStatus());
		}
	}

	@Test
	void testClientHandlerStillWorks() throws Exception {
		final WaldotTestClientHandler handler = new WaldotTestClientHandler(graph);
		try {
			assertTrue(handler.checkOpcUaVertexExists("x-marko"));
			assertTrue(handler.checkVertexExists("x-marko"));
			assertTrue(handler.checkOpcUaVertexValueEquals("x-marko", "age", 29));
			assertTrue(handler.checkVertexValueEquals("x-marko", "age", 29));
			assertEquals(29, handler.readIntOpcUaVertexValue("x-marko", "age"));
			handler.writeOpcUaVertexValue("x-marko", "age", 30);
			assertTrue(handler.checkVertexValueEquals("x-marko", "age", 30));
			assertEquals("6", handler.runExpression("g.V().count().next()").get(0));
			assertFalse(handler.getServerInfo().isEmpty());
			assertFalse(handler.checkOpcUaVertexExists("missing"));
			assertTrue(handler.checkOpcUaVertexValueBetween("x-marko", "age", 20, 40));
			assertTrue(handler.checkVertexValueBetween("x-marko", "age", 20, 40));
			assertTrue(handler.checkOpcUaVertexValueEquals("x-marko", "age", 30));

			final String[] created = handler.createVertexWithOpcUa("x-h1", "h1", null, new String[] { "value", "12" });
			assertEquals("", created[1], java.util.Arrays.toString(created));
			handler.createVertexWithOpcUa("x-h2", "h2", null, new String[0]);
			assertTrue(handler.checkVertexExists("x-h1"));
			assertTrue(handler.checkVertexValueEquals("x-h1", "value", "12"));
			final String[] edge = handler.createEdgeWithOpcUa("link-h", "ns=2;s=x-h1", "ns=2;s=x-h2", new String[0]);
			assertEquals("", edge[1], java.util.Arrays.toString(edge));
			assertEquals(1L, graph.traversal().E().hasLabel("link-h").count().next());
			handler.deleteVertexWithOpcUa("x-h1");
			assertFalse(handler.checkVertexExists("x-h1"));
			assertEquals(2, handler.deleteVertexWithOpcUa(987654).length);

			final AbstractOpcVertex emitter = (AbstractOpcVertex) graph.addVertex("id", "x-h-events", "label",
					"emitter", "event-notifier", "true");
			final List<Object> messages = new CopyOnWriteArrayList<>();
			handler.subscribeToOpcUaVertexEventsMessage("x-h-events", new WaldotTestClientHandler.EventObserver() {
				@Override
				public void onEvent(final String nodeId, final String eventType, final Object value) {
					messages.add(value);
				}
			});
			postEvent(emitter, "from handler");
			await("handler event", () -> messages.contains(LocalizedText.english("from handler")));
		} finally {
			handler.close();
		}
	}
}
