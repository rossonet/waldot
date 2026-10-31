package net.rossonet.waldot.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.DataChangeTrigger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.DeadbandType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.rossonet.waldot.cli.CliTestSupport.Result;
import net.rossonet.waldot.cli.CommandTestBase;
import net.rossonet.waldot.cli.support.ExitCodes;
import net.rossonet.waldot.client.api.WaldOTAgentClient.Status;
import net.rossonet.waldot.client.api.WaldotClientListener;
import net.rossonet.waldot.client.api.subscription.DataChangeListener;
import net.rossonet.waldot.client.api.subscription.DataItemOptions;
import net.rossonet.waldot.client.api.subscription.EventItemOptions;
import net.rossonet.waldot.client.api.subscription.MonitoredDataItem;
import net.rossonet.waldot.client.api.subscription.MonitoredEventItem;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroup;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroupOptions;
import net.rossonet.waldot.client.api.subscription.WaldotEvent;
import net.rossonet.waldot.client.api.subscription.WaldotEventListener;

/**
 * {@code subscribe}: the notifications are delivered by the listeners
 * captured from the mock subscription group.
 */
class SubscribeCommandTest extends CommandTestBase {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final DateTime TIME = new DateTime(
			Instant.parse("2026-10-31T10:00:00Z"));

	private static DataValue value(final Object value) {
		return new DataValue(new Variant(value), StatusCode.GOOD, TIME, TIME);
	}

	/** Values sent to each data listener as soon as it is subscribed. */
	private final Map<NodeId, List<DataValue>> dataValues = new LinkedHashMap<>();

	/** Events sent to each event listener as soon as it is subscribed. */
	private final Map<NodeId, List<WaldotEvent>> events = new LinkedHashMap<>();

	private SubscriptionGroup group;

	private final List<DataItemOptions> dataOptions = new ArrayList<>();

	private final List<EventItemOptions> eventOptions = new ArrayList<>();

	private Optional<StatusCode> itemStatus = Optional.empty();

	@BeforeEach
	void stubGroup() {
		group = mock(SubscriptionGroup.class);
		when(mock.subscriptions.defaultGroup()).thenReturn(group);
		when(mock.subscriptions.createGroup(anyString(),
				any(SubscriptionGroupOptions.class))).thenReturn(group);
		when(mock.graph.propertyNodeId(any(NodeId.class), anyString()))
				.thenAnswer(invocation -> new NodeId(2,
						invocation.<NodeId>getArgument(0).getIdentifier() + "/"
								+ invocation.getArgument(1)));
		when(group.subscribeData(any(NodeId.class), any(DataItemOptions.class),
				any(DataChangeListener.class))).thenAnswer(invocation -> {
					final NodeId nodeId = invocation.getArgument(0);
					dataOptions.add(invocation.getArgument(1));
					final DataChangeListener listener = invocation
							.getArgument(2);
					final MonitoredDataItem item = mock(MonitoredDataItem.class);
					when(item.status()).thenReturn(itemStatus);
					for (final DataValue value : dataValues
							.getOrDefault(nodeId, List.of())) {
						listener.onDataChange(item, value);
					}
					return item;
				});
		when(group.subscribeEvents(any(NodeId.class),
				any(EventItemOptions.class), any(WaldotEventListener.class)))
				.thenAnswer(invocation -> {
					final NodeId nodeId = invocation.getArgument(0);
					eventOptions.add(invocation.getArgument(1));
					final WaldotEventListener listener = invocation
							.getArgument(2);
					final MonitoredEventItem item = mock(
							MonitoredEventItem.class);
					when(item.status()).thenReturn(itemStatus);
					for (final WaldotEvent event : events
							.getOrDefault(nodeId, List.of())) {
						listener.onEvent(item, event);
					}
					return item;
				});
	}

	@Test
	void countStopsAfterNotifications() throws Exception {
		dataValues.put(new NodeId(2, "s1/value"),
				List.of(value(1), value(2), value(3)));
		final Result result = run("subscribe", "-p", "s1:value", "--count",
				"2", "-o", "value");
		assertEquals(0, result.exitCode(), result.err());
		// the third notification arrives after the stop and is dropped
		assertEquals(List.of("1", "2"), result.lines());
		verify(mock.subscriptions).defaultGroup();
		final DataItemOptions options = dataOptions.get(0);
		assertEquals(10, options.queueSize());
		assertTrue(options.discardOldest());
		assertEquals(DataChangeTrigger.StatusValue, options.trigger());
		assertEquals(DeadbandType.None, options.deadbandType());
		assertEquals(mock.configuration.getDefaultSamplingInterval(),
				options.samplingInterval());
	}

