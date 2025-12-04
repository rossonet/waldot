package net.rossonet.zenoh.client;

import static io.zenoh.Config.loadDefault;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import io.zenoh.Config;
import io.zenoh.Session;
import io.zenoh.exceptions.ZError;
import io.zenoh.handlers.Callback;
import io.zenoh.keyexpr.KeyExpr;
import io.zenoh.pubsub.Publisher;
import io.zenoh.sample.Sample;
import io.zenoh.session.SessionInfo;
import net.rossonet.zenoh.WaldotZenohException;
import net.rossonet.zenoh.ZenohHelper;
import net.rossonet.zenoh.client.api.AgentCommand;
import net.rossonet.zenoh.client.api.AgentConfigurationObject;
import net.rossonet.zenoh.client.api.AgentControlHandler;
import net.rossonet.zenoh.client.api.AgentErrorHandler;
import net.rossonet.zenoh.client.api.TelemetryUpdate;
import net.rossonet.zenoh.client.api.WaldotZenohClient;

public class WaldotZenohClientImpl implements WaldotZenohClient {

	public static boolean debugEnabled = false;
	private final int apiVersion;
	private final Callback<Sample> configurationCallback = new Callback<Sample>() {

		@Override
		public void run(Sample sample) {
			JSONObject payloadJson = null;
			try {
				payloadJson = new JSONObject(sample.getPayload());
			} catch (final Exception e) {
				elaborateErrorMessage("Error parsing configuration object message payload: " + sample.getPayload(), e);
			}
			final String topic = sample.getKeyExpr().toString();
			elaborateConfigurationObjectMessage(topic, payloadJson);
		}

	};
	private final Map<String, AgentConfigurationObject> configurationObjects = new HashMap<>();
	private final AgentControlHandler control;
	private final Callback<Sample> controlCallback = new Callback<Sample>() {

		@Override
		public void run(Sample sample) {
			JSONObject payloadJson = null;
			try {
				payloadJson = new JSONObject(sample.getPayload());
			} catch (final Exception e) {
				elaborateErrorMessage("Error parsing control message payload: " + sample.getPayload(), e);
			}
			final String topic = sample.getKeyExpr().toString();
			elaborateControlMessage(topic, payloadJson);
		}

	};

	private final Set<AgentErrorHandler> errorCallbacks = new HashSet<>();
	private final Callback<Sample> inputTelemetryCallback = new Callback<Sample>() {

		@Override
		public void run(Sample sample) {
			JSONObject payloadJson = null;
			try {
				payloadJson = new JSONObject(sample.getPayload());
			} catch (final Exception e) {
				elaborateErrorMessage("Error parsing input telemetry message payload: " + sample.getPayload(), e);
			}
			final String topic = sample.getKeyExpr().toString();
			elaborateInputTelemetryMessage(topic, payloadJson);
		}

	};
	private boolean needRunning = false;
	private final Callback<Sample> parameterCallback = new Callback<Sample>() {

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

	};
	private final Map<String, AgentCommand> registerCommands = new HashMap<>();
	private boolean registered = false;
	private final String runtimeUniqueId;
	private Session zenohClient;
	private Config zenohConfig;

	public WaldotZenohClientImpl(String runtimeUniqueId, AgentControlHandler control) {
		this.runtimeUniqueId = runtimeUniqueId;
		this.control = control;
		apiVersion = control.getVersionApi();
		registerCommands.putAll(control.getCommandMetadatas());
		configurationObjects.putAll(control.getConfigurationObjectMetadatas());
	}

	@Override
	public void addErrorCallback(AgentErrorHandler agentErrorHandler) {
		errorCallbacks.add(agentErrorHandler);
	}

	@Override
	public void close() throws Exception {
		stop();
	}

	private Session createClient() throws ZError {
		if (zenohConfig == null) {
			zenohConfig = loadDefault();
		}

		final Session session = ZenohHelper.createClient(zenohConfig, debugEnabled);
		final SessionInfo info = session.info();
		control.notifyZenohSessionCreated(info.zid(), info.routersZid(), info.peersZid());
		return session;
	}

	private JSONObject createDiscoveryPayload() {
		final JSONObject discoveryMessage = new JSONObject();
		discoveryMessage.put(ZenohHelper.UNIQUE_ID_LABEL, runtimeUniqueId);
		discoveryMessage.put(ZenohHelper.TIME_LABEL, System.currentTimeMillis());
		discoveryMessage.put(ZenohHelper.DTML_LABEL, control.getDtdlJson());
		discoveryMessage.put(ZenohHelper.VERSION_LABEL, apiVersion);
		return discoveryMessage;
	}

	protected void elaborateConfigurationObjectMessage(String topic, JSONObject payload) {
		final String name;
		// TODO elabora il messaggio di configurazione in ingresso
		// control.addConfigurationObjects(name,payload);
		// control.delConfigurationObjects(name);
		// control.updateConfigurationObjects(name,payload);
	}

