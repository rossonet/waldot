package net.rossonet.zenoh;

import static io.zenoh.Config.loadDefault;

import org.json.JSONObject;

import io.zenoh.Config;
import io.zenoh.Session;
import io.zenoh.Zenoh;
import io.zenoh.bytes.Encoding;
import io.zenoh.exceptions.ZError;
import io.zenoh.handlers.Callback;
import io.zenoh.keyexpr.KeyExpr;
import io.zenoh.pubsub.CallbackSubscriber;
import io.zenoh.pubsub.PublisherOptions;
import io.zenoh.pubsub.PutOptions;
import io.zenoh.qos.CongestionControl;
import io.zenoh.qos.Priority;
import io.zenoh.qos.Reliability;
import io.zenoh.sample.Sample;
import net.rossonet.waldot.utils.TextHelper;
import net.rossonet.zenoh.api.TelemetryData;
import net.rossonet.zenoh.api.WaldotAgentEndpoint;
import net.rossonet.zenoh.api.WaldotZenohClient;
import net.rossonet.zenoh.exception.WaldotZenohException;

public final class ZenohHelper {
	public static final String _BASE_AGENT_TOPIC = "wa";
	public static final String _BASE_WALDOT_TOPIC = "wot";
	private static final String _DISCOVERY_LASTPART = "discovery";
	public static final String _TOPIC_SEPARATOR = "/";
	public static final String ACKNOWLEDGE_COMMAND_TOPIC = "ack";
	public static final long AGENT_TIMEOUT_MS = 120000;
	public static final String AGENTS_DISCOVERY_TOPIC = _BASE_WALDOT_TOPIC + _TOPIC_SEPARATOR + _DISCOVERY_LASTPART;
	public static final String AGENTS_OPCUA_DIRECTORY = "bus/zenoh/agents";
	private static final String BASE_CONFIGURATIONS_TOPIC = "conf";
	public static final String BASE_CONTROL_TOPIC = "ctrl";
	public static final String BASE_OPCUA_DIRECTORY = "bus/zenoh";
	public static final String COMMANDS_LABEL = "c";
	public static final String CONTROL_REPLY_END_TOPIC = "reply";
	public static final String DATA_LABEL = "d";
	public static final String DTML_LABEL = "dtml";
	public static final String FLOW_START_COMMAND_TOPIC = "fstart";
	public static final String FLOW_STOP_COMMAND_TOPIC = "fstop";
	private static final String INPUT_DATA_TOPIC = "di";
	public static final String INTERNAL_TELEMETRY_TOPIC = "self";
	public static final String JOLLY_TOPIC = "**";
	public static final String KEEPALIVE_TOPIC = "live";
	public static final String MAIN_CONFIG_ID = "main";
	public static final String OBJECTS_LABEL = "o";
	private static final String PARAMETER_TOPIC = "pars";
	public static final String PING_COMMAND_TOPIC = "ping";
	public static final String PONG_COMMAND_TOPIC = "pong";
	public static final String PONG_LABEL = "pong";
	public static final String QUALITY_LABEL = "q";
	public static final String SELECTION_TAGS_LABEL = "s";
	public static final String SHUTDOWN_COMMAND_TOPIC = "shutdown";
	public static final String TELEMETRY_TOPIC = "telemetry";
	public static final String TIME_LABEL = "t";
	public static final String UNIQUE_ID_LABEL = "u";
	public static final String UPDATE_DISCOVERY_TOPIC = "update";
	public static final String VERSION_LABEL = "v";

	public static Session createClient(final Config zenohConfig, final boolean debug) throws ZError {
		if (debug) {
			System.setProperty("RUST_LOG", "zenoh=debug");
			Zenoh.initLogFromEnvOr("debug");
		} else {
			Zenoh.initLogFromEnvOr("error");
		}
		final Session session = Zenoh.open(zenohConfig);
		return session;
	}

	public static Session createClient(final JSONObject zenohClientConfig) throws ZError {
		if (zenohClientConfig == null) {
			final Config config = loadDefault();
			return createClient(config, false);
		} else {
			final Config config = Config.fromJson5(zenohClientConfig.toString());
			return createClient(config, false);
		}
	}

