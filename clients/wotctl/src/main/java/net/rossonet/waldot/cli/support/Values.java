package net.rossonet.waldot.cli.support;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.client.support.ArgumentConverter;

/**
 * Typed literals of the command line ({@code call}, {@code write --type}): a
 * value can be written as {@code tipo:valore}, e.g. {@code int32:5},
 * {@code double:1.5}, {@code bool:true}, {@code nodeid:ns=2;s=x}; without a
 * known prefix it is a string.
 */
public final class Values {

	/** Names of the supported types and their OPC UA data types. */
	public static final Map<String, NodeId> TYPES;

	static {
		final Map<String, NodeId> types = new LinkedHashMap<>();
		types.put("string", NodeIds.String);
		types.put("bool", NodeIds.Boolean);
		types.put("boolean", NodeIds.Boolean);
		types.put("sbyte", NodeIds.SByte);
		types.put("byte", NodeIds.Byte);
		types.put("int16", NodeIds.Int16);
		types.put("uint16", NodeIds.UInt16);
		types.put("int32", NodeIds.Int32);
		types.put("int", NodeIds.Int32);
		types.put("uint32", NodeIds.UInt32);
		types.put("int64", NodeIds.Int64);
		types.put("long", NodeIds.Int64);
		types.put("uint64", NodeIds.UInt64);
		types.put("float", NodeIds.Float);
		types.put("double", NodeIds.Double);
		types.put("nodeid", NodeIds.NodeId);
		types.put("text", NodeIds.LocalizedText);
		TYPES = java.util.Collections.unmodifiableMap(types);
	}

	/**
	 * Converts a value to a type.
	 *
	 * @throws IllegalArgumentException for an unknown type or an invalid value
	 */
	public static Object convert(final String value, final String type) {
		final String name = type.trim().toLowerCase(Locale.ROOT);
		final NodeId dataType = TYPES.get(name);
		if (dataType == null) {
			throw new IllegalArgumentException("tipo '" + type + "' sconosciuto (tipi: " + TYPES.keySet() + ")");
		}
		if (dataType.equals(NodeIds.Boolean)) {
			final String text = value.trim().toLowerCase(Locale.ROOT);
			return switch (text) {
			case "true", "1", "yes", "si", "sì" -> Boolean.TRUE;
			case "false", "0", "no" -> Boolean.FALSE;
			default -> throw new IllegalArgumentException("valore booleano non valido: '" + value + "'");
			};
		}
		return ArgumentConverter.convertScalar(value, dataType);
	}

	/** Converts a literal {@code [tipo:]valore}. */
	public static Object literal(final String literal) {
		final int separator = literal.indexOf(':');
		if (separator > 0) {
			final String prefix = literal.substring(0, separator).toLowerCase(Locale.ROOT);
			if (TYPES.containsKey(prefix)) {
				return convert(literal.substring(separator + 1), prefix);
			}
		}
		return literal;
	}

	private Values() {
	}
}
