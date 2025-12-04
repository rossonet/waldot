package net.rossonet.zenoh.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
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
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.dtdl.DtdlHandler;
import net.rossonet.waldot.opc.AbstractOpcCommand;
import net.rossonet.waldot.opc.AbstractOpcCommand.VariableNodeTypes;
import net.rossonet.waldot.utils.TextHelper;
import net.rossonet.zenoh.WaldotZenohException;
import net.rossonet.zenoh.ZenohHelper;
import net.rossonet.zenoh.client.api.AgentCommand;
import net.rossonet.zenoh.client.api.AgentConfigurationObject;
import net.rossonet.zenoh.client.api.AgentProperty;
import net.rossonet.zenoh.client.api.TelemetryData;
import net.rossonet.zenoh.controller.command.StartAgentCommand;
import net.rossonet.zenoh.controller.command.StopAgentCommand;

/**
 * * Represents a Zenoh Agent managed by the AgentLifeCycleManager
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public class ZenohAgent {

	private final static Logger logger = LoggerFactory.getLogger(ZenohAgent.class);

	public static ZenohAgent fromDiscoveryMessage(AgentLifeCycleManager agentLifeCycleManager,
			JSONObject discoveryMessage, String agentOpcUaDirectory) throws WaldotZenohException {
		final String uniqueId = discoveryMessage.getString(ZenohHelper.UNIQUE_ID_LABEL);
		final long discoveryMessageTime = discoveryMessage.getLong(ZenohHelper.TIME_LABEL);
		final JSONObject dtml = discoveryMessage.getJSONObject(ZenohHelper.DTML_LABEL);
		final int apiVersion = discoveryMessage.getInt(ZenohHelper.VERSION_LABEL);
		return new ZenohAgent(agentLifeCycleManager, uniqueId, agentOpcUaDirectory, dtml, apiVersion,
				discoveryMessageTime);
	}

	private final Map<String, AbstractOpcCommand> activeAddObjectCommands = new HashMap<>();
	private final Map<String, AbstractOpcCommand> activeCommands = new HashMap<>();
	private transient AgentLifeCycleManager agentLifeCycleManager;
	private Vertex agentManagerVertex;
	private final int apiVersion;
	private final String baseAgentsOpcDirectory;
	private final Map<String, AgentConfigurationObject> configurationObjects = new HashMap<>();
	private final Map<String, Vertex> configurationObjectVertices = new HashMap<>();
	private DtdlHandler dtmlHandler;
	private long lastDiscoveryMessageAtMs;
	private final Map<String, AgentProperty> propertyObjects = new HashMap<>();
	private final Map<String, AgentCommand> registerCommands = new HashMap<>();
	private final Map<String, TelemetryData> registerTelemetries = new HashMap<>();
	private StartAgentCommand startCommand;
	private StopAgentCommand stopCommand;
	private final String uniqueId;

	public ZenohAgent(AgentLifeCycleManager agentLifeCycleManager, String uniqueId, String baseAgentsOpcuaDirectory,
			JSONObject dtml, int apiVersion, long discoveryMessageTimeMs) {
		this.agentLifeCycleManager = agentLifeCycleManager;
		this.uniqueId = uniqueId;
		baseAgentsOpcDirectory = baseAgentsOpcuaDirectory;
		this.lastDiscoveryMessageAtMs = discoveryMessageTimeMs;
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
		subscribeToUpdateDiscoveryTopic();
		subscribeToAgentTelemetryTopic();
		subscribeToAgentInternalTelemetryTopic();
		subscribeToAgentKeepAliveTopic();
		subscribeToAgentConfigurationsTopic();
		subscribeToAgentParametersTopic();
	}

	private void addBaseCommands() {
		startCommand = new StartAgentCommand(this);
		agentLifeCycleManager.getNamespace().registerCommand(startCommand);
		stopCommand = new StopAgentCommand(this);
		agentLifeCycleManager.getNamespace().registerCommand(stopCommand);
	}

	private void addObjectAddCommand(String objectName, AgentConfigurationObject objectDetails) {
		if (activeAddObjectCommands.containsKey(objectName)) {
			logger.debug("Add Object Command {} already exists for agent {}, updating if necessary", objectName,
					uniqueId);
			// TODO: il cambio delle caratteristiche del comando se necessario
		} else {
			final AbstractOpcCommand command = new AbstractOpcCommand(agentLifeCycleManager.getGraph(),
					agentLifeCycleManager.getNamespace(),
					uniqueId + ".add." + TextHelper.cleanText(objectDetails.getConfigurationName()),
					"add " + objectDetails.getConfigurationName(), "add object that " + objectDetails.getDescription(),
					"agents/" + uniqueId, UInteger.valueOf(0), UInteger.valueOf(0), true, true) {

				@Override
				public Object[] runCommand(InvocationContext invocationContext, String[] inputValues) {
					if (inputValues == null || inputValues.length == 0 || inputValues[0] == null
							|| inputValues[0].isEmpty()) {
						logger.warn("No name provided for object to add for agent {}", uniqueId);
						return new Object[] {};
					}
					final String idToAdd = objectName + "." + inputValues[0];
					final String label = inputValues[0];
					logger.info("Adding object {} of type {} to agent {}", idToAdd,
							objectDetails.getConfigurationClassName(), uniqueId);
					final List<String> properties = new ArrayList<>();
					properties.add("id");
					properties.add(uniqueId + ":configuration:" + TextHelper.cleanText(idToAdd));
					properties.add("label");
					properties.add(label);
					properties.add("api-version");
					properties.add(Integer.toString(apiVersion));
					properties.add("directory");
					properties.add(baseAgentsOpcDirectory + "/" + uniqueId + "/configurations");
					properties.add("description");
					properties.add(objectDetails.getDescription());
					properties.add("class-name");
					properties.add(objectDetails.getConfigurationClassName());
					for (final Entry<String, AgentProperty> propertyObject : objectDetails.getProperties().entrySet()) {
						logger.debug("Adding property {} with default value {} to object {} of agent {}",
								propertyObject.getKey(), propertyObject.getValue().getAnnotation().defaultValue(),
								idToAdd, uniqueId);
						properties.add(TextHelper.cleanText(propertyObject.getKey()));
						properties.add(propertyObject.getValue().getAnnotation().defaultValue());
					}
					final Object[] objects = properties.toArray(new Object[properties.size()]);
					logger.debug("Adding configuration object vertex with properties: {}", properties);
					final Vertex objectVertex = agentLifeCycleManager.getGraph().addVertex(objects);
					configurationObjectVertices.put(idToAdd, objectVertex);
					sendObjectConfigurationUpdate(idToAdd, objectVertex);
					return new Object[] {};
				}
			};
			command.addInputArgument("name", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
					LocalizedText.english("name of the object to add"));
			agentLifeCycleManager.getNamespace().registerCommand(command);
			activeAddObjectCommands.put(objectName, command);
			logger.info("Registered new Add Object command {} for agent {}", objectName, uniqueId);
		}

	}

	public void checkAgentStatus() {
		// TODO controllare lo stato dell'agente e inviare un messaggio di keep alive se
		// necessario

	}

	protected void elaborateConfigurationObjectsMessage(Sample sample) {
		// TODO elaborate messaggio di configurazione oggetti

	}

	private void elaborateInternalTelemetryMessage(Sample sample) {
		// TODO elaborare telemetria interna con controllo di flusso

	}

	protected void elaborateKeepAliveMessage(Sample sample) {
		// TODO elaborare messaggio di keep alive

	}

	protected void elaborateParameterMessage(Sample sample) {
		// TODO elaborare messaggio di parametro

	}

	protected void elaborateTelemetryMessage(Sample sample) {
		// TODO elaborare messaggio di telemetria con controllo di flusso

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

	public Object[] getAgentVertexProperties() {
		final List<String> properties = new ArrayList<>();
		properties.add("id");
		properties.add(uniqueId);
		properties.add("label");
		properties.add("Agent Manager");
		properties.add("api_version");
		properties.add(Integer.toString(apiVersion));
		properties.add("directory");
		properties.add(baseAgentsOpcDirectory + "/" + uniqueId);
		properties.add("discovery_time");
		properties.add(Long.toString(lastDiscoveryMessageAtMs));
		properties.add("discovery_dtml");
		properties.add(new JSONObject(dtmlHandler).toString(2));
		final Object[] objects = properties.toArray(new Object[properties.size()]);
		return objects;
	}

	public WaldotGraph getGremlinGraph() {
		return agentLifeCycleManager.getGraph();
	}

	public Vertex getManagedVertex() {
		return agentManagerVertex;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public WaldotNamespace getWaldotNamespace() {
		return agentLifeCycleManager.getNamespace();
	}

	private void initializeOpcAgent() {
		updateManagedVertexObjects();
		addBaseCommands();
	}

	private void manageCommandOpcUaVertex(String commandName, AgentCommand commandDetails) {
		if (activeCommands.containsKey(commandName)) {
			logger.debug("Command {} already exists for agent {}, updating if necessary", commandName, uniqueId);
			// TODO: gestire il cambio delle caratteristiche del comando se necessario
		} else {
			final AbstractOpcCommand command = new AbstractOpcCommand(agentLifeCycleManager.getGraph(),
					agentLifeCycleManager.getNamespace(),
					uniqueId + "." + TextHelper.cleanText(commandDetails.getCommandName()),
					commandDetails.getCommandName(), commandDetails.getAnnotation().description(), "agents/" + uniqueId,
					UInteger.valueOf(commandDetails.getWriteMask()),
					UInteger.valueOf(commandDetails.getUserWriteMask()), commandDetails.isExecutable(),
					commandDetails.isUserExecutable()) {

				@Override
				public Object[] runCommand(InvocationContext invocationContext, String[] inputValues) {
					return sendCommandToAgent(commandName, inputValues);
				}
			};
			// agentLifeCycleManager.getNamespace().registerCommand(command);
			activeCommands.put(commandName, command);
			logger.info("Registered new command {} for agent {}", commandName, uniqueId);
		}

	}

	public void sendAcknowLedgeMessage() {
		final String topic = ZenohHelper.getAcknowLedgeTopic(getUniqueId());
		final JSONObject acknowledgeMessage = new JSONObject();
		acknowledgeMessage.put(ZenohHelper.UNIQUE_ID_LABEL, uniqueId);
		acknowledgeMessage.put(ZenohHelper.TIME_LABEL, System.currentTimeMillis());
		try {
			agentLifeCycleManager.getZenohClient().sendMessage(topic, acknowledgeMessage, getAcknowledgePutOptions());
		} catch (final WaldotZenohException e) {
			logger.error("Error sending acknowledge message to agent {}", uniqueId, e);
		}
	}

	public Object[] sendCommandToAgent(String commandId, String[] inputValues) {
		// TODO inviare il comando all'agente e recuperare il risultato con un lifetime
		return new Object[0];
	}

	public void sendObjectConfigurationUpdate(String objectId, Vertex vertexRepresentation) {
		// TODO invia aggiornamento configurazione oggetto all'agente

	}

	public void setManagedVertex(Vertex vertex) {
		agentManagerVertex = vertex;
		initializeOpcAgent();
	}

	private void subscribeToAgentConfigurationsTopic() {
		getAgentLifeCycleManager().getZenohClient()
				.subscribe(ZenohHelper.getAgentConfigurationsTopicsAll(getUniqueId()), new Callback<Sample>() {

					@Override
					public void run(Sample sample) {
						elaborateConfigurationObjectsMessage(sample);

					}

				});

	}

	private void subscribeToAgentInternalTelemetryTopic() {
		getAgentLifeCycleManager().getZenohClient()
				.subscribe(ZenohHelper.getAgentInternalTelemetryTopicsAll(getUniqueId()), new Callback<Sample>() {

					@Override
					public void run(Sample sample) {
						elaborateInternalTelemetryMessage(sample);

					}

				});

	}

	private void subscribeToAgentKeepAliveTopic() {
		getAgentLifeCycleManager().getZenohClient().subscribe(ZenohHelper.getAgentKeepAliveTopic(getUniqueId()),
				new Callback<Sample>() {

					@Override
					public void run(Sample sample) {
						elaborateKeepAliveMessage(sample);

					}
				});
	}

	private void subscribeToAgentParametersTopic() {
		getAgentLifeCycleManager().getZenohClient().subscribe(ZenohHelper.getAgentParametersTopicsAll(getUniqueId()),
				new Callback<Sample>() {

					@Override
					public void run(Sample sample) {
						elaborateParameterMessage(sample);

					}

				});

	}

	private void subscribeToAgentTelemetryTopic() {
		getAgentLifeCycleManager().getZenohClient()
				.subscribe(ZenohHelper.getAgentTelemetryTopicsSubscriptionAll(getUniqueId()), new Callback<Sample>() {

					@Override
					public void run(Sample sample) {
						elaborateTelemetryMessage(sample);

					}
				});

	}

	private void subscribeToUpdateDiscoveryTopic() {
		getAgentLifeCycleManager().getZenohClient().subscribe(ZenohHelper.getAgentUpdateDiscoveryTopic(getUniqueId()),
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
						final long discoveryMessageTime = payloadJson.getLong(ZenohHelper.TIME_LABEL);
						final JSONObject dtml = payloadJson.getJSONObject(ZenohHelper.DTML_LABEL);
						final int apiVersion = payloadJson.getInt(ZenohHelper.VERSION_LABEL);
						if (!ZenohAgent.this.uniqueId.equals(uniqueId)) {
							logger.warn("Received discovery update for different agent: {}", uniqueId);
							return;
						}
						if (discoveryMessageTime <= lastDiscoveryMessageAtMs) {
							logger.info("Ignoring out-of-date discovery update for agent {}", uniqueId);
							return;
						} else {
							lastDiscoveryMessageAtMs = discoveryMessageTime;
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
						updateManagedVertexObjects();
						sendAcknowLedgeMessage();
					}
				});
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

	private void updateManagedVertexObjects() {
		for (final Map.Entry<String, AgentConfigurationObject> configurationObject : configurationObjects.entrySet()) {
			addObjectAddCommand(configurationObject.getKey(), configurationObject.getValue());
		}
		for (final Entry<String, AgentCommand> commandObject : registerCommands.entrySet()) {
			manageCommandOpcUaVertex(commandObject.getKey(), commandObject.getValue());
		}
		for (final Entry<String, AgentProperty> propertyObject : propertyObjects.entrySet()) {
			logger.debug("Adding property {} with default value {} to managed vertex of agent {}",
					propertyObject.getKey(), propertyObject.getValue().getAnnotation().defaultValue(), uniqueId);
			agentManagerVertex.property(TextHelper.cleanText(propertyObject.getKey()),
					propertyObject.getValue().getAnnotation().defaultValue());
		}

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
