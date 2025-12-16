package net.rossonet.zenoh.api;

import java.util.Collection;

import io.zenoh.exceptions.ZError;
import net.rossonet.zenoh.api.message.TelemetryMessage;
import net.rossonet.zenoh.exception.WaldotZenohException;

public interface WaldotZenohClient extends AutoCloseable {

	public enum Status {
		ERROR, REGISTERING, RUNNING, STOPPED
	}

	void addErrorCallback(AgentErrorHandler agentErrorHandler);

	String getRuntimeUniqueId();

	Status getStatus();

	boolean isConnected();

	Collection<AgentErrorHandler> listErrorCallback();

	void removeErrorCallback(AgentErrorHandler agentErrorHandler);

	void scouting(long milliseconds) throws ZError, InterruptedException;

	boolean sendInternalTelemetry(TelemetryMessage<?> telemetryData);

	boolean sendTelemetry(TelemetryMessage<?> telemetryData);

	void start() throws WaldotZenohException;

	void stop() throws WaldotZenohException;
}
