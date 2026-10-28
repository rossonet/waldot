package net.rossonet.waldot.client.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONTokens;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONVersion;
import org.apache.tinkerpop.gremlin.structure.io.graphson.TinkerPopJacksonModule;
import org.apache.tinkerpop.shaded.jackson.core.JsonGenerator;
import org.apache.tinkerpop.shaded.jackson.core.JsonParser;
import org.apache.tinkerpop.shaded.jackson.core.JsonToken;
import org.apache.tinkerpop.shaded.jackson.core.type.WritableTypeId;
import org.apache.tinkerpop.shaded.jackson.databind.DeserializationContext;
import org.apache.tinkerpop.shaded.jackson.databind.JsonDeserializer;
import org.apache.tinkerpop.shaded.jackson.databind.JsonNode;
import org.apache.tinkerpop.shaded.jackson.databind.JsonSerializer;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import org.apache.tinkerpop.shaded.jackson.databind.SerializerProvider;
import org.apache.tinkerpop.shaded.jackson.databind.jsontype.TypeSerializer;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * Typed GraphSON v3 as written by the {@code query json} command of the
 * server: the ids of the WaldOT elements are OPC UA {@link NodeId}s with type
 * {@code tinker:nodeId}.
 */
public final class GraphSonSupport {

	/** Jackson module of the {@code tinker:nodeId} type. */
	public static final class NodeIdModule extends TinkerPopJacksonModule {

		private static final long serialVersionUID = 6214417520731391247L;

		public NodeIdModule() {
			super("waldot-client-nodeid");
			addSerializer(NodeId.class, new JsonSerializer<NodeId>() {
				@Override
				public void serialize(final NodeId value, final JsonGenerator gen, final SerializerProvider serializers)
						throws IOException {
					gen.writeString(value.toParseableString());
				}

				@Override
				public void serializeWithType(final NodeId value, final JsonGenerator gen,
						final SerializerProvider provider, final TypeSerializer typeSer) throws IOException {
					final WritableTypeId typeId = typeSer.writeTypePrefix(gen,
							typeSer.typeId(value, JsonToken.VALUE_STRING));
					serialize(value, gen, provider);
					typeSer.writeTypeSuffix(gen, typeId);
				}
			});
			addDeserializer(NodeId.class, new JsonDeserializer<NodeId>() {
				@Override
				public NodeId deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
					final JsonNode node = p.readValueAsTree();
					return NodeId.parse(
							node.has(GraphSONTokens.VALUE) ? node.get(GraphSONTokens.VALUE).asText() : node.asText());
				}
			});
		}

		@Override
		public Map<Class, String> getTypeDefinitions() {
			return Map.of(NodeId.class, "nodeId");
		}

		@Override
		public String getTypeNamespace() {
			return "tinker";
		}
	}

	private static final ObjectMapper MAPPER = GraphSONMapper.build().version(GraphSONVersion.V3_0)
			.addCustomModule(new NodeIdModule()).create().createMapper();

	/**
	 * Parses typed GraphSON v3.
	 *
	 * @return the rebuilt object (detached elements, lists, maps, values)
	 */
	public static Object read(final String json) throws IOException {
		if (json == null || json.isBlank()) {
			return null;
		}
		return MAPPER.readValue(json, Object.class);
	}

	/**
	 * A single value as a list of one element, a collection as a list, null as
	 * an empty list.
	 */
	public static List<Object> toList(final Object value) {
		if (value == null) {
			return new ArrayList<>();
		}
		if (value instanceof Collection<?> collection) {
			return new ArrayList<>(collection);
		}
		final List<Object> list = new ArrayList<>();
		list.add(value);
		return list;
	}

	/** Writes a value as typed GraphSON v3 (used by the tests). */
	public static String write(final Object value) throws IOException {
		return MAPPER.writeValueAsString(value);
	}

	private GraphSonSupport() {
	}
}
