package net.rossonet.zenoh.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.json.JSONException;
import org.json.JSONObject;

import io.zenoh.Config;
import io.zenoh.Session;
import io.zenoh.Zenoh;
import io.zenoh.config.WhatAmI;
import io.zenoh.exceptions.ZError;
import io.zenoh.handlers.Callback;
import io.zenoh.keyexpr.KeyExpr;
import io.zenoh.pubsub.CallbackSubscriber;
import io.zenoh.pubsub.Publisher;
import io.zenoh.sample.Sample;
import io.zenoh.scouting.Hello;
import io.zenoh.scouting.ScoutOptions;
import net.rossonet.zenoh.ZenohHelper;
import net.rossonet.zenoh.api.AgentControlHandler;
import net.rossonet.zenoh.api.AgentErrorHandler;
import net.rossonet.zenoh.api.WaldotZenohClient;
import net.rossonet.zenoh.api.message.RpcCommand;
import net.rossonet.zenoh.api.message.RpcConfiguration;
import net.rossonet.zenoh.api.message.TelemetryMessage;
import net.rossonet.zenoh.exception.WaldotZenohException;
import net.rossonet.zenoh.exception.ZenohSerializationException;

public class WaldotZenohClientImpl implements WaldotZenohClient {

	public static boolean debugEnabled = false;
	private static final int SHUTDOWN_EXIT_CODE = 99;
	private final int apiVersion;
	private final Set<AgentErrorHandler> errorCallbacks = new CopyOnWriteArraySet<>();
	private final List<Hello> hellos = new ArrayList<>();
	private final AgentControlHandler mainApplicationController;
	private volatile boolean needRunning = false;
	private final Map<String, Publisher> publishers = new ConcurrentHashMap<>();
	private volatile boolean registered = false;
	private final String runtimeUniqueId;
	private final Map<String, CallbackSubscriber> subcribers = new ConcurrentHashMap<>();
	private Session zenohClient;
	private Config zenohConfig;

	public WaldotZenohClientImpl(String runtimeUniqueId, AgentControlHandler mainApplicationController) {
		this.runtimeUniqueId = runtimeUniqueId;
		this.mainApplicationController = mainApplicationController;
		apiVersion = mainApplicationController.getVersionApi();
	}

	@Override
	public void addErrorCallback(AgentErrorHandler agentErrorHandler) {
		errorCallbacks.add(agentErrorHandler);
	}

	@Override
	public void close() throws Exception {
		stop();
	}

	private JSONObject createDiscoveryPayload() {
		final JSONObject discoveryMessage = new JSONObject();
		discoveryMessage.put(ZenohHelper.UNIQUE_ID_LABEL, runtimeUniqueId);
		discoveryMessage.put(ZenohHelper.TIME_LABEL, System.currentTimeMillis());
		discoveryMessage.put(ZenohHelper.DTML_LABEL, mainApplicationController.getDtdlJson());
		discoveryMessage.put(ZenohHelper.VERSION_LABEL, apiVersion);
		return discoveryMessage;
	}

	private void elaborateConfigurationObjectMessage(String topic, JSONObject payload) {
		try {
			final RpcConfiguration configuration = RpcConfiguration.fromJson(payload);
			if (!topic.equals(configuration.getConfigurationId())) {
				elaborateErrorMessage("Configuration topic and configuration id do not match: " + topic + " vs "
						+ configuration.getConfigurationId(), null);
			}
			mainApplicationController.addUpdateOrDeleteConfigurationObjects(configuration);
		} catch (final Exception e) {
			elaborateErrorMessage("Error parsing configuration object message: " + payload.toString(2), e);
		}
	}

