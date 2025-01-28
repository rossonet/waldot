package net.rossonet.waldot.namespaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.tinkerpop.gremlin.process.computer.GraphFilter;
import org.apache.tinkerpop.gremlin.process.computer.VertexComputeKey;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph.Variables;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.eclipse.milo.opcua.sdk.server.ObjectTypeManager;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.UaNodeManager;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.dtd.DataTypeDictionaryManager;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.ReferenceType;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.BootstrapProcedureStrategy;
import net.rossonet.waldot.api.ConsoleStrategy;
import net.rossonet.waldot.api.NamespaceListener;
import net.rossonet.waldot.api.OpcMappingStrategy;
import net.rossonet.waldot.api.WaldotCommand;
import net.rossonet.waldot.commands.AboutCommand;
import net.rossonet.waldot.commands.ExecCommand;
import net.rossonet.waldot.commands.HelpCommand;
import net.rossonet.waldot.commands.QueryCommand;
import net.rossonet.waldot.commands.VersionCommand;
import net.rossonet.waldot.configuration.HomunculusConfiguration;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcGraphComputerView;
import net.rossonet.waldot.gremlin.opcgraph.structure.AbstractOpcGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.AbstractOpcGraph.IdManager;
import net.rossonet.waldot.opc.AbstractWaldotCommand;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcEdge;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraphVariables;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcProperty;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertex;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertexProperty;
import net.rossonet.waldot.rules.DefaultRulesEngine;

public class HomunculusNamespace extends ManagedNamespaceWithLifecycle {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final HomunculusConfiguration configuration;
	private final DataTypeDictionaryManager dictionaryManager;
	private final SubscriptionModel subscriptionModel;
	private final OpcGraph gremlin;
	private OpcGraphComputerView graphComputerView;
	private final OpcMappingStrategy opcMappingStrategy;
	private final BootstrapProcedureStrategy bootstrapProcedureStrategy;
	private final String[] bootstrapProcedure;
	private final OpcGraphVariables opcGraphVariables;
	private final ConsoleStrategy consoleStrategy;
	private final DefaultRulesEngine rulesEngine = new DefaultRulesEngine(this);
	private final List<NamespaceListener> listeners = new ArrayList<>();

	public HomunculusNamespace(OpcUaServer server, OpcMappingStrategy opcMappingStrategy,
			ConsoleStrategy consoleStrategy, HomunculusConfiguration configuration,
			BootstrapProcedureStrategy bootstrapProcedureStrategy, String[] bootstrapProcedure) {
		super(server, configuration.getManagerNamespaceUri());
		this.opcMappingStrategy = opcMappingStrategy;
		this.consoleStrategy = consoleStrategy;
		this.configuration = configuration;
		this.bootstrapProcedureStrategy = bootstrapProcedureStrategy;
		this.bootstrapProcedure = bootstrapProcedure;
		opcGraphVariables = new OpcGraphVariables(this);
		subscriptionModel = new SubscriptionModel(server, this);
		dictionaryManager = new DataTypeDictionaryManager(getNodeContext(), configuration.getManagerNamespaceUri());
		getLifecycleManager().addLifecycle(dictionaryManager);
		getLifecycleManager().addLifecycle(subscriptionModel);
		getLifecycleManager().addStartupTask(this::runBootstrapProcedure);
		gremlin = OpcGraph.open();
		gremlin.setNamespace(this);
		opcMappingStrategy.initialize(this);
		consoleStrategy.initialize(this);
		addBaseCommands();
		logger.info("Namespace created");
		listeners.forEach(listener -> listener.onNamespaceCreated(this));
		bootstrapProcedureStrategy.initialize(this);

	}

	// TODO sostituire con registrazione comando da plugin
	private void addBaseCommands() {
		if (configuration.getWaldotCommandLabel() != null) {
			registerCommand(new QueryCommand(this));
		}
		if (configuration.getVersionCommandLabel() != null) {
			registerCommand(new VersionCommand(this));
		}
		if (configuration.getHelpCommandLabel() != null) {
			registerCommand(new HelpCommand(this));
		}
		if (configuration.getAboutCommandLabel() != null) {
			registerCommand(new AboutCommand(this));
		}
		if (configuration.getExecCommandLabel() != null) {
			registerCommand(new ExecCommand(this));
		}
	}

	public Edge addEdge(OpcVertex sourceVertex, OpcVertex targetVertex, String label, Object[] keyValues) {
		return opcMappingStrategy.addEdge(sourceVertex, targetVertex, label, keyValues);
	}

