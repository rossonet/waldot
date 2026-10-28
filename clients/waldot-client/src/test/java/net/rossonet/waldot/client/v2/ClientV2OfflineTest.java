package net.rossonet.waldot.client.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.DeadbandType;
import org.eclipse.milo.opcua.stack.core.types.structured.DataChangeFilter;
import org.eclipse.milo.opcua.stack.core.types.structured.EventFilter;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.client.api.WaldOTAgentClient;
import net.rossonet.waldot.client.api.WaldOTAgentClient.Status;
import net.rossonet.waldot.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.client.api.WaldotClient;
import net.rossonet.waldot.client.api.WaldotClientConfigurationBuilder;
import net.rossonet.waldot.client.api.WaldotClientException;
import net.rossonet.waldot.client.api.WaldotClientListener;
import net.rossonet.waldot.client.api.subscription.DataItemOptions;
import net.rossonet.waldot.client.api.subscription.EventItemOptions;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroup;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroupOptions;
import net.rossonet.waldot.client.api.subscription.WaldotEvent;

/**
 * Client v2 without a server: filters, event decoding, connection failures,
 * backoff callbacks and life cycle.
 */
class ClientV2OfflineTest {

	private static int freePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	private static WaldOTAgentClientConfiguration unreachable(final int attempts) throws IOException {
		return WaldotClientConfigurationBuilder.create().endpoint("opc.tcp://127.0.0.1:" + freePort() + "/waldot")
				.requestTimeout(Duration.ofSeconds(2)).reconnect(Duration.ofMillis(20), Duration.ofMillis(50), attempts)
				.build();
	}

	@Test
	void dataChangeFilterFollowsTheOptions() {
		assertNull(DataItemV2.filter(DataItemOptions.sampling(10)));
		final DataChangeFilter filter = DataItemV2.filter(DataItemOptions.sampling(10).absoluteDeadband(1.5));
		assertEquals(DeadbandType.Absolute.getValue(), filter.getDeadbandType().intValue());
		assertEquals(1.5, filter.getDeadbandValue());
		assertEquals(DeadbandType.Percent.getValue(),
				DataItemV2.filter(DataItemOptions.sampling(10).percentDeadband(5)).getDeadbandType().intValue());
	}

	@Test
	void eventFilterAndDecoding() {
		final EventItemOptions options = EventItemOptions.defaults().withFields(WaldotEvent.EVENT_TYPE,
				WaldotEvent.MESSAGE, WaldotEvent.SEVERITY);
		final EventFilter filter = EventItemV2.filter(options);
		assertEquals(3, filter.getSelectClauses().length);
		assertEquals(WaldotEvent.MESSAGE, filter.getSelectClauses()[1].getBrowsePath()[0].getName());
		final Variant[] values = { new Variant(NodeIds.BaseEventType), new Variant(LocalizedText.english("m")) };
		final WaldotEvent event = EventItemV2.toEvent(new NodeId(2, "v"), options, values);
		assertEquals("m", event.message());
		assertEquals(NodeIds.BaseEventType, event.eventType());
		assertNull(event.field(WaldotEvent.SEVERITY));
		assertEquals(new NodeId(2, "v"), event.emitter());
		assertNotNull(EventItemV2.toEvent(null, options.onlyTypes(NodeIds.BaseEventType), values));
		assertNull(EventItemV2.toEvent(null, options.onlyTypes(NodeIds.BaseModelChangeEventType), values));
		assertNull(EventItemV2.toEvent(null, options.onlyTypes(NodeIds.BaseEventType), null));
	}

