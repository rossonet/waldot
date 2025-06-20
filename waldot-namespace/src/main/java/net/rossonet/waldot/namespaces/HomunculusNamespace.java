package net.rossonet.waldot.namespaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.tinkerpop.gremlin.process.computer.GraphFilter;
import org.apache.tinkerpop.gremlin.process.computer.VertexComputeKey;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Graph.Variables;
import org.eclipse.milo.opcua.sdk.server.ObjectTypeManager;
import org.eclipse.milo.opcua.sdk.server.UaNodeManager;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.dtd.DataTypeDictionaryManager;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.factories.EventFactory;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.ReferenceType;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import net.rossonet.waldot.agent.auth.AgentRegisterAnonymousValidator;
import net.rossonet.waldot.agent.auth.AgentRegisterUsernameIdentityValidator;
import net.rossonet.waldot.agent.auth.AgentRegisterX509IdentityValidator;
import net.rossonet.waldot.api.NamespaceListener;
import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.configuration.WaldotConfiguration;
import net.rossonet.waldot.api.models.IdManager;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotGraphComputerView;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotProperty;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.models.WaldotVertexProperty;
import net.rossonet.waldot.api.rules.WaldotRulesEngine;
import net.rossonet.waldot.api.strategies.BootstrapStrategy;
import net.rossonet.waldot.api.strategies.ConsoleStrategy;
import net.rossonet.waldot.api.strategies.AgentManagementStrategy;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.commands.AboutCommand;
import net.rossonet.waldot.commands.HelpCommand;
import net.rossonet.waldot.commands.QueryCommand;
import net.rossonet.waldot.configuration.DefaultHomunculusConfiguration;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraphVariables;
import net.rossonet.waldot.jexl.RulesCmdFunction;
import net.rossonet.waldot.logger.TraceLogger;
import net.rossonet.waldot.logger.TraceLogger.ContexLogger;
import net.rossonet.waldot.opc.WaldotOpcUaServer;
import net.rossonet.waldot.rules.DefaultRulesEngine;

public class HomunculusNamespace extends ManagedNamespaceWithLifecycle implements WaldotNamespace {

	private final BootstrapStrategy bootstrapProcedureStrategy;
	private final WaldotConfiguration configuration;
	private final Logger consoleLogger = new TraceLogger(ContexLogger.CONSOLE);
	private final Logger bootLogger = new TraceLogger(ContexLogger.BOOT);
	private final ConsoleStrategy consoleStrategy;
	private final DataTypeDictionaryManager dictionaryManager;
	private WaldotGraphComputerView graphComputerView;
	private final WaldotGraph gremlin;
	private final RulesCmdFunction jexlWaldotCommandHelper;
	private final List<NamespaceListener> listeners = new ArrayList<>();
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Graph.Variables opcGraphVariables;
	private final MiloStrategy opcMappingStrategy;
	private final Set<PluginListener> plugins = new HashSet<>();
	private final WaldotRulesEngine rulesEngine;
	private final Logger rulesLogger = new TraceLogger(ContexLogger.RULES);

	private final SubscriptionModel subscriptionModel;
	private final String bootstrapUrl;
	private WaldotOpcUaServer waldotOpcUaServer;
	private AgentRegisterAnonymousValidator agentAnonymousValidator;
	private AgentRegisterUsernameIdentityValidator agentIdentityValidator;
	private AgentRegisterX509IdentityValidator agentX509IdentityValidator;
	private final AgentManagementStrategy agentManagementStrategy;

	public HomunculusNamespace(final WaldotOpcUaServer server, final MiloStrategy opcMappingStrategy,
			final ConsoleStrategy consoleStrategy, final DefaultHomunculusConfiguration configuration,
			final BootstrapStrategy bootstrapProcedureStrategy,
			final AgentManagementStrategy agentManagementStrategy, final String bootstrapUrl) {
		super(server.getServer(), configuration.getManagerNamespaceUri());
		this.waldotOpcUaServer = server;
		this.opcMappingStrategy = opcMappingStrategy;
		this.consoleStrategy = consoleStrategy;
		this.jexlWaldotCommandHelper = new RulesCmdFunction(this);
		this.configuration = configuration;
		this.bootstrapProcedureStrategy = bootstrapProcedureStrategy;
		this.agentManagementStrategy = agentManagementStrategy;
		this.bootstrapUrl = bootstrapUrl;
		opcGraphVariables = new OpcGraphVariables(this);
		subscriptionModel = new SubscriptionModel(server.getServer(), this);
		dictionaryManager = new DataTypeDictionaryManager(getNodeContext(), configuration.getManagerNamespaceUri());
		getLifecycleManager().addLifecycle(dictionaryManager);
		getLifecycleManager().addLifecycle(subscriptionModel);
		getLifecycleManager().addStartupTask(this::runBootstrapProcedure);
		gremlin = OpcGraph.open();
		gremlin.setNamespace(this);
		agentManagementStrategy.initialize(this);
		opcMappingStrategy.initialize(this);
		consoleStrategy.initialize(this);
		addBaseCommands();
		logger.info("Namespace created");
		listeners.forEach(listener -> listener.onNamespaceCreated(this));
		bootstrapProcedureStrategy.initialize(this);
		rulesEngine = new DefaultRulesEngine(this);
	}