	private void elaborateControlMessage(String topic, JSONObject controlMessage) {
		if (topic.startsWith(ZenohHelper.getBaseControlTopic(getRuntimeUniqueId()) + ZenohHelper._TOPIC_SEPARATOR)) {
			final String command = topic.substring(
					(ZenohHelper.getBaseControlTopic(getRuntimeUniqueId()) + ZenohHelper._TOPIC_SEPARATOR).length());
			if (command != null && !command.isEmpty()) {
				switch (command) {
				case ZenohHelper.PONG_COMMAND_TOPIC:
					// ignore pong messages
					break;
				case ZenohHelper.ACKNOWLEDGE_COMMAND_TOPIC:
					mainApplicationController.notifyAcknowledgeMessageReceived(controlMessage);
					registered = true;
					break;
				case ZenohHelper.SHUTDOWN_COMMAND_TOPIC:
					sendOkMessage(topic, controlMessage);
					zenohClient.close();
					System.exit(SHUTDOWN_EXIT_CODE);
					break;
				case ZenohHelper.PING_COMMAND_TOPIC:
					try {
						sendPongMessage(topic, controlMessage);
					} catch (JSONException | ZError e) {
						elaborateErrorMessage("Error sending pong message", e);
					}
					break;
				case ZenohHelper.FLOW_START_COMMAND_TOPIC:
					mainApplicationController.notifyDataFlowStartCommandReceived();
					sendOkMessage(topic, controlMessage);
					break;
				case ZenohHelper.FLOW_STOP_COMMAND_TOPIC:
					mainApplicationController.notifyDataFlowStopCommandReceived();
					sendOkMessage(topic, controlMessage);
					break;
				default:
					if (mainApplicationController.getCommandMetadatas().containsKey(command)) {
						try {
							final RpcCommand response = mainApplicationController
									.executeCommand(RpcCommand.fromJson(controlMessage));
							if (!publishers.containsKey(topic)) {
								publishers.put(topic, zenohClient.declarePublisher(KeyExpr.tryFrom(topic),
										ZenohHelper.getGlobalPublisherOptions()));
							}
							publishers.get(topic).put(response.toJson().toString(), ZenohHelper.getCommandPutOptions());
						} catch (final ZenohSerializationException | ZError e) {
							elaborateErrorMessage("Error executing command: " + command, e);
						}
					} else {
						elaborateErrorMessage("Unknown command received: " + command, null);
					}
				}
			}
		}
	}

	private void elaborateErrorMessage(String message, Throwable exception) {
		mainApplicationController.notifyError(message, exception);
		for (final AgentErrorHandler ecb : errorCallbacks) {
			ecb.notifyError(message, exception);
		}

	}

	private void elaborateInputTelemetryMessage(String topic, JSONObject payload) {
		try {
			final TelemetryMessage<?> fromJson = TelemetryMessage.fromJson(payload);
			if (!topic.endsWith(String.valueOf(fromJson.getTelemetryDataId()))) {
				elaborateErrorMessage("Input telemetry topic and telemetry id do not match: " + topic + " vs "
						+ fromJson.getTelemetryDataId(), null);
			}
			mainApplicationController.notifyInputTelemetry(fromJson);
		} catch (final ZenohSerializationException e) {
			elaborateErrorMessage("Error translating input telemetry message: " + payload.toString(2), e);
		}

	}

	private void elaborateParameterMessage(String topic, JSONObject payload) {
		try {
			final RpcConfiguration configuration = RpcConfiguration.fromJson(payload);
			mainApplicationController.updateControlParameters(configuration);
		} catch (final Exception e) {
			elaborateErrorMessage("Error parsing parameter message: " + payload.toString(2), e);
		}

	}

	public Collection<Hello> getHellos() {
		return hellos;
	}

	@Override
	public String getRuntimeUniqueId() {
		return runtimeUniqueId;
	}

	@Override
	public Status getStatus() {
		if (isConnected()) {
			if (registered) {
				return Status.RUNNING;
			} else {
				return Status.REGISTERING;
			}
		} else {
			if (!needRunning) {
				return Status.STOPPED;
			} else {
				return Status.ERROR;
			}
		}
	}

	public Config getZenohConfig() {
		return zenohConfig;
	}

	@Override
	public boolean isConnected() {
		try {
			return zenohClient != null && !zenohClient.isClosed() && zenohClient.info() != null
					&& zenohClient.info().routersZid() != null && zenohClient.info().routersZid().size() > 0;
		} catch (final Throwable e) {
			elaborateErrorMessage("during connection check", e);
		}
		return false;
	}

	@Override
	public Collection<AgentErrorHandler> listErrorCallback() {
		return errorCallbacks;
	}

	@Override
	public void removeErrorCallback(AgentErrorHandler agentErrorHandler) {
		errorCallbacks.remove(agentErrorHandler);
	}

	@Override
	public void scouting(long milliseconds) throws ZError, InterruptedException {
		final ScoutOptions scoutOptions = new ScoutOptions();
		scoutOptions.setWhatAmI(Set.of(WhatAmI.Peer, WhatAmI.Router));
		final var scout = Zenoh.scout(scoutOptions);
		final BlockingQueue<Optional<Hello>> receiver = scout.getReceiver();

		try {
			final long start = System.currentTimeMillis();
			while (System.currentTimeMillis() - start < milliseconds) {
				final Optional<Hello> wrapper = receiver.take();
				if (wrapper.isEmpty()) {
					break;
				}

				final Hello hello = wrapper.get();
				hellos.add(hello);
			}
		} finally {
			scout.stop();
		}

	}

