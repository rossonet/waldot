package net.rossonet.waldot.gremlin.opcgraph.strategies.opcua;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.opc.AbstractOpcCommand;
import net.rossonet.waldot.opc.AbstractOpcCommand.VariableNodeTypes;

public class MiloSingleServerBaseFolderManager {
	private AbstractOpcCommand addEdgeCommand;
	private AbstractOpcCommand addVertexCommand;
	private final Map<String, UaFolderNode> commandDirectoriesAndObjects = new HashMap<>();
	private UaFolderNode edgesFolderNode;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final MiloSingleServerBaseStrategy miloSingleServerBaseV0Strategy;
	private UaFolderNode variablesFolderNode;
	private final Map<String, UaFolderNode> vertexDirectories = new HashMap<>();
	private UaFolderNode verticesFolderNode;

	public MiloSingleServerBaseFolderManager(MiloSingleServerBaseStrategy miloSingleServerBaseV0Strategy) {
		this.miloSingleServerBaseV0Strategy = miloSingleServerBaseV0Strategy;
	}

	private UaFolderNode createEdgesFolder() {
		final UaFolderNode edgeFolder = new UaFolderNode(
				miloSingleServerBaseV0Strategy.getWaldotNamespace().getOpcUaNodeContext(),
				miloSingleServerBaseV0Strategy.getWaldotNamespace()
						.generateNodeId(miloSingleServerBaseV0Strategy.getRootFolderNode().getNodeId().getIdentifier()
								.toString() + "/Edges"),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().generateQualifiedName("Edges"),
				LocalizedText.english("Gremlin Edges"));
		addEdgeCommand = new AbstractOpcCommand(miloSingleServerBaseV0Strategy.getWaldotNamespace().getGremlinGraph(),
				miloSingleServerBaseV0Strategy.getWaldotNamespace(),
				miloSingleServerBaseV0Strategy.getRootFolderNode().getNodeId().getIdentifier().toString()
						+ "/Edges/add",
				"add edge", "add edge to Gremlin graph", null,
				miloSingleServerBaseV0Strategy.getWaldotNamespace().getConfiguration().getWaldotCommandWriteMask(),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().getConfiguration().getWaldotCommandUserWriteMask(),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().getConfiguration().getWaldotCommandExecutable(),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().getConfiguration()
						.getWaldotCommandUserExecutable()) {

			@Override
			public Object[] runCommand(InvocationContext invocationContext, String[] inputValues) {
				String output = "";
				String error = "";
				try {
					final String type = inputValues[0];
					final String sourceNode = inputValues[1];
					final WaldotVertex sourceVertex = miloSingleServerBaseV0Strategy.getWaldotNamespace()
							.getVertexNode(NodeId.parse(sourceNode));
					final String destinationNode = inputValues[2];
					final WaldotVertex destinationVertex = miloSingleServerBaseV0Strategy.getWaldotNamespace()
							.getVertexNode(NodeId.parse(destinationNode));
					final Object[] keyValues = inputValues.length > 2 && inputValues[3] != null
							&& inputValues[3].contains(",") ? inputValues[3].split(",") : new String[0];
					if (keyValues.length > 0) {
						ElementHelper.legalPropertyKeyValueArray(keyValues);
					}
					final WaldotEdge edge = waldotNamespace.addEdge(sourceVertex, destinationVertex, type, keyValues);
					output = edge.toString();
				} catch (final Exception e) {
					logger.error("Error adding edge", e);
					error = e.getMessage();
				}
				return new String[] { output, error };
			}
		};
		addEdgeCommand.addOutputArgument("output", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command output"));
		addEdgeCommand.addOutputArgument("error", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command error"));
		addEdgeCommand.addInputArgument("label", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("edge type to add"));
		addEdgeCommand.addInputArgument("source", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("node id of source vertex"));
		addEdgeCommand.addInputArgument("destination", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("node id of destination vertex"));
		addEdgeCommand.addInputArgument("keyValues", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("key values of edge to add, separated by comma"));
		miloSingleServerBaseV0Strategy.getWaldotNamespace().getStorageManager().addNode(addEdgeCommand);
		edgeFolder.addComponent(addEdgeCommand);
		return edgeFolder;
	}

	private UaFolderNode createVariablesFolder() {
		return new UaFolderNode(miloSingleServerBaseV0Strategy.getWaldotNamespace().getOpcUaNodeContext(),
				miloSingleServerBaseV0Strategy.getWaldotNamespace()
						.generateNodeId(miloSingleServerBaseV0Strategy.getRootFolderNode().getNodeId().getIdentifier()
								.toString() + "/Variables"),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().generateQualifiedName("Variables"),
				LocalizedText.english("Gremlin Variables"));
	}

	private UaFolderNode createVerticesFolder() {
		final UaFolderNode vertexFolder = new UaFolderNode(
				miloSingleServerBaseV0Strategy.getWaldotNamespace().getOpcUaNodeContext(),
				miloSingleServerBaseV0Strategy.getWaldotNamespace()
						.generateNodeId(miloSingleServerBaseV0Strategy.getRootFolderNode().getNodeId().getIdentifier()
								.toString() + "/Vertices"),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().generateQualifiedName("Vertices"),
				LocalizedText.english("Gremlin Vertices"));
		addVertexCommand = new AbstractOpcCommand(miloSingleServerBaseV0Strategy.getWaldotNamespace().getGremlinGraph(),
				miloSingleServerBaseV0Strategy.getWaldotNamespace(),
				miloSingleServerBaseV0Strategy.getRootFolderNode().getNodeId().getIdentifier().toString()
						+ "/Vertices/add",
				"add vertex", "add vertex to Gremlin graph", null,
				miloSingleServerBaseV0Strategy.getWaldotNamespace().getConfiguration().getWaldotCommandWriteMask(),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().getConfiguration().getWaldotCommandUserWriteMask(),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().getConfiguration().getWaldotCommandExecutable(),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().getConfiguration()
						.getWaldotCommandUserExecutable()) {

			@Override
			public Object[] runCommand(InvocationContext invocationContext, String[] inputValues) {
				String output = "";
				String error = "";
				try {
					final String nodeId = inputValues[0];
					NodeId node = null;
					if (nodeId == null || nodeId.isEmpty()) {
						node = miloSingleServerBaseV0Strategy.getWaldotNamespace().getNodeIdManager().getNextId(graph);
					} else {
						node = miloSingleServerBaseV0Strategy.getWaldotNamespace().generateNodeId(nodeId);
					}

					final String label = inputValues[1];
					final String type = inputValues[2];
					final List<String> keyValues = new ArrayList<>();
					keyValues.addAll((inputValues.length > 2 && inputValues[3] != null && inputValues[3].contains(","))
							? Arrays.asList(inputValues[3].split(","))
							: new ArrayList<>());
					if (type != null && !type.isEmpty() && !keyValues.contains(MiloStrategy.TYPE_FIELD.toLowerCase())) {
						keyValues.add(MiloStrategy.TYPE_FIELD.toLowerCase());
						keyValues.add(type);
					}
					if (label != null && !label.isEmpty()
							&& !keyValues.contains(MiloStrategy.LABEL_FIELD.toLowerCase())) {
						keyValues.add(MiloStrategy.LABEL_FIELD.toLowerCase());
						keyValues.add(label);
					}
					if (!keyValues.isEmpty()) {
						ElementHelper.legalPropertyKeyValueArray(keyValues.toArray());
					}
					final WaldotVertex vertex = waldotNamespace.addVertex(node, keyValues.toArray());
					output = vertex.toString();
				} catch (final Exception e) {
					logger.error("Error adding vertex", e);
					error = e.getMessage();
				}
				return new String[] { output, error };
			}
		};
		addVertexCommand.addOutputArgument("output", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command output"));
		addVertexCommand.addOutputArgument("error", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command error"));
		addVertexCommand.addInputArgument("nodeId", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("vertex nodeId to add"));
		addVertexCommand.addInputArgument("label", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("vertex label to add"));
		addVertexCommand.addInputArgument("type", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("vertex type to add"));
		addVertexCommand.addInputArgument("keyValues", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("key values of vertex to add, separated by comma"));
		miloSingleServerBaseV0Strategy.getWaldotNamespace().getStorageManager().addNode(addVertexCommand);
		vertexFolder.addComponent(addVertexCommand);
		return vertexFolder;
	}

	public Map<String, UaFolderNode> getCommandDirectories() {
		return this.commandDirectoriesAndObjects;
	}

	public UaFolderNode getEdgesFolderNode() {
		return edgesFolderNode;
	}

	public UaFolderNode getVariablesFolderNode() {
		return variablesFolderNode;
	}

	public Map<String, UaFolderNode> getVertexDirectories() {
		return vertexDirectories;
	}

	public UaFolderNode getVerticesFolderNode() {
		return verticesFolderNode;
	}

	public void initialize() {
		setVerticesFolderNode(createVerticesFolder());
		miloSingleServerBaseV0Strategy.getWaldotNamespace().getStorageManager().addNode(getVerticesFolderNode());
		miloSingleServerBaseV0Strategy.getRootFolderNode().addOrganizes(getVerticesFolderNode());
		setEdgesFolderNode(createEdgesFolder());
		miloSingleServerBaseV0Strategy.getWaldotNamespace().getStorageManager().addNode(getEdgesFolderNode());
		miloSingleServerBaseV0Strategy.getRootFolderNode().addOrganizes(getEdgesFolderNode());
		setVariablesFolderNode(createVariablesFolder());
		miloSingleServerBaseV0Strategy.getWaldotNamespace().getStorageManager().addNode(getVariablesFolderNode());
		miloSingleServerBaseV0Strategy.getRootFolderNode().addOrganizes(getVariablesFolderNode());

	}

	public void setEdgesFolderNode(UaFolderNode edgesFolderNode) {
		this.edgesFolderNode = edgesFolderNode;
	}

	public void setVariablesFolderNode(UaFolderNode variablesFolderNode) {
		this.variablesFolderNode = variablesFolderNode;
	}

	public void setVerticesFolderNode(UaFolderNode verticesFolderNode) {
		this.verticesFolderNode = verticesFolderNode;
	}

}