	private void addBaseCommands() {
		if (configuration.getWaldotCommandLabel() != null) {
			registerCommand(new QueryCommand(this));
		}
		if (configuration.getHelpCommandLabel() != null) {
			registerCommand(new HelpCommand(this));
		}
		if (configuration.getAboutCommandLabel() != null) {
			registerCommand(new AboutCommand(this));
		}
	}

	@Override
	public WaldotEdge addEdge(final WaldotVertex sourceVertex, final WaldotVertex targetVertex, final String label,
			final Object[] keyValues) {
		return (WaldotEdge) opcMappingStrategy.addEdge(sourceVertex, targetVertex, label, keyValues);
	}

	@Override
	public void addListener(final NamespaceListener listener) {
		listeners.add(listener);
	}

	@Override
	public WaldotVertex addVertex(final NodeId nodeId, final Object[] keyValues) {
		return opcMappingStrategy.addVertex(nodeId, keyValues);
	}

	@Override
	public void close() throws Exception {
		plugins.forEach(plugin -> plugin.stop());

	}

	@Override
	public WaldotGraphComputerView createGraphComputerView(final WaldotGraph graph, final GraphFilter graphFilter,
			final Set<VertexComputeKey> object) {
		return opcMappingStrategy.createGraphComputerView(graph, graphFilter, object);
	}

	@Override
	public <DATA_TYPE> WaldotProperty<DATA_TYPE> createOrUpdateWaldotEdgeProperty(final WaldotEdge waldotEdge,
			final String key, final DATA_TYPE value) {
		return opcMappingStrategy.createOrUpdateWaldotEdgeProperty(waldotEdge, key, value);
	}

	@Override
	public <DATA_TYPE> WaldotVertexProperty<DATA_TYPE> createOrUpdateWaldotVertexProperty(final WaldotVertex opcVertex,
			final String key, final DATA_TYPE value) {
		return opcMappingStrategy.createOrUpdateWaldotVertexProperty(opcVertex, key, value);
	}

	@Override
	public void dropGraphComputerView() {
		opcMappingStrategy.dropGraphComputerView();
	}

	@Override
	public NodeId generateNodeId(final Long nodeId) {
		return newNodeId(nodeId);
	}

	@Override
	public NodeId generateNodeId(final String nodeId) {
		return newNodeId(nodeId);
	}

	@Override
	public NodeId generateNodeId(final UInteger nodeId) {
		return newNodeId(nodeId);
	}

	@Override
	public NodeId generateNodeId(final UUID nodeId) {
		return newNodeId(nodeId);
	}

	@Override
	public QualifiedName generateQualifiedName(final String text) {
		return newQualifiedName(text);
	}

	@Override
	public AgentManagementStrategy getAgentManagementStrategy() {
		return agentManagementStrategy;
	}

	@Override
	public Logger getBootLogger() {
		return bootLogger;
	}

	@Override
	public String getBootstrapUrl() {
		return bootstrapUrl;
	}

	@Override
	public Object getCommandsAsFunction() {
		return jexlWaldotCommandHelper;
	}

	@Override
	public WaldotConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public Logger getConsoleLogger() {
		return consoleLogger;
	}

	@Override
	public WaldotVertex getEdgeInVertex(final WaldotEdge opcWaldotEdge) {
		return opcMappingStrategy.getEdgeInVertex(opcWaldotEdge);
	}

	@Override
	public WaldotEdge getEdgeNode(final NodeId nodeId) {
		return getEdges().get(nodeId);
	}

