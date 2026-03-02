package net.rossonet.oshi.vertex;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.opc.AbstractOpcVertex;

public class OshiComputerSystemVertex extends AbstractOpcVertex implements OshiStoreVertex, AutoCloseable {

	public static void generateParameters(final WaldotNamespace waldotNamespace, final UaObjectTypeNode imageTypeNode) {
		// TODO Auto-generated method stub

	}

	public OshiComputerSystemVertex(final WaldotGraph graph, final UaNodeContext context, final NodeId nodeId,
			final QualifiedName browseName, final LocalizedText displayName, final LocalizedText description,
			final UInteger writeMask, final UInteger userWriteMask, final UByte eventNotifier, final long version,
			final Object[] propertyKeyValues) {
		super(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier,
				version);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Object clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyAddFunctionalEdge(final WaldotEdge edge, final WaldotVertex sourceVertex,
			final WaldotVertex targetVertex, final String label, final String type, final Object[] propertyKeyValues) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyRemoveFunctionalEdge(final WaldotEdge edge) {
		// TODO Auto-generated method stub

	}

}
