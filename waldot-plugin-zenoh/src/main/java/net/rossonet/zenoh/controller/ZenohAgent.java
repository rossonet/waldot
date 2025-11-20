package net.rossonet.zenoh.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenoh.bytes.Encoding;
import io.zenoh.handlers.Callback;
import io.zenoh.pubsub.PutOptions;
import io.zenoh.qos.CongestionControl;
import io.zenoh.qos.Priority;
import io.zenoh.qos.Reliability;
import io.zenoh.sample.Sample;
import net.rossonet.waldot.agent.digitalTwin.DtdlHandler;
import net.rossonet.zenoh.WaldotZenohException;
import net.rossonet.zenoh.ZenohHelper;
import net.rossonet.zenoh.client.api.AgentCommand;
import net.rossonet.zenoh.client.api.AgentConfigurationObject;
import net.rossonet.zenoh.client.api.AgentProperty;
import net.rossonet.zenoh.client.api.TelemetryData;

public class ZenohAgent {

	private final static Logger logger = LoggerFactory.getLogger(ZenohAgent.class);

	public static ZenohAgent fromDiscoveryMessage(AgentLifeCycleManager agentLifeCycleManager,
			JSONObject discoveryMessage) throws WaldotZenohException {
		final String uniqueId = discoveryMessage.getString(ZenohHelper.UNIQUE_ID_LABEL);
		final long discoveryMessageTime = discoveryMessage.getLong(ZenohHelper.TIME_LABEL);
		final JSONObject dtml = discoveryMessage.getJSONObject(ZenohHelper.DTML_LABEL);
		final int apiVersion = discoveryMessage.getInt(ZenohHelper.VERSION_LABEL);
		return new ZenohAgent(agentLifeCycleManager, uniqueId, dtml, apiVersion, discoveryMessageTime);
	}

	private transient AgentLifeCycleManager agentLifeCycleManager;
	private final int apiVersion;
	private final Map<String, AgentConfigurationObject> configurationObjects = new HashMap<>();
	private final long discoveryMessageTime;
	private DtdlHandler dtmlHandler;
	private Vertex managedVertex;
	private final Map<String, AgentProperty> propertyObjects = new HashMap<>();
	private final Map<String, AgentCommand> registerCommands = new HashMap<>();
	private final Map<String, TelemetryData> registerTelemetries = new HashMap<>();
	private final String uniqueId;

	public ZenohAgent(AgentLifeCycleManager agentLifeCycleManager, String uniqueId, JSONObject dtml, int apiVersion,
			long discoveryMessageTime) {
		this.agentLifeCycleManager = agentLifeCycleManager;
		this.uniqueId = uniqueId;
		try {
			this.dtmlHandler = DtdlHandler.newFromDtdlV2(dtml.toString());
		} catch (final IOException e) {
			logger.error("Error parsing DTML from discovery info for agent {}", uniqueId, e);
		}
		this.apiVersion = apiVersion;
		updateConfigurationObjects(AgentConfigurationObject.fromDtml(dtmlHandler));
		updateCommands(AgentCommand.fromDtml(dtmlHandler));
		updateTelemetryObjects(TelemetryData.fromDtml(dtmlHandler));
		updatePropertyObjects(AgentProperty.fromDtml(dtmlHandler));
		this.discoveryMessageTime = discoveryMessageTime;
		subscribeToAgentDtmlTopic();
		subscribeToAgentTelemetryTopics();
		subscribeToAgentPropertyTopics();
		subscribeToAgentKeepAliveTopics();
	}

	public void checkAgentLifeCycle() {
		// TODO Auto-generated method stub

	}

	private PutOptions getAcknowledgePutOptions() {
		final PutOptions putOptions = new PutOptions();
		putOptions.setEncoding(Encoding.ZENOH_STRING);
		putOptions.setCongestionControl(CongestionControl.BLOCK);
		putOptions.setReliability(Reliability.RELIABLE);
		putOptions.setPriority(Priority.INTERACTIVE_HIGH);
		return putOptions;
	}

