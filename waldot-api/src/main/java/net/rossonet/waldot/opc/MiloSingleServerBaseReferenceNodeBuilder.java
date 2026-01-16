package net.rossonet.waldot.opc;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

import java.util.Optional;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.typetree.ReferenceType;
import org.eclipse.milo.opcua.sdk.server.ObjectTypeManager.ObjectNodeConstructor;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaReferenceTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.AccessRestrictionType;
import org.eclipse.milo.opcua.stack.core.types.structured.RolePermissionType;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.MiloStrategy;

/**
 * Builder for the reference nodes used in the WaldOT Gremlin OPC UA server.
 * This class generates the necessary nodes for edges, vertices, and rules in
 * the OPC UA address space.
 */
public class MiloSingleServerBaseReferenceNodeBuilder {

	public static UaObjectTypeNode edgeTypeNode;

	public static final UByte edgeVariableAccessLevel = AccessLevel.toValue(AccessLevel.CurrentRead,
			AccessLevel.CurrentWrite);

	public static final UByte edgeVariableUserAccessLevel = AccessLevel.toValue(AccessLevel.CurrentRead,
			AccessLevel.CurrentWrite);

	public static final UInteger edgeVariableUserWriteMask = UInteger.MIN;

	public static final UInteger edgeVariableWriteMask = UInteger.MIN;

	public static final UByte eventNotifierActive = ubyte(1);

	public static final UByte eventNotifierDisable = ubyte(0);

	public static final String HAS_GREMLIN_PROPERTY_REFERENCE = "HasGremlinProperty";

	public static final String HAS_REFERENCE_DESCRIPTION_REFERENCE = "HasReferenceDescription";

	public static final String HAS_SOURCE_NODE_REFERENCE = "HasSourceNode";
	public static final String HAS_TARGET_NODE_REFERENCE = "HasTargetNode";
	public static NodeId hasGremlinPropertyReferenceType;
	public static NodeId hasReferenceDescriptionReferenceType;
	public static NodeId hasSourceNodeReferenceType;
	public static NodeId hasTargetNodeReferenceType;
	public static UaObjectTypeNode interfaceTypeNode;
	private static final String IS_PROPERTY_OF_REFERENCE = "IsPropertyOf";

	private static final String IS_SOURCE_NODE_FOR_REFERENCE = "IsSourceNodeFor";
	private static final String IS_TARGET_NODE_FOR_REFERENCE = "IsTargetNodeFor";
	public static UaVariableNode isForwardTypeNode;
	public static UaVariableNode labelEdgeTypeNode;
	public static UaVariableNode labelVertexTypeNode;
	private final static ObjectNodeConstructor objectNodeConstructor = new ObjectNodeConstructor() {

		@Override
		public UaObjectNode apply(UaNodeContext context, NodeId nodeId, QualifiedName browseName,
				LocalizedText displayName, LocalizedText description, UInteger writeMask, UInteger userWriteMask,
				RolePermissionType[] rolePermissions, RolePermissionType[] userRolePermissions,
				AccessRestrictionType accessRestrictions) {
			return new UaObjectNode(context, nodeId, browseName, displayName, description, writeMask, userWriteMask,
					rolePermissions, userRolePermissions, accessRestrictions);
		}

	};
	private static final String REFERENCED_BY_REFERENCE = "ReferencedBy";
	public static UaVariableNode referenceTypeTypeNode;
	public static UaVariableNode sourceNodeTypeNode;
	public static UaVariableNode targetNodeTypeNode;
	public static UaVariableNode typeEdgeTypeNode;
	public static final UByte variableAccessLevel = AccessLevel.toValue(AccessLevel.CurrentRead,
			AccessLevel.CurrentWrite);
	public static final UByte variableUserAccessLevel = AccessLevel.toValue(AccessLevel.CurrentRead,
			AccessLevel.CurrentWrite);
	public static final UInteger variableUserWriteMask = UInteger.MIN;
	public static final UInteger variableWriteMask = UInteger.MIN;
	public static final int version = 0;
	public static UaObjectTypeNode vertexTypeNode;
	public static final UInteger vertexUserWriteMask = UInteger.MIN;
	public static final UInteger vertexWriteMask = UInteger.MIN;