	public void addListener(NamespaceListener listener) {
		listeners.add(listener);
	}

	public OpcVertex addVertex(NodeId nodeId, Object[] keyValues) {
		return opcMappingStrategy.addVertex(nodeId, keyValues);
	}

	public OpcGraphComputerView createGraphComputerView(AbstractOpcGraph graph, GraphFilter graphFilter,
			Set<VertexComputeKey> object) {
		return opcMappingStrategy.createGraphComputerView(graph, graphFilter, object);
	}

	public <DATA_TYPE> OpcProperty<DATA_TYPE> createOrUpdateOpcEdgeProperty(OpcEdge opcEdge, String key,
			DATA_TYPE value) {
		return opcMappingStrategy.createOrUpdateOpcEdgeProperty(opcEdge, key, value);
	}

	public <DATA_TYPE> VertexProperty<DATA_TYPE> createOrUpdateOpcVertexProperty(OpcVertex opcVertex, String key,
			DATA_TYPE value) {
		return opcMappingStrategy.createOrUpdateOpcVertexProperty(opcVertex, key, value);
	}

	public void dropGraphComputerView() {
		opcMappingStrategy.dropGraphComputerView();
	}

	public NodeId generateNodeId(Long nodeId) {
		return newNodeId(nodeId);
	}

	public NodeId generateNodeId(String nodeId) {
		return newNodeId(nodeId);
	}

	public NodeId generateNodeId(UInteger nodeId) {
		return newNodeId(nodeId);
	}

	public NodeId generateNodeId(UUID nodeId) {
		return newNodeId(nodeId);
	}

	public QualifiedName generateQualifiedName(String text) {
		return newQualifiedName(text);
	}

	public String[] getBootstrapProcedure() {
		return bootstrapProcedure;
	}

	public HomunculusConfiguration getConfiguration() {
		return configuration;
	}

	public OpcVertex getEdgeInVertex(OpcEdge opcEdge) {
		return opcMappingStrategy.getEdgeInVertex(opcEdge);
	}

	public Edge getEdgeNode(NodeId nodeId) {
		return getEdges().get(nodeId);
	}

	public OpcVertex getEdgeOutVertex(OpcEdge opcEdge) {
		return opcMappingStrategy.getEdgeOutVertex(opcEdge);
	}

	public <DATA_TYPE> List<OpcProperty<DATA_TYPE>> getEdgeProperties(OpcEdge opcEdge) {
		return opcMappingStrategy.getEdgeProperties(opcEdge);
	}

	public Map<NodeId, Edge> getEdges() {
		return opcMappingStrategy.getEdges();
	}

	public Map<NodeId, Edge> getEdges(OpcVertex opcVertex, Direction direction, String[] edgeLabels) {
		return opcMappingStrategy.getEdges(opcVertex, direction, edgeLabels);
	}

	public int getEdgesCount() {
		return getEdges().size();
	}

	public OpcGraphComputerView getGraphComputerView() {
		return graphComputerView;
	}

	public OpcGraph getGremlinGraph() {
		return gremlin;
	}

	public Collection<NamespaceListener> getListeners() {
		return listeners;
	}

	public NamespaceTable getNamespaceTable() {
		return getServer().getNamespaceTable();
	}

	public IdManager<NodeId> getNodeIdManager() {
		return OpcMappingStrategy.getNodeIdManager();
	}

	public ObjectTypeManager getNodeType() {
		return getServer().getObjectTypeManager();

	}

	public UaNodeContext getOpcUaNodeContext() {
		return getNodeContext();
	}

	public <DATA_TYPE> OpcEdge getPropertyReference(OpcProperty<DATA_TYPE> opcProperty) {
		return opcMappingStrategy.getPropertyReference(opcProperty);
	}

	public Map<NodeId, ReferenceType> getReferenceTypes() {
		return getServer().getReferenceTypes();
	}

	public DefaultRulesEngine getRulesEngine() {
		return rulesEngine;
	}

	public UaNodeManager getStorageManager() {
		return getNodeManager();

	}

	public OpcGraphVariables getVariables() {
		return opcGraphVariables;
	}

	public Vertex getVertexNode(NodeId nodeId) {
		return getVertices().get(nodeId);
	}

	public <DATA_TYPE> Map<String, OpcVertexProperty<DATA_TYPE>> getVertexProperties(OpcVertex opcVertex) {
		return opcMappingStrategy.getVertexProperties(opcVertex);
	}

