package net.rossonet.zenoh;

import static io.zenoh.Config.loadDefault;

import io.zenoh.Config;
import io.zenoh.Session;
import io.zenoh.Zenoh;
import io.zenoh.bytes.Encoding;
import io.zenoh.exceptions.ZError;
import io.zenoh.pubsub.PublisherOptions;
import io.zenoh.pubsub.PutOptions;
import io.zenoh.qos.CongestionControl;
import io.zenoh.qos.Priority;
import io.zenoh.qos.Reliability;

public class ZenohHelper {
	public static final String _BASE_AGENT_TOPIC = "agents";
	public static final String _TOPIC_SEPARATOR = "/";
	public static final String ACKNOWLEDGE_COMMAND_TOPIC = "acknowledge";
	public static final String AGENTS_DISCOVERY_TOPIC = _BASE_AGENT_TOPIC + _TOPIC_SEPARATOR + "discovery";
	public static final String AGENTS_OPCUA_DIRECTORY = "bus/zenoh/agents";
	private static final String BASE_CONFIGURATIONS_TOPIC = "configurations";
	public static final String BASE_CONTROL_TOPIC = "ctrl";
	public static final String BASE_OPCUA_DIRECTORY = "bus/zenoh";
	public static final String COMMANDS_LABEL = "c";
	public static final String DATA_LABEL = "d";
	public static final String DTML_LABEL = "dtml";
	public static final String FLOW_START_COMMAND_TOPIC = "fstart";
	public static final String FLOW_STOP_COMMAND_TOPIC = "fstop";
	private static final String INPUT_DATA_TOPIC = "di";
	public static final String INTERNAL_TELEMETRY_TOPIC = "internal";
	public static final String JOLLY_TOPIC = "**";
	public static final String KEEPALIVE_TOPIC = "live/status";
	public static final String OBJECTS_LABEL = "o";
	public static final String PING_COMMAND_TOPIC = "ping";
	public static final String PONG_LABEL = "pong";
	public static final String QUALITY_LABEL = "q";
	public static final String SELECTION_TAGS_LABEL = "s";
	public static final String SHUTDOWN_COMMAND_TOPIC = "shutdown";
	public static final String TELEMETRY_TOPIC = "telemetry";
	public static final String TIME_LABEL = "t";
	public static final String UNIQUE_ID_LABEL = "u";
	public static final String UPDATE_DISCOVERY_TOPIC = "live/update";
	public static final String VERSION_LABEL = "v";

	public static Session createClient() throws ZError {
		Zenoh.initLogFromEnvOr("error");
		final Config config = loadDefault();
		final Session session = Zenoh.open(config);
		return session;
	}

	public static String getAcknowLedgeTopic(String agentUniqueId) {
		return getBaseControlTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR
				+ ZenohHelper.ACKNOWLEDGE_COMMAND_TOPIC;
	}

	public static String getAgentConfigurationsTopicsAll(String agentUniqueId) {
		return getConfigurationsBaseTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.JOLLY_TOPIC;
	}

	public static String getAgentControlTopicsSubscriptionAll(String agentUniqueId) {
		return getBaseControlTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.JOLLY_TOPIC;
	}

	public static String getAgentInputDataTopicsSubscriptionAll(String agentUniqueId) {
		return getInputDataBaseTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.JOLLY_TOPIC;
	}

	public static String getAgentInternalTelemetryTopicsAll(String agentUniqueId) {
		return getInternalTelemetryBaseTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.JOLLY_TOPIC;
	}

	public static String getAgentKeepAliveTopic(String agentUniqueId) {
		return getBaseAgentTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.KEEPALIVE_TOPIC;
	}

	public static String getAgentTelemetryTopicsSubscriptionAll(String agentUniqueId) {
		return getTelemetryBaseTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.JOLLY_TOPIC;
	}

	public static String getAgentUpdateDiscoveryTopic(String agentUniqueId) {
		return getBaseAgentTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.UPDATE_DISCOVERY_TOPIC;
	}

	public static String getBaseAgentTopic(String agentUniqueId) {
		return ZenohHelper._BASE_AGENT_TOPIC + ZenohHelper._TOPIC_SEPARATOR + agentUniqueId;
	}

	public static String getBaseControlTopic(String agentUniqueId) {
		return getBaseAgentTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.BASE_CONTROL_TOPIC;
	}

	private static String getConfigurationsBaseTopic(String agentUniqueId) {
		return getBaseAgentTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.BASE_CONFIGURATIONS_TOPIC;
	}

	public static PutOptions getDiscoveryPutOptions() {
		final PutOptions putOptions = new PutOptions();
		putOptions.setEncoding(Encoding.ZENOH_STRING);
		putOptions.setCongestionControl(CongestionControl.BLOCK);
		putOptions.setReliability(Reliability.RELIABLE);
		putOptions.setPriority(Priority.INTERACTIVE_LOW);
		return putOptions;
	}

	public static PublisherOptions getGlobalPublisherOptions() {
		final PublisherOptions publisherOptions = new PublisherOptions();
		publisherOptions.setEncoding(Encoding.ZENOH_STRING);
		publisherOptions.setCongestionControl(CongestionControl.BLOCK);
		publisherOptions.setReliability(Reliability.RELIABLE);
		return publisherOptions;
	}

	private static String getInputDataBaseTopic(String agentUniqueId) {
		return getBaseAgentTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.INPUT_DATA_TOPIC;
	}

	public static String getInternalTelemetryBaseTopic(String agentUniqueId) {
		return getBaseAgentTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.INTERNAL_TELEMETRY_TOPIC;
	}

	public static String getTelemetryBaseTopic(String agentUniqueId) {
		return getBaseAgentTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.TELEMETRY_TOPIC;
	}

	private ZenohHelper() {
		throw new IllegalStateException("Utility class");
	}

}