	private static void generateEdgeTypeNode(final MiloStrategy miloStrategy) {
		edgeTypeNode = UaObjectTypeNode.builder(miloStrategy.getWaldotNamespace().getOpcUaNodeContext())
				.setNodeId(miloStrategy.getWaldotNamespace().generateNodeId("ObjectTypes/EdgeObjectType"))
				.setBrowseName(miloStrategy.getWaldotNamespace().generateQualifiedName("EdgeObjectType"))
				.setDisplayName(LocalizedText.english("Gremlin Edge Node")).setIsAbstract(false).build();
		labelEdgeTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				miloStrategy.getWaldotNamespace().getOpcUaNodeContext())
				.setNodeId(miloStrategy.getWaldotNamespace()
						.generateNodeId("ObjectTypes/EdgeObjectType." + MiloStrategy.LABEL_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(miloStrategy.getWaldotNamespace().generateQualifiedName(MiloStrategy.LABEL_FIELD))
				.setDisplayName(LocalizedText.english(MiloStrategy.LABEL_FIELD)).setDataType(NodeIds.String)
				.setTypeDefinition(NodeIds.BaseDataVariableType).build();
		labelEdgeTypeNode.addReference(new Reference(labelEdgeTypeNode.getNodeId(), NodeIds.HasModellingRule,
				NodeIds.ModellingRule_Mandatory.expanded(), true));
		labelEdgeTypeNode.setValue(new DataValue(new Variant("NaN")));
		edgeTypeNode.addComponent(labelEdgeTypeNode);
		typeEdgeTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				miloStrategy.getWaldotNamespace().getOpcUaNodeContext())
				.setNodeId(miloStrategy.getWaldotNamespace()
						.generateNodeId("ObjectTypes/EdgeObjectType." + MiloStrategy.TYPE_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(miloStrategy.getWaldotNamespace().generateQualifiedName(MiloStrategy.TYPE_FIELD))
				.setDisplayName(LocalizedText.english(MiloStrategy.TYPE_FIELD)).setDataType(NodeIds.String)
				.setTypeDefinition(NodeIds.BaseDataVariableType).build();
		typeEdgeTypeNode.addReference(new Reference(typeEdgeTypeNode.getNodeId(), NodeIds.HasModellingRule,
				NodeIds.ModellingRule_Mandatory.expanded(), true));
		typeEdgeTypeNode.setValue(new DataValue(new Variant("NaN")));
		edgeTypeNode.addComponent(typeEdgeTypeNode);
		sourceNodeTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				miloStrategy.getWaldotNamespace().getOpcUaNodeContext())
				.setNodeId(miloStrategy.getWaldotNamespace().generateNodeId("ObjectTypes/EdgeObjectType.SourceNode"))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(miloStrategy.getWaldotNamespace().generateQualifiedName("SourceNode"))
				.setDisplayName(LocalizedText.english("SourceNode")).setDataType(NodeIds.NodeId)
				.setTypeDefinition(NodeIds.BaseDataVariableType).build();
		sourceNodeTypeNode.addReference(new Reference(sourceNodeTypeNode.getNodeId(), NodeIds.HasModellingRule,
				NodeIds.ModellingRule_Mandatory.expanded(), true));
		sourceNodeTypeNode.setValue(new DataValue(new Variant(null)));
		edgeTypeNode.addComponent(sourceNodeTypeNode);
		targetNodeTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				miloStrategy.getWaldotNamespace().getOpcUaNodeContext())
				.setNodeId(miloStrategy.getWaldotNamespace().generateNodeId("ObjectTypes/EdgeObjectType.TargetNode"))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(miloStrategy.getWaldotNamespace().generateQualifiedName("TargetNode"))
				.setDisplayName(LocalizedText.english("TargetNode")).setDataType(NodeIds.NodeId)
				.setTypeDefinition(NodeIds.BaseDataVariableType).build();
		targetNodeTypeNode.addReference(new Reference(targetNodeTypeNode.getNodeId(), NodeIds.HasModellingRule,
				NodeIds.ModellingRule_Mandatory.expanded(), true));
		targetNodeTypeNode.setValue(new DataValue(new Variant(null)));
		edgeTypeNode.addComponent(targetNodeTypeNode);
		referenceTypeTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				miloStrategy.getWaldotNamespace().getOpcUaNodeContext())
				.setNodeId(miloStrategy.getWaldotNamespace().generateNodeId("ObjectTypes/EdgeObjectType.ReferenceType"))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(miloStrategy.getWaldotNamespace().generateQualifiedName("ReferenceType"))
				.setDisplayName(LocalizedText.english("ReferenceType")).setDataType(NodeIds.NodeId)
				.setTypeDefinition(NodeIds.BaseDataVariableType).build();
		referenceTypeTypeNode.addReference(new Reference(referenceTypeTypeNode.getNodeId(), NodeIds.HasModellingRule,
				NodeIds.ModellingRule_Mandatory.expanded(), true));
		referenceTypeTypeNode.setValue(new DataValue(new Variant(null)));
		edgeTypeNode.addComponent(referenceTypeTypeNode);
		isForwardTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				miloStrategy.getWaldotNamespace().getOpcUaNodeContext())
				.setNodeId(miloStrategy.getWaldotNamespace().generateNodeId("ObjectTypes/EdgeObjectType.IsForward"))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(miloStrategy.getWaldotNamespace().generateQualifiedName("IsForward"))
				.setDisplayName(LocalizedText.english("IsForward")).setDataType(NodeIds.Boolean)
				.setTypeDefinition(NodeIds.BaseDataVariableType).build();
		isForwardTypeNode.addReference(new Reference(isForwardTypeNode.getNodeId(), NodeIds.HasModellingRule,
				NodeIds.ModellingRule_Mandatory.expanded(), true));
		isForwardTypeNode.setValue(new DataValue(new Variant(true)));
		edgeTypeNode.addComponent(isForwardTypeNode);
		miloStrategy.getWaldotNamespace().getStorageManager().addNode(labelEdgeTypeNode);
		miloStrategy.getWaldotNamespace().getStorageManager().addNode(sourceNodeTypeNode);
		miloStrategy.getWaldotNamespace().getStorageManager().addNode(targetNodeTypeNode);
		miloStrategy.getWaldotNamespace().getStorageManager().addNode(referenceTypeTypeNode);
		miloStrategy.getWaldotNamespace().getStorageManager().addNode(isForwardTypeNode);
		miloStrategy.getWaldotNamespace().getStorageManager().addNode(edgeTypeNode);
		edgeTypeNode.addReference(
				new Reference(edgeTypeNode.getNodeId(), NodeIds.HasSubtype, NodeIds.BaseObjectType.expanded(), false));
		miloStrategy.getWaldotNamespace().getObjectTypeManager().registerObjectType(edgeTypeNode.getNodeId(),
				UaObjectNode.class, objectNodeConstructor);
	}

	private static void generateInterfaceRootNode(final MiloStrategy miloSingleServerBaseV0Strategy) {
		interfaceTypeNode = UaObjectTypeNode
				.builder(miloSingleServerBaseV0Strategy.getWaldotNamespace().getOpcUaNodeContext())
				.setNodeId(miloSingleServerBaseV0Strategy.getWaldotNamespace().generateNodeId("API"))
				.setBrowseName(miloSingleServerBaseV0Strategy.getWaldotNamespace().generateQualifiedName("API"))
				.setDisplayName(LocalizedText.english("WaldOT API & Commands")).setIsAbstract(false).build();
		miloSingleServerBaseV0Strategy.getWaldotNamespace().getStorageManager().addNode(interfaceTypeNode);
		interfaceTypeNode.addReference(new Reference(interfaceTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		miloSingleServerBaseV0Strategy.getWaldotNamespace().getObjectTypeManager()
				.registerObjectType(interfaceTypeNode.getNodeId(), UaObjectNode.class, objectNodeConstructor);

	}

	public static void generateRefereceNodes(final MiloStrategy miloSingleServerBaseV0Strategy) {
		generateVertexTypeNode(miloSingleServerBaseV0Strategy);
		generateEdgeTypeNode(miloSingleServerBaseV0Strategy);
		generateInterfaceRootNode(miloSingleServerBaseV0Strategy);
		hasReferenceDescriptionReferenceType = generateReferenceTypeNode(HAS_REFERENCE_DESCRIPTION_REFERENCE,
				REFERENCED_BY_REFERENCE, "Edge description with properties", NodeIds.NonHierarchicalReferences, false,
				false, miloSingleServerBaseV0Strategy.getWaldotNamespace());
		hasSourceNodeReferenceType = generateReferenceTypeNode(HAS_SOURCE_NODE_REFERENCE, IS_SOURCE_NODE_FOR_REFERENCE,
				"Source node for edge", NodeIds.NonHierarchicalReferences, false, false,
				miloSingleServerBaseV0Strategy.getWaldotNamespace());
		hasTargetNodeReferenceType = generateReferenceTypeNode(HAS_TARGET_NODE_REFERENCE, IS_TARGET_NODE_FOR_REFERENCE,
				"Target node for edge", NodeIds.NonHierarchicalReferences, false, false,
				miloSingleServerBaseV0Strategy.getWaldotNamespace());
		hasGremlinPropertyReferenceType = generateReferenceTypeNode(HAS_GREMLIN_PROPERTY_REFERENCE,
				IS_PROPERTY_OF_REFERENCE, "A property linked to an edge or vertex", NodeIds.HasComponent, false, false,
				miloSingleServerBaseV0Strategy.getWaldotNamespace());
	}

	public static NodeId generateReferenceTypeNode(final String reference, final String inverse,
			final String description, final NodeId subTypeId, final boolean isAbstract, final boolean isSymmetric,
			final WaldotNamespace waldotNamespace) {
		final UaReferenceTypeNode refType = new UaReferenceTypeNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId(reference), waldotNamespace.generateQualifiedName(reference),
				LocalizedText.english(reference), LocalizedText.english(description), UInteger.valueOf(0),
				UInteger.valueOf(0), false, false, LocalizedText.english(inverse));
		waldotNamespace.getStorageManager().addNode(refType);
		refType.addReference(new Reference(refType.getNodeId(), NodeIds.HasSubtype, subTypeId.expanded(), false));
		final ReferenceType referenceType = new ReferenceType() {

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
			public Optional<NodeId> getSupertypeId() {
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

		};
		waldotNamespace.getReferenceTypes().getRoot().addChild(referenceType);
		waldotNamespace.getOpcuaServer().updateReferenceTypeTree();
		return refType.getNodeId();
	}

	private static void generateVertexTypeNode(final MiloStrategy miloSingleServerBaseV0Strategy) {
		vertexTypeNode = UaObjectTypeNode
				.builder(miloSingleServerBaseV0Strategy.getWaldotNamespace().getOpcUaNodeContext())
				.setNodeId(miloSingleServerBaseV0Strategy.getWaldotNamespace()
						.generateNodeId("ObjectTypes/VertexObjectType"))
				.setBrowseName(
						miloSingleServerBaseV0Strategy.getWaldotNamespace().generateQualifiedName("VertexObjectType"))
				.setDisplayName(LocalizedText.english("Gremlin Vertex Node")).setIsAbstract(false).build();
		labelVertexTypeNode = new UaVariableNode.UaVariableNodeBuilder(
				miloSingleServerBaseV0Strategy.getWaldotNamespace().getOpcUaNodeContext())
				.setNodeId(miloSingleServerBaseV0Strategy.getWaldotNamespace()
						.generateNodeId("ObjectTypes/VertexObjectType." + MiloStrategy.LABEL_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(miloSingleServerBaseV0Strategy.getWaldotNamespace()
						.generateQualifiedName(MiloStrategy.LABEL_FIELD))
				.setDisplayName(LocalizedText.english(MiloStrategy.LABEL_FIELD)).setDataType(NodeIds.String)
				.setTypeDefinition(NodeIds.BaseDataVariableType).build();
		labelVertexTypeNode.addReference(new Reference(labelVertexTypeNode.getNodeId(), NodeIds.HasModellingRule,
				NodeIds.ModellingRule_Mandatory.expanded(), true));
		labelVertexTypeNode.setValue(new DataValue(new Variant("NaN")));
		vertexTypeNode.addComponent(labelVertexTypeNode);
		miloSingleServerBaseV0Strategy.getWaldotNamespace().getStorageManager().addNode(labelVertexTypeNode);
		miloSingleServerBaseV0Strategy.getWaldotNamespace().getStorageManager().addNode(vertexTypeNode);
		vertexTypeNode.addReference(new Reference(vertexTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		miloSingleServerBaseV0Strategy.getWaldotNamespace().getObjectTypeManager()
				.registerObjectType(vertexTypeNode.getNodeId(), UaObjectNode.class, objectNodeConstructor);
	}

	public static UByte getEventNotifier(Object[] propertyKeyValues) {
		final String event = getKeyValuesProperty(propertyKeyValues, MiloStrategy.EVENT_NOTIFIER_PARAMETER);
		if (event != null && !event.isEmpty()) {
			if (event.equalsIgnoreCase("active") || event.equalsIgnoreCase("enabled") || event.equalsIgnoreCase("true")
					|| event.equals("1")) {
				return eventNotifierActive;
			} else {
				return eventNotifierDisable;
			}
		}
		return eventNotifierDisable;
	}

	private static String getKeyValuesProperty(final Object[] propertyKeyValues, final String label) {
		for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
			if (propertyKeyValues[i] instanceof String && label.equals(propertyKeyValues[i])) {
				return propertyKeyValues[i + 1].toString();
			}
		}
		return null;
	}

	public static UInteger getUserWriteMask(Object[] propertyKeyValues) {
		final String userWriteMask = getKeyValuesProperty(propertyKeyValues, MiloStrategy.USER_WRITE_MASK_PARAMETER);
		// TODO parametrizzare correttamente user write mask
		return vertexUserWriteMask;
	}

	public static long getVersion(Object[] propertyKeyValues) {
		final String found = getKeyValuesProperty(propertyKeyValues, MiloStrategy.VERSION_PARAMETER);
		if (found == null || found.isEmpty()) {
			return version;
		}
		return Long.parseLong(found);
	}

	public static UInteger getWriteMask(Object[] propertyKeyValues) {
		final String writeMask = getKeyValuesProperty(propertyKeyValues, MiloStrategy.WRITE_MASK_PARAMETER);
		// TODO parametrizzare correttamente write mask
		return vertexWriteMask;
	}

}