	@Override
	public WaldotVertex getEdgeOutVertex(final WaldotEdge waldotEdge) {
		return opcMappingStrategy.getEdgeOutVertex(waldotEdge);
	}

	@Override
	public <DATA_TYPE> List<WaldotProperty<DATA_TYPE>> getEdgeProperties(final WaldotEdge waldotEdge) {
		return opcMappingStrategy.getProperties(waldotEdge);
	}

	@Override
	public Map<NodeId, WaldotEdge> getEdges() {
		return opcMappingStrategy.getEdges();
	}

	@Override
	public Map<NodeId, WaldotEdge> getEdges(final WaldotVertex vertex, final Direction direction,
			final String[] edgeLabels) {
		return opcMappingStrategy.getEdges(vertex, direction, edgeLabels);
	}

	@Override
	public int getEdgesCount() {
		return getEdges().size();
	}

	@Override
	public EventBus getEventBus() {
		return getServer().getEventBus();
	}

	@Override
	public EventFactory getEventFactory() {
		return getServer().getEventFactory();
	}

	@Override
	public WaldotGraphComputerView getGraphComputerView() {
		return graphComputerView;
	}

	@Override
	public WaldotGraph getGremlinGraph() {
		return gremlin;
	}

	@Override
	public Collection<NamespaceListener> getListeners() {
		return listeners;
	}

	@Override
	public NamespaceTable getNamespaceTable() {
		return getServer().getNamespaceTable();
	}

	@Override
	public IdManager<NodeId> getNodeIdManager() {
		return MiloStrategy.getNodeIdManager();
	}

	@Override
	public ObjectTypeManager getObjectTypeManager() {
		return getServer().getObjectTypeManager();

	}

	@Override
	public UaNodeContext getOpcUaNodeContext() {
		return getNodeContext();
	}

	@Override
	public WaldotOpcUaServer getOpcuaServer() {
		return waldotOpcUaServer;
	}

	@Override
	public Set<PluginListener> getPlugins() {
		return plugins;
	}

	@Override
	public <DATA_TYPE> WaldotEdge getPropertyReference(final WaldotProperty<DATA_TYPE> opcProperty) {
		return opcMappingStrategy.getPropertyReference(opcProperty);
	}

	@Override
	public Map<NodeId, ReferenceType> getReferenceTypes() {
		return getServer().getReferenceTypes();
	}

	@Override
	public WaldotRulesEngine getRulesEngine() {
		return rulesEngine;
	}

	@Override
	public Logger getRulesLogger() {
		return rulesLogger;
	}

	@Override
	public UaNodeManager getStorageManager() {
		return getNodeManager();

	}

	@Override
	public Graph.Variables getVariables() {
		return opcGraphVariables;
	}

	@Override
	public WaldotVertex getVertexNode(final NodeId nodeId) {
		return getVertices().get(nodeId);
	}

	@Override
	public <DATA_TYPE> Map<String, WaldotVertexProperty<DATA_TYPE>> getVertexProperties(final WaldotVertex opcVertex) {
		return opcMappingStrategy.getVertexProperties(opcVertex);
	}

	@Override
	public <DATA_TYPE> WaldotVertex getVertexPropertyReference(
			final WaldotVertexProperty<DATA_TYPE> opcVertexProperty) {
		return opcMappingStrategy.getVertexPropertyReference(opcVertexProperty);
	}

	@Override
	public Map<NodeId, WaldotVertex> getVertices() {
		return opcMappingStrategy.getVertices();
	}

	@Override
	public Map<NodeId, WaldotVertex> getVertices(final WaldotVertex opcVertex, final Direction direction,
			final String[] edgeLabels) {
		return opcMappingStrategy.getVertices(opcVertex, direction, edgeLabels);
	}

	@Override
	public int getVerticesCount() {
		return getVertices().size();
	}

	@Override
	public boolean hasNodeId(final NodeId nodeId) {
		return getNodeManager().containsNode(nodeId);
	}

	@Override
	public boolean inComputerMode() {
		return null != getGraphComputerView();
	}

	@Override
	public Object namespaceParametersGet(final String key) {
		return opcMappingStrategy.namespaceParametersGet(key);
	}

	@Override
	public Set<String> namespaceParametersKeySet() {
		return opcMappingStrategy.namespaceParametersKeySet();
	}

	@Override
	public void namespaceParametersPut(final String key, final Object value) {
		opcMappingStrategy.namespaceParametersPut(key, value);

	}

