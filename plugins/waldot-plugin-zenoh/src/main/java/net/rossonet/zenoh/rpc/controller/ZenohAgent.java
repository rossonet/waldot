package net.rossonet.zenoh.rpc.controller;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenoh.Session;
import io.zenoh.exceptions.ZError;
import io.zenoh.pubsub.CallbackSubscriber;
import io.zenoh.pubsub.Publisher;
import io.zenoh.sample.Sample;
import net.rossonet.waldot.api.PropertyObserver;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.dtdl.DtdlHandler;
import net.rossonet.waldot.opc.AbstractOpcCommand;
import net.rossonet.waldot.opc.AbstractOpcCommand.VariableNodeTypes;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.TextHelper;
import net.rossonet.waldot.utils.ThreadHelper;
import net.rossonet.zenoh.ZenohHelper;
import net.rossonet.zenoh.api.AgentCommandMetadata;
import net.rossonet.zenoh.api.AgentCommandParameter;
import net.rossonet.zenoh.api.AgentConfigurationMetadata;
import net.rossonet.zenoh.api.AgentProperty;
import net.rossonet.zenoh.api.TelemetryData;
import net.rossonet.zenoh.api.WaldotAgentEndpoint;
import net.rossonet.zenoh.api.message.RpcCommand;
import net.rossonet.zenoh.api.message.RpcConfiguration;
import net.rossonet.zenoh.exception.WaldotZenohException;
import net.rossonet.zenoh.rpc.controller.command.StartAgentCommand;
import net.rossonet.zenoh.rpc.controller.command.StopAgentCommand;