	private void elaborateControlMessage(String topic, JSONObject message) {
		if (topic.startsWith(ZenohHelper.getBaseControlTopic(getRuntimeUniqueId()) + ZenohHelper._TOPIC_SEPARATOR)) {
			final String command = topic.substring(
					(ZenohHelper.getBaseControlTopic(getRuntimeUniqueId()) + ZenohHelper._TOPIC_SEPARATOR).length());
			if (command != null && !command.isEmpty()) {
				switch (command) {
				case ZenohHelper.ACKNOWLEDGE_COMMAND_TOPIC:
					control.notifyAcknowledgeCommandReceived(message);
					registered = true;
					break;
				case ZenohHelper.SHUTDOWN_COMMAND_TOPIC:
					zenohClient.close();
					System.exit(0);
					break;
				case ZenohHelper.PING_COMMAND_TOPIC:
					try {
						sendPongMessage(topic);
					} catch (JSONException | ZError e) {
						elaborateErrorMessage("Error sending pong message", e);
					}
					break;
				case ZenohHelper.FLOW_START_COMMAND_TOPIC:
					control.notifyDataFlowStartCommandReceived(message);
					break;
				case ZenohHelper.FLOW_STOP_COMMAND_TOPIC:
					control.notifyDataFlowStopCommandReceived(message);
					break;
				default:
					if (registerCommands.containsKey(command)) {
						control.executeCommand(registerCommands.get(command), message);
					} else {
						elaborateErrorMessage("Unknown command received: " + command, null);
					}
				}
			}
		}
	}

	private void elaborateErrorMessage(String message, Throwable exception) {
		control.notifyError(message, exception);
		for (final AgentErrorHandler ecb : errorCallbacks) {
			ecb.notifyError(message, exception);
		}

	}

	protected void elaborateInputTelemetryMessage(String topic, JSONObject payload) {
		// TODO elabora il messaggio di telemetria in ingresso

	}

	protected void elaborateParameterMessage(String topic, JSONObject payload) {
		// TODO elabora il messaggio di parametro in ingresso

	}

	@Override
	public Map<String, AgentCommand> getCommands() {
		return registerCommands;
	}

	@Override
	public Map<String, AgentConfigurationObject> getConfigurationObjects() {
		return configurationObjects;
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
			return zenohClient != null && !zenohClient.isClosed() && zenohClient.info().routersZid() != null
					&& zenohClient.info().routersZid().size() > 0;
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

	private void sendDiscoveryMessage() throws ZError {
		final JSONObject discoveryMessage = createDiscoveryPayload();
		final Publisher publisher = zenohClient.declarePublisher(KeyExpr.tryFrom(ZenohHelper.AGENTS_DISCOVERY_TOPIC),
				ZenohHelper.getGlobalPublisherOptions());
		publisher.put(discoveryMessage.toString(), ZenohHelper.getDiscoveryPutOptions());
	}

	@Override
	public boolean sendInternalTelemetry(TelemetryUpdate<?> telemetryData) {
		// TODO invia la telemetria relativa agli stati interni dell'agente
		return false;
	}

	private void sendPongMessage(String topic) throws JSONException, ZError {
		zenohClient.declarePublisher(KeyExpr.tryFrom(topic), ZenohHelper.getGlobalPublisherOptions())
				.put(new JSONObject().put(ZenohHelper.PONG_LABEL, System.currentTimeMillis()).toString());
	}

	@Override
	public boolean sendTelemetry(TelemetryUpdate<?> telemetryData) {
		// TODO invia la telemetria relativa ai dati di processo
		return false;
	}

	private void sendUpdateDiscoveryMessage() throws ZError {
		final JSONObject discoveryMessage = createDiscoveryPayload();
		final Publisher publisher = zenohClient.declarePublisher(
				KeyExpr.tryFrom(ZenohHelper.getAgentUpdateDiscoveryTopic(getRuntimeUniqueId())),
				ZenohHelper.getGlobalPublisherOptions());
		publisher.put(discoveryMessage.toString(), ZenohHelper.getDiscoveryPutOptions());
	}

	public void setZenohConfig(Config zenohConfig) {
		this.zenohConfig = zenohConfig;
	}

	@Override
	public void start() throws WaldotZenohException {
		try {
			stop();
			zenohClient = createClient();
			sendDiscoveryMessage();
			subscribeCommandsTopic();
			subscribeInputDataTopics();
			subscribeConfigurationTopics();
			subscribeParameterTopics();
			needRunning = true;
		} catch (final ZError e) {
			throw new WaldotZenohException("Error starting WaldotZenohClientImpl", e);
		}
	}

	@Override
	public void stop() throws WaldotZenohException {
		if (zenohClient != null) {
			zenohClient.close();
		}
		zenohClient = null;
		needRunning = false;
	}

	private void subscribeCommandsTopic() throws ZError {
		zenohClient.declareSubscriber(
				KeyExpr.tryFrom(ZenohHelper.getAgentControlTopicsSubscriptionAll(getRuntimeUniqueId())),
				controlCallback);
	}

	private void subscribeConfigurationTopics() throws ZError {
		zenohClient.declareSubscriber(
				KeyExpr.tryFrom(ZenohHelper.getAgentConfigurationsTopicsAll(getRuntimeUniqueId())),
				configurationCallback);

	}

	private void subscribeInputDataTopics() throws ZError {
		zenohClient.declareSubscriber(
				KeyExpr.tryFrom(ZenohHelper.getAgentInputDataTopicsSubscriptionAll(getRuntimeUniqueId())),
				inputTelemetryCallback);
	}

	private void subscribeParameterTopics() throws ZError {
		zenohClient.declareSubscriber(KeyExpr.tryFrom(ZenohHelper.getAgentParametersTopicsAll(getRuntimeUniqueId())),
				parameterCallback);

	}

}
