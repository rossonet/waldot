package net.rossonet.waldot.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tinkerpop.gremlin.process.computer.GraphFilter;
import org.apache.tinkerpop.gremlin.process.computer.VertexComputeKey;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;

import com.google.common.collect.ImmutableMap;

import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcGraphComputerView;
import net.rossonet.waldot.gremlin.opcgraph.structure.AbstractOpcGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.AbstractOpcGraph.IdManager;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcEdge;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraphVariables;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcProperty;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertex;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertexProperty;
import net.rossonet.waldot.namespaces.HomunculusNamespace;
import net.rossonet.waldot.opc.AbstractWaldotCommand;

public interface OpcMappingStrategy {

	static IdManager<NodeId> getNodeIdManager() {
		return new IdManager<NodeId>() {

			@Override
			public boolean allow(final AbstractOpcGraph graph, final Object id) {
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
			public NodeId convert(final AbstractOpcGraph graph, final Object id) {
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
			public NodeId getNextId(final AbstractOpcGraph graph) {
				boolean found = false;
				while (!found) {
					final NodeId id = graph.getOpcNamespace().generateNodeId(graph.getGeneratedId().incrementAndGet());
					if (!graph.getOpcNamespace().hasNodeId(id)) {
						found = true;
						return id;
					}
				}
				return null;
			}

			private NodeId getNodeIdFromNumber(final AbstractOpcGraph graph, Integer nodeId) {
				return graph.getOpcNamespace().generateNodeId("i=" + Integer.valueOf(nodeId));
			}

			private NodeId getNodeIdFromString(final AbstractOpcGraph graph, String nodeId) {
				try {
					return NodeId.parse(nodeId);
				} catch (final Exception e) {
					return graph.getOpcNamespace().generateNodeId("s=" + nodeId);
				}
			}

		};
	}

	Edge addEdge(OpcVertex sourceVertex, OpcVertex targetVertex, String label, Object[] keyValues);

	OpcVertex addVertex(NodeId nodeId, Object[] keyValues);

	OpcGraphComputerView createGraphComputerView(AbstractOpcGraph graph, GraphFilter graphFilter,
			Set<VertexComputeKey> vertexComputeKey);

	<DATA_TYPE> OpcProperty<DATA_TYPE> createOrUpdateOpcEdgeProperty(OpcEdge opcEdge, String key, DATA_TYPE value);

	<DATA_TYPE> OpcVertexProperty<DATA_TYPE> createOrUpdateOpcVertexProperty(OpcVertex opcVertex, String key,
			DATA_TYPE value);

	void dropGraphComputerView();

	ImmutableMap<String, WaldotCommand> getCommandRegistry();

	OpcVertex getEdgeInVertex(OpcEdge opcEdge);

	OpcVertex getEdgeOutVertex(OpcEdge opcEdge);

	<DATA_TYPE> List<OpcProperty<DATA_TYPE>> getEdgeProperties(OpcEdge opcEdge);

	Map<NodeId, Edge> getEdges();

	Map<NodeId, Edge> getEdges(OpcVertex opcVertex, Direction direction, String[] edgeLabels);

	<DATA_TYPE> OpcEdge getPropertyReference(OpcProperty<DATA_TYPE> opcProperty);

	<DATA_TYPE> Map<String, OpcVertexProperty<DATA_TYPE>> getVertexProperties(OpcVertex opcVertex);

	<DATA_TYPE> OpcVertex getVertexPropertyReference(OpcVertexProperty<DATA_TYPE> opcVertexProperty);

	Map<NodeId, Vertex> getVertices();

	Map<NodeId, Vertex> getVertices(OpcVertex opcVertex, Direction direction, String[] edgeLabels);

	HomunculusNamespace initialize(HomunculusNamespace waldotNamespace);

	Object namespaceParametersGet(String key);

	Set<String> namespaceParametersKeySet();

	void namespaceParametersPut(String key, Object value);

	void namespaceParametersRemove(String key);

	OpcGraphVariables namespaceParametersToVariables();

	void registerCommand(WaldotCommand command);

	void registerCommandInputArgument(AbstractWaldotCommand waldotCommand, List<Argument> inputArguments);

	void registerCommandOutputArguments(AbstractWaldotCommand waldotCommand, List<Argument> outputArguments);

	void removeCommand(WaldotCommand command);

	void removeEdge(NodeId nodeId);

	void removeVertex(NodeId nodeId);

	void removeVertexProperty(NodeId nodeId);

	void resetNameSpace();

}
