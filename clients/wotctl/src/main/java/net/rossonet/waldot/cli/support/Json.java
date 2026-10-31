package net.rossonet.waldot.cli.support;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.XmlElement;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UNumber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.rossonet.waldot.client.api.graph.GraphSnapshot;

/**
 * Conversion of the values of the client (vertices, edges, OPC UA built-in
 * types, records of the API) to JSON and to plain text.
 * <ul>
 * <li>{@link NodeId}, {@link ExpandedNodeId}, {@link QualifiedName}: parseable
 * string ({@code ns=2;s=sensor-1});</li>
 * <li>{@link LocalizedText}: the text;</li>
 * <li>{@link DateTime}: ISO-8601 UTC instant (null if not set);</li>
 * <li>{@link StatusCode}: symbolic name ({@code Good},
 * {@code Bad_NodeIdUnknown});</li>
 * <li>{@link Variant}: its value; {@link DataValue}: value, status, source and
 * server time;</li>
 * <li>{@link ByteString} and {@code byte[]}: base64;</li>
 * <li>vertices: {@code {"id","label","properties"}}, edges:
 * {@code {"id","label","outV","inV","properties"}};</li>
 * <li>records: an object with the components; maps, collections and arrays
 * recursively.</li>
 * </ul>
 */
public final class Json {

	private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final int MAX_DEPTH = 64;

	private static ArrayNode array(final Iterator<?> iterator,
			final int depth) {
		final ArrayNode array = FACTORY.arrayNode();
		while (iterator.hasNext()) {
			array.add(toNode(iterator.next(), depth + 1));
		}
		return array;
	}

