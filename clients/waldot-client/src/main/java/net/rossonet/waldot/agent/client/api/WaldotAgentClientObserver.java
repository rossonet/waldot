package net.rossonet.waldot.agent.client.api;

public interface WaldotAgentClientObserver {

	default void onStatusChanged(final WaldOTAgentClient.Status status) {
		// Default implementation does nothing
	}

}
