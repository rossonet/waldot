package net.rossonet.waldot.gremlin.opcgraph.serializer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.tinkerpop.gremlin.structure.io.Buffer;
import org.apache.tinkerpop.gremlin.structure.io.binary.DataType;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONVersion;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoMapper;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoVersion;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import org.apache.tinkerpop.shaded.kryo.Kryo;
import org.apache.tinkerpop.shaded.kryo.io.Input;
import org.apache.tinkerpop.shaded.kryo.io.Output;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV3;

/**
 * Serialization of OPC UA {@link NodeId}s used as Gremlin element ids.
 */
class NodeIdSerializersTest {

	static NodeId[] nodeIds() {
		return new NodeId[] { new NodeId(2, "waldot/vertex"), new NodeId(1, UInteger.valueOf(42)),
				new NodeId(3, UUID.fromString("6f1c2c7e-6c3a-4e8a-9f43-2d5e0c1a7b11")), NodeId.NULL_VALUE };
	}

	@ParameterizedTest
	@MethodSource("nodeIds")
	void graphBinaryRoundTrip(final NodeId nodeId) throws Exception {
		final NodeIdCustomTypeSerializer serializer = new NodeIdCustomTypeSerializer();
		assertEquals(DataType.CUSTOM, serializer.getDataType());
		assertEquals("node-id", serializer.getTypeName());
		final Buffer buffer = buffer(ByteBuffer.allocate(512));
		serializer.write(nodeId, buffer, null);
		assertEquals(nodeId, serializer.read(buffer, null));
	}

	@ParameterizedTest
	@MethodSource("nodeIds")
	void graphSonWritesTheParseableString(final NodeId nodeId) throws Exception {
		final NodeIdGraphSONSerializer module = new NodeIdGraphSONSerializer();
		assertEquals("tinker", module.getTypeNamespace());
		assertEquals("nodeId", module.getTypeDefinitions().get(NodeId.class));
		final ObjectMapper untyped = GraphSONMapper.build().version(GraphSONVersion.V1_0).addCustomModule(module)
				.create().createMapper();
		final String json = untyped.writeValueAsString(nodeId);
		assertEquals("\"" + nodeId.toParseableString() + "\"", json);
		final ObjectMapper typed = GraphSONMapper.build().version(GraphSONVersion.V3_0).addCustomModule(module)
				.create().createMapper();
		final String typedJson = typed.writeValueAsString(nodeId);
		assertEquals("{\"@type\":\"tinker:nodeId\",\"@value\":\"" + nodeId.toParseableString() + "\"}", typedJson);
	}

	@ParameterizedTest
	@MethodSource("nodeIds")
	void graphSonRoundTrip(final NodeId nodeId) throws Exception {
		final NodeIdGraphSONSerializer module = new NodeIdGraphSONSerializer();
		for (final GraphSONVersion version : GraphSONVersion.values()) {
			final ObjectMapper mapper = GraphSONMapper.build().version(version).addCustomModule(module).create()
					.createMapper();
			assertEquals(nodeId, mapper.readValue(mapper.writeValueAsString(nodeId), NodeId.class), version.name());
		}
	}

	@ParameterizedTest
	@MethodSource("nodeIds")
	void gryoRoundTrip(final NodeId nodeId) {
		for (final GryoVersion version : new GryoVersion[] { GryoVersion.V1_0, GryoVersion.V3_0 }) {
			final Kryo kryo = GryoMapper.build().version(version).addRegistry(OpcIoRegistryV3.instance()).create()
					.createMapper();
			final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (Output output = new Output(bytes)) {
				kryo.writeObject(output, nodeId);
			}
			try (Input input = new Input(bytes.toByteArray())) {
				assertEquals(nodeId, kryo.readObject(input, NodeId.class), version.name());
			}
		}
	}

	/** A minimal {@link Buffer} over a {@link ByteBuffer} (write, flip, read). */
	private static Buffer buffer(final ByteBuffer data) {
		final boolean[] flipped = { false };
		return (Buffer) Proxy.newProxyInstance(Buffer.class.getClassLoader(), new Class<?>[] { Buffer.class },
				(proxy, method, args) -> {
					switch (method.getName()) {
					case "writeInt" -> {
						data.putInt((Integer) args[0]);
						return proxy;
					}
					case "writeBytes" -> {
						data.put((byte[]) args[0]);
						return proxy;
					}
					case "readInt" -> {
						if (!flipped[0]) {
							data.flip();
							flipped[0] = true;
						}
						return data.getInt();
					}
					case "readBytes" -> {
						data.get((byte[]) args[0]);
						return proxy;
					}
					default -> throw new UnsupportedOperationException(method.getName());
					}
				});
	}
}
