
package net.rossonet.waldot.gremlin.opcgraph.structure;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.models.WaldotVertexProperty;
import net.rossonet.waldot.opc.AbstractOpcVertexProperty;

public class OpcVertexProperty<DATA_TYPE> extends AbstractOpcVertexProperty<DATA_TYPE>
		implements WaldotVertexProperty<DATA_TYPE> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public OpcVertexProperty(WaldotGraph graph, final WaldotVertex vertex, final String key, final DATA_TYPE value,
			UaNodeContext context, NodeId nodeId, LocalizedText description, UInteger writeMask, UInteger userWriteMask,
			NodeId dataType, Integer valueRank, UInteger[] arrayDimensions, UByte accessLevel, UByte userAccessLevel,
			Double minimumSamplingInterval, boolean historizing, boolean allowNullPropertyValues) {
		super(graph, vertex, key, value, context, nodeId, description, writeMask, userWriteMask, dataType, valueRank,
				arrayDimensions, accessLevel, userAccessLevel, minimumSamplingInterval, historizing);

	}

	@Override
	public Object clone() {
		return new OpcVertexProperty<DATA_TYPE>(graph, getVertexPropertyReference(), key(), value(), getNodeContext(),
				getNodeId(), getDescription(), getWriteMask(), getUserWriteMask(), getDataType(), getValueRank(),
				getArrayDimensions(), getAccessLevel(), getUserAccessLevel(), getMinimumSamplingInterval(),
				getHistorizing(), allowNullPropertyValues);
	}

	public OpcVertexProperty<DATA_TYPE> copy(final OpcVertex newOwner) {
		return new OpcVertexProperty<DATA_TYPE>(graph, newOwner, key(), value(), getNodeContext(), getNodeId(),
				getDescription(), getWriteMask(), getUserWriteMask(), getDataType(), getValueRank(),
				getArrayDimensions(), getAccessLevel(), getUserAccessLevel(), getMinimumSamplingInterval(),
				getHistorizing(), allowNullPropertyValues);
	}

}
