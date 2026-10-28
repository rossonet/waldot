package net.rossonet.waldot.testutil;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.rossonet.waldot.api.configuration.WaldotConfiguration;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraphVariables;

/**
 * {@link WaldotNamespace} test doubles built with a JDK proxy, used to test the
 * Gremlin side without starting an OPC UA server.
 */
public final class FakeNamespaces {

	/**
	 * An empty namespace: no vertices and no edges, graph variables kept in the
	 * given map. Every other method returns the default value of its type.
	 */
	public static WaldotNamespace empty(final Map<String, Object> parameters) {
		return create(parameters, null);
	}

	/**
	 * An empty namespace whose {@link WaldotNamespace#getConfiguration()} returns
	 * the given configuration.
	 */
	public static WaldotNamespace withConfiguration(final WaldotConfiguration configuration) {
		return create(new ConcurrentHashMap<>(), configuration);
	}

	private static WaldotNamespace create(final Map<String, Object> parameters,
			final WaldotConfiguration configuration) {
		return (WaldotNamespace) Proxy.newProxyInstance(WaldotNamespace.class.getClassLoader(),
				new Class<?>[] { WaldotNamespace.class }, (proxy, method, args) -> {
					return switch (method.getName()) {
					case "getVertices", "getEdges" -> Collections.emptyMap();
					case "getVerticesCount", "getEdgesCount" -> 0;
					case "namespaceParametersGet" -> parameters.get(args[0]);
					case "namespaceParametersKeySet" -> parameters.keySet();
					case "namespaceParametersPut" -> {
						parameters.put((String) args[0], args[1]);
						yield null;
					}
					case "namespaceParametersRemove" -> {
						parameters.remove(args[0]);
						yield null;
					}
					case "getConfiguration" -> configuration;
					case "getVariables" -> new OpcGraphVariables((WaldotNamespace) proxy);
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					case "toString" -> "fake-namespace";
					default -> defaultValue(method.getReturnType());
					};
				});
	}

	/** An empty namespace with its own parameter map. */
	public static WaldotNamespace empty() {
		return empty(new ConcurrentHashMap<>());
	}

	private static Object defaultValue(final Class<?> type) {
		if (!type.isPrimitive() || type == void.class) {
			return null;
		}
		if (type == boolean.class) {
			return false;
		}
		if (type == long.class) {
			return 0L;
		}
		if (type == double.class) {
			return 0d;
		}
		if (type == float.class) {
			return 0f;
		}
		return 0;
	}

	private FakeNamespaces() {
	}
}