	@Override
	public void namespaceParametersRemove(final String key) {
		opcMappingStrategy.namespaceParametersRemove(key);

	}

	@Override
	public Variables namespaceParametersToVariables() {
		return opcMappingStrategy.namespaceParametersToVariables();
	}

	@Override
	public void onDataItemsCreated(final List<DataItem> dataItems) {
		subscriptionModel.onDataItemsCreated(dataItems);
		listeners.forEach(listener -> listener.onDataItemsCreated(dataItems));
	}

	@Override
	public void onDataItemsDeleted(final List<DataItem> dataItems) {
		subscriptionModel.onDataItemsDeleted(dataItems);
		listeners.forEach(listener -> listener.onDataItemsDeleted(dataItems));
	}

	@Override
	public void onDataItemsModified(final List<DataItem> dataItems) {
		subscriptionModel.onDataItemsModified(dataItems);
		listeners.forEach(listener -> listener.onDataItemsModified(dataItems));
	}

	@Override
	public void onMonitoringModeChanged(final List<MonitoredItem> monitoredItems) {
		subscriptionModel.onMonitoringModeChanged(monitoredItems);
		listeners.forEach(listener -> listener.onMonitoringModeChanged(monitoredItems));
	}

	@Override
	public void opcuaUpdateEvent(final UaNode sourceNode) {
		opcMappingStrategy.updateEventGenerator(sourceNode);

	}

	@Override
	public void registerAgentValidators(final AgentRegisterAnonymousValidator agentAnonymousValidator,
			final AgentRegisterUsernameIdentityValidator agentIdentityValidator,
			final AgentRegisterX509IdentityValidator agentX509IdentityValidator) {
		this.agentAnonymousValidator = agentAnonymousValidator;
		this.agentIdentityValidator = agentIdentityValidator;
		this.agentX509IdentityValidator = agentX509IdentityValidator;
		agentManagementStrategy.activate(agentAnonymousValidator, agentIdentityValidator, agentX509IdentityValidator);
	}

	@Override
	public void registerCommand(final WaldotCommand command) {
		opcMappingStrategy.registerCommand(command);
		listeners.forEach(listener -> listener.onCommandRegistered(command));
	}

	@Override
	public void registerPlugin(final PluginListener plugin) {
		plugins.add(plugin);
		plugin.initialize(this);
		logger.info("Registering commands from plugin {}", plugin.getClass().getSimpleName());
		plugin.getCommands().forEach(command -> registerCommand(command));
	}

	@Override
	public void removeCommand(final WaldotCommand command) {
		opcMappingStrategy.removeCommand(command);
		listeners.forEach(listener -> listener.onCommandRemoved(command));
	}

	@Override
	public void removeEdge(final NodeId expandedNodeId) {
		opcMappingStrategy.removeEdge(expandedNodeId);

	}

	@Override
	public void removeListener(final NamespaceListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void removeVertex(final NodeId nodeId) {
		opcMappingStrategy.removeVertex(nodeId);

	}

	@Override
	public void removeVertexProperty(final NodeId nodeId) {
		opcMappingStrategy.removeVertexProperty(nodeId);

	}

	@Override
	public void resetNameSpace() {
		for (final PluginListener plugin : plugins) {
			plugin.reset();
		}
		opcMappingStrategy.resetNameSpace();
		logger.info("Namespace reset done");
		listeners.forEach(listener -> listener.onNamespaceReset());
		logger.info("Running bootstrap procedure after reset");
		runBootstrapProcedure();
	}

	private void runBootstrapProcedure() {
		plugins.forEach(plugin -> plugin.start());
		bootstrapProcedureStrategy.runBootstrapProcedure();
		listeners.forEach(listener -> listener.onBootstrapProcedureCompleted());
	}

	@Override
	public Object runExpression(final String expression) {
		return consoleStrategy.runExpression(expression);
	}

	public void setOpcuaServer(final WaldotOpcUaServer opcuaServer) {
		this.waldotOpcUaServer = opcuaServer;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Namespace [VerticesCount=");
		builder.append(getVerticesCount());
		builder.append(", WaldotEdgesCount=");
		builder.append(getEdgesCount());
		builder.append(", ");
		builder.append("]");
		return builder.toString();
	}

	@Override
	public void unregisterPlugin(final PluginListener plugin) {
		plugins.remove(plugin);
	}

}
