package net.rossonet.zenoh.client.api;

public interface AgentErrorHandler {

	void notifyError(String message, Throwable exception);

}