	public static Config createDefaultConfig() {
		return loadDefault();
	}

	public static PutOptions getAcknowledgePutOptions() {
		final PutOptions putOptions = new PutOptions();
		putOptions.setEncoding(Encoding.ZENOH_STRING);
		putOptions.setCongestionControl(CongestionControl.BLOCK);
		putOptions.setReliability(Reliability.RELIABLE);
		putOptions.setPriority(Priority.INTERACTIVE_HIGH);
		return putOptions;
	}

	public static String getAcknowLedgeTopic(final String agentUniqueId) {
		return getBaseControlTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR
				+ ZenohHelper.ACKNOWLEDGE_COMMAND_TOPIC;
	}

	public static String getAgentConfigurationsTopicsAll(final String agentUniqueId) {
		return getConfigurationsBaseTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.JOLLY_TOPIC;
	}

	public static String getAgentControlReplyTopicsSubscriptionAll(final String agentUniqueId) {
		return getBaseControlTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.JOLLY_TOPIC
				+ ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.CONTROL_REPLY_END_TOPIC;
	}

	public static String getAgentControlTopicsSubscriptionAll(final String agentUniqueId) {
		return getBaseControlTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.JOLLY_TOPIC;
	}

	public static String getAgentInputDataTopicsSubscriptionAll(final String agentUniqueId) {
		return getInputDataBaseTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.JOLLY_TOPIC;
	}

	public static String getAgentInternalTelemetryTopicsAll(final String agentUniqueId) {
		return getInternalTelemetryBaseTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.JOLLY_TOPIC;
	}

	public static String getAgentKeepAliveTopic(final String agentUniqueId) {
		return getBaseAgentTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.KEEPALIVE_TOPIC;
	}

	public static String getAgentTelemetryTopicsSubscriptionAll(final String agentUniqueId) {
		return getTelemetryBaseTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.JOLLY_TOPIC;
	}

	public static String getAgentUpdateDiscoveryTopic(final String agentUniqueId) {
		return getBaseAgentTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.UPDATE_DISCOVERY_TOPIC;
	}

	public static String getBaseAgentTopic(final String agentUniqueId) {
		return ZenohHelper._BASE_AGENT_TOPIC + ZenohHelper._TOPIC_SEPARATOR + agentUniqueId;
	}

	public static String getBaseControlTopic(final String agentUniqueId) {
		return getBaseAgentTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.BASE_CONTROL_TOPIC;
	}

	public static PutOptions getCommandPutOptions() {
		final PutOptions putOptions = new PutOptions();
		putOptions.setEncoding(Encoding.ZENOH_STRING);
		putOptions.setCongestionControl(CongestionControl.BLOCK);
		putOptions.setReliability(Reliability.RELIABLE);
		putOptions.setPriority(Priority.REALTIME);
		return putOptions;
	}

	public static String getConfigurationsBaseTopic(final String agentUniqueId) {
		return getBaseAgentTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.BASE_CONFIGURATIONS_TOPIC;
	}

	public static PutOptions getDiscoveryPutOptions() {
		final PutOptions putOptions = new PutOptions();
		putOptions.setEncoding(Encoding.ZENOH_STRING);
		putOptions.setCongestionControl(CongestionControl.BLOCK);
		putOptions.setReliability(Reliability.RELIABLE);
		putOptions.setPriority(Priority.REALTIME);
		return putOptions;
	}

	public static PublisherOptions getGlobalPublisherOptions() {
		final PublisherOptions publisherOptions = new PublisherOptions();
		publisherOptions.setEncoding(Encoding.ZENOH_STRING);
		publisherOptions.setCongestionControl(CongestionControl.BLOCK);
		publisherOptions.setReliability(Reliability.BEST_EFFORT);
		return publisherOptions;
	}

	private static String getInputDataBaseTopic(final String agentUniqueId) {
		return getBaseAgentTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.INPUT_DATA_TOPIC;
	}

	public static String getInternalTelemetryBaseTopic(final String agentUniqueId) {
		return ZenohHelper.INTERNAL_TELEMETRY_TOPIC + ZenohHelper._TOPIC_SEPARATOR + agentUniqueId;
	}

