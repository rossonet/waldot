package net.rossonet.waldot.client.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import org.apache.tinkerpop.gremlin.structure.T;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.client.api.WaldOTAgentClient.Status;
import net.rossonet.waldot.client.api.WaldotClient;
import net.rossonet.waldot.client.api.WaldotClientConfigurationBuilder;
import net.rossonet.waldot.client.api.WaldotClientListener;
import net.rossonet.waldot.client.api.subscription.DataItemOptions;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroup;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroupListener;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroupOptions;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Restart of the server: the client goes to RECONNECTING, the Milo session
 * is restored, the subscription cannot be transferred to the new server and
 * the group is created again with its items.
 */
class ServerRestartIntegrationTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(60);

	private static void await(final String what, final BooleanSupplier condition) throws InterruptedException {
		final long deadline = System.nanoTime() + TIMEOUT.toNanos();
		while (!condition.getAsBoolean()) {
			if (System.nanoTime() > deadline) {
				throw new AssertionError("timeout waiting for " + what);
			}
			Thread.sleep(50);
		}
	}

	private WaldotClient client;
	private WaldotGraph graph;

	@AfterEach
	void cleanup() {
		if (client != null) {
			client.close();
		}
		WaldotTestEnvironment.cleanup(graph, null);
	}

	private WaldotGraph startServer(final int age) throws Exception {
		WaldotTestEnvironment.awaitOpcPortReleased();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		final WaldotGraph started = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy());
		started.addVertex(T.id, "x-sensor", T.label, "sensor", "value", age);
		return started;
	}

	@Test
	void subscriptionsSurviveAServerRestart() throws Exception {
		graph = startServer(1);
		final List<Status> statuses = new CopyOnWriteArrayList<>();
		final List<String> recreated = new CopyOnWriteArrayList<>();
		client = WaldotClient.create(WaldotClientConfigurationBuilder.create().agentName("restart-test")
				.keepAliveTimeout(Duration.ofSeconds(1)).requestTimeout(Duration.ofSeconds(3))
				.reconnect(Duration.ofMillis(100), Duration.ofSeconds(1), 0).build());
		client.addListener(new WaldotClientListener() {
			@Override
			public void onStatusChanged(final Status previous, final Status current) {
				statuses.add(current);
			}

			@Override
			public void onSubscriptionGroupRecreated(final String groupName) {
				recreated.add(groupName);
			}
		});
		client.connect(Duration.ofSeconds(20));
		final List<String> groupEvents = new CopyOnWriteArrayList<>();
		final SubscriptionGroup group = client.subscriptions().createGroup("restart",
				SubscriptionGroupOptions.withPublishingInterval(50));
		group.addListener(new SubscriptionGroupListener() {
			@Override
			public void onRecreated(final SubscriptionGroup g) {
				groupEvents.add("recreated");
			}

			@Override
			public void onTransferFailed(final SubscriptionGroup g,
					final org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode status) {
				groupEvents.add("transferFailed");
			}
		});
		final List<Integer> values = new CopyOnWriteArrayList<>();
		final NodeId value = client.graph().propertyNodeId(client.addressSpace().waldotNodeId("x-sensor"), "value");
		final var dataItem = group.subscribeData(value, DataItemOptions.sampling(10),
				(item, dataValue) -> values.add(((Number) dataValue.getValue().getValue()).intValue()));
		await("first value", () -> values.contains(1));

		// restart of the server on the same port
		graph.getWaldotNamespace().close();
		await("session lost", () -> client.getStatus() == Status.RECONNECTING);
		graph = startServer(2);
		await("session restored", () -> client.getStatus() == Status.RUNNING);
		await("group recreated", () -> recreated.contains("restart"));
		assertTrue(groupEvents.contains("transferFailed"), groupEvents.toString());
		assertTrue(groupEvents.contains("recreated"), groupEvents.toString());
		// the vertex is created after the start of the new server: the item
		// refused with Bad_NodeIdUnknown is created again by the client
		await("value of the new server", () -> values.contains(2));
		assertTrue(dataItem.status().orElseThrow().isGood());
		final DataValue read = client.addressSpace().readValue(value);
		assertEquals(2, ((Number) read.getValue().getValue()).intValue());
		assertTrue(statuses.contains(Status.RECONNECTING), statuses.toString());
	}
}
