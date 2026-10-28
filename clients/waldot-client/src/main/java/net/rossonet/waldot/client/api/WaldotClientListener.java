package net.rossonet.waldot.client.api;

import java.time.Duration;

import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;

import net.rossonet.waldot.client.api.WaldOTAgentClient.Status;

/**
 * Listener of the connection events of a WaldOT client. Every method has an
 * empty default implementation. The callbacks run on the client internal
 * threads: they must return quickly.
 */
public interface WaldotClientListener {

	/**
	 * A new connection attempt is scheduled.
	 *
	 * @param attempt number of the failed attempts so far
	 * @param delay   time before the next attempt
	 */
	default void onReconnectScheduled(final int attempt, final Duration delay) {
		// nothing by default
	}

	/** The server returned a service fault. */
	default void onServiceFault(final ServiceFault serviceFault) {
		// nothing by default
	}

	/** The connection status changed. */
	default void onStatusChanged(final Status previous, final Status current) {
		// nothing by default
	}

	/**
	 * A subscription group was created again on the server (after a
	 * reconnection with a failed subscription transfer).
	 */
	default void onSubscriptionGroupRecreated(final String groupName) {
		// nothing by default
	}
}
