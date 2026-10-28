package net.rossonet.waldot.client.api;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import net.rossonet.waldot.client.api.address.AddressSpaceBrowser;
import net.rossonet.waldot.client.api.command.WaldotCommandClient;
import net.rossonet.waldot.client.api.graph.GraphSnapshotClient;
import net.rossonet.waldot.client.api.graph.WaldotGraphClient;
import net.rossonet.waldot.client.api.subscription.WaldotSubscriptionClient;
import net.rossonet.waldot.client.v2.WaldotClientImplV2;

/**
 * WaldOT client with the extended API (second version):
 * <ul>
 * <li>connection life cycle with automatic reconnection (exponential
 * backoff) and listeners ({@link WaldotClientListener});</li>
 * <li>{@link #graph()}: structured queries and element descriptions;</li>
 * <li>{@link #commands()}: base and plugin commands;</li>
 * <li>{@link #subscriptions()}: values and events with all the OPC UA
 * parameters, modifiable at runtime;</li>
 * <li>{@link #snapshots()}: export, import and clear of the graph;</li>
 * <li>{@link #addressSpace()}: generic OPC UA browse, read, write, call.</li>
 * </ul>
 * The methods of the facades throw {@link WaldotClientException} (unchecked).
 *
 * <pre>
 * try (WaldotClient client = WaldotClient.create(configuration)) {
 * 	client.connect(Duration.ofSeconds(10));
 * 	List&lt;Vertex&gt; vertices = client.graph().vertices("g.V().hasLabel('sensor')");
 * }
 * </pre>
 */
public interface WaldotClient extends WaldOTAgentClient {

	/** Creates a client of the second version (not connected). */
	static WaldotClient create(final WaldOTAgentClientConfiguration configuration) {
		return new WaldotClientImplV2(configuration);
	}

	/** @return generic access to the OPC UA address space */
	AddressSpaceBrowser addressSpace();

	/** @return the commands of the server and of the elements */
	WaldotCommandClient commands();

	/**
	 * Stops the connection, deletes the subscription groups and releases the
	 * internal thread: the client cannot be started again.
	 */
	@Override
	void close();

	/**
	 * Starts the connection and waits for the {@link Status#RUNNING} status.
	 *
	 * @return this client
	 * @throws WaldotClientException if the client is not connected within the
	 *                               timeout or it gave up
	 */
	WaldotClient connect(Duration timeout);

	/** @return the graph of the server */
	WaldotGraphClient graph();

	/** @return the index of the WaldOT namespace on the server */
	int namespaceIndex();

	/** @return backup and restore of the graph */
	GraphSnapshotClient snapshots();

	@Override
	CompletableFuture<WaldOTAgentClient> startConnectionProcedure();

	@Override
	CompletableFuture<WaldOTAgentClient> stopConnectionProcedure();

	/** @return subscriptions to values and events */
	WaldotSubscriptionClient subscriptions();
}
