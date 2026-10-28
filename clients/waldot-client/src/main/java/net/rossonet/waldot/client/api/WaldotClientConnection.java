package net.rossonet.waldot.client.api;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;

import net.rossonet.waldot.client.api.WaldOTAgentClient.Status;

/**
 * Life cycle of a connection to a WaldOT server, common to every client
 * version. The status enum is {@link WaldOTAgentClient.Status} (kept there for
 * compatibility with the first client version).
 */
public interface WaldotClientConnection extends AutoCloseable {

	/** Registers a listener of the connection events. */
	void addListener(WaldotClientListener listener);

	/**
	 * Waits until the client reaches the given status.
	 *
	 * @return true if the status was reached within the timeout
	 */
	default boolean awaitStatus(final Status expected, final Duration timeout) throws InterruptedException {
		final long deadline = System.nanoTime() + timeout.toNanos();
		while (getStatus() != expected) {
			if (System.nanoTime() > deadline) {
				return false;
			}
			Thread.sleep(20);
		}
		return true;
	}

	WaldOTAgentClientConfiguration getConfiguration();

	/**
	 * @return the Milo client of the current connection (null before the
	 *         first connection)
	 */
	OpcUaClient getOpcUaClient();

	Status getStatus();

	/** @return true when the session is active */
	default boolean isConnected() {
		return getStatus() == Status.RUNNING;
	}

	/** Removes a listener registered with {@link #addListener}. */
	void removeListener(WaldotClientListener listener);

	/** Starts the connection procedure (asynchronous, with retries). */
	CompletableFuture<? extends WaldotClientConnection> startConnectionProcedure();

	/** Stops the connection: the client can be started again. */
	CompletableFuture<? extends WaldotClientConnection> stopConnectionProcedure();
}