	private void sendDiscoveryMessage() throws ZError {
		final JSONObject discoveryMessage = createDiscoveryPayload();
		if (!publishers.containsKey(ZenohHelper.AGENTS_DISCOVERY_TOPIC)) {
			publishers.put(ZenohHelper.AGENTS_DISCOVERY_TOPIC, zenohClient.declarePublisher(
					KeyExpr.tryFrom(ZenohHelper.AGENTS_DISCOVERY_TOPIC), ZenohHelper.getGlobalPublisherOptions()));
		}
		publishers.get(ZenohHelper.AGENTS_DISCOVERY_TOPIC).put(discoveryMessage.toString(),
				ZenohHelper.getDiscoveryPutOptions());
	}

	@Override
	public boolean sendInternalTelemetry(TelemetryMessage<?> telemetryData) {
		try {
			final String internalTelemetryBaseTopic = ZenohHelper.getInternalTelemetryBaseTopic(getRuntimeUniqueId(),
					mainApplicationController.getInternalTelemetryMetadata(telemetryData));
			if (!publishers.containsKey(internalTelemetryBaseTopic)) {
				publishers.put(internalTelemetryBaseTopic, zenohClient.declarePublisher(
						KeyExpr.tryFrom(internalTelemetryBaseTopic), ZenohHelper.getGlobalPublisherOptions()));
			}
			publishers.get(internalTelemetryBaseTopic).put(telemetryData.toJson().toString(),
					ZenohHelper.getInternalTelemetryPutOptions());
			return true;
		} catch (final ZError e) {
			elaborateErrorMessage("Error sending internal telemetry", e);
			return false;
		}
	}

	private void sendOkMessage(String topic, JSONObject controlMessage) {
		try {
			final RpcCommand request = RpcCommand.fromJson(controlMessage);
			final JSONObject replyPayload = new JSONObject().put(ZenohHelper.QUALITY_LABEL, "ok")
					.put(ZenohHelper.TIME_LABEL, System.currentTimeMillis());
			final RpcCommand reply = new RpcCommand(request, replyPayload);
			final String replyTopic = topic + ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.CONTROL_REPLY_END_TOPIC;
			if (!publishers.containsKey(replyTopic)) {
				publishers.put(replyTopic, zenohClient.declarePublisher(KeyExpr.tryFrom(replyTopic),
						ZenohHelper.getGlobalPublisherOptions()));
			}
			publishers.get(replyTopic).put(reply.toJson().toString(), ZenohHelper.getCommandPutOptions());
		} catch (final ZError | ZenohSerializationException e) {
			elaborateErrorMessage("Error sending OK message", e);

		}

	}

	private void sendPongMessage(String topic, JSONObject pingMessage) throws JSONException, ZError {
		final String pongTopic = ZenohHelper.getPongTopic(getRuntimeUniqueId());
		if (!publishers.containsKey(pongTopic)) {
			publishers.put(pongTopic,
					zenohClient.declarePublisher(KeyExpr.tryFrom(pongTopic), ZenohHelper.getGlobalPublisherOptions()));
		}
		publishers.get(pongTopic).put(new JSONObject().put(ZenohHelper.PONG_LABEL, System.currentTimeMillis())
				.put(ZenohHelper.DATA_LABEL, pingMessage).toString());
	}

	@Override
	public boolean sendTelemetry(TelemetryMessage<?> telemetryData) {
		try {
			final String internalTelemetryBaseTopic = ZenohHelper.getInternalTelemetryBaseTopic(getRuntimeUniqueId(),
					mainApplicationController.getInternalTelemetryMetadata(telemetryData));
			if (!publishers.containsKey(internalTelemetryBaseTopic)) {
				publishers.put(internalTelemetryBaseTopic, zenohClient.declarePublisher(
						KeyExpr.tryFrom(internalTelemetryBaseTopic), ZenohHelper.getGlobalPublisherOptions()));
			}
			publishers.get(internalTelemetryBaseTopic).put(telemetryData.toJson().toString(),
					ZenohHelper.getTelemetryPutOptions());
			return true;
		} catch (final ZError e) {
			elaborateErrorMessage("Error sending internal telemetry", e);
			return false;
		}
	}

	public void sendUpdateDiscoveryMessage() throws ZError {
		final JSONObject discoveryMessage = createDiscoveryPayload();
		final String agentUpdateDiscoveryTopic = ZenohHelper.getAgentUpdateDiscoveryTopic(getRuntimeUniqueId());
		if (!publishers.containsKey(agentUpdateDiscoveryTopic)) {
			publishers.put(agentUpdateDiscoveryTopic, zenohClient.declarePublisher(
					KeyExpr.tryFrom(agentUpdateDiscoveryTopic), ZenohHelper.getGlobalPublisherOptions()));
		}
		publishers.get(agentUpdateDiscoveryTopic).put(discoveryMessage.toString(),
				ZenohHelper.getDiscoveryPutOptions());
	}

	public void setZenohConfig(Config zenohConfig) {
		if (isConnected()) {
			throw new IllegalStateException("Cannot set Zenoh config while connected");
		}
		this.zenohConfig = zenohConfig;
	}

