package net.rossonet.waldot.gremlin.opcgraph.ser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.tinkerpop.gremlin.structure.io.Buffer;
import org.apache.tinkerpop.gremlin.structure.io.binary.DataType;
import org.apache.tinkerpop.gremlin.structure.io.binary.GraphBinaryReader;
import org.apache.tinkerpop.gremlin.structure.io.binary.GraphBinaryWriter;
import org.apache.tinkerpop.gremlin.structure.io.binary.types.CustomTypeSerializer;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public class NodeIdCustomTypeSerializer implements CustomTypeSerializer<NodeId> {

	@Override
	public DataType getDataType() {
		return DataType.CUSTOM;
	}

	@Override
	public String getTypeName() {
		return "node-id";
	}

	@Override
	public NodeId read(final Buffer buffer, final GraphBinaryReader context) throws IOException {
		return readValue(buffer, context, false);
	}

	@Override
	public NodeId readValue(Buffer buffer, GraphBinaryReader context, boolean nullable) throws IOException {
		final byte[] bytes = new byte[buffer.readInt()];
		buffer.readBytes(bytes);
		final String stringData = new String(bytes, StandardCharsets.UTF_8);
		return NodeId.parse(stringData);
	}

	@Override
	public void write(final NodeId value, final Buffer buffer, final GraphBinaryWriter context) throws IOException {
		writeValue(value, buffer, context, true);
	}

	@Override
	public void writeValue(NodeId value, Buffer buffer, GraphBinaryWriter context, boolean nullable)
			throws IOException {
		final byte[] stringBytes = value.toParseableString().getBytes(StandardCharsets.UTF_8);
		buffer.writeInt(stringBytes.length).writeBytes(stringBytes);

	}

}
