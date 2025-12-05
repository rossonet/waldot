package net.rossonet.zenoh.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import io.zenoh.config.ZenohId;
import net.rossonet.zenoh.api.message.TelemetryMessage;

public interface AgentControlHandler extends AutoCloseable {

	static final String EMPTY_DTML = """
			{
			    "@context": "dtmi:dtdl:context;2",
			    "@type": "Interface",
			    "@id": "dtmi:waldot:Agent;1",
			    "contents": []
			}
			""";

	default void addConfigurationObjects(String name, JSONObject configurationObject) {
	}

	default void delConfigurationObjects(String name) {
	}

	default void executeCommand(AgentCommand agentCommand, JSONObject message) {
	}

	default Map<String, AgentCommand> getCommandMetadatas() {
		return new HashMap<>();
	}

	default Map<String, AgentConfigurationObject> getConfigurationObjectMetadatas() {
		return new HashMap<>();
	}

	default Map<String, Object> getConfigurationObjects() {
		return new HashMap<>();
	}

	default JSONObject getDtdlJson() {
		final JSONObject jsonObject = new JSONObject(EMPTY_DTML);
		return jsonObject;
	}

	default int getVersionApi() {
		return 1;
	}

	default void notifyAcknowledgeCommandReceived(JSONObject message) {
	}

	default void notifyDataFlowStartCommandReceived(JSONObject message) {
	}

	default void notifyDataFlowStopCommandReceived(JSONObject message) {
	}

	default void notifyError(String message, Throwable exception) {
	}

	default void notifyTelemetry(TelemetryMessage<?> telemetry) {
	}

	default void notifyZenohSessionCreated(ZenohId zid, List<ZenohId> routersZid, List<ZenohId> peersZid) {
	}

	default void updateConfigurationObjects(String name, JSONObject configurationObject) {
	}

}
