package net.rossonet.waldot.client.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.DataChangeTrigger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.DeadbandType;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.exchange.DataExchangeProtocol;
import net.rossonet.waldot.client.api.WaldOTAgentClient.Status;
import net.rossonet.waldot.client.api.address.BrowseFilter;
import net.rossonet.waldot.client.api.command.ArgumentDescriptor;
import net.rossonet.waldot.client.api.command.CommandDescriptor;
import net.rossonet.waldot.client.api.command.CommandResult;
import net.rossonet.waldot.client.api.graph.ElementDescription;
import net.rossonet.waldot.client.api.graph.GraphChangeResult;
import net.rossonet.waldot.client.api.graph.GraphSnapshot;
import net.rossonet.waldot.client.api.graph.QueryResult;
import net.rossonet.waldot.client.api.subscription.DataItemOptions;
import net.rossonet.waldot.client.api.subscription.EventItemOptions;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroupOptions;
import net.rossonet.waldot.client.api.subscription.WaldotEvent;
import net.rossonet.waldot.client.v1.DefaultWaldOTAgentClientConfigurationV1;
import net.rossonet.waldot.client.v1.WaldOTAgentClientImplV1;
import net.rossonet.waldot.client.v2.WaldotClientImplV2;

/** Unit tests of the API model: configuration, DTOs, results, factories. */
class ApiModelTest {

