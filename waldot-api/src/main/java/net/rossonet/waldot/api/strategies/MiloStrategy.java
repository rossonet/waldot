package net.rossonet.waldot.api.strategies;

import java.util.List;
import java.util.Map;
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
import org.eclipse.milo.opcua.stack.core.Identifiers;
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
 * WaldotMappingStrategy is an interface that defines the operations for mapping
 * and managing vertices, edges, and properties in the Waldot graph model. It
 * provides methods to add edges and vertices, create or update properties,
 * manage graph computer views, and handle namespace parameters.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface MiloStrategy extends AutoCloseable {

	public static final String ACTION_FIELD = "Action";
	public static final String ALIAS_EDGE_PARAMETER = "alias";
	public static final Predicate<Reference> COMPONENT_OF_PREDICATE = (reference) -> reference.isInverse()
			&& Identifiers.HasComponent.equals(reference.getReferenceTypeId());
	public static final String CONDITION_FIELD = "Condition";
	public static final String DEFAULT_ACTION_VALUE = "log.info('action fired')";
	public static final boolean DEFAULT_CLEAR_FACTS_AFTER_EXECUTION = false;
	public static final String DEFAULT_CONDITION_VALUE = "true";
	public static final int DEFAULT_DELAY_BEFORE_EVALUATION = 0;
	public static final int DEFAULT_DELAY_BEFORE_EXECUTE = 0;
	public static final boolean DEFAULT_PARALLEL_EXECUTION = false;
	public static final int DEFAULT_PRIORITY_VALUE = 100;
	public static final int DEFAULT_REFACTORY_PERIOD_MS = 0;
	public static final String DESCRIPTION_PARAMETER = "description";
	public static final String DIRECTORY_PARAMETER = "directory";
	public static final String HAS_WALDOT_ALIAS = "HasAlias";
	public static final String HAS_WALDOT_RULE = "HasRule";
	public static final String ID_PARAMETER = "id";
	public static final String LABEL_FIELD = "Label";
	public static final String OBSERVER_EDGE_PARAMETER = "fire";
	public static final String PRIORITY_FIELD = "Priority";
	public static final String RULE_NODE_PARAMETER = "rule";
	public static final String TYPE_DEFINITION_PARAMETER = "type-node-id";

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

	Edge addEdge(WaldotVertex sourceVertex, WaldotVertex targetVertex, String label, Object[] keyValues);

	WaldotVertex addVertex(NodeId nodeId, Object[] keyValues);

	WaldotGraphComputerView createGraphComputerView(WaldotGraph graph, GraphFilter graphFilter,
			Set<VertexComputeKey> vertexComputeKey);

	<DATA_TYPE> WaldotProperty<DATA_TYPE> createOrUpdateWaldotEdgeProperty(WaldotEdge opcEdge, String key,
			DATA_TYPE value);

	<DATA_TYPE> WaldotVertexProperty<DATA_TYPE> createOrUpdateWaldotVertexProperty(WaldotVertex opcVertex, String key,
			DATA_TYPE value);

	void dropGraphComputerView();

	UaFolderNode getAssetRootFolderNode();

	WaldotVertex getEdgeInVertex(WaldotEdge opcEdge);

	WaldotVertex getEdgeOutVertex(WaldotEdge opcEdge);

	Map<NodeId, WaldotEdge> getEdges();

	Map<NodeId, WaldotEdge> getEdges(WaldotVertex opcVertex, Direction direction, String[] edgeLabels);

	<DATA_TYPE> List<WaldotProperty<DATA_TYPE>> getProperties(WaldotEdge edge);

	<DATA_TYPE> WaldotEdge getPropertyReference(WaldotProperty<DATA_TYPE> opcProperty);

	UaFolderNode getRootFolderNode();

	<DATA_TYPE> Map<String, WaldotVertexProperty<DATA_TYPE>> getVertexProperties(WaldotVertex opcVertex);

	<DATA_TYPE> WaldotVertex getVertexPropertyReference(WaldotVertexProperty<DATA_TYPE> opcVertexProperty);

	Map<NodeId, WaldotVertex> getVertices();

	Map<NodeId, WaldotVertex> getVertices(WaldotVertex opcVertex, Direction direction, String[] edgeLabels);

	WaldotNamespace initialize(WaldotNamespace waldotNamespace);

	Object namespaceParametersGet(String key);

	Set<String> namespaceParametersKeySet();

	void namespaceParametersPut(String key, Object value);

	void namespaceParametersRemove(String key);

	Graph.Variables namespaceParametersToVariables();

	void registerCommand(WaldotCommand command);

	void removeCommand(WaldotCommand command);

	void removeEdge(NodeId nodeId);

	void removeVertex(NodeId nodeId);

	void removeVertexProperty(NodeId nodeId);

	void resetNameSpace();

	void updateEventGenerator(Node sourceNode, String eventName, String eventDisplayName, String message, int severity);

}