	@Test
	void durationEndsWithoutNotifications() {
		final long start = System.nanoTime();
		final Result result = run("subscribe", "-n", "ns=0;i=2258",
				"--duration", "300ms");
		assertEquals(0, result.exitCode(), result.err());
		assertTrue(result.out().isEmpty(), result.out());
		assertTrue(System.nanoTime() - start >= TimeUnit.MILLISECONDS
				.toNanos(300));
	}

	@Test
	void eventsInEveryFormat() throws Exception {
		final Map<String, Object> fields = new LinkedHashMap<>();
		fields.put(WaldotEvent.MESSAGE, LocalizedText.english("allarme"));
		fields.put(WaldotEvent.SEVERITY, UShort.valueOf(500));
		fields.put(WaldotEvent.TIME, TIME);
		events.put(new NodeId(2, "plant-1"),
				List.of(new WaldotEvent(new NodeId(2, "plant-1"), fields)));
		final Result text = run("subscribe", "-e", "plant-1", "--count", "1",
				"--event-fields", "Message,Severity,Time", "--event-type",
				"i=2041", "--queue", "5", "--no-discard-oldest");
		assertEquals(0, text.exitCode(), text.err());
		assertEquals(
				"2026-10-31T10:00:00Z plant-1 [evento] allarme   (severity 500)",
				text.out().trim());
		final EventItemOptions options = eventOptions.get(0);
		assertEquals(List.of("Message", "Severity", "Time"), options.fields());
		assertEquals(Set.of(new NodeId(0, 2041)), options.eventTypes());
		assertEquals(5, options.queueSize());
		assertTrue(!options.discardOldest());

		assertEquals("allarme", run("subscribe", "-e", "plant-1", "-c", "1",
				"-o", "value").out().trim());
		final JsonNode json = MAPPER.readTree(run("subscribe", "-e", "plant-1",
				"-c", "1", "-o", "jsonl").out());
		assertEquals("event", json.get("type").asText());
		assertEquals("plant-1", json.get("vertex").asText());
		assertEquals(500, json.get("fields").get("Severity").asInt());
		final String topic = run("subscribe", "-e", "plant-1", "-c", "1", "-o",
				"topic").out().trim();
		assertTrue(topic.startsWith("waldot/plant-1/events {"), topic);

		// event without a message: the fields as JSON
		events.put(new NodeId(2, "plant-2"), List.of(new WaldotEvent(
				new NodeId(2, "plant-2"), Map.of(WaldotEvent.SEVERITY, 1))));
		assertEquals("{\"Severity\":1}", run("subscribe", "-e", "plant-2",
				"-c", "1", "-o", "value").out().trim());
		final String noTime = run("subscribe", "-e", "plant-2", "-c", "1")
				.out().trim();
		assertTrue(noTime.endsWith(" plant-2 [evento]    (severity 1)"),
				noTime);
	}

	@Test
	void lostConnectionIsExitCode3() throws Exception {
		final ArgumentCaptor<WaldotClientListener> listener = ArgumentCaptor
				.forClass(WaldotClientListener.class);
		final CompletableFuture<Result> running = CompletableFuture
				.supplyAsync(() -> run("subscribe", "-p", "s1:value"));
		org.mockito.Mockito.verify(mock.client,
				org.mockito.Mockito.timeout(10_000)).addListener(listener.capture());
		org.mockito.Mockito.verify(group, org.mockito.Mockito.timeout(10_000))
				.subscribeData(any(NodeId.class), any(DataItemOptions.class),
						any(DataChangeListener.class));
		listener.getValue().onReconnectScheduled(1, Duration.ofSeconds(1));
		listener.getValue().onSubscriptionGroupRecreated("default");
		listener.getValue().onStatusChanged(Status.RUNNING,
				Status.RECONNECTING);
		listener.getValue().onStatusChanged(Status.RECONNECTING,
				Status.UNRECOVERABLE_FAULTED);
		final Result result = running.get(10, TimeUnit.SECONDS);
		assertEquals(ExitCodes.CONNECTION, result.exitCode());
		assertTrue(result.err().contains("connessione persa"), result.err());
	}

