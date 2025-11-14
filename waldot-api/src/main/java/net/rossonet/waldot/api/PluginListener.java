package net.rossonet.waldot.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.eclipse.milo.opcua.sdk.server.ObjectTypeManager.ObjectNodeConstructor;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.AccessRestrictionType;
import org.eclipse.milo.opcua.stack.core.types.structured.RolePermissionType;

import net.rossonet.waldot.api.models.WaldotCommand;
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

	default boolean containsObjectDefinition(final NodeId typeDefinitionNodeId) {
		return false;
	}

	default boolean containsObjectLabel(final String typeDefinitionLabel) {
		return false;
	}

	default WaldotVertex createVertexObject(final NodeId typeDefinitionNodeId, final WaldotGraph graph,
			final UaNodeContext context, final NodeId nodeId, final QualifiedName browseName,
			final LocalizedText displayName, final LocalizedText description, final UInteger writeMask,
			final UInteger userWriteMask, final UByte eventNotifier, final long version, Object[] propertyKeyValues) {
		return null;
	}

	default Collection<WaldotCommand> getCommands() {
		return Collections.emptyList();
	}

	default NodeId getObjectLabel(final String typeDefinitionLabel) {
		return null;
	}

	default Map<String, Object> getRuleFunctions() {
		return Collections.emptyMap();
	}

	void initialize(WaldotNamespace waldotNamespace);

	default void reset() {

	}

	default void start() {

	}

	default void stop() {

	}
}
