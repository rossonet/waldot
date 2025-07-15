package net.rossonet.waldot.utils.gremlin;

import java.lang.reflect.Method;

import org.apache.tinkerpop.gremlin.structure.Vertex;

public class UpdateTrigger {

	private final Object analizedObject;

	private final Method method;
	private final Vertex vertex;

	public UpdateTrigger(Vertex vertex, Object analizedObject, Method method) {
		if (vertex == null) {
			throw new IllegalArgumentException("Vertex cannot be null");
		}
		if (analizedObject == null) {
			throw new IllegalArgumentException("AnalizedObject cannot be null");
		}
		if (method == null) {
			throw new IllegalArgumentException("Method cannot be null");
		}
		this.analizedObject = analizedObject;
		this.method = method;
		this.vertex = vertex;
	}

	public void invoke() {
		try {
			final Object result = method.invoke(analizedObject);
			vertex.property("value", result);
		} catch (final Exception e) {
			throw new RuntimeException("Error invoking method: " + method.getName(), e);
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("UpdateTrigger [vertex=");
		builder.append(vertex);
		builder.append(", analizedObject=");
		builder.append(analizedObject);
		builder.append("]");
		return builder.toString();
	}

}
