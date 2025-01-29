package net.rossonet.waldot.api;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;

public interface PluginListener {

	default boolean containsObjectDefinition(NodeId typeDefinitionNodeId) {
		return false;
	}

	default boolean containsObjectLabel(String typeDefinitionLabel) {
		return false;
	}

	default WaldotVertex createVertexObject(NodeId typeDefinitionNodeId, WaldotGraph graph, UaNodeContext context,
			NodeId nodeId, QualifiedName browseName, LocalizedText displayName, LocalizedText description,
			UInteger writeMask, UInteger userWriteMask, UByte eventNotifier, long version) {
		return null;
	}

	default Collection<WaldotCommand> getCommands() {
		return Collections.emptyList();
	}

	default NodeId getObjectLabel(String typeDefinitionLabel) {
		return null;
	}

	void initialize(WaldotNamespace waldotNamespace);

	default void reset() {

	}

	default void start() {

	}

	default void stop() {

	}
}
