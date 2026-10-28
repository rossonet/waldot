package net.rossonet.waldot.client.support;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ulong;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import net.rossonet.waldot.client.api.command.ArgumentDescriptor;

/**
 * Conversion of plain Java values to the declared data type of a method
 * argument (strings, booleans, integers, unsigned integers, floating point
 * numbers, NodeIds, localized texts; arrays when the value rank is not
 * scalar).
 */
public final class ArgumentConverter {

	private static Object convertArray(final Object value, final NodeId dataType) {
		final List<Object> elements = new ArrayList<>();
		if (value instanceof Collection<?> collection) {
			elements.addAll(collection);
		} else if (value.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(value); i++) {
				elements.add(Array.get(value, i));
			}
		} else {
			elements.add(value);
		}
		final List<Object> converted = new ArrayList<>(elements.size());
		Class<?> type = null;
		for (final Object element : elements) {
			final Object c = convertScalar(element, dataType);
			converted.add(c);
			if (c != null && type == null) {
				type = c.getClass();
			}
		}
		final Object array = Array.newInstance(type == null ? String.class : type, converted.size());
		for (int i = 0; i < converted.size(); i++) {
			Array.set(array, i, converted.get(i));
		}
		return array;
	}

	/**
	 * Converts a value to the type of an argument.
	 *
	 * @param value    the value (null stays null)
	 * @param argument the declared argument, null to keep the value
	 * @throws IllegalArgumentException if the value cannot be converted
	 */
	public static Object convert(final Object value, final ArgumentDescriptor argument) {
		if (value == null || argument == null || argument.dataType() == null) {
			return value;
		}
		if (argument.valueRank() > 0 || argument.valueRank() == 0 && isMultiple(value)) {
			return convertArray(value, argument.dataType());
		}
		return convertScalar(value, argument.dataType());
	}

	/** Converts a scalar to the given OPC UA data type. */
	public static Object convertScalar(final Object value, final NodeId dataType) {
		if (value == null || dataType == null) {
			return value;
		}
		try {
			if (dataType.equals(NodeIds.String)) {
				return value instanceof LocalizedText text ? text.getText() : value.toString();
			}
			if (dataType.equals(NodeIds.Boolean)) {
				return value instanceof Boolean ? value : Boolean.parseBoolean(value.toString().trim());
			}
			if (dataType.equals(NodeIds.Double)) {
				return number(value).doubleValue();
			}
			if (dataType.equals(NodeIds.Float)) {
				return number(value).floatValue();
			}
			if (dataType.equals(NodeIds.Int64)) {
				return number(value).longValue();
			}
			if (dataType.equals(NodeIds.Int32)) {
				return number(value).intValue();
			}
			if (dataType.equals(NodeIds.Int16)) {
				return number(value).shortValue();
			}
			if (dataType.equals(NodeIds.SByte)) {
				return number(value).byteValue();
			}
			if (dataType.equals(NodeIds.Byte)) {
				return ubyte(number(value).longValue());
			}
			if (dataType.equals(NodeIds.UInt16)) {
				return ushort(number(value).intValue());
			}
			if (dataType.equals(NodeIds.UInt32)) {
				return uint(number(value).longValue());
			}
			if (dataType.equals(NodeIds.UInt64)) {
				return ulong(number(value).longValue());
			}
			if (dataType.equals(NodeIds.NodeId)) {
				return value instanceof NodeId ? value : NodeId.parse(value.toString().trim());
			}
			if (dataType.equals(NodeIds.LocalizedText)) {
				return value instanceof LocalizedText ? value : LocalizedText.english(value.toString());
			}
		} catch (final RuntimeException e) {
			throw new IllegalArgumentException("cannot convert '" + value + "' to " + dataType, e);
		}
		return value;
	}

	private static boolean isMultiple(final Object value) {
		return value instanceof Collection<?> || value.getClass().isArray();
	}

	private static Number number(final Object value) {
		if (value instanceof Number number) {
			return number;
		}
		if (value instanceof Boolean bool) {
			return bool ? 1 : 0;
		}
		final String text = value.toString().trim();
		if (text.contains(".") || text.contains("e") || text.contains("E")) {
			return Double.parseDouble(text);
		}
		return Long.parseLong(text);
	}

	/** Builds the input variants of a call. */
	public static Variant[] toVariants(final Object[] values, final List<ArgumentDescriptor> arguments) {
		final Object[] inputs = values == null ? new Object[0] : values;
		final Variant[] variants = new Variant[inputs.length];
		for (int i = 0; i < inputs.length; i++) {
			final Object converted = convert(inputs[i],
					arguments != null && i < arguments.size() ? arguments.get(i) : null);
			variants[i] = converted instanceof Variant variant ? variant : new Variant(converted);
		}
		return variants;
	}

	/** Plain Java value of an output variant (arrays become lists). */
	public static Object fromVariant(final Variant variant) {
		final Object value = variant == null ? null : variant.getValue();
		if (value != null && value.getClass().isArray()) {
			if (value instanceof Object[] objects) {
				return Arrays.asList(objects.clone());
			}
			final List<Object> list = new ArrayList<>();
			for (int i = 0; i < Array.getLength(value); i++) {
				list.add(Array.get(value, i));
			}
			return list;
		}
		return value;
	}

	private ArgumentConverter() {
	}
}