	@Test
	void optionsOfTheDataItems() {
		dataValues.put(new NodeId(2, "pump-3/speed"), List.of(value(1450)));
		final Result result = run("subscribe", "-p", "pump-3:speed",
				"--sampling", "100", "--publishing", "500", "--queue", "20",
				"--trigger", "StatusValueTimestamp", "--deadband-absolute",
				"0.5", "-c", "1", "-o", "value");
		assertEquals(0, result.exitCode(), result.err());
		final ArgumentCaptor<SubscriptionGroupOptions> groupOptions = ArgumentCaptor
				.forClass(SubscriptionGroupOptions.class);
		verify(mock.subscriptions).createGroup(eq("wotctl"),
				groupOptions.capture());
		assertEquals(500.0, groupOptions.getValue().publishingInterval());
		verify(mock.subscriptions, never()).defaultGroup();
		final DataItemOptions options = dataOptions.get(0);
		assertEquals(100.0, options.samplingInterval());
		assertEquals(20, options.queueSize());
		assertEquals(DataChangeTrigger.StatusValueTimestamp, options.trigger());
		assertEquals(DeadbandType.Absolute, options.deadbandType());
		assertEquals(0.5, options.deadbandValue());
		run("subscribe", "-p", "pump-3:speed", "--deadband-percent", "2",
				"-c", "1");
		assertEquals(DeadbandType.Percent, dataOptions.get(1).deadbandType());
		assertEquals(2.0, dataOptions.get(1).deadbandValue());
	}

	@Test
	void refusedItemIsServerError() {
		itemStatus = Optional.of(new StatusCode(StatusCodes.Bad_FilterNotAllowed));
		final Result result = run("subscribe", "-p", "s1:value",
				"--deadband-absolute", "1");
		assertEquals(ExitCodes.SERVER_ERROR, result.exitCode());
		assertTrue(result.err().contains("rifiutata dal server: Bad_FilterNotAllowed"),
				result.err());
	}

	@Test
	void usageErrors() {
		assertEquals(ExitCodes.USAGE, run("subscribe").exitCode());
		assertEquals(ExitCodes.USAGE, run("subscribe", "-p", ":x").exitCode());
		assertEquals(ExitCodes.USAGE, run("subscribe", "-p", "x:").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("subscribe", "-p", "a:b", "--deadband-absolute", "-1")
						.exitCode());
		assertEquals(ExitCodes.USAGE,
				run("subscribe", "-p", "a:b", "--duration", "0s").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("subscribe", "-p", "a:b", "--duration", "presto")
						.exitCode());
		assertEquals(ExitCodes.USAGE,
				run("subscribe", "-p", "a:b", "--queue", "0").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("subscribe", "-p", "a:b", "--publishing", "-1").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("subscribe", "-p", "a:b", "--sampling", "-2").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("subscribe", "-p", "a:b", "--trigger", "Sempre").exitCode());
		org.mockito.Mockito.verifyNoInteractions(mock.client);
	}

	@Test
	void valuesInEveryFormat() throws Exception {
		dataValues.put(new NodeId(2, "s1/value"), List.of(value(21.5)));
		dataValues.put(new NodeId(2, "s 2/value"), List.of(value("a\nb")));
		dataValues.put(new NodeId(0, 2258), List.of(new DataValue(
				new Variant(7), new StatusCode(StatusCodes.Uncertain), null)));
		final Result text = run("subscribe", "-p", "s1:value", "-p",
				"s 2:value", "-n", "i=2258", "-c", "3");
		assertEquals(0, text.exitCode(), text.err());
		final List<String> lines = text.lines();
		assertEquals("2026-10-31T10:00:00Z s1/value = 21.5", lines.get(0));
		assertEquals("2026-10-31T10:00:00Z s 2/value = \"a\\nb\"",
				lines.get(1));
		assertTrue(lines.get(2).endsWith(" i=2258 = 7   (Uncertain)"),
				lines.get(2));

		final List<String> topics = run("subscribe", "-p", "s1:value", "-p",
				"s 2:value", "-n", "i=2258", "-c", "3", "-o", "topic",
				"--topic-prefix", "impianto/").lines();
		assertEquals(List.of("impianto/s1/value 21.5",
				"impianto/s_2/value \"a\\nb\"", "impianto/2258 7"), topics);
		assertEquals("s1/value 21.5", run("subscribe", "-p", "s1:value", "-c",
				"1", "-o", "topic", "--topic-prefix", "").out().trim());

		final JsonNode json = MAPPER.readTree(run("subscribe", "-p",
				"s1:value", "-c", "1", "-o", "json").out());
		assertEquals("data", json.get("type").asText());
		assertEquals("s1", json.get("vertex").asText());
		assertEquals("value", json.get("key").asText());
		assertEquals("ns=2;s=s1/value", json.get("nodeId").asText());
		assertEquals(21.5, json.get("value").asDouble());
		assertEquals("2026-10-31T10:00:00Z", json.get("sourceTime").asText());
	}
}