	@Test
	void giveUpAfterTheMaximumAttempts() throws Exception {
		final List<Status> statuses = new CopyOnWriteArrayList<>();
		final List<Duration> delays = new CopyOnWriteArrayList<>();
		try (WaldotClient client = WaldotClient.create(unreachable(3))) {
			client.addListener(new WaldotClientListener() {
				@Override
				public void onReconnectScheduled(final int attempt, final Duration delay) {
					delays.add(delay);
				}

				@Override
				public void onStatusChanged(final Status previous, final Status current) {
					statuses.add(current);
				}
			});
			final CompletableFuture<WaldOTAgentClient> future = client.startConnectionProcedure();
			final ExecutionException failure = assertThrows(ExecutionException.class,
					() -> future.get(30, TimeUnit.SECONDS));
			assertTrue(failure.getCause() instanceof WaldotClientException);
			assertEquals(Status.UNRECOVERABLE_FAULTED, client.getStatus());
			assertEquals(List.of(Duration.ofMillis(20), Duration.ofMillis(40)), delays);
			assertEquals(Status.FAULTED, statuses.get(0));
			assertTrue(statuses.contains(Status.FAULTED));
			assertEquals(Status.UNRECOVERABLE_FAULTED, statuses.get(statuses.size() - 1));
			assertNull(client.getOpcUaClient());
			assertEquals(2, client.namespaceIndex());
			// facades refuse to work without a connection
			final WaldotClientException notConnected = assertThrows(WaldotClientException.class,
					() -> client.graph().queryJson("1"));
			assertEquals(StatusCodes.Bad_NotConnected, notConnected.getStatusCode().getValue());
			assertThrows(WaldotClientException.class, () -> client.addressSpace().readValue(NodeIds.Server));
			assertThrows(WaldotClientException.class, () -> client.commands().globalCommands());
			assertThrows(WaldotClientException.class, () -> client.snapshots().clearGraph());
			// base operations keep the checked exception of the first version
			assertThrows(Exception.class, () -> client.getServerInfo());
		}
	}

	@Test
	void connectTimesOutAndStopCompletesTheFuture() throws Exception {
		try (WaldotClient client = WaldotClient.create(unreachable(0))) {
			final WaldotClientException timeout = assertThrows(WaldotClientException.class,
					() -> client.connect(Duration.ofMillis(300)));
			assertEquals(StatusCodes.Bad_Timeout, timeout.getStatusCode().getValue());
			final CompletableFuture<WaldOTAgentClient> pending = client.startConnectionProcedure();
			assertSame(pending, client.startConnectionProcedure());
			client.stopConnectionProcedure().get(5, TimeUnit.SECONDS);
			assertEquals(Status.STOPPED, client.getStatus());
			assertTrue(pending.isCompletedExceptionally());
			assertThrows(WaldotClientException.class, () -> client.connect(Duration.ofMillis(100)));
		}
	}

	@Test
	void closedClientCannotStart() throws Exception {
		final WaldotClient client = WaldotClient.create(unreachable(1));
		client.close();
		assertEquals(Status.CLOSED, client.getStatus());
		assertTrue(client.startConnectionProcedure().isCompletedExceptionally());
		client.close();
		assertEquals(Status.CLOSED, client.getStatus());
	}

	@Test
	void groupsAreKeptWhileDisconnected() throws Exception {
		try (WaldotClient client = WaldotClient.create(unreachable(1))) {
			final SubscriptionGroup group = client.subscriptions().createGroup("g",
					SubscriptionGroupOptions.withPublishingInterval(100));
			assertThrows(IllegalArgumentException.class, () -> client.subscriptions().createGroup("g", null));
			assertThrows(IllegalArgumentException.class, () -> client.subscriptions().createGroup(" ", null));
			assertTrue(group.subscriptionId().isEmpty());
			assertTrue(group.revisedPublishingInterval().isEmpty());
			assertTrue(group.isPublishingEnabled());
			final var item = group.subscribeData(new NodeId(2, "x"), null, (i, v) -> {
			});
			assertEquals(1, group.dataItems().size());
			assertTrue(item.isActive());
			assertTrue(item.revisedSamplingInterval().isEmpty());
			assertTrue(item.status().isEmpty());
			assertTrue(item.lastValue().isEmpty());
			item.modify(DataItemOptions.sampling(5));
			assertEquals(5.0, item.options().samplingInterval());
			final var events = group.subscribeEvents(new NodeId(2, "x"), null, (i, e) -> {
			});
			assertEquals(1, group.eventItems().size());
			events.modify(EventItemOptions.defaults().withFields(WaldotEvent.MESSAGE));
			assertEquals(List.of(WaldotEvent.MESSAGE), events.options().fields());
			group.setPublishingEnabled(false);
			assertFalse(group.isPublishingEnabled());
			item.close();
			assertFalse(item.isActive());
			assertTrue(group.dataItems().isEmpty());
			assertSame(group, client.subscriptions().group("g").orElseThrow());
			final SubscriptionGroup defaultGroup = client.subscriptions().defaultGroup();
			assertSame(defaultGroup, client.subscriptions().defaultGroup());
			assertEquals(2, client.subscriptions().groups().size());
			group.close();
			assertFalse(group.isActive());
			assertThrows(WaldotClientException.class, () -> group.subscribeData(new NodeId(2, "y"), null, null));
			assertEquals(1, client.subscriptions().groups().size());
		}
	}
}
