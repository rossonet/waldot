package net.rossonet.zenoh.client.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import io.zenoh.config.ZenohId;

public interface AgentControlHandler extends AutoCloseable {

	static final String EMPTY_DTML = """
			{
			    "@context": "dtmi:dtdl:context;2",
			    "@type": "Interface",
			    "@id": "dtmi:waldot:Agent;1",
			    "contents": []
			}
			""";

	default Map<String, AgentCommand> getCommands() {
		return new HashMap<>();
	}

	default Map<String, AgentConfigurationObject> getConfigurationObjects() {
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

	default void notifyTelemetry(TelemetryUpdate<?> telemetry) {
	}

	default void notifyZenohSessionCreated(ZenohId zid, List<ZenohId> routersZid, List<ZenohId> peersZid) {
	}

}