	/** Compact JSON on a single line. */
	public static String compact(final JsonNode node) {
		try {
			return MAPPER.writeValueAsString(node);
		} catch (final JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	private static ObjectNode dataValue(final DataValue value,
			final int depth) {
		final ObjectNode node = FACTORY.objectNode();
		node.set("value", toNode(value.getValue(), depth + 1));
		node.set("status", toNode(value.getStatusCode(), depth + 1));
		node.set("sourceTime", toNode(value.getSourceTime(), depth + 1));
		node.set("serverTime", toNode(value.getServerTime(), depth + 1));
		return node;
	}

	private static ObjectNode edge(final Edge edge, final int depth) {
		final ObjectNode node = FACTORY.objectNode();
		node.put("type", "edge");
		node.set("id", toNode(edge.id(), depth + 1));
		node.put("label", edge.label());
		try {
			node.set("outV", toNode(edge.outVertex().id(), depth + 1));
			node.set("inV", toNode(edge.inVertex().id(), depth + 1));
		} catch (final RuntimeException e) {
			// reference edge without vertices
		}
		node.set("properties", properties(edge.properties(), depth));
		return node;
	}

	/** @return the id of an element as text (parseable NodeId) */
	public static String id(final Object id) {
		return id instanceof final NodeId nodeId
				? nodeId.toParseableString()
				: String.valueOf(id);
	}

	private static String keyText(final Object key) {
		final JsonNode node = toNode(key, MAX_DEPTH - 1);
		return node.isValueNode() ? node.asText() : compact(node);
	}

	/** Indented JSON. */
	public static String pretty(final JsonNode node) {
		try {
			return MAPPER.writerWithDefaultPrettyPrinter()
					.writeValueAsString(node);
		} catch (final JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	private static ObjectNode properties(
			final Iterator<? extends Property<?>> iterator, final int depth) {
		final Map<String, List<Object>> values = new LinkedHashMap<>();
		try {
			while (iterator.hasNext()) {
				final Property<?> property = iterator.next();
				if (property.isPresent()) {
					values.computeIfAbsent(property.key(),
							k -> new ArrayList<>()).add(property.value());
				}
			}
		} catch (final RuntimeException e) {
			// element without properties (reference element)
		}
		final ObjectNode node = FACTORY.objectNode();
		values.forEach((key, list) -> node.set(key,
				list.size() == 1
						? toNode(list.get(0), depth + 1)
						: toNode(list, depth + 1)));
		return node;
	}

	private static ObjectNode record(final Record record, final int depth) {
		final ObjectNode node = FACTORY.objectNode();
		for (final RecordComponent component : record.getClass()
				.getRecordComponents()) {
			try {
				node.set(component.getName(), toNode(
						component.getAccessor().invoke(record), depth + 1));
			} catch (final ReflectiveOperationException | RuntimeException e) {
				node.put(component.getName(), "?");
			}
		}
		return node;
	}

	/**
	 * Text of a value for the line formats ({@code value}, {@code topic}):
	 * strings, numbers and booleans as they are, null as an empty string, the
	 * other values as compact JSON.
	 */
	public static String scalar(final Object value) {
		final JsonNode node = toNode(value);
		if (node.isNull() || node.isMissingNode()) {
			return "";
		}
		return node.isValueNode() ? node.asText() : compact(node);
	}

	/**
	 * Same as {@link #scalar(Object)} on a single line: a text with line breaks
	 * is written as a JSON string.
	 */
	public static String scalarLine(final Object value) {
		final String text = scalar(value);
		if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
			return compact(FACTORY.textNode(text));
		}
		return text;
	}

	/** Symbolic name of a status code ({@code Good}, {@code Bad_Timeout}). */
	public static String statusName(final StatusCode status) {
		if (status == null) {
			return null;
		}
		if (status.getValue() == 0) {
			return "Good";
		}
		return StatusCodes.lookup(status.getValue()).map(names -> names[0])
				.orElse(String.format("0x%08X", status.getValue()));
	}

	/** Human readable text of a value (vertices and edges on one line). */
	public static String text(final Object value) {
		if (value instanceof final Vertex vertex) {
			return "v[" + id(vertex.id()) + "] " + vertex.label() + " "
					+ compact(properties(vertex.properties(), 0));
		}
		if (value instanceof final Edge edge) {
			String ends = "";
			try {
				ends = " " + id(edge.outVertex().id()) + " -> "
						+ id(edge.inVertex().id());
			} catch (final RuntimeException e) {
				// reference edge
			}
			return "e[" + id(edge.id()) + "] " + edge.label() + ends + " "
					+ compact(properties(edge.properties(), 0));
		}
		return scalar(value);
	}

	/** Converts a value to a JSON tree. */
	public static JsonNode toNode(final Object value) {
		return toNode(value, 0);
	}

	private static JsonNode toNode(final Object value, final int depth) {
		if (depth > MAX_DEPTH) {
			return FACTORY.textNode(String.valueOf(value));
		}
		if (value == null) {
			return FACTORY.nullNode();
		}
		if (value instanceof final JsonNode node) {
			return node;
		}
		if (value instanceof final String text) {
			return FACTORY.textNode(text);
		}
		if (value instanceof final Boolean bool) {
			return FACTORY.booleanNode(bool);
		}
		if (value instanceof final ULong number) {
			return FACTORY.numberNode(number.toBigInteger());
		}
		if (value instanceof final UNumber number) {
			return FACTORY.numberNode(number.longValue());
		}
		if (value instanceof Integer || value instanceof Short
				|| value instanceof Byte) {
			return FACTORY.numberNode(((Number) value).intValue());
		}
		if (value instanceof final Long number) {
			return FACTORY.numberNode(number);
		}
		if (value instanceof final Float number) {
			return Float.isFinite(number)
					? FACTORY.numberNode(number)
					: FACTORY.textNode(number.toString());
		}
		if (value instanceof final Double number) {
			return Double.isFinite(number)
					? FACTORY.numberNode(number)
					: FACTORY.textNode(number.toString());
		}
		if (value instanceof final BigInteger number) {
			return FACTORY.numberNode(number);
		}
		if (value instanceof final BigDecimal number) {
			return FACTORY.numberNode(number);
		}
		if (value instanceof Number || value instanceof Character
				|| value instanceof UUID || value instanceof URI
				|| value instanceof Path || value instanceof File
				|| value instanceof Duration
				|| value instanceof TemporalAccessor) {
			return FACTORY.textNode(value.toString());
		}
		if (value instanceof final Enum<?> constant) {
			return FACTORY.textNode(constant.name());
		}
		if (value instanceof final NodeId nodeId) {
			return FACTORY.textNode(nodeId.toParseableString());
		}
		if (value instanceof final ExpandedNodeId nodeId) {
			return FACTORY.textNode(nodeId.toParseableString());
		}
		if (value instanceof final QualifiedName name) {
			return FACTORY.textNode(name.toParseableString());
		}
		if (value instanceof final LocalizedText text) {
			return text.getText() == null
					? FACTORY.nullNode()
					: FACTORY.textNode(text.getText());
		}
		if (value instanceof final DateTime time) {
			return time.isNull()
					? FACTORY.nullNode()
					: FACTORY.textNode(time.getJavaInstant().toString());
		}
		if (value instanceof final StatusCode status) {
			return FACTORY.textNode(statusName(status));
		}
		if (value instanceof final ByteString bytes) {
			return bytes.isNull()
					? FACTORY.nullNode()
					: FACTORY.textNode(Base64.getEncoder()
							.encodeToString(bytes.bytesOrEmpty()));
		}
		if (value instanceof final byte[] bytes) {
			return FACTORY.textNode(Base64.getEncoder().encodeToString(bytes));
		}
		if (value instanceof final Variant variant) {
			return toNode(variant.getValue(), depth + 1);
		}
		if (value instanceof final DataValue dataValue) {
			return dataValue(dataValue, depth);
		}
		if (value instanceof ExtensionObject || value instanceof XmlElement) {
			return FACTORY.textNode(value.toString());
		}
		if (value instanceof final Optional<?> optional) {
			return optional.isPresent()
					? toNode(optional.get(), depth + 1)
					: FACTORY.nullNode();
		}
		if (value instanceof final GraphSnapshot snapshot) {
			final ObjectNode node = FACTORY.objectNode();
			node.put("size", snapshot.size());
			node.put("compressed", snapshot.compressed());
			node.put("vertices", snapshot.vertices());
			node.put("edges", snapshot.edges());
			node.put("blocks", snapshot.blocks());
			return node;
		}
		if (value instanceof final Vertex vertex) {
			final ObjectNode node = FACTORY.objectNode();
			node.put("type", "vertex");
			node.set("id", toNode(vertex.id(), depth + 1));
			node.put("label", vertex.label());
			node.set("properties", properties(vertex.properties(), depth));
			return node;
		}
		if (value instanceof final Edge edge) {
			return edge(edge, depth);
		}
		if (value instanceof final VertexProperty<?> property) {
			final ObjectNode node = FACTORY.objectNode();
			node.put("key", property.key());
			node.set("value",
					property.isPresent()
							? toNode(property.value(), depth + 1)
							: FACTORY.nullNode());
			return node;
		}
		if (value instanceof final Property<?> property) {
			final ObjectNode node = FACTORY.objectNode();
			node.put("key", property.key());
			node.set("value",
					property.isPresent()
							? toNode(property.value(), depth + 1)
							: FACTORY.nullNode());
			return node;
		}
		if (value instanceof final org.apache.tinkerpop.gremlin.process.traversal.Path path) {
			final ArrayNode array = FACTORY.arrayNode();
			for (final Object object : path.objects()) {
				array.add(toNode(object, depth + 1));
			}
			return array;
		}
		if (value instanceof final Map<?, ?> map) {
			final ObjectNode node = FACTORY.objectNode();
			map.forEach((key, item) -> node.set(keyText(key),
					toNode(item, depth + 1)));
			return node;
		}
		if (value instanceof final Iterable<?> iterable) {
			return array(iterable.iterator(), depth);
		}
		if (value instanceof final Iterator<?> iterator) {
			return array(iterator, depth);
		}
		if (value.getClass().isArray()) {
			final ArrayNode array = FACTORY.arrayNode();
			final int length = Array.getLength(value);
			for (int i = 0; i < length; i++) {
				array.add(toNode(Array.get(value, i), depth + 1));
			}
			return array;
		}
		if (value instanceof final Record record) {
			return record(record, depth);
		}
		try {
			return MAPPER.valueToTree(value);
		} catch (final RuntimeException e) {
			return FACTORY.textNode(value.toString());
		}
	}

	private Json() {
	}
}
