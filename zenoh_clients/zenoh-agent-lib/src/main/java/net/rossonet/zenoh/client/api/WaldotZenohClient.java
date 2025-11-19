package net.rossonet.zenoh.client.api;

import java.util.Collection;
import java.util.Map;

import net.rossonet.zenoh.WaldotZenohException;

public interface WaldotZenohClient extends AutoCloseable {

	public enum Status {
		ERROR, REGISTERING, RUNNING, STOPPED
	}

	void addErrorCallback(AgentErrorHandler agentErrorHandler);

	Map<String, AgentCommand> getCommands();

	Map<String, AgentConfigurationObject> getConfigurationObjects();

	String getRuntimeUniqueId();

	Status getStatus();

	boolean isConnected();

	Collection<AgentErrorHandler> listErrorCallback();

	void removeErrorCallback(AgentErrorHandler agentErrorHandler);

	boolean sendInternalTelemetry(TelemetryUpdate<?> telemetryData);

	boolean sendTelemetry(TelemetryUpdate<?> telemetryData);

	void start() throws WaldotZenohException;

	void stop() throws WaldotZenohException;
}
