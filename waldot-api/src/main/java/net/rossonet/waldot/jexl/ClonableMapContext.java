package net.rossonet.waldot.jexl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.jexl3.JexlContext;

public class ClonableMapContext implements JexlContext {

	/**
	 * The wrapped variable map.
	 */
	private final Map<String, Object> map;

	/**
	 * Creates a MapContext on an automatically allocated underlying HashMap.
	 */
	public ClonableMapContext() {
		map = new HashMap<>();
	}

	public ClonableMapContext(final ClonableMapContext clonableMapContext) {
		if (clonableMapContext == null) {
			map = new HashMap<>();
		} else {
			map = new HashMap<>(clonableMapContext.map);
		}
	}

	/**
	 * Creates a MapContext wrapping an existing user provided map.
	 *
	 * @param vars the variable map
	 */
	public ClonableMapContext(final Map<String, Object> vars) {
		map = vars == null ? new HashMap<>() : vars;
	}

	/**
	 * Clears all variables.
	 */
	public void clear() {
		map.clear();
	}

	@Override
	public Object get(final String name) {
		return map.get(name);
	}

	@Override
	public boolean has(final String name) {
		return map.containsKey(name);
	}

	@Override
	public void set(final String name, final Object value) {
		map.put(name, value);
	}
}