	public AgentLifeCycleManager getAgentLifeCycleManager() {
		return agentLifeCycleManager;
	}

	public Object[] getAgentVertexProperties(String agentsOpcuaDirectory) {
		final List<String> properties = new ArrayList<>();
		properties.add("id");
		properties.add(uniqueId);
		properties.add("label");
		properties.add(uniqueId);
		properties.add("api-version");
		properties.add(Integer.toString(apiVersion));
		properties.add("directory");
		properties.add(agentsOpcuaDirectory);
		properties.add("discovery-time");
		properties.add(Long.toString(discoveryMessageTime));
		properties.add("dtml");
		properties.add(new JSONObject(dtmlHandler).toString(2));
		final Object[] objects = properties.toArray(new Object[properties.size()]);
		return objects;
	}

	public Vertex getManagedVertex() {
		return managedVertex;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	private void sendAcknowLedgeMessage(AgentLifeCycleManager agentLifeCycleManager) {
		final String topic = ZenohHelper.getAcknowLedgeTopic(getUniqueId());
		final JSONObject acknowledgeMessage = new JSONObject();
		acknowledgeMessage.put(ZenohHelper.UNIQUE_ID_LABEL, uniqueId);
		acknowledgeMessage.put(ZenohHelper.TIME_LABEL, System.currentTimeMillis());
		acknowledgeMessage.put(ZenohHelper.DTML_LABEL, new JSONObject(dtmlHandler));
		try {
			agentLifeCycleManager.getZenohClient().sendMessage(topic, acknowledgeMessage, getAcknowledgePutOptions());
		} catch (final WaldotZenohException e) {
			logger.error("Error sending acknowledge message to agent {}", uniqueId, e);
		}
	}

	public void sendFirstAcknowledgeMessage() {
		if (this.agentLifeCycleManager != null) {
			logger.warn("Acknowledge message already sent for agent {}", uniqueId);
			return;
		}
		sendAcknowLedgeMessage(agentLifeCycleManager);
	}

	public void setManagedVertex(Vertex vertex) {
		managedVertex = vertex;

	}

	private void subscribeToAgentDtmlTopic() {
		getAgentLifeCycleManager().getZenohClient().subscribe(ZenohHelper.getUpdateDiscoveryTopic(getUniqueId()),
				new Callback<Sample>() {

					@Override
					public void run(Sample sample) {
						JSONObject payloadJson = null;
						try {
							payloadJson = new JSONObject(sample.getPayload().toString());
						} catch (final Exception e) {
							logger.error("Error parsing discovery message payload: {}", sample.getPayload(), e);
							return;
						}
						final String uniqueId = payloadJson.getString(ZenohHelper.UNIQUE_ID_LABEL);
						long lastDiscoveryMessageTime = payloadJson.getLong(ZenohHelper.TIME_LABEL);
						final JSONObject dtml = payloadJson.getJSONObject(ZenohHelper.DTML_LABEL);
						final int apiVersion = payloadJson.getInt(ZenohHelper.VERSION_LABEL);
						if (!ZenohAgent.this.uniqueId.equals(uniqueId)) {
							logger.warn("Received discovery update for different agent: {}", uniqueId);
							return;
						}
						if (lastDiscoveryMessageTime <= discoveryMessageTime) {
							logger.info("Ignoring out-of-date discovery update for agent {}", uniqueId);
							return;
						}
						if (ZenohAgent.this.apiVersion != apiVersion) {
							logger.warn("Ignoring discovery update for agent {} with different API version: {}",
									uniqueId, apiVersion);
							return;
						}
						try {
							dtmlHandler = DtdlHandler.newFromDtdlV2(dtml.toString());
						} catch (final IOException e) {
							logger.error("Error parsing DTML from discovery info for agent {}", uniqueId, e);
						}
						updateConfigurationObjects(AgentConfigurationObject.fromDtml(dtmlHandler));
						updateCommands(AgentCommand.fromDtml(dtmlHandler));
						updateTelemetryObjects(TelemetryData.fromDtml(dtmlHandler));
						updatePropertyObjects(AgentProperty.fromDtml(dtmlHandler));
						lastDiscoveryMessageTime = discoveryMessageTime;
						updateManagedVertex();
						sendAcknowLedgeMessage(agentLifeCycleManager);
					}
				});
	}

	private void subscribeToAgentKeepAliveTopics() {
		// TODO Auto-generated method stub

	}

	private void subscribeToAgentPropertyTopics() {
		// TODO Auto-generated method stub

	}

	private void subscribeToAgentTelemetryTopics() {
		// TODO Auto-generated method stub

	}

	private void updateCommands(Map<String, AgentCommand> commands) {
		for (final Map.Entry<String, AgentCommand> entry : commands.entrySet()) {
			if (!registerCommands.containsKey(entry.getKey())) {
				registerCommands.put(entry.getKey(), entry.getValue());
				logger.info("Registered new command {} for agent {}", entry.getKey(), uniqueId);
			}
		}
		for (final String existingCommandKey : new ArrayList<>(registerCommands.keySet())) {
			if (!commands.containsKey(existingCommandKey)) {
				registerCommands.remove(existingCommandKey);
				logger.info("Unregistered command {} for agent {}", existingCommandKey, uniqueId);
			}
		}
	}

	private void updateConfigurationObjects(Map<String, AgentConfigurationObject> configurations) {
		for (final Map.Entry<String, AgentConfigurationObject> entry : configurations.entrySet()) {
			if (!configurationObjects.containsKey(entry.getKey())) {
				configurationObjects.put(entry.getKey(), entry.getValue());
				logger.info("Registered new configuration object {} for agent {}", entry.getKey(), uniqueId);
			}
		}
		for (final String existingConfigKey : new ArrayList<>(configurationObjects.keySet())) {
			if (!configurations.containsKey(existingConfigKey)) {
				configurationObjects.remove(existingConfigKey);
				logger.info("Unregistered configuration object {} for agent {}", existingConfigKey, uniqueId);
			}
		}
	}

	private void updateManagedVertex() {
		// TODO Auto-generated method stub

	}

	private void updatePropertyObjects(Map<String, AgentProperty> properties) {
		for (final Map.Entry<String, AgentProperty> entry : properties.entrySet()) {
			if (!propertyObjects.containsKey(entry.getKey())) {
				propertyObjects.put(entry.getKey(), entry.getValue());
				logger.info("Registered new property object {} for agent {}", entry.getKey(), uniqueId);
			}
		}
		for (final String existingPropertyKey : new ArrayList<>(propertyObjects.keySet())) {
			if (!properties.containsKey(existingPropertyKey)) {
				propertyObjects.remove(existingPropertyKey);
				logger.info("Unregistered property object {} for agent {}", existingPropertyKey, uniqueId);
			}
		}

	}

	private void updateTelemetryObjects(Map<String, TelemetryData> telemetries) {
		for (final Map.Entry<String, TelemetryData> entry : telemetries.entrySet()) {
			if (!registerTelemetries.containsKey(entry.getKey())) {
				registerTelemetries.put(entry.getKey(), entry.getValue());
				logger.info("Registered new telemetry object {} for agent {}", entry.getKey(), uniqueId);
			}
		}
		for (final String existingTelemetryKey : new ArrayList<>(registerTelemetries.keySet())) {
			if (!telemetries.containsKey(existingTelemetryKey)) {
				registerTelemetries.remove(existingTelemetryKey);
				logger.info("Unregistered telemetry object {} for agent {}", existingTelemetryKey, uniqueId);
			}
		}

	}
}
