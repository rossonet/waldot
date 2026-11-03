package net.rossonet.waldot.api.exchange;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.tinkerpop.gremlin.structure.io.graphson.TinkerPopJacksonModule;
import org.apache.tinkerpop.shaded.jackson.core.JsonGenerator;
import org.apache.tinkerpop.shaded.jackson.core.JsonParser;
import org.apache.tinkerpop.shaded.jackson.core.JsonToken;
import org.apache.tinkerpop.shaded.jackson.core.type.WritableTypeId;
import org.apache.tinkerpop.shaded.jackson.databind.DeserializationContext;
import org.apache.tinkerpop.shaded.jackson.databind.JsonDeserializer;
import org.apache.tinkerpop.shaded.jackson.databind.JsonSerializer;
import org.apache.tinkerpop.shaded.jackson.databind.SerializerProvider;
import org.apache.tinkerpop.shaded.jackson.databind.jsontype.TypeSerializer;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;

/**
 * Typed GraphSON v3 of the OPC UA built-in types of Milo that TinkerPop does
 * not know: the unsigned integers and {@link ByteString}. Without this module
 * the serialization of a property with one of these values fails ("Could not
 * find a type identifier for the class").
 * <p>
 * Types (namespace {@code opc}): {@code opc:UByte}, {@code opc:UInt16},
 * {@code opc:UInt32}, {@code opc:UInt64} (JSON numbers) and
 * {@code opc:ByteString} (base64 text; a null ByteString is written as an
 * empty one). The module is shared by the server ({@code query json},
 * snapshots) and by the client library, so that both read and write the same
 * types. {@code Short}, {@code Byte}, {@code BigInteger} and
 * {@code BigDecimal} come instead from the TinkerPop extended module
 * ({@code gx:Int16}, {@code gx:Byte}, ...), enabled with
 * {@code GraphSONMapper.Builder.addDefaultXModule(true)}.
 * </p>
 */
public final class OpcUaGraphSonModule extends TinkerPopJacksonModule {

	private interface ValueWriter<T> {
		void write(T value, JsonGenerator gen) throws IOException;
	}

	private static final long serialVersionUID = -4316924093512872351L;

	/** Namespace of the GraphSON types of this module. */
	public static final String TYPE_NAMESPACE = "opc";

	private static final Map<Class, String> TYPES = new LinkedHashMap<>();

	static {
		TYPES.put(UByte.class, "UByte");
		TYPES.put(UShort.class, "UInt16");
		TYPES.put(UInteger.class, "UInt32");
		TYPES.put(ULong.class, "UInt64");
		TYPES.put(ByteString.class, "ByteString");
	}

	private static BigInteger readInteger(final JsonParser p) throws IOException {
		final JsonToken token = p.currentToken();
		if (token == JsonToken.VALUE_NUMBER_INT) {
			return p.getBigIntegerValue();
		}
		return new BigInteger(p.getValueAsString().trim());
	}

	private static <T> JsonSerializer<T> serializer(final JsonToken shape, final ValueWriter<T> writer) {
		return new JsonSerializer<>() {
			@Override
			public void serialize(final T value, final JsonGenerator gen, final SerializerProvider serializers)
					throws IOException {
				writer.write(value, gen);
			}

			@Override
			public void serializeWithType(final T value, final JsonGenerator gen, final SerializerProvider provider,
					final TypeSerializer typeSer) throws IOException {
				final WritableTypeId typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(value, shape));
				writer.write(value, gen);
				typeSer.writeTypeSuffix(gen, typeId);
			}
		};
	}

	private static <T> JsonDeserializer<T> unsignedDeserializer(final Function<BigInteger, T> factory) {
		return new JsonDeserializer<>() {
			@Override
			public T deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
				return factory.apply(readInteger(p));
			}
		};
	}

	public OpcUaGraphSonModule() {
		super("waldot-opcua-types");
		addSerializer(UByte.class,
				serializer(JsonToken.VALUE_NUMBER_INT, (UByte v, JsonGenerator g) -> g.writeNumber(v.intValue())));
		addSerializer(UShort.class,
				serializer(JsonToken.VALUE_NUMBER_INT, (UShort v, JsonGenerator g) -> g.writeNumber(v.intValue())));
		addSerializer(UInteger.class,
				serializer(JsonToken.VALUE_NUMBER_INT, (UInteger v, JsonGenerator g) -> g.writeNumber(v.longValue())));
		addSerializer(ULong.class,
				serializer(JsonToken.VALUE_NUMBER_INT, (ULong v, JsonGenerator g) -> g.writeNumber(v.toBigInteger())));
		addSerializer(ByteString.class, serializer(JsonToken.VALUE_STRING, (ByteString v, JsonGenerator g) -> g
				.writeString(v.isNull() ? "" : Base64.getEncoder().encodeToString(v.bytesOrEmpty()))));
		addDeserializer(UByte.class, unsignedDeserializer(v -> Unsigned.ubyte(v.longValueExact())));
		addDeserializer(UShort.class, unsignedDeserializer(v -> Unsigned.ushort(v.intValueExact())));
		addDeserializer(UInteger.class, unsignedDeserializer(v -> Unsigned.uint(v.longValueExact())));
		addDeserializer(ULong.class, unsignedDeserializer(Unsigned::ulong));
		addDeserializer(ByteString.class, new JsonDeserializer<ByteString>() {
			@Override
			public ByteString deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
				return ByteString.of(Base64.getDecoder().decode(p.getValueAsString("")));
			}
		});
	}

	@Override
	public Map<Class, String> getTypeDefinitions() {
		return TYPES;
	}

	@Override
	public String getTypeNamespace() {
		return TYPE_NAMESPACE;
	}
}