	@Override
	public synchronized void start() throws WaldotZenohException {
		try {
			stop();
			zenohClient = ZenohHelper.createClient();
			subscribeInputTelemetryTopics();
			subscribeConfigurationTopics();
			subscribeParameterTopics();
			subscribeCommandTopic();
			sendDiscoveryMessage();
			needRunning = true;
			// scouting(5000);
		} catch (final ZError e) {
			throw new WaldotZenohException("Error starting WaldotZenohClientImpl", e);
		}
	}

	@Override
	public synchronized void stop() throws WaldotZenohException {
		if (zenohClient != null) {
			errorCallbacks.clear();
			for (final CallbackSubscriber subscriber : subcribers.values()) {
				subscriber.close();
			}
			subcribers.clear();
			for (final Publisher publisher : publishers.values()) {
				publisher.close();
			}
			publishers.clear();
			zenohClient.close();
		}
		zenohClient = null;
		needRunning = false;
	}

	private CallbackSubscriber subscribe(String topic, Callback<Sample> handler) {
		if (!isConnected()) {
			throw new IllegalStateException("Zenoh client not connected");
		}
		try {
			final CallbackSubscriber declareSubscriber = zenohClient.declareSubscriber(KeyExpr.tryFrom(topic), handler);
			return declareSubscriber;
		} catch (final ZError e) {
			elaborateErrorMessage("Error declaring subscriber on topic " + topic, e);
			throw new RuntimeException(e);
		}
	}

	private void subscribeCommandTopic() throws ZError {
		final String agentControlTopicsSubscriptionAll = ZenohHelper
				.getAgentControlTopicsSubscriptionAll(getRuntimeUniqueId());
		subcribers.put(agentControlTopicsSubscriptionAll,
				subscribe(agentControlTopicsSubscriptionAll, new Callback<Sample>() {

					@Override
					public void run(Sample sample) {
						try {
							JSONObject payloadJson = null;
							payloadJson = new JSONObject(new String(sample.getPayload().toBytes()));
							final String topic = sample.getKeyExpr().toString();
							if (topic.endsWith(ZenohHelper._TOPIC_SEPARATOR + ZenohHelper.CONTROL_REPLY_END_TOPIC)) {
								// ignore reply messages
								return;
							}
							elaborateControlMessage(topic, payloadJson);
						} catch (final Throwable e) {
							elaborateErrorMessage(
									"Error parsing mainApplicationController messagepayload: " + sample.getPayload(),
									e);
						}
					}

				}));
	}

	private void subscribeConfigurationTopics() throws ZError {
		final String agentConfigurationsTopicsAll = ZenohHelper.getAgentConfigurationsTopicsAll(getRuntimeUniqueId());
		subcribers.put(agentConfigurationsTopicsAll, subscribe(agentConfigurationsTopicsAll, new Callback<Sample>() {

			@Override
			public void run(Sample sample) {
				JSONObject payloadJson = null;
				try {
					payloadJson = new JSONObject(sample.getPayload());
				} catch (final Exception e) {
					elaborateErrorMessage("Error parsing configuration object message payload: " + sample.getPayload(),
							e);
				}
				final String topic = sample.getKeyExpr().toString();
				elaborateConfigurationObjectMessage(topic, payloadJson);
			}

		}));
	}

	private void subscribeInputTelemetryTopics() throws ZError {
		final String agentInputDataTopicsSubscriptionAll = ZenohHelper
				.getAgentInputDataTopicsSubscriptionAll(getRuntimeUniqueId());
		subcribers.put(agentInputDataTopicsSubscriptionAll,
				subscribe(agentInputDataTopicsSubscriptionAll, new Callback<Sample>() {

					@Override
					public void run(Sample sample) {
						JSONObject payloadJson = null;
						try {
							payloadJson = new JSONObject(sample.getPayload());
						} catch (final Exception e) {
							elaborateErrorMessage(
									"Error parsing input telemetry message payload: " + sample.getPayload(), e);
						}
						final String topic = sample.getKeyExpr().toString();
						elaborateInputTelemetryMessage(topic, payloadJson);
					}

				}));
	}

	private void subscribeParameterTopics() throws ZError {
		final String parameterTopic = ZenohHelper.getParameterTopic(getRuntimeUniqueId());
		subcribers.put(parameterTopic, subscribe(parameterTopic, new Callback<Sample>() {

			@Override
			public void run(Sample sample) {
				JSONObject payloadJson = null;
				try {
					payloadJson = new JSONObject(sample.getPayload());
				} catch (final Exception e) {
					elaborateErrorMessage("Error parsing parameter message payload: " + sample.getPayload(), e);
				}
				final String topic = sample.getKeyExpr().toString();
				elaborateParameterMessage(topic, payloadJson);
			}

		}));
	}

}