/**
 * * Represents a Zenoh Agent managed by the AgentLifeCycleManager
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public class ZenohAgent implements WaldotAgentEndpoint, AutoCloseable, PropertyObserver {

	private final static Logger logger = LoggerFactory.getLogger(ZenohAgent.class);
	protected static final long TIMEOUT_COMMAND_MS = 10000;

	public static ZenohAgent fromDiscoveryMessage(final AgentLifeCycleManager agentLifeCycleManager,
			final JSONObject discoveryMessage, final String agentOpcUaDirectory) throws WaldotZenohException {
		final String uniqueId = discoveryMessage.getString(ZenohHelper.UNIQUE_ID_LABEL);
		final long discoveryMessageTime = discoveryMessage.getLong(ZenohHelper.TIME_LABEL);
		final JSONObject dtml = discoveryMessage.getJSONObject(ZenohHelper.DTML_LABEL);
		final int apiVersion = discoveryMessage.getInt(ZenohHelper.VERSION_LABEL);
		return new ZenohAgent(agentLifeCycleManager, uniqueId, agentOpcUaDirectory, dtml, apiVersion,
				discoveryMessageTime);
	}

	private transient AgentLifeCycleManager agentLifeCycleManager;
	private WaldotVertex agentManagerVertex;
	private final int apiVersion;
	private final String baseAgentsOpcDirectory;
	private final Map<String, AgentConfigurationMetadata> configurationObjects = new HashMap<>();
	private final Map<String, WaldotVertex> configurationObjectVertices = new HashMap<>();
	private DtdlHandler dtmlHandler;
	private long lastDiscoveryMessageAtMs;
	private volatile long lastSeenMs;
	private final Map<String, AbstractOpcCommand> opcAddConfigurationCommands = new HashMap<>();
	private final Map<String, AbstractOpcCommand> opcRegisterCommands = new HashMap<>();
	private final Map<String, AgentProperty> propertyObjects = new HashMap<>();
	private final Map<String, Publisher> publishers = new ConcurrentHashMap<>();
	private final Map<String, AgentCommandMetadata> registerCommands = new HashMap<>();

	private final Map<String, TelemetryData> registerTelemetries = new HashMap<>();
	private StartAgentCommand startCommand;
	private StopAgentCommand stopCommand;
	private final Map<String, CallbackSubscriber> subcribers = new ConcurrentHashMap<>();

	private final Map<Long, CommandLifecycleRegister> suspendedCommands = new ConcurrentHashMap<>();

	private final String uniqueId;

	public ZenohAgent(final AgentLifeCycleManager agentLifeCycleManager, final String uniqueId,
			final String baseAgentsOpcuaDirectory, final JSONObject dtml, final int apiVersion,
			final long discoveryMessageTimeMs) {
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
		updateConfigurationMetadatas(AgentConfigurationMetadata.fromDtml(dtmlHandler));
		updateCommandMetadatas(AgentCommandMetadata.fromDtml(dtmlHandler));
		updateTelemetryMetadatas(TelemetryData.fromDtml(dtmlHandler));
		updatePropertyMetadatas(AgentProperty.fromDtml(dtmlHandler));
		try {
			ZenohHelper.subscribeToAgentTopicsFromWaldot(this);
		} catch (final ZError e) {
			logger.error("Error subscribing to topics for agent {}", uniqueId, e);
		}
		lastSeenMs = System.currentTimeMillis();
	}

	private void addBaseCommands() {
		startCommand = new StartAgentCommand(this);
		agentLifeCycleManager.getNamespace().registerCommand(startCommand);
		stopCommand = new StopAgentCommand(this);
		agentLifeCycleManager.getNamespace().registerCommand(stopCommand);
	}

	private void addConfigurationAddCommand(final String objectName, final AgentConfigurationMetadata objectDetails) {
		if (opcAddConfigurationCommands.containsKey(objectName)) {
			logger.debug("Add Configuration Command {} already exists for agent {}, updating if necessary", objectName,
					uniqueId);
		} else {
			final AbstractOpcCommand command = new AbstractOpcCommand(agentLifeCycleManager.getGraph(),
					agentLifeCycleManager.getNamespace(),
					uniqueId + ".add." + TextHelper.cleanText(objectDetails.getConfigurationName()),
					"add " + objectDetails.getConfigurationName(),
					"add configuration " + objectDetails.getDescription(), "agents/" + uniqueId, UInteger.valueOf(0),
					UInteger.valueOf(0), true, true) {

				@Override
				public Object[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
					if (inputValues == null || inputValues.length == 0 || inputValues[0] == null
							|| inputValues[0].isEmpty()) {
						logger.warn("No name provided for configuration to add for agent {}", uniqueId);
						return new Object[] { "No name provided for configuration to add." };
					}
					final String idToAdd = objectName + "." + inputValues[0];
					final String label = inputValues[0];
					logger.info("Adding configuration {} of type {} to agent {}", idToAdd,
							objectDetails.getConfigurationClassName(), uniqueId);
					final List<String> properties = new ArrayList<>();
					properties.add("id");
					properties.add(uniqueId + ":configuration:" + TextHelper.cleanText(idToAdd));
					properties.add("label");
					properties.add(label);
					properties.add(RpcConfiguration.EPOCH);
					properties.add("0");
					properties.add("api-version");
					properties.add(Integer.toString(apiVersion));
					properties.add("directory");
					properties.add(baseAgentsOpcDirectory + "/" + uniqueId + "/configurations");
					properties.add("description");
					properties.add(objectDetails.getDescription());
					properties.add("class-name");
					properties.add(objectDetails.getConfigurationClassName());
					for (final Entry<String, AgentProperty> propertyObject : objectDetails.getProperties().entrySet()) {
						logger.debug("Adding property {} with default value {} to configuration {} of agent {}",
								propertyObject.getKey(), propertyObject.getValue().getAnnotation().defaultValue(),
								idToAdd, uniqueId);
						properties.add(TextHelper.cleanText(propertyObject.getKey()));
						properties.add(propertyObject.getValue().getAnnotation().defaultValue());
					}
					final Object[] objects = properties.toArray(new Object[properties.size()]);
					logger.debug("Adding configuration configuration vertex with properties: {}", properties);
					final WaldotVertex objectVertex = (WaldotVertex) agentLifeCycleManager.getGraph()
							.addVertex(objects);
					// TODO: perché osservato il comando?
					// objectVertex.addPropertyObserver(this);
					configurationObjectVertices.put(idToAdd, objectVertex);
					sendConfigurationObjectVertexUpdateToAgent(idToAdd,
							properties.toArray(new String[properties.size()]));
					return new Object[] { "Configuration " + idToAdd + " added." };
				}
			};
			command.addInputArgument("name", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
					LocalizedText.english("name of the configuration to add"));
			command.addOutputArgument("result", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
					LocalizedText.english("result of the add configuration command"));
			agentLifeCycleManager.getNamespace().registerCommand(command);
			opcAddConfigurationCommands.put(objectName, command);
			logger.info("Registered new Add Configuration command {} for agent {}", objectName, uniqueId);
		}

	}

	public void checkAgentStatus() {
		if (System.currentTimeMillis() - lastSeenMs > (ZenohHelper.AGENT_TIMEOUT_MS / 2)) {
			sendPing();
		}
		// TODO: gestire agente considerato offline
	}

	@Override
	public void close() throws Exception {
		for (final CallbackSubscriber subscriber : subcribers.values()) {
			subscriber.close();
		}
		subcribers.clear();
		for (final Publisher publisher : publishers.values()) {
			try {
				publisher.close();
			} catch (final Exception e) {
				logger.error("Error closing publisher for agent {}", uniqueId, e);
			}
		}
		publishers.clear();
	}

	private AbstractOpcCommand createCommandVertex(final String commandName,
			final AgentCommandMetadata commandDetails) {
		return new AbstractOpcCommand(agentLifeCycleManager.getGraph(), agentLifeCycleManager.getNamespace(),
				uniqueId + "." + TextHelper.cleanText(commandDetails.getCommandName()), commandDetails.getCommandName(),
				commandDetails.getAnnotation().description(), "agents/" + uniqueId,
				UInteger.valueOf(commandDetails.getWriteMask()), UInteger.valueOf(commandDetails.getUserWriteMask()),
				commandDetails.isExecutable(), commandDetails.isUserExecutable()) {
//TODO aggiungere gli argomenti di input del comando OPC UA
			@Override
			public Object[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
				try {
					logger.info("Executing command {} on agent {}", commandName, uniqueId);
					return elaborateRemoteCommandOnAgent(commandName, commandDetails.getCommandParameters(),
							inputValues).get(TIMEOUT_COMMAND_MS, TimeUnit.MILLISECONDS).getOutputValues();
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					logger.error("Error executing command {} on agent {}", commandName, uniqueId, e);
					return new CommandLifecycleRegister(getUniqueId(), commandName, inputValues, e).getOutputValues();
				}
			}
		};
	}

	@Override
	public void elaborateCommandReplyMessage(final Sample sample) {
		JSONObject payloadJson = null;
		try {
			payloadJson = new JSONObject(sample.getPayload().toString());
		} catch (final Throwable e) {
			logger.error("Error parsing command reply message payload: {}", sample.getPayload(), e);
			return;
		}
		try {
			final RpcCommand command = RpcCommand.fromJson(payloadJson);
			if (!suspendedCommands.containsKey(command.getRelatedId())) {
				logger.warn("Received reply for unknown command with id {} from agent {}", command.getUniqueId(),
						uniqueId);
				return;
			} else {
				final CommandLifecycleRegister suspendedCommand = suspendedCommands.get(command.getRelatedId());
				if (command.getAgentId() == null || !command.getAgentId().equals(uniqueId)) {
					logger.warn(
							"Received reply for command with id {} from different agent {} instead of expected agent {}",
							command.getUniqueId(), command.getAgentId(), uniqueId);
					return;
				}
				if (suspendedCommand.getCommandId() == null
						|| !suspendedCommand.getCommandId().equals(command.getCommandId())) {
					logger.warn(
							"Received reply for command with id {} with different command id {} instead of expected command id {}",
							command.getUniqueId(), command.getCommandId(), suspendedCommand.getCommandId());
					return;
				}
				suspendedCommand.setReplyMessage(command.getReplyMessage());
				suspendedCommand.setSuccess(command.getExecutionCommandException() == null);
				suspendedCommand.setErrorMessage(command.getExecutionCommandException() != null
						? command.getExecutionCommandException().getMessage()
						: null);
				suspendedCommand.setReplyTimeMs(Instant.now().toEpochMilli());
				suspendedCommand.setStackTrace(command.getExecutionCommandException() != null
						? LogHelper.stackTraceToString(command.getExecutionCommandException())
						: null);
				suspendedCommand.setCompleted(true);
				suspendedCommand.getReentrantLock().lock();
				try {
					suspendedCommand.getCompleteCondition().signalAll();
				} finally {
					suspendedCommand.getReentrantLock().unlock();
				}
				suspendedCommands.remove(command.getUniqueId());
			}
		} catch (final Throwable e) {
			logger.error("Error deserializing command reply message payload: {}", payloadJson, e);
		}

	}

	@Override
	public void elaborateConfigurationMessage(final Sample sample) {
		// TODO elaborate messaggio di configurazione oggetti

	}

	@Override
	public void elaborateInternalTelemetryMessage(final Sample sample) {
		// TODO elaborare telemetria interna con controllo di flusso

	}

	@Override
	public void elaborateParameterMessage(final Sample sample) {
		// TODO elaborare messaggio di parametro

	}

	@Override
	public void elaboratePongMessage(final Sample sample) {
		lastSeenMs = System.currentTimeMillis();
	}

	public Future<CommandLifecycleRegister> elaborateRemoteCommandOnAgent(final String commandId,
			List<AgentCommandParameter> parameters, final String[] inputValues) {
		final String topic = ZenohHelper.getRpcCommandTopic(getUniqueId(), TextHelper.cleanText(commandId));
		final Map<String, Object> values = new HashMap<>();
		for (int i = 0; i < inputValues.length; i++) {
			values.put(parameters.get(i).getParameterName(), inputValues[i]);
		}
		final RpcCommand rpcCommand = new RpcCommand(getUniqueId(), commandId, values);
		try {
			final CommandLifecycleRegister calledCommand = new CommandLifecycleRegister(rpcCommand);
			suspendedCommands.put(rpcCommand.getUniqueId(), calledCommand);
			ZenohHelper.sendMessage(this, topic, rpcCommand.toJson(), ZenohHelper.getCommandPutOptions());
			return new Future<CommandLifecycleRegister>() {

				@Override
				public boolean cancel(final boolean mayInterruptIfRunning) {
					return false;
				}

				@Override
				public CommandLifecycleRegister get() {
					logger.debug("Waiting for command {} response from agent {}", commandId, uniqueId);
					if (!calledCommand.isCompleted()) {
						calledCommand.getReentrantLock().lock();
					}
					return calledCommand;
				}

				@Override
				public CommandLifecycleRegister get(final long timeout, final java.util.concurrent.TimeUnit unit) {
					logger.debug("Waiting for command {} response from agent {} with timeout {} {}", commandId,
							uniqueId, timeout, unit);
					if (!calledCommand.isCompleted()) {
						try {
							ThreadHelper.runWithTimeout(new Callable<Void>() {
								@Override
								public Void call() throws Exception {
									calledCommand.getReentrantLock().lock();
									try {
										while (!calledCommand.isCompleted()) {
											calledCommand.getCompleteCondition().await();
										}
									} finally {
										calledCommand.getReentrantLock().unlock();
									}

									return null;
								}
							}, timeout, unit);
						} catch (final Exception e) {
							logger.error("Timeout waiting for command {} response from agent {}", commandId, uniqueId,
									e);
						}
					}
					return calledCommand;
				}

				@Override
				public boolean isCancelled() {
					return false;
				}

				@Override
				public boolean isDone() {
					return calledCommand.isCompleted();
				}

			};
		} catch (final WaldotZenohException e) {
			logger.error("Error sending RPC command {} to agent {}", commandId, uniqueId, e);
			return null;
		}

	}

	@Override
	public void elaborateTelemetryMessage(final Sample sample) {
		// TODO elaborare messaggio di telemetria con controllo di flusso

	}

	@Override
	public void elaborateUpdateDiscoveryMessage(final Sample sample) {
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
			logger.warn("Ignoring discovery update for agent {} with different API version: {}", uniqueId, apiVersion);
			return;
		}
		try {
			dtmlHandler = DtdlHandler.newFromDtdlV2(dtml.toString());
		} catch (final IOException e) {
			logger.error("Error parsing DTML from discovery info for agent {}", uniqueId, e);
		}
		updateConfigurationMetadatas(AgentConfigurationMetadata.fromDtml(dtmlHandler));
		updateCommandMetadatas(AgentCommandMetadata.fromDtml(dtmlHandler));
		updateTelemetryMetadatas(TelemetryData.fromDtml(dtmlHandler));
		updatePropertyMetadatas(AgentProperty.fromDtml(dtmlHandler));
		updateManagedOpcVertexObjects();
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
		properties.add(RpcConfiguration.EPOCH);
		properties.add("0");
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

	@Override
	public Map<String, Publisher> getPublishers() {
		return publishers;
	}

	@Override
	public Map<String, CallbackSubscriber> getSubcribers() {
		return subcribers;
	}

	@Override
	public String getUniqueId() {
		return uniqueId;
	}

	public WaldotNamespace getWaldotNamespace() {
		return agentLifeCycleManager.getNamespace();
	}

	@Override
	public Session getZenohClient() {
		return getAgentLifeCycleManager().getZenohClient().getSession();
	}

	private void manageCommandOpcUaVertex(final String commandName, final AgentCommandMetadata commandDetails) {
		if (opcRegisterCommands.containsKey(commandName)) {
			logger.debug("Command {} already exists for agent {}, updating if necessary", commandName, uniqueId);
			// TODO: gestire il cambio delle caratteristiche del comando se necessario
		} else {
			final AbstractOpcCommand command = createCommandVertex(commandName, commandDetails);
			CommandLifecycleRegister.addStandardAgentCommandOutputArguments(command);
			agentLifeCycleManager.getNamespace().registerCommand(command);
			opcRegisterCommands.put(commandName, command);
			// logger.info("Registered new command {} for agent {}", commandName, uniqueId);
		}

	}

	public void propertyChanged(final UaNode sourceNode, final AttributeId attributeId, final Object value) {
		if (sourceNode.getNodeId().equals(agentManagerVertex.getNodeId())) {
			sendAgentManagerVertexUpdateToAgent(agentManagerVertex.getPropertiesAsStringArray());
		} else {
			boolean found = false;
			for (final Entry<String, WaldotVertex> configurationObject : configurationObjectVertices.entrySet()) {
				if (sourceNode.getNodeId().equals(configurationObject.getValue().getNodeId())) {
					sendConfigurationObjectVertexUpdateToAgent(configurationObject.getKey(),
							configurationObject.getValue().getPropertiesAsStringArray());
					found = true;
					break;
				}
			}
			if (!found) {
				logger.warn("Property change on unknown node {} for agent {}", sourceNode.getNodeId(), uniqueId);
			}
		}

	}

	@Override
	public void propertyChanged(UaNode sourceNode, String label, Object value) {
		// TODO Auto-generated method stub

	}

	private void sendAcknowLedgeMessageToAgent() {
		final String topic = ZenohHelper.getAcknowLedgeTopic(getUniqueId());
		final JSONObject acknowledgeMessage = new JSONObject();
		acknowledgeMessage.put(ZenohHelper.UNIQUE_ID_LABEL, uniqueId);
		acknowledgeMessage.put(ZenohHelper.TIME_LABEL, System.currentTimeMillis());
		final JSONArray configurationEpochArray = new JSONArray();
		for (final Entry<String, WaldotVertex> configurationObject : configurationObjectVertices.entrySet()) {
			final JSONObject configurationData = new JSONObject();
			configurationData.put("configuration_id", configurationObject.getKey());
			configurationData.put(RpcConfiguration.EPOCH,
					configurationObject.getValue().property(RpcConfiguration.EPOCH).value());
			configurationEpochArray.put(configurationData);
		}
		acknowledgeMessage.put("configurations", configurationEpochArray);
		try {
			ZenohHelper.sendMessage(this, topic, acknowledgeMessage, ZenohHelper.getAcknowledgePutOptions());
		} catch (final WaldotZenohException e) {
			logger.error("Error sending acknowledge message to agent {}", uniqueId, e);
		}
	}

	private void sendAgentManagerVertexUpdateToAgent(final String[] data) {
		final RpcConfiguration configuration = RpcConfiguration.fromProperties(ZenohHelper.MAIN_CONFIG_ID,
				List.of(data));
		final String topic = ZenohHelper.getParameterTopic(getUniqueId());
		try {
			ZenohHelper.sendMessage(this, topic, configuration.toJson(), ZenohHelper.getCommandPutOptions());
		} catch (final WaldotZenohException e) {
			logger.error("Error sending agent manager vertex update to agent {}", uniqueId, e);
		}

	}

	private void sendConfigurationObjectVertexUpdateToAgent(final String objectId, final String[] data) {
		final RpcConfiguration configuration = RpcConfiguration.fromProperties(objectId, List.of(data));
		final String topic = ZenohHelper.getConfigurationsBaseTopic(getUniqueId()) + ZenohHelper._TOPIC_SEPARATOR
				+ objectId;
		try {
			ZenohHelper.sendMessage(this, topic, configuration.toJson(), ZenohHelper.getCommandPutOptions());
		} catch (final WaldotZenohException e) {
			logger.error("Error sending configuration object {} vertex update to agent {}", objectId, uniqueId, e);
		}

	}

	public void sendPing() {
		final String topic = ZenohHelper.getPingTopic(getUniqueId());
		final JSONObject pingMessage = new JSONObject();
		pingMessage.put(ZenohHelper.UNIQUE_ID_LABEL, uniqueId);
		pingMessage.put(ZenohHelper.TIME_LABEL, System.currentTimeMillis());
		try {
			ZenohHelper.sendMessage(this, topic, pingMessage, ZenohHelper.getPingPutOptions());
		} catch (final WaldotZenohException e) {
			logger.error("Error sending acknowledge message to agent {}", uniqueId, e);
		}
	}

	public void setManagedVertex(final WaldotVertex vertex) {
		agentManagerVertex = vertex;
		updateManagedOpcVertexObjects();
		addBaseCommands();
		sendAcknowLedgeMessageToAgent();
	}

	private void updateCommandMetadatas(final Map<String, AgentCommandMetadata> commands) {
		for (final Map.Entry<String, AgentCommandMetadata> entry : commands.entrySet()) {
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

	private void updateConfigurationMetadatas(final Map<String, AgentConfigurationMetadata> configurations) {
		for (final Map.Entry<String, AgentConfigurationMetadata> entry : configurations.entrySet()) {
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

	private void updateManagedOpcVertexObjects() {
		for (final Map.Entry<String, AgentConfigurationMetadata> configurationObject : configurationObjects
				.entrySet()) {
			addConfigurationAddCommand(configurationObject.getKey(), configurationObject.getValue());
		}
		for (final Entry<String, AgentCommandMetadata> commandObject : registerCommands.entrySet()) {
			manageCommandOpcUaVertex(commandObject.getKey(), commandObject.getValue());
		}
		for (final Entry<String, AgentProperty> propertyObject : propertyObjects.entrySet()) {
			logger.debug("Adding property {} with default value {} to managed vertex of agent {}",
					propertyObject.getKey(), propertyObject.getValue().getAnnotation().defaultValue(), uniqueId);
			final String cleanText = TextHelper.cleanText(propertyObject.getKey());
			agentManagerVertex.property(cleanText, propertyObject.getValue().getAnnotation().defaultValue());
			agentManagerVertex.addPropertyObserver(this);
		}

	}

	private void updatePropertyMetadatas(final Map<String, AgentProperty> properties) {
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

	private void updateTelemetryMetadatas(final Map<String, TelemetryData> telemetries) {
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
