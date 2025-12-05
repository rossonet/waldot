package net.rossonet.zenoh.api;

import java.util.Collection;
import java.util.Map;

import net.rossonet.zenoh.WaldotZenohException;
import net.rossonet.zenoh.api.message.TelemetryMessage;

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

	boolean sendInternalTelemetry(TelemetryMessage<?> telemetryData);

	boolean sendTelemetry(TelemetryMessage<?> telemetryData);

	void start() throws WaldotZenohException;

	void stop() throws WaldotZenohException;
}
