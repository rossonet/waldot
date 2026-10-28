package net.rossonet.waldot.gremlin.opcgraph.serializer;

import org.apache.tinkerpop.shaded.kryo.Kryo;
import org.apache.tinkerpop.shaded.kryo.Serializer;
import org.apache.tinkerpop.shaded.kryo.io.Input;
import org.apache.tinkerpop.shaded.kryo.io.Output;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * Gryo (Kryo) serializer of OPC UA {@link NodeId}s: the id is written as its
 * parseable string ({@link NodeId#toParseableString()}) and read back with
 * {@link NodeId#parse(String)}.
 */
public final class NodeIdGryoSerializer extends Serializer<NodeId> {

	public NodeIdGryoSerializer() {
		super(false, true);
	}

	@Override
	public NodeId read(final Kryo kryo, final Input input, final Class<NodeId> type) {
		return NodeId.parse(input.readString());
	}

	@Override
	public void write(final Kryo kryo, final Output output, final NodeId nodeId) {
		output.writeString(nodeId.toParseableString());
	}
}
