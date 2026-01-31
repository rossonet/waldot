package net.rossonet.zenoh.api;

public interface AgentErrorHandler {

	void notifyError(String message, Throwable exception);

}
