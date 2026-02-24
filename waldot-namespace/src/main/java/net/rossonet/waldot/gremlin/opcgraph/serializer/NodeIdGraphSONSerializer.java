package net.rossonet.waldot.gremlin.opcgraph.serializer;

import java.io.IOException;
import java.util.Map;

import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONTokens;
import org.apache.tinkerpop.gremlin.structure.io.graphson.TinkerPopJacksonModule;
import org.apache.tinkerpop.shaded.jackson.core.JsonGenerator;
import org.apache.tinkerpop.shaded.jackson.core.JsonParser;
import org.apache.tinkerpop.shaded.jackson.core.JsonToken;
import org.apache.tinkerpop.shaded.jackson.core.type.WritableTypeId;
import org.apache.tinkerpop.shaded.jackson.databind.DeserializationContext;
import org.apache.tinkerpop.shaded.jackson.databind.JsonDeserializer;
import org.apache.tinkerpop.shaded.jackson.databind.JsonNode;
import org.apache.tinkerpop.shaded.jackson.databind.JsonSerializer;
import org.apache.tinkerpop.shaded.jackson.databind.SerializerProvider;
import org.apache.tinkerpop.shaded.jackson.databind.jsontype.TypeSerializer;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public final class NodeIdGraphSONSerializer extends TinkerPopJacksonModule {

	private static final long serialVersionUID = -2493998598679761756L;

	public NodeIdGraphSONSerializer() {
		super(NodeId.class.toString());

		addSerializer(NodeId.class, new JsonSerializer<NodeId>() {
			@Override
			public void serialize(NodeId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
				// gen.writeStartObject();
				// gen.writeStringField(GraphSONTokens.TYPE, "nodeId");
				// gen.writeFieldName(GraphSONTokens.VALUE);
				gen.writeString(value.toParseableString());
				// gen.writeEndObject();
			}

			@Override
			public void serializeWithType(NodeId value, JsonGenerator gen, SerializerProvider provider,
					TypeSerializer typeSer) throws IOException {
				final WritableTypeId typeId = typeSer.writeTypePrefix(gen,
						typeSer.typeId(value, JsonToken.VALUE_STRING));
				serialize(value, gen, provider);
				typeSer.writeTypeSuffix(gen, typeId);
			}

		});

		/* DESERIALIZER */
		addDeserializer(NodeId.class, new JsonDeserializer<NodeId>() {
			@Override
			public NodeId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
				final JsonNode n = p.readValueAsTree();
				if (n.has(GraphSONTokens.VALUE)) {
					return NodeId.parse(n.get(GraphSONTokens.VALUE).toString());
				}
				// fallback se non tipizzato
				return NodeId.parse(n.toString());
			}
		});
	}

	@Override
	public Map<Class, String> getTypeDefinitions() {
		return Map.of(NodeId.class, "nodeId");
		// return Map.of();
	}

	@Override
	public String getTypeNamespace() {
		return "tinker";
	}

}