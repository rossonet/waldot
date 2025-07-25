package net.rossonet.waldot.api.models;

import java.util.Collection;
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
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.factories.EventFactory;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.ReferenceType;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;

import com.google.common.eventbus.EventBus;

import net.rossonet.waldot.agent.auth.AgentRegisterAnonymousValidator;
import net.rossonet.waldot.agent.auth.AgentRegisterUsernameIdentityValidator;
import net.rossonet.waldot.agent.auth.AgentRegisterX509IdentityValidator;
import net.rossonet.waldot.api.NamespaceListener;
import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.configuration.WaldotConfiguration;
import net.rossonet.waldot.api.rules.WaldotRulesEngine;
import net.rossonet.waldot.api.strategies.AgentManagementStrategy;
import net.rossonet.waldot.opc.WaldotOpcUaServer;

/**
 * WaldotNamespace is an interface that defines the operations for managing a
 * namespace in the Waldot graph model. It provides methods to add edges and
 * vertices, manage properties, handle events, and interact with the graph
 * computer view.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface WaldotNamespace extends AutoCloseable {

	WaldotEdge addEdge(WaldotVertex sourceVertex, WaldotVertex targetVertex, String label, Object[] keyValues);

	void addListener(NamespaceListener listener);

	WaldotVertex addVertex(NodeId nodeId, Object[] keyValues);

	WaldotGraphComputerView createGraphComputerView(WaldotGraph graph, GraphFilter graphFilter,
			Set<VertexComputeKey> object);

	<DATA_TYPE> WaldotProperty<DATA_TYPE> createOrUpdateWaldotEdgeProperty(WaldotEdge edge, String key,
			DATA_TYPE value);

	<DATA_TYPE> WaldotVertexProperty<DATA_TYPE> createOrUpdateWaldotVertexProperty(WaldotVertex vertex, String key,
			DATA_TYPE value);

	void dropGraphComputerView();

	NodeId generateNodeId(Long nodeId);

	NodeId generateNodeId(String nodeid);

	NodeId generateNodeId(UInteger nodeId);

	NodeId generateNodeId(UUID nodeId);

	QualifiedName generateQualifiedName(String label);

	AgentManagementStrategy getAgentManagementStrategy();

	Logger getBootLogger();

	String getBootstrapUrl();

	Object getCommandsAsFunction();

	WaldotConfiguration getConfiguration();

	Logger getConsoleLogger();

	WaldotVertex getEdgeInVertex(WaldotEdge edge);

	WaldotEdge getEdgeNode(NodeId nodeId);

	WaldotVertex getEdgeOutVertex(WaldotEdge edge);

	public <DATA_TYPE> List<WaldotProperty<DATA_TYPE>> getEdgeProperties(WaldotEdge edge);

	Map<NodeId, WaldotEdge> getEdges();

	Map<NodeId, WaldotEdge> getEdges(WaldotVertex vertex, Direction direction, String[] edgeLabels);

	int getEdgesCount();

	EventBus getEventBus();

	EventFactory getEventFactory();

	WaldotGraphComputerView getGraphComputerView();

	WaldotGraph getGremlinGraph();

	Collection<NamespaceListener> getListeners();

	NamespaceTable getNamespaceTable();

	public String getNamespaceUri();

	IdManager<NodeId> getNodeIdManager();

	ObjectTypeManager getObjectTypeManager();

	public UaNodeContext getOpcUaNodeContext();

	WaldotOpcUaServer getOpcuaServer();

	Set<PluginListener> getPlugins();

	public <DATA_TYPE> WaldotEdge getPropertyReference(WaldotProperty<DATA_TYPE> property);

	public Map<NodeId, ReferenceType> getReferenceTypes();

	WaldotRulesEngine getRulesEngine();

	Logger getRulesLogger();

	UaNodeManager getStorageManager();

	Graph.Variables getVariables();

	WaldotVertex getVertexNode(NodeId nodeId);

	<DATA_TYPE> Map<String, WaldotVertexProperty<DATA_TYPE>> getVertexProperties(WaldotVertex vertex);

	public <DATA_TYPE> WaldotVertex getVertexPropertyReference(WaldotVertexProperty<DATA_TYPE> vertexProperty);

	Map<NodeId, WaldotVertex> getVertices();

	Map<NodeId, WaldotVertex> getVertices(WaldotVertex vertex, Direction direction, String[] edgeLabels);

	int getVerticesCount();

	boolean hasNodeId(NodeId nodeId);

	boolean inComputerMode();

	Object namespaceParametersGet(String key);

	Set<String> namespaceParametersKeySet();

	void namespaceParametersPut(String key, Object value);

	void namespaceParametersRemove(String key);

	Variables namespaceParametersToVariables();

	void opcuaUpdateEvent(UaNode sourceNode);

	void registerAgentValidators(AgentRegisterAnonymousValidator agentAnonymousValidator,
			AgentRegisterUsernameIdentityValidator agentIdentityValidator,
			AgentRegisterX509IdentityValidator agentX509IdentityValidator);

	void registerCommand(WaldotCommand command);

	void registerPlugin(PluginListener plugin);

	void removeCommand(WaldotCommand command);

	void removeEdge(NodeId expandedNodeId);

	void removeListener(NamespaceListener listener);

	void removeVertex(NodeId nodeId);

	void removeVertexProperty(NodeId nodeId);

	void resetNameSpace();

	Object runExpression(String expression);

	void shutdown();

	void startup();

	void unregisterPlugin(PluginListener plugin);

}
