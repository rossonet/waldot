package net.rossonet.waldot.api.models;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.tinkerpop.gremlin.process.computer.GraphFilter;
import org.apache.tinkerpop.gremlin.process.computer.VertexComputeKey;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Graph.Variables;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.typetree.ReferenceTypeTree;
import org.eclipse.milo.opcua.sdk.server.EventNotifier;
import org.eclipse.milo.opcua.sdk.server.ObjectTypeManager;
import org.eclipse.milo.opcua.sdk.server.UaNodeManager;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.factories.EventFactory;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;

import net.rossonet.waldot.api.NamespaceListener;
import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.configuration.WaldotConfiguration;
import net.rossonet.waldot.api.strategies.ClientManagementStrategy;
import net.rossonet.waldot.api.strategies.ConsoleStrategy;
import net.rossonet.waldot.api.strategies.HistoryStrategy;
import net.rossonet.waldot.client.auth.ClientRegisterAnonymousValidator;
import net.rossonet.waldot.client.auth.ClientRegisterUsernameIdentityValidator;
import net.rossonet.waldot.client.auth.ClientRegisterX509IdentityValidator;
import net.rossonet.waldot.opc.WaldotOpcUaServer;

/**
 * WaldotNamespace is an interface that defines the operations for managing a
 * namespace in the Waldot graph model. It provides methods to add edges and
 * vertices, manage properties, handle events, and interact with the graph
 * computer view.
 * 
 * <p>WaldotNamespace is the central hub for managing the WaldOT graph and OPC UA
 * address space. It provides:</p>
 * <ul>
 *   <li>Graph operations (add/remove vertices and edges)</li>
 *   <li>OPC UA node management</li>
 *   <li>Property management</li>
 *   <li>Strategy management (MiloStrategy, HistoryStrategy, ConsoleStrategy)</li>
 *   <li>Plugin management</li>
 *   <li>Configuration access</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Add a vertex
 * WaldotVertex vertex = namespace.addVertex(
 *     namespace.generateNodeId("device:001"),
 *     new Object[]{"label", "temperatureSensor"}
 * );
 * 
 * // Add another vertex and connect with an edge
 * WaldotVertex target = namespace.addVertex(
 *     namespace.generateNodeId("device:002"),
 *     new Object[]{"label", "actuator"}
 * );
 * WaldotEdge edge = namespace.addEdge(vertex, target, "controls", null);
 * 
 * // Set properties
 * namespace.createOrUpdateWaldotVertexProperty(vertex, "temperature", 25.5);
 * 
 * // Register a plugin
 * namespace.registerPlugin(new MyCustomPlugin());
 * 
 * // Execute expressions
 * namespace.runExpression("g.V().count()");
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface WaldotNamespace extends AutoCloseable {

	/**
	 * Adds an asset agent node to the OPC UA address space.
	 * 
	 * <p>The node is added to the asset clients folder for management by the
	 * client management strategy.</p>
	 * 
	 * @param node the UaNode to add as an asset agent
	 * @see UaNode
	 */
	void addAssetAgentNode(UaNode node);

	/**
	 * Adds a new edge to the graph connecting two vertices.
	 * 
	 * <p>Creates an edge between the source and target vertices with the given
	 * label and optional key-value pairs for properties.</p>
	 * 
	 * @param sourceVertex the source vertex
	 * @param targetVertex the target vertex
	 * @param label the edge label (relationship type)
	 * @param keyValues optional property key-value pairs
	 * @return the created WaldotEdge
	 * @see WaldotEdge
	 * @see WaldotVertex
	 */
	WaldotEdge addEdge(WaldotVertex sourceVertex, WaldotVertex targetVertex, String label, Object[] keyValues);

	/**
	 * Adds a namespace listener to receive namespace events.
	 * 
	 * @param listener the NamespaceListener to add
	 * @see NamespaceListener
	 * @see #removeListener(NamespaceListener)
	 */
	void addListener(NamespaceListener listener);

	/**
	 * Adds a new vertex to the graph.
	 * 
	 * <p>Creates a new vertex with the given NodeId and optional key-value pairs
	 * for properties. The vertex is registered in both the graph and the OPC UA
	 * address space.</p>
	 * 
	 * @param nodeId the NodeId for the new vertex
	 * @param keyValues optional property key-value pairs (e.g., "label", "device")
	 * @return the created WaldotVertex
	 * @see WaldotVertex
	 * @see NodeId
	 */
	WaldotVertex addVertex(NodeId nodeId, Object[] keyValues);

	/**
	 * Creates a GraphComputer view for OLAP processing.
	 * 
	 * <p>Sets up the graph for graph computer (OLAP) processing with the
	 * specified filter and compute keys.</p>
	 * 
	 * @param graph the WaldotGraph to use
	 * @param graphFilter the GraphFilter for vertex/edge filtering
	 * @param object the set of VertexComputeKey objects
	 * @return the created WaldotGraphComputerView
	 * @see WaldotGraphComputerView
	 * @see GraphFilter
	 */
	WaldotGraphComputerView createGraphComputerView(WaldotGraph graph, GraphFilter graphFilter,
			Set<VertexComputeKey> object);

	/**
	 * Creates or updates a property on an edge.
	 * 
	 * <p>If the property exists, its value is updated. Otherwise, a new
	 * property is created.</p>
	 * 
	 * @param edge the edge to modify
	 * @param key the property key
	 * @param value the property value
	 * @param <DATA_TYPE> the type of the property value
	 * @return the created or updated WaldotProperty
	 * @see WaldotProperty
	 * @see WaldotEdge
	 */
	<DATA_TYPE> WaldotProperty<DATA_TYPE> createOrUpdateWaldotEdgeProperty(WaldotEdge edge, String key,
			DATA_TYPE value);

	/**
	 * Creates or updates a property on a vertex.
	 * 
	 * <p>If the property exists, its value is updated. Otherwise, a new
	 * property is created.</p>
	 * 
	 * @param vertex the vertex to modify
	 * @param key the property key
	 * @param value the property value
	 * @param <DATA_TYPE> the type of the property value
	 * @return the created or updated WaldotVertexProperty
	 * @see WaldotVertexProperty
	 * @see WaldotVertex
	 */
	<DATA_TYPE> WaldotVertexProperty<DATA_TYPE> createOrUpdateWaldotVertexProperty(WaldotVertex vertex, String key,
			DATA_TYPE value);

	/**
	 * Deletes a directory (folder) node from the OPC UA address space.
	 * 
	 * <p>Removes the directory and all its contents from the graph and address space.</p>
	 * 
	 * @param directoryNodeId the NodeId of the directory to delete
	 * @return a status message indicating the result
	 */
	String deleteDirectory(String directoryNodeId);

	/**
	 * Drops the current GraphComputer view.
	 * 
	 * <p>Releases resources used by the OLAP view and returns to OLTP mode.</p>
	 * 
	 * @see #createGraphComputerView(WaldotGraph, GraphFilter, Set)
	 */
	void dropGraphComputerView();

	/**
	 * Generates a NodeId from a Long value.
	 * 
	 * @param nodeId the Long value
	 * @return the generated NodeId
	 * @see NodeId
	 */
	NodeId generateNodeId(Long nodeId);

	/**
	 * Generates a NodeId from a String.
	 * 
	 * <p>The string can be in standard NodeId format (e.g., "ns=2;i=123")
	 * or a simple identifier that will be converted.</p>
	 * 
	 * @param nodeid the String identifier
	 * @return the generated NodeId
	 * @see NodeId
	 */
	NodeId generateNodeId(String nodeid);

	/**
	 * Generates a NodeId from a UInteger value.
	 * 
	 * @param nodeId the UInteger value
	 * @return the generated NodeId
	 * @see NodeId
	 * @see UInteger
	 */
	NodeId generateNodeId(UInteger nodeId);

	/**
	 * Generates a NodeId from a UUID.
	 * 
	 * @param nodeId the UUID value
	 * @return the generated NodeId
	 * @see NodeId
	 * @see UUID
	 */
	NodeId generateNodeId(UUID nodeId);

	/**
	 * Generates a QualifiedName from a string label.
	 * 
	 * <p>QualifiedNames are used for OPC UA node identification and include
	 * both a namespace index and a name.</p>
	 * 
	 * @param label the name label
	 * @return the generated QualifiedName
	 * @see QualifiedName
	 */
	QualifiedName generateQualifiedName(String label);

	/**
	 * Returns the boot logger for startup messages.
	 * 
	 * <p>This logger is used for logging during the bootstrap procedure
	 * before the full logging system is initialized.</p>
	 * 
	 * @return the boot Logger
	 * @see Logger
	 */
	Logger getBootLogger();

	/**
	 * Returns the bootstrap URL used for configuration loading.
	 * 
	 * @return the bootstrap URL string
	 */
	String getBootstrapUrl();

	/**
	 * Returns the client management strategy.
	 * 
	 * <p>This strategy handles client connections, authentication, and
	 * session management.</p>
	 * 
	 * @return the ClientManagementStrategy
	 * @see ClientManagementStrategy
	 */
	ClientManagementStrategy getClientManagementStrategy();

	/**
	 * Returns the commands as a callable function object.
	 * 
	 * <p>This provides access to all registered commands through a unified
	 * function interface for use in expressions.</p>
	 * 
	 * @return the commands function object
	 */
	Object getCommandsAsFunction();

	/**
	 * Returns the Waldot configuration.
	 * 
	 * @return the WaldotConfiguration
	 * @see WaldotConfiguration
	 */
	WaldotConfiguration getConfiguration();

	/**
	 * Returns the console logger for command-line interface messages.
	 * 
	 * @return the console Logger
	 * @see Logger
	 */
	Logger getConsoleLogger();

	/**
	 * Returns the console strategy.
	 * 
	 * <p>This strategy handles expression evaluation and command execution
	 * through the console interface.</p>
	 * 
	 * @return the ConsoleStrategy
	 * @see ConsoleStrategy
	 */
	ConsoleStrategy getConsoleStrategy();

	/**
	 * Returns the incoming vertex of an edge.
	 * 
	 * <p>For a directed edge, returns the vertex that the edge points to.</p>
	 * 
	 * @param edge the edge to query
	 * @return the in vertex
	 * @see WaldotEdge
	 * @see WaldotVertex
	 */
	WaldotVertex getEdgeInVertex(WaldotEdge edge);

	/**
	 * Returns the edge with the given NodeId.
	 * 
	 * @param nodeId the NodeId of the edge
	 * @return the WaldotEdge, or null if not found
	 * @see WaldotEdge
	 * @see NodeId
	 */
	WaldotEdge getEdgeNode(NodeId nodeId);

	/**
	 * Returns the outgoing vertex of an edge.
	 * 
	 * <p>For a directed edge, returns the vertex that the edge originates from.</p>
	 * 
	 * @param edge the edge to query
	 * @return the out vertex
	 * @see WaldotEdge
	 * @see WaldotVertex
	 */
	WaldotVertex getEdgeOutVertex(WaldotEdge edge);

	/**
	 * Returns all properties of an edge.
	 * 
	 * @param edge the edge to query
	 * @param <DATA_TYPE> the type of property values
	 * @return list of WaldotProperty objects
	 * @see WaldotProperty
	 * @see WaldotEdge
	 */
	public <DATA_TYPE> List<WaldotProperty<DATA_TYPE>> getEdgeProperties(WaldotEdge edge);

	/**
	 * Returns a map of all edges in the graph, keyed by NodeId.
	 * 
	 * @return map of NodeId to WaldotEdge
	 * @see WaldotEdge
	 */
	Map<NodeId, WaldotEdge> getEdges();

	/**
	 * Returns edges connected to a vertex with optional filtering.
	 * 
	 * @param vertex the vertex to query
	 * @param direction the direction (OUT, IN, or BOTH)
	 * @param edgeLabels optional array of edge labels to filter
	 * @return map of NodeId to WaldotEdge
	 * @see WaldotEdge
	 * @see Direction
	 */
	Map<NodeId, WaldotEdge> getEdges(WaldotVertex vertex, Direction direction, String[] edgeLabels);

	/**
	 * Returns the total count of edges in the graph.
	 * 
	 * @return the edge count
	 */
	int getEdgesCount();

	/**
	 * Returns the event notifier for publishing events.
	 * 
	 * <p>The EventNotifier is used to publish OPC UA events to subscribed clients.</p>
	 * 
	 * @return the EventNotifier
	 * @see EventNotifier
	 */
	EventNotifier getEventBus();

	/**
	 * Returns the event factory for creating OPC UA events.
	 * 
	 * @return the EventFactory
	 * @see EventFactory
	 */
	EventFactory getEventFactory();

	/**
	 * Returns the current GraphComputer view, if in OLAP mode.
	 * 
	 * @return the WaldotGraphComputerView, or null if not in computer mode
	 * @see WaldotGraphComputerView
	 */
	WaldotGraphComputerView getGraphComputerView();

	/**
	 * Returns the underlying Gremlin graph.
	 * 
	 * <p>This provides access to the raw TinkerPop graph for advanced operations.</p>
	 * 
	 * @return the WaldotGraph (also implements Graph)
	 * @see WaldotGraph
	 */
	WaldotGraph getGremlinGraph();

	/**
	 * Returns the history strategy.
	 * 
	 * <p>This strategy handles historical data storage and retrieval for
	 * tracked properties.</p>
	 * 
	 * @return the HistoryStrategy
	 * @see HistoryStrategy
	 */
	HistoryStrategy getHistoryStrategy();

	/**
	 * Returns all registered namespace listeners.
	 * 
	 * @return collection of NamespaceListener
	 * @see NamespaceListener
	 */
	Collection<NamespaceListener> getListeners();

	/**
	 * Returns the namespace table for NodeId namespace resolution.
	 * 
	 * @return the NamespaceTable
	 * @see NamespaceTable
	 */
	NamespaceTable getNamespaceTable();

	/**
	 * Returns the namespace URI for this WaldotNamespace.
	 * 
	 * @return the namespace URI string
	 */
	public String getNamespaceUri();

	/**
	 * Returns the ID manager for NodeId generation and conversion.
	 * 
	 * @return the IdManager for NodeId
	 * @see IdManager
	 */
	IdManager<NodeId> getNodeIdManager();

	/**
	 * Returns the object type manager for OPC UA type registration.
	 * 
	 * @return the ObjectTypeManager
	 * @see ObjectTypeManager
	 */
	ObjectTypeManager getObjectTypeManager();

	/**
	 * Returns the OPC UA node context.
	 * 
	 * <p>This provides access to the OPC UA server's node context for
	 * node creation and management.</p>
	 * 
	 * @return the UaNodeContext
	 * @see UaNodeContext
	 */
	public UaNodeContext getOpcUaNodeContext();

	/**
	 * Returns the OPC UA server instance.
	 * 
	 * @return the WaldotOpcUaServer
	 * @see WaldotOpcUaServer
	 */
	WaldotOpcUaServer getOpcuaServer();

	/**
	 * Returns all registered plugins.
	 * 
	 * @return set of PluginListener
	 * @see PluginListener
	 */
	Set<PluginListener> getPlugins();

	/**
	 * Returns the edge that owns a property.
	 * 
	 * @param property the property to look up
	 * @param <DATA_TYPE> the type of the property value
	 * @return the owning WaldotEdge
	 * @see WaldotProperty
	 * @see WaldotEdge
	 */
	public <DATA_TYPE> WaldotEdge getPropertyReference(WaldotProperty<DATA_TYPE> property);

	/**
	 * Returns the reference type tree for the namespace.
	 * 
	 * @return the ReferenceTypeTree
	 * @see ReferenceTypeTree
	 */
	public ReferenceTypeTree getReferenceTypes();

	/**
	 * Returns the storage manager for OPC UA node persistence.
	 * 
	 * @return the UaNodeManager
	 * @see UaNodeManager
	 */
	UaNodeManager getStorageManager();

	/**
	 * Returns the scheduled executor for timed tasks.
	 * 
	 * <p>This executor is used for scheduled operations like property
	 * polling and periodic updates.</p>
	 * 
	 * @return the ScheduledExecutorService
	 * @see ScheduledExecutorService
	 */
	ScheduledExecutorService getTimer();

	/**
	 * Returns the graph variables for storing global data.
	 * 
	 * @return the Graph.Variables
	 * @see Graph.Variables
	 */
	Graph.Variables getVariables();

	/**
	 * Returns the vertex with the given NodeId.
	 * 
	 * @param nodeId the NodeId of the vertex
	 * @return the WaldotVertex, or null if not found
	 * @see WaldotVertex
	 * @see NodeId
	 */
	WaldotVertex getVertexNode(NodeId nodeId);

	/**
	 * Returns all properties of a vertex.
	 * 
	 * @param vertex the vertex to query
	 * @param <DATA_TYPE> the type of property values
	 * @return map of property name to WaldotVertexProperty
	 * @see WaldotVertexProperty
	 * @see WaldotVertex
	 */
	<DATA_TYPE> Map<String, WaldotVertexProperty<DATA_TYPE>> getVertexProperties(WaldotVertex vertex);

	/**
	 * Returns the vertex that owns a vertex property.
	 * 
	 * @param vertexProperty the vertex property to look up
	 * @param <DATA_TYPE> the type of the property value
	 * @return the owning WaldotVertex
	 * @see WaldotVertexProperty
	 * @see WaldotVertex
	 */
	public <DATA_TYPE> WaldotVertex getVertexPropertyReference(WaldotVertexProperty<DATA_TYPE> vertexProperty);

	/**
	 * Returns a map of all vertices in the graph, keyed by NodeId.
	 * 
	 * @return map of NodeId to WaldotVertex
	 * @see WaldotVertex
	 */
	Map<NodeId, WaldotVertex> getVertices();

	/**
	 * Returns vertices connected to a given vertex with optional filtering.
	 * 
	 * @param vertex the vertex to query
	 * @param direction the direction (OUT, IN, or BOTH)
	 * @param edgeLabels optional array of edge labels to filter
	 * @return map of NodeId to WaldotVertex
	 * @see WaldotVertex
	 * @see Direction
	 */
	Map<NodeId, WaldotVertex> getVertices(WaldotVertex vertex, Direction direction, String[] edgeLabels);

	/**
	 * Returns the total count of vertices in the graph.
	 * 
	 * @return the vertex count
	 */
	int getVerticesCount();

	/**
	 * Checks if a NodeId exists in the graph.
	 * 
	 * @param nodeId the NodeId to check
	 * @return true if the NodeId exists, false otherwise
	 * @see NodeId
	 */
	boolean hasNodeId(NodeId nodeId);

	/**
	 * Checks if the graph is in computer mode (OLAP processing).
	 * 
	 * @return true if in computer mode, false otherwise
	 */
	boolean inComputerMode();

	/**
	 * Lists all configured command names.
	 * 
	 * @return collection of command name strings
	 */
	Collection<String> listConfiguredCommands();

	/**
	 * Gets a namespace parameter value.
	 * 
	 * <p>Namespace parameters are key-value pairs stored in the graph
	 * for configuration and state management.</p>
	 * 
	 * @param key the parameter key
	 * @return the parameter value, or null if not found
	 */
	Object namespaceParametersGet(String key);

	/**
	 * Gets all namespace parameter keys.
	 * 
	 * @return set of parameter keys
	 * @see #namespaceParametersGet(String)
	 */
	Set<String> namespaceParametersKeySet();

	/**
	 * Sets a namespace parameter.
	 * 
	 * @param key the parameter key
	 * @param value the parameter value
	 */
	void namespaceParametersPut(String key, Object value);

	/**
	 * Removes a namespace parameter.
	 * 
	 * @param key the parameter key to remove
	 */
	void namespaceParametersRemove(String key);

	/**
	 * Converts all namespace parameters to graph variables.
	 * 
	 * <p>This provides a way to access namespace parameters through the
	 * graph's Variables interface.</p>
	 * 
	 * @return the Graph.Variables containing all parameters
	 */
	Variables namespaceParametersToVariables();

	/**
	 * Triggers an OPC UA event update.
	 * 
	 * @param sourceNode the source node for the event
	 * @see UaNode
	 */
	void opcuaUpdateEvent(UaNode sourceNode);

	/**
	 * Registers authentication validators for client connections.
	 * 
	 * <p>Sets up the validators for anonymous, username/password, and X.509
	 * certificate authentication.</p>
	 * 
	 * @param agentAnonymousValidator validator for anonymous access
	 * @param agentIdentityValidator validator for username/password
	 * @param agentX509IdentityValidator validator for X.509 certificates
	 * @see ClientRegisterAnonymousValidator
	 * @see ClientRegisterUsernameIdentityValidator
	 * @see ClientRegisterX509IdentityValidator
	 */
	void registerAgentValidators(ClientRegisterAnonymousValidator agentAnonymousValidator,
			ClientRegisterUsernameIdentityValidator agentIdentityValidator,
			ClientRegisterX509IdentityValidator agentX509IdentityValidator);

	/**
	 * Registers a command with the namespace.
	 * 
	 * <p>Registered commands become available through the console and can
	 * be invoked via OPC UA method calls.</p>
	 * 
	 * @param command the WaldotCommand to register
	 * @see WaldotCommand
	 * @see #removeCommand(WaldotCommand)
	 */
	void registerCommand(WaldotCommand command);

	/**
	 * Registers a plugin with the namespace.
	 * 
	 * <p>The plugin is initialized and started when registered.</p>
	 * 
	 * @param plugin the PluginListener to register
	 * @see PluginListener
	 * @see #unregisterPlugin(PluginListener)
	 */
	void registerPlugin(PluginListener plugin);

	/**
	 * Removes a command from the namespace.
	 * 
	 * @param command the WaldotCommand to remove
	 * @see #registerCommand(WaldotCommand)
	 */
	void removeCommand(WaldotCommand command);

	/**
	 * Removes an edge from the graph.
	 * 
	 * @param expandedNodeId the NodeId of the edge to remove
	 * @see WaldotEdge
	 */
	void removeEdge(NodeId expandedNodeId);

	/**
	 * Removes a namespace listener.
	 * 
	 * @param listener the NamespaceListener to remove
	 * @see #addListener(NamespaceListener)
	 */
	void removeListener(NamespaceListener listener);

	/**
	 * Removes a reference from the OPC UA address space.
	 * 
	 * @param reference the Reference to remove
	 * @see Reference
	 */
	void removeReference(Reference reference);

	/**
	 * Removes a vertex from the graph.
	 * 
	 * @param nodeId the NodeId of the vertex to remove
	 * @see WaldotVertex
	 */
	void removeVertex(NodeId nodeId);

	/**
	 * Removes a vertex property from the graph.
	 * 
	 * @param nodeId the NodeId of the property to remove
	 */
	void removeVertexProperty(NodeId nodeId);

	/**
	 * Resets the namespace to its initial state.
	 * 
	 * <p>Clears all vertices, edges, and parameters while preserving
	 * registered plugins and strategies.</p>
	 */
	void resetNameSpace();

	/**
	 * Runs a JEXL expression using the console strategy.
	 * 
	 * <p>This provides a convenient way to execute graph queries and
	 * commands through the console interface.</p>
	 * 
	 * @param expression the JEXL expression to execute
	 * @return the result of the expression
	 * @see ConsoleStrategy#runExpression(String)
	 */
	Object runExpression(String expression);

	/**
	 * Shuts down the namespace and all its components.
	 * 
	 * <p>This stops all strategies, closes connections, and releases resources.</p>
	 * 
	 * @see #startup()
	 */
	void shutdown();

	/**
	 * Starts up the namespace.
	 * 
	 * <p>This initializes all strategies and prepares the namespace for operation.</p>
	 * 
	 * @see #shutdown()
	 */
	void startup();

	/**
	 * Unregisters a plugin from the namespace.
	 * 
	 * <p>The plugin is stopped before being removed.</p>
	 * 
	 * @param plugin the PluginListener to unregister
	 * @see #registerPlugin(PluginListener)
	 */
	void unregisterPlugin(PluginListener plugin);

}
