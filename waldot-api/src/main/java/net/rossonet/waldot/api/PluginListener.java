package net.rossonet.waldot.api;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.ObjectTypeManager.ObjectNodeConstructor;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
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

import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;

/**
 * PluginListener interface for handling events related to Waldot plugins.
 * Implementations of this interface should define how to handle plugin
 * initialization, vertex creation, command retrieval, and rule functions.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface PluginListener {

	public final static ObjectNodeConstructor objectNodeConstructor = new ObjectNodeConstructor() {

		@Override
		public UaObjectNode apply(UaNodeContext context, NodeId nodeId, QualifiedName browseName,
				LocalizedText displayName, LocalizedText description, UInteger writeMask, UInteger userWriteMask,
				RolePermissionType[] rolePermissions, RolePermissionType[] userRolePermissions,
				AccessRestrictionType accessRestrictions) {
			return new UaObjectNode(context, nodeId, browseName, displayName, description, writeMask, userWriteMask,
					rolePermissions, userRolePermissions, accessRestrictions);
		}

	};

	public static void addStringParameterToTypeNode(WaldotNamespace waldotNamespace, UaObjectTypeNode typeNode,
			String variableId, NodeId dataType) {
		final UaVariableNode variable = new UaVariableNode.UaVariableNodeBuilder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(typeNode.getNodeId().toParseableString() + "." + variableId))
				.setAccessLevel(AccessLevel.READ_WRITE).setBrowseName(waldotNamespace.generateQualifiedName(variableId))
				.setDisplayName(LocalizedText.english(variableId)).setDataType(dataType)
				.setTypeDefinition(NodeIds.BaseDataVariableType).build();
		variable.addReference(new Reference(variable.getNodeId(), NodeIds.HasModellingRule,
				NodeIds.ModellingRule_Mandatory.expanded(), true));
		variable.setValue(new DataValue(new Variant("NaN")));
		typeNode.addComponent(variable);
		waldotNamespace.getStorageManager().addNode(variable);
	}

	default boolean containsEdgeType(String typeDefinitionLabel) {
		return false;
	}

	default boolean containsVertexType(final String typeDefinitionLabel) {
		return false;
	}

	default boolean containsVertexTypeNode(final NodeId typeDefinition) {
		return false;
	}

	default WaldotVertex createVertex(final NodeId typeDefinitionNodeId, final WaldotGraph graph,
			final UaNodeContext context, final NodeId nodeId, final QualifiedName browseName,
			final LocalizedText displayName, final LocalizedText description, final UInteger writeMask,
			final UInteger userWriteMask, final UByte eventNotifier, final long version, Object[] propertyKeyValues) {
		return null;
	}

	default Collection<WaldotCommand> getCommands() {
		return Collections.emptyList();
	}

	default NodeId getVertexTypeNode(final String typeDefinitionLabel) {
		return null;
	}

	void initialize(WaldotNamespace waldotNamespace);

	default void notifyAddEdge(WaldotEdge edge, WaldotVertex sourceVertex, WaldotVertex targetVertex, String label,
			String type, Object[] propertyKeyValues) {
	}

	default void reset() {

	}

	default void start() {

	}

	default void stop() {

	}

}