	public static String getInternalTelemetryBaseTopic(final String agentUniqueId, final TelemetryData telemetryData) {
		return ZenohHelper.INTERNAL_TELEMETRY_TOPIC + ZenohHelper._TOPIC_SEPARATOR + agentUniqueId
				+ ZenohHelper._TOPIC_SEPARATOR + telemetryData.getUniqueId();
	}

	public static PutOptions getInternalTelemetryPutOptions() {
		final PutOptions putOptions = new PutOptions();
		putOptions.setEncoding(Encoding.ZENOH_STRING);
		putOptions.setCongestionControl(CongestionControl.BLOCK);
		putOptions.setReliability(Reliability.RELIABLE);
		putOptions.setPriority(Priority.DATA);
		return putOptions;
	}

	public static String getParameterTopic(final String agentUniqueId) {
		return getBaseAgentTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.PARAMETER_TOPIC;
	}

	public static PutOptions getPingPutOptions() {
		final PutOptions putOptions = new PutOptions();
		putOptions.setEncoding(Encoding.ZENOH_STRING);
		putOptions.setCongestionControl(CongestionControl.DROP);
		putOptions.setReliability(Reliability.BEST_EFFORT);
		putOptions.setPriority(Priority.BACKGROUND);
		return putOptions;
	}

	public static String getPingTopic(final String agentUniqueId) {
		return getBaseControlTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.PING_COMMAND_TOPIC;
	}

	public static String getPongTopic(final String agentUniqueId) {
		return getBaseControlTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.PONG_COMMAND_TOPIC;
	}

	public static String getRpcCommandTopic(final String agentUniqueId, final String commandId) {
		return getBaseControlTopic(agentUniqueId) + ZenohHelper._TOPIC_SEPARATOR + TextHelper.cleanText(commandId);
	}

	public static String getTelemetryBaseTopic(final String agentUniqueId) {
		return ZenohHelper.TELEMETRY_TOPIC + ZenohHelper._TOPIC_SEPARATOR + agentUniqueId;
	}

	public static PutOptions getTelemetryPutOptions() {
		final PutOptions putOptions = new PutOptions();
		putOptions.setEncoding(Encoding.ZENOH_STRING);
		putOptions.setCongestionControl(CongestionControl.BLOCK);
		putOptions.setReliability(Reliability.RELIABLE);
		putOptions.setPriority(Priority.DATA_HIGH);
		return putOptions;
	}

	public static boolean isConnected(final Session session) throws ZError {
		return session != null && !session.isClosed() && session.info().routersZid() != null
				&& session.info().routersZid().size() > 0;
	}

	public static void sendMessage(final WaldotAgentEndpoint zenohAgent, final String topic, final JSONObject message,
			final PutOptions putOption) throws WaldotZenohException {
		if (!zenohAgent.getPublishers().containsKey(topic)) {
			try {
				zenohAgent.getPublishers().put(topic, zenohAgent.getZenohClient()
						.declarePublisher(KeyExpr.tryFrom(topic), ZenohHelper.getGlobalPublisherOptions()));
			} catch (final ZError e) {
				throw new WaldotZenohException("Error declaring publisher on topic " + topic, e);
			}
		}
		try {
			zenohAgent.getPublishers().get(topic).put(message.toString(), putOption);
		} catch (final ZError e) {
			throw new WaldotZenohException("Error sending message on topic " + topic + " message: " + message, e);
		}
	}

	private static CallbackSubscriber subscribe(final Session zenohAgent, final String topic,
			final Callback<Sample> callback) throws ZError {
		if (!isConnected(zenohAgent)) {
			throw new IllegalStateException("Zenoh client not connected");
		}
		return zenohAgent.declareSubscriber(KeyExpr.tryFrom(topic), callback);

	}

