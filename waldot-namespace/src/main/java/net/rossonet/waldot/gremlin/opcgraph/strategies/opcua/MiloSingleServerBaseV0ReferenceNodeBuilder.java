package net.rossonet.waldot.gremlin.opcgraph.strategies.opcua;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

import java.util.Optional;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaReferenceTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.ReferenceType;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.api.models.WaldotNamespace;

public class MiloSingleServerBaseV0ReferenceNodeBuilder {

	static UaVariableNode actionRuleTypeNode;
	static UaVariableNode conditionRuleTypeNode;
	static UaObjectTypeNode edgeTypeNode;

	static final UByte eventNotifierActive = ubyte(1);
	static final UByte eventNotifierDisable = ubyte(0);
	static NodeId hasPropertyReferenceType;
	static NodeId hasReferenceDescriptionReferenceType;
	static NodeId hasSourceNodeReferenceType;
	static NodeId hasTargetNodeReferenceType;
	static UaObjectTypeNode interfaceTypeNode;
	static UaVariableNode isForwardTypeNode;

	static UaVariableNode labelEdgeTypeNode;
	static UaVariableNode labelRuleTypeNode;
	static UaVariableNode labelVertexTypeNode;
	static UaVariableNode priorityRuleTypeNode;
	static UaVariableNode referenceTypeTypeNode;
	static UaObjectTypeNode ruleTypeNode;
	static UaVariableNode sourceNodeTypeNode;
	static UaVariableNode targetNodeTypeNode;
	static final UInteger userWriteMask = UInteger.MIN;
	static final int version = 0;
	static UaObjectTypeNode vertexTypeNode;
	static final UInteger writeMask = UInteger.MIN;

