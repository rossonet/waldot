package net.rossonet.waldot.commands.script;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.tinkerpop.gremlin.structure.Element;

import net.rossonet.waldot.api.changes.GraphChangeRecorder;

/**
 * Converts the value returned by a script into something that JSON can carry:
 * elements become their id, traversals and iterators lists (at most
 * {@link #MAX_ITEMS} items), maps and collections are converted recursively,
 * the rest becomes text.
 */
public final class ScriptResultValue {

	public static final int MAX_ITEMS = 1000;

	public static Object toJson(final Object value) {
		return toJson(value, 0);
	}

	private static Object toJson(final Object value, final int depth) {
		if (value == null || value instanceof Number || value instanceof Boolean || value instanceof String) {
			return value;
		}
		if (depth > 8) {
			return String.valueOf(value);
		}
		if (value instanceof final Element element) {
			return GraphChangeRecorder.id(element);
		}
		if (value instanceof final Map<?, ?> map) {
			final Map<String, Object> out = new LinkedHashMap<>();
			for (final Map.Entry<?, ?> e : map.entrySet()) {
				out.put(String.valueOf(e.getKey() instanceof final Element el ? GraphChangeRecorder.id(el) : e.getKey()),
						toJson(e.getValue(), depth + 1));
			}
			return out;
		}
		final Iterator<?> it;
		if (value instanceof final Iterable<?> iterable) {
			it = iterable.iterator();
		} else if (value instanceof final Iterator<?> iterator) {
			it = iterator;
		} else if (value.getClass().isArray()) {
			final int length = java.lang.reflect.Array.getLength(value);
			final List<Object> items = new ArrayList<>(Math.min(length, MAX_ITEMS + 1));
			for (int i = 0; i < length && i <= MAX_ITEMS; i++) {
				items.add(java.lang.reflect.Array.get(value, i));
			}
			it = items.iterator();
		} else {
			return String.valueOf(value);
		}
		final List<Object> out = new ArrayList<>();
		while (it.hasNext()) {
			if (out.size() >= MAX_ITEMS) {
				out.add("… (altri elementi omessi)");
				break;
			}
			out.add(toJson(it.next(), depth + 1));
		}
		return out;
	}

	private ScriptResultValue() {
	}
}