	private static byte[] gzip(final byte[] data) throws Exception {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
			gzip.write(data);
		}
		return out.toByteArray();
	}

	@Test
	void browseFilterMask() {
		assertEquals(0, BrowseFilter.children().nodeClassMask());
		final BrowseFilter filter = BrowseFilter.forward().withNodeClasses(NodeClass.Object, NodeClass.Method);
		assertEquals(NodeClass.Object.getValue() | NodeClass.Method.getValue(), filter.nodeClassMask());
		assertEquals(BrowseDirection.Forward, filter.direction());
		assertEquals(BrowseDirection.Both, BrowseFilter.all().direction());
		assertEquals(NodeIds.HierarchicalReferences, BrowseFilter.children().referenceTypeId());
	}

	@Test
	void builderSetsEveryValue() {
		final WaldOTAgentClientConfiguration configuration = WaldotClientConfigurationBuilder.create()
				.endpoint("opc.tcp://example:4840/waldot").agentName("agent-1").applicationUri("urn:test:agent")
				.username("user", "secret").security(SecurityPolicy.Basic256Sha256, MessageSecurityMode.SignAndEncrypt)
				.keyStore("/tmp/x.ks", "pwd").forceDiscoveredEndpointUrl("opc.tcp://nat:4840/waldot")
				.trustAllServerCertificates(true).requestTimeout(Duration.ofSeconds(7))
				.sessionTimeout(Duration.ofSeconds(70)).keepAliveTimeout(Duration.ofSeconds(3))
				.reconnect(Duration.ofMillis(100), Duration.ofSeconds(2), 5).defaultPublishingInterval(250)
				.defaultSamplingInterval(50).commandsRootNodeId("commands").waldotNamespaceUri("urn:x").build();
		assertEquals("opc.tcp://example:4840/waldot", configuration.getBaseWaldOTOpcEndpoint());
		assertEquals("opc.tcp://example:4840/waldot/discovery", configuration.getDiscoveryEndpoint());
		assertEquals("agent-1", configuration.getAgentUniqueName());
		assertEquals("urn:test:agent", configuration.getApplicationUri());
		assertEquals(AuthenticationMode.USERNAME, configuration.getAuthenticationMode());
		assertEquals("user", configuration.getUsername());
		assertEquals("secret", configuration.getPassword());
		assertEquals(SecurityPolicy.Basic256Sha256, configuration.getSecurityPolicy());
		assertEquals(MessageSecurityMode.SignAndEncrypt, configuration.getMessageSecurityMode());
		assertEquals("/tmp/x.ks", configuration.getKeyStorePath());
		assertEquals("pwd", configuration.getKeyStorePassword());
		assertEquals("opc.tcp://nat:4840/waldot", configuration.getForceDiscoveredEndpointUrl());
		assertTrue(configuration.isForceCertificateValidator());
		assertEquals(7000, configuration.getRequestTimeout());
		assertEquals(70000, configuration.getSessionTimeout());
		assertEquals(3000, configuration.getKeepAliveTimeout());
		assertEquals(100, configuration.getReconnectInitialDelay());
		assertEquals(2000, configuration.getReconnectMaxDelay());
		assertEquals(5, configuration.getReconnectMaxAttempts());
		assertEquals(250.0, configuration.getDefaultPublishingInterval());
		assertEquals(50.0, configuration.getDefaultSamplingInterval());
		assertEquals("commands", configuration.getCommandsRootNodeId());
		assertEquals("urn:x", configuration.getWaldotNamespaceUri());
		assertEquals(AuthenticationMode.ANONYMOUS,
				WaldotClientConfigurationBuilder.from(configuration).anonymous().build().getAuthenticationMode());
	}

	@Test
	void builderValidation() {
		assertThrows(IllegalStateException.class, () -> WaldotClientConfigurationBuilder.create().endpoint(" ").build());
		assertThrows(IllegalStateException.class,
				() -> WaldotClientConfigurationBuilder.create().username("", "x").build());
		assertThrows(IllegalStateException.class,
				() -> WaldotClientConfigurationBuilder.create().certificate(List.of(), null).build());
		assertThrows(IllegalStateException.class, () -> WaldotClientConfigurationBuilder.create()
				.reconnect(Duration.ofSeconds(2), Duration.ofSeconds(1), 0).build());
		assertThrows(IllegalStateException.class, () -> WaldotClientConfigurationBuilder.create()
				.reconnect(Duration.ofSeconds(1), Duration.ofSeconds(2), -1).build());
		assertThrows(IllegalArgumentException.class,
				() -> WaldotClientConfigurationBuilder.create().requestTimeout(Duration.ZERO));
		assertThrows(IllegalArgumentException.class,
				() -> WaldotClientConfigurationBuilder.create().defaultPublishingInterval(-1));
		assertThrows(NullPointerException.class, () -> WaldotClientConfigurationBuilder.from(null));
	}

	@Test
	void clientExceptionKeepsStatus() {
		final WaldotClientException fromUa = WaldotClientException.of("read",
				new UaException(StatusCodes.Bad_NodeIdUnknown, "unknown"));
		assertEquals(StatusCodes.Bad_NodeIdUnknown, fromUa.getStatusCode().getValue());
		assertTrue(fromUa.getMessage().startsWith("read: "));
		assertSame(fromUa, WaldotClientException.of("other", fromUa));
		assertEquals(StatusCode.BAD, WaldotClientException.of("x", new IllegalStateException("y")).getStatusCode());
		assertEquals(StatusCode.BAD, new WaldotClientException("plain").getStatusCode());
		assertEquals(StatusCode.BAD, new WaldotClientException("null", null, null).getStatusCode());
	}

	@Test
	void commandResultAccessors() {
		final CommandDescriptor command = new CommandDescriptor(new NodeId(2, "information"),
				new NodeId(2, "q"), "query", "information", "run",
				List.of(new ArgumentDescriptor("command", NodeIds.String, -1, null)),
				List.of(new ArgumentDescriptor("output", NodeIds.String, -1, null),
						new ArgumentDescriptor("error", NodeIds.String, -1, null)));
		assertEquals(1, command.outputIndex("error"));
		assertEquals(-1, command.outputIndex("missing"));
		final CommandResult ok = new CommandResult(command, StatusCode.GOOD, List.of("42", ""));
		assertTrue(ok.isSuccess());
		assertEquals("42", ok.getString("output"));
		assertEquals("", ok.getString("missing"));
		assertEquals("42", ok.get(0));
		assertNull(ok.get(5));
		assertEquals(Map.of("output", "42", "error", ""), ok.asMap());
		assertSame(ok, ok.orThrow());
		final CommandResult failed = new CommandResult(command, StatusCode.GOOD, List.of("", "boom"));
		assertEquals("boom", failed.error().orElseThrow());
		assertThrows(WaldotClientException.class, failed::orThrow);
		final CommandResult bad = new CommandResult(command, new StatusCode(StatusCodes.Bad_MethodInvalid), null);
		assertFalse(bad.isSuccess());
		assertEquals(StatusCodes.Bad_MethodInvalid,
				assertThrows(WaldotClientException.class, bad::orThrow).getStatusCode().getValue());
		final List<Object> values = new ArrayList<>();
		values.add(null);
		values.add(7);
		final CommandResult unnamed = new CommandResult(null, StatusCode.GOOD, values);
		assertEquals(List.of("0", "1"), new ArrayList<>(unnamed.asMap().keySet()));
		assertNull(unnamed.asStrings().get(0));
		assertEquals("7", unnamed.asStrings().get(1));
		assertNull(unnamed.get("error"));
	}

	@Test
	void dataItemOptions() {
		final DataItemOptions options = DataItemOptions.sampling(100);
		assertEquals(100.0, options.samplingInterval());
		assertEquals(10, options.queueSize());
		assertFalse(options.needsFilter());
		final DataItemOptions absolute = options.absoluteDeadband(2.5).queue(5, false)
				.monitoringMode(MonitoringMode.Sampling);
		assertEquals(DeadbandType.Absolute, absolute.deadbandType());
		assertEquals(2.5, absolute.deadbandValue());
		assertEquals(5, absolute.queueSize());
		assertFalse(absolute.discardOldest());
		assertEquals(MonitoringMode.Sampling, absolute.monitoringMode());
		assertTrue(absolute.needsFilter());
		assertEquals(DeadbandType.Percent, options.percentDeadband(10).deadbandType());
		assertTrue(options.trigger(DataChangeTrigger.StatusValueTimestamp).needsFilter());
		assertThrows(IllegalArgumentException.class, () -> options.queue(0, true));
		assertThrows(IllegalArgumentException.class, () -> options.absoluteDeadband(-1));
		final DataItemOptions defaults = new DataItemOptions(10, 1, true, null, null, 0, null);
		assertEquals(DataChangeTrigger.StatusValue, defaults.trigger());
		assertEquals(DeadbandType.None, defaults.deadbandType());
		assertEquals(MonitoringMode.Reporting, defaults.monitoringMode());
	}

	@Test
	void elementDescriptionLookup() {
		final ElementDescription.PropertyDescription name = new ElementDescription.PropertyDescription("name",
				new NodeId(2, "v/name"), "marko", "String");
		final ElementDescription vertex = new ElementDescription(new NodeId(2, "v"), "vertex", "person", "vertex",
				"v", "v", null, 1, null, null, List.of(name), List.of(), List.of(), List.of());
		assertSame(name, vertex.property("name"));
		assertNull(vertex.property("age"));
		assertFalse(vertex.isEdge());
	}

	@Test
	void eventOptionsAndEvent() {
		final EventItemOptions defaults = EventItemOptions.defaults();
		assertEquals(EventItemOptions.DEFAULT_FIELDS, defaults.fields());
		assertTrue(defaults.eventTypes().isEmpty());
		assertEquals(Set.of(NodeIds.BaseModelChangeEventType),
				defaults.onlyTypes(NodeIds.BaseModelChangeEventType).eventTypes());
		assertEquals(List.of("Message"), defaults.withFields("Message").fields());
		assertEquals(EventItemOptions.DEFAULT_FIELDS,
				new EventItemOptions(null, null, 1, true, null).fields());
		assertThrows(IllegalArgumentException.class, () -> new EventItemOptions(null, null, 0, true, null));

		final DateTime now = DateTime.now();
		final WaldotEvent event = new WaldotEvent(new NodeId(2, "v"),
				Map.of(WaldotEvent.EVENT_ID, ByteString.of(new byte[] { 1 }), WaldotEvent.EVENT_TYPE,
						NodeIds.BaseEventType, WaldotEvent.MESSAGE, LocalizedText.english("hello"),
						WaldotEvent.SEVERITY, 300, WaldotEvent.SOURCE_NAME, "v", WaldotEvent.SOURCE_NODE,
						new NodeId(2, "v"), WaldotEvent.TIME, now));
		assertEquals(ByteString.of(new byte[] { 1 }), event.eventId());
		assertEquals(NodeIds.BaseEventType, event.eventType());
		assertEquals("hello", event.message());
		assertEquals(300, event.severity());
		assertEquals("v", event.sourceName());
		assertEquals(new NodeId(2, "v"), event.sourceNode());
		assertEquals(now, event.time());
		final WaldotEvent empty = new WaldotEvent(null, Map.of(WaldotEvent.MESSAGE, "plain"));
		assertEquals("plain", empty.message());
		assertEquals(-1, empty.severity());
		assertNull(empty.eventId());
		assertNull(empty.eventType());
		assertNull(empty.sourceName());
		assertNull(empty.sourceNode());
		assertNull(empty.time());
	}

	@Test
	void factoriesReturnTheRightVersion() throws Exception {
		final WaldOTAgentClientConfiguration configuration = WaldOTAgentClientConfiguration.getDefaultConfiguration();
		assertInstanceOf(DefaultWaldOTAgentClientConfigurationV1.class, configuration);
		try (WaldOTAgentClient v1 = WaldOTAgentClient.withConfiguration(configuration)) {
			assertInstanceOf(WaldOTAgentClientImplV1.class, v1);
			assertEquals(Status.INIT, v1.getStatus());
			assertSame(configuration, v1.getConfiguration());
		}
		try (WaldotClient v2 = WaldotClient.create(configuration)) {
			assertInstanceOf(WaldotClientImplV2.class, v2);
			assertEquals(Status.INIT, v2.getStatus());
			assertFalse(v2.isConnected());
			assertSame(configuration, v2.getConfiguration());
		}
	}

	@Test
	void graphResults() {
		assertTrue(new QueryResult("1", "").isSuccess());
		assertTrue(new QueryResult("1", null).isSuccess());
		assertFalse(new QueryResult("", "error").isSuccess());
		final GraphChangeResult result = new GraphChangeResult(3, 2, null);
		assertEquals("", result.warnings());
		assertFalse(result.hasWarnings());
		assertTrue(new GraphChangeResult(0, 0, "skipped").hasWarnings());
	}

	@Test
	void graphSnapshot() throws Exception {
		final byte[] text = "{\"id\":1}\n".getBytes(StandardCharsets.UTF_8);
		final GraphSnapshot plain = GraphSnapshot.of(text);
		assertFalse(plain.compressed());
		assertEquals("{\"id\":1}\n", plain.text());
		assertEquals(text.length, plain.size());
		assertEquals(DataExchangeProtocol.SNAPSHOT_FORMAT, plain.format());
		final GraphSnapshot compressed = GraphSnapshot.of(gzip(text));
		assertTrue(compressed.compressed());
		assertEquals("{\"id\":1}\n", compressed.text());
		// defensive copies
		final byte[] data = plain.data();
		data[0] = 'X';
		assertEquals('{', plain.data()[0]);
		assertEquals(plain, GraphSnapshot.of(text));
		assertEquals(plain.hashCode(), GraphSnapshot.of(text).hashCode());
		assertFalse(plain.equals(compressed));
		assertTrue(plain.toString().contains("size=" + text.length));
		assertEquals(0, new GraphSnapshot(null, false, 0, 0, 1).size());
	}

	@Test
	void subscriptionGroupOptions() {
		final SubscriptionGroupOptions options = SubscriptionGroupOptions.withPublishingInterval(200);
		assertEquals(200.0, options.publishingInterval());
		assertTrue(options.publishingEnabled());
		assertEquals(50, options.priority(50).priority());
		assertEquals(100.0, options.publishingInterval(100).publishingInterval());
		assertEquals(10, options.maxNotificationsPerPublish(10).maxNotificationsPerPublish());
		assertThrows(IllegalArgumentException.class, () -> options.priority(256));
		assertThrows(IllegalArgumentException.class, () -> options.publishingInterval(-1));
	}

	@Test
	void observerReceivesTheNewStatus() {
		final List<Status> received = new ArrayList<>();
		final WaldotAgentClientObserver observer = new WaldotAgentClientObserver() {
			@Override
			public void onStatusChanged(final Status status) {
				received.add(status);
			}
		};
		observer.onStatusChanged(Status.INIT, Status.RUNNING);
		assertEquals(List.of(Status.RUNNING), received);
		// default methods of the listener do nothing
		final WaldotClientListener listener = new WaldotClientListener() {
		};
		listener.onReconnectScheduled(1, Duration.ZERO);
		listener.onServiceFault(null);
		listener.onStatusChanged(Status.INIT, Status.RUNNING);
		listener.onSubscriptionGroupRecreated("g");
	}
}