	public static void subscribeCommandTopic(final WaldotZenohClient waldotZenohClient) throws ZError {
		final String agentControlTopicsSubscriptionAll = ZenohHelper
				.getAgentControlTopicsSubscriptionAll(waldotZenohClient.getRuntimeUniqueId());
		waldotZenohClient.getSubcribers().put(agentControlTopicsSubscriptionAll,
				subscribe(waldotZenohClient.getSession(), agentControlTopicsSubscriptionAll, new Callback<Sample>() {

					@Override
					public void run(final Sample sample) {
						try {
							JSONObject payloadJson = null;
							payloadJson = new JSONObject(new String(sample.getPayload().toBytes()));
							final String topic = sample.getKeyExpr().toString();
							if (topic.endsWith(ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.CONTROL_REPLY_END_TOPIC)) {
								// ignore reply messages
								return;
							}
							waldotZenohClient.elaborateControlMessage(topic, payloadJson);
						} catch (final Throwable e) {
							waldotZenohClient.elaborateErrorMessage(
									"Error parsing mainApplicationController messagepayload: " + sample.getPayload(),
									e);
						}
					}

				}));
	}

	public static void subscribeConfigurationTopics(final WaldotZenohClient waldotZenohClient) throws ZError {
		final String agentConfigurationsTopicsAll = ZenohHelper
				.getAgentConfigurationsTopicsAll(waldotZenohClient.getRuntimeUniqueId());
		waldotZenohClient.getSubcribers().put(agentConfigurationsTopicsAll,
				subscribe(waldotZenohClient.getSession(), agentConfigurationsTopicsAll, new Callback<Sample>() {

					@Override
					public void run(final Sample sample) {
						JSONObject payloadJson = null;
						try {
							payloadJson = new JSONObject(sample.getPayload());
						} catch (final Exception e) {
							waldotZenohClient.elaborateErrorMessage(
									"Error parsing configuration object message payload: " + sample.getPayload(), e);
						}
						final String topic = sample.getKeyExpr().toString();
						waldotZenohClient.elaborateConfigurationObjectMessage(topic, payloadJson);
					}

				}));
	}

	public static void subscribeInputTelemetryTopics(final WaldotZenohClient waldotZenohClient) throws ZError {
		final String agentInputDataTopicsSubscriptionAll = ZenohHelper
				.getAgentInputDataTopicsSubscriptionAll(waldotZenohClient.getRuntimeUniqueId());
		waldotZenohClient.getSubcribers().put(agentInputDataTopicsSubscriptionAll,
				subscribe(waldotZenohClient.getSession(), agentInputDataTopicsSubscriptionAll, new Callback<Sample>() {

					@Override
					public void run(final Sample sample) {
						JSONObject payloadJson = null;
						try {
							payloadJson = new JSONObject(sample.getPayload());
						} catch (final Exception e) {
							waldotZenohClient.elaborateErrorMessage(
									"Error parsing input telemetry message payload: " + sample.getPayload(), e);
						}
						final String topic = sample.getKeyExpr().toString();
						waldotZenohClient.elaborateInputTelemetryMessage(topic, payloadJson);
					}

				}));
	}

	public static void subscribeParameterTopics(final WaldotZenohClient waldotZenohClient) throws ZError {
		final String parameterTopic = ZenohHelper.getParameterTopic(waldotZenohClient.getRuntimeUniqueId());
		waldotZenohClient.getSubcribers().put(parameterTopic,
				subscribe(waldotZenohClient.getSession(), parameterTopic, new Callback<Sample>() {

					@Override
					public void run(final Sample sample) {
						JSONObject payloadJson = null;
						try {
							payloadJson = new JSONObject(sample.getPayload());
						} catch (final Exception e) {
							waldotZenohClient.elaborateErrorMessage(
									"Error parsing parameter message payload: " + sample.getPayload(), e);
						}
						final String topic = sample.getKeyExpr().toString();
						waldotZenohClient.elaborateParameterMessage(topic, payloadJson);
					}

				}));
	}

	private static void subscribeToAgentCommandReplyTopic(final WaldotAgentEndpoint zenohAgent) throws ZError {
		final String agentControlReplyTopicsSubscriptionAll = ZenohHelper
				.getAgentControlReplyTopicsSubscriptionAll(zenohAgent.getUniqueId());
		zenohAgent.getSubcribers().put(agentControlReplyTopicsSubscriptionAll,
				subscribe(zenohAgent.getZenohClient(), agentControlReplyTopicsSubscriptionAll, new Callback<Sample>() {

					@Override
					public void run(final Sample sample) {
						zenohAgent.elaborateCommandReplyMessage(sample);

					}

				}));

	}

