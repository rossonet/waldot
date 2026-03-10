package net.rossonet.waldot.jexl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;

import net.rossonet.waldot.api.models.WaldotNamespace;

public class JexlCmdFunction {

	protected final WaldotNamespace waldotNamespace;

	/**
	 * Creates a new JexlCmdFunction associated with the given namespace.
	 * 
	 * @param waldotNamespace the WaldotNamespace to associate with this command function
	 */
	public JexlCmdFunction(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
	}

	/**
	 * Echoes the given message back as a string.
	 * If the message is null, returns the string "null".
	 * 
	 * @param message the object to echo
	 * @return the string representation of the message
	 */
	public String echo(final Object message) {
		if (message == null) {
			return "null";
		}
		return message.toString();
	}

	/**
	 * Exits the application with exit code 0.
	 * This terminates the JVM.
	 */
	public void exit() {
		exit(0);
	}

	/**
	 * Exits the application with the specified exit code.
	 * This terminates the JVM.
	 * 
	 * @param exitCode the exit code to use when terminating the JVM
	 */
	public void exit(final int exitCode) {
		System.exit(exitCode);
	}

	/**
	 * Gets the total count of edges in the graph.
	 * 
	 * @return the number of edges in the graph
	 */
	public int getEdgesCount() {
		return waldotNamespace.getEdgesCount();
	}

	/**
	 * Gets the total count of vertices in the graph.
	 * 
	 * @return the number of vertices in the graph
	 */
	public int getVerticesCount() {
		return waldotNamespace.getVerticesCount();
	}

	/**
	 * Returns a string containing all public method names of this class.
	 * This can be used to list available commands.
	 * 
	 * @return a comma-separated list of public method names
	 */
	public String help() {
		final Method[] allMethods = this.getClass().getDeclaredMethods();
		final StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (final Method method : allMethods) {
			if (Modifier.isPublic(method.getModifiers())) {
				if (!first) {
					sb.append(", ");
				}
				sb.append(method.getName());
				first = false;
			}
		}
		return sb.toString();
	}

	/**
	 * Returns a collection of all configured command names in the namespace.
	 * 
	 * @return collection of command name strings
	 */
	public Collection<String> listCommands() {
		return waldotNamespace.listConfiguredCommands();
	}

	/**
	 * Resets the namespace to its initial state.
	 * This clears all vertices, edges, and parameters.
	 */
	public void reset() {
		waldotNamespace.resetNameSpace();
	}

	/**
	 * Converts a value to a Number.
	 * If the value is already a Number, it is returned as-is.
	 * If the value is a String, it attempts to parse it as a Double.
	 * 
	 * @param value the value to convert
	 * @return the Number representation of the value, or 0 if conversion fails
	 * @throws IllegalArgumentException if the value cannot be converted to a number
	 */
	public Number toNumber(Object value) {
		if (value instanceof Number) {
			return (Number) value;
		} else if (value instanceof String) {
			try {
				return Double.parseDouble((String) value);
			} catch (final NumberFormatException e) {
				throw new IllegalArgumentException("Cannot convert value to number: " + value, e);
			}
		}
		return 0;

	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("jexl Command Function [");
		if (waldotNamespace != null) {
			builder.append("waldotNamespace=");
			builder.append(waldotNamespace);
		}
		builder.append("]");
		return builder.toString();
	}

}
