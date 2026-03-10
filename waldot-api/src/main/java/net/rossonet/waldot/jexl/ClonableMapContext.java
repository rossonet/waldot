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
	/**
	 * Creates a ClonableMapContext with an empty HashMap as the underlying storage.
	 */
	public ClonableMapContext() {
		map = new HashMap<>();
	}

	/**
	 * Creates a ClonableMapContext as a deep copy of another ClonableMapContext.
	 * 
	 * @param clonableMapContext the context to copy from, or null to create an empty context
	 */
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
	/**
	 * Creates a ClonableMapContext wrapping an existing user-provided map.
	 * 
	 * @param vars the variable map to wrap, or null to create an empty HashMap
	 */
	public ClonableMapContext(final Map<String, Object> vars) {
		map = vars == null ? new HashMap<>() : vars;
	}

	/**
	 * Clears all variables.
	 */
	/**
	 * Clears all variables from the context.
	 */
	public void clear() {
		map.clear();
	}

	/**
	 * Gets the value of a variable from the context.
	 * 
	 * @param name the variable name
	 * @return the value associated with the name, or null if not found
	 */
	@Override
	public Object get(final String name) {
		return map.get(name);
	}

	/**
	 * Checks if a variable exists in the context.
	 * 
	 * @param name the variable name
	 * @return true if the variable exists, false otherwise
	 */
	@Override
	public boolean has(final String name) {
		return map.containsKey(name);
	}

	/**
	 * Sets a variable in the context.
	 * 
	 * @param name  the variable name
	 * @param value the value to set
	 */
	@Override
	public void set(final String name, final Object value) {
		map.put(name, value);
	}
}