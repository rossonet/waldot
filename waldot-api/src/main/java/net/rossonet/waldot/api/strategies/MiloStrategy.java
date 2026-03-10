package net.rossonet.waldot.api.strategies;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.tinkerpop.gremlin.process.computer.GraphFilter;
import org.apache.tinkerpop.gremlin.process.computer.VertexComputeKey;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.nodes.Node;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.api.models.IdManager;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotGraphComputerView;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotProperty;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.models.WaldotVertexProperty;

/**
 * MiloStrategy is an interface that defines the operations for mapping
 * and managing vertices, edges, and properties in the Waldot graph model. It
 * provides methods to add edges and vertices, create or update properties,
 * manage graph computer views, and handle namespace parameters.
 * 
 * <p>MiloStrategy is the core strategy for OPC UA integration in WaldOT. It
 * handles the mapping between the TinkerPop graph structure and the OPC UA
 * address space. This includes:</p>
 * <ul>
 *   <li>Vertex and edge creation</li>
 *   <li>Property management</li>
 *   <li>OPC UA node management</li>
 *   <li>Reference handling</li>
 *   <li>GraphComputer view management</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Initialize strategy
 * MiloStrategy strategy = new MyMiloStrategy();
 * strategy.initialize(waldotNamespace);
 * 
 * // Add a vertex
 * WaldotVertex vertex = strategy.addVertex(
 *     namespace.generateNodeId("device:001"),
 *     new Object[]{"label", "sensor", "type", "temperature"}
 * );
 * 
 * // Add an edge
 * WaldotVertex target = strategy.addVertex(
 *     namespace.generateNodeId("device:002"),
 *     new Object[]{"label", "controller"}
 * );
 * WaldotEdge edge = strategy.addEdge(vertex, target, "controls", null);
 * 
 * // Set properties
 * strategy.createOrUpdateWaldotVertexProperty(vertex, "value", 25.5);
 * 
 * // Query elements
 * Map<NodeId, WaldotVertex> vertices = strategy.getVertices();
 * Map<NodeId, WaldotEdge> edges = strategy.getEdges();
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface MiloStrategy extends AutoCloseable {
	/**
	 * Predicate for filtering component references.
	 */
	public static final Predicate<Reference> COMPONENT_OF_PREDICATE = (reference) -> reference.isInverse()
			&& NodeIds.HasComponent.equals(reference.getReferenceTypeId());
	/**
	 * Default edge type label.
	 */
	public static final String DEFAULT_EDGE_TYPE = "edge";
	/**
	 * Default vertex label.
	 */
	public static final String DEFAULT_VERTEX_LABEL = "vertex";
	/**
	 * Parameter name for description.
	 */
	public static final String DESCRIPTION_PARAMETER = "description";
	/**
	 * Parameter name for directory.
	 */
	public static final String DIRECTORY_PARAMETER = "directory";
	/**
	 * Directory path separator.
	 */
	public static final String DIRECTORY_SPLIT_SIMBOL = "/";
	/**
	 * Parameter name for event notifier.
	 */
	public static final String EVENT_NOTIFIER_PARAMETER = "event-notifier";
	/**
	 * Edge type for fire events.
	 */
	public static final String FIRE_EDGE_TYPE = "fire";
	/**
	 * Directory for general commands.
	 */
	public static final String GENERAL_CMD_DIRECTORY = "general";
	/**
	 * Field name for history context.
	 */
	public static final String HISTORY_CONTEXT_FIELD = "HistoryContext";
	/**
	 * Field name for history flag.
	 */
	public static final String HISTORY_FIELD = "IsHistory";
	/**
	 * Parameter name for ID.
	 */
	public static final String ID_PARAMETER = "id";

	/**
	 * Field name for forward reference flag.
	 */
	public static final String IS_FORWARD = "IsForward";
	/**
	 * Field name for label.
	 */
	public static final String LABEL_FIELD = "Label";
	/**
	 * Edge type for link-from references.
	 */
	public static final String LINK_FROM_EDGE_TYPE = "link-from";
	/**
	 * Edge type for link-to references.
	 */
	public static final String LINK_TO_EDGE_TYPE = "link-to";
	/**
	 * Default priority value for monitored edges.
	 */
	public static final int MONITOR_EDGE_DEFAULT_PRIORITY_VALUE = 10;
	/**
	 * Field name for monitor edge priority.
	 */
	public static final String MONITOR_EDGE_PRIORITY_FIELD = "priority";
	/**
	 * Field name for name.
	 */
	public static final String NAME_FIELD = "Name";
	/**
	 * Property separator in NodeId strings.
	 */
	public static final String PROPERTY_SPLIT_SIMBOL_IN_NODEID = "/";
	/**
	 * Parameter name for reference type.
	 */
	public static final String REFERENCE_TYPE = "ReferenceType";
	/**
	 * Field name for source node.
	 */
	public static final String SOURCE_NODE = "SourceNode";
	/**
	 * Field name for target node.
	 */
	public static final String TARGET_NODE = "TargetNode";
	/**
	 * Field name for type.
	 */
	public static final String TYPE_FIELD = "Type";
	/**
	 * Parameter name for user write mask.
	 */
	public static final String USER_WRITE_MASK_PARAMETER = "user-write-mask";
	/**
	 * Parameter name for version.
	 */
	public static final String VERSION_PARAMETER = "version";
	/**
	 * Parameter name for write mask.
	 */
	public static final String WRITE_MASK_PARAMETER = "write-mask";

	/**
	 * Extracts the ID value from a key-value array.
	 * 
	 * <p>Looks for a key-value pair with the key "id" and returns its value.</p>
	 * 
	 * @param keyValues array of key-value pairs
	 * @return Optional containing the ID value if found
	 */
	public static Optional<Object> getIdValue(final Object... keyValues) {
		for (int i = 0; i < keyValues.length; i = i + 2) {
			if (keyValues[i] == null) {
				continue;
			}
			final String name = keyValues[i].toString();
			if (name != null && !name.isEmpty() && name.equals(MiloStrategy.ID_PARAMETER)) {
				return Optional.ofNullable(keyValues[i + 1]);
			}
		}
		return Optional.empty();
	}

	/**
	 * Extracts a property value from a key-value array.
	 * 
	 * @param propertyKeyValues array of key-value pairs
	 * @param label the property label to find
	 * @return the property value as String, or null if not found
	 */
	public static String getKeyValuesProperty(final Object[] propertyKeyValues, final String label) {
		for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
			if (propertyKeyValues[i] == null) {
				continue;
			}
			final String name = propertyKeyValues[i].toString();
			if (name != null && !name.isEmpty() && label.equals(name)) {
				return propertyKeyValues[i + 1].toString();
			}
		}
		return null;
	}

	/**
	 * Creates a default NodeId IdManager.
	 * 
	 * <p>This provides ID management for OPC UA NodeIds, handling conversion
	 * from various formats (String, Integer, NodeId) to NodeId objects.</p>
	 * 
	 * @return IdManager for NodeId
	 */
	static IdManager<NodeId> getNodeIdManager() {
		return new IdManager<NodeId>() {

			@Override
			public boolean allow(final WaldotGraph graph, final Object id) {
				if (id instanceof NodeId) {
					return true;
				} else if (id instanceof String) {
					try {
						if (convert(graph, id) != null) {
							return true;
						} else {
							return false;
						}
					} catch (final Exception iae) {
						return false;
					}
				} else {
					return false;
				}
			}

			@Override
			public NodeId convert(final WaldotGraph graph, final Object id) {
				if (null == id) {
					return null;
				} else if (id instanceof NodeId) {
					return (NodeId) id;
				} else if (id instanceof String) {
					return getNodeIdFromString(graph, id.toString());
				} else if (id instanceof Integer) {
					return getNodeIdFromNumber(graph, (Integer) id);
				} else {
					try {
						return getNodeIdFromString(graph, id.toString());
					} catch (final Exception e) {
						return null;
					}
				}
			}

			@Override
			public NodeId getNextId(final WaldotGraph graph) {
				boolean found = false;
				while (!found) {
					final NodeId id = graph.getWaldotNamespace().generateNodeId(graph.getGeneratedId());
					if (!graph.getWaldotNamespace().hasNodeId(id)) {
						found = true;
						return id;
					}
				}
				return null;
			}

			private NodeId getNodeIdFromNumber(final WaldotGraph graph, final Integer nodeId) {
				return graph.getWaldotNamespace().generateNodeId(UInteger.valueOf(nodeId));
			}

			private NodeId getNodeIdFromString(final WaldotGraph graph, final String nodeId) {
				try {
					return NodeId.parse(nodeId);
				} catch (final Exception e) {
					return graph.getWaldotNamespace().generateNodeId(nodeId);
				}
			}

		};
	}

	/**
	 * Adds a new edge to the graph.
	 * 
	 * @param sourceVertex the source vertex
	 * @param targetVertex the target vertex
	 * @param label the edge label
	 * @param keyValues optional property key-value pairs
	 * @return the created Edge
	 * @see Edge
	 */
	Edge addEdge(WaldotVertex sourceVertex, WaldotVertex targetVertex, String label, Object[] keyValues);

	/**
	 * Adds a new vertex to the graph.
	 * 
	 * @param nodeId the NodeId for the vertex
	 * @param keyValues optional property key-value pairs
	 * @return the created WaldotVertex
	 * @see WaldotVertex
	 */
	WaldotVertex addVertex(NodeId nodeId, Object[] keyValues);

	/**
	 * Creates a GraphComputer view for OLAP processing.
	 * 
	 * @param graph the WaldotGraph
	 * @param graphFilter the GraphFilter for filtering
	 * @param vertexComputeKey the compute keys
	 * @return the created WaldotGraphComputerView
	 * @see WaldotGraphComputerView
	 */
	WaldotGraphComputerView createGraphComputerView(WaldotGraph graph, GraphFilter graphFilter,
			Set<VertexComputeKey> vertexComputeKey);

	/**
	 * Creates or updates a property on an edge.
	 * 
	 * @param opcEdge the edge to modify
	 * @param key the property key
	 * @param value the property value
	 * @param <DATA_TYPE> the type of the property value
	 * @return the created or updated WaldotProperty
	 * @see WaldotProperty
	 */
	<DATA_TYPE> WaldotProperty<DATA_TYPE> createOrUpdateWaldotEdgeProperty(WaldotEdge opcEdge, String key,
			DATA_TYPE value);

	/**
	 * Creates or updates a property on a vertex.
	 * 
	 * @param opcVertex the vertex to modify
	 * @param key the property key
	 * @param value the property value
	 * @param <DATA_TYPE> the type of the property value
	 * @return the created or updated WaldotVertexProperty
	 * @see WaldotVertexProperty
	 */
	<DATA_TYPE> WaldotVertexProperty<DATA_TYPE> createOrUpdateWaldotVertexProperty(WaldotVertex opcVertex, String key,
			DATA_TYPE value);

	/**
	 * Deletes an OPC UA node by its NodeId string.
	 * 
	 * @param nodeId the NodeId string to delete
	 * @return status message
	 */
	String deleteOpcNodeId(String nodeId);

	/**
	 * Drops the GraphComputer view.
	 * 
	 * @see #createGraphComputerView(WaldotGraph, GraphFilter, Set)
	 */
	void dropGraphComputerView();

	/**
	 * Returns the root folder node for assets.
	 * 
	 * @return the UaFolderNode for assets
	 * @see UaFolderNode
	 */
	UaFolderNode getAssetRootFolderNode();

	/**
	 * Returns the incoming vertex of an edge.
	 * 
	 * @param opcEdge the edge to query
	 * @return the in vertex
	 * @see WaldotVertex
	 */
	WaldotVertex getEdgeInVertex(WaldotEdge opcEdge);

	/**
	 * Returns the outgoing vertex of an edge.
	 * 
	 * @param opcEdge the edge to query
	 * @return the out vertex
	 * @see WaldotVertex
	 */
	WaldotVertex getEdgeOutVertex(WaldotEdge opcEdge);

	/**
	 * Returns all edges in the graph.
	 * 
	 * @return map of NodeId to WaldotEdge
	 */
	Map<NodeId, WaldotEdge> getEdges();

	/**
	 * Returns edges connected to a vertex.
	 * 
	 * @param opcVertex the vertex to query
	 * @param direction the direction to traverse
	 * @param edgeLabels optional edge label filter
	 * @return map of NodeId to WaldotEdge
	 */
	Map<NodeId, WaldotEdge> getEdges(WaldotVertex opcVertex, Direction direction, String[] edgeLabels);

	/**
	 * Returns all properties of an edge.
	 * 
	 * @param edge the edge to query
	 * @param <DATA_TYPE> the type of property values
	 * @return list of WaldotProperty
	 */
	<DATA_TYPE> List<WaldotProperty<DATA_TYPE>> getProperties(WaldotEdge edge);

	/**
	 * Returns the edge that owns a property.
	 * 
	 * @param opcProperty the property to look up
	 * @param <DATA_TYPE> the type of the property value
	 * @return the owning WaldotEdge
	 */
	<DATA_TYPE> WaldotEdge getPropertyReference(WaldotProperty<DATA_TYPE> opcProperty);

	/**
	 * Returns the root folder node.
	 * 
	 * @return the UaFolderNode
	 * @see UaFolderNode
	 */
	UaFolderNode getRootFolderNode();

	/**
	 * Returns all properties of a vertex.
	 * 
	 * @param opcVertex the vertex to query
	 * @param <DATA_TYPE> the type of property values
	 * @return map of property name to WaldotVertexProperty
	 */
	<DATA_TYPE> Map<String, WaldotVertexProperty<DATA_TYPE>> getVertexProperties(WaldotVertex opcVertex);

	/**
	 * Returns the vertex that owns a property.
	 * 
	 * @param opcVertexProperty the vertex property to look up
	 * @param <DATA_TYPE> the type of the property value
	 * @return the owning WaldotVertex
	 */
	<DATA_TYPE> WaldotVertex getVertexPropertyReference(WaldotVertexProperty<DATA_TYPE> opcVertexProperty);

	/**
	 * Returns all vertices in the graph.
	 * 
	 * @return map of NodeId to WaldotVertex
	 */
	Map<NodeId, WaldotVertex> getVertices();

	/**
	 * Returns vertices connected to a vertex.
	 * 
	 * @param opcVertex the vertex to query
	 * @param direction the direction to traverse
	 * @param edgeLabels optional edge label filter
	 * @return map of NodeId to WaldotVertex
	 */
	Map<NodeId, WaldotVertex> getVertices(WaldotVertex opcVertex, Direction direction, String[] edgeLabels);

	/**
	 * Returns the WaldotNamespace.
	 * 
	 * @return the namespace
	 */
	WaldotNamespace getWaldotNamespace();

	/**
	 * Initializes the strategy with a namespace.
	 * 
	 * @param waldotNamespace the namespace to use
	 * @return the initialized namespace (for chaining)
	 */
	WaldotNamespace initialize(WaldotNamespace waldotNamespace);

	/**
	 * Gets a namespace parameter.
	 * 
	 * @param key the parameter key
	 * @return the parameter value
	 */
	Object namespaceParametersGet(String key);

	/**
	 * Gets all namespace parameter keys.
	 * 
	 * @return set of keys
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
	 * @param key the parameter key
	 */
	void namespaceParametersRemove(String key);

	/**
	 * Converts namespace parameters to graph variables.
	 * 
	 * @return the Graph.Variables
	 */
	Graph.Variables namespaceParametersToVariables();

	/**
	 * Registers a command.
	 * 
	 * @param command the command to register
	 * @see WaldotCommand
	 */
	void registerCommand(WaldotCommand command);

	/**
	 * Removes a command.
	 * 
	 * @param command the command to remove
	 */
	void removeCommand(WaldotCommand command);

	/**
	 * Removes an edge by NodeId.
	 * 
	 * @param nodeId the NodeId of the edge
	 */
	void removeEdge(NodeId nodeId);

	/**
	 * Removes a reference.
	 * 
	 * @param reference the reference to remove
	 * @see Reference
	 */
	void removeReference(Reference reference);

	/**
	 * Removes a vertex by NodeId.
	 * 
	 * @param nodeId the NodeId of the vertex
	 */
	void removeVertex(NodeId nodeId);

	/**
	 * Removes a vertex property by NodeId.
	 * 
	 * @param nodeId the NodeId of the property
	 */
	void removeVertexProperty(NodeId nodeId);

	/**
	 * Resets the namespace to initial state.
	 * 
	 */
	void resetNameSpace();

	/**
	 * Updates an event generator configuration.
	 * 
	 * @param sourceNode the source node
	 * @param eventName the event name
	 * @param eventDisplayName the display name
	 * @param message the message
	 * @param severity the severity level
	 * @see Node
	 */
	void updateEventGenerator(Node sourceNode, String eventName, String eventDisplayName, String message, int severity);

}
