package net.rossonet.zenoh.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import io.zenoh.config.ZenohId;
import net.rossonet.zenoh.api.message.RpcCommand;
import net.rossonet.zenoh.api.message.RpcConfiguration;
import net.rossonet.zenoh.api.message.TelemetryMessage;
import net.rossonet.zenoh.exception.ExecutionCommandException;

public interface AgentControlHandler extends AutoCloseable {

	static final String EMPTY_DTML = """
			{
			    "@context": "dtmi:dtdl:context;2",
			    "@type": "Interface",
			    "@id": "dtmi:waldot:Agent;1",
			    "contents": []
			}
			""";

	default void addUpdateOrDeleteConfigurationObjects(RpcConfiguration configuration) {
	};

	default void delConfigurationObjects(long configurationUid) {
	};

	default RpcCommand executeCommand(RpcCommand command) {
		final ExecutionCommandException ExecutionCommandException = new ExecutionCommandException(
				"Command " + command.getCommandId() + " not supported.");
		final RpcCommand response = new RpcCommand(command, ExecutionCommandException);
		return response;
	}

	default Map<String, AgentCommandMetadata> getCommandMetadatas() {
		return new HashMap<>();
	}

	default Map<String, AgentConfigurationMetadata> getConfigurationMetadatas() {
		return new HashMap<>();
	}

	default Collection<RpcConfiguration> getConfigurations() {
		return List.of();
	}

	default JSONObject getDtdlJson() {
		final JSONObject jsonObject = new JSONObject(EMPTY_DTML);
		return jsonObject;
	}

	default TelemetryData getIngressTelemetryMetadata(TelemetryMessage<?> telemetryData) {
		return null;
	}

	default Collection<TelemetryData> getIngressTelemetryMetadatas() {
		return List.of();
	}

	default TelemetryData getInternalTelemetryMetadata(TelemetryMessage<?> telemetryData) {
		return null;
	}

	default Collection<TelemetryData> getInternalTelemetryMetadatas() {
		return List.of();
	}

	default TelemetryData getTelemetryMetadata(TelemetryMessage<?> telemetryData) {
		return null;
	}

	default Collection<TelemetryData> getTelemetryMetadatas() {
		return List.of();
	}

	default int getVersionApi() {
		return 1;
	}

	default void notifyAcknowledgeMessageReceived(JSONObject message) {
	}

	default void notifyDataFlowStartCommandReceived() {
	}

	default void notifyDataFlowStopCommandReceived() {
	}

	default void notifyError(String message, Throwable exception) {
	}

	default void notifyInputTelemetry(TelemetryMessage<?> telemetry) {
	}

	default void notifyZenohSessionCreated(ZenohId zid, List<ZenohId> routersZid, List<ZenohId> peersZid) {
	}

	default void updateControlParameters(RpcConfiguration configuration) {
	};

}
