package net.rossonet.zenoh.api;

import java.util.Collection;
import java.util.Map;

import org.json.JSONObject;

import io.zenoh.Session;
import io.zenoh.exceptions.ZError;
import io.zenoh.pubsub.CallbackSubscriber;
import net.rossonet.zenoh.api.message.TelemetryMessage;
import net.rossonet.zenoh.exception.WaldotZenohException;

public interface WaldotZenohClient extends AutoCloseable {

	public enum Status {
		ERROR, REGISTERING, RUNNING, STOPPED
	}

	void addErrorCallback(AgentErrorHandler agentErrorHandler);

	void elaborateConfigurationObjectMessage(String topic, JSONObject payloadJson);

	void elaborateControlMessage(String topic, JSONObject payloadJson);

	void elaborateErrorMessage(String message, Throwable e);

	void elaborateInputTelemetryMessage(String topic, JSONObject payloadJson);

	void elaborateParameterMessage(String topic, JSONObject payloadJson);

	String getRuntimeUniqueId();

	Session getSession();

	Status getStatus();

	Map<String, CallbackSubscriber> getSubcribers();

	boolean isConnected();

	Collection<AgentErrorHandler> listErrorCallback();

	void propagateConfigurationsToWaldot();

	void propagateConfigurationsToWaldot(String configurationId);

	void removeErrorCallback(AgentErrorHandler agentErrorHandler);

	void scouting(long milliseconds) throws ZError, InterruptedException;

	boolean sendInternalTelemetry(TelemetryMessage<?> telemetryData);

	boolean sendTelemetry(TelemetryMessage<?> telemetryData);

	void start() throws WaldotZenohException;

	void stop() throws WaldotZenohException;
}
