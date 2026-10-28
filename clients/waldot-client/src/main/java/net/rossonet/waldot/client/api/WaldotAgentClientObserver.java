package net.rossonet.waldot.client.api;

/**
 * Status observer of the first client version: receives only the new status.
 * It is a {@link WaldotClientListener}, so it can also be registered with
 * {@link WaldotClientConnection#addListener}.
 */
public interface WaldotAgentClientObserver extends WaldotClientListener {

	default void onStatusChanged(final WaldOTAgentClient.Status status) {
		// Default implementation does nothing
	}

	@Override
	default void onStatusChanged(final WaldOTAgentClient.Status previous, final WaldOTAgentClient.Status current) {
		onStatusChanged(current);
	}

}
