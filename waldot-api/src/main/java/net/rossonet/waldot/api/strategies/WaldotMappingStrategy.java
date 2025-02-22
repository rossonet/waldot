package net.rossonet.waldot.api.strategies;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tinkerpop.gremlin.process.computer.GraphFilter;
import org.apache.tinkerpop.gremlin.process.computer.VertexComputeKey;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.api.models.IdManager;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotGraphComputerView;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotProperty;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.models.WaldotVertexProperty;

public interface WaldotMappingStrategy {

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

			private NodeId getNodeIdFromNumber(final WaldotGraph graph, Integer nodeId) {
				return graph.getWaldotNamespace().generateNodeId("i=" + Integer.valueOf(nodeId));
			}

			private NodeId getNodeIdFromString(final WaldotGraph graph, String nodeId) {
				try {
					return NodeId.parse(nodeId);
				} catch (final Exception e) {
					return graph.getWaldotNamespace().generateNodeId("s=" + nodeId);
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

	WaldotVertex getEdgeInVertex(WaldotEdge opcEdge);

	WaldotVertex getEdgeOutVertex(WaldotEdge opcEdge);

	Map<NodeId, WaldotEdge> getEdges();

	Map<NodeId, WaldotEdge> getEdges(WaldotVertex opcVertex, Direction direction, String[] edgeLabels);

	<DATA_TYPE> List<WaldotProperty<DATA_TYPE>> getProperties(WaldotEdge edge);

	<DATA_TYPE> WaldotEdge getPropertyReference(WaldotProperty<DATA_TYPE> opcProperty);

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

}