	private static void subscribeToAgentConfigurationsTopic(final WaldotAgentEndpoint zenohAgent) throws ZError {
		final String agentConfigurationsTopicsAll = ZenohHelper
				.getAgentConfigurationsTopicsAll(zenohAgent.getUniqueId());
		zenohAgent.getSubcribers().put(agentConfigurationsTopicsAll,
				subscribe(zenohAgent.getZenohClient(), agentConfigurationsTopicsAll, new Callback<Sample>() {
					@Override
					public void run(final Sample sample) {
						zenohAgent.elaborateConfigurationMessage(sample);
					}
				}));

	}

	private static void subscribeToAgentInternalTelemetryTopic(final WaldotAgentEndpoint zenohAgent) throws ZError {
		final String agentInternalTelemetryTopicsAll = ZenohHelper
				.getAgentInternalTelemetryTopicsAll(zenohAgent.getUniqueId());
		zenohAgent.getSubcribers().put(agentInternalTelemetryTopicsAll,
				subscribe(zenohAgent.getZenohClient(), agentInternalTelemetryTopicsAll, new Callback<Sample>() {
					@Override
					public void run(final Sample sample) {
						zenohAgent.elaborateInternalTelemetryMessage(sample);
					}
				}));

	}

	private static void subscribeToAgentParametersTopic(final WaldotAgentEndpoint zenohAgent) throws ZError {
		final String parameterTopic = ZenohHelper.getParameterTopic(zenohAgent.getUniqueId());
		zenohAgent.getSubcribers().put(parameterTopic,
				subscribe(zenohAgent.getZenohClient(), parameterTopic, new Callback<Sample>() {
					@Override
					public void run(final Sample sample) {
						zenohAgent.elaborateParameterMessage(sample);
					}
				}));

	}

	private static void subscribeToAgentPongTopic(final WaldotAgentEndpoint zenohAgent) throws ZError {
		final String pongTopic = ZenohHelper.getPongTopic(zenohAgent.getUniqueId());
		zenohAgent.getSubcribers().put(pongTopic,
				subscribe(zenohAgent.getZenohClient(), pongTopic, new Callback<Sample>() {
					@Override
					public void run(final Sample sample) {
						zenohAgent.elaboratePongMessage(sample);
					}
				}));
	}

	private static void subscribeToAgentTelemetryTopic(final WaldotAgentEndpoint zenohAgent) throws ZError {
		final String agentTelemetryTopicsSubscriptionAll = ZenohHelper
				.getAgentTelemetryTopicsSubscriptionAll(zenohAgent.getUniqueId());
		zenohAgent.getSubcribers().put(agentTelemetryTopicsSubscriptionAll,
				subscribe(zenohAgent.getZenohClient(), agentTelemetryTopicsSubscriptionAll, new Callback<Sample>() {
					@Override
					public void run(final Sample sample) {
						zenohAgent.elaborateTelemetryMessage(sample);
					}
				}));

	}

	public static void subscribeToAgentTopicsFromWaldot(final WaldotAgentEndpoint zenohAgent) throws ZError {
		subscribeToUpdateDiscoveryTopic(zenohAgent);
		subscribeToAgentCommandReplyTopic(zenohAgent);
		subscribeToAgentTelemetryTopic(zenohAgent);
		subscribeToAgentInternalTelemetryTopic(zenohAgent);
		subscribeToAgentPongTopic(zenohAgent);
		subscribeToAgentConfigurationsTopic(zenohAgent);
		subscribeToAgentParametersTopic(zenohAgent);
	}

	private static void subscribeToUpdateDiscoveryTopic(final WaldotAgentEndpoint zenohAgent) throws ZError {
		final String agentUpdateDiscoveryTopic = ZenohHelper.getAgentUpdateDiscoveryTopic(zenohAgent.getUniqueId());
		zenohAgent.getSubcribers().put(agentUpdateDiscoveryTopic,
				subscribe(zenohAgent.getZenohClient(), agentUpdateDiscoveryTopic, new Callback<Sample>() {
					@Override
					public void run(final Sample sample) {
						zenohAgent.elaborateUpdateDiscoveryMessage(sample);
					}
				}));
	}

	private ZenohHelper() {
		throw new IllegalStateException("Utility class");
	}

}
