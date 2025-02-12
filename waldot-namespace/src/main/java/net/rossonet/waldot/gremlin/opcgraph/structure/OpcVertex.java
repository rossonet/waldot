
package net.rossonet.waldot.gremlin.opcgraph.structure;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.opc.AbstractOpcVertex;

public class OpcVertex extends AbstractOpcVertex implements WaldotVertex {
	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public OpcVertex(WaldotGraph graph, UaNodeContext context, NodeId nodeId, QualifiedName browseName,
			LocalizedText displayName, LocalizedText description, UInteger writeMask, UInteger userWriteMask,
			UByte eventNotifier, long version) {
		super(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier,
				version);
	}

	@Override
	public Object clone() {
		return new OpcVertex(graph, getNodeContext(), getNodeId(), getBrowseName(), getDisplayName(), getDescription(),
				getWriteMask(), getUserWriteMask(), getEventNotifier(), version());

	}

}
