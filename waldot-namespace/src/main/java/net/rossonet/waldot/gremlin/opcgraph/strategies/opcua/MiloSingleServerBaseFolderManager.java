package net.rossonet.waldot.gremlin.opcgraph.strategies.opcua;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;

public class MiloSingleServerBaseFolderManager {

	private final Map<String, UaFolderNode> commandDirectoriesAndObjects = new HashMap<>();
	private UaFolderNode edgesFolderNode;
	private final MiloSingleServerBaseStrategy miloSingleServerBaseV0Strategy;
	private UaFolderNode variablesFolderNode;
	private final Map<String, UaFolderNode> vertexDirectories = new HashMap<>();
	private UaFolderNode verticesFolderNode;

	public MiloSingleServerBaseFolderManager(MiloSingleServerBaseStrategy miloSingleServerBaseV0Strategy) {
		this.miloSingleServerBaseV0Strategy = miloSingleServerBaseV0Strategy;
	}

	private UaFolderNode createEdgesFolder() {
		return new UaFolderNode(miloSingleServerBaseV0Strategy.getWaldotNamespace().getOpcUaNodeContext(),
				miloSingleServerBaseV0Strategy.getWaldotNamespace()
						.generateNodeId(miloSingleServerBaseV0Strategy.getRootFolderNode().getNodeId().getIdentifier()
								.toString() + "/Edges"),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().generateQualifiedName("Edges"),
				LocalizedText.english("Gremlin Edges"));
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
		return new UaFolderNode(miloSingleServerBaseV0Strategy.getWaldotNamespace().getOpcUaNodeContext(),
				miloSingleServerBaseV0Strategy.getWaldotNamespace()
						.generateNodeId(miloSingleServerBaseV0Strategy.getRootFolderNode().getNodeId().getIdentifier()
								.toString() + "/Vertices"),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().generateQualifiedName("Vertices"),
				LocalizedText.english("Gremlin Vertices"));
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
