package net.rossonet.waldot.gremlin.opcgraph.strategies.opcua;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;

public class MiloSingleServerBaseV0FolderManager {

	private UaFolderNode edgesFolderNode;
	private final MiloSingleServerBaseV0Strategy miloSingleServerBaseV0Strategy;
	private final Map<String, UaFolderNode> rulesDirectories = new HashMap<>();
	private UaFolderNode rulesFolderNode;
	private UaFolderNode variablesFolderNode;

	private final Map<String, UaFolderNode> vertexDirectories = new HashMap<>();

	private UaFolderNode verticesFolderNode;

	public MiloSingleServerBaseV0FolderManager(MiloSingleServerBaseV0Strategy miloSingleServerBaseV0Strategy) {
		this.miloSingleServerBaseV0Strategy = miloSingleServerBaseV0Strategy;
	}

	private UaFolderNode createEdgesFolder() {
		return new UaFolderNode(miloSingleServerBaseV0Strategy.getWaldotNamespace().getOpcUaNodeContext(),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().generateNodeId(
						miloSingleServerBaseV0Strategy.getRootNode().getNodeId().getIdentifier().toString() + "/Edges"),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().generateQualifiedName("Edges"),
				LocalizedText.english("Gremlin Edges"));
	}

	private UaFolderNode createRulesFolder() {
		return new UaFolderNode(miloSingleServerBaseV0Strategy.getWaldotNamespace().getOpcUaNodeContext(),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().generateNodeId(
						miloSingleServerBaseV0Strategy.getRootNode().getNodeId().getIdentifier().toString() + "/Rules"),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().generateQualifiedName("Rules"),
				LocalizedText.english("WaldoOT Rules"));
	}

	private UaFolderNode createVariablesFolder() {
		return new UaFolderNode(miloSingleServerBaseV0Strategy.getWaldotNamespace().getOpcUaNodeContext(),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().generateNodeId(
						miloSingleServerBaseV0Strategy.getRootNode().getNodeId().getIdentifier().toString()
								+ "/Variables"),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().generateQualifiedName("Variables"),
				LocalizedText.english("Gremlin Variables"));
	}

	private UaFolderNode createVerticesFolder() {
		return new UaFolderNode(miloSingleServerBaseV0Strategy.getWaldotNamespace().getOpcUaNodeContext(),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().generateNodeId(
						miloSingleServerBaseV0Strategy.getRootNode().getNodeId().getIdentifier().toString()
								+ "/Vertices"),
				miloSingleServerBaseV0Strategy.getWaldotNamespace().generateQualifiedName("Vertices"),
				LocalizedText.english("Gremlin Vertices"));
	}

	public UaFolderNode getEdgesFolderNode() {
		return edgesFolderNode;
	}

	public Map<String, UaFolderNode> getRulesDirectories() {
		return rulesDirectories;
	}

	public UaFolderNode getRulesFolderNode() {
		return rulesFolderNode;
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
		miloSingleServerBaseV0Strategy.getRootNode().addOrganizes(getVerticesFolderNode());
		setEdgesFolderNode(createEdgesFolder());
		miloSingleServerBaseV0Strategy.getWaldotNamespace().getStorageManager().addNode(getEdgesFolderNode());
		miloSingleServerBaseV0Strategy.getRootNode().addOrganizes(getEdgesFolderNode());
		setRulesFolderNode(createRulesFolder());
		miloSingleServerBaseV0Strategy.getWaldotNamespace().getStorageManager().addNode(getRulesFolderNode());
		miloSingleServerBaseV0Strategy.getRootNode().addOrganizes(getRulesFolderNode());
		setVariablesFolderNode(createVariablesFolder());
		miloSingleServerBaseV0Strategy.getWaldotNamespace().getStorageManager().addNode(getVariablesFolderNode());
		miloSingleServerBaseV0Strategy.getRootNode().addOrganizes(getVariablesFolderNode());

	}

	public void setEdgesFolderNode(UaFolderNode edgesFolderNode) {
		this.edgesFolderNode = edgesFolderNode;
	}

	public void setRulesFolderNode(UaFolderNode rulesFolderNode) {
		this.rulesFolderNode = rulesFolderNode;
	}

	public void setVariablesFolderNode(UaFolderNode variablesFolderNode) {
		this.variablesFolderNode = variablesFolderNode;
	}

	public void setVerticesFolderNode(UaFolderNode verticesFolderNode) {
		this.verticesFolderNode = verticesFolderNode;
	}

}