	public <DATA_TYPE> OpcVertex getVertexPropertyReference(OpcVertexProperty<DATA_TYPE> opcVertexProperty) {
		return opcMappingStrategy.getVertexPropertyReference(opcVertexProperty);
	}

	public Map<NodeId, Vertex> getVertices() {
		return opcMappingStrategy.getVertices();
	}

	public Map<NodeId, Vertex> getVertices(OpcVertex opcVertex, Direction direction, String[] edgeLabels) {
		return opcMappingStrategy.getVertices(opcVertex, direction, edgeLabels);
	}

	public int getVerticesCount() {
		return getVertices().size();
	}

	public boolean hasNodeId(NodeId nodeId) {
		return getNodeManager().containsNode(nodeId);
	}

	public boolean inComputerMode() {
		return null != getGraphComputerView();
	}

	public Object namespaceParametersGet(String key) {
		return opcMappingStrategy.namespaceParametersGet(key);
	}

	public Set<String> namespaceParametersKeySet() {
		return opcMappingStrategy.namespaceParametersKeySet();
	}

	public void namespaceParametersPut(String key, Object value) {
		opcMappingStrategy.namespaceParametersPut(key, value);

	}

	public void namespaceParametersRemove(String key) {
		opcMappingStrategy.namespaceParametersRemove(key);

	}

	public Variables namespaceParametersToVariables() {
		return opcMappingStrategy.namespaceParametersToVariables();
	}

	@Override
	public void onDataItemsCreated(List<DataItem> dataItems) {
		subscriptionModel.onDataItemsCreated(dataItems);
		listeners.forEach(listener -> listener.onDataItemsCreated(dataItems));
	}

	@Override
	public void onDataItemsDeleted(List<DataItem> dataItems) {
		subscriptionModel.onDataItemsDeleted(dataItems);
		listeners.forEach(listener -> listener.onDataItemsDeleted(dataItems));
	}

	@Override
	public void onDataItemsModified(List<DataItem> dataItems) {
		subscriptionModel.onDataItemsModified(dataItems);
		listeners.forEach(listener -> listener.onDataItemsModified(dataItems));
	}

	@Override
	public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
		subscriptionModel.onMonitoringModeChanged(monitoredItems);
		listeners.forEach(listener -> listener.onMonitoringModeChanged(monitoredItems));
	}

	public void registerCommand(WaldotCommand command) {
		opcMappingStrategy.registerCommand(command);
		listeners.forEach(listener -> listener.onCommandRegistered(command));
	}

	// TODO verificare
	public void registerMethodInputArgument(AbstractWaldotCommand abstractWaldotCommand,
			List<Argument> inputArguments) {
		opcMappingStrategy.registerCommandInputArgument(abstractWaldotCommand, inputArguments);

	}

	// TODO verificare
	public void registerMethodOutputArguments(AbstractWaldotCommand abstractWaldotCommand,
			List<Argument> inputArguments) {
		opcMappingStrategy.registerCommandOutputArguments(abstractWaldotCommand, inputArguments);

	}

	public void removeCommand(WaldotCommand command) {
		opcMappingStrategy.removeCommand(command);
		listeners.forEach(listener -> listener.onCommandRemoved(command));
	}

	public void removeEdge(NodeId expandedNodeId) {
		opcMappingStrategy.removeEdge(expandedNodeId);

	}

	public void removeListener(NamespaceListener listener) {
		listeners.remove(listener);
	}

	public void removeVertex(NodeId nodeId) {
		opcMappingStrategy.removeVertex(nodeId);

	}

	public void removeVertexProperty(NodeId nodeId) {
		opcMappingStrategy.removeVertexProperty(nodeId);

	}

	public void resetNameSpace() {
		opcMappingStrategy.resetNameSpace();
		logger.info("Bootstrap procedure completed");
		listeners.forEach(listener -> listener.onNamespaceReset());
		runBootstrapProcedure();
	}

	private void runBootstrapProcedure() {
		bootstrapProcedureStrategy.runBootstrapProcedure();
		logger.info("Bootstrap procedure completed");
		listeners.forEach(listener -> listener.onBootstrapProcedureCompleted());
	}

	public Object runExpression(String expression) {
		return consoleStrategy.runExpression(expression);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Namespace [VerticesCount=");
		builder.append(getVerticesCount());
		builder.append(", EdgesCount=");
		builder.append(getEdgesCount());
		builder.append(", ");
		builder.append("]");
		return builder.toString();
	}

}
