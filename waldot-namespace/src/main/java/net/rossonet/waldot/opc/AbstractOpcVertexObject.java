package net.rossonet.waldot.opc;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.gremlin.opcgraph.structure.AbstractOpcGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertex;

public abstract class AbstractOpcVertexObject extends OpcVertex {

	public AbstractOpcVertexObject(AbstractOpcGraph graph, UaNodeContext context, NodeId nodeId, QualifiedName browseName,
			LocalizedText displayName, LocalizedText description, UInteger writeMask, UInteger userWriteMask,
			UByte eventNotifier, long version) {
		super(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier,
				version);
	}

}
