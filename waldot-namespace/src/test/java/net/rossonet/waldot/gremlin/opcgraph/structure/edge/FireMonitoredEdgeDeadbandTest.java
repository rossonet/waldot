package net.rossonet.waldot.gremlin.opcgraph.structure.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.tinkerpop.gremlin.structure.Property;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.MonitoredEdge;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;

/**
 * Deadband on a fire edge uses the last value notified to the target.
 * The target does not store the property, so the edge must remember it.
 */
class FireMonitoredEdgeDeadbandTest {

	@Test
	void absoluteDeadbandSkipsChangesInsideTheBand() {
		final List<Object> fired = new ArrayList<>();
		final FireMonitoredEdge edge = fireEdge(Map.of(MonitoredEdge.DEADBAND_LABEL, 1.0d,
				MonitoredEdge.DEADBAND_TYPE_LABEL, MonitoredEdge.ABSOLUTE), fired);

		edge.propertyChanged(null, "temperature", value(10.0d));
		edge.propertyChanged(null, "temperature", value(10.4d));
		edge.propertyChanged(null, "temperature", value(11.0d));
		edge.propertyChanged(null, "temperature", value(12.1d));

		assertEquals(List.of(10.0d, 12.1d), fired);
	}

	@Test
	void percentageDeadbandUsesTheLastFiredValue() {
		final List<Object> fired = new ArrayList<>();
		final FireMonitoredEdge edge = fireEdge(Map.of(MonitoredEdge.DEADBAND_LABEL, 5.0d,
				MonitoredEdge.DEADBAND_TYPE_LABEL, MonitoredEdge.PERCENTAGE), fired);

		edge.propertyChanged(null, "temperature", value(100.0d));
		edge.propertyChanged(null, "temperature", value(104.0d));
		edge.propertyChanged(null, "temperature", value(106.0d));

		assertEquals(List.of(100.0d, 106.0d), fired);
	}

	@Test
	void deadbandIsIndependentPerProperty() {
		final List<Object> fired = new ArrayList<>();
		final FireMonitoredEdge edge = fireEdge(Map.of(MonitoredEdge.DEADBAND_LABEL, 1.0d,
				MonitoredEdge.DEADBAND_TYPE_LABEL, MonitoredEdge.ABSOLUTE), fired);

		edge.propertyChanged(null, "temperature", value(10.0d));
		edge.propertyChanged(null, "pressure", value(10.2d));
		edge.propertyChanged(null, "temperature", value(10.2d));

		assertEquals(List.of(10.0d, 10.2d), fired);
	}

	private static FireMonitoredEdge fireEdge(final Map<String, Object> edgeProperties, final List<Object> fired) {
		final WaldotEdge edge = proxy(WaldotEdge.class, (method, args) -> {
			if ("property".equals(method.getName()) && args != null && args.length == 1) {
				final Object stored = edgeProperties.get(args[0]);
				return stored == null ? Property.empty() : present(String.valueOf(args[0]), stored);
			}
			return null;
		});
		final WaldotVertex target = proxy(WaldotVertex.class, (method, args) -> {
			if ("fireProperty".equals(method.getName())) {
				fired.add(((DataValue) args[2]).getValue().getValue());
			}
			return null;
		});
		return new FireMonitoredEdge(proxy(WaldotNamespace.class, (method, args) -> null), edge,
				proxy(WaldotVertex.class, (method, args) -> null), target);
	}

	private static DataValue value(final double raw) {
		return new DataValue(new Variant(raw), StatusCode.GOOD, null, null);
	}

	private static Property<Object> present(final String key, final Object value) {
		return new Property<>() {
			@Override
			public String key() {
				return key;
			}

			@Override
			public Object value() {
				return value;
			}

			@Override
			public boolean isPresent() {
				return true;
			}

			@Override
			public org.apache.tinkerpop.gremlin.structure.Element element() {
				return null;
			}

			@Override
			public void remove() {
			}
		};
	}

	@FunctionalInterface
	private interface Call {
		Object invoke(java.lang.reflect.Method method, Object[] args) throws Throwable;
	}

	private static <T> T proxy(final Class<T> type, final Call call) {
		final Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
				(unused, method, args) -> {
					if (method.getDeclaringClass() == Object.class) {
						return switch (method.getName()) {
						case "hashCode" -> System.identityHashCode(unused);
						case "equals" -> unused == args[0];
						case "toString" -> type.getSimpleName();
						default -> null;
						};
					}
					final Object result = call.invoke(method, args);
					if (result != null || method.getReturnType() == void.class) {
						return result;
					}
					if (method.getReturnType() == boolean.class) {
						return false;
					}
					if (method.getReturnType() == int.class) {
						return 0;
					}
					if (method.getReturnType() == long.class) {
						return 0L;
					}
					return null;
				});
		return type.cast(proxy);
	}

}