	private static void generateEdgeTypeNode(MiloSingleServerBaseV0Strategy miloSingleServerBaseV0Strategy) {
		edgeTypeNode = UaObjectTypeNode.builder(miloSingleServerBaseV0Strategy.waldotNamespace.getOpcUaNodeContext())
				.setNodeId(miloSingleServerBaseV0Strategy.waldotNamespace.generateNodeId("ObjectTypes/EdgeObjectType"))
				.setBrowseName(miloSingleServerBaseV0Strategy.waldotNamespace.generateQualifiedName("EdgeObjectType"))
				.setDisplayName(LocalizedText.english("Gremlin Edge Node")).setIsAbstract(false).build();
		labelEdgeTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				miloSingleServerBaseV0Strategy.waldotNamespace.getOpcUaNodeContext())
				.setNodeId(miloSingleServerBaseV0Strategy.waldotNamespace
						.generateNodeId("ObjectTypes/EdgeObjectType." + MiloSingleServerBaseV0Strategy.LABEL_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(miloSingleServerBaseV0Strategy.waldotNamespace
						.generateQualifiedName(MiloSingleServerBaseV0Strategy.LABEL_FIELD))
				.setDisplayName(LocalizedText.english(MiloSingleServerBaseV0Strategy.LABEL_FIELD))
				.setDataType(Identifiers.String).setTypeDefinition(Identifiers.BaseDataVariableType).build();
		labelEdgeTypeNode.addReference(new Reference(labelEdgeTypeNode.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
		labelEdgeTypeNode.setValue(new DataValue(new Variant("NaN")));
		edgeTypeNode.addComponent(labelEdgeTypeNode);
		sourceNodeTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				miloSingleServerBaseV0Strategy.waldotNamespace.getOpcUaNodeContext())
				.setNodeId(miloSingleServerBaseV0Strategy.waldotNamespace
						.generateNodeId("ObjectTypes/EdgeObjectType.SourceNode"))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(miloSingleServerBaseV0Strategy.waldotNamespace.generateQualifiedName("SourceNode"))
				.setDisplayName(LocalizedText.english("SourceNode")).setDataType(Identifiers.NodeId)
				.setTypeDefinition(Identifiers.BaseDataVariableType).build();
		sourceNodeTypeNode.addReference(new Reference(sourceNodeTypeNode.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
		sourceNodeTypeNode.setValue(new DataValue(new Variant(null)));
		edgeTypeNode.addComponent(sourceNodeTypeNode);
		targetNodeTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				miloSingleServerBaseV0Strategy.waldotNamespace.getOpcUaNodeContext())
				.setNodeId(miloSingleServerBaseV0Strategy.waldotNamespace
						.generateNodeId("ObjectTypes/EdgeObjectType.TargetNode"))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(miloSingleServerBaseV0Strategy.waldotNamespace.generateQualifiedName("TargetNode"))
				.setDisplayName(LocalizedText.english("TargetNode")).setDataType(Identifiers.NodeId)
				.setTypeDefinition(Identifiers.BaseDataVariableType).build();
		targetNodeTypeNode.addReference(new Reference(targetNodeTypeNode.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
		targetNodeTypeNode.setValue(new DataValue(new Variant(null)));
		edgeTypeNode.addComponent(targetNodeTypeNode);
		referenceTypeTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				miloSingleServerBaseV0Strategy.waldotNamespace.getOpcUaNodeContext())
				.setNodeId(miloSingleServerBaseV0Strategy.waldotNamespace
						.generateNodeId("ObjectTypes/EdgeObjectType.ReferenceType"))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(miloSingleServerBaseV0Strategy.waldotNamespace.generateQualifiedName("ReferenceType"))
				.setDisplayName(LocalizedText.english("ReferenceType")).setDataType(Identifiers.NodeId)
				.setTypeDefinition(Identifiers.BaseDataVariableType).build();
		referenceTypeTypeNode.addReference(new Reference(referenceTypeTypeNode.getNodeId(),
				Identifiers.HasModellingRule, Identifiers.ModellingRule_Mandatory.expanded(), true));
		referenceTypeTypeNode.setValue(new DataValue(new Variant(null)));
		edgeTypeNode.addComponent(referenceTypeTypeNode);
		isForwardTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				miloSingleServerBaseV0Strategy.waldotNamespace.getOpcUaNodeContext())
				.setNodeId(miloSingleServerBaseV0Strategy.waldotNamespace
						.generateNodeId("ObjectTypes/EdgeObjectType.IsForward"))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(miloSingleServerBaseV0Strategy.waldotNamespace.generateQualifiedName("IsForward"))
				.setDisplayName(LocalizedText.english("IsForward")).setDataType(Identifiers.Boolean)
				.setTypeDefinition(Identifiers.BaseDataVariableType).build();
		isForwardTypeNode.addReference(new Reference(isForwardTypeNode.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
		isForwardTypeNode.setValue(new DataValue(new Variant(true)));
		edgeTypeNode.addComponent(isForwardTypeNode);
		miloSingleServerBaseV0Strategy.waldotNamespace.getStorageManager().addNode(labelEdgeTypeNode);
		miloSingleServerBaseV0Strategy.waldotNamespace.getStorageManager().addNode(sourceNodeTypeNode);
		miloSingleServerBaseV0Strategy.waldotNamespace.getStorageManager().addNode(targetNodeTypeNode);
		miloSingleServerBaseV0Strategy.waldotNamespace.getStorageManager().addNode(referenceTypeTypeNode);
		miloSingleServerBaseV0Strategy.waldotNamespace.getStorageManager().addNode(isForwardTypeNode);
		miloSingleServerBaseV0Strategy.waldotNamespace.getStorageManager().addNode(edgeTypeNode);
		edgeTypeNode.addReference(new Reference(edgeTypeNode.getNodeId(), Identifiers.HasSubtype,
				Identifiers.BaseObjectType.expanded(), false));
		miloSingleServerBaseV0Strategy.waldotNamespace.getNodeType().registerObjectType(edgeTypeNode.getNodeId(),
				UaObjectNode.class, UaObjectNode::new);
	}

	private static void generateInterfaceRootNode(MiloSingleServerBaseV0Strategy miloSingleServerBaseV0Strategy) {
		interfaceTypeNode = UaObjectTypeNode
				.builder(miloSingleServerBaseV0Strategy.waldotNamespace.getOpcUaNodeContext())
				.setNodeId(miloSingleServerBaseV0Strategy.waldotNamespace.generateNodeId("API"))
				.setBrowseName(miloSingleServerBaseV0Strategy.waldotNamespace.generateQualifiedName("API"))
				.setDisplayName(LocalizedText.english("WaldOT API & Commands")).setIsAbstract(false).build();
		miloSingleServerBaseV0Strategy.waldotNamespace.getStorageManager().addNode(interfaceTypeNode);
		interfaceTypeNode.addReference(new Reference(interfaceTypeNode.getNodeId(), Identifiers.HasSubtype,
				Identifiers.BaseObjectType.expanded(), false));
		miloSingleServerBaseV0Strategy.waldotNamespace.getNodeType().registerObjectType(interfaceTypeNode.getNodeId(),
				UaObjectNode.class, UaObjectNode::new);

	}

	static void generateRefereceNodes(MiloSingleServerBaseV0Strategy miloSingleServerBaseV0Strategy) {
		generateVertexTypeNode(miloSingleServerBaseV0Strategy);
		generateEdgeTypeNode(miloSingleServerBaseV0Strategy);
		generateInterfaceRootNode(miloSingleServerBaseV0Strategy);
		generateRulesTypeNode(miloSingleServerBaseV0Strategy);
		hasReferenceDescriptionReferenceType = generateReferenceTypeNode("HasReferenceDescription", "ReferencedBy",
				"Edge description with properties", Identifiers.NonHierarchicalReferences, false, false,
				miloSingleServerBaseV0Strategy.waldotNamespace);
		hasSourceNodeReferenceType = generateReferenceTypeNode("HasSourceNode", "IsSourceNodeFor",
				"Source node for edge", Identifiers.NonHierarchicalReferences, false, false,
				miloSingleServerBaseV0Strategy.waldotNamespace);
		hasTargetNodeReferenceType = generateReferenceTypeNode("HasTargetNode", "IsTargetNodeFor",
				"Target node for edge", Identifiers.NonHierarchicalReferences, false, false,
				miloSingleServerBaseV0Strategy.waldotNamespace);
		hasPropertyReferenceType = generateReferenceTypeNode("HasGremlinProperty", "IsPropertyOf",
				"A property linked to an edge or vertex", Identifiers.Organizes, false, false,
				miloSingleServerBaseV0Strategy.waldotNamespace);
		hasPropertyReferenceType = generateReferenceTypeNode(MiloSingleServerBaseV0Strategy.HAS_WALDOT_RULE,
				"IsFiredBy", "A rule fired by the events", Identifiers.HasComponent, false, false,
				miloSingleServerBaseV0Strategy.waldotNamespace);
	}

	static NodeId generateReferenceTypeNode(String reference, String inverse, String description, NodeId subTypeId,
			boolean isAbstract, boolean isSymmetric, WaldotNamespace waldotNamespace) {
		final UaReferenceTypeNode refType = new UaReferenceTypeNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId(reference), waldotNamespace.generateQualifiedName(reference),
				LocalizedText.english(reference), LocalizedText.english(description), UInteger.valueOf(0),
				UInteger.valueOf(0), false, false, LocalizedText.english(inverse));
		waldotNamespace.getStorageManager().addNode(refType);
		refType.addReference(new Reference(refType.getNodeId(), Identifiers.HasSubtype, subTypeId.expanded(), false));
		waldotNamespace.getReferenceTypes().put(refType.getNodeId(), new ReferenceType() {

			@Override
			public QualifiedName getBrowseName() {
				return refType.getBrowseName();
			}

			@Override
			public Optional<String> getInverseName() {
				return Optional.of(inverse);
			}

			@Override
			public NodeId getNodeId() {
				return refType.getNodeId();
			}

			@Override
			public Optional<NodeId> getSuperTypeId() {
				return Optional.of(subTypeId);
			}

			@Override
			public boolean isAbstract() {
				return isAbstract;
			}

			@Override
			public boolean isSymmetric() {
				return isSymmetric;
			}

		});
		return refType.getNodeId();
	}

	private static void generateRulesTypeNode(MiloSingleServerBaseV0Strategy miloSingleServerBaseV0Strategy) {
		ruleTypeNode = UaObjectTypeNode.builder(miloSingleServerBaseV0Strategy.waldotNamespace.getOpcUaNodeContext())
				.setNodeId(miloSingleServerBaseV0Strategy.waldotNamespace
						.generateNodeId("ObjectTypes/WaldOTRuleObjectType"))
				.setBrowseName(
						miloSingleServerBaseV0Strategy.waldotNamespace.generateQualifiedName("WaldOTRuleObjectType"))
				.setDisplayName(LocalizedText.english("WaldOT Rule Node")).setIsAbstract(false).build();
		labelRuleTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				miloSingleServerBaseV0Strategy.waldotNamespace.getOpcUaNodeContext())
				.setNodeId(miloSingleServerBaseV0Strategy.waldotNamespace.generateNodeId(
						"ObjectTypes/WaldOTRuleObjectType." + MiloSingleServerBaseV0Strategy.LABEL_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(miloSingleServerBaseV0Strategy.waldotNamespace
						.generateQualifiedName(MiloSingleServerBaseV0Strategy.LABEL_FIELD))
				.setDisplayName(LocalizedText.english(MiloSingleServerBaseV0Strategy.LABEL_FIELD))
				.setDataType(Identifiers.String).setTypeDefinition(Identifiers.BaseDataVariableType).build();
		labelRuleTypeNode.addReference(new Reference(labelRuleTypeNode.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
		labelRuleTypeNode.setValue(new DataValue(new Variant("NaN")));
		ruleTypeNode.addComponent(labelRuleTypeNode);

		conditionRuleTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				miloSingleServerBaseV0Strategy.waldotNamespace.getOpcUaNodeContext())
				.setNodeId(miloSingleServerBaseV0Strategy.waldotNamespace.generateNodeId(
						"ObjectTypes/WaldOTRuleObjectType." + MiloSingleServerBaseV0Strategy.CONDITION_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(miloSingleServerBaseV0Strategy.waldotNamespace
						.generateQualifiedName(MiloSingleServerBaseV0Strategy.CONDITION_FIELD))
				.setDisplayName(LocalizedText.english(MiloSingleServerBaseV0Strategy.CONDITION_FIELD))
				.setDataType(Identifiers.String).setTypeDefinition(Identifiers.BaseDataVariableType).build();
		conditionRuleTypeNode.addReference(new Reference(conditionRuleTypeNode.getNodeId(),
				Identifiers.HasModellingRule, Identifiers.ModellingRule_Mandatory.expanded(), true));
		conditionRuleTypeNode
				.setValue(new DataValue(new Variant(MiloSingleServerBaseV0Strategy.DEFAULT_CONDITION_VALUE)));
		ruleTypeNode.addComponent(conditionRuleTypeNode);

		actionRuleTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				miloSingleServerBaseV0Strategy.waldotNamespace.getOpcUaNodeContext())
				.setNodeId(miloSingleServerBaseV0Strategy.waldotNamespace.generateNodeId(
						"ObjectTypes/WaldOTRuleObjectType." + MiloSingleServerBaseV0Strategy.ACTION_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(miloSingleServerBaseV0Strategy.waldotNamespace
						.generateQualifiedName(MiloSingleServerBaseV0Strategy.ACTION_FIELD))
				.setDisplayName(LocalizedText.english(MiloSingleServerBaseV0Strategy.ACTION_FIELD))
				.setDataType(Identifiers.String).setTypeDefinition(Identifiers.BaseDataVariableType).build();
		actionRuleTypeNode.addReference(new Reference(actionRuleTypeNode.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
		actionRuleTypeNode.setValue(new DataValue(new Variant(MiloSingleServerBaseV0Strategy.DEFAULT_ACTION_VALUE)));
		ruleTypeNode.addComponent(actionRuleTypeNode);

		priorityRuleTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				miloSingleServerBaseV0Strategy.waldotNamespace.getOpcUaNodeContext())
				.setNodeId(miloSingleServerBaseV0Strategy.waldotNamespace.generateNodeId(
						"ObjectTypes/WaldOTRuleObjectType." + MiloSingleServerBaseV0Strategy.PRIORITY_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(miloSingleServerBaseV0Strategy.waldotNamespace
						.generateQualifiedName(MiloSingleServerBaseV0Strategy.PRIORITY_FIELD))
				.setDisplayName(LocalizedText.english(MiloSingleServerBaseV0Strategy.PRIORITY_FIELD))
				.setDataType(Identifiers.Int32).setTypeDefinition(Identifiers.BaseDataVariableType).build();
		priorityRuleTypeNode.addReference(new Reference(priorityRuleTypeNode.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
		priorityRuleTypeNode
				.setValue(new DataValue(new Variant(MiloSingleServerBaseV0Strategy.DEFAULT_PRIORITY_VALUE)));
		ruleTypeNode.addComponent(priorityRuleTypeNode);

		miloSingleServerBaseV0Strategy.waldotNamespace.getStorageManager().addNode(labelRuleTypeNode);
		miloSingleServerBaseV0Strategy.waldotNamespace.getStorageManager().addNode(conditionRuleTypeNode);
		miloSingleServerBaseV0Strategy.waldotNamespace.getStorageManager().addNode(actionRuleTypeNode);
		miloSingleServerBaseV0Strategy.waldotNamespace.getStorageManager().addNode(priorityRuleTypeNode);
		miloSingleServerBaseV0Strategy.waldotNamespace.getStorageManager().addNode(ruleTypeNode);
		ruleTypeNode.addReference(new Reference(ruleTypeNode.getNodeId(), Identifiers.HasSubtype,
				Identifiers.BaseObjectType.expanded(), false));
		miloSingleServerBaseV0Strategy.waldotNamespace.getNodeType().registerObjectType(ruleTypeNode.getNodeId(),
				UaObjectNode.class, UaObjectNode::new);
	}

	private static void generateVertexTypeNode(MiloSingleServerBaseV0Strategy miloSingleServerBaseV0Strategy) {
		vertexTypeNode = UaObjectTypeNode.builder(miloSingleServerBaseV0Strategy.waldotNamespace.getOpcUaNodeContext())
				.setNodeId(
						miloSingleServerBaseV0Strategy.waldotNamespace.generateNodeId("ObjectTypes/VertexObjectType"))
				.setBrowseName(miloSingleServerBaseV0Strategy.waldotNamespace.generateQualifiedName("VertexObjectType"))
				.setDisplayName(LocalizedText.english("Gremlin Vertex Node")).setIsAbstract(false).build();
		labelVertexTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				miloSingleServerBaseV0Strategy.waldotNamespace.getOpcUaNodeContext())
				.setNodeId(miloSingleServerBaseV0Strategy.waldotNamespace
						.generateNodeId("ObjectTypes/VertexObjectType." + MiloSingleServerBaseV0Strategy.LABEL_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(miloSingleServerBaseV0Strategy.waldotNamespace
						.generateQualifiedName(MiloSingleServerBaseV0Strategy.LABEL_FIELD))
				.setDisplayName(LocalizedText.english(MiloSingleServerBaseV0Strategy.LABEL_FIELD))
				.setDataType(Identifiers.String).setTypeDefinition(Identifiers.BaseDataVariableType).build();
		labelVertexTypeNode.addReference(new Reference(labelVertexTypeNode.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
		labelVertexTypeNode.setValue(new DataValue(new Variant("NaN")));
		vertexTypeNode.addComponent(labelVertexTypeNode);
		miloSingleServerBaseV0Strategy.waldotNamespace.getStorageManager().addNode(labelVertexTypeNode);
		miloSingleServerBaseV0Strategy.waldotNamespace.getStorageManager().addNode(vertexTypeNode);
		vertexTypeNode.addReference(new Reference(vertexTypeNode.getNodeId(), Identifiers.HasSubtype,
				Identifiers.BaseObjectType.expanded(), false));
		miloSingleServerBaseV0Strategy.waldotNamespace.getNodeType().registerObjectType(vertexTypeNode.getNodeId(),
				UaObjectNode.class, UaObjectNode::new);
	}

}
